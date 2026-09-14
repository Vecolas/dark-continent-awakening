package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeBranchNode;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeFoliageAnchor;
import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import java.util.ArrayList;
import java.util.List;

/** Selects formal foliage supports before any leaf voxel is generated. */
public final class WorldTreeFoliageAnchorGenerator {
    /**
     * A voxelized branch below this radius is only one block wide in places.
     * Anchoring a canopy there creates a mathematically valid but visibly
     * floating cluster, so such tips are deliberately ineligible.
     */
    static final double MIN_SUPPORT_RADIUS = WorldTreeBranchNetwork.MIN_TIP_RADIUS;

    private WorldTreeFoliageAnchorGenerator() {
    }

    public static List<WorldTreeFoliageAnchor> generate(long seed,
            List<WorldTreeBranchNode> branches) {
        List<WorldTreeFoliageAnchor> anchors = new ArrayList<>();
        for (WorldTreeBranchNode branch : branches) {
            // Primary and secondary branches are the arm. Foliage belongs to
            // the terminal fist, so a branch with children stays exposed.
            boolean terminalSupport = branch.order() >= 2 && branch.children().isEmpty();
            if (!terminalSupport) {
                continue;
            }
            int count = branch.order() == 4 && branch.startRadius() >= 5.5 ? 2 : 1;
            double firstT = branch.order() == 4 ? 0.76 : 0.80;
            double step = 0.14;
            double lastSupportedT = lastSupportedT(branch.spline());
            if (lastSupportedT < firstT) {
                continue;
            }
            for (int i = 0; i < count; i++) {
                double requestedT = firstT + step * i
                        + signed(mix(seed, branch.id(), i) >>> 11) * 0.035;
                double t = Math.min(lastSupportedT, Math.min(0.96, requestedT));
                if (t < firstT) {
                    continue;
                }
                WorldTreePoint position = WorldTreeBranchNetwork.bezier(branch.spline(), t);
                WorldTreePoint direction = WorldTreeBranchNetwork.tangentAt(branch.spline(), t);
                double supportRadius = WorldTreeBranchNetwork.radiusAt(branch.spline(), t);
                if (supportRadius < MIN_SUPPORT_RADIUS) {
                    continue;
                }
                double maxCluster = supportRadius > 7.0 ? 8.0
                        : supportRadius >= 4.0 ? 7.0 : 6.0;
                double clusterSize = Math.min(maxCluster,
                        2.5 + unit(mix(seed, branch.id(), i) >>> 23) * 5.5);
                String type = branch.order() == 4 ? "terminal"
                        : branch.order() == 1 ? "primary-supported" : "supported";
                anchors.add(new WorldTreeFoliageAnchor(anchors.size(), branch.id(), branch.order(),
                        t, position, direction, clusterSize, supportRadius, type,
                        branch.order() == 4 ? 0.70 : 0.64,
                        -1, List.of()));
            }
        }
        return List.copyOf(anchors);
    }

    private static double lastSupportedT(com.darkcontinent.nenfoundation.worldtree.WorldTreeSpline spline) {
        double denominator = spline.startRadius() - spline.endRadius();
        if (spline.startRadius() < MIN_SUPPORT_RADIUS) {
            return -1.0;
        }
        // Terminal branches are deliberately allowed to keep a constant
        // two/three-block voxel section. A zero taper is not a missing
        // support; it is the thick fist that carries the terminal foliage.
        if (denominator <= 0.0) {
            return 1.0;
        }
        return Math.min(1.0, (spline.startRadius() - MIN_SUPPORT_RADIUS) / denominator);
    }

    private static double unit(long value) {
        return (value & 0xFFFFL) / 65535.0;
    }

    private static double signed(long value) {
        return unit(value) * 2.0 - 1.0;
    }

    private static long mix(long seed, int id, int child) {
        long value = seed ^ ((long) id * 0x9E3779B97F4A7C15L)
                ^ ((long) child * 0xBF58476D1CE4E5B9L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
