package invmod.entity;

import invmod.block.NexusBlockEntity;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** A tamed wolf given a strange bone: a tougher guardian that hunts invaders and stays near its nexus. */
public class IMWolf extends Wolf {
    private @Nullable BlockPos nexusPos;

    public IMWolf(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(6, new StayNearNexusGoal());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                (target, level) -> target instanceof Invader));
    }

    public void setNexusPos(@Nullable BlockPos pos) {
        this.nexusPos = pos;
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6.0);
        this.setHealth(this.getMaxHealth());
    }

    public @Nullable BlockPos getNexusPos() {
        return this.nexusPos;
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return target instanceof Invader || super.wantsToAttack(target, owner);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.nexusPos != null) {
            output.putLong("InvasionNexus", this.nexusPos.asLong());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.nexusPos = input.getLong("InvasionNexus").map(BlockPos::of).orElse(null);
    }

    private class StayNearNexusGoal extends Goal {
        StayNearNexusGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            IMWolf wolf = IMWolf.this;
            return wolf.nexusPos != null && !wolf.isOrderedToSit() && wolf.getTarget() == null
                    && wolf.level().getBlockEntity(wolf.nexusPos) instanceof NexusBlockEntity
                    && wolf.distanceToSqr(Vec3.atCenterOf(wolf.nexusPos)) > 12 * 12;
        }

        @Override
        public void start() {
            BlockPos pos = IMWolf.this.nexusPos;
            if (pos != null) {
                IMWolf.this.getNavigation().moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 1.2);
            }
        }
    }
}
