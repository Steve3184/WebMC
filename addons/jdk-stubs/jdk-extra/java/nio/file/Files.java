package java.nio.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class Files {
    private Files() {}

    public static Path get(Path path, String attribute, LinkOption... options) {
        return null;
    }

    public static Object getAttribute(Path path, String attribute, LinkOption... options) {
        return null;
    }

    public static Path setAttribute(Path path, String attribute, Object value, LinkOption... options) {
        return path;
    }

    public static <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null;
    }

    public static <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) {
        return null;
    }

    public static Path copy(Path source, Path target, CopyOption... options) {
        return target;
    }

    public static Path move(Path source, Path target, CopyOption... options) {
        return target;
    }

    public static void delete(Path path) throws IOException {
    }

    public static boolean deleteIfExists(Path path) {
        return false;
    }

    public static DirectoryStream<Path> newDirectoryStream(Path dir) {
        return null;
    }

    public static DirectoryStream<Path> newDirectoryStream(Path dir, String glob) {
        return null;
    }

    public static BufferedReader newBufferedReader(Path path) {
        return null;
    }

    public static BufferedReader newBufferedReader(Path path, java.nio.charset.Charset cs) {
        return null;
    }

    public static BufferedWriter newBufferedWriter(Path path, java.nio.charset.Charset cs, OpenOption... options) {
        return null;
    }

    public static InputStream newInputStream(Path path, OpenOption... options) {
        return null;
    }

    public static OutputStream newOutputStream(Path path, OpenOption... options) {
        return null;
    }

    public static byte[] readAllBytes(Path path) {
        return new byte[0];
    }

    public static List<String> readAllLines(Path path) {
        return null;
    }

    public static List<String> readAllLines(Path path, java.nio.charset.Charset cs) {
        return null;
    }

    public static Path write(Path path, byte[] bytes, OpenOption... options) {
        return path;
    }

    public static Path write(Path path, Iterable<? extends CharSequence> lines, java.nio.charset.Charset cs, OpenOption... options) {
        return path;
    }

    public static Path createDirectory(Path dir, FileAttribute<?>... attrs) {
        return dir;
    }

    public static Path createDirectories(Path dir, FileAttribute<?>... attrs) {
        return dir;
    }

    public static Path createFile(Path path, FileAttribute<?>... attrs) {
        return path;
    }

    public static Path createTempFile(Path dir, String prefix, String suffix, FileAttribute<?>... attrs) {
        return null;
    }

    public static Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs) {
        return null;
    }

    public static Path createTempDirectory(Path dir, String prefix, FileAttribute<?>... attrs) {
        return null;
    }

    public static Path createTempDirectory(String prefix, FileAttribute<?>... attrs) {
        return null;
    }

    public static Path createLink(Path link, Path existing) {
        return link;
    }

    public static Path createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) {
        return link;
    }

    public static boolean exists(Path path, LinkOption... options) {
        return false;
    }

    public static boolean notExists(Path path, LinkOption... options) {
        return true;
    }

    public static boolean isSameFile(Path path, Path path2) {
        return false;
    }

    public static boolean isDirectory(Path path, LinkOption... options) {
        return false;
    }

    public static boolean isRegularFile(Path path, LinkOption... options) {
        return false;
    }

    public static boolean isSymbolicLink(Path path) {
        return false;
    }

    public static boolean isHidden(Path path) {
        return false;
    }

    public static String probeContentType(Path path) {
        return null;
    }

    public static Set<PosixFilePermission> getPosixFilePermissions(Path path, LinkOption... options) {
        return null;
    }

    public static Path setPosixFilePermissions(Path path, Set<PosixFilePermission> perms) {
        return path;
    }

    public static FileStore getFileStore(Path path) {
        return null;
    }

    public static boolean isReadable(Path path) {
        return false;
    }

    public static boolean isWritable(Path path) {
        return false;
    }

    public static boolean isExecutable(Path path) {
        return false;
    }

    public static Path walkFileTree(Path start, FileVisitor<? super Path> visitor) {
        return start;
    }

    public static Path walkFileTree(Path start, Set<java.nio.file.FileVisitOption> options, int maxDepth, FileVisitor<? super Path> visitor) {
        return start;
    }

    public static Stream<Path> walk(Path start, int maxDepth, FileVisitOption... options) {
        return null;
    }

    public static Stream<Path> walk(Path start, FileVisitOption... options) {
        return null;
    }

    public static Stream<Path> list(Path dir) {
        return null;
    }

    public static Stream<String> lines(Path path, java.nio.charset.Charset cs) {
        return null;
    }

    public static Stream<String> lines(Path path) {
        return null;
    }

    public static long copy(InputStream in, Path target, CopyOption... options) {
        return 0;
    }

    public static long copy(Path source, OutputStream out) {
        return 0;
    }

    public interface OpenOption {}
    public interface CopyOption {}
    public interface FileVisitOption {}

    public static class LinkOption {}
    public static class DirectoryStream<T> implements Closeable, Iterable<T> {
        @Override public Iterator<T> iterator() { return null; }
        @Override public void close() {}
    }
}
