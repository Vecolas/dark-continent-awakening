package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeBranchNode;
import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeSpline;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeTrunkProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Gera galhos major por splines cubicas, somente no chunk solicitado. */
public final class WorldTreeBranchGenerator {
    private static final int MAX_SAMPLE_COUNT = 96;
    private static final double MAX_BRANCH_REACH = 320.0;

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
            generateSpline(chunk, position, minX, minZ, maxX, maxZ, spline, layout);
        }
        for (WorldTreeBranchNode node : layout.branchNodes()) {
            if (node.isPrimary()) {
                continue;
            }
            WorldTreeSpline spline = node.spline();
            if (!chunkIntersectsSpline(minX, maxX, minZ, maxZ, spline)) {
                continue;
            }
            generateSpline(chunk, position, minX, minZ, maxX, maxZ, spline, layout);
        }
    }

        private static void generateSpline(ChunkAccess chunk, BlockPos.MutableBlockPos position,
            int minX, int minZ, int maxX, int maxZ, WorldTreeSpline spline,
            WorldTreeLayout layout) {
            int sampleCount = sampleCount(spline);
            int sectionCount = sectionCount(spline);
            for (int sample = 0; sample <= sampleCount; sample++) {
                double t = (double) sample / sampleCount;
                WorldTreePoint point = bezier(spline, t);
                double radius = sectionedRadiusAt(spline, t, sectionCount);
                int fromX = Math.max(minX, (int) Math.floor(point.x() - radius - 1.0));
                int toX = Math.min(maxX, (int) Math.ceil(point.x() + radius + 1.0));
                int fromZ = Math.max(minZ, (int) Math.floor(point.z() - radius - 1.0));
                int toZ = Math.min(maxZ, (int) Math.ceil(point.z() + radius + 1.0));
                int verticalRadius = (int) Math.ceil(radius * 0.72) + 1;
                for (int x = fromX; x < toX; x++) {
                    for (int z = fromZ; z < toZ; z++) {
                        double horizontalX = x - point.x();
                        double horizontalZ = z - point.z();
                        double horizontalSquared = horizontalX * horizontalX
                                + horizontalZ * horizontalZ;
                        if (horizontalSquared > (radius + 1.0) * (radius + 1.0)) {
                            continue;
                        }
                        int bottom = Math.max(chunk.getMinBuildHeight(),
                                (int) Math.floor(point.y()) - verticalRadius);
                        int top = Math.min(chunk.getMaxBuildHeight(),
                                (int) Math.ceil(point.y()) + verticalRadius + 1);
                        for (int y = bottom; y < top; y++) {
                            double irregular = 1.0 + 0.08 * Math.sin(x * 0.37 + z * 0.19 + y * 0.11);
                            double normalizedX = horizontalX / (radius * irregular);
                            double normalizedY = (y - point.y()) / (radius * 0.72 * irregular);
                            double normalized = normalizedX * normalizedX
                                    + normalizedY * normalizedY;
                            if (normalized <= 1.0) {
                                position.set(x, y, z);
                                var state = WorldTreeTrunkGenerator.stateForNormalized(
                                        normalized, x, y, z, layout.seed());
                                if (state != null) {
                                    chunk.setBlockState(position, state, false);
                                }
                            }
                        }
                    }
                }
            }
        }

    private static int sampleCount(WorldTreeSpline spline) {
        double length = 0.0;
        var points = spline.controlPoints();
        for (int i = 1; i < points.size(); i++) {
            WorldTreePoint previous = points.get(i - 1);
            WorldTreePoint current = points.get(i);
            length += Math.sqrt(square(current.x() - previous.x())
                    + square(current.y() - previous.y())
                    + square(current.z() - previous.z()));
        }
        // A cross-section every ~1.5 blocks overlaps the previous one even at
        // the minimum radius, preventing disconnected voxel tips.
        return Math.max(16, Math.min(MAX_SAMPLE_COUNT, (int) Math.ceil(length / 1.5)));
    }

    private static int sectionCount(WorldTreeSpline spline) {
        double length = 0.0;
        var points = spline.controlPoints();
        for (int i = 1; i < points.size(); i++) {
            WorldTreePoint previous = points.get(i - 1);
            WorldTreePoint current = points.get(i);
            length += Math.sqrt(square(current.x() - previous.x())
                    + square(current.y() - previous.y())
                    + square(current.z() - previous.z()));
        }
        return Math.max(6, (int) Math.ceil(length / 10.0));
    }

    /**
     * Keeps short sections at a readable thickness instead of producing a
     * triangular taper. The profile still narrows overall, but each section
     * behaves like a deformed cylinder and overlaps its neighbors.
     */
    private static double sectionedRadiusAt(WorldTreeSpline spline, double t, int sections) {
        if (t >= 1.0) {
            return spline.endRadius();
        }
        double scaled = t * sections;
        int section = Math.min(sections - 1, (int) Math.floor(scaled));
        double sectionStart = section / (double) sections;
        double within = scaled - section;
        double smooth = within * within * (3.0 - 2.0 * within) * 0.55;
        double current = radiusAt(spline, sectionStart);
        double next = radiusAt(spline, (section + 1.0) / sections);
        double radius = current + (next - current) * smooth;
        double deformation = 1.0 + 0.035 * Math.sin(section * 12.9898 + spline.startRadius());
        return Math.max(spline.endRadius(), radius * deformation);
    }

    private static double square(double value) {
        return value * value;
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
                        var state = WorldTreeTrunkGenerator.stateForNormalized(
                                normalized, x, y, z, layout.seed());
                        if (state != null) {
                            chunk.setBlockState(position, state, false);
                        }
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

}
