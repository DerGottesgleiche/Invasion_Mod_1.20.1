package invmod.item;

import invmod.block.NexusBlockEntity;
import invmod.entity.IMWolf;
import invmod.registry.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Feeding it to a tamed wolf near a nexus turns the wolf into a nexus guardian. */
public class StrangeBoneItem extends Item {
    public StrangeBoneItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Wolf wolf) || target instanceof IMWolf || !wolf.isTame()) {
            return InteractionResult.PASS;
        }
        if (!(target.level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        NexusBlockEntity nexus = NexusBlockEntity.findNearest(level, wolf.blockPosition(), 7);
        if (nexus == null) {
            player.sendSystemMessage(Component.literal("The wolf doesn't like this strange bone."));
            return InteractionResult.SUCCESS;
        }
        IMWolf guardian = wolf.convertTo(ModEntities.WOLF.get(), net.minecraft.world.entity.ConversionParams.single(wolf, true, true),
                newWolf -> newWolf.setNexusPos(nexus.getBlockPos()));
        if (guardian != null) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
