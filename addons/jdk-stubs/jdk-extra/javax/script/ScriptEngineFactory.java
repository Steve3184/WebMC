package javax.script;

public interface ScriptEngineFactory {
    String getEngineName();
    String getEngineVersion();
    java.util.List<String> getExtensions();
    java.util.List<String> getMimeTypes();
    java.util.List<String> getNames();
    String getLanguageName();
    String getLanguageVersion();
    Object getParameter(String key);
    String getMethodCallSyntax(String obj, String m, String... args);
    String getOutputStatement(String toDisplay);
    String getProgram(String... statements);
    ScriptEngine getScriptEngine();
}
