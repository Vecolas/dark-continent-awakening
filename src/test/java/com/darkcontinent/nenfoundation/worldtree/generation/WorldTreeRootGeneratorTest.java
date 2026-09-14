package com.darkcontinent.nenfoundation.worldtree.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayoutGenerator;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeSpline;
import org.junit.jupiter.api.Test;

class WorldTreeRootGeneratorTest {
    @Test
    void splineComecaETerminaNosPontosDoLayout() {
        WorldTreeSpline spline = WorldTreeLayoutGenerator.generate(3L, 0, 0).roots().get(0);

        assertEquals(spline.controlPoints().get(0), WorldTreeRootGenerator.bezier(spline, 0.0));
        assertEquals(spline.controlPoints().get(3), WorldTreeRootGenerator.bezier(spline, 1.0));
        assertEquals(spline.startRadius(), WorldTreeRootGenerator.radiusAt(spline, 0.0), 1.0e-12);
        assertEquals(spline.endRadius(), WorldTreeRootGenerator.radiusAt(spline, 1.0), 1.0e-12);
    }

    @Test
    void filtroEspacialNaoGeraRaizesForaDoEnvelope() {
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(4L, 5000, -3000);

        assertTrue(WorldTreeRootGenerator.chunkIntersectsRoots(4992, 5008, -3008, -2992,
                layout.overworldOriginX(), layout.overworldOriginZ()));
        assertFalse(WorldTreeRootGenerator.chunkIntersectsRoots(0, 16, 0, 16,
                layout.overworldOriginX(), layout.overworldOriginZ()));
    }

    @Test
    void groveTemRaioDeterministico() {
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(5L, 100, 200);

        assertTrue(WorldTreeBaseStructure.insideGrove(100, 200, layout));
        assertTrue(WorldTreeBaseStructure.insideGrove(280, 200, layout));
        assertFalse(WorldTreeBaseStructure.insideGrove(281, 200, layout));
    }
}
