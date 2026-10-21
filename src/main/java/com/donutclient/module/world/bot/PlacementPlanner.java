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
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Trouve comment poser un bloc pour qu'il ait la bonne orientation.
 *
 * <p>L'orientation d'un bloc posé dépend de la direction où regarde le joueur, de la face cliquée
 * et de la hauteur du clic (haut/bas pour les dalles et escaliers). Plutôt que de coder une règle par bloc,
 * on simule la pose avec {@code Block#getPlacementState} pour chaque combinaison possible et on garde
 * la première qui donne l'orientation voulue.
 */
public final class PlacementPlanner {
    /** Propriétés qui décrivent l'orientation et que la pose permet de choisir. */
    private static final List<Property<?>> ORIENTATION_PROPERTIES = List.of(
            Properties.FACING, Properties.HORIZONTAL_FACING, Properties.HOPPER_FACING,
            Properties.AXIS, Properties.HORIZONTAL_AXIS, Properties.BLOCK_HALF, Properties.SLAB_TYPE,
            Properties.BLOCK_FACE, Properties.ROTATION, Properties.ORIENTATION, Properties.VERTICAL_DIRECTION);
    private static final float[] PITCHES = {0f, 90f, -90f};
    private static final double[] HIT_HEIGHTS = {0.25, 0.75};

    /**
     * Une façon de poser : rotation du joueur + clic.
     * {@code exact} : l'orientation obtenue sera la bonne. {@code rotate} : la rotation compte (bloc orientable).
     */
    public record Plan(float yaw, float pitch, BlockHitResult hit, boolean exact, boolean rotate) {
    }

    private PlacementPlanner() {
    }

    /**
     * Cherche une pose pour {@code desired} en {@code pos}. Renvoie une pose exacte si possible,
     * sinon une pose quelconque (orientation peut-être fausse), ou {@code null} s'il n'y a aucun appui.
     */
    public static Plan plan(BlockPos pos, BlockState desired, ItemStack stack) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float savedYaw = mc.player.getYaw();
        float savedPitch = mc.player.getPitch();
        boolean hasOrientation = ORIENTATION_PROPERTIES.stream().anyMatch(desired::contains);
        Plan fallback = null;

        try {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.offset(direction);
                BlockState neighborState = mc.world.getBlockState(neighbor);
                // Il faut un appui, et pas un bloc qu'on ouvrirait en cliquant (coffre, four...).
                if (neighborState.isReplaceable() || neighborState.hasBlockEntity()) continue;

                Direction side = direction.getOpposite();
                if (!faceAllowed(desired, stack.getItem(), side)) continue;

                for (double height : side.getAxis().isHorizontal() ? HIT_HEIGHTS : new double[]{0.5}) {
                    Vec3d hitPos = faceHit(neighbor, side, height);
                    BlockHitResult hit = new BlockHitResult(hitPos, side, neighbor, false);
                    if (fallback == null) fallback = new Plan(savedYaw, savedPitch, hit, !hasOrientation, false);
                    if (!hasOrientation) return fallback;

                    // 16 directions pour les blocs à 16 rotations (panneaux, têtes, bannières).
                    for (int i = 0; i < 16; i++) {
                        float yaw = i * 22.5f;
                        for (float pitch : PITCHES) {
                            mc.player.setYaw(yaw);
                            mc.player.setPitch(pitch);
                            ItemPlacementContext context = new ItemPlacementContext(mc.player, Hand.MAIN_HAND, stack, hit);
                            BlockState result = desired.getBlock().getPlacementState(context);
                            if (result != null && sameOrientation(result, desired)) {
                                return new Plan(yaw, pitch, hit, true, true);
                            }
                        }
                    }
                }
            }
        } finally {
            mc.player.setYaw(savedYaw);
            mc.player.setPitch(savedPitch);
        }
        return fallback;
    }

    /** Point du clic sur la face {@code side} du bloc {@code neighbor}, à la hauteur relative donnée. */
    private static Vec3d faceHit(BlockPos neighbor, Direction side, double height) {
        Vec3d center = Vec3d.ofCenter(neighbor).add(Vec3d.of(side.getVector()).multiply(0.5));
        return side.getAxis().isHorizontal() ? new Vec3d(center.x, neighbor.getY() + height, center.z) : center;
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
