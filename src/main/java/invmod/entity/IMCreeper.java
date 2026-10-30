package invmod.entity;

import invmod.block.NexusBlockEntity;
import invmod.entity.ai.GoToNexusGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Creeper invader. Walks to the nexus and blows up next to it, or blows open any wall it gets stuck at.
 */
public class IMCreeper extends Creeper implements Invader {
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(IMCreeper.class, EntityDataSerializers.INT);
    private final InvaderData invaderData = new InvaderData(DATA_VARIANT);
    private int stuckTicks;
    private Vec3 lastPos = Vec3.ZERO;
    private boolean reportedDeath;

    public IMCreeper(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes().add(Attributes.FOLLOW_RANGE, 24.0);
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
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SwellGoal(this));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new GoToNexusGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        InvaderGoals.addTargetGoals(this);
    }

    @Override
    public void setupInvader(int tier, int flavour, int texture) {
        this.invaderData.setVariant(this, tier, flavour, texture);
        InvaderData.applyStats(this, 20, 0, 0.25, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.isRemoved() && !this.reportedDeath && this.getHealth() > 0.0F) {
            // Exploding discards the creeper without a death event, so report it to the nexus here.
            this.reportedDeath = true;
            NexusBlockEntity nexus = this.getNexus();
            if (nexus != null) {
                if (this.distanceToSqr(Vec3.atCenterOf(nexus.getBlockPos())) < 25.0) {
                    nexus.attackNexus(5);
                }
                nexus.registerMobDied();
            }
            return;
        }
        BlockPos nexusPos = this.getNexusPos();
        if (nexusPos == null || this.getTarget() != null) {
            return;
        }
        double dist = this.distanceToSqr(Vec3.atCenterOf(nexusPos));
        if (this.tickCount % 20 == 0) {
            this.stuckTicks = this.position().distanceToSqr(this.lastPos) < 0.25 ? this.stuckTicks + 20 : 0;
            this.lastPos = this.position();
        }
        if (dist < 9.0 || this.stuckTicks > 160) {
            this.ignite();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return !this.isNexusBound() && super.removeWhenFarAway(distSqr);
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
}
