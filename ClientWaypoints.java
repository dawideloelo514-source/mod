package pl.sharedwaypoints;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Waypointy zsynchronizowane z serwera (po stronie klienta). */
public final class ClientWaypoints {
    private static final Map<UUID, Waypoint> MAP = new LinkedHashMap<>();
    /** Zwiększane przy każdej zmianie - ekrany używają tego do odświeżania. */
    public static int version = 0;

    private ClientWaypoints() {}

    public static List<Waypoint> all() {
        return new ArrayList<>(MAP.values());
    }

    public static void replaceAll(List<Waypoint> list) {
        MAP.clear();
        for (Waypoint w : list) MAP.put(w.id, w);
        version++;
    }

    public static void put(Waypoint w) {
        MAP.put(w.id, w);
        version++;
    }

    public static void remove(UUID id) {
        MAP.remove(id);
        version++;
    }

    public static void clear() {
        MAP.clear();
        version++;
    }
}
