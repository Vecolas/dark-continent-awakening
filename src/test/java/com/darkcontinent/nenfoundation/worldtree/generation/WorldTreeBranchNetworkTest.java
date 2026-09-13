package com.darkcontinent.nenfoundation.worldtree.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayoutGenerator;
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
}
