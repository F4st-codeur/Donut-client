package com.donutclient.module.world;

import com.donutclient.module.setting.ModeSetting;
import com.donutclient.module.world.bot.HumanAim;
import com.donutclient.module.world.bot.PlacementPlanner;
import com.donutclient.module.world.bot.SchematicBotModule;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Bot qui mine tout seul la zone du schéma Litematica chargé, couche par couche en partant du haut. */
public class SchemDiggerModule extends SchematicBotModule {
    private static final String DIFFERENT = "Différents";
    private static final String MATCHING = "Identiques";
    private static final String ALL = "Tout";

    private final ModeSetting mode = add(new ModeSetting("Mode", DIFFERENT, DIFFERENT, MATCHING, ALL));

    // Point visé sur le bloc en cours : choisi une fois par bloc, comme un joueur qui vise un endroit précis.
    private Vec3d aimPoint;
    private Direction aimSide;
    private boolean mining;

    public SchemDiggerModule() {
        super("SchemDigger", "Mine le schéma Litematica. Différents = creuse ce qui ne correspond pas au schéma, "
                + "Identiques = démonte le build du schéma, Tout = vide la zone.");
    }

    @Override
    protected boolean isTarget(BlockPos pos, BlockState schematic, BlockState world) {
        if (world.isAir() || !world.getFluidState().isEmpty()) return false;
        if (world.getHardness(mc.world, pos) < 0) return false; // incassable (bedrock...)
        boolean wanted = switch (mode.get()) {
            case MATCHING -> world == schematic;
            case ALL -> true;
            default -> world != schematic;
        };
        return wanted && !touchesLava(pos);
    }

    private boolean touchesLava(BlockPos pos) {
        for (Direction direction : Direction.values()) {
            FluidState fluid = mc.world.getFluidState(pos.offset(direction));
            if (fluid.isIn(FluidTags.LAVA)) return true;
        }
        return false;
    }

    @Override
    protected double priority(BlockPos pos, Vec3d eyes) {
        // Couche la plus haute d'abord, puis la plus proche.
        return -pos.getY() * 1000.0 + eyes.squaredDistanceTo(Vec3d.ofCenter(pos));
    }

    @Override
    protected void onNewTarget() {
        aimPoint = null;
        mining = false;
    }

    @Override
    protected boolean act(BlockPos pos, BlockState schematic) {
        if (aimPoint == null || !isVisible(aimPoint, pos)) {
            // Il faut voir le bloc pour le miner : on cherche un point visible sur une de ses faces.
            if (!pickAimPoint(pos)) return false;
            if (mining) {
                mc.interactionManager.cancelBlockBreaking();
                mining = false;
            }
        }

        // Change d'outil avant de viser, puis laisse le temps à la main de suivre.
        if (!mining && selectBestTool(mc.world.getBlockState(pos))) {
            delay(randomTicks(3, 1));
            return true;
        }

        float[] rotation = HumanAim.rotationTo(mc.player.getEyePos(), aimPoint);
        if (!aim.turnTowards(rotation[0], rotation[1], aimSpeed(), 2.5f)) {
            // Tant que le serveur ne nous voit pas regarder le bloc, on ne commence pas à le casser.
            if (!mining) return true;
        }

        mining = true;
        mc.interactionManager.updateBlockBreakingProgress(pos, aimSide);
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    /** Choisit au hasard un point visible sur une face du bloc, en préférant la face tournée vers le joueur. */
    private boolean pickAimPoint(BlockPos pos) {
        Vec3d center = Vec3d.ofCenter(pos);
        Vec3d eyes = mc.player.getEyePos();
        Direction facing = Direction.getFacing(eyes.x - center.x, eyes.y - center.y, eyes.z - center.z);
        for (int attempt = 0; attempt < 24; attempt++) {
            Direction side = attempt < 8 ? facing : Direction.values()[random.nextInt(6)];
            Vec3d point = randomPointOnFace(pos, side);
            BlockHitResult hit = raycastTo(point);
            // Blocs pas pleins (herbe, fleurs...) : on prend la face réellement touchée.
            if (hit != null && hit.getBlockPos().equals(pos)) {
                aimPoint = hit.getPos();
                aimSide = hit.getSide();
                return true;
            }
        }
        return false;
    }

    private boolean isVisible(Vec3d point, BlockPos pos) {
        BlockHitResult hit = raycastTo(point);
        return hit != null && hit.getBlockPos().equals(pos);
    }

    /** Point au hasard sur une face, plutôt vers le milieu. */
    private Vec3d randomPointOnFace(BlockPos pos, Direction side) {
        double u = 0.5 + clampedGaussian() * 0.3;
        double v = 0.5 + clampedGaussian() * 0.3;
        return PlacementPlanner.facePoint(pos, side, u, v);
    }

    private double clampedGaussian() {
        return Math.max(-1.0, Math.min(1.0, random.nextGaussian() * 0.5));
    }

    /** Prend le meilleur outil de la hotbar. Renvoie {@code true} si l'objet en main a changé. */
    private boolean selectBestTool(BlockState state) {
        int current = mc.player.getInventory().getSelectedSlot();
        int best = current;
        float bestSpeed = mc.player.getInventory().getStack(best).getMiningSpeedMultiplier(state);
        for (int slot = 0; slot < 9; slot++) {
            float speed = mc.player.getInventory().getStack(slot).getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = slot;
            }
        }
        if (best == current) return false;
        mc.player.getInventory().setSelectedSlot(best);
        return true;
    }

    @Override
    protected void stopActing() {
        if (mining && mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
        mining = false;
    }
}
