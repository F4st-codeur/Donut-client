package com.donutclient.module.world.bot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Tourne la caméra comme le ferait un joueur à la souris :
 * vitesse qui ralentit en approchant de la cible, petites imprécisions,
 * et pas de rotation qui sont des multiples du pas de la souris (selon la sensibilité).
 */
public final class HumanAim {
    private final Random random;

    public HumanAim(Random random) {
        this.random = random;
    }

    /**
     * Fait un pas vers la rotation voulue.
     *
     * <p>Les bots agissent en fin de tick : la rotation donnée ici n'est envoyée au serveur qu'au tick suivant.
     * On renvoie donc {@code true} seulement si la caméra était déjà sur la cible au début de ce tick,
     * c'est-à-dire si le serveur la connaît déjà.
     *
     * @param maxSpeed  vitesse maximale en degrés par tick
     * @param tolerance écart accepté en degrés
     */
    public boolean turnTowards(float yaw, float pitch, float maxSpeed, float tolerance) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        float dYaw = MathHelper.wrapDegrees(yaw - player.getYaw());
        float dPitch = pitch - player.getPitch();
        double distance = Math.sqrt(dYaw * dYaw + dPitch * dPitch);
        if (distance <= tolerance) return true;

        // Rapide au début, plus lent à la fin, avec une vitesse qui varie un peu à chaque tick.
        double speed = Math.min(maxSpeed * (0.75 + random.nextDouble() * 0.5),
                Math.max(distance * (0.3 + random.nextDouble() * 0.3), 1.5 + random.nextDouble()));
        double stepYaw;
        double stepPitch;
        if (distance <= speed) {
            // Dernier pas : on s'arrête près de la cible, rarement pile dessus.
            stepYaw = dYaw + random.nextGaussian() * tolerance * 0.25;
            stepPitch = dPitch + random.nextGaussian() * tolerance * 0.25;
        } else {
            double scale = speed / distance;
            // La main ne va jamais parfaitement droit : légère courbe et tremblement.
            stepYaw = dYaw * scale + random.nextGaussian() * speed * 0.06;
            stepPitch = dPitch * scale + random.nextGaussian() * speed * 0.06;
        }

        double gcd = mouseStep();
        stepYaw = Math.round(stepYaw / gcd) * gcd;
        stepPitch = Math.round(stepPitch / gcd) * gcd;
        player.setYaw(player.getYaw() + (float) stepYaw);
        player.setPitch(MathHelper.clamp(player.getPitch() + (float) stepPitch, -90f, 90f));
        return false;
    }

    /** Écart angulaire entre la caméra et la rotation donnée, en degrés. */
    public static double angleTo(float yaw, float pitch) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        float dYaw = MathHelper.wrapDegrees(yaw - player.getYaw());
        float dPitch = pitch - player.getPitch();
        return Math.sqrt(dYaw * dYaw + dPitch * dPitch);
    }

    /** Rotation {yaw, pitch} pour regarder {@code point} depuis {@code eyes}. */
    public static float[] rotationTo(Vec3d eyes, Vec3d point) {
        double dx = point.x - eyes.x;
        double dy = point.y - eyes.y;
        double dz = point.z - eyes.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (MathHelper.atan2(dz, dx) * MathHelper.DEGREES_PER_RADIAN) - 90f;
        float pitch = (float) -(MathHelper.atan2(dy, horizontal) * MathHelper.DEGREES_PER_RADIAN);
        return new float[]{yaw, pitch};
    }

    /** Plus petite rotation possible avec la souris, comme dans {@code Mouse#updateMouse}. */
    private static double mouseStep() {
        double sensitivity = MinecraftClient.getInstance().options.getMouseSensitivity().getValue();
        double f = sensitivity * 0.6 + 0.2;
        return f * f * f * 8.0 * 0.15;
    }
}
