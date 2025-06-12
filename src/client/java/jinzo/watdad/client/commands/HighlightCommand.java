package jinzo.watdad.client.commands;

import jinzo.watdad.client.WatdadClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import static jinzo.watdad.client.WatdadClient.*;

public class HighlightCommand {
    public static void renderBlockOutline(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        MatrixStack matrices = context.matrixStack();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = context.camera().getPos();
        VertexConsumer vertexConsumer = context.consumers().getBuffer(RenderLayer.getLines());

        if (client.player.isSpectator()) {
            if (getConfig().displayHighlightOres) {
                BlockPos center = client.player.getBlockPos();

                for (BlockPos pos : BlockPos.iterateOutwards(center, getConfig().oreRenderRange, getConfig().oreRenderRange, getConfig().oreRenderRange)) {
                    BlockState state = client.world.getBlockState(pos);
                    Block block = state.getBlock();
                    if (ORE_COLORS.containsKey(block)) {
                        float[] baseColor = ORE_COLORS.get(block);
                        boolean exposed = isExposed(pos, client);

                        float[] color = baseColor;
                        boolean revealHeld = WatdadClient.holdRevealKey.isPressed();

                        if (getConfig().exposeOres && !exposed && !revealHeld) {
                            color = new float[]{1.0f, 0.0f, 1.0f}; // Magenta if hidden and expose is on
                        }

                        Box box = new Box(pos).expand(0.002);
                        float x1 = (float)(box.minX - camera.x);
                        float y1 = (float)(box.minY - camera.y);
                        float z1 = (float)(box.minZ - camera.z);
                        float x2 = (float)(box.maxX - camera.x);
                        float y2 = (float)(box.maxY - camera.y);
                        float z2 = (float)(box.maxZ - camera.z);

                        // Always draw the base ore color
                        drawBoxOutline(vertexConsumer, matrix, x1, y1, z1, x2, y2, z2, color[0], color[1], color[2], 1.0f);
                    }
                }
                return;
            }
        }  else {
            if (!getConfig().displayHighlightBlock || !highlightBlock) return;

            if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

            BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
            BlockPos pos = hit.getBlockPos();
            lastHighlightedBlock = pos;

            Vec3d playerPos = client.player.getPos();
            double distanceSq = pos.getSquaredDistanceFromCenter(playerPos.x, playerPos.y, playerPos.z);
            if (distanceSq > getConfig().oreRenderRange * getConfig().oreRenderRange) return;

            Box box = new Box(pos).expand(0.002);
            float x1 = (float)(box.minX - camera.x);
            float y1 = (float)(box.minY - camera.y);
            float z1 = (float)(box.minZ - camera.z);
            float x2 = (float)(box.maxX - camera.x);
            float y2 = (float)(box.maxY - camera.y);
            float z2 = (float)(box.maxZ - camera.z);

            drawBoxOutline(vertexConsumer, matrix, x1, y1, z1, x2, y2, z2, 1, 0, 0, 1);
        }
    }

    public static void drawBoxOutline(VertexConsumer vertexConsumer, Matrix4f matrix, float x1, float y1, float z1,
                                float x2, float y2, float z2, float r, float g, float b, float a) {
        line(vertexConsumer, matrix, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(vertexConsumer, matrix, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(vertexConsumer, matrix, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(vertexConsumer, matrix, x1, y1, z2, x1, y1, z1, r, g, b, a);

        line(vertexConsumer, matrix, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(vertexConsumer, matrix, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(vertexConsumer, matrix, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(vertexConsumer, matrix, x1, y2, z2, x1, y2, z1, r, g, b, a);

        line(vertexConsumer, matrix, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(vertexConsumer, matrix, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(vertexConsumer, matrix, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(vertexConsumer, matrix, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    public static void line(VertexConsumer vertexConsumer, Matrix4f matrix,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float r, float g, float b, float a) {
        vertexConsumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0);
    }

    private static boolean isExposed(BlockPos pos, MinecraftClient client) {
        // Check the 6 directly adjacent directions
        BlockPos[] directions = new BlockPos[] {
                pos.up(),
                pos.down(),
                pos.north(),
                pos.south(),
                pos.east(),
                pos.west()
        };

        for (BlockPos adjacentPos : directions) {
            BlockState state = client.world.getBlockState(adjacentPos);
            if (state.isAir() || state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.LAVA) {
                return true; // Exposed to air, water, or lava
            }
        }

        return false; // Fully surrounded
    }
}
