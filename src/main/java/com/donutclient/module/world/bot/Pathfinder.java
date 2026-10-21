package com.donutclient.module.world.bot;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Recherche de chemin A* sur la grille des blocs.
 *
 * <p>Chaque nœud est une position où le joueur peut se tenir debout (ses pieds). Depuis un nœud,
 * on peut marcher sur les 8 cases voisines, monter d'un bloc en sautant, ou descendre jusqu'à
 * {@value #MAX_FALL} blocs. A* explore en priorité les nœuds dont « coût déjà parcouru + distance
 * restante estimée » est le plus petit, ce qui donne le chemin le plus court sans tout explorer.
 */
public final class Pathfinder {
    private static final int MAX_NODES = 8000;
    private static final int MAX_FALL = 3;
    private static final double JUMP_COST = 1.5;
    private static final double FALL_COST = 0.5;
    private static final int[][] DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };
    private static final Set<Block> DANGEROUS = Set.of(
            Blocks.LAVA, Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.MAGMA_BLOCK, Blocks.CACTUS,
            Blocks.SWEET_BERRY_BUSH, Blocks.COBWEB, Blocks.POWDER_SNOW, Blocks.CAMPFIRE,
            Blocks.SOUL_CAMPFIRE, Blocks.WITHER_ROSE, Blocks.POINTED_DRIPSTONE);

    private final World world;

    private Pathfinder(World world) {
        this.world = world;
    }

    private static final class Node {
        final BlockPos pos;
        final double g;
        final double f;
        final Node parent;

        Node(BlockPos pos, double g, double h, Node parent) {
            this.pos = pos;
            this.g = g;
            this.f = g + h;
            this.parent = parent;
        }
    }

    /**
     * Cherche un chemin depuis {@code start} jusqu'à une position qui satisfait {@code isGoal}.
     * Si le but n'est pas atteint dans la limite de recherche, renvoie un chemin partiel vers la position
     * explorée la plus proche de {@code target}, ou {@code null} si aucun chemin ne fait progresser.
     * Le premier élément est la position de départ.
     */
    public static List<BlockPos> find(World world, BlockPos start, BlockPos target, Predicate<BlockPos> isGoal) {
        return new Pathfinder(world).search(start, target, isGoal);
    }

    private List<BlockPos> search(BlockPos start, BlockPos target, Predicate<BlockPos> isGoal) {
        PriorityQueue<Node> open = new PriorityQueue<>((a, b) -> Double.compare(a.f, b.f));
        Map<BlockPos, Double> bestCost = new HashMap<>();
        Node startNode = new Node(start, 0, heuristic(start, target), null);
        open.add(startNode);
        bestCost.put(start, 0.0);

        Node closest = startNode;
        double closestH = heuristic(start, target);
        int expanded = 0;

        while (!open.isEmpty() && expanded++ < MAX_NODES) {
            Node node = open.poll();
            if (node.g > bestCost.getOrDefault(node.pos, Double.MAX_VALUE)) continue; // doublon périmé
            if (isGoal.test(node.pos)) return toPath(node);

            double h = heuristic(node.pos, target);
            if (h < closestH) {
                closestH = h;
                closest = node;
            }

            for (Move move : neighbors(node.pos)) {
                double g = node.g + move.cost;
                if (g < bestCost.getOrDefault(move.pos, Double.MAX_VALUE)) {
                    bestCost.put(move.pos, g);
                    open.add(new Node(move.pos, g, heuristic(move.pos, target), node));
                }
            }
        }

        // Pas de chemin complet : on avance au moins vers le point le plus proche trouvé.
        if (closest != startNode && heuristic(start, target) - closestH >= 2) return toPath(closest);
        return null;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        return Math.sqrt(a.getSquaredDistance(b));
    }

    private static List<BlockPos> toPath(Node node) {
        List<BlockPos> path = new ArrayList<>();
        for (Node n = node; n != null; n = n.parent) path.add(n.pos);
        Collections.reverse(path);
        return path;
    }

    private record Move(BlockPos pos, double cost) {
    }

    private List<Move> neighbors(BlockPos pos) {
        List<Move> moves = new ArrayList<>(12);
        for (int[] d : DIRECTIONS) {
            boolean diagonal = d[0] != 0 && d[1] != 0;
            BlockPos next = pos.add(d[0], 0, d[1]);

            if (diagonal) {
                // Pas de coupe de coin : les deux cases adjacentes doivent être libres.
                if (!isOpen(pos.add(d[0], 0, 0)) || !isOpen(pos.add(0, 0, d[1]))) continue;
                if (canStand(next)) moves.add(new Move(next, Math.sqrt(2)));
                continue;
            }

            if (canStand(next)) {
                moves.add(new Move(next, 1));
                continue;
            }
            // Monter d'un bloc : il faut de la place au-dessus de la tête pour sauter.
            BlockPos up = next.up();
            if (canStand(up) && isPassable(pos.up(2))) {
                moves.add(new Move(up, 1 + JUMP_COST));
                continue;
            }
            // Descendre : on sort de la case puis on tombe jusqu'au premier sol.
            if (isOpen(next)) {
                for (int fall = 1; fall <= MAX_FALL; fall++) {
                    BlockPos below = next.down(fall);
                    if (canStand(below)) {
                        moves.add(new Move(below, 1 + fall * FALL_COST));
                        break;
                    }
                    if (!isPassable(below)) break;
                }
            }
        }
        return moves;
    }

    /** Le joueur peut-il se tenir debout avec les pieds à cette position ? */
    public boolean canStand(BlockPos feet) {
        return isOpen(feet) && isSolidGround(feet.down());
    }

    /** Place libre pour les pieds et la tête. */
    private boolean isOpen(BlockPos feet) {
        return isPassable(feet) && isPassable(feet.up());
    }

    private boolean isPassable(BlockPos pos) {
        if (!world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return false;
        BlockState state = world.getBlockState(pos);
        return state.getCollisionShape(world, pos).isEmpty()
                && state.getFluidState().isEmpty()
                && !DANGEROUS.contains(state.getBlock());
    }

    private boolean isSolidGround(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (DANGEROUS.contains(state.getBlock())) return false;
        VoxelShape shape = state.getCollisionShape(world, pos);
        // Les barrières et murets (1,5 bloc de haut) ne sont pas des sols praticables.
        return !shape.isEmpty() && shape.getMax(Direction.Axis.Y) <= 1.0;
    }
}
