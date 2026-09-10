package com.darkcontinent.nenfoundation.nen.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Portao do schema v1 do perfil persistente. */
class PersistentNenDataTest {

    private static final ResourceLocation TEN =
            ResourceLocation.fromNamespaceAndPath("nenfoundation", "ten");
    private static final ResourceLocation REN =
            ResourceLocation.fromNamespaceAndPath("nenfoundation", "ren");

    private static PersistentNenData cheio() {
        return new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL,
                true,
                NenCategory.TRANSMUTATION,
                true,
                42.5D, 0.75D, 1.25D,
                Map.of(TEN, 0.4D, REN, 0.1D),
                Set.of(TEN, REN),
                Set.of(ResourceLocation.fromNamespaceAndPath("nenfoundation", "aura_cortante")),
                Set.of(ResourceLocation.fromNamespaceAndPath("nenfoundation", "despertou")));
    }

    @Test
    @DisplayName("o perfil de quem nunca tocou em Nen e inteiramente neutro")
    void padraoNeutro() {
        PersistentNenData p = PersistentNenData.NAO_DESPERTADO;
        assertFalse(p.awakened(), "Campo com default util faz todo dado mentir.");
        assertSame(NenCategory.UNDETERMINED, p.category());
        assertFalse(p.categoryRevealed());
        assertEquals(0.0D, p.auraPotential());
        assertEquals(0.0D, p.control());
        assertEquals(0.0D, p.output());
        assertTrue(p.unlockedTechniques().isEmpty());
        assertTrue(p.unlockedAbilities().isEmpty());
        assertTrue(p.progressionFlags().isEmpty());
        assertTrue(p.techniqueProficiency().isEmpty());
        assertEquals(PersistentNenData.SCHEMA_ATUAL, p.schemaVersion());
    }

    @Test
    @DisplayName("o codec faz ida e volta sem perder campo")
    void idaEVolta() {
        PersistentNenData original = cheio();

        DataResult<JsonElement> escrito =
                PersistentNenData.CODEC.encodeStart(JsonOps.INSTANCE, original);
        JsonElement json = escrito.getOrThrow(msg -> new AssertionError("Falhou ao escrever: " + msg));

        PersistentNenData lido = PersistentNenData.CODEC
                .parse(JsonOps.INSTANCE, json)
                .getOrThrow(msg -> new AssertionError("Falhou ao ler: " + msg));

        assertEquals(original, lido,
                "Ida e volta perdeu ou alterou campo. Em jogo isso e progresso"
                        + " apagado no logout, sem nenhuma linha no log.");
    }

    @Test
    @DisplayName("um JSON vazio produz o perfil neutro, e nao um erro")
    void jsonVazioViraNeutro() {
        PersistentNenData lido = PersistentNenData.CODEC
                .parse(JsonOps.INSTANCE, new com.google.gson.JsonObject())
                .getOrThrow(msg -> new AssertionError("Falhou ao ler objeto vazio: " + msg));
        assertEquals(PersistentNenData.NAO_DESPERTADO, lido,
                "Save de jogador criado antes de um campo existir tem de ler como"
                        + " neutro. Erro aqui trava o login em vez de degradar.");
    }

    @Test
    @DisplayName("categoriaVisivel esconde a categoria enquanto ela nao foi revelada")
    void categoriaEscondida() {
        PersistentNenData escondida = new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, true, NenCategory.SPECIALIZATION, false,
                1.0D, 1.0D, 1.0D, Map.of(), Set.of(), Set.of(), Set.of());

        assertSame(NenCategory.SPECIALIZATION, escondida.category(),
                "A categoria existe internamente antes da revelacao.");
        assertSame(NenCategory.UNDETERMINED, escondida.categoriaVisivel(),
                "A interface nao pode ver a categoria antes da Water Divination.");
    }

    @Test
    @DisplayName("as colecoes do record sao imutaveis")
    void colecoesImutaveis() {
        PersistentNenData p = cheio();
        assertThrows(UnsupportedOperationException.class,
                () -> p.unlockedTechniques().add(TEN),
                "Mutar o perfil por uma referencia guardada em outro lugar contorna"
                        + " o servico -- e contorna a sincronizacao junto.");
        assertThrows(UnsupportedOperationException.class,
                () -> p.progressionFlags().clear());
    }

    @Test
    @DisplayName("proficiencia de tecnica nunca treinada e zero, e nao nulo")
    void proficienciaComPiso() {
        assertEquals(0.0D, PersistentNenData.NAO_DESPERTADO.proficiencia(TEN));
    }
}
