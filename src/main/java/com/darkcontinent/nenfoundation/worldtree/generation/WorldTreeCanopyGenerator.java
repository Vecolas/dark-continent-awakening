package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeFoliageAnchor;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Voxelizes only formal foliage anchors and their connected transitions. */
public final class WorldTreeCanopyGenerator {
    private WorldTreeCanopyGenerator() {
    }

    public static void generate(ChunkAccess chunk, WorldTreeLayout layout) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

        for (WorldTreeFoliageAnchor anchor : layout.foliageAnchors()) {
            if (!isValidAnchor(anchor, layout)) {
                continue;
            }
            WorldTreePoint center = clusterCenter(anchor);
            if (!chunkIntersectsCluster(minX, maxX, minZ, maxZ, center, anchor.clusterSize())) {
                continue;
            }
            placeTransition(chunk, position, minX, minZ, maxX, maxZ, anchor, center, layout.seed());
            placeCluster(chunk, position, minX, minZ, maxX, maxZ, anchor, center, layout.seed());
        }
    }

    static boolean isValidAnchor(WorldTreeFoliageAnchor anchor, WorldTreeLayout layout) {
        return anchor.anchorBranchId() >= 0
                && anchor.anchorBranchId() < layout.branchNodes().size()
                && anchor.branchOrder() >= 1
                && anchor.supportRadius() >= WorldTreeFoliageAnchorGenerator.MIN_SUPPORT_RADIUS
                && anchor.clusterSize() <= Math.max(4.0, anchor.supportRadius() * 2.0);
    }

    static WorldTreePoint clusterCenter(WorldTreeFoliageAnchor anchor) {
        WorldTreePoint direction = anchor.anchorDirection();
        double size = anchor.clusterSize();
        // The anchor is the wrist: foliage grows beyond it as a terminal fist
        // instead of wrapping the whole arm in a continuous leaf shell.
        return new WorldTreePoint(
                anchor.anchorPosition().x() + direction.x() * size * 0.30,
                anchor.anchorPosition().y() + size * 0.18 + direction.y() * size * 0.16,
                anchor.anchorPosition().z() + direction.z() * size * 0.30);
    }

    static double clusterRadius(long seed, int anchorId) {
        long mixed = mix(seed, anchorId, 17);
        return 2.0 + Math.floorMod(mixed, 9);
    }

    static double clusterRadius(long seed, int branchId, int clusterId) {
        return clusterRadius(seed, branchId * 31 + clusterId);
    }

    /** Compatibility helper retained for geometry tests and old debug callers. */
    static WorldTreePoint foliageCenter(com.darkcontinent.nenfoundation.worldtree.WorldTreeSpline branch,
            WorldTreePoint branchPoint, double radius, long seed, int branchId, int clusterId) {
        WorldTreePoint direction = WorldTreeBranchNetwork.tangentAt(branch,
                0.5);
        return new WorldTreePoint(
                branchPoint.x() + direction.x() * radius * 0.34,
                branchPoint.y() + radius * 0.32 + direction.y() * radius * 0.18,
                branchPoint.z() + direction.z() * radius * 0.34);
    }

    static boolean chunkIntersectsCluster(int minX, int maxX, int minZ, int maxZ,
            WorldTreePoint center, double radius) {
        double envelope = radius * 1.85;
        return minX <= center.x() + envelope && maxX >= center.x() - envelope
                && minZ <= center.z() + envelope && maxZ >= center.z() - envelope;
    }

    private static void placeTransition(ChunkAccess chunk, BlockPos.MutableBlockPos position,
            int minX, int minZ, int maxX, int maxZ, WorldTreeFoliageAnchor anchor,
            WorldTreePoint center, long seed) {
        int steps = 2 + (int) Math.min(2.0, anchor.clusterSize() * 0.20);
        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            WorldTreePoint point = lerp(anchor.anchorPosition(), center, t);
            double radius = Math.max(1.0, anchor.supportRadius() * 0.28)
                    + anchor.clusterSize() * 0.14 * t;
            placeLeafEllipsoid(chunk, position, minX, minZ, maxX, maxZ,
                    point, radius, radius * 0.72, radius * 0.72, seed,
                    anchor.id(), 100 + step, 0.80);
        }
    }

    private static void placeCluster(ChunkAccess chunk, BlockPos.MutableBlockPos position,
            int minX, int minZ, int maxX, int maxZ, WorldTreeFoliageAnchor anchor,
            WorldTreePoint center, long seed) {
        double radius = anchor.clusterSize();
        placeLeafEllipsoid(chunk, position, minX, minZ, maxX, maxZ,
                center, radius * 1.12, radius * 0.72, radius * 0.78,
                seed, anchor.id(), 0, anchor.coverageProfile());
    }

    private static void placeLeafEllipsoid(ChunkAccess chunk, BlockPos.MutableBlockPos position,
            int minX, int minZ, int maxX, int maxZ, WorldTreePoint center,
            double lengthRadius, double crossRadius, double verticalRadius,
            long seed, int anchorId, int pass, double coverage) {
        int fromX = Math.max(minX, (int) Math.floor(center.x() - lengthRadius));
        int toX = Math.min(maxX, (int) Math.ceil(center.x() + lengthRadius) + 1);
        int fromZ = Math.max(minZ, (int) Math.floor(center.z() - lengthRadius));
        int toZ = Math.min(maxZ, (int) Math.ceil(center.z() + lengthRadius) + 1);
        int fromY = Math.max(chunk.getMinBuildHeight(),
                (int) Math.floor(center.y() - verticalRadius));
        int toY = Math.min(chunk.getMaxBuildHeight(),
                (int) Math.ceil(center.y() + verticalRadius) + 1);
        for (int x = fromX; x < toX; x++) {
            for (int z = fromZ; z < toZ; z++) {
                for (int y = fromY; y < toY; y++) {
                    double normalizedX = (x - center.x()) / lengthRadius;
                    double normalizedZ = (z - center.z()) / crossRadius;
                    double normalizedY = (y - center.y()) / verticalRadius;
                    double normalized = normalizedX * normalizedX
                            + normalizedZ * normalizedZ
                            + normalizedY * normalizedY;
                    if (normalized > 1.0
                            || y < center.y() - verticalRadius * 0.42
                            || cavity(seed, x, y, z, anchorId, pass, coverage)) {
                        continue;
                    }
                    position.set(x, y, z);
                    if (chunk.getBlockState(position).isAir()) {
                        chunk.setBlockState(position,
                                leafState(center.y(), seed, x, y, z, anchorId), false);
                    }
                }
            }
        }
    }

    private static WorldTreePoint lerp(WorldTreePoint first, WorldTreePoint second, double t) {
        return new WorldTreePoint(
                first.x() + (second.x() - first.x()) * t,
                first.y() + (second.y() - first.y()) * t,
                first.z() + (second.z() - first.z()) * t);
    }

    private static boolean cavity(long seed, int x, int y, int z,
            int anchorId, int pass, double coverage) {
        long value = seed ^ ((long) x * 341873128712L) ^ ((long) y * 132897987541L)
                ^ ((long) z * 42317861L) ^ ((long) anchorId * 31L + pass);
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        return Math.floorMod(value, 1000) >= (int) (coverage * 1000.0);
    }

    private static BlockState leafState(double y, long seed, int x, int blockY, int z,
            int anchorId) {
        long mixed = seed ^ ((long) x * 341873128712L) ^ ((long) blockY * 132897987541L)
                ^ ((long) z * 42317861L) ^ ((long) anchorId * 31L);
        int variant = Math.floorMod(mixed ^ (mixed >>> 29), 100);
        if (y >= 1340.0 || (y >= 820.0 && variant < 12)) {
            return WorldTreeBlocks.WORLD_TREE_LEAVES_PALE.get().defaultBlockState();
        }
        return y >= 820.0 || variant >= 72
                ? WorldTreeBlocks.WORLD_TREE_LEAVES_DENSE.get().defaultBlockState()
                : WorldTreeBlocks.WORLD_TREE_LEAVES.get().defaultBlockState();
    }

    private static long mix(long seed, int id, int child) {
        long value = seed ^ ((long) id * 0x9E3779B97F4A7C15L)
                ^ ((long) child * 0xBF58476D1CE4E5B9L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
