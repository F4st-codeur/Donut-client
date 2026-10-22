package com.donutclient.module.world.bot;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.VerticallyAttachableBlockItem;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;

/**
 * Trouve comment poser un bloc pour qu'il ait la bonne orientation.
 *
 * <p>L'orientation d'un bloc posé dépend de la direction où regarde le joueur, de la face cliquée
 * et de la hauteur du clic (haut/bas pour les dalles et escaliers). Plutôt que de coder une règle par bloc,
 * on simule la pose avec {@code Block#getPlacementState} pour chaque combinaison possible et on garde
 * celle qui donne l'orientation voulue avec le mouvement de caméra le plus naturel.
 */
public final class PlacementPlanner {
    /** Propriétés qui décrivent l'orientation et que la pose permet de choisir. */
    private static final List<Property<?>> ORIENTATION_PROPERTIES = List.of(
            Properties.FACING, Properties.HORIZONTAL_FACING, Properties.HOPPER_FACING,
            Properties.AXIS, Properties.HORIZONTAL_AXIS, Properties.BLOCK_HALF, Properties.SLAB_TYPE,
            Properties.BLOCK_FACE, Properties.ROTATION, Properties.ORIENTATION, Properties.VERTICAL_DIRECTION);
    /** Inclinaisons essayées quand la rotation ne peut pas simplement regarder le point cliqué. */
    private static final float[] HUMAN_PITCHES = {0f, 20f, -20f, 45f, -45f, 65f, -65f, 80f, -80f, 90f, -90f};
    private static final double[] FACE_OFFSETS = {0.2, 0.5, 0.8};

    /**
     * Une façon de poser : rotation du joueur + clic.
     * {@code exact} : l'orientation obtenue sera la bonne. {@code rotate} : la rotation compte (bloc orientable).
     */
    public record Plan(float yaw, float pitch, BlockHitResult hit, boolean exact, boolean rotate) {
    }

    private PlacementPlanner() {
    }

    /**
     * Cherche une pose pour {@code desired} en {@code pos}, comme le ferait un joueur :
     * clique sur une face visible ({@code canSee}) et choisit la rotation la plus proche de la caméra actuelle. Si possible, la rotation regarde
     * directement le point cliqué ; sinon on garde le clic et on cherche une rotation qui donne
     * la bonne orientation. À défaut, renvoie une pose non exacte (orientation fausse) en regardant le clic,
     * ou {@code null} s'il n'y a aucune face visible.
     */
    public static Plan plan(BlockPos pos, BlockState desired, ItemStack stack, Random random,
                                 BiPredicate<Vec3d, BlockHitResult> canSee) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float savedYaw = mc.player.getYaw();
        float savedPitch = mc.player.getPitch();
        Vec3d eyes = mc.player.getEyePos();
        boolean hasOrientation = ORIENTATION_PROPERTIES.stream().anyMatch(desired::contains);
        Plan best = null;
        double bestCost = Double.MAX_VALUE;
        Plan fallback = null;
        double fallbackCost = Double.MAX_VALUE;

