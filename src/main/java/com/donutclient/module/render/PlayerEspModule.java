package com.donutclient.module.render;

import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.module.setting.BooleanSetting;
import com.donutclient.module.setting.ModeSetting;
import com.donutclient.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Montre les autres joueurs à travers les murs : contour lumineux et/ou boîte. */
public class PlayerEspModule extends Module {
    private static final String GLOW = "Contour";
    private static final String BOX = "Boîte";
    private static final String BOTH = "Les deux";
    private static final int BOX_COLOR = 0x50FF3030;
    private static final int GLOW_COLOR = 0xFF3030;

    private final ModeSetting mode = add(new ModeSetting("Mode", GLOW, GLOW, BOX, BOTH));
    private final BooleanSetting throughWalls = add(new BooleanSetting("À travers les murs", true));

    public PlayerEspModule() {
        super("PlayerESP", "Montre les autres joueurs, même derrière les murs.", Category.RENDER);
    }

    /** Ce joueur doit-il avoir le contour lumineux ? (utilisé par le mixin) */
    public boolean shouldGlow(Entity entity) {
        return isEnabled() && !mode.is(BOX) && isTarget(entity);
    }

    public int getGlowColor() {
        return GLOW_COLOR;
    }

    private boolean isTarget(Entity entity) {
        return entity instanceof PlayerEntity && entity != mc.player && !entity.isSpectator();
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (mode.is(GLOW) || mc.world == null) return;
        float tickProgress = mc.getRenderTickCounter().getTickProgress(true);
        Vec3d camera = context.worldState().cameraRenderState.pos;
        MatrixStack matrices = context.matrices();
        VertexConsumer buffer = context.consumers().getBuffer(RenderUtil.boxLayer(throughWalls.get()));

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        for (Entity entity : mc.world.getEntities()) {
            if (!isTarget(entity)) continue;
            // Position interpolée entre deux ticks pour une boîte fluide.
            Vec3d lerped = entity.getLerpedPos(tickProgress);
            Box box = entity.getBoundingBox().offset(lerped.x - entity.getX(), lerped.y - entity.getY(), lerped.z - entity.getZ());
            RenderUtil.drawBox(buffer, matrix, box, BOX_COLOR);
        }
        matrices.pop();
    }
}
