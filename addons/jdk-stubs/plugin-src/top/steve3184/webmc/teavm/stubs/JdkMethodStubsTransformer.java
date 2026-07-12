package top.steve3184.webmc.teavm.stubs;

import java.util.HashMap;
import java.util.Map;
import org.teavm.model.AccessLevel;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;

/**
 * Adds stub bodies for JCL methods that exist in real Java but are missing
 * from TeaVM 0.13.x's classlib. Each entry encodes whether the method is
 * static or instance, plus the return shape so the body can be a single
 * default-value return.
 *
 * Without these, TeaVM's strict reachability analysis fails the build with
 * "Method X.Y was not found" even though our JCL is otherwise complete.
 */
public class JdkMethodStubsTransformer implements ClassHolderTransformer {
    private record StubMethod(String name, String descriptor, boolean isStatic, ReturnKind ret) {}

    private enum ReturnKind {
        VOID,
        NULL_OBJECT,
        ZERO_INT,
        ZERO_LONG,
        ZERO_FLOAT,
        FALSE_BOOL,
        // Returns Collections.emptyEnumeration() — used for getResources()/
        // getSystemResources() on ClassLoader. Returning null here makes
        // SLF4J's ServiceLoader-based provider walk crash with "Cannot read
        // properties of null (reading '$hasMoreElements')" because the JDK's
        // ServiceLoader iterates the result without null-check.
        EMPTY_ENUMERATION,
        // Bypass SLF4J's broken bind() / ServiceLoader walk entirely:
        // org.slf4j.LoggerFactory.getLogger(String) and getLogger(Class)
        // get replaced with calls to StubHelpers.consoleLogger* which
        // return our ConsoleLogger directly. Saves us from null-Enumeration
        // crashes that even the EMPTY_ENUMERATION fix above cannot prevent
        // (ClassLoader.getResources looks intrinsic-ish on TeaVM and resists
        // the addMethod transform).
        CONSOLE_LOGGER_BY_NAME,
        CONSOLE_LOGGER_BY_CLASS,
    }

