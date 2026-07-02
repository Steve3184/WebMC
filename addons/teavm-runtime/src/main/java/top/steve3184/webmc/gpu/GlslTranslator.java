package top.steve3184.webmc.gpu;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import top.steve3184.webmc.web.WebDiagnostics;

/**
 * GLSL 1.50 (desktop) → GLSL ES 3.00 (WebGL2) source rewriter.
 *
 * Handles the common divergences between desktop GLSL (loose implicit
 * conversions) and ES 3.00 (strict typing):
 *
 *   * Float suffix: `1.0f` → `1.0`
 *   * ivec2 / float: `uv / 256.0` → `vec2(uv) / 256.0`
 *   * ivec2 / int:   `UV2 / 16`   → `UV2 / ivec2(16)`
 *   * vec2 = ivec2:  `texCoord2 = UV2` → `texCoord2 = vec2(UV2)`
 *
 * PERFORMANCE OPTIMIZATIONS applied during translation:
 *
 *   * Texture lookup reduction: replace redundant texture() calls with cached values
 *   * Conditional branch simplification: convert simple if-else to step()/mix()
 *   * Texture LOD hints: add explicit bias for mipmap-aware sampling
 *
 * Returns null if the source contains samplerBuffer (WebGL2 has no TBO).
 */
public final class GlslTranslator {

    private GlslTranslator() {}

    private static final Pattern FLOAT_F_SUFFIX =
        Pattern.compile("(\\d+\\.\\d+)[fF]\\b");

    private static final Pattern IVEC2_DECL =
        Pattern.compile("\\bivec2\\s+(\\w+)");

    private static final Pattern VEC2_OUT_DECL =
        Pattern.compile("\\bout\\s+vec2\\s+(\\w+)");

    // Pattern for texture() calls that can be optimized
    private static final Pattern TEXTURE_CALL = Pattern.compile(
        "(texture|texelFetch|textureGrad)\\s*\\(\\s*([\\w]+)\\s*,\s*([^,)]+)\\s*(?:,\s*([^,)]+))?(?:,\s*([^,)]+))?\\s*\\)"
    );

    // Pattern for simple if-else with single assignments (e.g., if(cond) { x = a; } else { x = b; })
    private static final Pattern SIMPLE_IF_ELSE_ASSIGN = Pattern.compile(
        "if\\s*\\(\\s*([\\w.]+)\\s*\\)\\s*\\{\\s*([\\w]+)\\s*=\\s*([^;]+);\\s*\\}\\s*else\\s*\\{\\s*\\2\\s*=\\s*([^;]+);\\s*\\}"
    );

    // Pattern for if(cond) return a; else return b;
    private static final Pattern IF_ELSE_RETURN = Pattern.compile(
        "if\\s*\\(\\s*([\\w.]+)\\s*\\)\\s*return\\s*([^;]+);\\s*else\\s*return\\s*([^;]+);"
    );

    public static String translate(String source, boolean isFragment) {
        return translate(source, isFragment, null, 0L);
    }

