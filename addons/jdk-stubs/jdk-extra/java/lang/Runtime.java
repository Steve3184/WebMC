package java.lang;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class Runtime {
    private static Runtime currentRuntime = new Runtime();

    private Runtime() {}

    public static Runtime getRuntime() {
        return currentRuntime;
    }

    public void exit(int status) {
        System.exit(status);
    }

    public void halt(int status) {
        System.exit(status);
    }

    public Process exec(String command) throws IOException {
        return null;
    }

    public Process exec(String command, String[] envp) throws IOException {
        return null;
    }

    public Process exec(String command, String[] envp, File dir) throws IOException {
        return null;
    }

    public Process exec(String[] cmdarray) throws IOException {
        return null;
    }

    public Process exec(String[] cmdarray, String[] envp) throws IOException {
        return null;
    }

    public Process exec(String[] cmdarray, String[] envp, File dir) throws IOException {
        return null;
    }

    public Process exec(String[] cmdarray, String[] envp, File dir, Map<String, String> redirectErrorStream) throws IOException {
        return null;
    }

    public void addShutdownHook(Thread hook) {
    }

    public boolean removeShutdownHook(Thread hook) {
        return false;
    }

    public void runFinalization() {
    }

    public static void runFinalizersOnExit(boolean value) {
    }

    public void load(String filename) {
    }

    public void loadLibrary(String libname) {
    }

    public String getLibraryPath() {
        return null;
    }

    public void traceInstructions(boolean on) {
    }

    public void traceMethodCalls(boolean on) {
    }

    public long maxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public long totalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    public long freeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public long availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public void gc() {
    }

    public native void nativeExit(int status);
    public native int availableProcessors0();
}
