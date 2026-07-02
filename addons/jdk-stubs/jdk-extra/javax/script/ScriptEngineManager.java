package javax.script;

import java.util.Collections;
import java.util.List;

public class ScriptEngineManager {
    public ScriptEngineManager() {}
    public ScriptEngineManager(ClassLoader loader) {}
    public ScriptEngine getEngineByName(String shortName) { return null; }
    public ScriptEngine getEngineByExtension(String extension) { return null; }
    public ScriptEngine getEngineByMimeType(String mimeType) { return null; }
    public List<ScriptEngineFactory> getEngineFactories() { return Collections.emptyList(); }
    public void registerEngineName(String name, ScriptEngineFactory factory) {}
    public void registerEngineMimeType(String type, ScriptEngineFactory factory) {}
    public void registerEngineExtension(String extension, ScriptEngineFactory factory) {}
    public void put(String key, Object value) {}
    public Object get(String key) { return null; }
}
