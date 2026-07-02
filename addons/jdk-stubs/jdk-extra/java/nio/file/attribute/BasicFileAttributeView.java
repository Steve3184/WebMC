package java.nio.file.attribute;

public interface BasicFileAttributeView extends FileAttributeView {
    @Override default String name() { return "basic"; }
    BasicFileAttributes readAttributes();
    void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime);
}
