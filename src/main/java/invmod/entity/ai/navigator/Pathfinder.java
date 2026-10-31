package invmod.entity.ai.navigator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * A* search over invader path nodes, ported from the original PathfinderIM. Nodes are keyed by position
 * and action, so "stand here" and "build a ladder here" are different nodes.
 */
public final class Pathfinder {
    private record Key(long pos, PathAction action) {
    }

    private final Map<Key, PathNode> nodes = new HashMap<>();
    private final NodeHeap open = new NodeHeap();
    private final List<PathNode> options = new ArrayList<>();
    private PathNode target;
    private float searchRange;

    /**
     * Returns the path to the target, or to the node closest to it when the target cannot be reached within
     * the node limit; null when no step at all is possible.
     */
    public @Nullable Path createPath(Pathfindable mob, TerrainView terrain, BlockPos start, BlockPos goal,
            float targetRadius, float maxSearchRange, int nodeLimit) {
        this.nodes.clear();
        this.open.clear();
        this.searchRange = maxSearchRange;
        PathNode startNode = this.openNode(start.getX(), start.getY(), start.getZ(), PathAction.NONE);
        this.target = this.openNode(goal.getX(), goal.getY(), goal.getZ(), PathAction.NONE);
        startNode.totalCost = 0.0F;
        startNode.estimate = estimate(startNode, this.target);
        startNode.score = startNode.estimate;
        this.open.add(startNode);
        PathNode closest = startNode;
        float closestDistance = startNode.distanceTo(this.target);

        while (!this.open.isEmpty()) {
            if (this.nodes.size() > nodeLimit) {
                break;
            }
            PathNode node = this.open.poll();
            node.closed = true;
            float distance = node.distanceTo(this.target);
            if (node.samePos(this.target) || distance < targetRadius + 0.1F) {
                closest = node;
                break;
            }
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = node;
            }
            this.options.clear();
            mob.addPathOptions(terrain, node, this);
            for (PathNode next : this.options) {
                float cost = node.totalCost + mob.pathCost(node, next, terrain);
                if (!next.inHeap() || cost < next.totalCost) {
                    next.previous = node;
                    next.totalCost = cost;
                    next.estimate = estimate(next, this.target);
                    if (next.inHeap()) {
                        this.open.changeScore(next, cost + next.estimate);
                    } else {
                        next.score = cost + next.estimate;
                        this.open.add(next);
                    }
                }
            }
        }
        this.open.clear();
        if (closest == startNode) {
            return null;
        }
        List<PathNode> list = new ArrayList<>();
        for (PathNode n = closest; n != null; n = n.previous) {
            list.add(n);
        }
        Collections.reverse(list);
        return new Path(list, this.target);
    }

    public int nodesOpened() {
        return this.nodes.size();
    }

    /** Called by {@link Pathfindable#addPathOptions} for every node reachable from the one being examined. */
    public void addOption(int x, int y, int z, PathAction action) {
        PathNode node = this.openNode(x, y, z, action);
        if (!node.closed && node.distanceTo(this.target) < this.searchRange) {
            this.options.add(node);
        }
    }

    public void addOption(BlockPos pos, PathAction action) {
        this.addOption(pos.getX(), pos.getY(), pos.getZ(), action);
    }

    private PathNode openNode(int x, int y, int z, PathAction action) {
        return this.nodes.computeIfAbsent(new Key(BlockPos.asLong(x, y, z), action), k -> new PathNode(x, y, z, action));
    }

    private static float estimate(PathNode from, PathNode to) {
        return Math.abs(to.x - from.x) + Math.abs(to.y - from.y) + Math.abs(to.z - from.z) * 1.01F;
    }
}
