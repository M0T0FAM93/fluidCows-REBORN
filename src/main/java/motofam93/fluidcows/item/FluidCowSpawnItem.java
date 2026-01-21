package motofam93.fluidcows.item;

import motofam93.fluidcows.FluidCows;
import motofam93.fluidcows.ModRegistries;
import motofam93.fluidcows.client.render.FluidCowItemRenderer;
import motofam93.fluidcows.entity.FluidCowEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Consumer;

public class FluidCowSpawnItem extends Item {
    public static final String NBT_FLUID = "Fluid";

    public FluidCowSpawnItem(Properties props) {
        super(props);
    }

    public static ItemStack withFluid(ResourceLocation rl) {
        ItemStack stack = new ItemStack(ModRegistries.FLUID_COW_SPAWN_ITEM.get());
        setFluid(stack, rl);
        return stack;
    }

    public static void setFluid(ItemStack stack, ResourceLocation rl) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
                data -> data.update(tag -> tag.putString(NBT_FLUID, rl.toString())));
    }

    public static ResourceLocation getFluidOrNull(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        if (!tag.contains(NBT_FLUID)) return null;
        return ResourceLocation.tryParse(tag.getString(NBT_FLUID));
    }

    public static ResourceLocation getFluid(ItemStack stack) {
        ResourceLocation rl = getFluidOrNull(stack);
        return rl != null ? rl : ResourceLocation.withDefaultNamespace("water");
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation fluidId = getFluid(stack);
        var fluid = BuiltInRegistries.FLUID.get(fluidId);
        FluidStack fluidStack = new FluidStack(fluid, 1000);
        Component fluidName = fluidStack.getHoverName();
        String nameStr = fluidName.getString();
        
        // Check if it's an unlocalized key
        if (nameStr.startsWith("fluid.") || nameStr.startsWith("block.") || 
            nameStr.startsWith("item.") || nameStr.startsWith("fluid_type.")) {
            String niceName = formatFluidName(fluidId.getPath());
            return Component.literal(niceName + " Cow");
        }
        
        return Component.empty().append(fluidName).append(Component.literal(" Cow"));
    }

    /**
     * Converts a fluid path like "molten_iron" to "Molten Iron"
     */
    private static String formatFluidName(String path) {
        path = path.replace("flowing_", "")
                   .replace("fluid_", "")
                   .replace("_fluid", "")
                   .replace("_still", "");
        
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use on block: Spawn").withStyle(ChatFormatting.GRAY));
        if (ModList.get().isLoaded("jei")) {
            tooltip.add(Component.literal("JEI: R = Breeding,  U = Info").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (getFluidOrNull(stack) == null) {
            tooltip.add(Component.literal("Invalid cow item (no fluid set)").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();

        ResourceLocation fluidId = getFluidOrNull(stack);
        if (fluidId == null || BuiltInRegistries.FLUID.get(fluidId) == null) {
            if (player != null && !level.isClientSide) {
                player.displayClientMessage(
                        Component.literal("[FluidCows] This cow item has no valid fluid.")
                                .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }

        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        BlockPos pos = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();
        BlockPos spawnAt = pos.relative(face);

        Cow base = ModRegistries.FLUID_COW.get().create(server);
        if (!(base instanceof FluidCowEntity fc)) {
            return InteractionResult.FAIL;
        }

        fc.setFluidRL(fluidId);

        BlockState state = server.getBlockState(spawnAt);
        double y = state.getCollisionShape(server, spawnAt).isEmpty()
                ? spawnAt.getY() : spawnAt.getY() + 1;
        fc.moveTo(spawnAt.getX() + 0.5D, y, spawnAt.getZ() + 0.5D,
                player != null ? player.getYRot() : 0F, 0.0F);

        server.addFreshEntity(fc);

        server.getServer().execute(() -> {
            if (!fc.isRemoved() && !fluidId.equals(fc.getFluidRL())) {
                FluidCows.LOGGER.warn("Spawned cow fluid changed on first tick: {} -> {}. Forcing back.",
                        fluidId, fc.getFluidRL());
                fc.setFluidRL(fluidId);
            }
        });

        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return FluidCowItemRenderer.INSTANCE;
            }
        });
    }
}
