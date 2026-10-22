package com.donutclient.module.world.bot;

import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.module.setting.BooleanSetting;
import com.donutclient.module.setting.NumberSetting;
import com.donutclient.util.LitematicaBridge;
import com.donutclient.util.LitematicaBridge.Region;
import net.minecraft.block.BlockState;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Base des bots qui travaillent sur le schéma Litematica chargé :
 * scanne les zones du schéma petit à petit, choisit la cible la plus proche,
 * marche jusqu'à elle en suivant un chemin calculé par {@link Pathfinder} puis agit dessus.
 *
 * <p>Pour ressembler à un joueur : la caméra tourne progressivement ({@link HumanAim}),
 * le bot hésite un peu avant chaque bloc, n'utilise pas toujours toute sa portée
 * et fait de temps en temps une courte pause.
 */
public abstract class SchematicBotModule extends Module {
    private static final int SCAN_BUDGET_PER_TICK = 40_000;
    private static final int MAX_TARGETS = 200_000;
    private static final int STUCK_TICKS = 40;
    private static final int MAX_PATH_FAILURES = 3;
    private static final int BLACKLIST_TICKS = 400;
    protected static final double REACH = 4.5;

    protected final Random random = new Random();
    protected final HumanAim aim = new HumanAim(random);
    private final NumberSetting aimSpeed = add(new NumberSetting("Vitesse visée", 20, 5, 60, 5));
    private final BooleanSetting pauses = add(new BooleanSetting("Pauses", true));

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
    private double reach = REACH;
    private float walkPitch;
    private int blocksBeforePause;
    private int pauseTicks;
    protected String status = "";

    protected SchematicBotModule(String name, String description) {
        super(name, description, Category.WORLD);
    }

    /** Ce bloc doit-il être traité ? (appelé pendant le scan, doit être rapide) */
    protected abstract boolean isTarget(BlockPos pos, BlockState schematic, BlockState world);

    /** Ordre de priorité : plus petit = traité en premier. */
    protected abstract double priority(BlockPos pos, Vec3d eyes);

    /**
     * Agit sur une cible à portée (un pas à la fois : viser, puis agir).
     * Renvoie {@code false} si c'est impossible pour l'instant. Utiliser {@link #delay} pour attendre.
     */
    protected abstract boolean act(BlockPos pos, BlockState schematic);

    /** Appelé quand le bot n'agit plus (cible perdue, module coupé). */
    protected void stopActing() {
    }

    /** Appelé quand le bot passe à une nouvelle cible. */
    protected void onNewTarget() {
    }

    /** Vitesse maximale de la caméra, en degrés par tick. */
    protected float aimSpeed() {
        return aimSpeed.get();
    }

    /** Attend au moins {@code ticks} ticks avant la prochaine action. */
    protected void delay(int ticks) {
        cooldown = Math.max(cooldown, ticks);
    }

