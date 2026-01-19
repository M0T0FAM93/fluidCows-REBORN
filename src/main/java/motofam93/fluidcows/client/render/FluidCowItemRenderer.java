package motofam93.fluidcows.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import motofam93.fluidcows.ModRegistries;
import motofam93.fluidcows.entity.FluidCowEntity;
import motofam93.fluidcows.item.FluidCowSpawnItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class FluidCowItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static float spinAngle(float degPerSec) {
        double secs = System.nanoTime() * 1e-9;
        return (float) ((secs * degPerSec) % 360.0);
    }

    public static final FluidCowItemRenderer INSTANCE = new FluidCowItemRenderer();

    private final EntityRenderDispatcher dispatcher;

    public FluidCowItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
        this.dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
    }

    @Override
    public void renderByItem(ItemStack stack,
                             ItemDisplayContext ctx,
                             PoseStack pose,
                             MultiBufferSource buffer,
                             int packedLight,
                             int packedOverlay) {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) return;

        FluidCowEntity entity = ModRegistries.FLUID_COW.get().create(level);
        if (entity == null) return;

        ResourceLocation rl = FluidCowSpawnItem.getFluid(stack);
        entity.setFluidRL(rl);
        entity.setBaby(false);

        final float SPEED_DEG_PER_SEC = 30f;
        float angle = spinAngle(SPEED_DEG_PER_SEC);
        float baseYaw = 180f;

        pose.pushPose();

        switch (ctx) {
            case GUI -> {
                float yaw = baseYaw + angle;
                entity.setYRot(yaw); entity.yRotO = yaw;
                entity.setYBodyRot(yaw); entity.yBodyRotO = yaw;
                entity.setYHeadRot(yaw); entity.yHeadRotO = yaw;

                pose.translate(0.5, 0.10, 0.5);
                pose.scale(0.60f, 0.60f, 0.60f);
                pose.mulPose(Axis.YP.rotationDegrees(180f));
                pose.mulPose(Axis.XP.rotationDegrees(18f));
            }
            case FIXED -> {
                float yaw = baseYaw + angle;
                entity.setYRot(yaw); entity.yRotO = yaw;
                entity.setYBodyRot(yaw); entity.yBodyRotO = yaw;
                entity.setYHeadRot(yaw); entity.yHeadRotO = yaw;

                pose.translate(0.5, 0.58, 0.5);
                pose.scale(0.40f, 0.40f, 0.40f);
                pose.mulPose(Axis.YP.rotationDegrees(180f));
            }
            case GROUND -> {
                float yaw = baseYaw + angle;
                entity.setYRot(yaw); entity.yRotO = yaw;
                entity.setYBodyRot(yaw); entity.yBodyRotO = yaw;
                entity.setYHeadRot(yaw); entity.yHeadRotO = yaw;

                pose.translate(0.5, 0.52, 0.5);
                pose.scale(0.35f, 0.35f, 0.35f);
                pose.mulPose(Axis.YP.rotationDegrees(180f));
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                float yaw = baseYaw;
                entity.setYRot(yaw); entity.yRotO = yaw;
                entity.setYBodyRot(yaw); entity.yBodyRotO = yaw;
                entity.setYHeadRot(yaw); entity.yHeadRotO = yaw;

                pose.translate(0.5, 0.62, 0.5);
                pose.scale(0.32f, 0.32f, 0.32f);
                pose.mulPose(Axis.YP.rotationDegrees(180f));
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                float yaw = baseYaw;
                entity.setYRot(yaw); entity.yRotO = yaw;
                entity.setYBodyRot(yaw); entity.yBodyRotO = yaw;
                entity.setYHeadRot(yaw); entity.yHeadRotO = yaw;

                pose.translate(0.5, 0.65, 0.5);
                pose.scale(0.35f, 0.35f, 0.35f);
                pose.mulPose(Axis.YP.rotationDegrees(180f));
            }
            default -> {
                float yaw = baseYaw;
                entity.setYRot(yaw); entity.yRotO = yaw;
                entity.setYBodyRot(yaw); entity.yBodyRotO = yaw;
                entity.setYHeadRot(yaw); entity.yHeadRotO = yaw;

                pose.translate(0.5, 0.60, 0.5);
                pose.scale(0.40f, 0.40f, 0.40f);
                pose.mulPose(Axis.YP.rotationDegrees(180f));
            }
        }

        dispatcher.setRenderShadow(false);
        dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0F, 0.0F, pose, buffer, packedLight);
        pose.popPose();
    }
}
