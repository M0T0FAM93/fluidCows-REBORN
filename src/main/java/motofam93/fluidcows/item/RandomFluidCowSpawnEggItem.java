package motofam93.fluidcows.item;

import motofam93.fluidcows.ModRegistries;
import motofam93.fluidcows.entity.FluidCowEntity;
import motofam93.fluidcows.util.EnabledFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidStack;

public class RandomFluidCowSpawnEggItem extends Item {

    public RandomFluidCowSpawnEggItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ServerLevel server = (ServerLevel) level;
        BlockPos clicked = ctx.getClickedPos().relative(ctx.getClickedFace());
        BlockState stateAt = level.getBlockState(clicked);

        double x = clicked.getX() + 0.5D;
        double y = stateAt.getCollisionShape(level, clicked).isEmpty() ? clicked.getY() : clicked.getY() + 1;
        double z = clicked.getZ() + 0.5D;

        FluidCowEntity cow = ModRegistries.FLUID_COW.get().create(server);
        if (cow == null) return InteractionResult.PASS;

        cow.moveTo(x, y, z, server.random.nextFloat() * 360.0F, 0.0F);

        ResourceLocation fluid = EnabledFluids.pickRandom(server.getRandom());
        cow.setFluidRL(fluid);

        var fluidName = new FluidStack(BuiltInRegistries.FLUID.get(fluid), 1000).getHoverName().copy();
        cow.setCustomName(Component.empty().append(fluidName).append(Component.literal(" Cow")));

        server.addFreshEntity(cow);
        server.gameEvent(null, GameEvent.ENTITY_PLACE, clicked);

        ItemStack stack = ctx.getItemInHand();
        Player player = ctx.getPlayer();
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        ItemStack stack = player.getItemInHand(hand);

        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        UseOnContext ctx = new UseOnContext(player, hand, hit);
        InteractionResult result = this.useOn(ctx);
        return new InteractionResultHolder<>(result, stack);
    }
}
