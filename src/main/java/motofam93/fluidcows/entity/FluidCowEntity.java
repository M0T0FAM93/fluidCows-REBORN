package motofam93.fluidcows.entity;

import motofam93.fluidcows.ModRegistries;
import motofam93.fluidcows.util.EnabledFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

public class FluidCowEntity extends Cow {
    private static final ResourceLocation DEFAULT_FLUID = ResourceLocation.fromNamespaceAndPath("minecraft", "empty");
    private static final String NBT_FLUID = "Fluid";
    private static final String NBT_MILK = "MilkCooldown";

    private static final EntityDataAccessor<String> DATA_FLUID = 
        SynchedEntityData.defineId(FluidCowEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_MILK_CD = 
        SynchedEntityData.defineId(FluidCowEntity.class, EntityDataSerializers.INT);

    public FluidCowEntity(EntityType<? extends Cow> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLUID, DEFAULT_FLUID.toString());
        builder.define(DATA_MILK_CD, 0);
    }

    public ResourceLocation getFluidRL() {
        String s = this.entityData.get(DATA_FLUID);
        ResourceLocation rl = ResourceLocation.tryParse(s);
        return rl != null ? rl : DEFAULT_FLUID;
    }

    public void setFluidRL(ResourceLocation rl) {
        this.entityData.set(DATA_FLUID, (rl != null ? rl : DEFAULT_FLUID).toString());
        refreshCustomName();
    }

    public int getMilkCooldownTicks() {
        return Math.max(0, this.entityData.get(DATA_MILK_CD));
    }

    public void setMilkCooldownTicks(int ticks) {
        this.entityData.set(DATA_MILK_CD, Math.max(0, ticks));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(NBT_FLUID, getFluidRL().toString());
        tag.putInt(NBT_MILK, getMilkCooldownTicks());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(NBT_FLUID)) {
            ResourceLocation rl = ResourceLocation.tryParse(tag.getString(NBT_FLUID));
            setFluidRL(rl != null ? rl : DEFAULT_FLUID);
        } else {
            setFluidRL(DEFAULT_FLUID);
        }
        if (tag.contains(NBT_MILK)) {
            setMilkCooldownTicks(tag.getInt(NBT_MILK));
        }
        refreshCustomName();
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide) {
            if (getFluidRL().equals(DEFAULT_FLUID)) {
                setFluidRL(EnabledFluids.pickRandom(this.level().getRandom()));
            }
            refreshCustomName();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            int cd = getMilkCooldownTicks();
            if (cd > 0) setMilkCooldownTicks(cd - 1);
        }
    }

    private void refreshCustomName() {
        Fluid f = BuiltInRegistries.FLUID.get(getFluidRL());
        if (f != null && f != Fluids.EMPTY) {
            Component fluidName = new FluidStack(f, 1000).getHoverName();
            this.setCustomName(Component.empty().append(fluidName).append(Component.literal(" Cow")));
        } else {
            this.setCustomName(null);
        }
    }

    @Override
    public InteractionResult mobInteract(net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.BUCKET)) {
            if (!level().isClientSide) {
                if (getMilkCooldownTicks() > 0) {
                    return InteractionResult.SUCCESS;
                }
                Fluid f = BuiltInRegistries.FLUID.get(getFluidRL());
                if (f != null && f != Fluids.EMPTY) {
                    ItemStack filled = FluidUtil.getFilledBucket(new FluidStack(f, 1000));
                    if (!filled.isEmpty()) {
                        if (!player.getAbilities().instabuild) held.shrink(1);
                        if (!player.addItem(filled)) player.drop(filled, false);
                        this.level().levelEvent(null, 1000, this.blockPosition(), 0);

                        int milkCd = EnabledFluids.getBucketCooldownTicks(this.getFluidRL());
                        setMilkCooldownTicks(milkCd);
                        return InteractionResult.CONSUME;
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal partner) {
        super.spawnChildFromBreeding(level, partner);
        
        int selfCd = EnabledFluids.getBreedingCooldown(this.getFluidRL());
        this.setAge(selfCd);

        if (partner instanceof FluidCowEntity fc) {
            int partnerCd = EnabledFluids.getBreedingCooldown(fc.getFluidRL());
            fc.setAge(partnerCd);
        }
    }

    @Nullable
    @Override
    public Cow getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Cow child = ModRegistries.FLUID_COW.get().create(level);
        if (child instanceof FluidCowEntity fc) {
            ResourceLocation childFluid = this.getFluidRL();
            fc.setFluidRL(childFluid);

            int growTicks = EnabledFluids.getGrowthTimeTicks(childFluid);
            fc.setAge(-Math.max(0, growTicks));
        } else if (child != null) {
            child.setAge(-24000);
        }
        return child;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.TemptGoal(
                this, 1.25D,
                motofam93.fluidcows.util.BreedingManager.ingredientForParent(getFluidRL()),
                false
        ));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return motofam93.fluidcows.util.BreedingManager.isBreedingItemForParent(getFluidRL(), stack);
    }
}
