package motofam93.fluidcows.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidDripParticle extends TextureSheetParticle {

    private FluidDripParticle(ClientLevel level, double x, double y, double z, Fluid fluid) {
        super(level, x, y, z);
        this.setSize(0.01F, 0.01F);
        this.gravity = 1.2F;
        this.lifetime = 40;
        this.quadSize = 0.03F;
        setFluidAppearance(fluid);
        this.xd = 0;
        this.yd = -0.05;
        this.zd = 0;
    }

    private void setFluidAppearance(Fluid fluid) {
        try {
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
            FluidStack stack = new FluidStack(fluid, 1000);

            ResourceLocation stillTexture = ext.getStillTexture(stack);
            if (stillTexture != null) {
                TextureAtlasSprite sprite = Minecraft.getInstance()
                        .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(stillTexture);
                this.setSprite(sprite);
            }

            int tint = ext.getTintColor(stack);
            float a = ((tint >> 24) & 0xFF) / 255.0F;
            float r = ((tint >> 16) & 0xFF) / 255.0F;
            float g = ((tint >> 8) & 0xFF) / 255.0F;
            float b = (tint & 0xFF) / 255.0F;

            this.setColor(r, g, b);
            if (a > 0) this.alpha = a;
        } catch (Throwable t) {
            this.setColor(1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.yd -= this.gravity * 0.04;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.98;
        this.zd *= 0.98;

        if (this.onGround) this.remove();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    public static void spawn(ClientLevel level, double x, double y, double z, ResourceLocation fluidRL) {
        Fluid fluid = BuiltInRegistries.FLUID.get(fluidRL);
        if (fluid == null || fluid == Fluids.EMPTY) return;
        Particle particle = new FluidDripParticle(level, x, y, z, fluid);
        Minecraft.getInstance().particleEngine.add(particle);
    }
}
