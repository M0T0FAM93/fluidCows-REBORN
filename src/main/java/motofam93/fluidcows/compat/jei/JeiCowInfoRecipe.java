package motofam93.fluidcows.compat.jei;

import net.minecraft.world.item.ItemStack;

final class JeiCowInfoRecipe {
    final ItemStack cow;
    final int breedTicks, growTicks, milkTicks, weight;

    JeiCowInfoRecipe(ItemStack cow, int breedTicks, int growTicks, int milkTicks, int weight) {
        this.cow = cow;
        this.breedTicks = Math.max(0, breedTicks);
        this.growTicks = Math.max(0, growTicks);
        this.milkTicks = Math.max(0, milkTicks);
        this.weight = Math.max(0, weight);
    }
}
