package com.darkcontinent.nenfoundation.worldtree.generation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayoutGenerator;
import org.junit.jupiter.api.Test;

class WorldTreeFoliageValidationTest {
    @Test
    void vinteSeedsProduzemFolhagemTerminalMensuravel() {
        for (long seed = 0; seed < 20; seed++) {
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            WorldTreeFoliageValidation validation = WorldTreeFoliageValidation.inspect(layout);

            assertTrue(validation.valid(), "seed " + seed + " falhou: "
                    + validation.failures());
            assertTrue(validation.terminalBranches() > 0);
            assertTrue(validation.validAnchors() >= 3);
            assertTrue(validation.projectedLeafBlocks() > 0);
        }
    }
}
