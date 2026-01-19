package motofam93.fluidcows;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import motofam93.fluidcows.client.TexturedMaskCache;
import motofam93.fluidcows.entity.FluidCowEntity;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class FluidCowRenderers {

    public static void register() {
        FluidCows.LOGGER.debug("Registering FluidCow entity renderer");
        EntityRenderers.register(ModRegistries.FLUID_COW.get(), CowRenderer::new);
    }

    public static class CowRenderer extends MobRenderer<FluidCowEntity, CowModel<FluidCowEntity>> {
        private static final ResourceLocation BASE =
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/cow/cow.png");

        public CowRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new CowModel<>(ctx.bakeLayer(ModelLayers.COW)), 0.7F);
            this.addLayer(new FluidOverlayLayer(this));
        }

        @Override
        public ResourceLocation getTextureLocation(FluidCowEntity cow) {
            return BASE;
        }
    }

    public static class FluidOverlayLayer extends RenderLayer<FluidCowEntity, CowModel<FluidCowEntity>> {
        public FluidOverlayLayer(CowRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose,
                           MultiBufferSource buffers,
                           int packedLight,
                           FluidCowEntity cow,
                           float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {

            Fluid fluid = BuiltInRegistries.FLUID.get(cow.getFluidRL());
            if (fluid == null || fluid == Fluids.EMPTY) return;

            ResourceLocation overlay = TexturedMaskCache.get(fluid);
            if (overlay == null) return;

            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(overlay));

            this.getParentModel().renderToBuffer(pose, vc, packedLight, OverlayTexture.NO_OVERLAY);
        }
    }
}
