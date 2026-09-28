package arc.util;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;

/**
 * Temporary scratch objects.
 *
 * <p>Multi-context runtime code must use the accessor methods ({@code v1()}, {@code r1()}, ...), which are
 * thread-local and therefore safe for coarse-grained parallel GameContext execution. The legacy public fields are
 * retained for source/binary compatibility with existing Arc/Mindustry code and third-party mods, but remain
 * process-global and are not safe for parallel runtime work.</p>
 */
public class Tmp{
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Vec2 v1 = new Vec2();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Vec2 v2 = new Vec2();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Vec2 v3 = new Vec2();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Vec2 v4 = new Vec2();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Vec2 v5 = new Vec2();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Vec2 v6 = new Vec2();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Vec3 v31 = new Vec3();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Vec3 v32 = new Vec3();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Vec3 v33 = new Vec3();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Vec3 v34 = new Vec3();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Rect r1 = new Rect();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Rect r2 = new Rect();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Rect r3 = new Rect();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Circle cr1 = new Circle();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Circle cr2 = new Circle();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Circle cr3 = new Circle();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Vec2 t1 = new Vec2();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Color c1 = new Color();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Color c2 = new Color();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Color c3 = new Color();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Color c4 = new Color();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Point2 p1 = new Point2();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Point2 p2 = new Point2();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Point2 p3 = new Point2();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final TextureRegion tr1 = new TextureRegion();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final TextureRegion tr2 = new TextureRegion();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Mat m1 = new Mat();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Mat m2 = new Mat();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Mat m3 = new Mat();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Mat m4 = new Mat();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Bezier<Vec2> bz2 = new Bezier<>();
    /** @deprecated Process-global compatibility scratch. Runtime code should use the thread-local accessor instead. */
    @Deprecated
    public static final Bezier<Vec3> bz3 = new Bezier<>();

    private static final ThreadLocal<State> local = ThreadLocal.withInitial(State::new);

    public static Vec2 v1(){ return local.get().v1; }
    public static Vec2 v2(){ return local.get().v2; }
    public static Vec2 v3(){ return local.get().v3; }
    public static Vec2 v4(){ return local.get().v4; }
    public static Vec2 v5(){ return local.get().v5; }
    public static Vec2 v6(){ return local.get().v6; }
    public static Vec2 v7(){ return local.get().v7; }
    public static Vec3 v31(){ return local.get().v31; }
    public static Vec3 v32(){ return local.get().v32; }
    public static Vec3 v33(){ return local.get().v33; }
    public static Vec3 v34(){ return local.get().v34; }
    public static Rect r1(){ return local.get().r1; }
    public static Rect r2(){ return local.get().r2; }
    public static Rect r3(){ return local.get().r3; }
    public static Circle cr1(){ return local.get().cr1; }
    public static Circle cr2(){ return local.get().cr2; }
    public static Circle cr3(){ return local.get().cr3; }
    public static Vec2 t1(){ return local.get().t1; }
    public static Color c1(){ return local.get().c1; }
    public static Color c2(){ return local.get().c2; }
    public static Color c3(){ return local.get().c3; }
    public static Color c4(){ return local.get().c4; }
    public static Point2 p1(){ return local.get().p1; }
    public static Point2 p2(){ return local.get().p2; }
    public static Point2 p3(){ return local.get().p3; }
    public static TextureRegion tr1(){ return local.get().tr1; }
    public static TextureRegion tr2(){ return local.get().tr2; }
    public static Mat m1(){ return local.get().m1; }
    public static Mat m2(){ return local.get().m2; }
    public static Mat m3(){ return local.get().m3; }
    public static Mat m4(){ return local.get().m4; }
    public static Bezier<Vec2> bz2(){ return local.get().bz2; }
    public static Bezier<Vec3> bz3(){ return local.get().bz3; }

    /** Drops this thread's scratch state after a long-lived runtime worker is retired. */
    public static void clearThreadLocal(){ local.remove(); }

    private static final class State{
        final Vec2 v1 = new Vec2();
        final Vec2 v2 = new Vec2();
        final Vec2 v3 = new Vec2();
        final Vec2 v4 = new Vec2();
        final Vec2 v5 = new Vec2();
        final Vec2 v6 = new Vec2();
        final Vec2 v7 = new Vec2();
        final Vec3 v31 = new Vec3();
        final Vec3 v32 = new Vec3();
        final Vec3 v33 = new Vec3();
        final Vec3 v34 = new Vec3();
        final Rect r1 = new Rect();
        final Rect r2 = new Rect();
        final Rect r3 = new Rect();
        final Circle cr1 = new Circle();
        final Circle cr2 = new Circle();
        final Circle cr3 = new Circle();
        final Vec2 t1 = new Vec2();
        final Color c1 = new Color();
        final Color c2 = new Color();
        final Color c3 = new Color();
        final Color c4 = new Color();
        final Point2 p1 = new Point2();
        final Point2 p2 = new Point2();
        final Point2 p3 = new Point2();
        final TextureRegion tr1 = new TextureRegion();
        final TextureRegion tr2 = new TextureRegion();
        final Mat m1 = new Mat();
        final Mat m2 = new Mat();
        final Mat m3 = new Mat();
        final Mat m4 = new Mat();
        final Bezier<Vec2> bz2 = new Bezier<>();
        final Bezier<Vec3> bz3 = new Bezier<>();
    }
}
