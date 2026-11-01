package invmod.entity;

import invmod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Boulder thrown by throwers: hurts what it hits and smashes the blocks around the impact. */
public class Boulder extends ThrowableItemProjectile {
    /** Blocks stronger than this (by the invader strength formula) survive a boulder. */
    private static final float MAX_SMASH_STRENGTH = 8.0F;

    public Boulder(EntityType<? extends Boulder> type, Level level) {
        super(type, level);
    }

    public Boulder(Level level, LivingEntity thrower) {
        super(ModEntities.BOULDER.get(), thrower, level, new ItemStack(Items.COBBLESTONE));
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBBLESTONE;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        Entity entity = hitResult.getEntity();
        if (this.level() instanceof ServerLevel level && !(entity instanceof Invader)) {
            entity.hurtServer(level, this.damageSources().thrown(this, this.getOwner()), 10.0F);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos center = hitResult.getBlockPos();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if ((pos.equals(center) || this.random.nextFloat() < 0.4F) && Terrain.isDestructible(level, pos)
                    && Terrain.blockStrength(level, pos) <= MAX_SMASH_STRENGTH) {
                Terrain.removeBlock(level, pos.immutable());
            }
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()),
                this.getX(), this.getY(), this.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
        level.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.5F, 1.4F);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }
}
