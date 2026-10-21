package com.donutclient.module.world;

import com.donutclient.module.setting.NumberSetting;
import com.donutclient.module.world.bot.SchematicBotModule;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Bot qui construit tout seul le schéma Litematica chargé, couche par couche en partant du bas. */
public class SchemBuilderModule extends SchematicBotModule {
    private final NumberSetting delay = add(new NumberSetting("Délai (ticks)", 2, 0, 10, 1));

    private String missing;

    public SchemBuilderModule() {
        super("SchemBuilder", "Construit le schéma Litematica avec les blocs de ton inventaire.");
    }

    @Override
    protected int actionDelay() {
        return delay.get();
    }

    @Override
    public String getInfo() {
        return missing != null ? status + ", manque " + missing : status;
    }

    @Override
    protected boolean isTarget(BlockPos pos, BlockState schematic, BlockState world) {
        if (schematic.isAir() || schematic == world || !world.isReplaceable()) return false;
        // Fluides, feu, etc. : pas d'item à poser.
        return schematic.getBlock().asItem() != Items.AIR;
    }

    @Override
    protected double priority(BlockPos pos, Vec3d eyes) {
        // Couche la plus basse d'abord, puis la plus proche.
        return pos.getY() * 1000.0 + eyes.squaredDistanceTo(Vec3d.ofCenter(pos));
    }

    @Override
    protected boolean act(BlockPos pos, BlockState schematic) {
        // Ne pas se bloquer soi-même dans le bloc.
        if (mc.player.getBoundingBox().intersects(new Box(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1))) return false;

        Item item = schematic.getBlock().asItem();
        if (!selectItem(item)) {
            missing = item.getName().getString();
            return false;
        }
        missing = null;

        // Il faut un bloc voisin solide contre lequel poser.
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.offset(direction);
            BlockState neighborState = mc.world.getBlockState(neighbor);
            // Évite d'ouvrir un coffre, un four... au lieu de poser.
            if (neighborState.isReplaceable() || neighborState.hasBlockEntity()) continue;

            Direction side = direction.getOpposite();
            Vec3d hit = Vec3d.ofCenter(neighbor).add(Vec3d.of(side.getVector()).multiply(0.5));
            lookAt(hit);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hit, side, neighbor, false));
            mc.player.swingHand(Hand.MAIN_HAND);
            return true;
        }
        return false;
    }

    /** Met l'item en main, en le ramenant de l'inventaire vers la hotbar si besoin. */
    private boolean selectItem(Item item) {
        PlayerInventory inventory = mc.player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getStack(slot).isOf(item)) {
                inventory.setSelectedSlot(slot);
                return true;
            }
        }
        for (int slot = 9; slot < 36; slot++) {
            if (inventory.getStack(slot).isOf(item)) {
                // Dans l'écran d'inventaire du joueur, les slots 9 à 35 ont le même numéro.
                int hotbar = inventory.getSelectedSlot();
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, hotbar, SlotActionType.SWAP, mc.player);
                return true;
            }
        }
        return false;
    }
}
