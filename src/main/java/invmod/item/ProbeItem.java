package invmod.item;

import invmod.block.NexusBlockEntity;
import invmod.entity.Terrain;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Nexus adjuster (changes the spawn range) or material probe (shows how hard a block is for invaders). */
public class ProbeItem extends Item {
    private final boolean materialProbe;

    public ProbeItem(Item.Properties properties, boolean materialProbe) {
        super(properties);
        this.materialProbe = materialProbe;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (this.materialProbe) {
            if (!level.isClientSide()) {
                float strength = Terrain.blockStrength(level, context.getClickedPos());
                player.sendSystemMessage(Component.literal("Block strength: " + Math.round(strength * 100.0F) / 100.0F));
            }
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(context.getClickedPos()) instanceof NexusBlockEntity nexus) {
            if (!level.isClientSide()) {
                int newRange = nexus.getSpawnRadius() + (player.isShiftKeyDown() ? -8 : 8);
                if (newRange < 32) {
                    newRange = 128;
                }
                if (newRange > 128) {
                    newRange = 32;
                }
                if (nexus.setSpawnRadius(newRange)) {
                    player.sendSystemMessage(Component.literal("Nexus range changed to: " + nexus.getSpawnRadius()));
                } else {
                    player.sendSystemMessage(Component.literal("Can't change the range while the nexus is active"));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
