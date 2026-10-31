package invmod.entity.terrain;

import invmod.Config;
import invmod.entity.Terrain;
import invmod.registry.ModBlocks;
import invmod.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Works through a list of block changes one at a time, each taking its cost in ticks, within reach of the
 * mob (the original TerrainModifier). Digging shows crack progress and plays the scrape sound.
 */
public class TerrainModifier {
    private final Mob mob;
    private final float reach;
    private final List<ModifyBlockEntry> entries = new ArrayList<>();
    private @Nullable TaskListener listener;
    private int index;
    private int timer;
    private int elapsed;
    private int scrapeTimer;
    private boolean outOfRange;
    private boolean failed;

    public TerrainModifier(Mob mob, float reach) {
        this.mob = mob;
        this.reach = reach;
    }

    public boolean isBusy() {
        return !this.entries.isEmpty();
    }

    public boolean isReadyFor(TaskListener asker) {
        return this.entries.isEmpty() || this.listener == asker;
    }

    public boolean requestTask(List<ModifyBlockEntry> task, TaskListener listener) {
        if (task.isEmpty() || !this.isReadyFor(listener)) {
            return false;
        }
        this.cancel();
        this.entries.addAll(task);
        this.listener = listener;
        this.index = 0;
        this.outOfRange = false;
        this.failed = false;
        this.startEntry();
        return true;
    }

    public void cancel() {
        this.clearCrack();
        this.entries.clear();
        this.index = 0;
        this.listener = null;
    }

    public void tick() {
        if (this.entries.isEmpty()) {
            return;
        }
        ModifyBlockEntry entry = this.entries.get(this.index);
        Level level = this.mob.level();
        if (this.isDone(entry)) {
            this.next();
            return;
        }
        this.elapsed++;
        if (this.mob.tickCount % 8 == 0) {
            this.mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
        }
        this.mob.getLookControl().setLookAt(Vec3.atCenterOf(entry.pos()));
        if (entry.newState() == null) {
            level.destroyBlockProgress(this.mob.getId(), entry.pos(), Math.min(9, 10 * this.elapsed / Math.max(1, this.timer)));
            if (--this.scrapeTimer <= 0) {
                this.scrapeTimer = 45 + this.mob.getRandom().nextInt(20);
                this.mob.playSound(ModSounds.SCRAPE.get(), 0.85F, 1.0F / (this.mob.getRandom().nextFloat() * 0.5F + 1.0F));
            }
        }
        if (this.elapsed >= this.timer) {
            this.apply(entry);
            this.next();
        }
    }

    private void startEntry() {
        while (this.index < this.entries.size() && this.isDone(this.entries.get(this.index))) {
            this.index++;
        }
        if (this.index >= this.entries.size()) {
            this.finish();
            return;
        }
        this.timer = Math.max(1, this.entries.get(this.index).cost());
        this.elapsed = 0;
    }

    private void next() {
        this.clearCrack();
        this.index++;
        this.startEntry();
    }

    private boolean isDone(ModifyBlockEntry entry) {
        BlockState current = this.mob.level().getBlockState(entry.pos());
        return entry.newState() == null ? Terrain.isPassable(this.mob.level(), entry.pos(), current) : current == entry.newState();
    }

    private void apply(ModifyBlockEntry entry) {
        Level level = this.mob.level();
        BlockPos pos = entry.pos();
        double dist = Math.sqrt(this.mob.distanceToSqr(Vec3.atCenterOf(pos)
                .subtract(0, this.mob.getBbHeight() / 2.0 - 0.5, 0)));
        if (dist > this.reach + this.mob.getBbWidth()) {
            this.outOfRange = true;
            return;
        }
        BlockState old = level.getBlockState(pos);
        if (old.is(ModBlocks.NEXUS.get())) {
            this.failed = true;
            return;
        }
        if (entry.newState() == null) {
            if (!Terrain.isDestructible(level, pos)) {
                this.failed = true;
                return;
            }
            level.destroyBlock(pos, Config.DROP_DESTRUCTED_BLOCKS.get(), this.mob);
        } else {
            if (!old.canBeReplaced() && !Terrain.isPassable(level, pos, old)) {
                if (!Terrain.isDestructible(level, pos)) {
                    this.failed = true;
                    return;
                }
                level.destroyBlock(pos, Config.DROP_DESTRUCTED_BLOCKS.get(), this.mob);
            }
            if (!entry.newState().canSurvive(level, pos) || !level.setBlockAndUpdate(pos, entry.newState())) {
                this.failed = true;
                return;
            }
            level.playSound(null, pos, entry.newState().getSoundType().getPlaceSound(), SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    private void finish() {
        TaskListener done = this.listener;
        int result = this.outOfRange ? TaskListener.OUT_OF_RANGE : this.failed ? TaskListener.FAILED : TaskListener.SUCCESS;
        this.entries.clear();
        this.index = 0;
        this.listener = null;
        if (done != null) {
            done.onTaskFinished(result);
        }
    }

    private void clearCrack() {
        if (!this.entries.isEmpty() && this.index < this.entries.size()) {
            this.mob.level().destroyBlockProgress(this.mob.getId(), this.entries.get(this.index).pos(), -1);
        }
    }

    /** For sounds of ladders placed by builders. */
    static void playLadder(Mob mob, BlockPos pos) {
        mob.level().playSound(null, pos, SoundEvents.LADDER_PLACE, SoundSource.HOSTILE, 1.0F, 1.0F);
    }
}
