package com.darkcontinent.nenfoundation.worldtree.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayoutGenerator;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeBranchNode;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeFoliageAnchor;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeSpline;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorldTreeBranchNetworkTest {
    @Test
    void redeCriaSubgalhosDeterministicosEComTaper() {
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(42L, 0, 0);
        List<?> first = WorldTreeBranchNetwork.secondaryAndTertiary(layout);
        List<?> second = WorldTreeBranchNetwork.secondaryAndTertiary(layout);

        assertTrue(first.size() > layout.branches().size());
        assertEquals(first.toString(), second.toString());
    }

    @Test
    void grafoTemPaiTaperEFilhosIndexados() {
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(42L, 0, 0);

        assertTrue(layout.branchNodes().size() > layout.branches().size());
        for (WorldTreeBranchNode node : layout.branchNodes()) {
            if (node.isPrimary()) {
                assertEquals(-1, node.parentId());
                continue;
            }
            WorldTreeBranchNode parent = layout.branchNodes().get(node.parentId());
            assertTrue(node.endRadius() >= WorldTreeBranchNetwork.MIN_TIP_RADIUS,
                    "ponta fina demais no galho " + node.id());
            assertTrue(node.startRadius() <= WorldTreeBranchNetwork.radiusAt(
                    parent.spline(), node.attachmentT()) * 0.65 + 0.0001);
            assertTrue(node.endRadius() < node.startRadius());
            assertTrue(parent.children().contains(node.id()));
            assertTrue(distance(node.spline().controlPoints().get(0), node.attachmentPosition())
                    <= node.startRadius());
        }
    }

    @Test
    void anchorsNuncaNascemEmPontaVoxelizadaComUmBloco() {
        int checked = 0;
        for (long seed = 0; seed < 10; seed++) {
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            for (WorldTreeSpline primary : layout.branches()) {
                assertTrue(primary.endRadius() >= WorldTreeBranchNetwork.MIN_TIP_RADIUS,
                        "ponta fina demais no galho primario");
            }
            for (WorldTreeFoliageAnchor anchor : layout.foliageAnchors()) {
                double support = WorldTreeBranchNetwork.radiusAt(
                        layout.branchNodes().get(anchor.anchorBranchId()).spline(), anchor.anchorT());
                assertTrue(anchor.branchOrder() >= 2);
                assertTrue(anchor.anchorT() >= 0.76,
                        "folhagem saiu do punho terminal no seed " + seed);
                assertTrue(support >= WorldTreeFoliageAnchorGenerator.MIN_SUPPORT_RADIUS);
                assertTrue(anchor.supportRadius() >= WorldTreeFoliageAnchorGenerator.MIN_SUPPORT_RADIUS);
                assertTrue(anchor.supportRadius() >= WorldTreeBranchNetwork.MIN_TIP_RADIUS);
                checked++;
            }
            assertTrue(layout.foliageAnchors().size() >= 3,
                    "seed " + seed + " perdeu a copa por falta de anchors");
        }
        assertTrue(checked > 0, "nenhum anchor foi validado");
    }

    private static double distance(com.darkcontinent.nenfoundation.worldtree.WorldTreePoint a,
            com.darkcontinent.nenfoundation.worldtree.WorldTreePoint b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
