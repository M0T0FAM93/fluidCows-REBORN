package motofam93.fluidcows.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class DropdownWidget<T> extends AbstractWidget {

    private final Font font;
    private final Supplier<List<T>> optionsSupplier;
    private final Function<T, String> formatter;
    private T selectedValue;
    private boolean isOpen = false;
    private int scrollOffset = 0;
    private Consumer<T> responder;
    private List<T> cachedOptions;

    private static final int MAX_VISIBLE = 6;
    private static final int OPTION_HEIGHT = 16;

    public DropdownWidget(Font font, int x, int y, int width, int height, Component label, Object parentScreen,
                          Supplier<List<T>> optionsSupplier, Function<T, String> formatter) {
        super(x, y, width, height, label);
        this.font = font;
        this.optionsSupplier = optionsSupplier;
        this.formatter = formatter;
    }

    public void setValue(T value) { this.selectedValue = value; }
    public T getValue() { return selectedValue; }
    public void setResponder(Consumer<T> responder) { this.responder = responder; }
    public boolean isOpen() { return isOpen; }
    public void close() { isOpen = false; }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bgColor = this.isHovered ? 0xFF555555 : 0xFF333333;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
        graphics.renderOutline(getX(), getY(), width, height, 0xFFAAAAAA);

        String displayText = formatter.apply(selectedValue);
        if (font.width(displayText) > width - 20) displayText = font.plainSubstrByWidth(displayText, width - 25) + "...";
        graphics.drawString(font, displayText, getX() + 4, getY() + (height - 8) / 2, 0xFFFFFF);
        graphics.drawString(font, isOpen ? "▲" : "▼", getX() + width - 12, getY() + (height - 8) / 2, 0xFFFFFF);

        if (isOpen) renderDropdown(graphics, mouseX, mouseY);
    }

    private void renderDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        if (cachedOptions == null) cachedOptions = new ArrayList<>(optionsSupplier.get());

        int listHeight = Math.min(cachedOptions.size(), MAX_VISIBLE) * OPTION_HEIGHT;
        int listY = getY() + height;

        graphics.fill(getX(), listY, getX() + width, listY + listHeight, 0xEE222222);
        graphics.renderOutline(getX(), listY, width, listHeight, 0xFFAAAAAA);

        int visibleCount = Math.min(cachedOptions.size(), MAX_VISIBLE);
        for (int i = 0; i < visibleCount; i++) {
            int index = scrollOffset + i;
            if (index >= cachedOptions.size()) break;

            T option = cachedOptions.get(index);
            int optionY = listY + i * OPTION_HEIGHT;

            boolean hovered = mouseX >= getX() && mouseX < getX() + width && mouseY >= optionY && mouseY < optionY + OPTION_HEIGHT;
            if (hovered) graphics.fill(getX() + 1, optionY, getX() + width - 1, optionY + OPTION_HEIGHT, 0xFF444488);

            if (option != null && option.equals(selectedValue) || (option == null && selectedValue == null)) {
                graphics.drawString(font, "►", getX() + 2, optionY + 4, 0x55FF55);
            }

            String text = formatter.apply(option);
            if (font.width(text) > width - 20) text = font.plainSubstrByWidth(text, width - 25) + "...";
            graphics.drawString(font, text, getX() + 14, optionY + 4, 0xFFFFFF);
        }

        if (scrollOffset > 0) graphics.drawString(font, "▲", getX() + width - 10, listY + 2, 0xAAAAAA);
        if (scrollOffset + MAX_VISIBLE < cachedOptions.size()) graphics.drawString(font, "▼", getX() + width - 10, listY + listHeight - 10, 0xAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;

        if (button == 0) {
            if (mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height) {
                isOpen = !isOpen;
                if (isOpen) {
                    cachedOptions = new ArrayList<>(optionsSupplier.get());
                    scrollOffset = 0;
                }
                return true;
            }

            if (isOpen && cachedOptions != null) {
                int listY = getY() + height;
                int listHeight = Math.min(cachedOptions.size(), MAX_VISIBLE) * OPTION_HEIGHT;

                if (mouseX >= getX() && mouseX < getX() + width && mouseY >= listY && mouseY < listY + listHeight) {
                    int clickedIndex = (int) ((mouseY - listY) / OPTION_HEIGHT) + scrollOffset;
                    if (clickedIndex >= 0 && clickedIndex < cachedOptions.size()) {
                        selectedValue = cachedOptions.get(clickedIndex);
                        if (responder != null) responder.accept(selectedValue);
                        isOpen = false;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOpen && cachedOptions != null) {
            int listY = getY() + height;
            int listHeight = Math.min(cachedOptions.size(), MAX_VISIBLE) * OPTION_HEIGHT;

            if (mouseX >= getX() && mouseX < getX() + width && mouseY >= listY && mouseY < listY + listHeight) {
                int maxScroll = Math.max(0, cachedOptions.size() - MAX_VISIBLE);
                scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) scrollY));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY)) return true;
        if (isOpen && cachedOptions != null) {
            int listY = getY() + height;
            int listHeight = Math.min(cachedOptions.size(), MAX_VISIBLE) * OPTION_HEIGHT;
            return mouseX >= getX() && mouseX < getX() + width && mouseY >= listY && mouseY < listY + listHeight;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
