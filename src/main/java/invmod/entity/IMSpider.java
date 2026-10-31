package invmod.entity;

import invmod.registry.ModEntities;
import invmod.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Spider invader. It climbs walls on its own. Tier 1 flavour 1 is a baby spider; tier 2 is either a
 * jumping spider (flavour 0) that pounces, or a mother spider (flavour 1) that hatches babies.
 */
public class IMSpider extends Spider implements Invader {
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(IMSpider.class, EntityDataSerializers.INT);
    private final InvaderData invaderData = new InvaderData(DATA_VARIANT);
    private int eggTimer = 200;

    public IMSpider(EntityType<? extends Spider> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes().add(Attributes.FOLLOW_RANGE, 24.0);
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
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F) {
            @Override
            public boolean canUse() {
                return IMSpider.this.isJumpingSpider() && super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true));
        InvaderGoals.addTargetGoals(this);
    }

    @Override
    public void setupInvader(int tier, int flavour, int texture) {
        this.invaderData.setVariant(this, tier, flavour, texture);
        if (tier == 2) {
            InvaderData.applyStats(this, flavour == 1 ? 23 : 18, flavour == 1 ? 3 : 5, flavour == 1 ? 0.27 : 0.34, 1.0);
        } else if (flavour == 1) {
            InvaderData.applyStats(this, 3, 1, 0.35, 0.45);
        } else {
            InvaderData.applyStats(this, 18, 3, 0.3, 1.0);
        }
    }

    public boolean isJumpingSpider() {
        return this.getTier() == 2 && this.getFlavour() == 0;
    }

    public boolean isMotherSpider() {
        return this.getTier() == 2 && this.getFlavour() == 1;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel level && this.isMotherSpider() && this.isAlive() && --this.eggTimer <= 0) {
            this.eggTimer = 400 + this.random.nextInt(200);
            this.hatchBabies(level);
        }
    }

    private void hatchBabies(ServerLevel level) {
        int count = 2 + this.random.nextInt(3);
        for (int i = 0; i < count; i++) {
            IMSpider baby = ModEntities.SPIDER.get().create(level, EntitySpawnReason.BREEDING);
            if (baby != null) {
                baby.invaderData.setNexusPos(this.getNexusPos());
                baby.setupInvader(1, 1, 0);
                baby.snapTo(this.getX(), this.getY(), this.getZ(), this.random.nextFloat() * 360.0F, 0.0F);
                baby.setPersistenceRequired();
                level.addFreshEntity(baby);
            }
        }
        this.playSound(ModSounds.EGG_HATCH.get(), 1.0F, 1.0F);
    }

    @Override
    public boolean canClimbWalls() {
        return true;
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
