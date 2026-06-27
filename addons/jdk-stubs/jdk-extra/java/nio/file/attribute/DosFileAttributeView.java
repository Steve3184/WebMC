package java.nio.file.attribute;

public interface DosFileAttributeView extends BasicFileAttributeView {
    DosFileAttributes readAttributes();
    void setReadOnly(boolean value);
    void setHidden(boolean value);
    void setSystem(boolean value);
    void setArchive(boolean value);
}
