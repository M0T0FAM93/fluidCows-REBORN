package motofam93.fluidcows.client.gui;

import motofam93.fluidcows.FluidCowConfigGenerator;
import motofam93.fluidcows.util.EnabledFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FluidCowConfigScreen extends Screen {

    private final Screen parent;
    private List<ResourceLocation> allFluids;
    private List<ResourceLocation> filteredFluids;
    private EditBox searchBox;
    private int scrollOffset = 0;
    private static final int ITEMS_PER_PAGE = 10;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 2;

    public FluidCowConfigScreen(Screen parent) {
        super(Component.literal("Fluid Cows Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        allFluids = new ArrayList<>();
        Path configRoot = FluidCowConfigGenerator.configRoot();

        if (Files.isDirectory(configRoot)) {
            try (var namespaces = Files.list(configRoot)) {
                namespaces.filter(Files::isDirectory).forEach(nsPath -> {
                    String namespace = nsPath.getFileName().toString();
                    try (var files = Files.walk(nsPath)) {
                        files.filter(p -> p.toString().endsWith(".json"))
                             .forEach(file -> {
                                 Path rel = nsPath.relativize(file);
                                 String pathStr = rel.toString().replace('\\', '/');
                                 if (pathStr.endsWith(".json")) pathStr = pathStr.substring(0, pathStr.length() - 5);
                                 allFluids.add(ResourceLocation.fromNamespaceAndPath(namespace, pathStr));
                             });
                    } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
        }

        allFluids.sort(Comparator.comparing(ResourceLocation::toString));
        filteredFluids = new ArrayList<>(allFluids);

        int centerX = this.width / 2;
        int topY = 40;

        searchBox = new EditBox(this.font, centerX - 150, topY, 300, 20, Component.literal("Search..."));
        searchBox.setHint(Component.literal("Search fluids..."));
        searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(searchBox);

        this.addRenderableWidget(Button.builder(Component.literal("▲"), b -> scroll(-1))
                .bounds(centerX + 160, topY + 30, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("▼"), b -> scroll(1))
                .bounds(centerX + 160, topY + 30 + (ITEMS_PER_PAGE * (BUTTON_HEIGHT + BUTTON_SPACING)) - 20, 20, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(centerX - 50, this.height - 30, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reload All"), b -> {
            FluidCowConfig.reloadAll();
            Minecraft.getInstance().player.displayClientMessage(Component.literal("§aFluid Cows configs reloaded!"), false);
        }).bounds(centerX - 155, this.height - 30, 100, 20)
          .tooltip(Tooltip.create(Component.literal("Reload all configs from disk"))).build());

        rebuildFluidButtons();
    }

    private void onSearchChanged(String text) {
        scrollOffset = 0;
        if (text == null || text.isEmpty()) {
            filteredFluids = new ArrayList<>(allFluids);
        } else {
            String lower = text.toLowerCase();
            filteredFluids = allFluids.stream()
                    .filter(rl -> {
                        if (rl.toString().toLowerCase().contains(lower)) return true;
                        Fluid fluid = BuiltInRegistries.FLUID.get(rl);
                        if (fluid != null) {
                            String name = new FluidStack(fluid, 1000).getHoverName().getString().toLowerCase();
                            return name.contains(lower);
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }
        rebuildFluidButtons();
    }

    private void scroll(int direction) {
        int maxScroll = Math.max(0, filteredFluids.size() - ITEMS_PER_PAGE);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + direction));
        rebuildFluidButtons();
    }

    private void rebuildFluidButtons() {
        this.children().removeIf(w -> w instanceof FluidButton);
        this.renderables.removeIf(w -> w instanceof FluidButton);

        int centerX = this.width / 2;
        int startY = 70;

        for (int i = 0; i < ITEMS_PER_PAGE && (scrollOffset + i) < filteredFluids.size(); i++) {
            ResourceLocation rl = filteredFluids.get(scrollOffset + i);
            int y = startY + i * (BUTTON_HEIGHT + BUTTON_SPACING);
            FluidButton btn = new FluidButton(centerX - 150, y, 300, BUTTON_HEIGHT, rl,
                    b -> Minecraft.getInstance().setScreen(new FluidCowEditScreen(this, rl)));
            this.addRenderableWidget(btn);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        String countText = filteredFluids.size() + " fluids" +
                (scrollOffset > 0 ? " (scroll: " + (scrollOffset + 1) + "-" +
                        Math.min(scrollOffset + ITEMS_PER_PAGE, filteredFluids.size()) + ")" : "");
        graphics.drawCenteredString(this.font, countText, this.width / 2, 28, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll(scrollY > 0 ? -1 : 1);
        return true;
    }

    private class FluidButton extends Button {
        private final ResourceLocation fluidId;

        public FluidButton(int x, int y, int width, int height, ResourceLocation fluidId, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.fluidId = fluidId;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);

            Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
            String name;
            if (fluid != null && fluid != Fluids.EMPTY) {
                name = new FluidStack(fluid, 1000).getHoverName().getString();
                if (name.startsWith("fluid.") || name.startsWith("block.") || name.startsWith("fluid_type.")) {
                    name = formatFluidName(fluidId.getPath());
                }
            } else {
                name = formatFluidName(fluidId.getPath());
            }

            boolean enabled = EnabledFluids.isEnabled(fluidId);
            graphics.drawString(font, enabled ? "§a✓" : "§c✗", getX() + 5, getY() + 6, 0xFFFFFF);

            String displayText = name + " §7(" + fluidId.getNamespace() + ")";
            if (font.width(displayText) > width - 30) displayText = font.plainSubstrByWidth(displayText, width - 35) + "...";
            graphics.drawString(font, displayText, getX() + 20, getY() + 6, 0xFFFFFF);
        }

        private String formatFluidName(String path) {
            path = path.replace("flowing_", "").replace("fluid_", "").replace("_fluid", "").replace("_still", "");
            String[] parts = path.split("_");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(Character.toUpperCase(part.charAt(0)));
                    if (part.length() > 1) sb.append(part.substring(1).toLowerCase());
                }
            }
            return sb.toString();
        }
    }
}
