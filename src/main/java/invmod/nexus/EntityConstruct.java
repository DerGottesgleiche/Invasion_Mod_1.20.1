package invmod.nexus;

/** Everything needed to build one invader: type, tier, texture and flavour (sub-variant). */
public record EntityConstruct(IMEntityType mobType, int tier, int texture, int flavour, int minAngle, int maxAngle) {
}
