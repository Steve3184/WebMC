package javax.script;

public interface ScriptEngine {
    Object eval(String script);
    Object eval(java.io.Reader reader);
    Object get(String key);
    void put(String key, Object value);
    Bindings getBindings(int scope);
    void setBindings(Bindings bindings, int scope);
    Bindings createBindings();
    ScriptContext getContext();
    void setContext(ScriptContext context);
    ScriptEngineFactory getFactory();

    int ENGINE = 100;
    int ENGINE_VERSION = 101;
    int NAME = 102;
    int LANGUAGE = 103;
    int LANGUAGE_VERSION = 104;
    int FILENAME = 105;

    interface Bindings extends java.util.Map<String, Object> {}
    interface ScriptContext {}
}
