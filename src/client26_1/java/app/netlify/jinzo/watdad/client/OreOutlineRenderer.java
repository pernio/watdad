package app.netlify.jinzo.watdad.client;

import app.netlify.jinzo.watdad.client.config.OreOutlineConfig;
import app.netlify.jinzo.watdad.client.config.OreOutlineConfigManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class OreOutlineRenderer {
    private final OreOutlineConfigManager configManager = new OreOutlineConfigManager();
    private final List<VisibleOre> visibleOres = new ArrayList<>();
    private BlockPos lastScanCenter = BlockPos.ZERO;
    private long nextScanTick;

    public void initialize() {
        configManager.loadOrCreate();
    }

    public OreOutlineConfigManager getConfigManager() {
        return configManager;
    }

    public void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (client.level == null || player == null) {
            visibleOres.clear();
            return;
        }

        configManager.reloadIfChanged();
        OreOutlineConfig config = configManager.getConfig();
        if (!config.enabled() || config.ores().isEmpty()) {
            visibleOres.clear();
            return;
        }

        BlockPos cameraPos = BlockPos.containing(player.getPosition(1.0F));
        if (client.level.getGameTime() >= nextScanTick || chebyshevDistance(cameraPos, lastScanCenter) >= 2) {
            rescan(client, cameraPos, config);
        }

        if (visibleOres.isEmpty()) {
            return;
        }

        PoseStack poseStack = context.poseStack();
        MultiBufferSource consumers = context.bufferSource();
        if (poseStack == null || consumers == null) {
            return;
        }

        Camera camera = client.gameRenderer.getMainCamera();
        Vec3 cameraPosVec = camera.position();
        for (VisibleOre ore : visibleOres) {
            draw(poseStack, consumers, cameraPosVec, ore, config.renderMode());
        }
    }

    private void rescan(Minecraft client, BlockPos center, OreOutlineConfig config) {
        visibleOres.clear();
        lastScanCenter = center.immutable();
        nextScanTick = client.level.getGameTime() + config.scanIntervalTicks();

        int radius = Math.max(1, config.scanRadius());
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - radius; y <= center.getY() + radius; y++) {
                if (y < client.level.getMinY() || y > client.level.getMaxY()) {
                    continue;
                }

                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    if (!client.level.hasChunk(x >> 4, z >> 4)) {
                        continue;
                    }

                    mutable.set(x, y, z);
                    OreOutlineConfig.OutlineColor color = config.ores().get(client.level.getBlockState(mutable).getBlock());
                    if (color == null) {
                        continue;
                    }

                    EnumSet<Direction> visibleFaces = collectVisibleFaces(client.level, mutable);
                    OreOutlineConfig.OutlineColor displayColor = color;
                    if (config.exposedOnly() && visibleFaces.isEmpty()) {
                        displayColor = config.unexposedBlocksColor();
                    }

                    visibleOres.add(new VisibleOre(mutable.immutable(), displayColor, visibleFaces));
                }
            }
        }
    }

    private EnumSet<Direction> collectVisibleFaces(BlockGetter level, BlockPos pos) {
        EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.isAir() || !neighborState.getFluidState().isEmpty() || !neighborState.canOcclude()) {
                faces.add(direction);
            }
        }

        return faces;
    }

    private void draw(
            PoseStack poseStack,
            MultiBufferSource consumers,
            Vec3 camera,
            VisibleOre ore,
            OreOutlineConfig.RenderMode renderMode
    ) {
        if (renderMode == OreOutlineConfig.RenderMode.FILLED) {
            drawFilled(poseStack, consumers, camera, ore);
            return;
        }

        drawOutline(poseStack, consumers, camera, ore);
    }

    private void drawOutline(PoseStack poseStack, MultiBufferSource consumers, Vec3 camera, VisibleOre ore) {
        ShapeRenderer.renderShape(
                poseStack,
                consumers.getBuffer(RenderTypes.lines()),
                Shapes.block(),
                ore.pos.getX() - camera.x,
                ore.pos.getY() - camera.y,
                ore.pos.getZ() - camera.z,
                ore.color.argb(),
                1.0F
        );
    }

    private void drawFilled(PoseStack poseStack, MultiBufferSource consumers, Vec3 camera, VisibleOre ore) {
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = consumers.getBuffer(RenderTypes.debugFilledBox());
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

        quad(pose, consumer, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        quad(pose, consumer, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        quad(pose, consumer, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);
        quad(pose, consumer, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, alpha);
        quad(pose, consumer, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        quad(pose, consumer, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
    }

    private void quad(
            PoseStack.Pose pose,
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
        consumer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, x2, y2, z2).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, x3, y3, z3).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, x4, y4, z4).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, x4, y4, z4).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, x3, y3, z3).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, x2, y2, z2).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, alpha);
    }

    private int chebyshevDistance(BlockPos first, BlockPos second) {
        return Math.max(
                Math.max(Math.abs(first.getX() - second.getX()), Math.abs(first.getY() - second.getY())),
                Math.abs(first.getZ() - second.getZ())
        );
    }

    private record VisibleOre(BlockPos pos, OreOutlineConfig.OutlineColor color, EnumSet<Direction> visibleFaces) {
    }
}
