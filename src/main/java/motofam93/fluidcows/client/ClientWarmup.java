package motofam93.fluidcows.client;

import motofam93.fluidcows.FluidCows;
import motofam93.fluidcows.util.EnabledFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class ClientWarmup {
    private ClientWarmup() {}

    public static void warmupAll() {
        int count = 0;
        try {
            for (ResourceLocation rl : EnabledFluids.all()) {
                Fluid f = BuiltInRegistries.FLUID.get(rl);
                if (f == null || f == Fluids.EMPTY) continue;
                TexturedMaskCache.get(f);
                count++;
            }
            FluidCows.LOGGER.info("Pre-generated overlays for {} enabled fluids", count);
        } catch (Exception e) {
            FluidCows.LOGGER.error("Failed to warmup client overlays", e);
        }
    }
}
