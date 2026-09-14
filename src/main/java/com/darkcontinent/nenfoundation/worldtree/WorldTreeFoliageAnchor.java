package com.darkcontinent.nenfoundation.worldtree;

import java.util.List;
import java.util.Objects;

/** Formal support point for one foliage mass. No cluster exists without one. */
public record WorldTreeFoliageAnchor(
        int id,
        int anchorBranchId,
        int branchOrder,
        double anchorT,
        WorldTreePoint anchorPosition,
        WorldTreePoint anchorDirection,
        double clusterSize,
        double supportRadius,
        String foliageType,
        double coverageProfile,
        int parentFoliageNode,
        List<Integer> connectedChildren) {

    public WorldTreeFoliageAnchor {
        if (id < 0 || anchorBranchId < 0 || branchOrder < 1 || branchOrder > 4) {
            throw new IllegalArgumentException("foliage anchor metadata invalido");
        }
        if (!Double.isFinite(anchorT) || anchorT < 0.0 || anchorT > 1.0
                || !Double.isFinite(clusterSize) || clusterSize < 2.0
                || !Double.isFinite(supportRadius) || supportRadius <= 0.0
                || !Double.isFinite(coverageProfile) || coverageProfile <= 0.0
                || coverageProfile > 1.0) {
            throw new IllegalArgumentException("foliage anchor values invalidos");
        }
        anchorPosition = Objects.requireNonNull(anchorPosition, "anchorPosition");
        anchorDirection = Objects.requireNonNull(anchorDirection, "anchorDirection");
        foliageType = Objects.requireNonNull(foliageType, "foliageType");
        connectedChildren = List.copyOf(Objects.requireNonNull(connectedChildren,
                "connectedChildren"));
    }
}
