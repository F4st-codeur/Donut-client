package com.donutclient.module.world;

import com.donutclient.module.setting.ModeSetting;
import com.donutclient.module.world.bot.SchematicBotModule;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Bot qui mine tout seul la zone du schéma Litematica chargé, couche par couche en partant du haut. */
public class SchemDiggerModule extends SchematicBotModule {
    private static final String DIFFERENT = "Différents";
    private static final String MATCHING = "Identiques";
    private static final String ALL = "Tout";

    private final ModeSetting mode = add(new ModeSetting("Mode", DIFFERENT, DIFFERENT, MATCHING, ALL));

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
    protected boolean act(BlockPos pos, BlockState schematic) {
        Vec3d center = Vec3d.ofCenter(pos);
        Vec3d eyes = mc.player.getEyePos();
        Direction side = Direction.getFacing(eyes.x - center.x, eyes.y - center.y, eyes.z - center.z);

        selectBestTool(mc.world.getBlockState(pos));
        lookAt(center);
        mc.interactionManager.updateBlockBreakingProgress(pos, side);
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private void selectBestTool(BlockState state) {
        int best = mc.player.getInventory().getSelectedSlot();
        float bestSpeed = mc.player.getInventory().getStack(best).getMiningSpeedMultiplier(state);
        for (int slot = 0; slot < 9; slot++) {
            float speed = mc.player.getInventory().getStack(slot).getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = slot;
            }
        }
        mc.player.getInventory().setSelectedSlot(best);
    }

    @Override
    protected void stopActing() {
        if (mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
    }
}
