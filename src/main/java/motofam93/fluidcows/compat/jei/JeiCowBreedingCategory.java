package motofam93.fluidcows.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import motofam93.fluidcows.FluidCows;
import motofam93.fluidcows.item.FluidCowSpawnItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;

public final class JeiCowBreedingCategory implements IRecipeCategory<JeiCowBreedingRecipe> {
    public static final RecipeType<JeiCowBreedingRecipe> TYPE =
            RecipeType.create(FluidCows.MOD_ID, "cow_breeding", JeiCowBreedingRecipe.class);

    private static final int W = 176, H = 96;
    private static final int TEXT = 0xFFAA00AA;
    private static final ResourceLocation HEART =
            ResourceLocation.fromNamespaceAndPath(FluidCows.MOD_ID, "textures/gui/heart.png");

    private static final int PARENT_Y = 24;
    private static final int HEART_Y = 30;
    private static final int CHANCE_Y = 50;
    private static final int CHILD_Y = 66;
    private static final int NAMES_Y = 44;
    private static final int CHILD_NAME_Y = H - 10;

    private final IDrawable background;
    private final IDrawable icon;

    public JeiCowBreedingCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(W, H);
        this.icon = gui.createDrawableItemStack(new ItemStack(Items.WHEAT));
    }

    @Override
    public RecipeType<JeiCowBreedingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Fluid Cow Breeding");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, JeiCowBreedingRecipe r, IFocusGroup focuses) {
        b.addSlot(RecipeIngredientRole.CATALYST, (W / 2) - 8, 6)
                .addItemStack(r.breedingItem);

        b.addSlot(RecipeIngredientRole.INPUT, 22, PARENT_Y)
                .setCustomRenderer(VanillaTypes.ITEM_STACK, JeiCowStackRenderer.INSTANCE)
                .addItemStack(r.parent1);

        b.addSlot(RecipeIngredientRole.INPUT, W - 22 - 18, PARENT_Y)
                .setCustomRenderer(VanillaTypes.ITEM_STACK, JeiCowStackRenderer.INSTANCE)
                .addItemStack(r.parent2);

        b.addSlot(RecipeIngredientRole.OUTPUT, (W / 2) - 9, CHILD_Y)
                .setCustomRenderer(VanillaTypes.ITEM_STACK, JeiCowStackRenderer.INSTANCE)
                .addItemStack(r.result);
    }

    @Override
    public void draw(JeiCowBreedingRecipe r, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
        Font f = Minecraft.getInstance().font;

        int heartX = (W / 2) - 8;
        try {
            g.blit(HEART, heartX, HEART_Y, 0, 0, 16, 16, 16, 16);
        } catch (Throwable ignored) {
            g.drawString(f, "❤", heartX + 3, HEART_Y + 4, 0xFFFF5555, false);
        }

        String chance = Math.max(0, Math.min(100, r.chance)) + "%";
        g.drawString(f, chance, (W - f.width(chance)) / 2, CHANCE_Y, TEXT, false);

        String leftName = f.plainSubstrByWidth(fluidName(r.parent1), 80);
        String rightName = f.plainSubstrByWidth(fluidName(r.parent2), 80);
        String childName = f.plainSubstrByWidth(fluidName(r.result), 100);

        g.drawString(f, leftName, 10, NAMES_Y, TEXT, false);
        g.drawString(f, rightName, W - 10 - f.width(rightName), NAMES_Y, TEXT, false);
        g.drawString(f, childName, (W - f.width(childName)) / 2, CHILD_NAME_Y, TEXT, false);
    }

    private static String fluidName(ItemStack cowItem) {
        var rl = FluidCowSpawnItem.getFluid(cowItem);
        var fluid = BuiltInRegistries.FLUID.get(rl);
        if (fluid == null) return rl.toString();
        return new FluidStack(fluid, 1000).getHoverName().getString();
    }
}
