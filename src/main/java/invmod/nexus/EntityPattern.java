package invmod.nexus;

import invmod.util.RandomSelectionPool;

public class EntityPattern implements IEntityIMPattern {
    private final IMEntityType entityType;
    private final RandomSelectionPool<Integer> tierPool = new RandomSelectionPool<>();
    private final RandomSelectionPool<Integer> texturePool = new RandomSelectionPool<>();
    private final RandomSelectionPool<Integer> flavourPool = new RandomSelectionPool<>();

    public EntityPattern(IMEntityType entityType) {
        this.entityType = entityType;
    }

    @Override
    public EntityConstruct generateEntityConstruct() {
        return this.generateEntityConstruct(-180, 180);
    }

    @Override
    public EntityConstruct generateEntityConstruct(int minAngle, int maxAngle) {
        Integer tier = this.tierPool.selectNext();
        Integer texture = this.texturePool.selectNext();
        Integer flavour = this.flavourPool.selectNext();
        return new EntityConstruct(this.entityType, tier == null ? 1 : tier, texture == null ? 0 : texture,
                flavour == null ? 0 : flavour, minAngle, maxAngle);
    }

    public EntityPattern addTier(int tier, float weight) {
        this.tierPool.addEntry(tier, weight);
        return this;
    }

    public EntityPattern addTexture(int texture, float weight) {
        this.texturePool.addEntry(texture, weight);
        return this;
    }

    public EntityPattern addFlavour(int flavour, float weight) {
        this.flavourPool.addEntry(flavour, weight);
        return this;
    }

    @Override
    public String toString() {
        return "EntityPattern#" + this.entityType;
    }
}
