package java.nio.file;

import java.io.File;
import java.nio.file.attribute.BasicFileAttributes;

public interface Path {
    Path getParent();
    Path getRoot();
    Path getFileName();
    Path getName(int index);
    int getNameCount();
    Path subpath(int beginIndex, int endIndex);
    boolean isAbsolute();
    Path toAbsolutePath();
    Path toRealPath(java.nio.file.LinkOption... options);
    File toFile();
    boolean startsWith(Path other);
    boolean startsWith(String other);
    boolean endsWith(Path other);
    boolean endsWith(String other);
    Path normalize();
    Path resolve(Path other);
    Path resolve(String other);
    Path resolveSibling(Path other);
    Path resolveSibling(String other);
    Path relativize(Path other);
    String toString();
    int compareTo(Path other);
    boolean equals(Object obj);
    int hashCode();
    java.net.URI toUri();
}
