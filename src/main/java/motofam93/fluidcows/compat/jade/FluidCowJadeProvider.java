package motofam93.fluidcows.compat.jade;

import motofam93.fluidcows.entity.FluidCowEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum FluidCowJadeProvider
        implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath("fluidcows", "milk_cooldown");

    // ----- Server -> Client data sync -----
    @Override
    public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
        if (accessor.getEntity() instanceof FluidCowEntity fc) {
            tag.putInt("milk_cd", fc.getMilkCooldownTicks()); // ticks remaining
        }
    }

    // ----- Tooltip rendering -----
    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("milk_cd")) return;

        int ticks = data.getInt("milk_cd");
        if (ticks <= 0) return;

        tooltip.add(Component.translatable(
                "jade.fluidcows.milk_cooldown",
                Component.literal(formatTicks(ticks))
        ));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    private static String formatTicks(int ticks) {
        int secs = (ticks + 19) / 20;
        int m = secs / 60, s = secs % 60;
        return m + "m " + (s < 10 ? "0" : "") + s + "s";
    }
}
