package motofam93.fluidcows;

import motofam93.fluidcows.item.FluidCowSpawnItem;
import motofam93.fluidcows.util.EnabledFluids;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FluidCows.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fluidcows.main"))
                    .icon(() -> FluidCowSpawnItem.withFluid(ResourceLocation.withDefaultNamespace("water")))
                    .displayItems((params, output) -> {
                        for (ResourceLocation rl : EnabledFluids.all()) {
                            output.accept(FluidCowSpawnItem.withFluid(rl));
                        }
                        output.accept(new ItemStack(ModRegistries.FLUID_COW_SPAWN_EGG.get()));
                    })
                    .build());

    public static void init(IEventBus modBus) {
        TABS.register(modBus);
    }
}
