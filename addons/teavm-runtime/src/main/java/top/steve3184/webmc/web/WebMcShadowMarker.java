package top.steve3184.webmc.web;

/** Sentinel class — referenced from our shadow LogUtils to verify the
 *  shadow won the TeaVM classpath race. If TeaVM uses the JAR's LogUtils,
 *  this class is unreachable. If it uses ours, you'll see it in the
 *  reachability graph (no missing-method errors should reference it).
 */
public final class WebMcShadowMarker {
    private WebMcShadowMarker() {}
    public static void touch() {}
}
