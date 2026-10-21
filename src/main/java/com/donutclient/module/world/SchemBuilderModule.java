package com.donutclient.module.world;

import com.donutclient.module.setting.BooleanSetting;
import com.donutclient.module.setting.NumberSetting;
import com.donutclient.module.world.bot.PlacementPlanner;
import com.donutclient.module.world.bot.SchematicBotModule;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Bot qui construit tout seul le schéma Litematica chargé, couche par couche en partant du bas. */
public class SchemBuilderModule extends SchematicBotModule {
    private final NumberSetting delay = add(new NumberSetting("Délai (ticks)", 2, 0, 10, 1));
    private final BooleanSetting strictOrientation = add(new BooleanSetting("Orientation stricte", true));

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
        // Moitié haute d'une porte, tête d'un lit... : posée automatiquement avec l'autre moitié.
        if (schematic.contains(Properties.DOUBLE_BLOCK_HALF)
                && schematic.get(Properties.DOUBLE_BLOCK_HALF).asString().equals("upper")) return false;
        if (schematic.contains(Properties.BED_PART)
                && schematic.get(Properties.BED_PART).asString().equals("head")) return false;
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

        // Choisit la face cliquée et la rotation qui donnent la bonne orientation.
        ItemStack stack = mc.player.getInventory().getStack(mc.player.getInventory().getSelectedSlot());
        PlacementPlanner.Plan plan = PlacementPlanner.plan(pos, schematic, stack);
        if (plan == null) return false;
        if (!plan.exact() && strictOrientation.get()) return false;

        float yaw = plan.yaw();
        float pitch = plan.pitch();
        if (plan.rotate()) {
            // Le serveur utilise la rotation qu'il connaît : on la lui envoie avant de poser.
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
        } else {
            lookAt(plan.hit().getPos());
        }
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, plan.hit());
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
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
