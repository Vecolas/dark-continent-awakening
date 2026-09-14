package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeBranchNode;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeFoliageAnchor;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic structural gate for foliage generation.
 *
 * <p>This is intentionally independent of Minecraft chunks and rendering. It
 * catches the silent failure where the branch graph exists but no terminal
 * support produces any leaf volume, before a server or client is started.
 */
public record WorldTreeFoliageValidation(
        int terminalBranches,
        int validAnchors,
        int projectedLeafBlocks,
        List<String> failures) {

    public WorldTreeFoliageValidation {
        failures = List.copyOf(failures);
    }

    public boolean valid() {
        return failures.isEmpty();
    }

    public static WorldTreeFoliageValidation inspect(WorldTreeLayout layout) {
        List<String> failures = new ArrayList<>();
        int terminalBranches = 0;
        for (WorldTreeBranchNode branch : layout.branchNodes()) {
            if (isEligibleTerminal(branch)) {
                terminalBranches++;
            }
        }

        int validAnchors = 0;
        int projectedLeafBlocks = 0;
        for (WorldTreeFoliageAnchor anchor : layout.foliageAnchors()) {
            if (!isValidAnchor(anchor, layout)) {
                failures.add("anchor " + anchor.id() + " sem suporte terminal valido");
                continue;
            }
            validAnchors++;
            projectedLeafBlocks += projectedLeafBlocks(anchor);
        }

        if (terminalBranches == 0) {
            failures.add("nenhum ramo terminal elegivel para folhagem");
        }
        if (validAnchors < 3) {
            failures.add("menos de 3 anchors de folhagem validos: " + validAnchors);
        }
        if (projectedLeafBlocks <= 0) {
            failures.add("nenhum volume de folhas projetado");
        }
        return new WorldTreeFoliageValidation(terminalBranches, validAnchors,
                projectedLeafBlocks, failures);
    }

    public static int projectedLeafBlocks(WorldTreeFoliageAnchor anchor) {
        double radius = anchor.clusterSize();
        double ellipsoidVolume = (4.0 / 3.0) * Math.PI
                * (radius * 1.12) * (radius * 0.72) * (radius * 0.78);
        double transitionVolume = Math.max(1.0, anchor.supportRadius() * 0.28)
                * Math.max(2.0, radius * 0.20) * Math.max(2.0, radius * 0.20);
        return Math.max(1, (int) Math.ceil((ellipsoidVolume + transitionVolume)
                * anchor.coverageProfile()));
    }

    private static boolean isValidAnchor(WorldTreeFoliageAnchor anchor, WorldTreeLayout layout) {
        if (anchor.anchorBranchId() < 0 || anchor.anchorBranchId() >= layout.branchNodes().size()) {
            return false;
        }
        WorldTreeBranchNode branch = layout.branchNodes().get(anchor.anchorBranchId());
        double support = WorldTreeBranchNetwork.radiusAt(branch.spline(), anchor.anchorT());
        return isEligibleTerminal(branch)
                && anchor.branchOrder() == branch.order()
                && anchor.anchorT() >= 0.70
                && support >= WorldTreeFoliageAnchorGenerator.MIN_SUPPORT_RADIUS
                && anchor.supportRadius() >= WorldTreeFoliageAnchorGenerator.MIN_SUPPORT_RADIUS
                && anchor.clusterSize() <= Math.max(4.0, support * 2.0);
    }

    private static boolean isEligibleTerminal(WorldTreeBranchNode branch) {
        return branch.order() >= 2 && branch.children().isEmpty();
    }
}
