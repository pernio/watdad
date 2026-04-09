package app.netlify.jinzo.watdad.client.screen;

import app.netlify.jinzo.watdad.client.WatdadClient;
import app.netlify.jinzo.watdad.client.config.OreOutlineConfig;
import app.netlify.jinzo.watdad.client.config.OreOutlineConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.Formatting;

public final class WatdadConfigScreen extends Screen {
    private final Screen parent;
    private boolean exposedOnly;
    private int scanRadius;
    private OreOutlineConfig.RenderMode renderMode;

    public WatdadConfigScreen(Screen parent) {
        super(Text.literal("Watdad"));
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

        addDrawableChild(ButtonWidget.builder(exposedButtonText(), button -> {
            exposedOnly = !exposedOnly;
            button.setMessage(exposedButtonText());
        }).dimensions(centerX - 100, top, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(renderModeButtonText(), button -> {
            renderMode = renderMode.next();
            button.setMessage(renderModeButtonText());
        }).dimensions(centerX - 100, top + 28, 200, 20).build());

        addDrawableChild(new RadiusSlider(centerX - 100, top + 56, 200, 20, scanRadius));

        addDrawableChild(ButtonWidget.builder(Text.literal("Open blocks.yml"), button -> {
            OreOutlineConfigManager manager = WatdadClient.getConfigManager();
            if (manager != null) {
                Util.getOperatingSystem().open(manager.getOreFile().toFile());
            }
        }).dimensions(centerX - 100, top + 84, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> saveAndClose())
                .dimensions(centerX - 100, top + 140, 97, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(centerX + 3, top + 140, 97, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, radiusText(), width / 2, height / 4 + 36, 0xD0D0D0);
        context.drawCenteredTextWithShadow(textRenderer, displayedRangeText(), width / 2, height / 4 + 74, 0xB8B8B8);
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("block colors live in config/watdad/blocks.yml").formatted(Formatting.GRAY),
                width / 2,
                height / 4 + 110,
                0xA0A0A0
        );
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Open blocks.yml opens the YAML config file.").formatted(Formatting.DARK_GRAY),
                width / 2,
                height / 4 + 122,
                0x808080
        );
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private Text exposedButtonText() {
        return Text.literal("Exposed blocks only: " + (exposedOnly ? "ON" : "OFF"));
    }

    private Text radiusText() {
        return Text.literal("Scan radius: " + scanRadius + " blocks");
    }

    private Text displayedRangeText() {
        int diameter = (scanRadius * 2) + 1;
        return Text.literal("Current displayed range: " + diameter + " x " + diameter + " x " + diameter + " blocks");
    }

    private Text renderModeButtonText() {
        return Text.literal("Render mode: " + (renderMode == OreOutlineConfig.RenderMode.OUTLINE ? "Outline" : "Filled"));
    }

    private void refresh() {
        clearAndInit();
    }

    private void saveAndClose() {
        OreOutlineConfigManager manager = WatdadClient.getConfigManager();
        if (manager != null) {
            manager.saveMenuSettings(exposedOnly, scanRadius, renderMode);
        }
        close();
    }

    private final class RadiusSlider extends SliderWidget {
        private static final int MIN_RADIUS = 1;
        private static final int MAX_RADIUS = 64;

        private RadiusSlider(int x, int y, int width, int height, int currentRadius) {
            super(x, y, width, height, Text.empty(), toSliderValue(currentRadius));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Radius: " + scanRadius));
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
