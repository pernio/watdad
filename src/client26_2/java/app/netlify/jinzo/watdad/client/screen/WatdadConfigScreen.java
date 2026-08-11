package app.netlify.jinzo.watdad.client.screen;

import app.netlify.jinzo.watdad.client.WatdadClient;
import app.netlify.jinzo.watdad.client.config.OreOutlineConfig;
import app.netlify.jinzo.watdad.client.config.OreOutlineConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Desktop;
import java.io.IOException;

public final class WatdadConfigScreen extends Screen {
    private final Screen parent;
    private boolean exposedOnly;
    private int scanRadius;
    private OreOutlineConfig.RenderMode renderMode;

    public WatdadConfigScreen(Screen parent) {
        super(Component.literal("Watdad"));
        this.parent = parent;

        OreOutlineConfigManager manager = WatdadClient.getConfigManager();
        OreOutlineConfig config = manager != null ? manager.getConfig() : OreOutlineConfig.defaults();
        this.exposedOnly = config.exposedOnly();
        this.scanRadius = config.scanRadius();
        this.renderMode = config.renderMode();
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int top = height / 4;

        addRenderableWidget(Button.builder(exposedButtonText(), button -> {
            exposedOnly = !exposedOnly;
            button.setMessage(exposedButtonText());
        }).bounds(centerX - 100, top, 200, 20).build());

        addRenderableWidget(Button.builder(renderModeButtonText(), button -> {
            renderMode = renderMode.next();
            button.setMessage(renderModeButtonText());
        }).bounds(centerX - 100, top + 28, 200, 20).build());

        addRenderableWidget(new RadiusSlider(centerX - 100, top + 56, 200, 20, scanRadius));

        addRenderableWidget(Button.builder(Component.literal("Open blocks.yml"), button -> openOreConfig())
                .bounds(centerX - 100, top + 84, 200, 20)
                .tooltip(Tooltip.create(Component.literal("Open config/watdad/blocks.yml")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveAndClose())
                .bounds(centerX - 100, top + 140, 97, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(centerX + 3, top + 140, 97, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
        graphics.centeredText(font, title, width / 2, 20, 0xFFFFFF);
        graphics.centeredText(font, radiusText(), width / 2, height / 4 + 36, 0xD0D0D0);
        graphics.centeredText(font, displayedRangeText(), width / 2, height / 4 + 74, 0xB8B8B8);
        graphics.centeredText(
                font,
                Component.literal("block colors live in config/watdad/blocks.yml").withStyle(ChatFormatting.GRAY),
                width / 2,
                height / 4 + 110,
                0xA0A0A0
        );
        graphics.centeredText(
                font,
                Component.literal("Open blocks.yml opens the YAML config file.").withStyle(ChatFormatting.DARK_GRAY),
                width / 2,
                height / 4 + 122,
                0x808080
        );
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreenAndShow(parent);
        }
    }

    private Component exposedButtonText() {
        return Component.literal("Exposed blocks only: " + (exposedOnly ? "ON" : "OFF"));
    }

    private Component radiusText() {
        return Component.literal("Scan radius: " + scanRadius + " blocks");
    }

    private Component displayedRangeText() {
        int diameter = (scanRadius * 2) + 1;
        return Component.literal("Current displayed range: " + diameter + " x " + diameter + " x " + diameter + " blocks");
    }

    private Component renderModeButtonText() {
        return Component.literal("Render mode: " + (renderMode == OreOutlineConfig.RenderMode.OUTLINE ? "Outline" : "Filled"));
    }

    private void saveAndClose() {
        OreOutlineConfigManager manager = WatdadClient.getConfigManager();
        if (manager != null) {
            manager.saveMenuSettings(exposedOnly, scanRadius, renderMode);
        }
        onClose();
    }

    private void openOreConfig() {
        OreOutlineConfigManager manager = WatdadClient.getConfigManager();
        if (manager == null || !Desktop.isDesktopSupported()) {
            return;
        }

        try {
            Desktop.getDesktop().open(manager.getOreFile().toFile());
        } catch (IOException ignored) {
        }
    }

    private final class RadiusSlider extends AbstractSliderButton {
        private static final int MIN_RADIUS = 1;
        private static final int MAX_RADIUS = 64;

        private RadiusSlider(int x, int y, int width, int height, int currentRadius) {
            super(x, y, width, height, Component.empty(), toSliderValue(currentRadius));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("Radius: " + scanRadius));
        }

        @Override
        protected void applyValue() {
            scanRadius = toRadius(value);
            updateMessage();
        }

        private static double toSliderValue(int radius) {
            return (double) (radius - MIN_RADIUS) / (MAX_RADIUS - MIN_RADIUS);
        }

        private static int toRadius(double sliderValue) {
            return MIN_RADIUS + (int) Math.round(sliderValue * (MAX_RADIUS - MIN_RADIUS));
        }
    }
}
