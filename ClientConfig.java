package pl.sharedwaypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Lokalne ustawienia gracza (config/sharedwaypoints.json). */
public class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ClientConfig instance = new ClientConfig();

    public boolean showHud = true;
    public boolean showNames = true;
    public boolean showDistance = true;
    public int maxDistance = 0; // 0 = bez limitu
    public Set<String> hidden = new HashSet<>();

    public static final int[] DISTANCES = {0, 100, 250, 500, 1000, 2500};

    public static ClientConfig get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("sharedwaypoints.json");
    }

    public static void load() {
        Path p = path();
        if (!Files.exists(p)) return;
        try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            ClientConfig c = GSON.fromJson(r, ClientConfig.class);
            if (c != null) {
                if (c.hidden == null) c.hidden = new HashSet<>();
                instance = c;
            }
        } catch (Exception e) {
            SharedWaypoints.LOGGER.error("Nie udało się wczytać configu", e);
        }
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
            GSON.toJson(this, w);
        } catch (Exception e) {
            SharedWaypoints.LOGGER.error("Nie udało się zapisać configu", e);
        }
    }

    public void cycleDistance() {
        int idx = 0;
        for (int i = 0; i < DISTANCES.length; i++) if (DISTANCES[i] == maxDistance) idx = i;
        maxDistance = DISTANCES[(idx + 1) % DISTANCES.length];
    }
}
