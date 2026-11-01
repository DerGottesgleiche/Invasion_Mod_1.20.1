package invmod.entity;

import invmod.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;

/**
 * Invasion zombie. Tier 1: plain digger (flavour 0) or sword zombie (1). Tier 2: armoured digger (0),
 * dual-wielding marauder (1) or tar zombie that bursts into flames (2). Tier 3: the zombie brute.
 */
public class IMZombie extends Zombie implements Invader {
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(IMZombie.class, EntityDataSerializers.INT);
    protected final InvaderData invaderData = new InvaderData(DATA_VARIANT);

    public IMZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
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
    protected void addBehaviourGoals() {
    }

    @Override
    public void setupInvader(int tier, int flavour, int texture) {
        if (texture == 0) {
            texture = switch (tier) {
                case 2 -> flavour == 2 ? 5 : (this.random.nextBoolean() ? 2 : 4);
                case 3 -> 6;
                default -> this.random.nextBoolean() ? 0 : 1;
            };
        }
        this.invaderData.setVariant(this, tier, flavour, texture);
        this.applyTierStats(true);
    }

    protected void applyTierStats(boolean equip) {
        int tier = this.getTier();
        int flavour = this.getFlavour();
        double health = switch (tier) {
            case 2 -> 30;
            case 3 -> 65;
            default -> 20;
        };
        double damage;
        double scale = 1.0;
        double speed = 0.25;
        ItemStack mainHand = ItemStack.EMPTY;
        ItemStack offHand = ItemStack.EMPTY;
        ItemStack chest = ItemStack.EMPTY;
        if (tier == 1) {
            damage = flavour == 1 ? 6 : 4;
            if (flavour == 1) {
                mainHand = new ItemStack(Items.WOODEN_SWORD);
            }
        } else if (tier == 2) {
            scale = 1.12;
            switch (flavour) {
                case 1 -> {
                    damage = 10;
                    mainHand = new ItemStack(this.random.nextBoolean() ? Items.IRON_SWORD : Items.IRON_AXE);
                    offHand = new ItemStack(this.random.nextBoolean() ? Items.IRON_SWORD : Items.IRON_AXE);
                }
                case 2 -> damage = 5;
                default -> {
                    damage = 7;
                    chest = new ItemStack(Items.IRON_CHESTPLATE);
                }
            }
        } else {
            damage = 18;
            scale = 1.21;
            speed = 0.23;
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        }
        InvaderData.applyStats(this, health, damage, speed, scale);
        if (equip) {
            this.setItemSlot(EquipmentSlot.MAINHAND, mainHand);
            this.setItemSlot(EquipmentSlot.OFFHAND, offHand);
            this.setItemSlot(EquipmentSlot.CHEST, chest);
            this.setDropChance(EquipmentSlot.MAINHAND, 0.2F);
            this.setDropChance(EquipmentSlot.OFFHAND, 0.1F);
            this.setDropChance(EquipmentSlot.CHEST, 0.25F);
        }
    }

    @Override
    public TerrainSkill terrainSkill() {
        // Sword-carrying zombies and marauders don't dig, as in the original.
        return this.getFlavour() == 1 ? TerrainSkill.NONE : TerrainSkill.DIG;
    }

    @Override
    public float digSpeed() {
        return this.getTier() == 3 ? 2.0F : 1.0F;
    }

    @Override
    public int nexusDamage() {
        return this.getTier() == 3 ? 4 : 2;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.isTarZombie() && this.isOnFire() && this.isAlive()) {
            this.burstIntoFlames();
        }
    }

    protected boolean isTarZombie() {
        return this.getTier() == 2 && this.getFlavour() == 2;
    }

    /** Tar zombies catch fire and explode into a fireball, setting everything around them alight. */
    private void burstIntoFlames() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos center = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (level.getBlockState(pos).isAir() && Blocks.FIRE.defaultBlockState().canSurvive(level, pos)) {
                level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
            }
        }
        for (Entity entity : level.getEntities(this, new AABB(center).inflate(1.5))) {
            entity.igniteForSeconds(8.0F);
        }
        this.playSound(ModSounds.FIREBALL.get(), 1.5F, 1.0F);
        this.hurtServer(level, level.damageSources().inFire(), 500.0F);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && this.getTier() == 3) {
            // The brute's swing knocks targets flying.
            target.push(-Mth.sin(this.getYRot() * Mth.DEG_TO_RAD) * 1.5, 0.4, Mth.cos(this.getYRot() * Mth.DEG_TO_RAD) * 1.5);
            this.playSound(SoundEvents.GENERIC_BIG_FALL, 1.0F, 1.0F);
        }
        return hit;
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    public void setBaby(boolean baby) {
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public boolean canBreakDoors() {
        return false;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
    }

    @Override
    protected void handleAttributes(float difficultyModifier, net.minecraft.world.entity.EntitySpawnReason spawnReason) {
        this.applyTierStats(false);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return !this.isNexusBound() && super.removeWhenFarAway(distSqr);
    }

    @Override
    public boolean isPushable() {
        return this.getTier() != 3 && super.isPushable();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.getTier() == 3) {
            return this.random.nextInt(3) == 0 ? ModSounds.BIGZOMBIE.get() : null;
        }
        return SoundEvents.ZOMBIE_AMBIENT;
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
