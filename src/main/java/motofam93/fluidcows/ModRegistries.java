package motofam93.fluidcows;

import motofam93.fluidcows.entity.FluidCowEntity;
import motofam93.fluidcows.item.FluidCowSpawnItem;
import motofam93.fluidcows.item.RandomFluidCowSpawnEggItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRegistries {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, FluidCows.MOD_ID);

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, FluidCows.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<FluidCowEntity>> FLUID_COW =
            ENTITY_TYPES.register("fluid_cow",
                    () -> EntityType.Builder.of(FluidCowEntity::new, MobCategory.CREATURE)
                            .sized(0.9f, 1.4f)
                            .build(FluidCows.MOD_ID + ":fluid_cow"));

    public static final DeferredHolder<Item, Item> FLUID_COW_SPAWN_EGG =
            ITEMS.register("fluid_cow_spawn_egg",
                    () -> new RandomFluidCowSpawnEggItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> FLUID_COW_SPAWN_ITEM =
            ITEMS.register("fluid_cow_item",
                    () -> new FluidCowSpawnItem(new Item.Properties().stacksTo(16)));

    public static void init(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        ModCreativeTabs.init(modBus);
    }
}
