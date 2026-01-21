package motofam93.fluidcows.client.gui;

import motofam93.fluidcows.util.EnabledFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class FluidCowEditScreen extends Screen {

    private final Screen parent;
    private final ResourceLocation fluidId;
    private FluidCowConfig config;

    private CycleButton<Boolean> enabledButton;
    private EditBox spawnWeightBox;
    private EditBox breedingCooldownBox;
    private EditBox growthTimeBox;
    private EditBox bucketCooldownBox;
    private EditBox breedingChanceBox;
    private DropdownWidget<ResourceLocation> parent1Dropdown;
    private DropdownWidget<ResourceLocation> parent2Dropdown;
    private ItemSearchWidget breedingItemSearch;
    private boolean hasChanges = false;

    public FluidCowEditScreen(Screen parent, ResourceLocation fluidId) {
        super(Component.literal("Edit: " + getFluidDisplayName(fluidId)));
        this.parent = parent;
        this.fluidId = fluidId;
    }

    private static String getFluidDisplayName(ResourceLocation fluidId) {
        Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
        if (fluid != null && fluid != Fluids.EMPTY) {
            String name = new FluidStack(fluid, 1000).getHoverName().getString();
            if (!name.startsWith("fluid.") && !name.startsWith("block.") && !name.startsWith("fluid_type.")) {
                return name;
            }
        }
        String path = fluidId.getPath().replace("flowing_", "").replace("fluid_", "").replace("_fluid", "").replace("_still", "");
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

    @Override
    protected void init() {
        config = FluidCowConfig.load(fluidId);

        int centerX = this.width / 2;
        int leftCol = centerX - 155;
        int rightCol = centerX + 15;
        int y = 48;
        int rowHeight = 25;
        int fieldWidth = 150;

        enabledButton = CycleButton.onOffBuilder(config.enabled)
                .create(leftCol, y, fieldWidth, 20, Component.literal("Enabled"), (btn, val) -> {
                    config.enabled = val;
                    hasChanges = true;
                });
        this.addRenderableWidget(enabledButton);
        y += rowHeight;

        spawnWeightBox = new EditBox(this.font, leftCol, y, fieldWidth, 20, Component.literal("Spawn Weight"));
        spawnWeightBox.setValue(String.valueOf(config.spawnWeight));
        spawnWeightBox.setTooltip(Tooltip.create(Component.literal("Higher = more common spawns")));
        spawnWeightBox.setResponder(s -> hasChanges = true);
        this.addRenderableWidget(spawnWeightBox);
        y += rowHeight;

        breedingCooldownBox = new EditBox(this.font, leftCol, y, fieldWidth, 20, Component.literal("Breeding Cooldown"));
        breedingCooldownBox.setValue(FluidCowConfig.formatTicks(config.breedingCooldownTicks));
        breedingCooldownBox.setTooltip(Tooltip.create(Component.literal("Time before cow can breed again")));
        breedingCooldownBox.setResponder(s -> hasChanges = true);
        this.addRenderableWidget(breedingCooldownBox);
        y += rowHeight;

        growthTimeBox = new EditBox(this.font, leftCol, y, fieldWidth, 20, Component.literal("Growth Time"));
        growthTimeBox.setValue(FluidCowConfig.formatTicks(config.growthTimeTicks));
        growthTimeBox.setTooltip(Tooltip.create(Component.literal("Time for baby to grow up")));
        growthTimeBox.setResponder(s -> hasChanges = true);
        this.addRenderableWidget(growthTimeBox);
        y += rowHeight;

        bucketCooldownBox = new EditBox(this.font, leftCol, y, fieldWidth, 20, Component.literal("Bucket Cooldown"));
        bucketCooldownBox.setValue(FluidCowConfig.formatTicks(config.bucketCooldownTicks));
        bucketCooldownBox.setTooltip(Tooltip.create(Component.literal("Time before cow can be milked again")));
        bucketCooldownBox.setResponder(s -> hasChanges = true);
        this.addRenderableWidget(bucketCooldownBox);

        y = 48;

        breedingItemSearch = new ItemSearchWidget(this.font, rightCol, y, fieldWidth, 20, Component.literal("Breeding Item"), this);
        breedingItemSearch.setValue(config.breedingItem);
        breedingItemSearch.setResponder(s -> hasChanges = true);
        this.addRenderableWidget(breedingItemSearch);
        y += rowHeight;

        breedingChanceBox = new EditBox(this.font, rightCol, y, fieldWidth, 20, Component.literal("Breeding Chance"));
        breedingChanceBox.setValue(String.valueOf(config.breedingChance));
        breedingChanceBox.setTooltip(Tooltip.create(Component.literal("% chance to get this cow when breeding parents")));
        breedingChanceBox.setResponder(s -> hasChanges = true);
        this.addRenderableWidget(breedingChanceBox);
        y += rowHeight;

        parent1Dropdown = new DropdownWidget<>(this.font, rightCol, y, fieldWidth, 20,
                Component.literal("Parent 1"), this, this::getAvailableFluids, this::formatFluidOption);
        parent1Dropdown.setValue(config.parent1.isEmpty() ? null : ResourceLocation.tryParse(config.parent1));
        parent1Dropdown.setResponder(rl -> hasChanges = true);
        this.addRenderableWidget(parent1Dropdown);
        y += rowHeight;

        parent2Dropdown = new DropdownWidget<>(this.font, rightCol, y, fieldWidth, 20,
                Component.literal("Parent 2"), this, this::getAvailableFluids, this::formatFluidOption);
        parent2Dropdown.setValue(config.parent2.isEmpty() ? null : ResourceLocation.tryParse(config.parent2));
        parent2Dropdown.setResponder(rl -> hasChanges = true);
        this.addRenderableWidget(parent2Dropdown);

        int buttonY = this.height - 30;

        this.addRenderableWidget(Button.builder(Component.literal("Save & Apply"), b -> saveAndApply())
                .bounds(centerX - 155, buttonY, 100, 20)
                .tooltip(Tooltip.create(Component.literal("Save changes and reload configs"))).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(centerX - 50, buttonY, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reset"), b -> resetToDefaults())
                .bounds(centerX + 55, buttonY, 100, 20)
                .tooltip(Tooltip.create(Component.literal("Reset to default values"))).build());
    }

    private List<ResourceLocation> getAvailableFluids() {
        List<ResourceLocation> fluids = new ArrayList<>();
        fluids.add(null);
        for (ResourceLocation rl : EnabledFluids.all()) {
            if (!rl.equals(fluidId)) fluids.add(rl);
        }
        fluids.sort((a, b) -> {
            if (a == null) return -1;
            if (b == null) return 1;
            return formatFluidOption(a).compareToIgnoreCase(formatFluidOption(b));
        });
        return fluids;
    }

    private String formatFluidOption(ResourceLocation rl) {
        if (rl == null) return "(None)";
        return getFluidDisplayName(rl);
    }

    private void saveAndApply() {
        try {
            config.enabled = enabledButton.getValue();
            config.spawnWeight = Math.max(0, Integer.parseInt(spawnWeightBox.getValue().trim()));
            config.breedingCooldownTicks = FluidCowConfig.parseTicks(breedingCooldownBox.getValue());
            config.growthTimeTicks = FluidCowConfig.parseTicks(growthTimeBox.getValue());
            config.bucketCooldownTicks = FluidCowConfig.parseTicks(bucketCooldownBox.getValue());
            config.breedingChance = Math.max(0, Math.min(100, Integer.parseInt(breedingChanceBox.getValue().trim())));
            config.breedingItem = breedingItemSearch.getValue();
            config.parent1 = parent1Dropdown.getValue() != null ? parent1Dropdown.getValue().toString() : "";
            config.parent2 = parent2Dropdown.getValue() != null ? parent2Dropdown.getValue().toString() : "";

            if (config.save()) {
                FluidCowConfig.reloadAll();
                hasChanges = false;
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§aConfig saved! Spawning/breeding updated. §7(JEI needs world rejoin)"), false);
                onClose();
            } else {
                Minecraft.getInstance().player.displayClientMessage(Component.literal("§cFailed to save config!"), false);
            }
        } catch (NumberFormatException e) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("§cInvalid number format!"), false);
        }
    }

    private void resetToDefaults() {
        enabledButton.setValue(true);
        spawnWeightBox.setValue("1");
        breedingCooldownBox.setValue("5m 0s");
        growthTimeBox.setValue("20m 0s");
        bucketCooldownBox.setValue("3m 20s");
        breedingChanceBox.setValue("33");
        breedingItemSearch.setValue("minecraft:wheat");
        parent1Dropdown.setValue(null);
        parent2Dropdown.setValue(null);
        hasChanges = true;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "§7" + fluidId.toString(), this.width / 2, 30, 0xAAAAAA);

        int leftLabelX = this.width / 2 - 230;
        int rightLabelX = this.width / 2 + 170;
        int y = 53;
        int rowHeight = 25;

        graphics.drawString(this.font, "Enabled:", leftLabelX, y, 0xFFFFFF);
        y += rowHeight;
        graphics.drawString(this.font, "Spawn Weight:", leftLabelX, y, 0xFFFFFF);
        y += rowHeight;
        graphics.drawString(this.font, "Breed CD:", leftLabelX, y, 0xFFFFFF);
        y += rowHeight;
        graphics.drawString(this.font, "Growth:", leftLabelX, y, 0xFFFFFF);
        y += rowHeight;
        graphics.drawString(this.font, "Milk CD:", leftLabelX, y, 0xFFFFFF);

        y = 53;
        graphics.drawString(this.font, "Breed Item:", rightLabelX, y, 0xFFFFFF);
        y += rowHeight;
        graphics.drawString(this.font, "Chance %:", rightLabelX, y, 0xFFFFFF);
        y += rowHeight;
        graphics.drawString(this.font, "Parent 1:", rightLabelX, y, 0xFFFFFF);
        y += rowHeight;
        graphics.drawString(this.font, "Parent 2:", rightLabelX, y, 0xFFFFFF);

        if (hasChanges) {
            graphics.drawCenteredString(this.font, "§e* Unsaved changes", this.width / 2, this.height - 45, 0xFFFFFF);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (parent1Dropdown != null && parent1Dropdown.isOpen() && !parent1Dropdown.isMouseOver(mouseX, mouseY)) parent1Dropdown.close();
        if (parent2Dropdown != null && parent2Dropdown.isOpen() && !parent2Dropdown.isMouseOver(mouseX, mouseY)) parent2Dropdown.close();
        if (breedingItemSearch != null && breedingItemSearch.isOpen() && !breedingItemSearch.isMouseOver(mouseX, mouseY)) breedingItemSearch.close();
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
