package java.lang;

public class ProcessBuilder {
    public ProcessBuilder(java.util.List<String> command) {}
    public ProcessBuilder(String... command) {}
    public ProcessBuilder command(String... command) { return this; }
    public ProcessBuilder command(java.util.List<String> command) { return this; }
    public java.util.List<String> command() { return java.util.Collections.emptyList(); }
    public java.util.Map<String,String> environment() { return java.util.Collections.emptyMap(); }
    public java.io.File directory() { return null; }
    public ProcessBuilder directory(java.io.File directory) { return this; }
    public ProcessBuilder redirectErrorStream(boolean redirectErrorStream) { return this; }
    public boolean redirectErrorStream() { return false; }
    public Process start() { throw new UnsupportedOperationException("ProcessBuilder.start not supported in browser"); }
}
