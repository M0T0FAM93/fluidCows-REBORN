package motofam93.fluidcows.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.ingredients.IIngredientRenderer;
import motofam93.fluidcows.ModRegistries;
import motofam93.fluidcows.entity.FluidCowEntity;
import motofam93.fluidcows.item.FluidCowSpawnItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Collections;
import java.util.List;

final class JeiCowStackRenderer implements IIngredientRenderer<ItemStack> {
    static final JeiCowStackRenderer INSTANCE = new JeiCowStackRenderer();

    @Override
    public void render(GuiGraphics gg, ItemStack stack) {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) return;

        FluidCowEntity cow = ModRegistries.FLUID_COW.get().create(level);
        if (cow == null) return;

        cow.setFluidRL(FluidCowSpawnItem.getFluid(stack));
        cow.setBaby(false);

        float angle = spinAngle(30f);
        float yaw = 180f + angle;
        cow.setYRot(yaw); cow.yRotO = yaw;
        cow.setYBodyRot(yaw); cow.yBodyRotO = yaw;
        cow.setYHeadRot(yaw); cow.yHeadRotO = yaw;

        PoseStack pose = gg.pose();
        pose.pushPose();

        pose.translate(8, 16, 150);
        pose.scale(8.2f, -8.2f, 8.2f);

        EntityRenderDispatcher disp = mc.getEntityRenderDispatcher();
        disp.setRenderShadow(false);
        disp.render(cow, 0, 0, 0, 0f, 0f, pose, gg.bufferSource(), 0xF000F0);
        gg.flush();

        pose.popPose();
    }

    @Override
    public List<Component> getTooltip(ItemStack ingredient, TooltipFlag tooltipFlag) {
        return Collections.emptyList();
    }

    private static float spinAngle(float degPerSec) {
        double secs = System.nanoTime() * 1e-9;
        return (float) ((secs * degPerSec) % 360.0);
    }
}
