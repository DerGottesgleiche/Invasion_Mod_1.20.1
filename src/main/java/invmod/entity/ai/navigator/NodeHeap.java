package invmod.entity.ai.navigator;

import java.util.Arrays;

/** Binary min-heap of path nodes ordered by score, as the original NodeContainer. */
final class NodeHeap {
    private PathNode[] nodes = new PathNode[1024];
    private int count;

    void clear() {
        for (int i = 0; i < this.count; i++) {
            this.nodes[i].heapIndex = -1;
        }
        Arrays.fill(this.nodes, 0, this.count, null);
        this.count = 0;
    }

    boolean isEmpty() {
        return this.count == 0;
    }

    void add(PathNode node) {
        if (this.count == this.nodes.length) {
            this.nodes = Arrays.copyOf(this.nodes, this.count << 1);
        }
        this.nodes[this.count] = node;
        node.heapIndex = this.count;
        this.up(this.count++);
    }

    PathNode poll() {
        PathNode top = this.nodes[0];
        this.nodes[0] = this.nodes[--this.count];
        this.nodes[this.count] = null;
        if (this.count > 0) {
            this.nodes[0].heapIndex = 0;
            this.down(0);
        }
        top.heapIndex = -1;
        return top;
    }

    void changeScore(PathNode node, float score) {
        float old = node.score;
        node.score = score;
        if (score < old) {
            this.up(node.heapIndex);
        } else {
            this.down(node.heapIndex);
        }
    }

    private void up(int i) {
        PathNode node = this.nodes[i];
        while (i > 0) {
            int parent = (i - 1) >> 1;
            PathNode p = this.nodes[parent];
            if (node.score >= p.score) {
                break;
            }
            this.nodes[i] = p;
            p.heapIndex = i;
            i = parent;
        }
        this.nodes[i] = node;
        node.heapIndex = i;
    }

    private void down(int i) {
        PathNode node = this.nodes[i];
        while (true) {
            int left = (i << 1) + 1;
            if (left >= this.count) {
                break;
            }
            int right = left + 1;
            int child = right < this.count && this.nodes[right].score < this.nodes[left].score ? right : left;
            if (this.nodes[child].score >= node.score) {
                break;
            }
            this.nodes[i] = this.nodes[child];
            this.nodes[i].heapIndex = i;
            i = child;
        }
        this.nodes[i] = node;
        node.heapIndex = i;
    }
}
