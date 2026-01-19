package motofam93.fluidcows;

import motofam93.fluidcows.entity.FluidCowEntity;
import motofam93.fluidcows.util.EnabledFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class ModSpawns {
    private ModSpawns() {}

    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(ModSpawns::onEntityJoinLevel);
    }

    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.loadedFromDisk()) return;

        if (!(event.getLevel() instanceof ServerLevel level)) return;

        if (event.getEntity().getType() != EntityType.COW) return;
        if (!(event.getEntity() instanceof Cow cow)) return;

        int replaceChancePercent = FluidCowsMainConfig.replaceChancePercent();
        if (replaceChancePercent <= 0) return;
        if (replaceChancePercent < 100 && level.random.nextInt(100) >= replaceChancePercent) return;

        ResourceLocation fluidId = EnabledFluids.pickWeighted(level.random);

        FluidCowEntity fc = ModRegistries.FLUID_COW.get().create(level);
        if (fc == null) return;

        fc.setFluidRL(fluidId);

        BlockPos pos = cow.blockPosition();
        fc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, cow.getYRot(), cow.getXRot());

        level.addFreshEntity(fc);

        event.setCanceled(true);
        cow.discard();
    }
}
