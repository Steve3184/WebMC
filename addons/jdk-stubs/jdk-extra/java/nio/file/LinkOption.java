package java.nio.file;

/**
 * Stub for java.nio.file.LinkOption.
 * This is an enum-like class with NOFOLLOW_LINKS constant.
 * Note: In JDK, this is actually an interface with nested enum, but some code
 * expects it as a standalone class. We create it as a concrete class for TeaVM compatibility.
 */
public class LinkOption {
    public static final LinkOption NOFOLLOW_LINKS = new LinkOption();

    private LinkOption() {}

    @Override
    public String toString() {
        return "NOFOLLOW_LINKS";
    }
}