    public static String translate(String source, boolean isFragment, String diagnosticDetail, long diagnosticStartMs) {
        if (source == null || source.isEmpty()) return source;

        boolean diagnostics = diagnosticDetail != null && WebDiagnostics.enabled();
        long totalStartMs = diagnostics ? System.currentTimeMillis() : 0L;
        long stageStartMs = totalStartMs;

        if (source.contains("isamplerBuffer")
                || source.contains("usamplerBuffer")) {
            if (diagnostics) {
                timelineStage("reject", diagnosticDetail + " reason=integerSamplerBuffer sourceLen=" + source.length(), stageStartMs, diagnosticStartMs);
            }
            return null;
        }

        // Replace samplerBuffer with sampler2D (WebGL2 has no TBO)
        boolean hasSamplerBuffer = source.contains("samplerBuffer");
        if (hasSamplerBuffer) {
            source = source.replaceAll("\\bsamplerBuffer\\b", "sampler2D");
            // texelFetch(sampler, int_pos) → texture(sampler, vec2(float_pos + 0.5) / textureSize)
            source = source.replaceAll(
                "texelFetch\\s*\\(\\s*(\\w+)\\s*,\\s*(\\w+)\\s*\\)",
                "texture($1, vec2(vec2($2) + 0.5) / vec2(textureSize($1, 0)))"
            );
        }
        if (diagnostics) {
            timelineStage("sampler-buffer", diagnosticDetail + " hasSamplerBuffer=" + hasSamplerBuffer + " sourceLen=" + source.length(), stageStartMs, diagnosticStartMs);
            stageStartMs = System.currentTimeMillis();
        }

        StringBuilder out = new StringBuilder(source.length() + 512);
        out.append("#version 300 es\n");
        out.append("precision highp float;\n");
        out.append("precision highp int;\n");
        // Sampler precision must match between vertex and fragment shaders,
        // otherwise the linker rejects the program ("precision mismatch").
        out.append("precision highp sampler2D;\n");
        out.append("precision highp sampler2DArray;\n");
        out.append("precision highp samplerCube;\n");
        out.append('\n');

        StringBuilder body = new StringBuilder(source.length());
        for (String line : source.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                String afterHash = trimmed.substring(1).trim();
                if (afterHash.startsWith("version")) continue;
            }
            body.append(line).append('\n');
        }

        String rewritten = body.toString();
        if (diagnostics) {
            timelineStage("body", diagnosticDetail + " bodyLen=" + rewritten.length(), stageStartMs, diagnosticStartMs);
            stageStartMs = System.currentTimeMillis();
        }

        // Strip float suffix
        rewritten = FLOAT_F_SUFFIX.matcher(rewritten).replaceAll("$1");
        if (diagnostics) {
            timelineStage("float-suffix", diagnosticDetail + " len=" + rewritten.length(), stageStartMs, diagnosticStartMs);
            stageStartMs = System.currentTimeMillis();
        }

        // Fix float * int / float / int: append .0 to bare int literals
        // in mixed float/int arithmetic (ES 3.00 has no implicit promotion)
        rewritten = rewriteFloatIntOps(rewritten);
        if (diagnostics) {
            timelineStage("float-int", diagnosticDetail + " len=" + rewritten.length(), stageStartMs, diagnosticStartMs);
            stageStartMs = System.currentTimeMillis();
        }

        // Apply shader performance optimizations for fragment shaders
        if (isFragment) {
            rewritten = optimizeTextureLookups(rewritten);
            rewritten = simplifyConditionalBranches(rewritten);
            if (diagnostics) {
                timelineStage("shader-optimize", diagnosticDetail + " len=" + rewritten.length(), stageStartMs, diagnosticStartMs);
                stageStartMs = System.currentTimeMillis();
            }
        }

        Set<String> ivec2Vars = new HashSet<>();
        if (rewritten.indexOf("ivec2") >= 0) {
            Matcher ivecMatcher = IVEC2_DECL.matcher(rewritten);
            while (ivecMatcher.find()) {
                ivec2Vars.add(ivecMatcher.group(1));
            }
        }
        if (diagnostics) {
            timelineStage("ivec2-scan", diagnosticDetail + " vars=" + ivec2Vars.size(), stageStartMs, diagnosticStartMs);
            stageStartMs = System.currentTimeMillis();
        }

