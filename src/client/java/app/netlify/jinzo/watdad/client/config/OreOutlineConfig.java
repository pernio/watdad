package app.netlify.jinzo.watdad.client.config;

import net.minecraft.block.Block;

import java.util.Map;

public record OreOutlineConfig(
        boolean enabled,
        boolean exposedOnly,
        int scanRadius,
        long scanIntervalTicks,
        RenderMode renderMode,
        OutlineColor unexposedBlocksColor,
        Map<Block, OutlineColor> ores
) {
    public static OreOutlineConfig defaults() {
        return new OreOutlineConfig(
                true,
                true,
                24,
                20L,
                RenderMode.OUTLINE,
                OutlineColor.fromRgb(0xFF66CC),
                Map.of()
        );
    }

    public enum RenderMode {
        OUTLINE,
        FILLED;

        public RenderMode next() {
            return this == OUTLINE ? FILLED : OUTLINE;
        }
    }

    public record OutlineColor(float red, float green, float blue, float alpha) {
        public static OutlineColor fromRgb(int rgb) {
            float red = ((rgb >> 16) & 0xFF) / 255.0F;
            float green = ((rgb >> 8) & 0xFF) / 255.0F;
            float blue = (rgb & 0xFF) / 255.0F;
            return new OutlineColor(red, green, blue, 1.0F);
        }

        public int argb() {
            int alphaInt = clampToByte(alpha);
            int redInt = clampToByte(red);
            int greenInt = clampToByte(green);
            int blueInt = clampToByte(blue);
            return (alphaInt << 24) | (redInt << 16) | (greenInt << 8) | blueInt;
        }

        private static int clampToByte(float value) {
            return Math.max(0, Math.min(255, (int) (value * 255.0F)));
        }
    }
}
