package java.nio.file;

public abstract class FileStore {
    protected FileStore() {}
    public abstract String name();
    public abstract String type();
    public abstract boolean isReadOnly();
    public abstract long getTotalSpace();
    public abstract long getUsableSpace();
    public abstract long getUnallocatedSpace();
    public abstract boolean supportsFileAttributeView(Class<? extends java.nio.file.attribute.FileAttributeView> type);
    public abstract boolean supportsFileAttributeView(String name);
    public abstract <V extends java.nio.file.attribute.FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type);
    public abstract Object getAttribute(String attribute);
}
