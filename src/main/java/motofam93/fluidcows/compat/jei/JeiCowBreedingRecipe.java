package motofam93.fluidcows.compat.jei;

import net.minecraft.world.item.ItemStack;

final class JeiCowBreedingRecipe {
    final ItemStack parent1, parent2, breedingItem, result;
    final int chance;

    JeiCowBreedingRecipe(ItemStack p1, ItemStack p2, ItemStack breedingItem, ItemStack result, int chance) {
        this.parent1 = p1;
        this.parent2 = p2;
        this.breedingItem = breedingItem;
        this.result = result;
        this.chance = Math.max(0, Math.min(100, chance));
    }
}
