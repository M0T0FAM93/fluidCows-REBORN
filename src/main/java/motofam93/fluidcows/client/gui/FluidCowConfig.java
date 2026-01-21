package motofam93.fluidcows.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import motofam93.fluidcows.FluidCowConfigGenerator;
import motofam93.fluidcows.FluidCows;
import motofam93.fluidcows.util.EnabledFluids;
import net.minecraft.resources.ResourceLocation;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class FluidCowConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public ResourceLocation fluidId;
    public boolean enabled = true;
    public int spawnWeight = 1;
    public int breedingCooldownTicks = 6000;
    public int growthTimeTicks = 24000;
    public int bucketCooldownTicks = 4000;
    public String breedingItem = "minecraft:wheat";
    public String parent1 = "";
    public String parent2 = "";
    public int breedingChance = 33;

    public FluidCowConfig(ResourceLocation fluidId) {
        this.fluidId = fluidId;
    }

    public static FluidCowConfig load(ResourceLocation fluidId) {
        FluidCowConfig config = new FluidCowConfig(fluidId);
        Path file = getConfigPath(fluidId);

        if (!Files.exists(file)) return config;

        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();

            config.enabled = obj.has("enabled") ? obj.get("enabled").getAsBoolean() : true;
            config.spawnWeight = obj.has("spawn_weight") ? obj.get("spawn_weight").getAsInt() : 1;
            config.breedingCooldownTicks = obj.has("breeding_cooldown_ticks") ? obj.get("breeding_cooldown_ticks").getAsInt() : 6000;
            config.growthTimeTicks = obj.has("growth_time_ticks") ? obj.get("growth_time_ticks").getAsInt() : 24000;
            config.bucketCooldownTicks = obj.has("bucket_cooldown_ticks") ? obj.get("bucket_cooldown_ticks").getAsInt() : 4000;

            if (obj.has("breeding") && obj.get("breeding").isJsonObject()) {
                JsonObject breeding = obj.getAsJsonObject("breeding");
                config.breedingItem = breeding.has("breeding_item") ? breeding.get("breeding_item").getAsString() : "minecraft:wheat";
                config.parent1 = breeding.has("parent_1") ? breeding.get("parent_1").getAsString() : "";
                config.parent2 = breeding.has("parent_2") ? breeding.get("parent_2").getAsString() : "";
                config.breedingChance = breeding.has("chance") ? breeding.get("chance").getAsInt() : 33;
            }
        } catch (Throwable t) {
            FluidCows.LOGGER.error("Failed to load config for {}: {}", fluidId, t.getMessage());
        }

        return config;
    }

    public boolean save() {
        Path file = getConfigPath(fluidId);

        try {
            Files.createDirectories(file.getParent());

            JsonObject obj;
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    obj = JsonParser.parseReader(reader).getAsJsonObject();
                }
            } else {
                obj = new JsonObject();
                obj.addProperty("fluid", fluidId.toString());
            }

            obj.addProperty("enabled", enabled);
            obj.addProperty("spawn_weight", spawnWeight);
            obj.addProperty("breeding_cooldown_ticks", breedingCooldownTicks);
            obj.addProperty("growth_time_ticks", growthTimeTicks);
            obj.addProperty("bucket_cooldown_ticks", bucketCooldownTicks);

            JsonObject breeding;
            if (obj.has("breeding") && obj.get("breeding").isJsonObject()) {
                breeding = obj.getAsJsonObject("breeding");
            } else {
                breeding = new JsonObject();
                obj.add("breeding", breeding);
            }

            breeding.addProperty("breeding_item", breedingItem);
            breeding.addProperty("parent_1", parent1);
            breeding.addProperty("parent_2", parent2);
            breeding.addProperty("chance", breedingChance);

            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(obj, writer);
            }

            return true;
        } catch (Throwable t) {
            FluidCows.LOGGER.error("Failed to save config for {}: {}", fluidId, t.getMessage());
            return false;
        }
    }

    public static void reloadAll() {
        EnabledFluids.reloadFromDisk();
        motofam93.fluidcows.util.BreedingManager.reload();
    }

    private static Path getConfigPath(ResourceLocation fluidId) {
        return FluidCowConfigGenerator.configRoot()
                .resolve(fluidId.getNamespace())
                .resolve(fluidId.getPath() + ".json");
    }

    public static String formatTicks(int ticks) {
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    public static int parseTicks(String input) {
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            int ticks = 0;
            String lower = input.toLowerCase().trim();

            if (lower.contains("m")) {
                String[] parts = lower.split("m");
                ticks += Integer.parseInt(parts[0].trim()) * 60 * 20;
                if (parts.length > 1) {
                    lower = parts[1];
                } else {
                    return ticks;
                }
            }

            if (lower.contains("s")) {
                String secPart = lower.replace("s", "").trim();
                if (!secPart.isEmpty()) ticks += Integer.parseInt(secPart) * 20;
            }

            return ticks;
        }
    }
}
