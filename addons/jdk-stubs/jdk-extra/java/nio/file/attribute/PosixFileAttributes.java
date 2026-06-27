package java.nio.file.attribute;

public interface PosixFileAttributes extends BasicFileAttributes {
    java.security.Principal owner();
    java.security.Principal group();
    java.util.Set<PosixFilePermission> permissions();
}
