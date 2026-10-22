package com.donutclient.module.world;

import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.module.setting.BooleanSetting;
import com.donutclient.module.setting.NumberSetting;
import com.donutclient.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Analyse les chunks chargés et donne à chacun un score selon les blocs « de joueur » qu'il contient
 * (coffres, hoppers, shulkers, fours, enchant, beacon...). Au-dessus du seuil, le chunk est considéré
 * comme une base : il est surligné et signalé dans le chat.
 */
public class BaseScannerModule extends Module {
    private static final int SCAN_INTERVAL = 20;
    private static final int BASE_COLOR = 0x30FF2020;

    private final NumberSetting threshold = add(new NumberSetting("Seuil", 20, 5, 200, 5));
    private final BooleanSetting chat = add(new BooleanSetting("Message chat", true));
    private final BooleanSetting highlight = add(new BooleanSetting("Surligner", true));
    private final BooleanSetting throughWalls = add(new BooleanSetting("À travers les murs", true));

    /** Chunk détecté comme base : score et hauteur des blocs trouvés. */
    private record Base(int chunkX, int chunkZ, int score, int minY, int maxY, String summary) {
    }

    private final Map<Long, Base> bases = new HashMap<>();
    private final Set<Long> announced = new HashSet<>();
    private int timer;

    public BaseScannerModule() {
        super("BaseScanner", "Analyse les chunks et repère les bases (coffres, hoppers, shulkers...).", Category.WORLD);
    }

    @Override
    protected void onDisable() {
        bases.clear();
        announced.clear();
    }

    @Override
    public String getInfo() {
        return bases.size() + (bases.size() > 1 ? " bases" : " base");
    }

    @Override
    public void onTick() {
        if (mc.world == null || ++timer < SCAN_INTERVAL) return;
        timer = 0;

        bases.clear();
        int radius = mc.options.getClampedViewDistance();
        int centerX = mc.player.getChunkPos().x;
        int centerZ = mc.player.getChunkPos().z;
        for (int cx = centerX - radius; cx <= centerX + radius; cx++) {
            for (int cz = centerZ - radius; cz <= centerZ + radius; cz++) {
                if (!mc.world.getChunkManager().isChunkLoaded(cx, cz)) continue;
                Base base = analyze(mc.world.getChunk(cx, cz), cx, cz);
                if (base == null) continue;
                long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
                bases.put(key, base);
                if (chat.get() && announced.add(key)) announce(base);
            }
        }
    }

    private Base analyze(WorldChunk chunk, int cx, int cz) {
        int score = 0;
        int storage = 0, hoppers = 0, shulkers = 0, spawners = 0;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            int points = points(blockEntity);
            if (blockEntity instanceof MobSpawnerBlockEntity) spawners++;
            if (points == 0) continue;
            score += points;
            if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity) storage++;
            if (blockEntity instanceof HopperBlockEntity) hoppers++;
            if (blockEntity instanceof ShulkerBoxBlockEntity) shulkers++;
            int y = blockEntity.getPos().getY();
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        // Donjon naturel : un spawner avec un ou deux coffres, ce n'est pas une base.
        if (spawners > 0) score -= Math.min(storage, 2 * spawners) * 2;
        if (score < threshold.get()) return null;

        String summary = storage + " coffres, " + hoppers + " hoppers, " + shulkers + " shulkers";
        return new Base(cx, cz, score, minY, maxY, summary);
    }

    /** Points par bloc : plus un bloc est rare dans la nature, plus il compte. */
    private static int points(BlockEntity be) {
        if (be instanceof ShulkerBoxBlockEntity) return 5;
        if (be instanceof BeaconBlockEntity) return 10;
        if (be instanceof EnchantingTableBlockEntity) return 4;
        if (be instanceof HopperBlockEntity) return 3;
        if (be instanceof EnderChestBlockEntity) return 3;
        if (be instanceof BrewingStandBlockEntity) return 3;
        if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) return 2;
        if (be instanceof AbstractFurnaceBlockEntity) return 2;
        if (be instanceof BedBlockEntity) return 2;
        if (be instanceof DispenserBlockEntity) return 1;
        if (be instanceof SignBlockEntity || be instanceof BannerBlockEntity) return 1;
        return 0;
    }

    private void announce(Base base) {
        int x = (base.chunkX() << 4) + 8;
        int z = (base.chunkZ() << 4) + 8;
        Text message = Text.literal("[BaseScanner] ").formatted(Formatting.RED)
                .append(Text.literal("Base possible en " + x + " " + base.maxY() + " " + z).formatted(Formatting.WHITE))
                .append(Text.literal(" (score " + base.score() + " : " + base.summary() + ")").formatted(Formatting.GRAY));
        mc.inGameHud.getChatHud().addMessage(message);
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (!highlight.get() || bases.isEmpty()) return;
        Vec3d camera = context.worldState().cameraRenderState.pos;
        MatrixStack matrices = context.matrices();
        VertexConsumer buffer = context.consumers().getBuffer(RenderUtil.boxLayer(throughWalls.get()));

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        for (Base base : bases.values()) {
            float x = base.chunkX() << 4;
            float z = base.chunkZ() << 4;
            RenderUtil.drawBox(buffer, matrix, x, base.minY(), z, x + 16, base.maxY() + 1, z + 16, BASE_COLOR);
        }
        matrices.pop();
    }
}
