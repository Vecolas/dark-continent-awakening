package com.darkcontinent.nenfoundation.worldtree.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeSpline;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorldTreeBranchGeneratorTest {
    @Test
    void bezierMantemExtremosDoGalho() {
        WorldTreeSpline spline = spline();

        assertEquals(spline.controlPoints().get(0), WorldTreeBranchGenerator.bezier(spline, 0.0));
        assertEquals(spline.controlPoints().get(3), WorldTreeBranchGenerator.bezier(spline, 1.0));
    }

    @Test
    void envelopeRejeitaChunkDistante() {
        assertTrue(WorldTreeBranchGenerator.chunkIntersectsSpline(-16, 0, -16, 0, spline()));
        assertFalse(WorldTreeBranchGenerator.chunkIntersectsSpline(512, 528, 512, 528, spline()));
        assertTrue(WorldTreeBranchGenerator.chunkIntersectsBranches(-16, 0, -16, 0));
    }

    @Test
    void raioAfinaDeFormaDeterministica() {
        WorldTreeSpline spline = spline();

        assertEquals(18.0, WorldTreeBranchGenerator.radiusAt(spline, 0.0));
        assertEquals(6.0, WorldTreeBranchGenerator.radiusAt(spline, 1.0));
        assertEquals(12.0, WorldTreeBranchGenerator.radiusAt(spline, 0.5));
    }

    private static WorldTreeSpline spline() {
        return new WorldTreeSpline(List.of(
                new WorldTreePoint(0, 600, 0),
                new WorldTreePoint(20, 610, 0),
                new WorldTreePoint(80, 630, 20),
                new WorldTreePoint(140, 650, 40)), 18, 6);
    }
}
