package invmod.entity.ai;

import invmod.entity.IMPigEngy;
import invmod.entity.Invader;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * When an invader has no way to the nexus it sticks with a nearby pigman engineer, stops digging, and its
 * help speeds up the engineer's building (original EntityAITargetOnNoNexusPath + EntityAIWaitForEngy).
 */
public class FollowEngineerGoal extends Goal {
    private static final double SEARCH_RANGE = 8.0;
    private static final double FOLLOW_DISTANCE = 4.0;
    private final Mob mob;
    private final Invader invader;
    private IMPigEngy engineer;
    private int timer;
    private int cooldown;

    public FollowEngineerGoal(Mob mob) {
        this.mob = mob;
        this.invader = (Invader) mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        if (!this.invader.isNexusBound() || this.mob.getTarget() != null
                || this.invader.terrainSkill() == Invader.TerrainSkill.BUILD_LADDERS
                || this.invader.navigator().lastPathDistanceToTarget() <= 4.0F) {
            return false;
        }
        List<IMPigEngy> engineers = this.mob.level().getEntitiesOfClass(IMPigEngy.class,
                this.mob.getBoundingBox().inflate(SEARCH_RANGE), e -> e.isAlive() && e.isNexusBound());
        if (engineers.isEmpty()) {
            return false;
        }
        this.engineer = engineers.get(0);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Once the engineer has climbed up there may be a way: go back to planning a path to the nexus.
        return this.engineer != null && this.engineer.isAlive() && this.mob.getTarget() == null && this.timer > 0
                && this.engineer.getY() - this.mob.getY() < 2.0;
    }

    @Override
    public void start() {
        this.timer = 200;
        this.invader.invaderData().setCanDig(false);
    }

    @Override
    public void stop() {
        this.engineer = null;
        this.cooldown = 60;
        this.invader.invaderData().setCanDig(true);
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.timer--;
        if (this.mob.distanceToSqr(this.engineer) > FOLLOW_DISTANCE * FOLLOW_DISTANCE) {
            if (this.mob.tickCount % 10 == 0) {
                this.mob.getNavigation().moveTo(this.engineer, 1.0);
            }
        } else {
            this.mob.getNavigation().stop();
            this.engineer.support(1.0F);
        }
        this.mob.getLookControl().setLookAt(this.engineer);
    }
}
