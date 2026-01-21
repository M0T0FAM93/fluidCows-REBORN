package motofam93.fluidcows.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import motofam93.fluidcows.FluidCowConfigGenerator;
import motofam93.fluidcows.ModRegistries;
import motofam93.fluidcows.entity.FluidCowEntity;
import motofam93.fluidcows.util.BreedingManager;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class FluidCowsCommands {
    private FluidCowsCommands() {}

    private static final SuggestionProvider<CommandSourceStack> ENABLED_FLUID_SUGGESTIONS = (ctx, builder) -> {
        for (ResourceLocation rl : EnabledFluids.all()) builder.suggest(rl.toString());
        return builder.buildFuture();
    };

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("fluidcows")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("config").executes(ctx -> {
                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                        player.sendSystemMessage(Component.literal("§aPress F7 to open the Fluid Cows config GUI."));
                    }
                    return 1;
                }))
                .then(Commands.literal("reload").executes(ctx -> {
                    EnabledFluids.reloadFromDisk();
                    BreedingManager.reload();
                    ctx.getSource().sendSuccess(() -> Component.literal("[FluidCows] Reloaded configs."), true);
                    return 1;
                }))
                .then(Commands.literal("regen").executes(ctx -> {
                    FluidCowConfigGenerator.generateAll();
                    EnabledFluids.reloadFromDisk();
                    BreedingManager.reload();
                    ctx.getSource().sendSuccess(() -> Component.literal("[FluidCows] Generated missing configs and reloaded."), true);
                    return 1;
                }))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("fluid", ResourceLocationArgument.id())
                                .suggests(ENABLED_FLUID_SUGGESTIONS)
                                .executes(ctx -> spawnAt(ctx.getSource(), ResourceLocationArgument.getId(ctx, "fluid"), BlockPos.containing(ctx.getSource().getPosition())))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> spawnAt(ctx.getSource(), ResourceLocationArgument.getId(ctx, "fluid"), BlockPosArgument.getLoadedBlockPos(ctx, "pos")))))
                        .then(Commands.literal("random")
                                .executes(ctx -> spawnAt(ctx.getSource(), EnabledFluids.pickRandom(ctx.getSource().getLevel().random), BlockPos.containing(ctx.getSource().getPosition())))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> spawnAt(ctx.getSource(), EnabledFluids.pickRandom(ctx.getSource().getLevel().random), BlockPosArgument.getLoadedBlockPos(ctx, "pos"))))))
                .then(Commands.literal("debug")
                        .then(Commands.literal("color")
                                .then(Commands.argument("fluid", ResourceLocationArgument.id())
                                        .executes(ctx -> printColors(ctx.getSource(), ResourceLocationArgument.getId(ctx, "fluid")))))
                        .then(Commands.literal("info")
                                .then(Commands.argument("fluid", ResourceLocationArgument.id())
                                        .suggests(ENABLED_FLUID_SUGGESTIONS)
                                        .executes(ctx -> printFluidInfo(ctx.getSource(), ResourceLocationArgument.getId(ctx, "fluid")))))
                        .then(Commands.literal("nearby")
                                .executes(ctx -> inspectNearbyCows(ctx.getSource(), 16))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                                        .executes(ctx -> inspectNearbyCows(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
                        .then(Commands.literal("validate").executes(ctx -> validateAllFluids(ctx.getSource())))
                        .then(Commands.literal("problematic").executes(ctx -> listProblematicFluids(ctx.getSource())))));
    }

    private static int spawnAt(CommandSourceStack src, ResourceLocation fluidId, BlockPos pos) {
        if (!new HashSet<>(EnabledFluids.all()).contains(fluidId)) {
            src.sendFailure(Component.literal("[FluidCows] Fluid " + fluidId + " is not enabled."));
            return 0;
        }

        ServerLevel level = src.getLevel();
        FluidCowEntity cow = ModRegistries.FLUID_COW.get().create(level);
        if (cow == null) {
            src.sendFailure(Component.literal("[FluidCows] Could not create cow entity."));
            return 0;
        }

        cow.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        cow.setFluidRL(fluidId);
        cow.setCustomName(new FluidStack(BuiltInRegistries.FLUID.get(fluidId), 1000).getHoverName().copy().append(Component.literal(" Cow")));
        level.addFreshEntity(cow);
        src.sendSuccess(() -> Component.literal("[FluidCows] Spawned " + fluidId + " cow at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()), true);
        return 1;
    }

    private static int printColors(CommandSourceStack src, ResourceLocation id) {
        Fluid fluid = BuiltInRegistries.FLUID.get(id);
        if (fluid == null) {
            src.sendFailure(Component.literal("[FluidCows] Unknown fluid: " + id));
            return 0;
        }

        int mapTint;
        try {
            var state = fluid.defaultFluidState().createLegacyBlock();
            mapTint = 0xFF000000 | state.getMapColor(src.getLevel(), BlockPos.containing(src.getPosition())).col;
        } catch (Throwable ignored) {
            mapTint = -1;
        }

        final int finalMapTint = mapTint;
        final int hashTint = hashedColor(id);

        src.sendSuccess(() -> Component.literal("[FluidCows] Color probes for " + id), false);
        src.sendSuccess(() -> coloredLine("MapColor", finalMapTint, null), false);
        src.sendSuccess(() -> coloredLine("Hashed", hashTint, null), false);
        return 1;
    }

    private static int printFluidInfo(CommandSourceStack src, ResourceLocation id) {
        Fluid fluid = BuiltInRegistries.FLUID.get(id);
        src.sendSuccess(() -> Component.literal("§6=== Fluid Debug Info: " + id + " ==="), false);

        if (fluid == null || fluid == Fluids.EMPTY) {
            src.sendSuccess(() -> Component.literal("§c  Registry: NOT FOUND"), false);
            return 0;
        }

        src.sendSuccess(() -> Component.literal("§a  Registry: Found"), false);
        src.sendSuccess(() -> Component.literal("§7  Class: " + fluid.getClass().getName()), false);

        boolean enabled = EnabledFluids.isEnabled(id);
        src.sendSuccess(() -> Component.literal(enabled ? "§a  Enabled: Yes" : "§c  Enabled: No"), false);

        try {
            String name = new FluidStack(fluid, 1000).getHoverName().getString();
            src.sendSuccess(() -> Component.literal("§7  Display Name: §f" + name), false);
        } catch (Throwable t) {
            src.sendSuccess(() -> Component.literal("§c  Display Name: ERROR"), false);
        }

        try {
            var bucket = fluid.getBucket();
            if (bucket != null && bucket != Items.AIR) {
                src.sendSuccess(() -> Component.literal("§a  Bucket: " + BuiltInRegistries.ITEM.getKey(bucket)), false);
            } else {
                src.sendSuccess(() -> Component.literal("§e  Bucket: None"), false);
            }
        } catch (Throwable t) {
            src.sendSuccess(() -> Component.literal("§c  Bucket: ERROR"), false);
        }

        if (enabled) {
            src.sendSuccess(() -> Component.literal("§7  Weight: " + EnabledFluids.getWeight(id)), false);
            src.sendSuccess(() -> Component.literal("§7  Breed CD: " + EnabledFluids.getBreedingCooldown(id) + "t"), false);
            src.sendSuccess(() -> Component.literal("§7  Growth: " + EnabledFluids.getGrowthTimeTicks(id) + "t"), false);
            src.sendSuccess(() -> Component.literal("§7  Milk CD: " + EnabledFluids.getBucketCooldownTicks(id) + "t"), false);
        }

        return 1;
    }

    private static int inspectNearbyCows(CommandSourceStack src, int radius) {
        ServerLevel level = src.getLevel();
        AABB box = new AABB(BlockPos.containing(src.getPosition())).inflate(radius);
        List<FluidCowEntity> cows = level.getEntitiesOfClass(FluidCowEntity.class, box);

        if (cows.isEmpty()) {
            src.sendSuccess(() -> Component.literal("§e[FluidCows] No fluid cows within " + radius + " blocks."), false);
            return 0;
        }

        src.sendSuccess(() -> Component.literal("§6=== Found " + cows.size() + " Fluid Cows ==="), false);

        for (FluidCowEntity cow : cows) {
            ResourceLocation fluidRL = cow.getFluidRL();
            Fluid fluid = BuiltInRegistries.FLUID.get(fluidRL);
            String fluidName;
            boolean problematic = false;

            try {
                fluidName = new FluidStack(fluid, 1000).getHoverName().getString();
                if (fluidName.chars().anyMatch(c -> c < 32 || c > 126)) problematic = true;
            } catch (Throwable t) {
                fluidName = "ERROR";
                problematic = true;
            }

            if (fluid == null || fluid == Fluids.EMPTY) problematic = true;

            String color = problematic ? "§c" : "§a";
            String finalName = fluidName;
            boolean finalProblematic = problematic;
            src.sendSuccess(() -> Component.literal(color + "  • " + fluidRL + " §7(\"" + finalName + "\")" + (finalProblematic ? " §c⚠" : "")), false);
            src.sendSuccess(() -> Component.literal("§8    at " + String.format("%.1f, %.1f, %.1f", cow.getX(), cow.getY(), cow.getZ())), false);
        }

        return cows.size();
    }

    private static int validateAllFluids(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("§6=== Validating Enabled Fluids ==="), false);

        int valid = 0, invalid = 0;
        List<ResourceLocation> problems = new ArrayList<>();

        for (ResourceLocation rl : EnabledFluids.all()) {
            Fluid fluid = BuiltInRegistries.FLUID.get(rl);
            boolean isValid = true;
            StringBuilder issues = new StringBuilder();

            if (fluid == null || fluid == Fluids.EMPTY) {
                isValid = false;
                issues.append("not in registry, ");
            } else {
                try {
                    String name = new FluidStack(fluid, 1000).getHoverName().getString();
                    if (name.chars().anyMatch(c -> c < 32 || c > 126) || name.isEmpty() || name.contains("�")) {
                        isValid = false;
                        issues.append("bad name, ");
                    }
                } catch (Throwable t) {
                    isValid = false;
                    issues.append("name error, ");
                }
            }

            if (isValid) {
                valid++;
            } else {
                invalid++;
                problems.add(rl);
                String issueStr = issues.toString();
                if (issueStr.endsWith(", ")) issueStr = issueStr.substring(0, issueStr.length() - 2);
                String finalIssueStr = issueStr;
                src.sendSuccess(() -> Component.literal("§c  ✗ " + rl + " §7(" + finalIssueStr + ")"), false);
            }
        }

        int fv = valid, fi = invalid;
        src.sendSuccess(() -> Component.literal("§6=== §a" + fv + " valid§6, §c" + fi + " problematic ==="), false);
        return invalid;
    }

    private static int listProblematicFluids(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("§6=== Problematic Fluids ==="), false);
        int count = 0;

        for (ResourceLocation rl : EnabledFluids.all()) {
            Fluid fluid = BuiltInRegistries.FLUID.get(rl);

            if (fluid == null || fluid == Fluids.EMPTY) {
                src.sendSuccess(() -> Component.literal("§c  " + rl + " §7- not in registry"), false);
                count++;
                continue;
            }

            try {
                String name = new FluidStack(fluid, 1000).getHoverName().getString();
                if (name.chars().anyMatch(c -> c < 32 || c > 126) || name.contains("�")) {
                    src.sendSuccess(() -> Component.literal("§c  " + rl + " §7- bad name"), false);
                    count++;
                }
            } catch (Throwable t) {
                src.sendSuccess(() -> Component.literal("§c  " + rl + " §7- error"), false);
                count++;
            }
        }

        if (count == 0) src.sendSuccess(() -> Component.literal("§a  None found!"), false);
        return count;
    }

    private static MutableComponent coloredLine(String label, int argb, String suffix) {
        String hex = (argb == -1) ? "N/A" : "0x" + String.format("%08X", argb);
        MutableComponent c = Component.literal(label + ": " + hex);
        int rgb = (argb == -1) ? 0x888888 : (argb & 0x00FFFFFF);
        return suffix == null ? c.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))) : c.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))).append(Component.literal(" " + suffix));
    }

    private static int hashedColor(ResourceLocation id) {
        int h = id.toString().hashCode();
        return (0xFF << 24) | ((64 + ((h >> 16) & 0x7F)) << 16) | ((64 + ((h >> 8) & 0x7F)) << 8) | (64 + (h & 0x7F));
    }
}
