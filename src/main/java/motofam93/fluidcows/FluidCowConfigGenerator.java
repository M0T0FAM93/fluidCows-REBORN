package motofam93.fluidcows;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FluidCowConfigGenerator {
    private FluidCowConfigGenerator() {}

    private static final Path ROOT = Path.of("config", FluidCows.MOD_ID);

    public static Path configRoot() {
        return ROOT;
    }

    public static void generateAll() {
        try {
            Files.createDirectories(ROOT);
        } catch (IOException ignored) {}
        
        for (Fluid f : BuiltInRegistries.FLUID) {
            if (f == Fluids.EMPTY) continue;
            writeOne(f);
        }
    }

    private static void writeOne(Fluid f) {
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(f);
        if (id == null || id.getPath().contains("flowing")) return;

        Path file = ROOT.resolve(Path.of(id.getNamespace(), id.getPath() + ".json"));
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException ignored) {}
        
        if (Files.exists(file)) return;

        String sprite = getStillSpriteId(f, id);

        String json = """
    {
      "fluid": "%s",
      "enabled": true,
      "spawn_weight": 1,
      "breeding_cooldown_ticks": 6000,
      "growth_time_ticks": 24000,
      "bucket_cooldown_ticks": 4000,
      "sprite": "%s",
      "breeding": {
        "breeding_item": "minecraft:wheat",
        "parent_1": "",
        "parent_2": "",
        "chance": 33
      }
    }
    """.formatted(id, sprite);

        try (Writer w = Files.newBufferedWriter(file)) {
            w.write(json);
        } catch (IOException e) {
            FluidCows.LOGGER.error("Failed writing config file {}: {}", file, e.getMessage());
        }
    }

    private static String getStillSpriteId(Fluid f, ResourceLocation fallbackId) {
        try {
            ResourceLocation still = IClientFluidTypeExtensions.of(f).getStillTexture(new FluidStack(f, 1000));
            if (still != null) return still.toString();
        } catch (Throwable ignored) {}
        
        String base = fallbackId.getPath().replace("flowing_", "").replace("_flowing", "");
        return ResourceLocation.fromNamespaceAndPath(fallbackId.getNamespace(), "block/" + base + "_still").toString();
    }
}
