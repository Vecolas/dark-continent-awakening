package com.darkcontinent.nenfoundation.worldtree.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeHollow;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayoutGenerator;
import org.junit.jupiter.api.Test;

class WorldTreeHollowGeneratorTest {
    @Test
    void layoutPossuiPocketsInternosDeterministicos() {
        WorldTreeLayout primeiro = WorldTreeLayoutGenerator.generate(123L, 0, 0);
        WorldTreeLayout segundo = WorldTreeLayoutGenerator.generate(123L, 0, 0);

        assertEquals(primeiro.hollows(), segundo.hollows());
        assertTrue(primeiro.hollows().size() >= 8);
        assertTrue(primeiro.hollows().size() <= 12);
        for (WorldTreeHollow hollow : primeiro.hollows()) {
            assertTrue(hollow.contains(hollow.centerX(), hollow.centerY(), hollow.centerZ()));
        }
    }

    @Test
    void rotaPrincipalPermaneceDentroDoEnvelopeDoTronco() {
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(456L, 0, 0);
        for (int y = layout.trunk().baseY(); y < layout.trunk().topY(); y += 37) {
            double x = WorldTreeHollowGenerator.routeX(y, layout.seed());
            double z = WorldTreeHollowGenerator.routeZ(y, layout.seed());
            double distance = Math.hypot(x, z);
            double radius = WorldTreeTrunkGenerator.irregularRadius(layout.trunk().radiusAt(y),
                    (int) Math.round(x), y, (int) Math.round(z), layout.seed());
            assertTrue(distance + 4.0 < radius,
                    "rota interna deve preservar madeira ao redor em Y=" + y);
        }
    }
}
