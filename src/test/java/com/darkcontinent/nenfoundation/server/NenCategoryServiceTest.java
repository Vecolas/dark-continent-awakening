package com.darkcontinent.nenfoundation.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.progression.Marcos;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao das mutacoes de categoria.
 *
 * <p>Cobre a parte PURA: o que atribuir e revelar fazem com o perfil. O ciclo
 * completo -- eventos, idempotencia do servico, sync, o que o cliente recebe
 * -- depende de um servidor de pe e esta no gametest.
 *
 * <p>As mutacoes foram extraidas justamente para isso: logica que so pode ser
 * exercitada com o Minecraft aberto nao e exercitada.
 */
class NenCategoryServiceTest {

    private static ResourceLocation id(String caminho) {
        return ResourceLocation.fromNamespaceAndPath("nenfoundation", caminho);
    }

    /** Um jogador desperto, sem categoria. O estado de onde a atribuicao parte. */
    private static PersistentNenData desperto() {
        return NenAwakeningService.comDespertar(PersistentNenData.NAO_DESPERTADO);
    }

    @Test
    @DisplayName("atribuir grava a categoria e NAO revela")
    void atribuirNaoRevela() {
        PersistentNenData depois =
                NenCategoryService.comCategoria(desperto(), NenCategory.CONJURATION);

        assertSame(NenCategory.CONJURATION, depois.category());
        assertFalse(depois.categoryRevealed(),
                "Atribuir revelou junto. Se os dois fossem o mesmo passo, a"
                        + " Adivinhacao da Agua nao DESCOBRIRIA a categoria -- ela a"
                        + " criaria, e o ritual viraria um sorteio com animacao.");
        assertSame(NenCategory.UNDETERMINED, depois.categoriaVisivel(),
                "A categoria vazou para o que a interface le.");
    }

    @Test
    @DisplayName("atribuir NAO poe marco: os marcos viajam ate o cliente")
    void atribuirNaoPoeMarco() {
        PersistentNenData depois =
                NenCategoryService.comCategoria(desperto(), NenCategory.SPECIALIZATION);

        assertEquals(Set.of(Marcos.DESPERTOU), depois.progressionFlags(),
                "Apareceu marco novo na atribuicao. Os marcos viajam INTEIROS"
                        + " no snapshot ate o cliente: um marco aqui contaria ao"
                        + " cliente um fato que o jogador ainda nao tem.");
    }

    @Test
    @DisplayName("revelar liga a revelacao, poe o marco e nao toca na categoria")
    void revelarLigaEMarca() {
        PersistentNenData atribuido =
                NenCategoryService.comCategoria(desperto(), NenCategory.EMISSION);

        PersistentNenData depois = NenCategoryService.comRevelacao(atribuido);

        assertTrue(depois.categoryRevealed());
        assertSame(NenCategory.EMISSION, depois.category(), "a categoria mudou ao revelar.");
        assertSame(NenCategory.EMISSION, depois.categoriaVisivel(),
                "Depois de revelada, a interface tem de ver a categoria real.");
        assertTrue(depois.temMarco(Marcos.CATEGORIA_REVELADA),
                "Sem o marco, a quest 'descubra o seu Nen' nunca completa -- e"
                        + " nada acusa, porque o booleano esta certo.");
    }

    @Test
    @DisplayName("revelar duas vezes devolve o MESMO objeto")
    void revelarEIdempotente() {
        PersistentNenData revelado = NenCategoryService.comRevelacao(
                NenCategoryService.comCategoria(desperto(), NenCategory.MANIPULATION));

        assertSame(revelado, NenCategoryService.comRevelacao(revelado),
                "Devolver um objeto novo e igual funcionaria -- o servico de"
                        + " perfil compararia e nao gravaria. Devolver o MESMO deixa"
                        + " explicito que nao ha o que fazer.");
    }

    @Test
    @DisplayName("perfil revelado sem o marco e consertado")
    void reveladoSemMarcoEConsertado() {
        // Save antigo, ou alguem que ligou o booleano por outro caminho. O
        // servico nao assume que os dois andam juntos.
        PersistentNenData pelaMetade = new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, true, NenCategory.ENHANCEMENT, true,
                0.0D, 0.0D, 0.0D, Map.of(), Set.of(), Set.of(), Set.of());

        assertTrue(NenCategoryService.comRevelacao(pelaMetade).temMarco(Marcos.CATEGORIA_REVELADA),
                "category_revealed=true sem o marco e um estado pela metade."
                        + " Tratar como 'ja revelou' deixaria o marco faltando para"
                        + " sempre.");
    }

    @Test
    @DisplayName("atribuir preserva todo o resto do perfil")
    void atribuirNaoPisaNoResto() {
        PersistentNenData antes = new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, true,
                NenCategory.UNDETERMINED, false,
                42.5D, 0.75D, 1.25D,
                Map.of(id("ten"), 0.4D),
                Set.of(id("ten")),
                Set.of(id("disparo_de_aura")),
                Set.of(id("outro_marco")));

        PersistentNenData depois =
                NenCategoryService.comCategoria(antes, NenCategory.TRANSMUTATION);

        assertSame(NenCategory.TRANSMUTATION, depois.category());
        assertTrue(depois.awakened(), "awakened");
        assertEquals(42.5D, depois.auraPotential(), "aura_potential");
        assertEquals(0.75D, depois.control(), "control");
        assertEquals(1.25D, depois.output(), "output");
        assertEquals(Map.of(id("ten"), 0.4D), depois.techniqueProficiency());
        assertEquals(Set.of(id("ten")), depois.unlockedTechniques());
        assertEquals(Set.of(id("disparo_de_aura")), depois.unlockedAbilities());
        assertTrue(depois.temMarco(id("outro_marco")), "marco que ja existia sumiu.");
    }

    @Test
    @DisplayName("revelar preserva todo o resto do perfil")
    void revelarNaoPisaNoResto() {
        PersistentNenData antes = new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, true,
                NenCategory.EMISSION, false,
                42.5D, 0.75D, 1.25D,
                Map.of(id("ten"), 0.4D),
                Set.of(id("ten")),
                Set.of(id("disparo_de_aura")),
                Set.of(id("outro_marco")));

        PersistentNenData depois = NenCategoryService.comRevelacao(antes);

        assertEquals(42.5D, depois.auraPotential(), "aura_potential");
        assertEquals(0.75D, depois.control(), "control");
        assertEquals(1.25D, depois.output(), "output");
        assertEquals(Map.of(id("ten"), 0.4D), depois.techniqueProficiency());
        assertEquals(Set.of(id("ten")), depois.unlockedTechniques());
        assertEquals(Set.of(id("disparo_de_aura")), depois.unlockedAbilities());
        assertTrue(depois.temMarco(id("outro_marco")), "marco que ja existia sumiu.");
        assertTrue(depois.temMarco(Marcos.CATEGORIA_REVELADA));
    }
}
