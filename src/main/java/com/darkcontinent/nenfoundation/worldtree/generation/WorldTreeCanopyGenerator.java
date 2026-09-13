package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeSpline;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Gera clusters de folhas esparsos e determinísticos ao redor dos galhos. */
public final class WorldTreeCanopyGenerator {
    private static final int CLUSTERS_PER_BRANCH = 6;

    private WorldTreeCanopyGenerator() {
    }

    public static void generate(ChunkAccess chunk, WorldTreeLayout layout) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

        for (int branchId = 0; branchId < layout.branches().size(); branchId++) {
            WorldTreeSpline branch = layout.branches().get(branchId);
            for (int clusterId = 0; clusterId < CLUSTERS_PER_BRANCH; clusterId++) {
                double t = 0.55 + clusterId * 0.09;
                WorldTreePoint center = WorldTreeRootGenerator.bezier(branch, t);
                double radius = clusterRadius(layout.seed(), branchId, clusterId);
                if (!chunkIntersectsCluster(minX, maxX, minZ, maxZ, center, radius)) {
                    continue;
                }
                placeCluster(chunk, position, minX, minZ, maxX, maxZ,
                        branch, center, radius, layout.seed(), branchId, clusterId);
            }
        }
    }

    static double clusterRadius(long seed, int branchId, int clusterId) {
        long mixed = seed ^ ((long) branchId * 0x9E3779B97F4A7C15L)
                ^ ((long) clusterId * 0xBF58476D1CE4E5B9L);
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        return 6.0 + Math.floorMod(mixed, 13);
    }

    static boolean chunkIntersectsCluster(int minX, int maxX, int minZ, int maxZ,
            WorldTreePoint center, double radius) {
        double envelope = radius * 1.85;
        return minX <= center.x() + envelope && maxX >= center.x() - envelope
                && minZ <= center.z() + envelope && maxZ >= center.z() - envelope;
    }

    private static void placeCluster(ChunkAccess chunk, BlockPos.MutableBlockPos position,
            int minX, int minZ, int maxX, int maxZ, WorldTreeSpline branch,
            WorldTreePoint center, double radius,
            long seed, int branchId, int clusterId) {
        WorldTreePoint first = branch.controlPoints().get(0);
        WorldTreePoint last = branch.controlPoints().get(3);
        double angle = Math.atan2(last.z() - first.z(), last.x() - first.x());
        double lengthRadius = radius * 1.85;
        double crossRadius = radius * 0.78;
        int fromX = Math.max(minX, (int) Math.floor(center.x() - lengthRadius));
        int toX = Math.min(maxX, (int) Math.ceil(center.x() + lengthRadius) + 1);
        int fromZ = Math.max(minZ, (int) Math.floor(center.z() - lengthRadius));
        int toZ = Math.min(maxZ, (int) Math.ceil(center.z() + lengthRadius) + 1);
        double verticalRadius = radius * 0.82;
        int fromY = Math.max(chunk.getMinBuildHeight(), (int) Math.floor(center.y() - verticalRadius));
        int toY = Math.min(chunk.getMaxBuildHeight(), (int) Math.ceil(center.y() + verticalRadius) + 1);

        for (int x = fromX; x < toX; x++) {
            for (int z = fromZ; z < toZ; z++) {
                for (int y = fromY; y < toY; y++) {
                    double along = (x - center.x()) * Math.cos(angle)
                            + (z - center.z()) * Math.sin(angle);
                    double across = -(x - center.x()) * Math.sin(angle)
                            + (z - center.z()) * Math.cos(angle);
                    double normalized = Math.pow(along / lengthRadius, 2.0)
                            + Math.pow(across / crossRadius, 2.0)
                            + Math.pow((y - center.y()) / verticalRadius, 2.0);
                    if (normalized > 1.0 || cavity(seed, x, y, z, branchId, clusterId)) {
                        continue;
                    }
                    position.set(x, y, z);
                    if (chunk.getBlockState(position).isAir()) {
                        chunk.setBlockState(position, leafState(center.y()), false);
                    }
                }
            }
        }
    }

    private static boolean cavity(long seed, int x, int y, int z, int branchId, int clusterId) {
        long value = seed ^ ((long) x * 341873128712L) ^ ((long) y * 132897987541L)
                ^ ((long) z * 42317861L) ^ ((long) branchId * 31L + clusterId);
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        return Math.floorMod(value, 100) >= 38;
    }

    private static BlockState leafState(double y) {
        if (y >= 1340.0) {
            return WorldTreeBlocks.WORLD_TREE_LEAVES_PALE.get().defaultBlockState();
        }
        return y >= 820.0
                ? WorldTreeBlocks.WORLD_TREE_LEAVES_DENSE.get().defaultBlockState()
                : WorldTreeBlocks.WORLD_TREE_LEAVES.get().defaultBlockState();
    }
}
