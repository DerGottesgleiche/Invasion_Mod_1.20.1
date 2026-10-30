package invmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Per-invader state shared by all invader classes. Tier, flavour and texture are packed into one synced
 * int (each class defines its own accessor) so the client renderer can pick textures and sizes.
 */
public class InvaderData {
    private final EntityDataAccessor<Integer> variant;
    private @Nullable BlockPos nexusPos;

    public InvaderData(EntityDataAccessor<Integer> variant) {
        this.variant = variant;
    }

    public static int pack(int tier, int flavour, int texture) {
        return (tier & 0xF) | (flavour & 0xF) << 4 | (texture & 0xFF) << 8;
    }

    public EntityDataAccessor<Integer> variantAccessor() {
        return this.variant;
    }

    public void setVariant(Mob mob, int tier, int flavour, int texture) {
        mob.getEntityData().set(this.variant, pack(tier, flavour, texture));
    }

    public int getTier(Mob mob) {
        return Math.max(1, mob.getEntityData().get(this.variant) & 0xF);
    }

    public int getFlavour(Mob mob) {
        return mob.getEntityData().get(this.variant) >> 4 & 0xF;
    }

    public int getTexture(Mob mob) {
        return mob.getEntityData().get(this.variant) >> 8 & 0xFF;
    }

    public @Nullable BlockPos getNexusPos() {
        return this.nexusPos;
    }

    public void setNexusPos(@Nullable BlockPos nexusPos) {
        this.nexusPos = nexusPos;
    }

    public void save(Mob mob, ValueOutput output) {
        output.putInt("InvasionVariant", mob.getEntityData().get(this.variant));
        if (this.nexusPos != null) {
            output.putLong("InvasionNexus", this.nexusPos.asLong());
        }
    }

    public void load(Mob mob, ValueInput input) {
        mob.getEntityData().set(this.variant, input.getIntOr("InvasionVariant", pack(1, 0, 0)));
        this.nexusPos = input.getLong("InvasionNexus").map(BlockPos::of).orElse(null);
    }

    /** Sets max health (and heals to it), attack damage, speed and size. Values of 0 leave the attribute alone. */
    public static void applyStats(Mob mob, double health, double damage, double speed, double scale) {
        set(mob, mob.getAttribute(Attributes.MAX_HEALTH), health);
        set(mob, mob.getAttribute(Attributes.ATTACK_DAMAGE), damage);
        set(mob, mob.getAttribute(Attributes.MOVEMENT_SPEED), speed);
        set(mob, mob.getAttribute(Attributes.SCALE), scale);
        if (health > 0) {
            mob.setHealth(mob.getMaxHealth());
        }
    }

    private static void set(Mob mob, @Nullable AttributeInstance attribute, double value) {
        if (attribute != null && value > 0) {
            attribute.setBaseValue(value);
        }
    }
}
