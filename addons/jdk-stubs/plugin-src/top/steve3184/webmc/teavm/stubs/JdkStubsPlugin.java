package top.steve3184.webmc.teavm.stubs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 * Inject JDK shadow stubs (java.util.concurrent.locks.*, sun.misc.Unsafe,
 * various other JDK classes TeaVM 0.13.x doesn't ship) into the dependency
 * analyzer. Plain "put .class on classpath" doesn't work — TeaVM treats
 * java.* / sun.* as a system namespace resolved against the built-in classlib
 * only. The supported escape hatch is the plugin SPI: register a
 * DependencyListener and call {@link DependencyAgent#submitClassFile} with
 * raw bytecode for each name we want to materialize.
 *
 * Class list comes from META-INF/jdk-stubs-index.txt, generated at jar-build
 * time by the gradle task `generateStubIndex` so we never have to hand-list
 * every inner class.
 */
public class JdkStubsPlugin implements TeaVMPlugin {
    @Override
    public void install(TeaVMHost host) {
        System.err.println("[jdk-stubs-plugin] install() called — registering DependencyListener and ClassHolderTransformer");
        host.add(new JdkMethodStubsTransformer());
        host.add(new AbstractDependencyListener() {
            @Override
            public void started(DependencyAgent agent) {
                ClassLoader cl = JdkStubsPlugin.class.getClassLoader();
                java.util.List<String> entries = readIndex(cl);
                System.err.println("[jdk-stubs-plugin] started() — submitting " + entries.size() + " stubs");
                int ok = 0, fail = 0, skipped = 0;
                for (String entry : entries) {
                    String resourcePath = "shadow-classes/" + entry;
                    try (InputStream in = cl.getResourceAsStream(resourcePath)) {
                        if (in == null) {
                            System.err.println("[jdk-stubs-plugin] missing resource: " + resourcePath);
                            fail++;
                            continue;
                        }
                        byte[] bytes = in.readAllBytes();
                        try {
                            agent.submitClassFile(bytes);
                            ok++;
                        } catch (IllegalArgumentException e) {
                            // Class already exists in TeaVM classlib - skip silently
                            if (e.getMessage() != null && e.getMessage().contains("already defined")) {
                                skipped++;
                                continue;
                            }
                            throw e;
                        }
                    } catch (Exception e) {
                        System.err.println("[jdk-stubs-plugin] failed to inject " + resourcePath + ": " + e);
                        fail++;
                    }
                }
                System.err.println("[jdk-stubs-plugin] done — " + ok + " ok, " + skipped + " skipped, " + fail + " failed");
            }
        });
    }

    private static java.util.List<String> readIndex(ClassLoader cl) {
        java.util.List<String> result = new java.util.ArrayList<>();
        try (InputStream in = cl.getResourceAsStream("META-INF/jdk-stubs-index.txt")) {
            if (in == null) {
                System.err.println("[jdk-stubs-plugin] index file not found — no stubs will be injected");
                return result;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) result.add(line);
            }
        } catch (IOException e) {
            System.err.println("[jdk-stubs-plugin] failed to read index: " + e);
        }
        return result;
    }
}
