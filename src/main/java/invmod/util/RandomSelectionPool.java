package invmod.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Picks an entry at random, weighted. Entries may themselves be pools. */
public class RandomSelectionPool<T> implements ISelect<T> {
    private record Entry<T>(ISelect<T> selector, float weight) {
    }

    private final List<Entry<T>> pool = new ArrayList<>();
    private final Random rand = new Random();
    private float totalWeight;

    public void addEntry(T entry, float weight) {
        this.addEntry(new SingleSelection<>(entry), weight);
    }

    public void addEntry(ISelect<T> entry, float weight) {
        this.pool.add(new Entry<>(entry, weight));
        this.totalWeight += weight;
    }

    @Override
    public T selectNext() {
        float r = this.rand.nextFloat() * this.totalWeight;
        for (Entry<T> entry : this.pool) {
            if (r < entry.weight()) {
                return entry.selector().selectNext();
            }
            r -= entry.weight();
        }
        return this.pool.isEmpty() ? null : this.pool.get(0).selector().selectNext();
    }

    @Override
    public RandomSelectionPool<T> clone() {
        RandomSelectionPool<T> clone = new RandomSelectionPool<>();
        for (Entry<T> entry : this.pool) {
            clone.addEntry(entry.selector(), entry.weight());
        }
        return clone;
    }

    @Override
    public void reset() {
    }

    @Override
    public String toString() {
        return "RandomSelectionPool#Size=" + this.pool.size();
    }
}
