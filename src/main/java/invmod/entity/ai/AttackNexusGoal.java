package invmod.entity.ai;

import invmod.block.NexusBlockEntity;
import invmod.entity.Invader;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** Hits the nexus when standing next to it. */
public class AttackNexusGoal extends Goal {
    private final Mob mob;
    private final Invader invader;
    private int cooldown;

    public AttackNexusGoal(Mob mob) {
        this.mob = mob;
        this.invader = (Invader) mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.invader.isNexusBound() && this.inReach();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private boolean inReach() {
        BlockPos pos = this.invader.getNexusPos();
        if (pos == null) {
            return false;
        }
        double reach = 1.5 + this.mob.getBbWidth();
        Vec3 center = Vec3.atCenterOf(pos);
        double dx = Math.max(0, Math.abs(center.x - this.mob.getX()) - 0.5);
        double dz = Math.max(0, Math.abs(center.z - this.mob.getZ()) - 0.5);
        double dy = center.y - (this.mob.getY() + this.mob.getBbHeight() / 2.0);
        return dx * dx + dz * dz < reach * reach && Math.abs(dy) < 1.5 + this.mob.getBbHeight() / 2.0;
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
        this.cooldown = 10;
    }

    @Override
    public void tick() {
        BlockPos pos = this.invader.getNexusPos();
        if (pos == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (--this.cooldown <= 0) {
            this.cooldown = 20;
            this.mob.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
            NexusBlockEntity nexus = this.invader.getNexus();
            if (nexus != null) {
                nexus.attackNexus(this.invader.nexusDamage());
            }
        }
    }
}
