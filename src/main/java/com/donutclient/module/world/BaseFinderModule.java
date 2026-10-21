package com.donutclient.module.world;

import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.module.setting.BooleanSetting;
import com.donutclient.module.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.AbstractPlantStemBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repère les signes d'activité d'un joueur : les plantes ne poussent que dans les chunks chargés,
 * donc plus elles ont grandi, plus quelqu'un est resté à côté longtemps.
 *
 * <ul>
 *     <li><b>Kelp</b> : à la génération, le haut d'un kelp a un âge de 20 à 23. Un haut à l'âge
 *     maximum (25) a donc forcément poussé après la génération du monde.</li>
 *     <li><b>Améthyste</b> : surligne les grappes d'améthyste (et en option les gros bourgeons).</li>
 * </ul>
 */
public class BaseFinderModule extends Module {
    private static final int KELP_COLOR = 0x6000FF60;
    private static final int AMETHYST_COLOR = 0x60C060FF;
    private static final int BUD_COLOR = 0x50FF90FF;
    private static final int CHUNKS_PER_TICK = 4;
    private static final int MAX_RESULTS = 3000;

    private final BooleanSetting kelp = add(new BooleanSetting("Kelp", true));
    private final BooleanSetting amethyst = add(new BooleanSetting("Amethyst", true));
    private final BooleanSetting buds = add(new BooleanSetting("Gros bourgeons", false));
    private final NumberSetting minKelpHeight = add(new NumberSetting("Kelp min (blocs)", 1, 1, 30, 1));
    private final NumberSetting radius = add(new NumberSetting("Rayon (chunks)", 6, 2, 16, 1));

    private record Highlight(int x, int minY, int z, int maxY, int color) {
    }

    private final Map<Long, List<Highlight>> results = new HashMap<>();
    private List<int[]> scanOrder = List.of();
    private int scanOrderRadius = -1;
    private int cursor;
    private int kelpCount, amethystCount;

    public BaseFinderModule() {
        super("BaseFinder", "Surligne le kelp et l'améthyste qui ont poussé (= activité).", Category.WORLD);
    }

    @Override
    protected void onDisable() {
        results.clear();
        cursor = 0;
    }

    @Override
    public void onTick() {
        if (mc.world == null) return;
        int r = radius.get();
        if (r != scanOrderRadius) buildScanOrder(r);

        int centerX = mc.player.getChunkPos().x;
        int centerZ = mc.player.getChunkPos().z;
        for (int i = 0; i < CHUNKS_PER_TICK; i++) {
            if (cursor >= scanOrder.size()) {
                cursor = 0;
                pruneFarChunks(centerX, centerZ, r);
                recount();
            }
            int[] offset = scanOrder.get(cursor++);
            scanChunk(centerX + offset[0], centerZ + offset[1]);
        }
    }

