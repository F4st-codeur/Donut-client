package com.donutclient.module.world.bot;

import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.util.LitematicaBridge;
import com.donutclient.util.LitematicaBridge.Region;
import net.minecraft.block.BlockState;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base des bots qui travaillent sur le schéma Litematica chargé :
 * scanne les zones du schéma petit à petit, choisit la cible la plus proche,
 * marche jusqu'à elle en suivant un chemin calculé par {@link Pathfinder} puis agit dessus.
 */
public abstract class SchematicBotModule extends Module {
    private static final int SCAN_BUDGET_PER_TICK = 40_000;
    private static final int MAX_TARGETS = 200_000;
    private static final int STUCK_TICKS = 40;
    private static final int MAX_PATH_FAILURES = 3;
    private static final int BLACKLIST_TICKS = 400;
    protected static final double REACH = 4.5;

    // Scan incrémental : on remplit "pending" puis on le bascule dans "targets" à la fin d'une passe.
    private List<Region> regions = List.of();
    private int regionIndex;
    private long regionCursor;
    private List<BlockPos> pending = new ArrayList<>();
    private List<BlockPos> targets = new ArrayList<>();
    private boolean scannedOnce;

    private final Map<BlockPos, Integer> blacklist = new HashMap<>();
    private BlockPos walkTarget;
    private List<BlockPos> path;
    private int pathIndex;
    private int pathFailures;
    private Vec3d lastPos;
    private int stuckTicks;
    private int cooldown;
    private int ticks;
    protected String status = "";

    protected SchematicBotModule(String name, String description) {
        super(name, description, Category.WORLD);
    }

    /** Ce bloc doit-il être traité ? (appelé pendant le scan, doit être rapide) */
    protected abstract boolean isTarget(BlockPos pos, BlockState schematic, BlockState world);

    /** Ordre de priorité : plus petit = traité en premier. */
    protected abstract double priority(BlockPos pos, Vec3d eyes);

    /** Agit sur une cible à portée. Renvoie {@code false} si c'est impossible pour l'instant. */
    protected abstract boolean act(BlockPos pos, BlockState schematic);

    /** Nombre de ticks à attendre après une action réussie. */
    protected int actionDelay() {
        return 0;
    }

    /** Appelé quand le bot n'agit plus (cible perdue, module coupé). */
    protected void stopActing() {
    }

    @Override
    protected void onEnable() {
        regions = List.of();
        targets = new ArrayList<>();
        pending = new ArrayList<>();
        blacklist.clear();
        walkTarget = null;
        path = null;
        scannedOnce = false;
        status = "";
    }

    @Override
    protected void onDisable() {
        stopActing();
        releaseMovement();
    }

    @Override
    public String getInfo() {
        return status;
    }

    @Override
    public void onTick() {
        ticks++;
        if (mc.currentScreen != null) {
            stopActing();
            releaseMovement();
            return;
        }
        World schematic = LitematicaBridge.getSchematicWorld();
        if (!LitematicaBridge.isAvailable() || schematic == null) {
            status = "Litematica absent";
            releaseMovement();
            return;
        }
        scan(schematic);
        blacklist.values().removeIf(until -> until < ticks);

        BlockPos target = pickTarget(schematic);
        if (target == null) {
            status = !scannedOnce ? "scan..." : regions.isEmpty() ? "aucun schéma" : "terminé";
            stopActing();
            releaseMovement();
            return;
        }
        status = targets.size() + " restants";

        Vec3d eyes = mc.player.getEyePos();
        BlockPos feet = mc.player.getBlockPos();
        // Si le joueur est dans le bloc cible (ex. : bloc à poser), il doit d'abord se décaler.
        boolean insideTarget = target.equals(feet) || target.equals(feet.up());
        if (!insideTarget && eyes.distanceTo(Vec3d.ofCenter(target)) <= REACH) {
            releaseMovement();
            stuckTicks = 0;
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            if (act(target, schematic.getBlockState(target))) {
                cooldown = actionDelay();
            } else {
                giveUp(target);
                stopActing();
            }
        } else {
            stopActing();
            walkTowards(target);
        }
    }

    private void scan(World schematic) {
        if (regions.isEmpty() || regionIndex >= regions.size()) {
            if (!regions.isEmpty()) scannedOnce = true;
            targets = pending;
            pending = new ArrayList<>();
            regions = LitematicaBridge.getRegions();
            regionIndex = 0;
            regionCursor = 0;
            if (regions.isEmpty()) {
                scannedOnce = true;
                return;
            }
        }

        int budget = SCAN_BUDGET_PER_TICK;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        while (budget-- > 0 && regionIndex < regions.size()) {
            Region r = regions.get(regionIndex);
            long sizeX = r.maxX() - r.minX() + 1;
            long sizeZ = r.maxZ() - r.minZ() + 1;
            long i = regionCursor++;
            int x = r.minX() + (int) (i % sizeX);
            int z = r.minZ() + (int) ((i / sizeX) % sizeZ);
            int y = r.minY() + (int) (i / (sizeX * sizeZ));
            pos.set(x, y, z);
            if (pending.size() < MAX_TARGETS && isLoaded(schematic, x, z) && isLoaded(mc.world, x, z)
                    && isTarget(pos, schematic.getBlockState(pos), mc.world.getBlockState(pos))) {
                pending.add(pos.toImmutable());
            }
            if (regionCursor >= r.volume()) {
                regionIndex++;
                regionCursor = 0;
            }
        }
    }

