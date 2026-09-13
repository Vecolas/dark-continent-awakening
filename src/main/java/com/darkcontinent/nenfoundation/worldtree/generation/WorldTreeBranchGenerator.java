package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeSpline;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeTrunkProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Gera galhos major por splines cubicas, somente no chunk solicitado. */
public final class WorldTreeBranchGenerator {
    private static final int SAMPLE_COUNT = 96;
    private static final double MAX_BRANCH_REACH = 235.0;

    private WorldTreeBranchGenerator() {
    }

    public static void generate(ChunkAccess chunk, WorldTreeLayout layout) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        if (!chunkIntersectsBranches(minX, maxX, minZ, maxZ)) {
            return;
        }

        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        for (WorldTreeSpline spline : layout.branches()) {
            if (!chunkIntersectsSpline(minX, maxX, minZ, maxZ, spline)) {
                continue;
            }
            generateAttachment(chunk, position, minX, minZ, maxX, maxZ, spline, layout);
            for (int sample = 0; sample <= SAMPLE_COUNT; sample++) {
                double t = (double) sample / SAMPLE_COUNT;
                WorldTreePoint point = bezier(spline, t);
                double radius = radiusAt(spline, t);
                int fromX = Math.max(minX, (int) Math.floor(point.x() - radius - 1.0));
                int toX = Math.min(maxX, (int) Math.ceil(point.x() + radius + 1.0));
                int fromZ = Math.max(minZ, (int) Math.floor(point.z() - radius - 1.0));
                int toZ = Math.min(maxZ, (int) Math.ceil(point.z() + radius + 1.0));
                int verticalRadius = (int) Math.ceil(radius * 0.72) + 1;
                for (int x = fromX; x < toX; x++) {
                    for (int z = fromZ; z < toZ; z++) {
                        double horizontal = Math.hypot(x - point.x(), z - point.z());
                        if (horizontal > radius + 1.0) {
                            continue;
                        }
                        int bottom = Math.max(chunk.getMinBuildHeight(),
                                (int) Math.floor(point.y()) - verticalRadius);
                        int top = Math.min(chunk.getMaxBuildHeight(),
                                (int) Math.ceil(point.y()) + verticalRadius + 1);
                        for (int y = bottom; y < top; y++) {
                            double irregular = 1.0 + 0.08 * Math.sin(x * 0.37 + z * 0.19 + y * 0.11);
                            double normalized = Math.pow(horizontal / (radius * irregular), 2.0)
                                    + Math.pow((y - point.y()) / (radius * 0.72 * irregular), 2.0);
                            if (normalized <= 1.0) {
                                position.set(x, y, z);
                                chunk.setBlockState(position, branchState(normalized), false);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Liga cada galho ao eixo vivo da árvore. Galhos da Crown/Summit podem
     * nascer acima do fim do tronco principal; nesse caso o volume funciona
     * como um contraforte vertical, em vez de deixar uma ilha suspensa.
     */
    private static void generateAttachment(ChunkAccess chunk, BlockPos.MutableBlockPos position,
            int minX, int minZ, int maxX, int maxZ, WorldTreeSpline spline,
            WorldTreeLayout layout) {
        WorldTreePoint branchStart = spline.controlPoints().get(0);
        WorldTreeTrunkProfile trunk = layout.trunk();
        double anchorY = Math.max(trunk.baseY(), Math.min(trunk.topY() - 1.0, branchStart.y()));
        WorldTreePoint anchor = new WorldTreePoint(0.0, anchorY, 0.0);
        double distance = Math.sqrt(Math.pow(anchor.x() - branchStart.x(), 2.0)
                + Math.pow(anchor.y() - branchStart.y(), 2.0)
                + Math.pow(anchor.z() - branchStart.z(), 2.0));
        int samples = Math.max(8, (int) Math.ceil(distance / 4.0));
        double anchorRadius = Math.min(trunk.radiusAt((int) anchorY) * 0.82,
                spline.startRadius() * 1.35);
        for (int sample = 0; sample <= samples; sample++) {
            double t = (double) sample / samples;
            WorldTreePoint point = lerp(anchor, branchStart, t);
            double radius = anchorRadius + (spline.startRadius() - anchorRadius) * t;
            placeEllipsoid(chunk, position, minX, minZ, maxX, maxZ, point, radius, layout);
        }
    }

    private static WorldTreePoint lerp(WorldTreePoint first, WorldTreePoint second, double t) {
        return new WorldTreePoint(
                first.x() + (second.x() - first.x()) * t,
                first.y() + (second.y() - first.y()) * t,
                first.z() + (second.z() - first.z()) * t);
    }

    private static void placeEllipsoid(ChunkAccess chunk, BlockPos.MutableBlockPos position,
            int minX, int minZ, int maxX, int maxZ, WorldTreePoint point, double radius,
            WorldTreeLayout layout) {
        int fromX = Math.max(minX, (int) Math.floor(point.x() - radius - 1.0));
        int toX = Math.min(maxX, (int) Math.ceil(point.x() + radius + 1.0));
        int fromZ = Math.max(minZ, (int) Math.floor(point.z() - radius - 1.0));
        int toZ = Math.min(maxZ, (int) Math.ceil(point.z() + radius + 1.0));
        int verticalRadius = (int) Math.ceil(radius * 0.72) + 1;
        for (int x = fromX; x < toX; x++) {
            for (int z = fromZ; z < toZ; z++) {
                double horizontal = Math.hypot(x - point.x(), z - point.z());
                if (horizontal > radius + 1.0) {
                    continue;
                }
                int bottom = Math.max(chunk.getMinBuildHeight(),
                        (int) Math.floor(point.y()) - verticalRadius);
                int top = Math.min(chunk.getMaxBuildHeight(),
                        (int) Math.ceil(point.y()) + verticalRadius + 1);
                for (int y = bottom; y < top; y++) {
                    double normalized = Math.pow(horizontal / radius, 2.0)
                            + Math.pow((y - point.y()) / (radius * 0.72), 2.0);
                    if (normalized <= 1.0) {
                        position.set(x, y, z);
                        chunk.setBlockState(position, branchState(normalized), false);
                    }
                }
            }
        }
    }

    static boolean chunkIntersectsBranches(int minX, int maxX, int minZ, int maxZ) {
        return minX <= MAX_BRANCH_REACH && maxX >= -MAX_BRANCH_REACH
                && minZ <= MAX_BRANCH_REACH && maxZ >= -MAX_BRANCH_REACH;
    }

    static boolean chunkIntersectsSpline(int minX, int maxX, int minZ, int maxZ, WorldTreeSpline spline) {
        double minSplineX = Double.POSITIVE_INFINITY;
        double maxSplineX = Double.NEGATIVE_INFINITY;
        double minSplineZ = Double.POSITIVE_INFINITY;
        double maxSplineZ = Double.NEGATIVE_INFINITY;
        for (WorldTreePoint point : spline.controlPoints()) {
            minSplineX = Math.min(minSplineX, point.x());
            maxSplineX = Math.max(maxSplineX, point.x());
            minSplineZ = Math.min(minSplineZ, point.z());
            maxSplineZ = Math.max(maxSplineZ, point.z());
        }
        double padding = spline.startRadius() + 2.0;
        return minX <= maxSplineX + padding && maxX >= minSplineX - padding
                && minZ <= maxSplineZ + padding && maxZ >= minSplineZ - padding;
    }

    static WorldTreePoint bezier(WorldTreeSpline spline, double t) {
        return WorldTreeRootGenerator.bezier(spline, t);
    }

    static double radiusAt(WorldTreeSpline spline, double t) {
        return WorldTreeRootGenerator.radiusAt(spline, t);
    }

    private static net.minecraft.world.level.block.state.BlockState branchState(double normalized) {
        if (normalized >= 0.78) {
            return WorldTreeBlocks.WORLD_TREE_BARK.get().defaultBlockState();
        }
        if (normalized >= 0.48) {
            return WorldTreeBlocks.WORLD_TREE_SAPWOOD.get().defaultBlockState();
        }
        return WorldTreeBlocks.WORLD_TREE_HEARTWOOD.get().defaultBlockState();
    }
}
