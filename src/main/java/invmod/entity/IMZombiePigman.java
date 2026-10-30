package invmod.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Zombie pigman invader: fire immune, golden sword, tiers 1-3. Digs its way to the nexus. */
public class IMZombiePigman extends IMZombie {
    public IMZombiePigman(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    public void setupInvader(int tier, int flavour, int texture) {
        this.invaderData.setVariant(this, tier, 0, tier == 3 ? 2 : 0);
        this.applyTierStats(true);
    }

    @Override
    protected void applyTierStats(boolean equip) {
        int tier = this.getTier();
        double health = tier == 3 ? 65 : tier == 2 ? 30 : 20;
        double damage = tier == 3 ? 15 : tier == 2 ? 10 : 8;
        double scale = tier == 3 ? 1.21 : tier == 2 ? 1.12 : 1.0;
        InvaderData.applyStats(this, health, damage, 0.27, scale);
        if (tier == 3) {
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.8);
        }
        if (equip) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(tier == 1 ? Items.GOLDEN_SWORD : Items.GOLDEN_AXE));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.1F);
        }
    }

    @Override
    public TerrainSkill terrainSkill() {
        return TerrainSkill.DIG;
    }

    @Override
    protected boolean isTarZombie() {
        return false;
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
}
