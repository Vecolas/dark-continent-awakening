package com.darkcontinent.nenfoundation.enemy.chimera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChimeraStateTest {
    @Test
    void genomaMantemTraitsCuradosEValoresNormalizados() {
        ChimeraGenome genome = new ChimeraGenome("wolf", "bat", ChimeraMorphology.WINGED,
                Set.of(ChimeraTrait.SPEED, ChimeraTrait.WINGS), 0.4F, 0.6F, 0.5F, 0.2F);
        assertEquals(2, genome.traits().size());
        assertEquals(ChimeraMorphology.WINGED, genome.morphology());
    }

    @Test
    void coloniaAcumulaFoodGenePoolEAlertaComLimites() {
        ChimeraColonyState colony = new ChimeraColonyState(UUID.randomUUID(), UUID.randomUUID(),
                ChimeraColonyStage.FOUNDED, 0, Map.of(), 3, 0);
        colony = colony.alimentar("wolf", 2).elevarAlerta();
        assertEquals(2, colony.foodScore());
        assertEquals(2, colony.genePool().get("wolf"));
        assertEquals(4, colony.alertLevel());
        assertEquals(4, colony.elevarAlerta().alertLevel());
    }

    @Test
    void rejeitaGenomaForaDaFaixaEAlimentoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new ChimeraGenome(
                "wolf", "bat", ChimeraMorphology.BIPED, Set.of(), 1.1F, 0, 0, 0));
        ChimeraColonyState colony = new ChimeraColonyState(UUID.randomUUID(), UUID.randomUUID(),
                ChimeraColonyStage.FOUNDED, 0, Map.of(), 0, 0);
        assertThrows(IllegalArgumentException.class, () -> colony.alimentar("", 1));
    }
}
