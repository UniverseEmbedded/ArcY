package arc;

import arc.func.*;
import arc.struct.*;

import java.util.*;

/**
 * Event dispatch facade with an explicitly bindable runtime bus.
 *
 * The historical static API is retained, but mutable listener state is no longer forced to be process-global. Code
 * running inside a game runtime binds that runtime's {@link EventBus}; process/application listeners continue to live
 * on the global bus. Runtime buses may delegate fired events to the global bus so application-level observers remain
 * compatible without leaking runtime-local listeners into other worlds.
 */
@SuppressWarnings("unchecked")
public class Events{
    private static final EventBus global = new EventBus(null);
    private static final ThreadLocal<EventBus> current = new ThreadLocal<>();
    private static volatile Prov<EventBus> defaultBusProvider;

    public static EventBus global(){
        return global;
    }

    public static EventBus current(){
        EventBus bus = current.get();
        if(bus != null) return bus;
        Prov<EventBus> provider = defaultBusProvider;
        if(provider != null){
            EventBus provided = provider.get();
            if(provided != null) return provided;
        }
        return global;
    }

    /**
     * Installs an application-owned resolver for the implicit event bus. This is used by engines that keep runtime
     * ownership outside Arc itself; explicit {@link #enter(EventBus)} bindings still take precedence.
     */
    public static void setDefaultBusProvider(Prov<EventBus> provider){
        defaultBusProvider = provider;
    }

    /** Binds an event bus to the current thread until the returned scope is closed. */
    public static Scope enter(EventBus bus){
        Objects.requireNonNull(bus);
        EventBus previous = current.get();
        current.set(bus);
        return new Scope(previous);
    }

    /** Handle an event by class on the currently-bound bus. */
    public static <T> void on(Class<T> type, Cons<T> listener){
        current().on(type, listener);
    }

    /** Handle an event by enum trigger on the currently-bound bus. */
    public static void run(Object type, Runnable listener){
        current().run(type, listener);
    }

    /** Only use this method if you have the exact listener reference used for registration. */
    public static <T> boolean remove(Class<T> type, Cons<T> listener){
        return current().remove(type, listener);
    }

    /** Fires an enum trigger on the currently-bound bus. */
    public static <T extends Enum<T>> void fire(Enum<T> type){
        current().fire(type);
    }

    /** Fires a non-enum event by class on the currently-bound bus. */
    public static <T> void fire(T type){
        current().fire(type);
    }

    public static <T> void fire(Class<?> ctype, T type){
        current().fire(ctype, type);
    }

    /** Clears listeners from the currently-bound bus only. */
    public static void clear(){
        current().clear();
    }

    public static final class Scope implements AutoCloseable{
        private final EventBus previous;
        private boolean closed;

        private Scope(EventBus previous){
            this.previous = previous;
        }

        @Override
        public void close(){
            if(closed) return;
            closed = true;
            if(previous == null) current.remove();
            else current.set(previous);
        }
    }

    /** Mutable listener state owned by one runtime or by the process-global application scope. */
    public static class EventBus{
        private final ObjectMap<Object, Seq<Cons<?>>> events = new ObjectMap<>();
        private final EventBus parent;

        public EventBus(){
            this(null);
        }

        public EventBus(EventBus parent){
            if(parent == this) throw new IllegalArgumentException("event bus cannot parent itself");
            this.parent = parent;
        }

        public synchronized <T> void on(Class<T> type, Cons<T> listener){
            events.get(type, () -> new Seq<>(Cons.class)).add(listener);
        }

        public synchronized void run(Object type, Runnable listener){
            events.get(type, () -> new Seq<>(Cons.class)).add(e -> listener.run());
        }

        public synchronized <T> boolean remove(Class<T> type, Cons<T> listener){
            Seq<Cons<?>> listeners = events.get(type);
            return listeners != null && listeners.remove(listener);
        }

        public <T extends Enum<T>> void fire(Enum<T> type){
            fireLocal(type, type);
            if(parent != null) parent.fire(type);
        }

        public <T> void fire(T type){
            fire(type.getClass(), type);
        }

        public <T> void fire(Class<?> ctype, T type){
            fireLocal(ctype, type);
            if(parent != null) parent.fire(ctype, type);
        }

        private <T> void fireLocal(Object key, T type){
            // Runtime buses may delegate to the process bus from different GameContext worker threads. Snapshot the
            // listener list under the bus lock, then invoke outside the lock so listeners can safely register/remove
            // listeners (or recursively fire events) without corrupting ObjectMap/Seq iteration or deadlocking.
            Seq<Cons<?>> snapshot;
            synchronized(this){
                Seq<Cons<?>> listeners = events.get(key);
                if(listeners == null || listeners.isEmpty()) return;
                snapshot = listeners.copy();
            }
            for(Cons listener : snapshot) listener.get(type);
        }

        public synchronized void clear(){
            events.clear();
        }

        public synchronized int listenerTypeCount(){
            return events.size;
        }

        /** Number of listeners registered for one event/trigger key on this bus. Useful for lifecycle leak gates. */
        public synchronized int listenerCount(Object type){
            Seq<Cons<?>> listeners = events.get(type);
            return listeners == null ? 0 : listeners.size;
        }
    }
}
