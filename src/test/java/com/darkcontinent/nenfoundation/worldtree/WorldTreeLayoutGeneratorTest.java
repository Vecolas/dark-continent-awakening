package com.darkcontinent.nenfoundation.worldtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorldTreeLayoutGeneratorTest {
    @Test
    void mesmaSeedEOrigemProduzemLayoutIgual() {
        WorldTreeLayout primeiro = WorldTreeLayoutGenerator.generate(123456789L, 2400, -1700);
        WorldTreeLayout segundo = WorldTreeLayoutGenerator.generate(123456789L, 2400, -1700);

        assertEquals(primeiro, segundo);
        assertNotSame(primeiro, segundo);
    }

    @Test
    void origemNaoMudaGeometriaMasMudaContratoEspacial() {
        WorldTreeLayout primeiro = WorldTreeLayoutGenerator.generate(42L, 100, 200);
        WorldTreeLayout segundo = WorldTreeLayoutGenerator.generate(42L, -100, -200);

        assertEquals(primeiro.trunk(), segundo.trunk());
        assertEquals(primeiro.roots(), segundo.roots());
        assertEquals(primeiro.branches(), segundo.branches());
        assertEquals(100, primeiro.overworldOriginX());
        assertEquals(-200, segundo.overworldOriginZ());
    }

    @Test
    void layoutRespeitaFaixasDeEscala() {
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(7L, 0, 0);

        assertTrue(layout.roots().size() >= 8, "o minimo de raizes deve ser respeitado");
        assertTrue(layout.roots().size() <= 14);
        assertTrue(layout.branches().size() >= 3);
        for (WorldTreeZone zone : new WorldTreeZone[] {
                WorldTreeZone.CLOUD_SEA, WorldTreeZone.MID_BOUGHS,
                WorldTreeZone.HIGH_CANOPY, WorldTreeZone.CROWN, WorldTreeZone.SUMMIT
        }) {
            long branchesInZone = layout.branches().stream()
                    .filter(branch -> branch.controlPoints().get(0).y() >= zone.minY()
                            && branch.controlPoints().get(0).y() < zone.maxYExclusive())
                    .count();
            assertTrue(branchesInZone >= 3 && branchesInZone <= 7,
                    "a zona deve possuir de 3 a 7 galhos major: " + zone);
        }
        assertEquals(48, layout.trunk().baseY());
        assertEquals(1200, layout.trunk().topY());
        assertEquals(WorldTreeZone.CLOUD_SEA, WorldTreeZone.forY(400));
        assertEquals(WorldTreeZone.SUMMIT, WorldTreeZone.forY(1450));
    }

    @Test
    void versaoDesconhecidaNaoEUsadaSilenciosamente() {
        assertThrows(IllegalArgumentException.class,
                () -> WorldTreeLayoutGenerator.generate(1L, 999, 0, 0));
    }

    @Test
    void zonasTemLimitesSemSobreposicao() {
        for (int i = 0; i < WorldTreeZone.values().length - 1; i++) {
            WorldTreeZone atual = WorldTreeZone.values()[i];
            WorldTreeZone proxima = WorldTreeZone.values()[i + 1];
            assertEquals(atual.maxYExclusive(), proxima.minY());
            assertTrue(atual.contains(atual.minY()));
            assertTrue(!atual.contains(atual.maxYExclusive()));
        }
    }
}
