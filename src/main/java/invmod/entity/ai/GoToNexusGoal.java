package invmod.entity.ai;

import invmod.block.NexusBlockEntity;
import invmod.entity.Invader;
import invmod.entity.ai.navigator.NavigatorIM;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Takes a nexus-bound invader to its nexus with the invader pathfinder (original EntityAIGoToNexus): it plans
 * a path that may dig through walls or (for engineers) build up them, re-plans when stuck, and backs off its
 * requests when planning keeps failing, falling back to walking straight at the nexus.
 */
public class GoToNexusGoal extends Goal {
    private final Mob mob;
    private final Invader invader;
    private final double speed;
    private BlockPos lastRequestPos = BlockPos.ZERO;
    private int requestTimer;
    private int failedCount;

    public GoToNexusGoal(Mob mob, double speed) {
        this.mob = mob;
        this.invader = (Invader) mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        BlockPos nexus = this.invader.getNexusPos();
        if (nexus == null) {
            return false;
        }
        if (this.mob.level().isLoaded(nexus) && !(this.mob.level().getBlockEntity(nexus) instanceof NexusBlockEntity)) {
            // The nexus is gone: from now on this is an ordinary hostile mob.
            this.invader.invaderData().setNexusPos(null);
            return false;
        }
        return this.mob.getTarget() == null && !AttackNexusGoal.touchesNexus(this.mob, nexus);
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
        this.requestTimer = 0;
        this.setPathToNexus();
    }

    @Override
    public void stop() {
        this.invader.navigator().clearPath();
    }

    @Override
    public void tick() {
        NavigatorIM nav = this.invader.navigator();
        BlockPos nexus = this.invader.getNexusPos();
        if (nexus == null) {
            return;
        }
        if (!nav.isWaitingForTask()) {
            Vec3 c = Vec3.atCenterOf(nexus);
            this.mob.getLookControl().setLookAt(c.x, c.y, c.z, 10.0F, this.mob.getMaxHeadXRot());
        }
        nav.tick();
        if (nav.noPath() && !nav.isWaitingForTask() && this.mob.onClimbable() && !this.mob.onGround()) {
            // No way on from this ladder: climb back down and make room (an engineer may need to build here).
            BlockPos column = this.mob.blockPosition();
            this.mob.setDeltaMovement((column.getX() + 0.5 - this.mob.getX()) * 0.3, -0.15,
                    (column.getZ() + 0.5 - this.mob.getZ()) * 0.3);
            return;
        }
        if (nav.noPath() && this.distanceToNexus() < 3.0) {
            // Last step: walk up against the nearest face of the nexus.
            Vec3 side = this.nearestSide(nexus);
            this.mob.getMoveControl().setWantedPosition(side.x, side.y, side.z, this.speed);
            return;
        }
        if (this.failedCount > 1 && nav.noPath() && this.mob.onGround()) {
            this.mob.getMoveControl().setWantedPosition(nexus.getX() + 0.5, nexus.getY(), nexus.getZ() + 0.5, this.speed);
        }
        if (!nav.isWaitingForTask() && (nav.noPath() || nav.stuckTime() > 40)) {
            this.setPathToNexus();
        }
    }

    private void setPathToNexus() {
        BlockPos nexus = this.invader.getNexusPos();
        if (nexus == null || this.requestTimer-- > 0) {
            return;
        }
        NavigatorIM nav = this.invader.navigator();
        boolean pathSet = nav.moveTo(nexus, 1.0F, this.speed);
        BlockPos here = this.mob.blockPosition();
        if (!pathSet || (nav.lastPathDistanceToTarget() > 3.0F && Math.sqrt(here.distSqr(this.lastRequestPos)) < 3.5)) {
            this.failedCount++;
            this.requestTimer = 40 * this.failedCount + this.mob.getRandom().nextInt(10);
        } else {
            this.failedCount = 0;
            this.requestTimer = 20;
        }
        this.lastRequestPos = here;
    }

    private Vec3 nearestSide(BlockPos nexus) {
        Vec3 c = Vec3.atCenterOf(nexus);
        double dx = this.mob.getX() - c.x;
        double dz = this.mob.getZ() - c.z;
        double reach = 0.5 + this.mob.getBbWidth() * 0.5 + 0.1;
        double y = Math.max(this.mob.getY(), nexus.getY());
        return Math.abs(dx) > Math.abs(dz) ? new Vec3(c.x + Math.signum(dx) * reach, y, c.z)
                : new Vec3(c.x, y, c.z + Math.signum(dz) * reach);
    }

    private double distanceToNexus() {
        BlockPos nexus = this.invader.getNexusPos();
        return nexus == null ? 0 : Math.sqrt(this.mob.distanceToSqr(Vec3.atCenterOf(nexus)));
    }
}
