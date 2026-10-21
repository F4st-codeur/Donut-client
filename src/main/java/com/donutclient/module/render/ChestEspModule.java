package com.donutclient.module.render;

import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.module.setting.BooleanSetting;
import com.donutclient.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Surligne les coffres et autres conteneurs, visibles à travers les murs. */
public class ChestEspModule extends Module {
    private static final int CHEST_COLOR = 0x60FFAA00;
    private static final int TRAPPED_COLOR = 0x60FF3030;
    private static final int ENDER_COLOR = 0x60A040FF;
    private static final int SHULKER_COLOR = 0x60FF60C0;
    private static final int BARREL_COLOR = 0x60A06030;
    private static final int OTHER_COLOR = 0x60A0A0A0;

    private final BooleanSetting chests = add(new BooleanSetting("Coffres", true));
    private final BooleanSetting enderChests = add(new BooleanSetting("Ender chests", true));
    private final BooleanSetting shulkers = add(new BooleanSetting("Shulkers", true));
    private final BooleanSetting barrels = add(new BooleanSetting("Barils", true));
    private final BooleanSetting others = add(new BooleanSetting("Fours/hoppers", false));
    private final BooleanSetting throughWalls = add(new BooleanSetting("À travers les murs", true));

    private record Highlight(BlockPos pos, int color) {
    }

    private List<Highlight> highlights = List.of();

    public ChestEspModule() {
        super("ChestESP", "Surligne les coffres, ender chests, shulkers et barils.", Category.RENDER);
    }

    @Override
    protected void onDisable() {
        highlights = List.of();
    }

    @Override
    public String getInfo() {
        return String.valueOf(highlights.size());
    }

    @Override
    public void onTick() {
        if (mc.world == null) return;
        List<Highlight> found = new ArrayList<>();
        int radius = mc.options.getClampedViewDistance();
        int centerX = mc.player.getChunkPos().x;
        int centerZ = mc.player.getChunkPos().z;
        for (int cx = centerX - radius; cx <= centerX + radius; cx++) {
            for (int cz = centerZ - radius; cz <= centerZ + radius; cz++) {
                if (!mc.world.getChunkManager().isChunkLoaded(cx, cz)) continue;
                WorldChunk chunk = mc.world.getChunk(cx, cz);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    int color = colorOf(blockEntity);
                    if (color != 0) found.add(new Highlight(blockEntity.getPos(), color));
                }
            }
        }
        highlights = found;
    }

    private int colorOf(BlockEntity blockEntity) {
        // TrappedChestBlockEntity hérite de ChestBlockEntity : on le teste en premier.
        if (blockEntity instanceof TrappedChestBlockEntity) return chests.get() ? TRAPPED_COLOR : 0;
        if (blockEntity instanceof ChestBlockEntity) return chests.get() ? CHEST_COLOR : 0;
        if (blockEntity instanceof EnderChestBlockEntity) return enderChests.get() ? ENDER_COLOR : 0;
        if (blockEntity instanceof ShulkerBoxBlockEntity) return shulkers.get() ? SHULKER_COLOR : 0;
        if (blockEntity instanceof BarrelBlockEntity) return barrels.get() ? BARREL_COLOR : 0;
        if (blockEntity instanceof AbstractFurnaceBlockEntity || blockEntity instanceof HopperBlockEntity
                || blockEntity instanceof DispenserBlockEntity) return others.get() ? OTHER_COLOR : 0;
        return 0;
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (highlights.isEmpty()) return;
        Vec3d camera = context.worldState().cameraRenderState.pos;
        MatrixStack matrices = context.matrices();
        VertexConsumer buffer = context.consumers().getBuffer(RenderUtil.boxLayer(throughWalls.get()));

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        for (Highlight h : highlights) {
            BlockPos p = h.pos();
            // Légèrement plus petit qu'un bloc, comme un coffre.
            RenderUtil.drawBox(buffer, matrix, p.getX() + 0.0625f, p.getY(), p.getZ() + 0.0625f,
                    p.getX() + 0.9375f, p.getY() + 0.875f, p.getZ() + 0.9375f, h.color());
        }
        matrices.pop();
    }
}
