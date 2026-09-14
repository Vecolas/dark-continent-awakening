package com.darkcontinent.nenfoundation.worldtree;

import java.util.List;
import java.util.Objects;

/** Logical branch graph node; geometry is voxelized only after this graph is valid. */
public record WorldTreeBranchNode(
        int id,
        int parentId,
        int order,
        double attachmentT,
        WorldTreePoint attachmentPosition,
        WorldTreePoint attachmentDirection,
        WorldTreeSpline spline,
        double startRadius,
        double endRadius,
        List<Integer> children,
        int mainChildId) {

    public WorldTreeBranchNode {
        if (id < 0 || parentId >= id || order < 1 || order > 4) {
            throw new IllegalArgumentException("branch graph node metadata invalido");
        }
        if (parentId == id) {
            throw new IllegalArgumentException("galho nao pode ser pai de si mesmo");
        }
        if (!Double.isFinite(attachmentT) || attachmentT < 0.0 || attachmentT > 1.0) {
            throw new IllegalArgumentException("attachmentT invalido");
        }
        attachmentPosition = Objects.requireNonNull(attachmentPosition, "attachmentPosition");
        attachmentDirection = Objects.requireNonNull(attachmentDirection, "attachmentDirection");
        spline = Objects.requireNonNull(spline, "spline");
        if (!Double.isFinite(startRadius) || !Double.isFinite(endRadius)
                || startRadius <= 0.0 || endRadius <= 0.0 || endRadius >= startRadius) {
            throw new IllegalArgumentException("taper do galho invalido");
        }
        children = List.copyOf(Objects.requireNonNull(children, "children"));
        if (mainChildId >= 0 && !children.contains(mainChildId)) {
            throw new IllegalArgumentException("mainChildId precisa ser filho do node");
        }
    }

    public boolean isPrimary() {
        return order == 1;
    }
}
