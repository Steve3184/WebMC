package java.lang;

import java.util.Collections;
import java.util.Set;
import java.util.List;

public final class ModuleLayer {
    ModuleLayer() {}
    public static ModuleLayer boot() { throw new UnsupportedOperationException(); }
    public Set<Module> modules() { return Collections.emptySet(); }
    public java.util.Optional<Module> findModule(String name) { return java.util.Optional.empty(); }
    public ClassLoader findLoader(String name) { return null; }
    public List<ModuleLayer> parents() { return Collections.emptyList(); }
}
