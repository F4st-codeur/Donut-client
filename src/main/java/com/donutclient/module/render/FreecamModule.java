package com.donutclient.module.render;

import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.module.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Détache la caméra du joueur : la caméra vole librement (ZQSD, espace, shift) pendant que
 * le joueur reste immobile sur place. La caméra suit la direction où regarde la souris.
 */
public class FreecamModule extends Module {
    private final NumberSetting speed = add(new NumberSetting("Vitesse", 10, 1, 50, 1));

    private ArmorStandEntity camera;

    public FreecamModule() {
        super("Freecam", "Caméra libre : déplace la vue sans bouger ton personnage.", Category.RENDER);
    }

    /** Le mixin d'entrée clavier bloque les déplacements du joueur quand c'est vrai. */
    public boolean isActive() {
        return isEnabled() && camera != null;
    }

    @Override
    protected void onEnable() {
        if (mc.player == null || mc.world == null) {
            setEnabled(false);
            return;
        }
        // Entité invisible qui sert juste de point de vue (jamais ajoutée au monde).
        camera = new ArmorStandEntity(mc.world, mc.player.getX(), mc.player.getY(), mc.player.getZ());
        copyRotation();
        camera.lastX = camera.getX();
        camera.lastY = camera.getY();
        camera.lastZ = camera.getZ();
        mc.setCameraEntity(camera);
    }

    @Override
    protected void onDisable() {
        camera = null;
        if (mc.player != null) mc.setCameraEntity(mc.player);
    }

    @Override
    public void onTick() {
        if (camera == null) return;
        if (mc.world == null || camera.getEntityWorld() != mc.world) {
            // Changement de monde ou de dimension : on coupe.
            setEnabled(false);
            return;
        }
        if (mc.getCameraEntity() != camera) mc.setCameraEntity(camera);

        double forward = axis(mc.options.forwardKey.isPressed(), mc.options.backKey.isPressed());
        double strafe = axis(mc.options.leftKey.isPressed(), mc.options.rightKey.isPressed());
        double vertical = axis(mc.options.jumpKey.isPressed(), mc.options.sneakKey.isPressed());
        double blocksPerTick = speed.get() / 20.0;
        if (mc.options.sprintKey.isPressed()) blocksPerTick *= 2;

        float yawRad = mc.player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        double sin = MathHelper.sin(yawRad);
        double cos = MathHelper.cos(yawRad);
        double dx = (-sin * forward + cos * strafe) * blocksPerTick;
        double dz = (cos * forward + sin * strafe) * blocksPerTick;
        double dy = vertical * blocksPerTick;

        camera.lastX = camera.getX();
        camera.lastY = camera.getY();
        camera.lastZ = camera.getZ();
        camera.setPosition(camera.getX() + dx, camera.getY() + dy, camera.getZ() + dz);
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        copyRotation();
    }

    /** Appelé à chaque frame pour que la caméra suive la souris sans à-coups. */
    public void copyRotation() {
        if (camera == null || mc.player == null) return;
        camera.setYaw(mc.player.getYaw());
        camera.setPitch(mc.player.getPitch());
        camera.lastYaw = mc.player.getYaw();
        camera.lastPitch = mc.player.getPitch();
        camera.setHeadYaw(mc.player.getYaw());
    }

    private static double axis(boolean positive, boolean negative) {
        return (positive ? 1 : 0) - (negative ? 1 : 0);
    }
}