    private static final Map<String, StubMethod[]> STUBS = new HashMap<>();
    static {
        STUBS.put("java.lang.System", new StubMethod[] {
            new StubMethod("exit", "(I)V", true, ReturnKind.VOID),
            new StubMethod("getenv", "()Ljava/util/Map;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("loadLibrary", "(Ljava/lang/String;)V", true, ReturnKind.VOID),
            new StubMethod("load", "(Ljava/lang/String;)V", true, ReturnKind.VOID),
            new StubMethod("mapLibraryName", "(Ljava/lang/String;)Ljava/lang/String;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.lang.Package", new StubMethod[] {
            new StubMethod("getSpecificationVersion", "()Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.lang.Class", new StubMethod[] {
            new StubMethod("getGenericInterfaces", "()[Ljava/lang/reflect/Type;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getGenericSuperclass", "()Ljava/lang/reflect/Type;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getModule", "()Ljava/lang/Module;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getSigners", "()[Ljava/lang/Object;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getTypeParameters", "()[Ljava/lang/reflect/TypeVariable;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("isAnonymousClass", "()Z", false, ReturnKind.FALSE_BOOL),
        });
        STUBS.put("java.lang.ClassLoader", new StubMethod[] {
            new StubMethod("getResource", "(Ljava/lang/String;)Ljava/net/URL;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getResources", "(Ljava/lang/String;)Ljava/util/Enumeration;", false, ReturnKind.EMPTY_ENUMERATION),
            new StubMethod("getSystemResource", "(Ljava/lang/String;)Ljava/net/URL;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("getSystemResources", "(Ljava/lang/String;)Ljava/util/Enumeration;", true, ReturnKind.EMPTY_ENUMERATION),
            new StubMethod("loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.lang.Math", new StubMethod[] {
            new StubMethod("fma", "(FFF)F", true, ReturnKind.ZERO_FLOAT),
        });
        STUBS.put("java.lang.Integer", new StubMethod[] {
            new StubMethod("parseUnsignedInt", "(Ljava/lang/String;I)I", true, ReturnKind.ZERO_INT),
            new StubMethod("toUnsignedLong", "(I)J", true, ReturnKind.ZERO_LONG),
        });
        STUBS.put("java.lang.Long", new StubMethod[] {
            new StubMethod("parseUnsignedLong", "(Ljava/lang/String;I)J", true, ReturnKind.ZERO_LONG),
        });
        STUBS.put("java.lang.Character", new StubMethod[] {
            new StubMethod("codePointOf", "(Ljava/lang/String;)I", true, ReturnKind.ZERO_INT),
            new StubMethod("toString", "(I)Ljava/lang/String;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.lang.SecurityManager", new StubMethod[] {
            new StubMethod("getClassContext", "()[Ljava/lang/Class;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.lang.reflect.Field", new StubMethod[] {
            new StubMethod("getGenericType", "()Ljava/lang/reflect/Type;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.io.File", new StubMethod[] {
            new StubMethod("toPath", "()Ljava/nio/file/Path;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.io.RandomAccessFile", new StubMethod[] {
            new StubMethod("getChannel", "()Ljava/nio/channels/FileChannel;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.util.regex.Pattern", new StubMethod[] {
            new StubMethod("asPredicate", "()Ljava/util/function/Predicate;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.util.UUID", new StubMethod[] {
            new StubMethod("getMostSignificantBits", "()J", false, ReturnKind.ZERO_LONG),
            new StubMethod("nameUUIDFromBytes", "([B)Ljava/util/UUID;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.util.Date", new StubMethod[] {
            new StubMethod("from", "(Ljava/time/Instant;)Ljava/util/Date;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.util.stream.StreamSupport", new StubMethod[] {
            new StubMethod("intStream", "(Ljava/util/Spliterator$OfInt;Z)Ljava/util/stream/IntStream;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("longStream", "(Ljava/util/Spliterator$OfLong;Z)Ljava/util/stream/LongStream;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.util.concurrent.ConcurrentHashMap", new StubMethod[] {
            new StubMethod("newKeySet", "()Ljava/util/concurrent/ConcurrentHashMap$KeySetView;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.lang.Math", new StubMethod[] {
            new StubMethod("fma", "(FFF)F", true, ReturnKind.ZERO_FLOAT),
            new StubMethod("fma", "(DDD)D", true, ReturnKind.ZERO_LONG),  // double encoded as long for default
        });
        STUBS.put("java.lang.Integer", new StubMethod[] {
            new StubMethod("parseUnsignedInt", "(Ljava/lang/String;I)I", true, ReturnKind.ZERO_INT),
            new StubMethod("toUnsignedLong", "(I)J", true, ReturnKind.ZERO_LONG),
            new StubMethod("sum", "(II)I", true, ReturnKind.ZERO_INT),
        });
        STUBS.put("java.lang.Runtime", new StubMethod[] {
            new StubMethod("addShutdownHook", "(Ljava/lang/Thread;)V", false, ReturnKind.VOID),
            new StubMethod("removeShutdownHook", "(Ljava/lang/Thread;)Z", false, ReturnKind.FALSE_BOOL),
            new StubMethod("exec", "(Ljava/lang/String;)Ljava/lang/Process;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("exec", "([Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/Process;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("exec", "([Ljava/lang/String;)Ljava/lang/Process;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("maxMemory", "()J", false, ReturnKind.ZERO_LONG),
        });
        STUBS.put("java.lang.Thread", new StubMethod[] {
            new StubMethod("setContextClassLoader", "(Ljava/lang/ClassLoader;)V", false, ReturnKind.VOID),
            new StubMethod("getThreadGroup", "()Ljava/lang/ThreadGroup;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("<init>", "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;)V", false, ReturnKind.VOID),
            new StubMethod("<init>", "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;J)V", false, ReturnKind.VOID),
        });
        STUBS.put("java.lang.ref.Reference", new StubMethod[] {
            new StubMethod("<init>", "(Ljava/lang/Object;Ljava/lang/ref/ReferenceQueue;)V", false, ReturnKind.VOID),
        });
        STUBS.put("java.lang.reflect.Method", new StubMethod[] {
            new StubMethod("getExceptionTypes", "()[Ljava/lang/Class;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getParameterAnnotations", "()[[Ljava/lang/annotation/Annotation;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.io.File", new StubMethod[] {
            new StubMethod("toPath", "()Ljava/nio/file/Path;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("canExecute", "()Z", false, ReturnKind.FALSE_BOOL),
            new StubMethod("setReadable", "(ZZ)Z", false, ReturnKind.FALSE_BOOL),
        });
        STUBS.put("java.io.BufferedReader", new StubMethod[] {
            new StubMethod("transferTo", "(Ljava/io/Writer;)J", false, ReturnKind.ZERO_LONG),
        });
        STUBS.put("java.net.URL", new StubMethod[] {
            new StubMethod("openConnection", "(Ljava/net/Proxy;)Ljava/net/URLConnection;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.net.HttpURLConnection", new StubMethod[] {
            new StubMethod("getContentLengthLong", "()J", false, ReturnKind.ZERO_LONG),
        });
        STUBS.put("java.nio.file.Files", new StubMethod[] {
            new StubMethod("setLastModifiedTime", "(Ljava/nio/file/Path;Ljava/nio/file/attribute/FileTime;)Ljava/nio/file/Path;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("getPosixFilePermissions", "(Ljava/nio/file/Path;[Ljava/nio/file/LinkOption;)Ljava/util/Set;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("setPosixFilePermissions", "(Ljava/nio/file/Path;Ljava/util/Set;)Ljava/nio/file/Path;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("getFileStore", "(Ljava/nio/file/Path;)Ljava/nio/file/FileStore;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("getFileAttributeView", "(Ljava/nio/file/Path;Ljava/lang/Class;[Ljava/nio/file/LinkOption;)Ljava/nio/file/attribute/FileAttributeView;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("newByteChannel", "(Ljava/nio/file/Path;Ljava/util/Set;[Ljava/nio/file/attribute/FileAttribute;)Ljava/nio/channels/SeekableByteChannel;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("getAttribute", "(Ljava/nio/file/Path;Ljava/lang/String;[Ljava/nio/file/LinkOption;)Ljava/lang/Object;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("getOwner", "(Ljava/nio/file/Path;[Ljava/nio/file/LinkOption;)Ljava/nio/file/attribute/UserPrincipal;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.nio.file.FileSystem", new StubMethod[] {
            new StubMethod("getPathMatcher", "(Ljava/lang/String;)Ljava/nio/file/PathMatcher;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.util.Base64", new StubMethod[] {
            new StubMethod("getMimeDecoder", "()Ljava/util/Base64$Decoder;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("getMimeEncoder", "(I[B)Ljava/util/Base64$Encoder;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.util.concurrent.TimeUnit", new StubMethod[] {
            new StubMethod("convert", "(Ljava/time/Duration;)J", false, ReturnKind.ZERO_LONG),
        });
        STUBS.put("java.util.Locale", new StubMethod[] {
            new StubMethod("getISO3Country", "()Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getISO3Language", "()Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getDisplayLanguage", "()Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getDisplayCountry", "()Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getDisplayVariant", "()Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getExtensionKeys", "()Ljava/util/Set;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getExtension", "(C)Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("forLanguageTag", "(Ljava/lang/String;)Ljava/util/Locale;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("getScript", "()Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getUnicodeLocaleAttributes", "()Ljava/util/Set;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getUnicodeLocaleKeys", "()Ljava/util/Set;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getUnicodeLocaleType", "(Ljava/lang/String;)Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.util.UUID", new StubMethod[] {
            new StubMethod("<init>", "(JJ)V", false, ReturnKind.VOID),
            new StubMethod("getMostSignificantBits", "()J", false, ReturnKind.ZERO_LONG),
            new StubMethod("getLeastSignificantBits", "()J", false, ReturnKind.ZERO_LONG),
            new StubMethod("version", "()I", false, ReturnKind.ZERO_INT),
            new StubMethod("nameUUIDFromBytes", "([B)Ljava/util/UUID;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.lang.invoke.MethodHandle", new StubMethod[] {
            new StubMethod("invokeExact", "()Z", false, ReturnKind.FALSE_BOOL),
            new StubMethod("invoke", "(J)V", false, ReturnKind.VOID),
        });
        STUBS.put("java.lang.invoke.MethodHandles$Lookup", new StubMethod[] {
            new StubMethod("findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.lang.invoke.MethodHandles", new StubMethod[] {
            new StubMethod("lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.util.Spliterators", new StubMethod[] {
            new StubMethod("emptySpliterator", "()Ljava/util/Spliterator;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("iterator", "(Ljava/util/Spliterator;)Ljava/util/Iterator;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("iterator", "(Ljava/util/Spliterator$OfInt;)Ljava/util/PrimitiveIterator$OfInt;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.util.concurrent.ScheduledExecutorService", new StubMethod[] {
            new StubMethod("execute", "(Ljava/lang/Runnable;)V", false, ReturnKind.VOID),
            new StubMethod("shutdown", "()V", false, ReturnKind.VOID),
            new StubMethod("shutdownNow", "()Ljava/util/List;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("awaitTermination", "(JLjava/util/concurrent/TimeUnit;)Z", false, ReturnKind.FALSE_BOOL),
        });
        STUBS.put("java.util.concurrent.ScheduledThreadPoolExecutor", new StubMethod[] {
            new StubMethod("getQueue", "()Ljava/util/concurrent/BlockingQueue;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("io.netty.util.concurrent.EventExecutor", new StubMethod[] {
            new StubMethod("execute", "(Ljava/lang/Runnable;)V", false, ReturnKind.VOID),
            new StubMethod("isTerminated", "()Z", false, ReturnKind.FALSE_BOOL),
            new StubMethod("awaitTermination", "(JLjava/util/concurrent/TimeUnit;)Z", false, ReturnKind.FALSE_BOOL),
        });
        STUBS.put("io.netty.channel.EventLoop", new StubMethod[] {
            new StubMethod("execute", "(Ljava/lang/Runnable;)V", false, ReturnKind.VOID),
        });
        STUBS.put("java.lang.Long", new StubMethod[] {
            new StubMethod("parseUnsignedLong", "(Ljava/lang/String;I)J", true, ReturnKind.ZERO_LONG),
            new StubMethod("sum", "(JJ)J", true, ReturnKind.ZERO_LONG),
        });
        STUBS.put("java.lang.reflect.Array", new StubMethod[] {
            new StubMethod("newInstance", "(Ljava/lang/Class;[I)Ljava/lang/Object;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.util.regex.Matcher", new StubMethod[] {
            new StubMethod("group", "(Ljava/lang/String;)Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.util.Arrays", new StubMethod[] {
            new StubMethod("compare", "([Ljava/lang/Comparable;[Ljava/lang/Comparable;)I", true, ReturnKind.ZERO_INT),
        });
        STUBS.put("java.lang.Thread", new StubMethod[] {
            new StubMethod("setContextClassLoader", "(Ljava/lang/ClassLoader;)V", false, ReturnKind.VOID),
            new StubMethod("getThreadGroup", "()Ljava/lang/ThreadGroup;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("<init>", "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;)V", false, ReturnKind.VOID),
            new StubMethod("<init>", "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;J)V", false, ReturnKind.VOID),
            new StubMethod("getState", "()Ljava/lang/Thread$State;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.nio.channels.FileChannel", new StubMethod[] {
            new StubMethod("open", "(Ljava/nio/file/Path;[Ljava/nio/file/OpenOption;)Ljava/nio/channels/FileChannel;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.nio.channels.SelectionKey", new StubMethod[] {
            new StubMethod("channel", "()Ljava/nio/channels/SelectableChannel;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.nio.channels.spi.SelectorProvider", new StubMethod[] {
            new StubMethod("openSelector", "()Ljava/nio/channels/spi/AbstractSelector;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.nio.channels.Channels", new StubMethod[] {
            new StubMethod("newWriter", "(Ljava/nio/channels/WritableByteChannel;Ljava/nio/charset/Charset;)Ljava/io/Writer;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.nio.file.FileSystems", new StubMethod[] {
            new StubMethod("newFileSystem", "(Ljava/nio/file/Path;)Ljava/nio/file/FileSystem;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.lang.management.ManagementFactory", new StubMethod[] {
            new StubMethod("getThreadMXBean", "()Ljava/lang/management/ThreadMXBean;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.lang.invoke.MethodType", new StubMethod[] {
            new StubMethod("methodType", "(Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.lang.invoke.MethodHandles$Lookup", new StubMethod[] {
            new StubMethod("findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("findStaticGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.lang.Module", new StubMethod[] {
            new StubMethod("getLayer", "()Ljava/lang/ModuleLayer;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.util.Spliterators", new StubMethod[] {
            new StubMethod("emptySpliterator", "()Ljava/util/Spliterator;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("iterator", "(Ljava/util/Spliterator;)Ljava/util/Iterator;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("iterator", "(Ljava/util/Spliterator$OfInt;)Ljava/util/PrimitiveIterator$OfInt;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("spliterator", "([DIII)Ljava/util/Spliterator$OfDouble;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("io.netty.util.internal.logging.Log4J2Logger", new StubMethod[] {
            new StubMethod("log", "(Lorg/apache/logging/log4j/Level;Ljava/lang/String;Ljava/lang/Throwable;)V", false, ReturnKind.VOID),
        });
        STUBS.put("java.lang.Thread", new StubMethod[] {
            new StubMethod("setContextClassLoader", "(Ljava/lang/ClassLoader;)V", false, ReturnKind.VOID),
            new StubMethod("getThreadGroup", "()Ljava/lang/ThreadGroup;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("<init>", "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;)V", false, ReturnKind.VOID),
            new StubMethod("<init>", "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;J)V", false, ReturnKind.VOID),
            new StubMethod("getState", "()Ljava/lang/Thread$State;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("threadId", "()J", false, ReturnKind.ZERO_LONG),
            new StubMethod("sleep", "(Ljava/time/Duration;)V", true, ReturnKind.VOID),
        });
        STUBS.put("java.net.Inet6Address", new StubMethod[] {
            new StubMethod("getScopedInterface", "()Ljava/net/NetworkInterface;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getScopeId", "()I", false, ReturnKind.ZERO_INT),
        });
        STUBS.put("java.net.Socket", new StubMethod[] {
            new StubMethod("connect", "(Ljava/net/SocketAddress;I)V", false, ReturnKind.VOID),
            new StubMethod("getKeepAlive", "()Z", false, ReturnKind.FALSE_BOOL),
            new StubMethod("setKeepAlive", "(Z)V", false, ReturnKind.VOID),
            new StubMethod("getReceiveBufferSize", "()I", false, ReturnKind.ZERO_INT),
            new StubMethod("setReceiveBufferSize", "(I)V", false, ReturnKind.VOID),
            new StubMethod("getReuseAddress", "()Z", false, ReturnKind.FALSE_BOOL),
            new StubMethod("setReuseAddress", "(Z)V", false, ReturnKind.VOID),
            new StubMethod("getSendBufferSize", "()I", false, ReturnKind.ZERO_INT),
            new StubMethod("setSendBufferSize", "(I)V", false, ReturnKind.VOID),
            new StubMethod("getSoLinger", "()I", false, ReturnKind.ZERO_INT),
            new StubMethod("setSoLinger", "(ZI)V", false, ReturnKind.VOID),
            new StubMethod("getTcpNoDelay", "()Z", false, ReturnKind.FALSE_BOOL),
            new StubMethod("setTcpNoDelay", "(Z)V", false, ReturnKind.VOID),
            new StubMethod("getTrafficClass", "()I", false, ReturnKind.ZERO_INT),
            new StubMethod("setTrafficClass", "(I)V", false, ReturnKind.VOID),
        });
        STUBS.put("java.nio.file.FileSystem", new StubMethod[] {
            new StubMethod("getPathMatcher", "(Ljava/lang/String;)Ljava/nio/file/PathMatcher;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("newWatchService", "()Ljava/nio/file/WatchService;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.time.Duration", new StubMethod[] {
            new StubMethod("toSeconds", "()J", false, ReturnKind.ZERO_LONG),
        });
        STUBS.put("java.util.zip.Inflater", new StubMethod[] {
            new StubMethod("inflate", "(Ljava/nio/ByteBuffer;)I", false, ReturnKind.ZERO_INT),
            new StubMethod("setInput", "(Ljava/nio/ByteBuffer;)V", false, ReturnKind.VOID),
        });
        STUBS.put("java.lang.invoke.MethodType", new StubMethod[] {
            new StubMethod("methodType", "(Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("methodType", "(Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", true, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.lang.invoke.MethodHandle", new StubMethod[] {
            new StubMethod("invokeExact", "()I", false, ReturnKind.ZERO_INT),
            new StubMethod("invokeExact", "()Z", false, ReturnKind.FALSE_BOOL),
            new StubMethod("invoke", "(J)V", false, ReturnKind.VOID),
            new StubMethod("invokeExact", "(Ljava/util/zip/Checksum;Ljava/nio/ByteBuffer;)V", false, ReturnKind.VOID),
            new StubMethod("invokeExact", "()Ljava/util/zip/Checksum;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("java.lang.String", new StubMethod[] {
            new StubMethod("<init>", "([BIII)V", false, ReturnKind.VOID),
        });
        STUBS.put("java.nio.channels.SocketChannel", new StubMethod[] {
            new StubMethod("bind", "(Ljava/net/SocketAddress;)Ljava/nio/channels/SocketChannel;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("javax.crypto.Cipher", new StubMethod[] {
            new StubMethod("getOutputSize", "(I)I", false, ReturnKind.ZERO_INT),
            new StubMethod("update", "([BII[B)I", false, ReturnKind.ZERO_INT),
        });
        STUBS.put("java.nio.file.Path", new StubMethod[] {
            new StubMethod("register", "(Ljava/nio/file/WatchService;[Ljava/nio/file/WatchEvent$Kind;)Ljava/nio/file/WatchKey;", false, ReturnKind.NULL_OBJECT),
        });
        // ConcurrentHashMap — also try TeaVM's renamed class name
        StubMethod[] chmMethods = {
            new StubMethod("<init>", "(IFI)V", false, ReturnKind.VOID),
            new StubMethod("newKeySet", "()Ljava/util/concurrent/ConcurrentHashMap$KeySetView;", true, ReturnKind.NULL_OBJECT),
        };
        STUBS.put("java.util.concurrent.ConcurrentHashMap", chmMethods);
        STUBS.put("org.teavm.classlib.java.util.concurrent.TConcurrentHashMap", chmMethods);
        // Replace netty Recycler.LocalPool.isTerminated to avoid its
        // Thread.State.TERMINATED reference (TeaVM has no Thread$State).
        STUBS.put("io.netty.util.Recycler$LocalPool", new StubMethod[] {
            new StubMethod("isTerminated", "()Z", false, ReturnKind.FALSE_BOOL),
        });
        // Short-circuit SLF4J: bypass bind()/ServiceLoader walk that crashes
        // on TeaVM (null Enumeration from ClassLoader.getResources). Both
        // overloads of LoggerFactory.getLogger return our ConsoleLogger.
        StubMethod[] sl4fLoggerFactory = {
            new StubMethod("getLogger", "(Ljava/lang/String;)Lorg/slf4j/Logger;", true, ReturnKind.CONSOLE_LOGGER_BY_NAME),
            new StubMethod("getLogger", "(Ljava/lang/Class;)Lorg/slf4j/Logger;", true, ReturnKind.CONSOLE_LOGGER_BY_CLASS),
        };
        STUBS.put("org.slf4j.LoggerFactory", sl4fLoggerFactory);
        // java.security.cert — X509Certificate stub for HTTPS/SSL in browser
        STUBS.put("java.security.cert.X509Certificate", new StubMethod[] {
            new StubMethod("getSubjectX500Principal", "()Ljavax/security/auth/x500/X500Principal;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getIssuerX500Principal", "()Ljavax/security/auth/x500/X500Principal;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getSubjectDN", "()Ljava/security/Principal;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getIssuerDN", "()Ljava/security/Principal;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getNotBefore", "()Ljava/util/Date;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getNotAfter", "()Ljava/util/Date;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getSubjectAlternativeNames", "()Ljava/util/Collection;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getIssuerAlternativeNames", "()Ljava/util/Collection;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getExtensionValue", "(Ljava/lang/String;)[B", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("javax.security.auth.x500.X500Principal", new StubMethod[] {
            new StubMethod("<init>", "(Ljava/lang/String;)V", false, ReturnKind.VOID),
            new StubMethod("getName", "()Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getName", "(Ljava/lang/String;)Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
        });
        // java.text.SimpleDateFormat — needed by Apache HttpClient DateUtils
        STUBS.put("java.text.SimpleDateFormat", new StubMethod[] {
            new StubMethod("<init>", "(Ljava/lang/String;)V", false, ReturnKind.VOID),
            new StubMethod("<init>", "(Ljava/lang/String;Ljava/util/Locale;)V", false, ReturnKind.VOID),
            new StubMethod("set2DigitYearStart", "(Ljava/util/Date;)V", false, ReturnKind.VOID),
            new StubMethod("get2DigitYearStart", "()Ljava/util/Date;", false, ReturnKind.NULL_OBJECT),
        });
        // java.lang.Class — all reflection methods merged
        STUBS.put("java.lang.Class", new StubMethod[] {
            new StubMethod("getGenericInterfaces", "()[Ljava/lang/reflect/Type;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getGenericSuperclass", "()Ljava/lang/reflect/Type;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getModule", "()Ljava/lang/Module;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getSigners", "()[Ljava/lang/Object;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getTypeParameters", "()[Ljava/lang/reflect/TypeVariable;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("isAnonymousClass", "()Z", false, ReturnKind.FALSE_BOOL),
            new StubMethod("getResource", "(Ljava/lang/String;)Ljava/net/URL;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getProtectionDomain", "()Ljava/security/ProtectionDomain;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getDeclaredClasses", "()[Ljava/lang/Class;", false, ReturnKind.NULL_OBJECT),
        });
        // java.security.Principal — interface stub for certificate principals
        STUBS.put("java.security.Principal", new StubMethod[] {
            new StubMethod("getName", "()Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
        });
        // org.apache.logging.log4j — needed by Netty logging
        STUBS.put("org.apache.logging.log4j.LogManager", new StubMethod[] {
            new StubMethod("<init>", "()V", false, ReturnKind.VOID),
            new StubMethod("getLogger", "(Ljava/lang/String;)Lorg/apache/logging/log4j/Logger;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("org.apache.logging.log4j.Logger", new StubMethod[] {
            new StubMethod("info", "(Ljava/lang/String;)V", false, ReturnKind.VOID),
            new StubMethod("warn", "(Ljava/lang/String;)V", false, ReturnKind.VOID),
            new StubMethod("debug", "(Ljava/lang/String;)V", false, ReturnKind.VOID),
            new StubMethod("error", "(Ljava/lang/String;)V", false, ReturnKind.VOID),
            new StubMethod("trace", "(Ljava/lang/String;)V", false, ReturnKind.VOID),
        });
        STUBS.put("org.apache.logging.log4j.Level", new StubMethod[] {
            new StubMethod("<init>", "(Ljava/lang/String;)V", false, ReturnKind.VOID),
        });
        // org.apache.log4j — older logging (used by some netty bridges)
        STUBS.put("org.apache.log4j.Logger", new StubMethod[] {
            new StubMethod("getLogger", "(Ljava/lang/String;)Lorg/apache/log4j/Logger;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("info", "(Ljava/lang/Object;)V", false, ReturnKind.VOID),
            new StubMethod("warn", "(Ljava/lang/Object;)V", false, ReturnKind.VOID),
            new StubMethod("debug", "(Ljava/lang/Object;)V", false, ReturnKind.VOID),
            new StubMethod("error", "(Ljava/lang/Object;)V", false, ReturnKind.VOID),
        });
        STUBS.put("org.apache.log4j.Level", new StubMethod[] {
            new StubMethod("<init>", "(Ljava/lang/String;)V", false, ReturnKind.VOID),
        });
        STUBS.put("org.apache.log4j.Priority", new StubMethod[] {
            new StubMethod("<init>", "(Ljava/lang/String;)V", false, ReturnKind.VOID),
        });
        // javax.naming.ldap — needed by Apache HttpClient SSL hostname verification
        STUBS.put("javax.naming.ldap.LdapName", new StubMethod[] {
            new StubMethod("<init>", "(Ljava/lang/String;)V", false, ReturnKind.VOID),
            new StubMethod("getComponents", "()Ljava/util/List;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("size", "()I", false, ReturnKind.ZERO_INT),
            new StubMethod("toString", "()Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getRdns", "()Ljava/util/List;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("compareTo", "(Ljava/lang/Object;)I", false, ReturnKind.ZERO_INT),
            new StubMethod("getParserClassName", "()Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("javax.naming.ldap.Rdn", new StubMethod[] {
            new StubMethod("<init>", "(Ljava/lang/String;)V", false, ReturnKind.VOID),
            new StubMethod("<init>", "()V", false, ReturnKind.VOID),
            new StubMethod("getType", "()Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getValue", "()Ljava/lang/Object;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("toString", "()Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("getRdns", "()Ljava/util/List;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("compareTo", "(Ljava/lang/Object;)I", false, ReturnKind.ZERO_INT),
            new StubMethod("toAttributes", "()Ljavax/naming/directory/Attributes;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("javax.naming.directory.Attribute", new StubMethod[] {
            new StubMethod("get", "()Ljava/lang/Object;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("get", "(I)Ljava/lang/Object;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("size", "()I", false, ReturnKind.ZERO_INT),
            new StubMethod("getAll", "()Ljava/util/Enumeration;", false, ReturnKind.NULL_OBJECT),
        });
        STUBS.put("javax.naming.directory.BasicAttribute", new StubMethod[] {
            new StubMethod("<init>", "(Ljava/lang/String;)V", false, ReturnKind.VOID),
            new StubMethod("<init>", "(Ljava/lang/String;Ljava/lang/Object;)V", false, ReturnKind.VOID),
        });
        // Thread.dumpStack
        STUBS.put("java.lang.Thread", new StubMethod[] {
            new StubMethod("dumpStack", "()V", false, ReturnKind.VOID),
            new StubMethod("setContextClassLoader", "(Ljava/lang/ClassLoader;)V", false, ReturnKind.VOID),
            new StubMethod("getThreadGroup", "()Ljava/lang/ThreadGroup;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("<init>", "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;)V", false, ReturnKind.VOID),
            new StubMethod("<init>", "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;J)V", false, ReturnKind.VOID),
            new StubMethod("getState", "()Ljava/lang/Thread$State;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("threadId", "()J", false, ReturnKind.ZERO_LONG),
            new StubMethod("sleep", "(Ljava/time/Duration;)V", true, ReturnKind.VOID),
        });
        // FileInputStream.getChannel
        STUBS.put("java.io.FileInputStream", new StubMethod[] {
            new StubMethod("getChannel", "()Ljava/nio/channels/FileChannel;", false, ReturnKind.NULL_OBJECT),
        });
        // FileOutputStream.getChannel
        STUBS.put("java.io.FileOutputStream", new StubMethod[] {
            new StubMethod("getChannel", "()Ljava/nio/channels/FileChannel;", false, ReturnKind.NULL_OBJECT),
        });
        // Collections.unmodifiableSortedSet
        STUBS.put("java.util.Collections", new StubMethod[] {
            new StubMethod("unmodifiableSortedSet", "(Ljava/util/SortedSet;)Ljava/util/SortedSet;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("unmodifiableSortedMap", "(Ljava/util/SortedMap;)Ljava/util/SortedMap;", true, ReturnKind.NULL_OBJECT),
            new StubMethod("unmodifiableSequencedSet", "(Ljava/util/SequencedSet;)Ljava/util/SequencedSet;", true, ReturnKind.NULL_OBJECT),
        });
        // SSLSocketFactory.getDefault (inherited from SocketFactory, returns SocketFactory)
        STUBS.put("javax.net.ssl.SSLSocketFactory", new StubMethod[] {
            new StubMethod("getDefault", "()Ljavax/net/SocketFactory;", true, ReturnKind.NULL_OBJECT),
        });
        // MarkerManager.getMarker
        STUBS.put("org.apache.logging.log4j.MarkerManager", new StubMethod[] {
            new StubMethod("getMarker", "(Ljava/lang/String;)Lorg/apache/logging/log4j/Marker;", true, ReturnKind.NULL_OBJECT),
        });
        // LoggerAdapter.getLogger
        STUBS.put("org.apache.logging.log4j.spi.LoggerAdapter", new StubMethod[] {
            new StubMethod("getLogger", "(Ljava/lang/String;)Ljava/lang/Object;", false, ReturnKind.NULL_OBJECT),
        });
        // ReentrantReadWriteLock
        STUBS.put("java.util.concurrent.locks.ReentrantReadWriteLock", new StubMethod[] {
            new StubMethod("getReadLockCount", "()I", false, ReturnKind.ZERO_INT),
            new StubMethod("isWriteLocked", "()Z", false, ReturnKind.FALSE_BOOL),
        });
        // StringCharacterIterator
        STUBS.put("java.text.StringCharacterIterator", new StubMethod[] {
            new StubMethod("<init>", "(Ljava/lang/String;)V", false, ReturnKind.VOID),
        });
        // PosixFileAttributes — owner and group
        STUBS.put("java.nio.file.attribute.PosixFileAttributes", new StubMethod[] {
            new StubMethod("owner", "()Ljava/nio/file/attribute/UserPrincipal;", false, ReturnKind.NULL_OBJECT),
            new StubMethod("group", "()Ljava/nio/file/attribute/GroupPrincipal;", false, ReturnKind.NULL_OBJECT),
        });
        // UserPrincipal — needed by Apache Commons Compress for tar file handling
        STUBS.put("java.nio.file.attribute.UserPrincipal", new StubMethod[] {
            new StubMethod("getName", "()Ljava/lang/String;", false, ReturnKind.NULL_OBJECT),
        });
    }

    /** Static fields to inject (className → field names). For Thread$State.TERMINATED:
     *  TeaVM's TThread doesn't have a State inner enum, so we need to add the missing
     *  enum constant to whatever class TeaVM resolves Thread$State to. The reference
     *  comes from netty's Recycler.LocalPool.isTerminated. */
    private static final Map<String, String[]> STATIC_FIELDS = new HashMap<>();
    static {
        // TeaVM's Thread class has Thread.State as inner enum
        STATIC_FIELDS.put("java.lang.Thread$State", new String[] {
            "NEW", "RUNNABLE", "BLOCKED", "WAITING", "TIMED_WAITING", "TERMINATED"
        });
        // Our standalone ThreadState stub class (different name)
        STATIC_FIELDS.put("java.lang.ThreadState", new String[] {
            "NEW", "RUNNABLE", "BLOCKED", "WAITING", "TIMED_WAITING", "TERMINATED"
        });
    }

    /** Classes whose `native` methods should be replaced with empty bodies.
     *  These are platform-specific native bindings (Tracy profiler, Cocoa,
     *  JNA, netty epoll, Linux text-to-speech) that have no JS analog and
     *  shouldn't be reached at runtime in browser anyway. */
    private static final java.util.Set<String> STRIP_NATIVES = new java.util.HashSet<>(java.util.Arrays.asList(
        "ca.weblite.objc.RuntimeUtils",
        "com.mojang.jtracy.TracyBindings",
        "com.mojang.text2speech.NarratorLinux$FliteLibrary",
        "com.mojang.text2speech.NarratorLinux$FliteLibrary$CmuUsKal16",
        "com.sun.jna.Native",
        "io.netty.channel.epoll.LinuxSocket",
        "io.netty.channel.epoll.Native",
        "io.netty.channel.epoll.NativeStaticallyReferencedJniMethods",
        "io.netty.channel.unix.Buffer",
        "io.netty.channel.unix.ErrorsStaticallyReferencedJniMethods",
        "io.netty.channel.unix.FileDescriptor",
        "io.netty.channel.unix.LimitsStaticallyReferencedJniMethods",
        "io.netty.channel.unix.Socket"
    ));

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals("org.teavm.classlib.java.nio.file.impl.TDefaultFileSystem")) {
            fixDefaultFileSystem(cls, context);
        } else if (cls.getName().equals("top.steve3184.webmc.vfs.WebFs")) {
            fixWebFs(cls, context);
        }

        // Replace native method bodies with no-op stubs for known JNI-only classes
        if (STRIP_NATIVES.contains(cls.getName())) {
            for (MethodHolder mh : cls.getMethods()) {
                if (!mh.getModifiers().contains(ElementModifier.NATIVE)) continue;
                mh.getModifiers().remove(ElementModifier.NATIVE);
                ProgramEmitter pe = ProgramEmitter.create(mh, context.getHierarchy());
                ValueType ret = mh.getResultType();
                if (ret == ValueType.VOID) {
                    pe.exit();
                } else if (ret instanceof ValueType.Primitive) {
                    switch (((ValueType.Primitive) ret).getKind()) {
                        case LONG:
                            pe.constant(0L).returnValue();
                            break;
                        case FLOAT:
                            pe.constant(0F).returnValue();
                            break;
                        case DOUBLE:
                            pe.constant(0D).returnValue();
                            break;
                        default:
                            pe.constant(0).returnValue();
                    }
                } else {
                    pe.constantNull(ret).returnValue();
                }
            }
        }

        // Add missing static fields (e.g. Thread$State.TERMINATED on TeaVM's Thread.State)
        String[] fields = STATIC_FIELDS.get(cls.getName());
        if (fields != null) {
            ValueType selfType = ValueType.object(cls.getName());
            for (String fieldName : fields) {
                if (cls.getField(fieldName) != null) continue;
                FieldHolder fh = new FieldHolder(fieldName);
                fh.setLevel(AccessLevel.PUBLIC);
                fh.getModifiers().add(ElementModifier.STATIC);
                fh.getModifiers().add(ElementModifier.FINAL);
                fh.setType(selfType);
                cls.addField(fh);
            }
        }

        StubMethod[] methods = STUBS.get(cls.getName());
        if (methods == null) return;
        for (StubMethod sm : methods) {
            MethodDescriptor desc = MethodDescriptor.parse(sm.name + sm.descriptor);
            MethodHolder existing = cls.getMethod(desc);
            MethodHolder mh;
            if (existing != null) {
                // Method already exists — REPLACE its body with our stub. This
                // is used to patch out problematic methods in classes we don't
                // own (like netty's Recycler$LocalPool.isTerminated which
                // references Thread.State.TERMINATED that TeaVM can't resolve).
                mh = existing;
                mh.setProgram(null);  // clear previous program; ProgramEmitter creates a new one below
            } else {
                mh = new MethodHolder(desc);
                mh.setLevel(AccessLevel.PUBLIC);
                if (sm.isStatic) {
                    mh.getModifiers().add(ElementModifier.STATIC);
                }
            }
            ProgramEmitter pe = ProgramEmitter.create(mh, context.getHierarchy());
            switch (sm.ret) {
                case VOID:
                    pe.exit();
                    break;
                case NULL_OBJECT:
                    pe.constantNull(extractReturnType(sm.descriptor)).returnValue();
                    break;
                case ZERO_INT:
                case FALSE_BOOL:
                    pe.constant(0).returnValue();
                    break;
                case ZERO_LONG:
                    pe.constant(0L).returnValue();
                    break;
                case ZERO_FLOAT:
                    pe.constant(0F).returnValue();
                    break;
                case EMPTY_ENUMERATION:
                    pe.invoke("top.steve3184.webmc.teavm.stubs.StubHelpers", "emptyEnumeration",
                            ValueType.parse("Ljava/util/Enumeration;")).returnValue();
                    break;
                case CONSOLE_LOGGER_BY_NAME:
                case CONSOLE_LOGGER_BY_CLASS:
                    pe.invoke("top.steve3184.webmc.teavm.stubs.StubHelpers", "consoleLogger",
                            ValueType.parse("Lorg/slf4j/Logger;")).returnValue();
                    break;
                default:
                    pe.exit();
            }
            if (existing == null) cls.addMethod(mh);
        }
    }

    private void fixDefaultFileSystem(ClassHolder cls, ClassHolderTransformerContext context) {
        FieldHolder vfsField = cls.getField("vfs");
        if (vfsField != null) {
            vfsField.getModifiers().remove(ElementModifier.FINAL);
        }

        // Overwrite constructor to avoid early VFS capture
        MethodHolder ctor = cls.getMethod(new MethodDescriptor("<init>", ValueType.VOID));
        if (ctor != null) {
            ProgramEmitter pe = ProgramEmitter.create(ctor, context.getHierarchy());
            pe.var(0, ValueType.object(cls.getName()))
              .invoke(InvocationType.SPECIAL, cls.getParent(), "<init>", ValueType.VOID);
            pe.exit();
        }

        // Add static setter: public static void setVfsInstance(VirtualFileSystem v)
        MethodHolder setter = new MethodHolder("setVfsInstance",
                ValueType.object("org.teavm.runtime.fs.VirtualFileSystem"), ValueType.VOID);
        setter.setLevel(AccessLevel.PUBLIC);
        setter.getModifiers().add(ElementModifier.STATIC);
        
        ProgramEmitter pe = ProgramEmitter.create(setter, context.getHierarchy());
        // Read static TDefaultFileSystem.INSTANCE
        pe.getField(new FieldReference(cls.getName(), "INSTANCE"), ValueType.object(cls.getName()))
          // Set instance field 'vfs' on that instance
          .setField("vfs", pe.var(1, ValueType.object("org.teavm.runtime.fs.VirtualFileSystem")));
        pe.exit();
        cls.addMethod(setter);
    }

    private void fixWebFs(ClassHolder cls, ClassHolderTransformerContext context) {
        // Find updateVfsInstance method and provide body
        MethodHolder mh = cls.getMethod(new MethodDescriptor("updateVfsInstance", 
                ValueType.object("java.lang.Object"), ValueType.VOID));
        if (mh != null) {
            ProgramEmitter pe = ProgramEmitter.create(mh, context.getHierarchy());
            pe.invoke("org.teavm.classlib.java.nio.file.impl.TDefaultFileSystem", "setVfsInstance",
                    ValueType.VOID, pe.var(1, ValueType.object("java.lang.Object"))
                    .cast(ValueType.object("org.teavm.runtime.fs.VirtualFileSystem")));
            pe.exit();
        }
    }

    private static ValueType extractReturnType(String desc) {
        int closeParen = desc.indexOf(')');
        if (closeParen < 0) return ValueType.object("java.lang.Object");
        return ValueType.parse(desc.substring(closeParen + 1));
    }
}
