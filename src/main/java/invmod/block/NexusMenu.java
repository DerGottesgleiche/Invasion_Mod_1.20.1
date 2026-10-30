package invmod.block;

import invmod.registry.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Two slots, as in the original: catalyst/agent input on the left, nexus output (flux, traps) on the right. */
public class NexusMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public NexusMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(2), new SimpleContainerData(10));
    }

    public NexusMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(invmod.registry.ModMenus.NEXUS.get(), containerId);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, 10);
        this.container = container;
        this.data = data;
        container.startOpen(inventory.player);
        this.addSlot(new Slot(container, 0, 32, 33));
        this.addSlot(new Slot(container, 1, 102, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 2) {
                if (!this.moveItemStackTo(stack, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return result;
    }

    public int getNexusLevel() {
        return this.data.get(0);
    }

    public int getKills() {
        return this.data.get(1);
    }

    public int getSpawnRadius() {
        return this.data.get(2);
    }

    public int getMode() {
        return this.data.get(3);
    }

    public int getCurrentWave() {
        return this.data.get(4);
    }

    public int getPowerLevel() {
        return this.data.get(5);
    }

    public int getActivationTimer() {
        return this.data.get(6);
    }

    public int getFlux() {
        return this.data.get(7);
    }

    public int getCookTime() {
        return this.data.get(8);
    }

    public int getHp() {
        return this.data.get(9);
    }

    public boolean isActivating() {
        int t = this.getActivationTimer();
        return t > 0 && t < NexusBlockEntity.TOTAL_ACTIVATION_TIME;
    }

    public boolean hasCatalystInput() {
        ItemStack input = this.slots.get(0).getItem();
        return input.is(ModItems.NEXUS_CATALYST.get()) || input.is(ModItems.STRONG_CATALYST.get());
    }
}
