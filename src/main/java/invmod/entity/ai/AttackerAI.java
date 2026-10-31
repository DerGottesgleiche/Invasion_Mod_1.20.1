package invmod.entity.ai;

import invmod.block.NexusBlockEntity;
import invmod.entity.ai.navigator.EngineerPathing;
import invmod.entity.ai.navigator.Path;
import invmod.entity.ai.navigator.PathAction;
import invmod.entity.ai.navigator.PathNode;
import invmod.entity.ai.navigator.Pathfinder;
import invmod.entity.ai.navigator.ScaffoldPathing;
import invmod.entity.ai.navigator.TerrainView;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The nexus' coordination of its attackers (original AttackerAI): it keeps a map of how many invaders stand
 * where, so paths spread the horde out, and plans scaffolds when engineers cannot otherwise reach a nexus.
 */
public class AttackerAI {
    private static final int SCAFFOLD_SEARCH_NODES = 6000;
    private final NexusBlockEntity nexus;
    private final Long2IntOpenHashMap density = new Long2IntOpenHashMap();
    private final List<Scaffold> scaffolds = new ArrayList<>();
    private final Pathfinder pathfinder = new Pathfinder();
    private int scaffoldLimit = 2;
    private int minDistanceBetweenScaffolds = 9;
    private int nextScaffoldCalcTimer;
    private int updateScaffoldTimer;
    private int nextDensityUpdate;

    public AttackerAI(NexusBlockEntity nexus) {
        this.nexus = nexus;
    }

    public void tick(List<Mob> mobs) {
        this.nextScaffoldCalcTimer--;
        if (--this.updateScaffoldTimer <= 0) {
            this.updateScaffoldTimer = 40;
            this.updateScaffolds();
            this.scaffoldLimit = 2 + this.nexus.getCurrentWave() / 2;
            this.minDistanceBetweenScaffolds = 90 / (this.nexus.getCurrentWave() + 10);
        }
        if (--this.nextDensityUpdate <= 0) {
            this.nextDensityUpdate = 20;
            this.density.clear();
            for (Mob mob : mobs) {
                long key = mob.blockPosition().asLong();
                this.density.put(key, Math.min(7, this.density.get(key) + 1));
            }
        }
    }

    public int densityAt(int x, int y, int z) {
        return this.density.get(BlockPos.asLong(x, y, z));
    }

    public List<Scaffold> getScaffolds() {
        return this.scaffolds;
    }

    public int minDistanceBetweenScaffolds() {
        return this.minDistanceBetweenScaffolds;
    }

    public @Nullable Scaffold scaffoldAt(int x, int y, int z) {
        for (Scaffold scaffold : this.scaffolds) {
            if (scaffold.contains(x, y, z)) {
                return scaffold;
            }
        }
        return null;
    }

    /** An engineer that cannot reach the nexus asks for scaffolds; returns whether new ones were planned. */
    public boolean askGenerateScaffolds(Mob engineer) {
        if (this.nextScaffoldCalcTimer > 0 || this.scaffolds.size() > this.scaffoldLimit || this.nexus.getLevel() == null) {
            return false;
        }
        this.nextScaffoldCalcTimer = 200;
        Level level = this.nexus.getLevel();
        EngineerPathing base = new EngineerPathing(engineer);
        BlockPos target = this.nexus.getBlockPos();
        base.setGoal(target);
        ScaffoldPathing scaffoldPathing = new ScaffoldPathing(base, this);
        BlockPos start = engineer.blockPosition();
        float range = 12.0F + (float) Math.sqrt(start.distSqr(target));
        Path path = this.pathfinder.createPath(scaffoldPathing, new TerrainView(level, this), start, target, 1.1F, range,
                SCAFFOLD_SEARCH_NODES);
        if (path == null) {
            return false;
        }
        List<Scaffold> found = this.extractScaffolds(path, level);
        if (found.isEmpty()) {
            return false;
        }
        if (found.size() > 1) {
            // Keep only the scaffolds the engineers cannot do without, as the original did.
            List<Scaffold> needed = new ArrayList<>();
            for (Scaffold candidate : found) {
                Path without = this.pathfinder.createPath(base, new TerrainView(level, this, candidate.pos()), start, target,
                        1.1F, range, SCAFFOLD_SEARCH_NODES);
                if (without == null || without.distanceEndToTarget() > 1.5F) {
                    needed.add(candidate);
                }
            }
            found = needed.isEmpty() ? found.subList(0, 1) : needed;
        }
        this.scaffolds.addAll(found);
        return true;
    }

    private List<Scaffold> extractScaffolds(Path path, Level level) {
        List<Scaffold> list = new ArrayList<>();
        int startIndex = -1;
        for (int i = 0; i < path.length(); i++) {
            PathNode node = path.node(i);
            boolean up = node.action == PathAction.SCAFFOLD_UP;
            if (startIndex < 0 && up) {
                startIndex = i;
            } else if (startIndex >= 0 && !up) {
                PathNode base = path.node(startIndex);
                PathNode top = path.node(i - 1);
                Scaffold scaffold = new Scaffold(new BlockPos(base.x, base.y - 1, base.z), top.y - base.y + 2);
                this.orient(scaffold, level);
                scaffold.setInitialIntegrity(level);
                list.add(scaffold);
                startIndex = -1;
            }
        }
        return list;
    }

    /** Hang the scaffold on the side with the most existing blocks. */
    private void orient(Scaffold scaffold, Level level) {
        int most = -1;
        Direction best = Direction.EAST;
        for (Direction dir : PathAction.SIDES) {
            int count = 0;
            for (int h = 0; h < scaffold.targetHeight(); h++) {
                BlockPos p1 = scaffold.pos().above(h).relative(dir);
                BlockPos p2 = scaffold.pos().above(h).relative(dir, 2);
                if (level.getBlockState(p1).isCollisionShapeFullBlock(level, p1)) {
                    count++;
                }
                if (level.getBlockState(p2).isCollisionShapeFullBlock(level, p2)) {
                    count++;
                }
            }
            if (count > most) {
                most = count;
                best = dir;
            }
        }
        scaffold.setOrientation(best);
    }

    private void updateScaffolds() {
        Level level = this.nexus.getLevel();
        if (level == null) {
            return;
        }
        for (int i = this.scaffolds.size() - 1; i >= 0; i--) {
            Scaffold scaffold = this.scaffolds.get(i);
            scaffold.updateStatus(level);
            if (level instanceof ServerLevel server && scaffold.percentCompleted() < 1.0F) {
                BlockPos p = scaffold.pos();
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
            }
            // A scaffold that has been knocked down well below what was built is abandoned.
            if (scaffold.percentIntact() + 0.05F < 0.4F * scaffold.percentCompleted()) {
                this.scaffolds.remove(i);
            }
        }
    }

    public void clear() {
        this.scaffolds.clear();
        this.density.clear();
    }

    public void save(ValueOutput output) {
        ValueOutput.ValueOutputList list = output.childrenList("Scaffolds");
        for (Scaffold scaffold : this.scaffolds) {
            scaffold.save(list.addChild());
        }
    }

    public void load(ValueInput input) {
        this.scaffolds.clear();
        for (ValueInput child : input.childrenListOrEmpty("Scaffolds")) {
            this.scaffolds.add(Scaffold.load(child));
        }
    }
}
