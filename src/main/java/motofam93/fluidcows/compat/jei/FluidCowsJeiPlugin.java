package motofam93.fluidcows.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import motofam93.fluidcows.FluidCows;
import motofam93.fluidcows.ModRegistries;
import motofam93.fluidcows.item.FluidCowSpawnItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

@JeiPlugin
public final class FluidCowsJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(FluidCows.MOD_ID, "jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration reg) {
        reg.registerSubtypeInterpreter(
                ModRegistries.FLUID_COW_SPAWN_ITEM.get(),
                (stack, context) -> {
                    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                    if (data == null) return "";
                    CompoundTag tag = data.copyTag();
                    if (!tag.contains(FluidCowSpawnItem.NBT_FLUID)) return "";
                    return tag.getString(FluidCowSpawnItem.NBT_FLUID);
                }
        );
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        var gui = reg.getJeiHelpers().getGuiHelper();
        reg.addRecipeCategories(new JeiCowBreedingCategory(gui));
        reg.addRecipeCategories(new JeiCowInfoCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        reg.addRecipes(JeiCowBreedingCategory.TYPE, JeiCowData.buildBreedingRecipes());
        reg.addRecipes(JeiCowInfoCategory.TYPE, JeiCowData.buildInfoRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(new ItemStack(ModRegistries.FLUID_COW_SPAWN_ITEM.get()),
                JeiCowBreedingCategory.TYPE, JeiCowInfoCategory.TYPE);
    }
}
