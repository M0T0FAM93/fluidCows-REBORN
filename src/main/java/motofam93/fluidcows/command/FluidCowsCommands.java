package motofam93.fluidcows.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import motofam93.fluidcows.FluidCowConfigGenerator;
import motofam93.fluidcows.ModRegistries;
import motofam93.fluidcows.entity.FluidCowEntity;
import motofam93.fluidcows.util.EnabledFluids;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.HashSet;

public final class FluidCowsCommands {
    private FluidCowsCommands() {}

    private static final SuggestionProvider<CommandSourceStack> ENABLED_FLUID_SUGGESTIONS = (ctx, builder) -> {
        for (ResourceLocation rl : EnabledFluids.all()) {
            builder.suggest(rl.toString());
        }
        return builder.buildFuture();
    };

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("fluidcows")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("reload").executes(ctx -> {
                    EnabledFluids.reloadFromDisk();
                    motofam93.fluidcows.util.BreedingManager.reload();
                    ctx.getSource().sendSuccess(() -> Component.literal("[FluidCows] Reloaded configs."), true);
                    return 1;
                }))
                .then(Commands.literal("regen").executes(ctx -> {
                    FluidCowConfigGenerator.generateAll();
                    EnabledFluids.reloadFromDisk();
                    motofam93.fluidcows.util.BreedingManager.reload();
                    ctx.getSource().sendSuccess(() -> Component.literal("[FluidCows] Generated missing configs and reloaded."), true);
                    return 1;
                }))

                .then(Commands.literal("spawn")
                        .then(Commands.argument("fluid", ResourceLocationArgument.id())
                                .suggests(ENABLED_FLUID_SUGGESTIONS)
                                .executes(ctx -> spawnAt(ctx.getSource(),
                                        ResourceLocationArgument.getId(ctx, "fluid"),
                                        BlockPos.containing(ctx.getSource().getPosition())))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> spawnAt(ctx.getSource(),
                                                ResourceLocationArgument.getId(ctx, "fluid"),
                                                BlockPosArgument.getLoadedBlockPos(ctx, "pos")))
                                )
                        )
                        .then(Commands.literal("random")
                                .executes(ctx -> spawnAt(ctx.getSource(),
                                        EnabledFluids.pickRandom(ctx.getSource().getLevel().random),
                                        BlockPos.containing(ctx.getSource().getPosition())))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> spawnAt(ctx.getSource(),
                                                EnabledFluids.pickRandom(ctx.getSource().getLevel().random),
                                                BlockPosArgument.getLoadedBlockPos(ctx, "pos")))
                                )
                        )
                )

                .then(Commands.literal("debug")
                        .then(Commands.literal("color")
                                .then(Commands.argument("fluid", ResourceLocationArgument.id())
                                        .executes(ctx -> printColors(ctx.getSource(), 
                                                ResourceLocationArgument.getId(ctx, "fluid")))
                                )
                        )
                )
        );
    }

    private static int spawnAt(CommandSourceStack src, ResourceLocation fluidId, BlockPos pos) {
        var enabled = new HashSet<>(EnabledFluids.all());
        if (!enabled.contains(fluidId)) {
            src.sendFailure(Component.literal("[FluidCows] Fluid " + fluidId + " is not enabled in configs."));
            return 0;
        }

        ServerLevel level = src.getLevel();
        FluidCowEntity cow = ModRegistries.FLUID_COW.get().create(level);
        if (cow == null) {
            src.sendFailure(Component.literal("[FluidCows] Could not create cow entity."));
            return 0;
        }

        cow.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        cow.setFluidRL(fluidId);

        var fluid = BuiltInRegistries.FLUID.get(fluidId);
        cow.setCustomName(new FluidStack(fluid, 1000).getHoverName().copy()
                .append(Component.literal(" Cow")));

        level.addFreshEntity(cow);
        src.sendSuccess(() -> Component.literal("[FluidCows] Spawned " + fluidId + " cow at "
                + pos.getX() + " " + pos.getY() + " " + pos.getZ()), true);
        return 1;
    }

    private static int printColors(CommandSourceStack src, ResourceLocation id) {
        var level = src.getLevel();
        var fluid = BuiltInRegistries.FLUID.get(id);
        if (fluid == null) {
            src.sendFailure(Component.literal("[FluidCows] Unknown fluid: " + id));
            return 0;
        }

        int mapTint;
        try {
            var state = fluid.defaultFluidState().createLegacyBlock();
            var map = state.getMapColor(level, BlockPos.containing(src.getPosition()));
            mapTint = 0xFF000000 | map.col;
        } catch (Throwable ignored) {
            mapTint = -1;
        }

        final int mapTintFinal = mapTint;
        final int hashTintFinal = hashedColor(id);

        src.sendSuccess(() -> Component.literal("[FluidCows] Color probes for " + id), false);
        src.sendSuccess(() -> coloredLine("FluidType tint", -1, "(client-only)"), false);
        src.sendSuccess(() -> coloredLine("Bucket tint", -1, "(client-only)"), false);
        src.sendSuccess(() -> coloredLine("MapColor", mapTintFinal, null), false);
        src.sendSuccess(() -> coloredLine("Hashed", hashTintFinal, null), false);
        return 1;
    }

    private static MutableComponent coloredLine(String label, int argb, String suffix) {
        String hex = (argb == -1) ? "N/A" : "0x" + String.format("%08X", argb);
        MutableComponent c = Component.literal(label + ": " + hex);
        int rgb = (argb == -1) ? 0x888888 : (argb & 0x00FFFFFF);
        return (suffix == null)
                ? c.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)))
                : c.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)))
                .append(Component.literal(" " + suffix));
    }

    private static int hashedColor(ResourceLocation id) {
        int h = id.toString().hashCode();
        int r = 64 + ((h >> 16) & 0x7F);
        int g = 64 + ((h >> 8) & 0x7F);
        int b = 64 + (h & 0x7F);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}
