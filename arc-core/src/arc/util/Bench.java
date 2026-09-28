package arc.util;

import arc.struct.*;

/** Lightweight per-thread benchmark accumulator; safe for concurrent runtime workers. */
public class Bench{
    private static final ThreadLocal<State> state = ThreadLocal.withInitial(State::new);

    public static void begin(String name){
        State value = state.get();
        if(value.lastName != null){
            endi(value);
        }else{
            value.totalStart = Time.millis();
        }
        value.last = Time.millis();
        value.lastName = name;
    }

    public static void end(){
        State value = state.get();
        endi(value);
        long total = Time.timeSinceMillis(value.totalStart);

        value.times.each((name, time) -> {
            Log.info("[PERF] @: @ms (@%)", name, time, (int)((float)time / total * 100));
        });
        Log.info("[PERF] TOTAL: @ms", total);
        value.times.clear();
    }

    private static void endi(State value){
        if(value.lastName == null) return;
        value.times.put(value.lastName, Time.timeSinceMillis(value.last));
        value.lastName = null;
    }

    private static final class State{
        long totalStart, last;
        String lastName;
        final ObjectMap<String, Long> times = new ObjectMap<>();
    }
}
