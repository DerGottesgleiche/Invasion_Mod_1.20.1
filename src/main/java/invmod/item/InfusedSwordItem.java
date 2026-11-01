package invmod.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A diamond-strength sword whose bar is a charge, not durability: every hit charges it, and once full a
 * right click heals 6 hearts (or, sneaking, restores hunger) and empties it again.
 */
public class InfusedSwordItem extends Item {
    public InfusedSwordItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity mob, LivingEntity attacker) {
        if (stack.getDamageValue() > 0) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity mob, LivingEntity attacker) {
        // Never loses durability: the bar is the charge.
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() != 0) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            player.getFoodData().eat(6, 0.5F);
            player.playSound(SoundEvents.PLAYER_BURP, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        } else {
            player.heal(6.0F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0, player.getZ(), 6, 1.0, 0.5, 1.0, 0.0);
            }
        }
        stack.setDamageValue(stack.getMaxDamage());
        return InteractionResult.SUCCESS;
    }
}