        try {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.offset(direction);
                BlockState neighborState = mc.world.getBlockState(neighbor);
                if (neighborState.isReplaceable() || neighborState.hasBlockEntity()) continue;
                Direction side = direction.getOpposite();
                if (!faceAllowed(desired, stack.getItem(), side)) continue;

                for (double u : FACE_OFFSETS) {
                    for (double v : FACE_OFFSETS) {
                        Vec3d hitPos = facePoint(neighbor, side, u + (random.nextDouble() - 0.5) * 0.15,
                                v + (random.nextDouble() - 0.5) * 0.15);
                        BlockHitResult hit = new BlockHitResult(hitPos, side, neighbor, false);
                        if (!canSee.test(hitPos, hit)) continue;

                        float[] look = HumanAim.rotationTo(eyes, hitPos);
                        // Regarder le point cliqué : c'est ce que fait un joueur.
                        if (!hasOrientation || placesCorrectly(mc, desired, stack, hit, look[0], look[1])) {
                            double cost = angleFrom(savedYaw, savedPitch, look[0], look[1]) + random.nextDouble() * 5;
                            if (cost < bestCost) {
                                bestCost = cost;
                                best = new Plan(look[0], look[1], hit, true, hasOrientation);
                            }
                            continue;
                        }
                        double lookCost = angleFrom(savedYaw, savedPitch, look[0], look[1]);
                        if (lookCost < fallbackCost) {
                            fallbackCost = lookCost;
                            fallback = new Plan(look[0], look[1], hit, false, false);
                        }
                        // Sinon : même clic, mais une rotation qui donne la bonne orientation (moins naturel, pénalisé).
                        for (int i = 0; i < 16; i++) {
                            float yaw = i * 22.5f + (random.nextFloat() - 0.5f) * 8f;
                            for (float pitch : HUMAN_PITCHES) {
                                if (!placesCorrectly(mc, desired, stack, hit, yaw, pitch)) continue;
                                double cost = 200 + angleFrom(savedYaw, savedPitch, yaw, pitch)
                                        + angleFrom(look[0], look[1], yaw, pitch);
                                if (cost < bestCost) {
                                    bestCost = cost;
                                    best = new Plan(yaw, pitch, hit, true, true);
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            mc.player.setYaw(savedYaw);
            mc.player.setPitch(savedPitch);
        }
        return best != null ? best : fallback;
    }

    /** La pose avec la rotation actuelle du joueur donnera-t-elle la bonne orientation ? */
    public static boolean placesCorrectly(BlockState desired, ItemStack stack, BlockHitResult hit) {
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockState result = desired.getBlock().getPlacementState(
                new ItemPlacementContext(mc.player, Hand.MAIN_HAND, stack, hit));
        return result != null && sameOrientation(result, desired);
    }

    private static boolean placesCorrectly(MinecraftClient mc, BlockState desired, ItemStack stack,
                                           BlockHitResult hit, float yaw, float pitch) {
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
        return placesCorrectly(desired, stack, hit);
    }

    private static double angleFrom(float yawA, float pitchA, float yawB, float pitchB) {
        float dYaw = MathHelper.wrapDegrees(yawB - yawA);
        float dPitch = pitchB - pitchA;
        return Math.sqrt(dYaw * dYaw + dPitch * dPitch);
    }

    /** Point de la face {@code side} du bloc {@code neighbor}, aux coordonnées relatives (u, v) de la face. */
    public static Vec3d facePoint(BlockPos neighbor, Direction side, double u, double v) {
        double w = side.getOffsetX() + side.getOffsetY() + side.getOffsetZ() > 0 ? 1.0 : 0.0;
        return switch (side.getAxis()) {
            case X -> new Vec3d(neighbor.getX() + w, neighbor.getY() + u, neighbor.getZ() + v);
            case Y -> new Vec3d(neighbor.getX() + u, neighbor.getY() + w, neighbor.getZ() + v);
            case Z -> new Vec3d(neighbor.getX() + u, neighbor.getY() + v, neighbor.getZ() + w);
        };
    }

    /**
     * Torches, panneaux, etc. : le même item donne la version murale ou au sol selon la face cliquée.
     * {@code side} est la face cliquée du bloc d'appui, donc la direction du bloc posé par rapport à lui.
     */
    private static boolean faceAllowed(BlockState desired, Item item, Direction side) {
        if (!(item instanceof BlockItem blockItem)) return true;
        boolean wallVariant = desired.getBlock() != blockItem.getBlock();
        if (wallVariant) return side.getAxis().isHorizontal();
        if (item instanceof VerticallyAttachableBlockItem) return !side.getAxis().isHorizontal();
        return true;
    }

    private static boolean sameOrientation(BlockState a, BlockState b) {
        for (Property<?> property : ORIENTATION_PROPERTIES) {
            if (!b.contains(property)) continue;
            if (!a.contains(property) || !a.get(property).equals(b.get(property))) return false;
        }
        return true;
    }
}
