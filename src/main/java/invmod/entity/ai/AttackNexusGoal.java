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
        BlockPos nexus = this.invader.getNexusPos();
        return nexus != null && touchesNexus(this.mob, nexus);
    }

    /**
     * The original isNexusInRange: the nexus must touch the block box the mob occupies, on a side at any of its
     * heights, right above its head or right under its feet.
     */
    public static boolean touchesNexus(Mob mob, BlockPos nexus) {
        int sizeX = Math.max(1, (int) (mob.getBbWidth() + 1.0F));
        int sizeY = Math.max(1, (int) (mob.getBbHeight() + 1.0F));
        BlockPos feet = BlockPos.containing(mob.getX() - (sizeX - 1) * 0.5, mob.getY() + 0.5, mob.getZ() - (sizeX - 1) * 0.5);
        int dx = nexus.getX() - feet.getX();
        int dy = nexus.getY() - feet.getY();
        int dz = nexus.getZ() - feet.getZ();
        boolean insideX = dx >= 0 && dx < sizeX;
        boolean insideZ = dz >= 0 && dz < sizeX;
        boolean besideX = (dx == -1 || dx == sizeX) && insideZ;
        boolean besideZ = (dz == -1 || dz == sizeX) && insideX;
        if ((besideX || besideZ) && dy >= 0 && dy < sizeY) {
            return true;
        }
        return insideX && insideZ && (dy == -1 || dy == sizeY);
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