    private static boolean isLoaded(World world, int x, int z) {
        return world.getChunkManager().isChunkLoaded(x >> 4, z >> 4);
    }

    private BlockPos pickTarget(World schematic) {
        // On garde la même cible tant qu'elle est valide, pour ne pas recalculer le chemin sans arrêt.
        if (walkTarget != null && !blacklist.containsKey(walkTarget)
                && isTarget(walkTarget, schematic.getBlockState(walkTarget), mc.world.getBlockState(walkTarget))) {
            return walkTarget;
        }
        Vec3d eyes = mc.player.getEyePos();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (BlockPos pos : targets) {
            if (blacklist.containsKey(pos)) continue;
            // Le scan date d'une passe : on revérifie que le bloc est toujours à traiter.
            if (!isTarget(pos, schematic.getBlockState(pos), mc.world.getBlockState(pos))) continue;
            double score = priority(pos, eyes);
            if (score < bestScore) {
                bestScore = score;
                best = pos;
            }
        }
        if (best != null && !best.equals(walkTarget)) {
            walkTarget = best;
            path = null;
            pathFailures = 0;
            stuckTicks = 0;
        }
        return best;
    }

    private void walkTowards(BlockPos target) {
        if (path == null) {
            BlockPos feet = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() + 0.2, mc.player.getZ());
            path = Pathfinder.find(mc.world, feet, target, pos -> canReachFrom(pos, target));
            pathIndex = 1;
            if (path == null) {
                giveUp(target);
                return;
            }
        }
        if (pathIndex >= path.size()) {
            // Fin du chemin (partiel ou non) mais cible encore hors de portée : on recalcule.
            path = null;
            if (++pathFailures > MAX_PATH_FAILURES) giveUp(target);
            return;
        }

        BlockPos node = path.get(pathIndex);
        Vec3d player = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        double dx = node.getX() + 0.5 - player.x;
        double dz = node.getZ() + 0.5 - player.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        if (horizontal < 0.35 && Math.abs(player.y - node.getY()) < 1.2) {
            pathIndex++;
            stuckTicks = 0;
            return;
        }
        // Trop loin du chemin (chute, poussé...) : on recalcule.
        if (horizontal > 3 || Math.abs(player.y - node.getY()) > 4) {
            path = null;
            return;
        }

        mc.player.setYaw((float) (MathHelper.atan2(dz, dx) * MathHelper.DEGREES_PER_RADIAN) - 90f);
        mc.player.setPitch(0f);
        mc.options.forwardKey.setPressed(true);
        boolean climb = node.getY() > player.y + 0.5;
        mc.options.jumpKey.setPressed(mc.player.isOnGround() && (climb || mc.player.horizontalCollision));

        if (lastPos != null && lastPos.squaredDistanceTo(player) < 0.0025) {
            if (++stuckTicks > STUCK_TICKS) {
                stuckTicks = 0;
                path = null;
                if (++pathFailures > MAX_PATH_FAILURES) giveUp(target);
            }
        } else {
            stuckTicks = 0;
        }
        lastPos = player;
    }

    /** Depuis cette position (pieds), la cible est-elle à portée sans être dans le joueur ? */
    private static boolean canReachFrom(BlockPos feet, BlockPos target) {
        if (feet.equals(target) || feet.up().equals(target)) return false;
        Vec3d eyes = new Vec3d(feet.getX() + 0.5, feet.getY() + 1.62, feet.getZ() + 0.5);
        return eyes.distanceTo(Vec3d.ofCenter(target)) <= REACH - 0.3;
    }

    private void giveUp(BlockPos target) {
        blacklist.put(target, ticks + BLACKLIST_TICKS);
        walkTarget = null;
        path = null;
        pathFailures = 0;
        releaseMovement();
    }

    private void releaseMovement() {
        if (mc.options == null) return;
        mc.options.forwardKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        KeyBinding.updatePressedStates();
    }

    /** Oriente la caméra du joueur vers un point. */
    protected void lookAt(Vec3d point) {
        Vec3d eyes = mc.player.getEyePos();
        double dx = point.x - eyes.x;
        double dy = point.y - eyes.y;
        double dz = point.z - eyes.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        mc.player.setYaw((float) (MathHelper.atan2(dz, dx) * MathHelper.DEGREES_PER_RADIAN) - 90f);
        mc.player.setPitch((float) -(MathHelper.atan2(dy, horizontal) * MathHelper.DEGREES_PER_RADIAN));
    }
}