    /** Délai aléatoire autour de {@code mean} ticks (jamais négatif). */
    protected int randomTicks(double mean, double deviation) {
        return (int) Math.max(0, Math.round(mean + random.nextGaussian() * deviation));
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
        cooldown = 0;
        pauseTicks = 0;
        blocksBeforePause = nextPauseIn();
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
        if (pauseTicks > 0) {
            // Petite pause, comme un joueur qui regarde son chat ou boit un coup.
            pauseTicks--;
            status = "pause";
            stopActing();
            releaseMovement();
            return;
        }
        status = targets.size() + " restants";

        Vec3d eyes = mc.player.getEyePos();
        BlockPos feet = mc.player.getBlockPos();
        // Si le joueur est dans le bloc cible (ex. : bloc à poser), il doit d'abord se décaler.
        boolean insideTarget = target.equals(feet) || target.equals(feet.up());
        if (!insideTarget && eyes.distanceTo(Vec3d.ofCenter(target)) <= reach) {
            releaseMovement();
            stuckTicks = 0;
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            if (!act(target, schematic.getBlockState(target))) {
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
            // Un peu de hasard pour ne pas suivre un ordre parfait (ne change pas de couche).
            double score = priority(pos, eyes) + random.nextDouble() * 3.0;
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
            // Un joueur n'utilise pas toujours toute sa portée.
            reach = REACH - random.nextDouble() * 0.8;
            // Temps de réaction avant de s'occuper du bloc suivant, parfois plus long.
            delay(random.nextDouble() < 0.06 ? 8 + random.nextInt(18) : randomTicks(3, 1.5));
            if (pauses.get() && --blocksBeforePause <= 0) {
                pauseTicks = 30 + random.nextInt(90);
                blocksBeforePause = nextPauseIn();
            }
            onNewTarget();
        }
        return best;
    }

    private void walkTowards(BlockPos target) {
        if (path == null) {
            BlockPos feet = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() + 0.2, mc.player.getZ());
            path = Pathfinder.find(mc.world, feet, target, pos -> canReachFrom(pos, target));
            pathIndex = 1;
            walkPitch = 5f + random.nextFloat() * 20f;
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
        BlockPos next = pathIndex + 1 < path.size() ? path.get(pathIndex + 1) : null;
        Vec3d player = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        double dx = node.getX() + 0.5 - player.x;
        double dz = node.getZ() + 0.5 - player.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        // Un joueur ne passe pas par le centre de chaque bloc : sur du plat, il coupe les virages.
        boolean flatNext = next != null && next.getY() == node.getY() && node.getY() == mc.player.getBlockY();
        if (horizontal < (flatNext ? 0.7 : 0.35) && Math.abs(player.y - node.getY()) < 1.2) {
            pathIndex++;
            stuckTicks = 0;
            return;
        }
        // Trop loin du chemin (chute, poussé...) : on recalcule.
        if (horizontal > 3 || Math.abs(player.y - node.getY()) > 4) {
            path = null;
            return;
        }

        // Le regard part un peu en avant sur le chemin, l'inclinaison bouge doucement.
        float moveYaw = (float) (MathHelper.atan2(dz, dx) * MathHelper.DEGREES_PER_RADIAN) - 90f;
        float lookYaw = moveYaw;
        if (next != null && horizontal < 1.5) {
            float nextYaw = (float) (MathHelper.atan2(next.getZ() + 0.5 - player.z, next.getX() + 0.5 - player.x)
                    * MathHelper.DEGREES_PER_RADIAN) - 90f;
            lookYaw = moveYaw + MathHelper.wrapDegrees(nextYaw - moveYaw) * (float) (1.0 - horizontal / 1.5);
        }
        walkPitch = MathHelper.clamp(walkPitch + (float) random.nextGaussian() * 0.4f, 0f, 30f);
        aim.turnTowards(lookYaw, walkPitch, aimSpeed() * 1.5f, 3f);

        // Touches comme au clavier : Z seul, Z+Q / Z+D en diagonale, ou pas de côté quand on est tout près.
        float offset = MathHelper.wrapDegrees(moveYaw - mc.player.getYaw());
        float abs = Math.abs(offset);
        boolean forward = abs < 67.5f;
        boolean side = abs > 22.5f && (abs < 67.5f || abs < 112.5f && horizontal < 1.5);
        mc.options.forwardKey.setPressed(forward);
        mc.options.leftKey.setPressed(side && offset < 0);
        mc.options.rightKey.setPressed(side && offset > 0);

        // Sprint en ligne droite quand il reste du chemin, on le relâche à l'approche.
        boolean longWay = path.size() - pathIndex > 5 && player.distanceTo(Vec3d.ofCenter(target)) > 8;
        mc.options.sprintKey.setPressed(forward && !side && longWay);

        // Saute seulement au pied de la marche, ou si on bute contre quelque chose.
        boolean climb = node.getY() > player.y + 0.5 && horizontal < 1.3;
        mc.options.jumpKey.setPressed(mc.player.isOnGround() && (climb || forward && mc.player.horizontalCollision));

        boolean moving = forward || side;
        if (moving && lastPos != null && lastPos.squaredDistanceTo(player) < 0.0025) {
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
    private boolean canReachFrom(BlockPos feet, BlockPos target) {
        if (feet.equals(target) || feet.up().equals(target)) return false;
        Vec3d eyes = new Vec3d(feet.getX() + 0.5, feet.getY() + 1.62, feet.getZ() + 0.5);
        return eyes.distanceTo(Vec3d.ofCenter(target)) <= reach - 0.3;
    }

    private int nextPauseIn() {
        return 25 + random.nextInt(70);
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
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        KeyBinding.updatePressedStates();
    }

    /**
     * Un joueur voit-il ce point de la face {@code side} du bloc {@code pos} depuis ses yeux,
     * sans autre bloc devant et à portée ?
     */
    protected boolean canSee(Vec3d point, BlockPos pos, Direction side) {
        BlockHitResult hit = raycastTo(point);
        return hit != null && hit.getBlockPos().equals(pos) && hit.getSide() == side;
    }

    /** Premier bloc touché en regardant vers {@code point} (ou {@code null} si rien ou hors de portée). */
    protected BlockHitResult raycastTo(Vec3d point) {
        Vec3d eyes = mc.player.getEyePos();
        if (eyes.distanceTo(point) > REACH) return null;
        // On vise un peu derrière le point pour être sûr de toucher la face.
        Vec3d end = point.add(point.subtract(eyes).normalize().multiply(0.05));
        BlockHitResult hit = mc.world.raycast(new RaycastContext(eyes, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }
}
