package invmod.entity.ai.navigator;

import invmod.entity.ai.AttackerAI;
import invmod.entity.ai.Scaffold;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Used by the attacker AI to find out where scaffolds would help (original Scaffold as IPathfindable): an
 * engineer's moves plus "build a scaffold column here", which is cheap once started.
 */
public class ScaffoldPathing implements Pathfindable {
    private final MobPathing base;
    private final AttackerAI ai;

    public ScaffoldPathing(MobPathing base, AttackerAI ai) {
        this.base = base;
        this.ai = ai;
    }

    @Override
    public void addPathOptions(TerrainView terrain, PathNode node, Pathfinder pathfinder) {
        BlockPos above = node.pos().above();
        BlockState aboveState = terrain.state(above);
        if (node.action == PathAction.SCAFFOLD_UP) {
            // Keep climbing the scaffold while there is room above, or step off it sideways.
            if (this.base.collide(terrain, above) == MobPathing.CLEAR && !this.base.avoids(aboveState)) {
                pathfinder.addOption(above, PathAction.SCAFFOLD_UP);
            }
            this.base.addWalkOptions(terrain, node, pathfinder);
            return;
        }
        this.base.addPathOptions(terrain, node, pathfinder);
        for (Scaffold scaffold : this.ai.getScaffolds()) {
            if (Math.sqrt(scaffold.pos().distSqr(node.pos())) < this.ai.minDistanceBetweenScaffolds()) {
                return;
            }
        }
        BlockState ground = terrain.state(node.pos().below(2));
        if (aboveState.isAir() && !MobPathing.isPassable(terrain, node.pos().below(2), ground)) {
            for (int i = 1; i < 4; i++) {
                if (!terrain.state(node.pos().above(i)).isAir()) {
                    return;
                }
            }
            pathfinder.addOption(above, PathAction.SCAFFOLD_UP);
        }
    }

    @Override
    public float pathCost(PathNode from, PathNode to, TerrainView terrain) {
        BlockState state = terrain.state(to.x, to.y, to.z);
        float material = MobPathing.isPassable(terrain, to.pos(), state) ? 1.0F : 2.2F;
        if (to.action == PathAction.SCAFFOLD_UP) {
            if (from.action != PathAction.SCAFFOLD_UP) {
                material *= 3.4F;
            }
            return from.distanceTo(to) * 0.85F * material;
        }
        if (to.action == PathAction.BRIDGE) {
            return from.distanceTo(to) * 1.1F * (from.action == PathAction.SCAFFOLD_UP ? 0.0F : material);
        }
        if (to.action.isLadder()) {
            return from.distanceTo(to) * 1.5F * material;
        }
        return this.base.pathCost(from, to, terrain);
    }
}
