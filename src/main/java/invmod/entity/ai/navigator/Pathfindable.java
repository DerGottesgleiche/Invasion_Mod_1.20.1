package invmod.entity.ai.navigator;

/** Something the invader pathfinder can plan for: it says where it can go from a node and what that costs. */
public interface Pathfindable {
    float pathCost(PathNode from, PathNode to, TerrainView terrain);

    void addPathOptions(TerrainView terrain, PathNode node, Pathfinder pathfinder);
}
