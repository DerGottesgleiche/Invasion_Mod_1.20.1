package invmod.entity.ai.navigator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pigman engineer rules (original EntityIMPigEngy): on top of walking and digging, an engineer plans
 * ladders up walls, ladder towers where it must first put a block to hang the ladder on, bridges over gaps,
 * and climbs the scaffolds the nexus' attacker AI has planned.
 */
public class EngineerPathing extends MobPathing {
    public EngineerPathing(Mob mob) {
        super(mob);
    }

    /** A tower block can go here: it is already a full block, or empty space (not a ladder or other fixture). */
    protected boolean canBuildTowerSide(TerrainView terrain, BlockPos pos) {
        return this.isFullBlock(terrain, pos) || terrain.state(pos).canBeReplaced();
    }

    protected boolean isFullBlock(TerrainView terrain, BlockPos pos) {
        BlockState state = terrain.state(pos);
        return state.isCollisionShapeFullBlock(terrain.level(), pos);
    }

    @Override
    public void addPathOptions(TerrainView terrain, PathNode node, Pathfinder pathfinder) {
        if (node.action == PathAction.SCAFFOLD_UP) {
            // On a scaffold: keep climbing it or step off sideways, nothing else.
            if (terrain.isScaffold(node.x, node.y + 1, node.z) && this.collide(terrain, node.pos().above()) == CLEAR) {
                pathfinder.addOption(node.pos().above(), PathAction.SCAFFOLD_UP);
            }
            this.addWalkOptions(terrain, node, pathfinder);
            return;
        }
        super.addPathOptions(terrain, node, pathfinder);
        // Bridges: step out over a gap at the same height and put a block underneath.
        for (Direction dir : PathAction.SIDES) {
            BlockPos next = node.pos().relative(dir);
            if (this.collide(terrain, next) > BLOCKED && !this.canStandOn(terrain, next)) {
                for (int drop = 0; drop > -4; drop--) {
                    BlockPos below = next.above(drop - 1);
                    if (!terrain.state(below).canBeReplaced()) {
                        break;
                    }
                    pathfinder.addOption(next.above(drop), PathAction.BRIDGE);
                }
            }
        }
    }

    @Override
    protected void addVerticalOptions(TerrainView terrain, PathNode node, Pathfinder pathfinder) {
        super.addVerticalOptions(terrain, node, pathfinder);
        BlockPos pos = node.pos();
        BlockPos above = pos.above();
        if (this.collide(terrain, above) <= BLOCKED) {
            return;
        }
        if (terrain.state(above).canBeReplaced()) {
            if (node.action.isLadderTower()) {
                // A tower step continues as a plain ladder only where there is wall to hang it on (original continueLadder).
                Direction wall = node.action.wallSide();
                if (this.isFullBlock(terrain, above.relative(wall))) {
                    pathfinder.addOption(above, PathAction.ladderAgainst(wall));
                }
            } else {
                for (int i = 0; i < 4; i++) {
                    Direction dir = PathAction.SIDES[i];
                    if (node.action.isLadder() && node.action != PathAction.LADDER[i]) {
                        continue;
                    }
                    if (this.isFullBlock(terrain, above.relative(dir))) {
                        pathfinder.addOption(above, PathAction.LADDER[i]);
                    }
                }
            }
        }
        if (node.action == PathAction.NONE || node.action == PathAction.BRIDGE) {
            // Ladder tower: there is a wall further up, but nothing to hang a ladder on at this height.
            int maxHeight = 4;
            for (int i = this.sizeY; i < 4; i++) {
                if (!isPassable(terrain, pos.above(i), terrain.state(pos.above(i)))) {
                    maxHeight = i - this.sizeY;
                    break;
                }
            }
            for (int i = 0; i < 4; i++) {
                Direction dir = PathAction.SIDES[i];
                if (this.isFullBlock(terrain, above.relative(dir)) || !this.canBuildTowerSide(terrain, pos.relative(dir))
                        || !this.canBuildTowerSide(terrain, above.relative(dir))) {
                    continue;
                }
                for (int h = 1; h < maxHeight + 1; h++) {
                    BlockPos side = pos.relative(dir).above(h);
                    if (!terrain.state(side).isAir()) {
                        if (this.isFullBlock(terrain, side)) {
                            pathfinder.addOption(above, PathAction.LADDER_TOWER[i]);
                        }
                        break;
                    }
                }
            }
        }
        if (terrain.isScaffold(above.getX(), above.getY(), above.getZ())) {
            pathfinder.addOption(above, PathAction.SCAFFOLD_UP);
        }
    }

    @Override
    public float pathCost(PathNode from, PathNode to, TerrainView terrain) {
        float distance = from.distanceTo(to);
        BlockState state = terrain.state(to.x, to.y, to.z);
        float material = !state.isAir() && this.isDiggable(terrain, to.pos()) ? 3.2F : 1.0F;
        switch (to.action) {
            case BRIDGE:
                return distance * 1.7F * material;
            case SCAFFOLD_UP:
                return distance * 0.5F;
            case LADDER_UP_PX, LADDER_UP_NX, LADDER_UP_PZ, LADDER_UP_NZ:
                return state.getBlock() instanceof LadderBlock ? distance * 0.7F : distance * 1.3F * material;
            case LADDER_TOWER_UP_PX, LADDER_TOWER_UP_NX, LADDER_TOWER_UP_PZ, LADDER_TOWER_UP_NZ:
                return distance * 1.4F;
            default:
                return super.pathCost(from, to, terrain);
        }
    }
}
