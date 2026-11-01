package invmod.entity;

import invmod.registry.ModItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Pigman engineer: does not dig, but builds ladders up walls so the horde can follow it over them. */
public class IMPigEngy extends Monster implements Invader {
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(IMPigEngy.class, EntityDataSerializers.INT);
    private final InvaderData invaderData = new InvaderData(DATA_VARIANT);
    private float supportThisTick;
    private int askForScaffoldTimer = 60;

    public IMPigEngy(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.FOLLOW_RANGE, 24.0);
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
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        InvaderGoals.addTargetGoals(this);
    }

    @Override
    public void setupInvader(int tier, int flavour, int texture) {
        this.invaderData.setVariant(this, tier, flavour, tier >= 2 ? 1 : 0);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ENGY_HAMMER.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    public TerrainSkill terrainSkill() {
        return TerrainSkill.BUILD_LADDERS;
    }

    /** Tier 2 engineers build with cobblestone, tier 1 with planks (original TerrainBuilder setup). */
    @Override
    public net.minecraft.world.level.block.state.BlockState buildMaterial() {
        return this.getTier() >= 2 ? net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState()
                : net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState();
    }

    @Override
    public float buildMaterialCost() {
        return this.getTier() >= 2 ? invmod.entity.terrain.TerrainBuilder.COBBLE_COST : invmod.entity.terrain.TerrainBuilder.PLANKS_COST;
    }

    @Override
    public float digSpeed() {
        return this.getTier() <= 1 ? 1.0F : this.getTier() * 0.75F;
    }

    /** Invaders waiting near the engineer help it build faster. */
    public void support(float amount) {
        this.supportThisTick += amount;
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel level) {
        super.customServerAiStep(level);
        invmod.entity.terrain.TerrainBuilder builder = this.navigator().builder();
        if (builder != null) {
            builder.setBuildRate(1.0F + this.supportThisTick * 0.33F);
        }
        this.supportThisTick = 0.0F;
        // Ask the nexus for scaffolds when there is no way up to it (original updateAITick).
        invmod.block.NexusBlockEntity nexus = this.getNexus();
        if (nexus != null && --this.askForScaffoldTimer <= 0) {
            int weight = 1;
            int dy = nexus.getBlockPos().getY() - this.blockPosition().getY();
            if (dy > 1) {
                weight = Math.max(6000 / Math.max(1, nexus.getBlockPos().getY()) - this.blockPosition().getY(), 1);
            }
            if (this.navigator().lastPathDistanceToTarget() > 2.0F || this.random.nextInt(weight) == 0) {
                if (nexus.getAttackerAI().askGenerateScaffolds(this)) {
                    this.navigator().clearPath();
                    this.askForScaffoldTimer = 60;
                } else {
                    this.askForScaffoldTimer = 140;
                }
            } else {
                this.askForScaffoldTimer = 20;
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return !this.isNexusBound() && super.removeWhenFarAway(distSqr);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIFIED_PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIFIED_PIGLIN_DEATH;
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
