package motofam93.fluidcows.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import motofam93.fluidcows.FluidCowConfigGenerator;
import motofam93.fluidcows.FluidCows;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class EnabledFluids {
    private EnabledFluids() {}

    private static final class Rec {
        final ResourceLocation rl;
        final boolean enabled;
        final int weight;

        Rec(ResourceLocation rl, boolean enabled, int weight) {
            this.rl = rl;
            this.enabled = enabled;
            this.weight = weight;
        }
    }

    private static final Map<ResourceLocation, Rec> RECORDS = new LinkedHashMap<>();
    private static int TOTAL_WEIGHT = 0;

    public static void reloadFromDisk() {
        RECORDS.clear();
        TOTAL_WEIGHT = 0;

        Path root = FluidCowConfigGenerator.configRoot();
        if (!Files.isDirectory(root)) return;

        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json"))
                 .filter(p -> p.getParent() != null && !p.getParent().equals(root))
                 .forEach(EnabledFluids::readOne);
        } catch (IOException e) {
            FluidCows.LOGGER.error("Failed walking config directory: {}", e.getMessage());
        }

        for (Rec r : RECORDS.values()) {
            if (r.enabled && r.weight > 0) TOTAL_WEIGHT += r.weight;
        }

        FluidCows.LOGGER.info("Loaded {} fluid cow configs ({} enabled, total weight: {})",
                RECORDS.size(),
                RECORDS.values().stream().filter(r -> r.enabled).count(), 
                TOTAL_WEIGHT);
    }

    private static void readOne(Path file) {
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();

            ResourceLocation rl = null;
            if (obj.has("fluid")) {
                rl = ResourceLocation.parse(obj.get("fluid").getAsString());
            } else {
                Path root = FluidCowConfigGenerator.configRoot();
                Path rel = root.relativize(file);
                if (rel.getNameCount() >= 2) {
                    String ns = rel.getName(0).toString();
                    String pathPart = rel.subpath(1, rel.getNameCount()).toString().replace('\\', '/');
                    if (pathPart.endsWith(".json")) pathPart = pathPart.substring(0, pathPart.length() - 5);
                    rl = ResourceLocation.fromNamespaceAndPath(ns, pathPart);
                }
            }

            if (rl == null) return;
            if (rl.getNamespace().equals("minecraft") && rl.getPath().equals("empty")) return;
            if (rl.getPath().startsWith("flowing_")) return;

            boolean enabled = obj.has("enabled") ? obj.get("enabled").getAsBoolean() : true;
            int weight = obj.has("spawn_weight") ? obj.get("spawn_weight").getAsInt() : 1;

            if (enabled && !isFluidValid(rl)) {
                FluidCows.LOGGER.warn("Skipping invalid fluid: {}", rl);
                return;
            }

            RECORDS.put(rl, new Rec(rl, enabled, weight));
        } catch (Throwable t) {
            FluidCows.LOGGER.error("Failed to read config {}: {}", file, t.getMessage());
        }
    }

    public static boolean isFluidValid(ResourceLocation rl) {
        try {
            Fluid fluid = BuiltInRegistries.FLUID.get(rl);
            if (fluid == null || fluid == Fluids.EMPTY) return false;

            String name = new FluidStack(fluid, 1000).getHoverName().getString();
            if (name == null || name.isEmpty()) return false;
            if (name.startsWith("block.") || name.startsWith("fluid.") || name.startsWith("item.")) return false;
            if (name.contains("�")) return false;

            for (char c : name.toCharArray()) {
                if (c < 32 && c != '\n' && c != '\r' && c != '\t') return false;
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static int getBreedingCooldown(ResourceLocation rl) {
        int value = readValueFromDisk(rl, "breeding_cooldown_ticks", 6000);
        FluidCows.LOGGER.debug("[FluidCows] getBreedingCooldown({}) = {}", rl, value);
        return value;
    }

    public static int getGrowthTimeTicks(ResourceLocation rl) {
        int value = readValueFromDisk(rl, "growth_time_ticks", 24000);
        FluidCows.LOGGER.info("[FluidCows] getGrowthTimeTicks({}) = {} ticks ({}s)", rl, value, value / 20);
        return value;
    }

    public static int getBucketCooldownTicks(ResourceLocation rl) {
        int value = readValueFromDisk(rl, "bucket_cooldown_ticks", 4000);
        FluidCows.LOGGER.debug("[FluidCows] getBucketCooldownTicks({}) = {}", rl, value);
        return value;
    }

    private static int readValueFromDisk(ResourceLocation rl, String key, int defaultValue) {
        Path file = FluidCowConfigGenerator.configRoot().resolve(rl.getNamespace()).resolve(rl.getPath() + ".json");
        FluidCows.LOGGER.debug("[FluidCows] Reading {} from {}", key, file.toAbsolutePath());
        if (!Files.exists(file)) {
            FluidCows.LOGGER.warn("[FluidCows] Config file not found: {}", file.toAbsolutePath());
            return defaultValue;
        }
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            int value = obj.has(key) ? obj.get(key).getAsInt() : defaultValue;
            FluidCows.LOGGER.debug("[FluidCows] Read {}={} from {}", key, value, file);
            return value;
        } catch (Throwable t) {
            FluidCows.LOGGER.error("[FluidCows] Failed to read {}: {}", file, t.getMessage());
            return defaultValue;
        }
    }

    public static Collection<ResourceLocation> all() {
        return RECORDS.values().stream().filter(r -> r.enabled).map(r -> r.rl).collect(Collectors.toUnmodifiableSet());
    }

    public static Collection<ResourceLocation> allIncludingDisabled() {
        return RECORDS.keySet();
    }

    public static ResourceLocation pickRandom(RandomSource rand) {
        List<Rec> enabled = RECORDS.values().stream().filter(r -> r.enabled).toList();
        if (enabled.isEmpty()) return ResourceLocation.withDefaultNamespace("water");
        return enabled.get(rand.nextInt(enabled.size())).rl;
    }

    public static ResourceLocation pickWeighted(RandomSource rand) {
        if (TOTAL_WEIGHT <= 0) return pickRandom(rand);
        int roll = rand.nextInt(TOTAL_WEIGHT);
        int acc = 0;
        for (Rec r : RECORDS.values()) {
            if (!r.enabled || r.weight <= 0) continue;
            acc += r.weight;
            if (roll < acc) return r.rl;
        }
        return pickRandom(rand);
    }

    public static int getWeight(ResourceLocation rl) {
        Rec r = RECORDS.get(rl);
        return r != null ? r.weight : 1;
    }

    public static boolean isEnabled(ResourceLocation rl) {
        Rec r = RECORDS.get(rl);
        return r != null && r.enabled;
    }
}