    private void buildScanOrder(int r) {
        List<int[]> order = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) order.add(new int[]{dx, dz});
        }
        order.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]));
        scanOrder = order;
        scanOrderRadius = r;
        cursor = 0;
    }

    private void scanChunk(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
            results.remove(key);
            return;
        }
        WorldChunk chunk = mc.world.getChunk(chunkX, chunkZ);
        List<Highlight> found = new ArrayList<>();
        ChunkSection[] sections = chunk.getSectionArray();
        int bottomSection = chunk.getBottomSectionCoord();

        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section == null || section.isEmpty() || !section.hasAny(this::isCandidate)) continue;
            int baseY = (bottomSection + i) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!isCandidate(state)) continue;
                        Highlight h = check(state, (chunkX << 4) + x, baseY + y, (chunkZ << 4) + z);
                        if (h != null) found.add(h);
                    }
                }
            }
        }

        if (found.isEmpty()) results.remove(key);
        else results.put(key, found);
    }

    private boolean isCandidate(BlockState state) {
        return (kelp.get() && state.isOf(Blocks.KELP))
                || (amethyst.get() && state.isOf(Blocks.AMETHYST_CLUSTER))
                || (buds.get() && state.isOf(Blocks.LARGE_AMETHYST_BUD));
    }

    private Highlight check(BlockState state, int x, int y, int z) {
        if (state.isOf(Blocks.KELP)) {
            // Haut de kelp à l'âge max : il a poussé depuis la génération du monde.
            if (state.get(AbstractPlantStemBlock.AGE) < AbstractPlantStemBlock.MAX_AGE) return null;
            BlockPos.Mutable pos = new BlockPos.Mutable(x, y - 1, z);
            while (mc.world.getBlockState(pos).isOf(Blocks.KELP_PLANT)) pos.move(0, -1, 0);
            int bottom = pos.getY() + 1;
            if (y - bottom + 1 < minKelpHeight.get()) return null;
            return new Highlight(x, bottom, z, y, KELP_COLOR);
        }
        if (state.isOf(Blocks.AMETHYST_CLUSTER)) return new Highlight(x, y, z, y, AMETHYST_COLOR);
        return new Highlight(x, y, z, y, BUD_COLOR);
    }

    private void pruneFarChunks(int centerX, int centerZ, int r) {
        results.keySet().removeIf(key -> {
            int cx = (int) (key >> 32);
            int cz = (int) (long) key;
            return Math.abs(cx - centerX) > r || Math.abs(cz - centerZ) > r;
        });
    }

    private void recount() {
        kelpCount = 0;
        amethystCount = 0;
        for (List<Highlight> list : results.values()) {
            for (Highlight h : list) {
                if (h.color() == KELP_COLOR) kelpCount++;
                else amethystCount++;
            }
        }
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    @Override
    public String getInfo() {
        return kelpCount + " kelp, " + amethystCount + " améthyste";
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (results.isEmpty()) return;
        Vec3d camera = context.worldState().cameraRenderState.pos;
        MatrixStack matrices = context.matrices();
        VertexConsumer buffer = context.consumers().getBuffer(RenderLayers.debugFilledBox());

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int drawn = 0;
        for (List<Highlight> list : results.values()) {
            for (Highlight h : list) {
                if (drawn++ >= MAX_RESULTS) break;
                drawBox(buffer, matrix, h.x(), h.minY(), h.z(), h.x() + 1, h.maxY() + 1, h.z() + 1, h.color());
            }
        }
        matrices.pop();
    }

    private static void drawBox(VertexConsumer b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int c) {
        // Nord
        b.vertex(m, x1, y1, z1).color(c); b.vertex(m, x2, y1, z1).color(c);
        b.vertex(m, x2, y2, z1).color(c); b.vertex(m, x1, y2, z1).color(c);
        // Sud
        b.vertex(m, x2, y1, z2).color(c); b.vertex(m, x1, y1, z2).color(c);
        b.vertex(m, x1, y2, z2).color(c); b.vertex(m, x2, y2, z2).color(c);
        // Ouest
        b.vertex(m, x1, y1, z2).color(c); b.vertex(m, x1, y1, z1).color(c);
        b.vertex(m, x1, y2, z1).color(c); b.vertex(m, x1, y2, z2).color(c);
        // Est
        b.vertex(m, x2, y1, z1).color(c); b.vertex(m, x2, y1, z2).color(c);
        b.vertex(m, x2, y2, z2).color(c); b.vertex(m, x2, y2, z1).color(c);
        // Haut
        b.vertex(m, x1, y2, z1).color(c); b.vertex(m, x2, y2, z1).color(c);
        b.vertex(m, x2, y2, z2).color(c); b.vertex(m, x1, y2, z2).color(c);
        // Bas
        b.vertex(m, x1, y1, z2).color(c); b.vertex(m, x2, y1, z2).color(c);
        b.vertex(m, x2, y1, z1).color(c); b.vertex(m, x1, y1, z1).color(c);
    }
}
