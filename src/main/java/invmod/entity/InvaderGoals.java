package invmod.entity;

import invmod.Config;
import invmod.entity.ai.AttackNexusGoal;
import invmod.entity.ai.GoToNexusGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.player.Player;

/** The goal set shared by the invaders. Attack goals are added by each mob. */
public final class InvaderGoals {
    private InvaderGoals() {
    }

    public static void addMovementGoals(PathfinderMob mob, double speed) {
        mob.goalSelector.addGoal(0, new FloatGoal(mob));
        mob.goalSelector.addGoal(2, new AttackNexusGoal(mob));
        mob.goalSelector.addGoal(4, new invmod.entity.ai.FollowEngineerGoal(mob));
        mob.goalSelector.addGoal(5, new GoToNexusGoal(mob, speed));
        mob.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(mob, 0.8) {
            @Override
            public boolean canUse() {
                return !((Invader) mob).isNexusBound() && super.canUse();
            }
        });
        mob.goalSelector.addGoal(8, new LookAtPlayerGoal(mob, Player.class, 8.0F));
        mob.goalSelector.addGoal(8, new RandomLookAroundGoal(mob));
    }

    public static void addTargetGoals(Mob mob) {
        mob.targetSelector.addGoal(1, new HurtByTargetGoal((PathfinderMob) mob, Invader.class));
        // Players they can see within sight range, and players they "sense" without line of sight.
        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Player.class, true));
        mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob, Player.class, 10, false, false,
                (target, level) -> target.distanceTo(mob) < Config.MOB_SENSE_RANGE.get()));
        mob.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(mob, AbstractGolem.class, true));
        mob.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(mob, LivingEntity.class, 10, true, false,
                (target, level) -> target instanceof IMWolf));
    }
}
