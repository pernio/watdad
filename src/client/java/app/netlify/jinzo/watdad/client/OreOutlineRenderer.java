package app.netlify.jinzo.watdad.client;

import app.netlify.jinzo.watdad.client.config.OreOutlineConfig;
import app.netlify.jinzo.watdad.client.config.OreOutlineConfigManager;
import net.minecraft.block.BlockState;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockRenderView;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class OreOutlineRenderer {
    private final OreOutlineConfigManager configManager = new OreOutlineConfigManager();
    private final List<VisibleOre> visibleOres = new ArrayList<>();
    private BlockPos lastScanCenter = BlockPos.ORIGIN;
    private long nextScanTick;

    public void initialize() {
        configManager.loadOrCreate();
    }

    public OreOutlineConfigManager getConfigManager() {
        return configManager;
    }

    public void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (client.world == null || player == null || !player.isSpectator()) {
            visibleOres.clear();
            return;
        }

        configManager.reloadIfChanged();
        OreOutlineConfig config = configManager.getConfig();
        if (!config.enabled() || config.ores().isEmpty()) {
            visibleOres.clear();
            return;
        }

        BlockPos cameraPos = BlockPos.ofFloored(player.getCameraPosVec(1.0F));
        if (client.world.getTime() >= nextScanTick || cameraPos.getChebyshevDistance(lastScanCenter) >= 2) {
            rescan(client, cameraPos, config);
        }

        if (visibleOres.isEmpty()) {
            return;
        }

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            return;
        }

        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        for (VisibleOre ore : visibleOres) {
            draw(context, consumers, camera, ore, config.renderMode());
        }
    }

    private void rescan(MinecraftClient client, BlockPos center, OreOutlineConfig config) {
        visibleOres.clear();
        lastScanCenter = center.toImmutable();
        nextScanTick = client.world.getTime() + config.scanIntervalTicks();

        int radius = Math.max(1, config.scanRadius());
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - radius; y <= center.getY() + radius; y++) {
                if (y < client.world.getBottomY() || y > client.world.getTopYInclusive()) {
                    continue;
                }

                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    if (!client.world.isChunkLoaded(x >> 4, z >> 4)) {
                        continue;
                    }

                    mutable.set(x, y, z);
                    OreOutlineConfig.OutlineColor color = config.ores().get(client.world.getBlockState(mutable).getBlock());
                    if (color == null) {
                        continue;
                    }

                    EnumSet<Direction> visibleFaces = collectVisibleFaces(client.world, mutable);
                    OreOutlineConfig.OutlineColor displayColor = color;
                    if (config.exposedOnly() && visibleFaces.isEmpty()) {
                        displayColor = config.unexposedBlocksColor();
                    }

                    visibleOres.add(new VisibleOre(mutable.toImmutable(), displayColor, visibleFaces));
                }
            }
        }
    }

    private EnumSet<Direction> collectVisibleFaces(BlockRenderView world, BlockPos pos) {
        EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.offset(direction);
            BlockState neighborState = world.getBlockState(neighborPos);

            if (neighborState.isAir() || !neighborState.getFluidState().isEmpty() || !neighborState.isOpaqueFullCube()) {
                faces.add(direction);
            }
        }

        return faces;
    }

    private void draw(
            WorldRenderContext context,
            VertexConsumerProvider consumers,
            Vec3d camera,
            VisibleOre ore,
            OreOutlineConfig.RenderMode renderMode
    ) {
        if (renderMode == OreOutlineConfig.RenderMode.FILLED) {
            drawFilled(context, consumers, camera, ore);
            return;
        }

        drawOutline(context, consumers, camera, ore);
    }

    private void drawOutline(WorldRenderContext context, VertexConsumerProvider consumers, Vec3d camera, VisibleOre ore) {
        MatrixStack matrices = context.matrices();
        VertexRendering.drawOutline(
                matrices,
                consumers.getBuffer(RenderLayers.lines()),
                VoxelShapes.fullCube(),
                ore.pos.getX() - camera.x,
                ore.pos.getY() - camera.y,
                ore.pos.getZ() - camera.z,
                ore.color.argb(),
                1.0F
        );
    }

    private void drawFilled(WorldRenderContext context, VertexConsumerProvider consumers, Vec3d camera, VisibleOre ore) {
        MatrixStack.Entry entry = context.matrices().peek();
        VertexConsumer consumer = consumers.getBuffer(RenderLayers.debugFilledBox());
        float red = ore.color.red();
        float green = ore.color.green();
        float blue = ore.color.blue();
        float alpha = 0.35F;

        float minX = (float) (ore.pos.getX() - camera.x);
        float minY = (float) (ore.pos.getY() - camera.y);
        float minZ = (float) (ore.pos.getZ() - camera.z);
        float maxX = minX + 1.0F;
        float maxY = minY + 1.0F;
        float maxZ = minZ + 1.0F;

        quad(entry, consumer, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        quad(entry, consumer, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        quad(entry, consumer, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);
        quad(entry, consumer, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, alpha);
        quad(entry, consumer, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        quad(entry, consumer, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
    }

    private void quad(
            MatrixStack.Entry entry,
            VertexConsumer consumer,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        consumer.vertex(entry, x1, y1, z1).color(red, green, blue, alpha);
        consumer.vertex(entry, x2, y2, z2).color(red, green, blue, alpha);
        consumer.vertex(entry, x3, y3, z3).color(red, green, blue, alpha);
        consumer.vertex(entry, x4, y4, z4).color(red, green, blue, alpha);

        // Emit the reverse winding too so filled faces remain visible from either side.
        consumer.vertex(entry, x4, y4, z4).color(red, green, blue, alpha);
        consumer.vertex(entry, x3, y3, z3).color(red, green, blue, alpha);
        consumer.vertex(entry, x2, y2, z2).color(red, green, blue, alpha);
        consumer.vertex(entry, x1, y1, z1).color(red, green, blue, alpha);
    }

    private record VisibleOre(BlockPos pos, OreOutlineConfig.OutlineColor color, EnumSet<Direction> visibleFaces) {
    }
}
