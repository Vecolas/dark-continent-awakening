package com.darkcontinent.nenfoundation.enemy.encounter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class EncounterDefinitionTest {
    @Test
    void capturaPodeSerResolucaoNaoLetal() {
        EncounterDefinition definition = new EncounterDefinition("hyper_puffball", EncounterKind.CAPTURE,
                Set.of("nenfoundation:hyper_puffball"), 2, false, true);
        assertFalse(definition.allowsLethalResolution());
        assertTrue(definition.requiresObservation());
    }

    @Test
    void encontroNaturalNaoExigeObservacaoFormal() {
        assertThrows(IllegalArgumentException.class, () -> new EncounterDefinition(
                "bad", EncounterKind.NATURAL, Set.of("x"), 1, true, true));
    }
}
