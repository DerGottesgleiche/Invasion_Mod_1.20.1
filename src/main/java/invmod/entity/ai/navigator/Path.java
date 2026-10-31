package invmod.entity.ai.navigator;

import java.util.List;
import org.jspecify.annotations.Nullable;

public class Path {
    private final List<PathNode> nodes;
    private final PathNode target;
    private int index;

    public Path(List<PathNode> nodes, PathNode target) {
        this.nodes = nodes;
        this.target = target;
    }

    public int length() {
        return this.nodes.size();
    }

    public int index() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isFinished() {
        return this.index >= this.nodes.size();
    }

    public PathNode node(int i) {
        return this.nodes.get(i);
    }

    public @Nullable PathNode current() {
        return this.isFinished() ? null : this.nodes.get(this.index);
    }

    public PathNode last() {
        return this.nodes.get(this.nodes.size() - 1);
    }

    public PathNode target() {
        return this.target;
    }

    public float totalCost() {
        return this.last().totalCost;
    }

    /** How far the end of the path is from where it was meant to go. */
    public float distanceEndToTarget() {
        return this.last().distanceTo(this.target);
    }

    public List<PathNode> nodes() {
        return this.nodes;
    }
}
