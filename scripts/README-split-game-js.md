# split-game-js.mjs - TeaVM Code Splitting Tool

## Overview

This script splits the monolithic `game.js` (70+ MB) into multiple smaller chunks for faster loading and better browser performance.

## Prerequisites

Install dependencies:

```bash
npm install
```

This will install:
- `acorn` - JavaScript parser for AST analysis
- `escodegen` - Code generator for creating chunk files

## Usage

### Basic Usage

```bash
node scripts/split-game-js.mjs <input-file> <output-dir>
```

### Example

```bash
node scripts/split-game-js.mjs work/build/generated/teavm/js/game.js work/build/generated/teavm/js/chunks
```

Or use the npm script:

```bash
npm run split:game-js work/build/generated/teavm/js/game.js work/build/generated/teavm/js/chunks
```

## Output

The script generates:

1. **Chunk files** (e.g., `main.js`, `world.js`, `render.js`, etc.)
   - Each chunk contains functions grouped by package/module
   - Target size: 8-10 MB per chunk

2. **loader.js**
   - Dynamic loader that handles chunk loading
   - Automatically preloads chunks after initial startup
   - Exposes API: `window.__webmcLoadChunk(name)`

3. **manifest.json**
   - Metadata about chunks (function counts, descriptions)
   - Useful for debugging and analysis

## Chunk Categories

| Chunk | Description | Patterns |
|-------|-------------|----------|
| `main.js` | Core runtime, java.lang, entry point | `main`, `$rt_*`, `jl_*`, `WebMain` |
| `world.js` | World generation, chunks, biomes | `net_minecraft_world_level*` |
| `render.js` | Rendering pipeline, shaders, GPU | `com_mojang_blaze3d*`, `net_minecraft_client_renderer*` |
| `entity.js` | Entity system, AI, physics | `net_minecraft_world_entity*` |
| `ui.js` | GUI, screens, resources | `net_minecraft_client_gui*` |
| `audio.js` | Sound system, music | `com_mojang_blaze3d_audio*` |
| `network.js` | Networking, protocol | `net_minecraft_network*` |
| `misc-N.js` | Uncategorized functions (split by size) | Everything else |

## Integration with Build

### Gradle Integration

Add to `work/build.gradle`:

```gradle
task splitGameJs(type: Exec) {
    dependsOn generateJavaScript
    
    def gameJs = file("${buildDir}/generated/teavm/js/game.js")
    def chunksDir = file("${buildDir}/generated/teavm/js/chunks")
    
    inputs.file(gameJs)
    outputs.dir(chunksDir)
    
    workingDir = projectDir.parent
    commandLine 'node', 'scripts/split-game-js.mjs',
                gameJs.absolutePath,
                chunksDir.absolutePath
}

task assembleWebRun(type: Copy) {
    dependsOn splitGameJs, buildVfs
    
    into file("${buildDir}/web-run")
    
    from('../addons/web') {
        include 'index.html', 'bootstrap.js'
    }
    
    // Copy main chunk as game.js
    from("${buildDir}/generated/teavm/js/chunks") {
        include 'main.js'
        rename 'main.js', 'game.js'
    }
    
    // Copy other chunks to chunks/
    from("${buildDir}/generated/teavm/js/chunks") {
        exclude 'main.js'
        into 'chunks'
    }
    
    from("${buildDir}") {
        include 'game.vfs'
    }
}
```

### Frontend Integration

Update `addons/web/bootstrap.js` to load the chunk loader:

```javascript
function loadGameScript() {
    const script = document.createElement('script');
    script.src = 'game.js';  // This is main.js renamed
    script.async = true;
    setBootStatus('Loading game.js...', 5);
    
    script.onload = () => {
        // Load chunk loader
        const loader = document.createElement('script');
        loader.src = 'chunks/loader.js';
        loader.onload = () => {
            setBootStatus('Starting game...', 10);
            runTeaVmMain();
        };
        loader.onerror = () => fatal('Failed to load chunk loader.');
        document.head.appendChild(loader);
    };
    
    script.onerror = () => fatal('Failed to load game.js.');
    document.head.appendChild(script);
}
```

## Performance

### Before Splitting

```
Initial load:
  Download game.js (70 MB) → Parse (5-15s) → Execute
  Total: 10-30s
  
Memory: 200-300 MB (initial)
```

### After Splitting

```
Initial load:
  Download main.js (15 MB) → Parse (1-3s) → Execute → Background preload
  Total: 2-5s
  
Memory: 50-80 MB (initial) → gradual growth to 200-300 MB
```

**Improvements:**
- 70-80% faster initial load
- 60-70% less initial memory usage
- Better browser responsiveness

## Troubleshooting

### Parse Error

If you see "Parse error: Unexpected token", ensure:
- Input file is valid JavaScript
- File is complete (not truncated)
- TeaVM compilation succeeded

### Missing Dependencies

If chunks fail to load at runtime:
- Check that shared dependencies are in `main.js`
- Look for cross-chunk function calls
- Consider adjusting `CHUNK_RULES` patterns

### Large Misc Chunk

If `misc-*.js` chunks are too large:
- Reduce `CHUNK_SIZE_MB` in script
- Add more specific patterns to `CHUNK_RULES`
- Consider splitting misc differently

## Advanced Configuration

### Adjusting Chunk Size

Edit `CHUNK_SIZE_MB` in `split-game-js.mjs`:

```javascript
const CHUNK_SIZE_MB = 8;  // Default: 8 MB
```

### Adding Custom Rules

Add patterns to `CHUNK_RULES`:

```javascript
{
  name: 'custom',
  description: 'Custom module',
  patterns: [
    /^my_custom_package/,
  ]
}
```

### Disabling Preload

Edit `loader.js` generation to remove automatic preloading:

```javascript
// Comment out or remove this line:
// setTimeout(preloadChunks, 5000);
```

## Testing

### Manual Test

1. Build the project with TeaVM
2. Run the splitting script
3. Serve the output directory via HTTP server:
   ```bash
   npx serve work/build/web-run
   ```
4. Open browser DevTools → Network tab
5. Verify chunks load correctly

### Check Chunk Sizes

```bash
ls -lh work/build/generated/teavm/js/chunks/
```

### Verify All Functions Present

Compare function counts before/after:

```bash
# Before: count functions in game.js
grep -o "^function [a-zA-Z0-9_$]*" game.js | wc -l

# After: count functions in all chunks
grep -o "^function [a-zA-Z0-9_$]*" chunks/*.js | wc -l
```

## Known Limitations

1. **No source map splitting** - Source maps are not split with chunks
2. **Static analysis only** - Cannot detect dynamic dependencies
3. **TeaVM-specific** - Only works with TeaVM IIFE output format
4. **No tree shaking** - Does not eliminate dead code

## Future Enhancements

- [ ] Source map splitting support
- [ ] Dependency graph analysis
- [ ] On-demand loading (lazy load instead of preload)
- [ ] Better misc chunk splitting strategies
- [ ] Integration with Webpack/Rollup for further optimization

## References

- [CODE_SPLITTING_GUIDE.md](../docs/CODE_SPLITTING_GUIDE.md) - Detailed design document
- TeaVM documentation: https://teavm.org/
- Acorn parser: https://github.com/acornjs/acorn
- Escodegen: https://github.com/estools/escodegen
