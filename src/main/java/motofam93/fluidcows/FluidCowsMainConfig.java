package motofam93.fluidcows;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FluidCowsMainConfig {
    private FluidCowsMainConfig() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "main_config.json";
    private static final int DEFAULT_REPLACE_PERCENT = 15;

    private static volatile int VANILLA_REPLACE_PERCENT = DEFAULT_REPLACE_PERCENT;

    public static int replaceChancePercent() {
        return VANILLA_REPLACE_PERCENT;
    }

    public static void loadOrCreate() {
        Path root = FluidCowConfigGenerator.configRoot();
        Path file = root.resolve(FILE_NAME);

        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            FluidCows.LOGGER.warn("Could not create config directory {}: {}", root, e.toString());
        }

        if (!Files.exists(file)) {
            writeDefault(file);
            return;
        }

        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject obj = GSON.fromJson(r, JsonObject.class);
            int pct = obj != null && obj.has("vanilla_replace_percent") 
                ? obj.get("vanilla_replace_percent").getAsInt() 
                : DEFAULT_REPLACE_PERCENT;
            VANILLA_REPLACE_PERCENT = clamp0to100(pct);
        } catch (Throwable t) {
            FluidCows.LOGGER.warn("Failed reading {}: {} (using defaults)", file, t.toString());
            VANILLA_REPLACE_PERCENT = DEFAULT_REPLACE_PERCENT;
        }
    }

    public static void ensureExists() {
        Path root = FluidCowConfigGenerator.configRoot();
        Path file = root.resolve(FILE_NAME);
        if (!Files.exists(file)) writeDefault(file);
    }

    private static void writeDefault(Path file) {
        JsonObject obj = new JsonObject();
        obj.addProperty("vanilla_replace_percent", DEFAULT_REPLACE_PERCENT);

        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(obj, w);
            }
        } catch (IOException e) {
            FluidCows.LOGGER.warn("Could not write {}: {}", file, e.toString());
        }

        VANILLA_REPLACE_PERCENT = DEFAULT_REPLACE_PERCENT;
    }

    private static int clamp0to100(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
