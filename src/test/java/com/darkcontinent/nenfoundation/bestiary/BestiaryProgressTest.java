package com.darkcontinent.nenfoundation.bestiary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class BestiaryProgressTest {
    @Test
    void observacaoELutaAvancamSemRegredirConhecimento() {
        var observed = BestiaryProgress.UNKNOWN.observe(42L);
        var fought = observed.fought();

        assertEquals(BestiaryKnowledgeLevel.OBSERVED, observed.knowledgeLevel());
        assertEquals(BestiaryKnowledgeLevel.FOUGHT, fought.knowledgeLevel());
        assertEquals(1, fought.timesSeen());
        assertEquals(1, fought.timesFought());
        assertEquals(42L, fought.firstSeenTime());
    }

    @Test
    void progressoNaoAceitaContadorNegativo() {
        assertThrows(IllegalArgumentException.class, () -> new BestiaryProgress(
                BestiaryKnowledgeLevel.UNKNOWN, -1, 0, 0, 0, 0L, 0L,
                java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of()));
    }

    @Test
    void dadosDoJogadorCodificamSemPerderEntrada() {
        var id = ResourceLocation.fromNamespaceAndPath("nenfoundation", "foxbear");
        var data = BestiaryPlayerData.EMPTY.withProgress(id, BestiaryProgress.UNKNOWN.observe(12L));

        var json = BestiaryPlayerData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        var decoded = BestiaryPlayerData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEquals(BestiaryKnowledgeLevel.OBSERVED, decoded.progress(id).knowledgeLevel());
        assertEquals(12L, decoded.progress(id).firstSeenTime());
    }

    @Test
    void pesquisaAcumulaPontosEAvancaAteOMinimoSolicitado() {
        var progress = BestiaryProgress.UNKNOWN
                .withResearchPoints(2, BestiaryKnowledgeLevel.OBSERVED)
                .withResearchPoints(1, BestiaryKnowledgeLevel.STUDIED);

        assertEquals(BestiaryKnowledgeLevel.STUDIED, progress.knowledgeLevel());
        assertEquals(3, progress.researchPoints());
    }

    @Test
    void pesquisaNaoPermitePontosNegativos() {
        assertThrows(IllegalArgumentException.class,
                () -> BestiaryProgress.UNKNOWN.withResearchPoints(-1, BestiaryKnowledgeLevel.STUDIED));
    }
}
