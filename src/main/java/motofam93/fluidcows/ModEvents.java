package motofam93.fluidcows;

import net.minecraft.world.entity.animal.Cow;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class ModEvents {
    private ModEvents() {}

    public static void registerAttributes(final EntityAttributeCreationEvent event) {
        event.put(ModRegistries.FLUID_COW.get(), Cow.createAttributes().build());
    }
}
