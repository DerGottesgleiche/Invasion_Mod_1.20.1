package invmod.nexus;

public interface IEntityIMPattern {
    EntityConstruct generateEntityConstruct();

    EntityConstruct generateEntityConstruct(int minAngle, int maxAngle);
}