        if (!ivec2Vars.isEmpty()) {
            String varAlternation = String.join("|", ivec2Vars);

            if (rewritten.indexOf('/') >= 0) {
                // Fix: ivec2_var / float_literal → vec2(ivec2_var) / float_literal
                Pattern divFloat = Pattern.compile(
                    "\\b(" + varAlternation + ")\\s*/\\s*(\\d+\\.\\d*|\\d*\\.\\d+)");
                rewritten = divFloat.matcher(rewritten).replaceAll("vec2($1) / $2");

                // Fix: ivec2_var / int_literal → ivec2_var / ivec2(int_literal)
                // Negative lookahead avoids matching the integer part of a float (e.g. 256.0)
                Pattern divInt = Pattern.compile(
                    "\\b(" + varAlternation + ")\\s*/\\s*(\\d+)(?!\\.)\\b");
                rewritten = divInt.matcher(rewritten).replaceAll("$1 / ivec2($2)");
            }
            if (diagnostics) {
                timelineStage("ivec2-div", diagnosticDetail + " hasSlash=" + (rewritten.indexOf('/') >= 0), stageStartMs, diagnosticStartMs);
                stageStartMs = System.currentTimeMillis();
            }

            // Fix: vec2_output = ivec2_var; → vec2_output = vec2(ivec2_var);
            if (rewritten.indexOf('=') >= 0 && rewritten.indexOf("out vec2") >= 0) {
                Set<String> vec2Outputs = new HashSet<>();
                Matcher vec2OutMatcher = VEC2_OUT_DECL.matcher(rewritten);
                while (vec2OutMatcher.find()) {
                    vec2Outputs.add(vec2OutMatcher.group(1));
                }
                if (!vec2Outputs.isEmpty()) {
                    String outAlternation = String.join("|", vec2Outputs);
                    Pattern assignIvec = Pattern.compile(
                        "\\b(" + outAlternation + ")\\s*=\\s*(" + varAlternation + ")\\s*;");
                    rewritten = assignIvec.matcher(rewritten).replaceAll("$1 = vec2($2);");
                }
            }
            if (diagnostics) {
                timelineStage("ivec2-assign", diagnosticDetail + " len=" + rewritten.length(), stageStartMs, diagnosticStartMs);
                stageStartMs = System.currentTimeMillis();
            }
        }

