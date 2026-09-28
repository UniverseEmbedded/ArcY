package arc.util;

import arc.*;
import arc.func.*;
import arc.struct.*;
import arc.util.Timer.*;
import arc.util.pooling.Pool.*;
import arc.util.pooling.*;

import java.util.*;

/**
 * Time facade whose mutable simulation clock can be bound per runtime.
 * System wall-clock helpers remain process-wide; simulation delta/time/delayed runs live in {@link Context}.
 */
public class Time{
    /** Conversion factors for ticks to other unit values. */
    public static final float toSeconds = 60f, toMinutes = 60f * 60f, toHours = 60f * 60f * 60f;
    public static final long nanosPerMilli = 1000000;

    private static final Context global = new Context();
    private static final ThreadLocal<Context> current = new ThreadLocal<>();
    private static volatile Prov<Context> defaultContextProvider;

    public static Context global(){
        return global;
    }

    public static Context current(){
        Context value = current.get();
        if(value != null) return value;
        Prov<Context> provider = defaultContextProvider;
        if(provider != null){
            Context provided = provider.get();
            if(provided != null) return provided;
        }
        return global;
    }

    /** Installs an application-owned resolver for the implicit simulation-time context. */
    public static void setDefaultContextProvider(Prov<Context> provider){
        defaultContextProvider = provider;
    }

    /** Binds a simulation clock to this thread until the returned scope is closed. */
    public static Scope enter(Context context){
        Objects.requireNonNull(context);
        Context previous = current.get();
        current.set(context);
        return new Scope(previous);
    }

    public static float delta(){ return current().delta; }
    public static void setDelta(float delta){ current().delta = delta; }
    public static float time(){ return current().time; }
    public static void setTime(float time){ current().time = time; current().timeRaw = time; }
    public static float globalTime(){ return current().globalTime; }

    public static Seq<DelayRun> getRuns(){
        return current().runs;
    }

    public static void setRuns(Seq<DelayRun> runs){
        current().runs = Objects.requireNonNull(runs);
    }

    /** Runs a task with a delay of several ticks in the currently-bound simulation runtime. */
    public static void run(float delay, Runnable r){
        DelayRun run = Pools.obtain(DelayRun.class, DelayRun::new);
        run.finish = r;
        run.delay = delay;
        current().runs.add(run);
    }

    /**
     * Runs a wall-clock application task. This remains process-level by design; game-runtime code should prefer
     * {@link #run(float, Runnable)} or its GameContext scheduler so ownership is explicit.
     */
    public static Task runTask(float delay, Runnable r){
        return Timer.schedule(r, delay / 60f);
    }

    public static void mark(){
        current().marks.add(nanos());
    }

    /** A value of -1 means mark() wasn't called beforehand. */
    public static float elapsed(){
        LongSeq marks = current().marks;
        if(marks.size == 0){
            return -1;
        }else{
            return timeSinceNanos(marks.pop()) / 1000000f;
        }
    }

    public static void updateGlobal(){
        Context context = current();
        context.delta = context.deltaimpl.get();
        context.globalTimeRaw += context.delta;

        if(Double.isInfinite(context.timeRaw) || Double.isNaN(context.timeRaw)) context.timeRaw = 0;

        context.time = (float)context.timeRaw;
        context.globalTime = (float)context.globalTimeRaw;
    }

    public static void update(){
        Context context = current();
        context.timeRaw += context.delta;
        context.removal.clear();

        if(Double.isInfinite(context.timeRaw) || Double.isNaN(context.timeRaw)) context.timeRaw = 0;

        context.time = (float)context.timeRaw;
        context.globalTime = (float)context.globalTimeRaw;

        for(DelayRun run : context.runs){
            run.delay -= context.delta;
            if(run.delay <= 0){
                run.finish.run();
                context.removal.add(run);
                Pools.free(run);
            }
        }
        context.runs.removeAll(context.removal);
    }

    public static double getInternalTime(){
        return current().timeRaw;
    }

    public static void setInternalTime(double timeInternal){
        Context context = current();
        context.timeRaw = timeInternal;
        context.time = (float)timeInternal;
    }

    public static void clear(){
        current().runs.clear();
    }

    /** Returns the delta provider owned by the currently-bound simulation runtime. */
    public static Floatp deltaProvider(){
        return current().deltaimpl;
    }

    public static void setDeltaProvider(Floatp impl){
        Context context = current();
        context.deltaimpl = Objects.requireNonNull(impl);
        context.delta = impl.get();
    }

    /** @return The current value of the system timer, in nanoseconds. */
    public static long nanos(){ return System.nanoTime(); }
    /** @return milliseconds since the Unix epoch. */
    public static long millis(){ return System.currentTimeMillis(); }
    public static long nanosToMillis(long nanos){ return nanos / nanosPerMilli; }
    public static long millisToNanos(long millis){ return millis * nanosPerMilli; }
    public static long timeSinceNanos(long prevTime){ return nanos() - prevTime; }
    public static long timeSinceMillis(long prevTime){ return millis() - prevTime; }

    private static float defaultDelta(){
        // Headless/bootstrap and orderly-dispose paths may legitimately have no Graphics instance.
        // Keep simulation time deterministic rather than dereferencing a process-global renderer.
        return Core.graphics == null ? 1f : Math.min(Core.graphics.getDeltaTime() * 60f, 3f);
    }

    /** Mutable simulation-time state. One instance belongs to one real game runtime. */
    public static class Context{
        private float delta = 1f;
        private float time, globalTime;
        private double timeRaw, globalTimeRaw;
        private Seq<DelayRun> runs = new Seq<>();
        private final Seq<DelayRun> removal = new Seq<>();
        private final LongSeq marks = new LongSeq();
        private Floatp deltaimpl = Time::defaultDelta;

        public float delta(){ return delta; }
        public float time(){ return time; }
        public float globalTime(){ return globalTime; }
        public int pendingRuns(){ return runs.size; }
    }

    public static final class Scope implements AutoCloseable{
        private final Context previous;
        private boolean closed;

        private Scope(Context previous){ this.previous = previous; }

        @Override
        public void close(){
            if(closed) return;
            closed = true;
            if(previous == null) current.remove();
            else current.set(previous);
        }
    }

    public static class DelayRun implements Poolable{
        float delay;
        Runnable finish;

        @Override
        public void reset(){
            delay = 0;
            finish = null;
        }
    }
}
