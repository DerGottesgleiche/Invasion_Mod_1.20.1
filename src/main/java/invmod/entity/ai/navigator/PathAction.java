package invmod.entity.ai.navigator;

import net.minecraft.core.Direction;

/**
 * What an invader has to do to enter a path node, as in the original mod. The four orientations of the
 * ladder actions name the side the wall is on (+X, -X, +Z, -Z).
 */
public enum PathAction {
    NONE, SWIM, DIG, BRIDGE, SCAFFOLD_UP,
    LADDER_UP_PX, LADDER_UP_NX, LADDER_UP_PZ, LADDER_UP_NZ,
    LADDER_TOWER_UP_PX, LADDER_TOWER_UP_NX, LADDER_TOWER_UP_PZ, LADDER_TOWER_UP_NZ;

    /** Horizontal directions in the original's index order: +X, -X, +Z, -Z. */
    public static final Direction[] SIDES = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
    public static final PathAction[] LADDER = {LADDER_UP_PX, LADDER_UP_NX, LADDER_UP_PZ, LADDER_UP_NZ};
    public static final PathAction[] LADDER_TOWER = {LADDER_TOWER_UP_PX, LADDER_TOWER_UP_NX, LADDER_TOWER_UP_PZ, LADDER_TOWER_UP_NZ};

    public boolean isLadder() {
        return this.ordinal() >= LADDER_UP_PX.ordinal() && this.ordinal() <= LADDER_UP_NZ.ordinal();
    }

    public boolean isLadderTower() {
        return this.ordinal() >= LADDER_TOWER_UP_PX.ordinal();
    }

    /** For ladder and ladder tower actions: the side the wall is on. */
    public Direction wallSide() {
        if (this.isLadder()) {
            return SIDES[this.ordinal() - LADDER_UP_PX.ordinal()];
        }
        if (this.isLadderTower()) {
            return SIDES[this.ordinal() - LADDER_TOWER_UP_PX.ordinal()];
        }
        return Direction.UP;
    }

    public static PathAction ladderAgainst(Direction wall) {
        for (int i = 0; i < SIDES.length; i++) {
            if (SIDES[i] == wall) {
                return LADDER[i];
            }
        }
        return NONE;
    }
}
