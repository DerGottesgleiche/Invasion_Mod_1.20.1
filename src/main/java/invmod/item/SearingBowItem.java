package invmod.item;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A bow whose arrows burn and hit harder. */
public class SearingBowItem extends BowItem {
    public SearingBowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
        arrow.igniteForSeconds(100.0F);
        arrow.setBaseDamage(3.0);
        return arrow;
    }
}
