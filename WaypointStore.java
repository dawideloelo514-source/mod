package pl.sharedwaypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Serwerowa baza waypointów (zapisywana w folderze świata: sharedwaypoints.json). */
public final class WaypointStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Waypoint> MAP = new LinkedHashMap<>();
    private static Path file;

    private WaypointStore() {}

    public static void load(MinecraftServer server) {
        MAP.clear();
        file = server.getSavePath(WorldSavePath.ROOT).resolve("sharedwaypoints.json");
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Waypoint[] arr = GSON.fromJson(r, Waypoint[].class);
            if (arr != null) {
                for (Waypoint w : arr) {
                    if (w == null || w.id == null || w.name == null || w.dimension == null) continue;
                    if (w.owner == null) w.owner = new UUID(0, 0);
                    if (w.ownerName == null) w.ownerName = "";
                    MAP.put(w.id, w);
                }
            }
        } catch (Exception e) {
            SharedWaypoints.LOGGER.error("Nie udało się wczytać waypointów", e);
        }
    }

    public static void save() {
        if (file == null) return;
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(new ArrayList<>(MAP.values()), w);
        } catch (IOException e) {
            SharedWaypoints.LOGGER.error("Nie udało się zapisać waypointów", e);
        }
    }

    public static List<Waypoint> all() {
        return new ArrayList<>(MAP.values());
    }

    public static Waypoint get(UUID id) {
        return MAP.get(id);
    }

    public static void put(Waypoint w) {
        MAP.put(w.id, w);
        save();
    }

    public static Waypoint remove(UUID id) {
        Waypoint w = MAP.remove(id);
        if (w != null) save();
        return w;
    }

    public static int countOwnedBy(UUID owner) {
        int n = 0;
        for (Waypoint w : MAP.values()) if (owner.equals(w.owner)) n++;
        return n;
    }
}
