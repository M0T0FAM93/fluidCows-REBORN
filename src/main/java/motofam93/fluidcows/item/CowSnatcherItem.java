package motofam93.fluidcows.item;

import motofam93.fluidcows.entity.FluidCowEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CowSnatcherItem extends Item {

    public CowSnatcherItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof FluidCowEntity cow)) {
            return InteractionResult.PASS;
        }

        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel level = (ServerLevel) player.level();

        ItemStack cowItem = FluidCowSpawnItem.withFluid(cow.getFluidRL());

        ItemEntity drop = new ItemEntity(
                level,
                cow.getX(),
                cow.getY() + 0.5,
                cow.getZ(),
                cowItem
        );
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);

        level.playSound(
                null,
                cow.getX(), cow.getY(), cow.getZ(),
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                1.0F,
                0.8F + level.random.nextFloat() * 0.4F
        );

        cow.discard();

        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click a Fluid Cow to capture it").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Drops the cow as an item").withStyle(ChatFormatting.DARK_GRAY));
    }
}
