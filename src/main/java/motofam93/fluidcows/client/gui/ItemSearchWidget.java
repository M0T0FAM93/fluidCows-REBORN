package motofam93.fluidcows.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ItemSearchWidget extends AbstractWidget {

    private final Font font;
    private String value = "";
    private String displayText = "";
    private boolean isOpen = false;
    private boolean isFocused = false;
    private int scrollOffset = 0;
    private int cursorPos = 0;
    private int cursorBlink = 0;
    private Consumer<String> responder;
    private List<ResourceLocation> filteredItems = new ArrayList<>();

    private static final int MAX_VISIBLE = 8;
    private static final int OPTION_HEIGHT = 18;

    public ItemSearchWidget(Font font, int x, int y, int width, int height, Component label, Object parentScreen) {
        super(x, y, width, height, label);
        this.font = font;
    }

    public void setValue(String value) {
        this.value = value != null ? value : "";
        this.displayText = this.value;
        this.cursorPos = this.value.length();
        updateFilteredItems();
    }

    public String getValue() { return value; }
    public void setResponder(Consumer<String> responder) { this.responder = responder; }
    public boolean isOpen() { return isOpen; }
    public void close() { isOpen = false; }

    private void updateFilteredItems() {
        String search = displayText.toLowerCase();
        if (search.isEmpty()) {
            filteredItems.clear();
            return;
        }

        filteredItems = BuiltInRegistries.ITEM.keySet().stream()
                .filter(rl -> {
                    if (rl.toString().toLowerCase().contains(search)) return true;
                    Item item = BuiltInRegistries.ITEM.get(rl);
                    if (item != null && item != Items.AIR) {
                        return new ItemStack(item).getHoverName().getString().toLowerCase().contains(search);
                    }
                    return false;
                })
                .sorted((a, b) -> {
                    boolean aExact = a.toString().toLowerCase().startsWith(search);
                    boolean bExact = b.toString().toLowerCase().startsWith(search);
                    if (aExact && !bExact) return -1;
                    if (bExact && !aExact) return 1;
                    return a.toString().compareTo(b.toString());
                })
                .limit(50)
                .collect(Collectors.toList());

        scrollOffset = 0;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        cursorBlink++;

        int bgColor = isFocused ? 0xFF000000 : 0xFF222222;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
        graphics.renderOutline(getX(), getY(), width, height, isFocused ? 0xFFFFFFFF : 0xFFAAAAAA);

        String text = displayText;
        int textX = getX() + 4;
        int textY = getY() + (height - 8) / 2;

        ResourceLocation itemRL = ResourceLocation.tryParse(value);
        if (itemRL != null) {
            Item item = BuiltInRegistries.ITEM.get(itemRL);
            if (item != null && item != Items.AIR) {
                graphics.renderItem(new ItemStack(item), getX() + 2, getY() + 2);
                textX = getX() + 22;
            }
        }

        if (font.width(text) > width - (textX - getX()) - 4) text = font.plainSubstrByWidth(text, width - (textX - getX()) - 8) + "...";
        graphics.drawString(font, text, textX, textY, 0xFFFFFF);

        if (isFocused && (cursorBlink / 10) % 2 == 0) {
            int cursorX = textX + font.width(displayText.substring(0, Math.min(cursorPos, displayText.length())));
            graphics.fill(cursorX, textY - 1, cursorX + 1, textY + 9, 0xFFFFFFFF);
        }

        if (isOpen && !filteredItems.isEmpty()) renderDropdown(graphics, mouseX, mouseY);
    }

    private void renderDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        int listHeight = Math.min(filteredItems.size(), MAX_VISIBLE) * OPTION_HEIGHT;
        int listY = getY() + height;

        graphics.fill(getX() - 1, listY - 1, getX() + width + 1, listY + listHeight + 1, 0xFF000000);
        graphics.fill(getX(), listY, getX() + width, listY + listHeight, 0xFF222222);
        graphics.renderOutline(getX(), listY, width, listHeight, 0xFFAAAAAA);

        int visibleCount = Math.min(filteredItems.size(), MAX_VISIBLE);
        for (int i = 0; i < visibleCount; i++) {
            int index = scrollOffset + i;
            if (index >= filteredItems.size()) break;

            ResourceLocation itemRL = filteredItems.get(index);
            int optionY = listY + i * OPTION_HEIGHT;

            boolean hovered = mouseX >= getX() && mouseX < getX() + width && mouseY >= optionY && mouseY < optionY + OPTION_HEIGHT;
            if (hovered) graphics.fill(getX() + 1, optionY, getX() + width - 1, optionY + OPTION_HEIGHT, 0xFF444488);

            Item item = BuiltInRegistries.ITEM.get(itemRL);
            if (item != null && item != Items.AIR) graphics.renderItem(new ItemStack(item), getX() + 2, optionY + 1);

            String displayName = item != null ? new ItemStack(item).getHoverName().getString() : itemRL.toString();
            if (font.width(displayName) > width - 24) displayName = font.plainSubstrByWidth(displayName, width - 28) + "...";
            graphics.drawString(font, displayName, getX() + 22, optionY + 5, 0xFFFFFF);
        }

        if (scrollOffset > 0) graphics.drawString(font, "▲", getX() + width - 10, listY + 2, 0xAAAAAA);
        if (scrollOffset + MAX_VISIBLE < filteredItems.size()) graphics.drawString(font, "▼", getX() + width - 10, listY + listHeight - 10, 0xAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;

        if (button == 0) {
            if (mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height) {
                isFocused = true;
                isOpen = true;
                return true;
            }

            if (isOpen && !filteredItems.isEmpty()) {
                int listY = getY() + height;
                int listHeight = Math.min(filteredItems.size(), MAX_VISIBLE) * OPTION_HEIGHT;

                if (mouseX >= getX() && mouseX < getX() + width && mouseY >= listY && mouseY < listY + listHeight) {
                    int clickedIndex = (int) ((mouseY - listY) / OPTION_HEIGHT) + scrollOffset;
                    if (clickedIndex >= 0 && clickedIndex < filteredItems.size()) {
                        value = filteredItems.get(clickedIndex).toString();
                        displayText = value;
                        cursorPos = value.length();
                        if (responder != null) responder.accept(value);
                        isOpen = false;
                        isFocused = false;
                        return true;
                    }
                }
            }

            isFocused = false;
            isOpen = false;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused) return false;

        if (keyCode == 259 && cursorPos > 0) {
            displayText = displayText.substring(0, cursorPos - 1) + displayText.substring(cursorPos);
            cursorPos--;
            updateFilteredItems();
            isOpen = true;
            return true;
        }

        if (keyCode == 261 && cursorPos < displayText.length()) {
            displayText = displayText.substring(0, cursorPos) + displayText.substring(cursorPos + 1);
            updateFilteredItems();
            return true;
        }

        if (keyCode == 263 && cursorPos > 0) { cursorPos--; return true; }
        if (keyCode == 262 && cursorPos < displayText.length()) { cursorPos++; return true; }

        if (keyCode == 257 && !filteredItems.isEmpty()) {
            value = filteredItems.get(0).toString();
            displayText = value;
            cursorPos = value.length();
            if (responder != null) responder.accept(value);
            isOpen = false;
            isFocused = false;
            return true;
        }

        if (keyCode == 256) {
            displayText = value;
            cursorPos = value.length();
            isOpen = false;
            isFocused = false;
            return true;
        }

        if (keyCode == 258 && !filteredItems.isEmpty()) {
            displayText = filteredItems.get(0).toString();
            cursorPos = displayText.length();
            updateFilteredItems();
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!isFocused) return false;

        if (Character.isLetterOrDigit(chr) || chr == ':' || chr == '_' || chr == '/' || chr == '.') {
            displayText = displayText.substring(0, cursorPos) + chr + displayText.substring(cursorPos);
            cursorPos++;
            updateFilteredItems();
            isOpen = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOpen && !filteredItems.isEmpty()) {
            int listY = getY() + height;
            int listHeight = Math.min(filteredItems.size(), MAX_VISIBLE) * OPTION_HEIGHT;

            if (mouseX >= getX() && mouseX < getX() + width && mouseY >= listY && mouseY < listY + listHeight) {
                int maxScroll = Math.max(0, filteredItems.size() - MAX_VISIBLE);
                scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) scrollY));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY)) return true;
        if (isOpen && !filteredItems.isEmpty()) {
            int listY = getY() + height;
            int listHeight = Math.min(filteredItems.size(), MAX_VISIBLE) * OPTION_HEIGHT;
            return mouseX >= getX() && mouseX < getX() + width && mouseY >= listY && mouseY < listY + listHeight;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
