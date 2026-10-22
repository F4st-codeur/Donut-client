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
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Bot qui construit tout seul le schéma Litematica chargé, couche par couche en partant du bas. */
public class SchemBuilderModule extends SchematicBotModule {
    private static final int MAX_AIM_TICKS = 40;

    private final NumberSetting delay = add(new NumberSetting("Délai (ticks)", 2, 0, 10, 1));
    private final BooleanSetting strictOrientation = add(new BooleanSetting("Orientation stricte", true));

    private String missing;
    // Pose prévue pour la cible en cours : calculée une fois, puis la caméra tourne jusqu'à elle.
    private PlacementPlanner.Plan plan;
    private int aimTicks;
    private boolean precise;
    private int pendingSwap = -1;

    public SchemBuilderModule() {
        super("SchemBuilder", "Construit le schéma Litematica avec les blocs de ton inventaire.");
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
    protected void onNewTarget() {
        plan = null;
        aimTicks = 0;
        precise = false;
    }

    @Override
    protected boolean act(BlockPos pos, BlockState schematic) {
        // Ne pas se bloquer soi-même dans le bloc.
        if (mc.player.getBoundingBox().intersects(new Box(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1))) return false;

        Item item = schematic.getBlock().asItem();
        int ready = prepareItem(item);
        if (ready < 0) {
            missing = item.getName().getString();
            return false;
        }
        missing = null;
        if (ready > 0) return true;

        ItemStack stack = mc.player.getInventory().getStack(mc.player.getInventory().getSelectedSlot());
        if (plan == null) {
            // Comme un joueur : clique sur une face qu'il voit, avec une rotation proche de la sienne.
            plan = PlacementPlanner.plan(pos, schematic, stack, random,
                    (point, hit) -> canSee(point, hit.getBlockPos(), hit.getSide()));
            if (plan == null) return false;
            if (!plan.exact() && strictOrientation.get()) return false;
            aimTicks = 0;
            precise = false;
        }

        // Tourne la caméra petit à petit ; on ne pose que quand le serveur nous voit viser le bon endroit.
        if (++aimTicks > MAX_AIM_TICKS) return false;
        if (!aim.turnTowards(plan.yaw(), plan.pitch(), aimSpeed(), precise ? 0.4f : 2f)) return true;

        if (!canSee(plan.hit().getPos(), plan.hit().getBlockPos(), plan.hit().getSide())) {
            plan = null; // Quelque chose gêne : on cherche une autre face.
            return true;
        }
        // La caméra est un peu à côté de la rotation prévue : on vérifie que l'orientation reste bonne.
        if (plan.exact() && plan.rotate() && !PlacementPlanner.placesCorrectly(schematic, stack, plan.hit())) {
            if (precise && aimTicks > MAX_AIM_TICKS / 2) return !strictOrientation.get() && place();
            precise = true; // On s'approche plus près de la rotation prévue.
            return true;
        }
        return place();
    }

    private boolean place() {
        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, plan.hit());
        if (result.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
        plan = null;
        aimTicks = 0;
        precise = false;
        delay(delay.get() + randomTicks(2, 1.5));
        return true;
    }

    /**
     * Met l'item en main, en le ramenant de l'inventaire vers la hotbar si besoin, avec le temps qu'il faudrait
     * à un joueur. Renvoie 0 si l'item est en main, 1 s'il faut attendre, -1 s'il n'y en a pas.
     */
    private int prepareItem(Item item) {
        PlayerInventory inventory = mc.player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (!inventory.getStack(slot).isOf(item)) continue;
            if (slot == inventory.getSelectedSlot()) return 0;
            inventory.setSelectedSlot(slot);
            delay(randomTicks(3, 1));
            return 1;
        }
        for (int slot = 9; slot < 36; slot++) {
            if (!inventory.getStack(slot).isOf(item)) continue;
            if (pendingSwap != slot) {
                // Le temps d'ouvrir l'inventaire et de trouver l'item.
                pendingSwap = slot;
                delay(randomTicks(7, 2));
                return 1;
            }
            // Dans l'écran d'inventaire du joueur, les slots 9 à 35 ont le même numéro.
            int syncId = mc.player.playerScreenHandler.syncId;
            mc.interactionManager.clickSlot(syncId, slot, inventory.getSelectedSlot(), SlotActionType.SWAP, mc.player);
            // Puis on referme l'inventaire, comme en jeu.
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
            pendingSwap = -1;
            delay(randomTicks(6, 2));
            return 1;
        }
        pendingSwap = -1;
        return -1;
    }
}
