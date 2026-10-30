package invmod.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Draws entries without replacement until the pool is empty, then refills it. */
public class FiniteSelectionPool<T> implements ISelect<T> {
    private final List<ISelect<T>> selectors = new ArrayList<>();
    private final List<Integer> remaining = new ArrayList<>();
    private final List<Integer> original = new ArrayList<>();
    private final Random rand = new Random();
    private int totalAmount;
    private int originalAmount;

    public void addEntry(T entry, int amount) {
        this.addEntry(new SingleSelection<>(entry), amount);
    }

    public void addEntry(ISelect<T> entry, int amount) {
        this.selectors.add(entry);
        this.remaining.add(amount);
        this.original.add(amount);
        this.originalAmount = (this.totalAmount += amount);
    }

    @Override
    public T selectNext() {
        if (this.totalAmount < 1) {
            this.regeneratePool();
        }
        if (this.totalAmount < 1) {
            return null;
        }
        int r = this.rand.nextInt(this.totalAmount);
        for (int i = 0; i < this.selectors.size(); i++) {
            int amountLeft = this.remaining.get(i);
            if (r < amountLeft) {
                this.remaining.set(i, amountLeft - 1);
                this.totalAmount--;
                return this.selectors.get(i).selectNext();
            }
            r -= amountLeft;
        }
        return null;
    }

    @Override
    public FiniteSelectionPool<T> clone() {
        FiniteSelectionPool<T> clone = new FiniteSelectionPool<>();
        for (int i = 0; i < this.selectors.size(); i++) {
            clone.addEntry(this.selectors.get(i), this.original.get(i));
        }
        return clone;
    }

    @Override
    public void reset() {
        this.regeneratePool();
    }

    private void regeneratePool() {
        this.totalAmount = this.originalAmount;
        for (int i = 0; i < this.remaining.size(); i++) {
            this.remaining.set(i, this.original.get(i));
        }
    }
}
