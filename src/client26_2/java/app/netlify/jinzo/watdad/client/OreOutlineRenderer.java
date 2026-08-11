package app.netlify.jinzo.watdad.client;

import app.netlify.jinzo.watdad.client.config.OreOutlineConfig;
import app.netlify.jinzo.watdad.client.config.OreOutlineConfigManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
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

        Camera camera = context.gameRenderer().mainCamera();
        BlockPos cameraPos = camera.blockPosition();
        if (client.level.getGameTime() >= nextScanTick || chebyshevDistance(cameraPos, lastScanCenter) >= 2) {
            rescan(client, cameraPos, config);
        }

        if (visibleOres.isEmpty()) {
            return;
        }

        PoseStack poseStack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();
        CameraRenderState cameraRenderState = context.levelState().cameraRenderState;
        if (poseStack == null || collector == null || cameraRenderState == null || cameraRenderState.pos == null) {
            return;
        }

        for (VisibleOre ore : visibleOres) {
            draw(poseStack, collector, cameraRenderState, ore, config.renderMode());
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
            SubmitNodeCollector collector,
            CameraRenderState cameraRenderState,
            VisibleOre ore,
            OreOutlineConfig.RenderMode renderMode
    ) {
        if (renderMode == OreOutlineConfig.RenderMode.FILLED) {
            drawFilled(collector, cameraRenderState, ore);
            return;
        }

        drawOutline(poseStack, collector, cameraRenderState, ore);
    }

    private void drawOutline(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraRenderState,
            VisibleOre ore
    ) {
        Vec3 cameraPos = cameraRenderState.pos;
        poseStack.pushPose();
        poseStack.translate(
                ore.pos.getX() - cameraPos.x,
                ore.pos.getY() - cameraPos.y,
                ore.pos.getZ() - cameraPos.z
        );
        collector.submitShapeOutline(
                poseStack,
                Shapes.block(),
                RenderTypes.lines(),
                ore.color.argb(),
                1.0F,
                false
        );
        poseStack.popPose();
    }

    private void drawFilled(SubmitNodeCollector collector, CameraRenderState cameraRenderState, VisibleOre ore) {
        DrawableGizmoPrimitives primitives = new DrawableGizmoPrimitives();
        Vec3 cameraPos = cameraRenderState.pos;

        Vec3 min = new Vec3(
                ore.pos.getX() - cameraPos.x,
                ore.pos.getY() - cameraPos.y,
                ore.pos.getZ() - cameraPos.z
        );
        Vec3 max = min.add(1.0D, 1.0D, 1.0D);

        Vec3 p000 = new Vec3(min.x, min.y, min.z);
        Vec3 p001 = new Vec3(min.x, min.y, max.z);
        Vec3 p010 = new Vec3(min.x, max.y, min.z);
        Vec3 p011 = new Vec3(min.x, max.y, max.z);
        Vec3 p100 = new Vec3(max.x, min.y, min.z);
        Vec3 p101 = new Vec3(max.x, min.y, max.z);
        Vec3 p110 = new Vec3(max.x, max.y, min.z);
        Vec3 p111 = new Vec3(max.x, max.y, max.z);

        primitives.addQuad(p000, p100, p110, p010, ore.color.argb());
        primitives.addQuad(p101, p001, p011, p111, ore.color.argb());
        primitives.addQuad(p001, p000, p010, p011, ore.color.argb());
        primitives.addQuad(p100, p101, p111, p110, ore.color.argb());
        primitives.addQuad(p010, p110, p111, p011, ore.color.argb());
        primitives.addQuad(p001, p101, p100, p000, ore.color.argb());

        primitives.submit(collector, cameraRenderState, false);
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
