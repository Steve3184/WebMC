package top.steve3184.webmc.teavm.stubs;

/** Static helpers invoked by JdkMethodStubsTransformer's emitted method
 *  bodies — emitting `Collections.emptyEnumeration()` directly via
 *  ProgramEmitter is awkward, so we route through these helpers. */
public final class StubHelpers {
    private StubHelpers() {}

    public static java.util.Enumeration<Object> emptyEnumeration() {
        return java.util.Collections.emptyEnumeration();
    }

    /** Returns a console-backed SLF4J logger. SLF4J's bind() walks
     *  ServiceLoader for providers and crashes on TeaVM (null Enumeration);
     *  the transformer replaces LoggerFactory.getLogger() bodies with
     *  invocations of this helper to short-circuit that walk. The logger
     *  name is fixed to "slf4j" since TeaVM's ProgramEmitter's parameter
     *  forwarding via var(0,...) was producing broken JS (var$0 undefined). */
    public static org.slf4j.Logger consoleLogger() {
        return new com.mojang.logging.ConsoleLogger("slf4j");
    }
}