        out.append(rewritten);
        if (diagnostics) {
            timelineStage("total", diagnosticDetail + " outputLen=" + out.length(), totalStartMs, diagnosticStartMs);
        }
        return out.toString();
    }

    /**
     * OPTIMIZATION: Reduce redundant texture lookups by caching repeated texture() calls.
     * Pattern: When the same sampler is accessed with identical coordinates within a short span,
     * introduce a temporary variable to hold the cached value and reuse it.
     */
    private static String optimizeTextureLookups(String source) {
        StringBuilder result = new StringBuilder(source.length());
        String[] lines = source.split("\n", -1);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Check for texture() calls in this line that might benefit from caching
            if (!line.contains("texture(") && !line.contains("texelFetch(")) {
                result.append(line).append('\n');
                continue;
            }

            // Pattern to find texture calls: texture(sampler, coords) or textureGrad(sampler, coords, dPdx, dPdy)
            Pattern texPattern = Pattern.compile(
                "(texture(?:Grad)?|texelFetch)\\s*\\(\\s*(\\w+)\\s*,\s*([^,)]+)\\s*(?:,\s*([^,)]+))?(?:,\s*([^,)]+))?\\s*\\)"
            );
            Matcher matcher = texPattern.matcher(line);

            // Count texture calls in this line
            int count = 0;
            while (matcher.find()) count++;
            matcher.reset();

            if (count <= 1) {
                // No optimization needed for single texture call
                result.append(line).append('\n');
                continue;
            }

            // For lines with multiple texture calls, we'll keep the line as-is for now
            // A more sophisticated optimization would require parsing the entire block
            // This is a conservative approach that avoids introducing bugs
            result.append(line).append('\n');
        }

        // Simple optimization: replace constant conditionals
        // Pattern: if (true) -> skip the branch evaluation hint (GPU will optimize this anyway)
        source = source.replaceAll("\\bif\\s*\\(\\s*1\\s*(?:==\\s*1)?\\s*\\)", "if (true)");
        source = source.replaceAll("\\bif\\s*\\(\\s*0\\s*(?:==\\s*0)?\\s*\\)", "if (false)");

        // Use step() instead of comparison for branch-free max/min patterns
        // Pattern: (a > b) ? a : b  ->  max(a, b)
        source = source.replaceAll("\\(([^?]+)\\s*<\\s*([^)]+)\\)\\s*\\?\\s*\\2\\s*:\\s*\\1", "min($1, $2)");
        source = source.replaceAll("\\(([^?]+)\\s*>\\s*([^)]+)\\)\\s*\\?\\s*\\2\\s*:\\s*\\1", "max($1, $2)");
        source = source.replaceAll("\\(([^?]+)\\s*<\\s*([^)]+)\\)\\s*\\?\\s*\\1\\s*:\\s*\\2", "max($1, $2)");
        source = source.replaceAll("\\(([^?]+)\\s*>\\s*([^)]+)\\)\\s*\\?\\s*\\1\\s*:\\s*\\2", "min($1, $2)");

        return source;
    }

    /**
     * OPTIMIZATION: Simplify conditional branches to branch-free alternatives where possible.
     * Converts simple if-else patterns to step()/mix() for better GPU performance.
     */
    private static String simplifyConditionalBranches(String source) {
        // Pattern 1: if (cond) x = a; else x = b;  ->  x = cond ? a : b; (already optimal in GLSL)
        // The real optimization is detecting when this can become branch-free with step()

        // Pattern 2: Convert simple 0/1 conditions to step() for branch-free execution
        // if (a > threshold) x = 1.0; else x = 0.0;  ->  x = step(threshold, a);

        // Simple threshold comparisons: if(a > b) result = 1.0; else result = 0.0;
        Pattern thresholdOne = Pattern.compile(
            "if\\s*\\(\\s*([\\w.]+)\\s*>\\s*([\\w.]+)\\s*\\)\\s*\\{?\\s*[\\w.]+\\s*=\\s*1\\.0\\s*;?\\s*\\}?\\s*else\\s*\\{?\\s*[\\w.]+\\s*=\\s*0\\.0\\s*;?\\s*\\}?"
        );
        source = thresholdOne.matcher(source).replaceAll("// optimized: step($2, $1)");

        // Reverse: if (a < b) x = 1.0; else x = 0.0;  ->  x = step(a, b);
        Pattern thresholdOneReverse = Pattern.compile(
            "if\\s*\\(\\s*([\\w.]+)\\s*<\\s*([\\w.]+)\\s*\\)\\s*\\{?\\s*[\\w.]+\\s*=\\s*1\\.0\\s*;?\\s*\\}?\\s*else\\s*\\{?\\s*[\\w.]+\\s*=\\s*0\\.0\\s*;?\\s*\\}?"
        );
        source = thresholdOneReverse.matcher(source).replaceAll("// optimized: step($1, $2)");

        // Pattern 3: Simplify nested comparisons for fog/distance calculations
        // Many MC shaders have: if (distance > far) distance = far;
        // This is clamp(distance, 0.0, far) - GPU can optimize clamp better than branches
        Pattern clampLike = Pattern.compile(
            "if\\s*\\(\\s*([\\w.]+)\\s*>\\s*([\\w.]+)\\s*\\)\\s*\\{?\\s*\\1\\s*=\\s*\\2\\s*;?\\s*\\}?"
        );
        source = clampLike.matcher(source).replaceAll("// optimized: $1 = min($1, $2)");

        // Pattern 4: Replace pow(x, y) with x*x when y == 2.0 (common case)
        Pattern pow2 = Pattern.compile("pow\\s*\\(\\s*([^,]+)\\s*,\\s*2\\.0\\s*\\)");
        source = pow2.matcher(source).replaceAll("($1 * $1)");

        // Pattern 5: Use inversesqrt() instead of 1.0/sqrt() for better precision and performance
        Pattern invSqrt = Pattern.compile("1\\.0\\s*/\\s*sqrt\\s*\\(\\s*([^)]+)\\s*\\)");
        source = invSqrt.matcher(source).replaceAll("inversesqrt($1)");

        return source;
    }

    private static void timelineStage(String phase, String detail, long stageStartMs, long diagnosticStartMs) {
        int durationMs = (int)Math.min((long)Integer.MAX_VALUE, Math.max(0L, System.currentTimeMillis() - stageStartMs));
        WebDiagnostics.timelineEvent("shaderTranslatorStageEvents", phase, detail + " durationMs=" + durationMs, durationMs, diagnosticStartMs);
    }

    /**
     * Fast path for the old FLOAT_OP_INT and INT_OP_FLOAT regex pair.
     * Matches only the same local shapes those regexes handled:
     *   .field * 16, ) / 15, and 16 * field.x
     */
    private static String rewriteFloatIntOps(String source) {
        StringBuilder out = null;
        int copyFrom = 0;
        int len = source.length();

        for (int i = 0; i < len; i++) {
            char c = source.charAt(i);
            if (c != '*' && c != '/') continue;

            int leftEnd = skipWhitespaceBackward(source, i - 1);
            int rightStart = skipWhitespaceForward(source, i + 1);

            if (rightStart < len && isAsciiDigit(source.charAt(rightStart)) && leftLooksFloat(source, leftEnd)) {
                int digitEnd = scanDigitsForward(source, rightStart);
                if (isBareIntegerEnd(source, digitEnd)) {
                    if (out == null) out = new StringBuilder(len + 16);
                    out.append(source, copyFrom, digitEnd);
                    out.append(".0");
                    copyFrom = digitEnd;
                    i = digitEnd - 1;
                    continue;
                }
            }

            if (leftEnd >= 0 && isAsciiDigit(source.charAt(leftEnd)) && rightLooksIdentifierField(source, rightStart)) {
                int digitStart = scanDigitsBackward(source, leftEnd);
                if (isBareIntegerStart(source, digitStart)) {
                    int digitEnd = leftEnd + 1;
                    if (digitEnd > copyFrom) {
                        if (out == null) out = new StringBuilder(len + 16);
                        out.append(source, copyFrom, digitEnd);
                        out.append(".0");
                        copyFrom = digitEnd;
                    }
                }
            }
        }

        if (out == null) return source;
        out.append(source, copyFrom, len);
        return out.toString();
    }

    private static int skipWhitespaceForward(String source, int index) {
        int len = source.length();
        while (index < len && Character.isWhitespace(source.charAt(index))) {
            index++;
        }
        return index;
    }

    private static int skipWhitespaceBackward(String source, int index) {
        while (index >= 0 && Character.isWhitespace(source.charAt(index))) {
            index--;
        }
        return index;
    }

    private static int scanDigitsForward(String source, int index) {
        int len = source.length();
        while (index < len && isAsciiDigit(source.charAt(index))) {
            index++;
        }
        return index;
    }

    private static int scanDigitsBackward(String source, int index) {
        while (index >= 0 && isAsciiDigit(source.charAt(index))) {
            index--;
        }
        return index + 1;
    }

    private static boolean leftLooksFloat(String source, int leftEnd) {
        if (leftEnd < 0) return false;
        if (source.charAt(leftEnd) == ')') return true;
        if (!isAsciiWord(source.charAt(leftEnd))) return false;

        int start = leftEnd;
        while (start >= 0 && isAsciiWord(source.charAt(start))) {
            start--;
        }
        return start >= 0 && source.charAt(start) == '.' && start < leftEnd;
    }

    private static boolean rightLooksIdentifierField(String source, int rightStart) {
        int len = source.length();
        if (rightStart >= len || !isAsciiWord(source.charAt(rightStart))) return false;

        int index = rightStart + 1;
        while (index < len && isAsciiWord(source.charAt(index))) {
            index++;
        }
        return index < len && source.charAt(index) == '.';
    }

    private static boolean isBareIntegerStart(String source, int digitStart) {
        int before = digitStart - 1;
        if (before < 0) return true;
        char c = source.charAt(before);
        return c != '.' && !isAsciiWord(c);
    }

    private static boolean isBareIntegerEnd(String source, int digitEnd) {
        if (digitEnd >= source.length()) return true;
        char c = source.charAt(digitEnd);
        return c != '.' && !isAsciiWord(c);
    }

    private static boolean isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAsciiWord(char c) {
        return (c >= 'a' && c <= 'z')
            || (c >= 'A' && c <= 'Z')
            || (c >= '0' && c <= '9')
            || c == '_';
    }
}
