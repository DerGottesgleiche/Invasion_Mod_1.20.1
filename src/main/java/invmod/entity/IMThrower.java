package invmod.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The thrower: a big, slow brute that hurls boulders (tier 1) or lit TNT (tier 2) at players and at the
 * walls between it and the nexus.
 */
public class IMThrower extends Monster implements Invader {
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(IMThrower.class, EntityDataSerializers.INT);
    private final InvaderData invaderData = new InvaderData(DATA_VARIANT);

    public IMThrower(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, InvaderData.pack(1, 0, 0));
    }

    @Override
    public InvaderData invaderData() {
        return this.invaderData;
    }

    @Override
    protected void registerGoals() {
        InvaderGoals.addMovementGoals(this, 1.0);
        this.goalSelector.addGoal(3, new ThrowGoal());
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
        InvaderGoals.addTargetGoals(this);
    }

    @Override
    public void setupInvader(int tier, int flavour, int texture) {
        this.invaderData.setVariant(this, tier, flavour, tier >= 2 ? 1 : 0);
        InvaderData.applyStats(this, tier >= 2 ? 70 : 50, tier >= 2 ? 8 : 6, 0, 0);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return !this.isNexusBound() && super.removeWhenFarAway(distSqr);
    }

    void throwAt(Vec3 target) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        Vec3 from = new Vec3(this.getX(), this.getEyeY() + 0.5, this.getZ());
        Vec3 delta = target.subtract(from);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (this.getTier() >= 2) {
            PrimedTnt tnt = new PrimedTnt(level, from.x, from.y, from.z, this);
            tnt.setFuse(50);
            double t = Math.max(10.0, horizontal / 0.6);
            tnt.setDeltaMovement(delta.x / t, delta.y / t + 0.04 * t / 2.0, delta.z / t);
            level.addFreshEntity(tnt);
            this.playSound(SoundEvents.TNT_PRIMED, 1.0F, 1.0F);
        } else {
            Boulder boulder = new Boulder(level, this);
            boulder.setPos(from);
            boulder.shoot(delta.x, delta.y + horizontal * 0.1, delta.z, 1.1F, 2.0F);
            level.addFreshEntity(boulder);
            this.playSound(SoundEvents.WITHER_SHOOT, 0.6F, 0.6F);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        this.invaderData.save(this, output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.invaderData.load(this, input);
    }

    /** Throws at the current target, or at the first wall between the thrower and its nexus. */
    private class ThrowGoal extends Goal {
        private int cooldown = 40;
        private @Nullable Vec3 aim;

        ThrowGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (--this.cooldown > 0) {
                return false;
            }
            this.aim = this.findAim();
            return this.aim != null;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            IMThrower thrower = IMThrower.this;
            this.cooldown = (thrower.getTier() >= 2 ? 120 : 70) + thrower.random.nextInt(40);
            if (this.aim != null) {
                thrower.getLookControl().setLookAt(this.aim);
                thrower.throwAt(this.aim);
            }
        }

        private @Nullable Vec3 findAim() {
            IMThrower thrower = IMThrower.this;
            LivingEntity target = thrower.getTarget();
            if (target != null && target.isAlive() && thrower.distanceToSqr(target) < 24 * 24 && thrower.distanceToSqr(target) > 9) {
                return target.position().add(0, target.getBbHeight() / 2.0, 0);
            }
            BlockPos nexus = thrower.getNexusPos();
            if (nexus == null || thrower.distanceToSqr(Vec3.atCenterOf(nexus)) > 40 * 40) {
                return null;
            }
            Vec3 from = thrower.getEyePosition();
            BlockHitResult hit = thrower.level().clip(new ClipContext(from, Vec3.atCenterOf(nexus), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, thrower));
            if (hit.getType() != HitResult.Type.BLOCK || hit.getBlockPos().equals(nexus)
                    || hit.getLocation().distanceToSqr(from) > 20 * 20) {
                return null;
            }
            return Terrain.isDestructible(thrower.level(), hit.getBlockPos()) ? Vec3.atCenterOf(hit.getBlockPos()) : null;
        }
    }
}
