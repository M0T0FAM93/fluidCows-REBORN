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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class JeiCowInfoCategory implements IRecipeCategory<JeiCowInfoRecipe> {
    public static final RecipeType<JeiCowInfoRecipe> TYPE =
            RecipeType.create(FluidCows.MOD_ID, "cow_info", JeiCowInfoRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public JeiCowInfoCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(176, 72);
        this.icon = gui.createDrawableItemStack(new ItemStack(Items.BOOK));
    }

    @Override
    public RecipeType<JeiCowInfoRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Fluid Cow Info");
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
    public void setRecipe(IRecipeLayoutBuilder b, JeiCowInfoRecipe r, IFocusGroup focuses) {
        b.addSlot(RecipeIngredientRole.INPUT, 10, 18)
                .setCustomRenderer(VanillaTypes.ITEM_STACK, JeiCowStackRenderer.INSTANCE)
                .addItemStack(r.cow);
    }

    @Override
    public void draw(JeiCowInfoRecipe r, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
        final Font font = Minecraft.getInstance().font;
        final int color = 0xFFAA00AA;

        int x = 36;
        int y = 16;

        g.drawString(font, "Spawn Weight: " + r.weight, x, y, color, false);
        y += 14;
        g.drawString(font, "Breeding Cooldown: " + pretty(r.breedTicks), x, y, color, false);
        y += 14;
        g.drawString(font, "Baby Growth Time: " + pretty(r.growTicks), x, y, color, false);
        y += 14;
        g.drawString(font, "Milking Cooldown: " + pretty(r.milkTicks), x, y, color, false);
    }

    private static String pretty(int ticks) {
        int s = Math.max(0, (ticks + 19) / 20);
        int m = s / 60, r = s % 60;
        return m > 0 ? (m + "m " + (r < 10 ? "0" : "") + r + "s") : (r + "s");
    }
}
