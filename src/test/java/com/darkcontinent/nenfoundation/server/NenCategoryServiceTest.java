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
 * <p>Ele cobre a parte PURA: o que atribuir e revelar fazem com o perfil. O
 * ciclo completo -- recusas, eventos, snapshot, dois jogadores -- depende de um
 * servidor de pe e esta no gametest.
 *
 * <p>As mutacoes foram extraidas justamente para isso. Logica que so pode ser
 * exercitada com o Minecraft aberto nao e exercitada.
 */
class NenCategoryServiceTest {

    private static ResourceLocation id(String caminho) {
        return ResourceLocation.fromNamespaceAndPath("nenfoundation", caminho);
    }

    /** Um jogador desperto, sem categoria. O estado normal antes do ritual. */
    private static PersistentNenData desperto() {
        return new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, true, NenCategory.UNDETERMINED, false,
                0.0D, 0.0D, 0.0D, Map.of(), Set.of(), Set.of(), Set.of(Marcos.DESPERTOU));
    }

    // -------------------------------------------------------- atribuicao

    @Test
    @DisplayName("atribuir grava a categoria e NAO revela")
    void atribuirNaoRevela() {
        PersistentNenData depois =
                NenCategoryService.comCategoria(desperto(), NenCategory.CONJURATION);

        assertSame(NenCategory.CONJURATION, depois.category());
        assertFalse(depois.categoryRevealed(),
                "Atribuir ligou a revelacao junto. Se a categoria so passasse a"
                        + " ser conhecida ao ser criada, a Water Divination nao"
                        + " descobriria nada -- ela criaria.");
        assertFalse(depois.temMarco(Marcos.CATEGORIA_REVELADA),
                "O marco da revelacao entrou numa atribuicao.");
    }

    @Test
    @DisplayName("a categoria atribuida NAO sai no que a interface enxerga")
    void atribuicaoNaoVazaNaProjecao() {
        PersistentNenData depois =
                NenCategoryService.comCategoria(desperto(), NenCategory.SPECIALIZATION);

        assertSame(NenCategory.UNDETERMINED, depois.categoriaVisivel(),
                "categoriaVisivel() entregou a categoria antes da revelacao. E"
                        + " este valor que vai para o snapshot: com ele errado, a"
                        + " revelacao vira teatro e qualquer cliente ja sabe.");
    }

    @Test
    @DisplayName("atribuir preserva todo o resto do perfil")
    void atribuirNaoPisaNoResto() {
        PersistentNenData antes = new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, true, NenCategory.UNDETERMINED, false,
                42.5D, 0.75D, 1.25D,
                Map.of(id("ten"), 0.4D),
                Set.of(id("ten")),
                Set.of(id("disparo_de_aura")),
                Set.of(Marcos.DESPERTOU, id("outro_marco")));

        PersistentNenData depois =
                NenCategoryService.comCategoria(antes, NenCategory.EMISSION);

        assertSame(NenCategory.EMISSION, depois.category());
        assertTrue(depois.awakened(), "awakened");
        assertEquals(42.5D, depois.auraPotential(), "aura_potential");
        assertEquals(0.75D, depois.control(), "control");
        assertEquals(1.25D, depois.output(), "output");
        assertEquals(Map.of(id("ten"), 0.4D), depois.techniqueProficiency());
        assertEquals(Set.of(id("ten")), depois.unlockedTechniques());
        assertEquals(Set.of(id("disparo_de_aura")), depois.unlockedAbilities());
        assertTrue(depois.temMarco(Marcos.DESPERTOU),
                "Marco que ja existia nao pode sumir ao atribuir categoria.");
        assertTrue(depois.temMarco(id("outro_marco")));
    }

    @Test
    @DisplayName("atribuir a mesma categoria devolve o MESMO objeto")
    void atribuirEIdempotente() {
        PersistentNenData primeira =
                NenCategoryService.comCategoria(desperto(), NenCategory.MANIPULATION);
        PersistentNenData segunda =
                NenCategoryService.comCategoria(primeira, NenCategory.MANIPULATION);

        assertSame(primeira, segunda,
                "Devolver um objeto novo e igual faria o servico de perfil"
                        + " comparar, achar igual e nao gravar -- funcionaria. Mas"
                        + " devolver o MESMO deixa a intencao explicita.");
    }

    // -------------------------------------------------------- revelacao

    @Test
    @DisplayName("revelar liga o booleano e poe o marco")
    void revelarLigaEMarca() {
        PersistentNenData comCategoria =
                NenCategoryService.comCategoria(desperto(), NenCategory.TRANSMUTATION);

        PersistentNenData depois = NenCategoryService.comRevelacao(comCategoria);

        assertTrue(depois.categoryRevealed());
        assertTrue(depois.temMarco(Marcos.CATEGORIA_REVELADA),
                "O marco e o que a quest consulta DEPOIS do fato. O evento passa"
                        + " uma vez e some; quem relogar e reabrir o questbook nao"
                        + " tem evento nenhum para escutar.");
        assertSame(NenCategory.TRANSMUTATION, depois.categoriaVisivel(),
                "Depois de revelar, a projecao tem de mostrar a categoria.");
    }

    @Test
    @DisplayName("revelar NAO inventa categoria para quem nao tem")
    void revelarNaoSorteia() {
        PersistentNenData depois = NenCategoryService.comRevelacao(desperto());

        assertSame(NenCategory.UNDETERMINED, depois.category(),
                "A revelacao atribuiu uma categoria. Consertar 'quando falta'"
                        + " esconde para sempre quem chamou o ritual fora de ordem.");
        assertSame(NenCategory.UNDETERMINED, depois.categoriaVisivel(),
                "Revelou o neutro: o jogador veria 'Undetermined' como se fosse"
                        + " um resultado.");
    }

    @Test
    @DisplayName("revelar preserva a categoria e o resto do perfil")
    void revelarNaoPisaNoResto() {
        PersistentNenData antes = new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, true, NenCategory.ENHANCEMENT, false,
                10.0D, 0.5D, 2.0D,
                Map.of(id("ten"), 0.2D),
                Set.of(id("ten")),
                Set.of(),
                Set.of(Marcos.DESPERTOU));

        PersistentNenData depois = NenCategoryService.comRevelacao(antes);

        assertSame(NenCategory.ENHANCEMENT, depois.category(),
                "A revelacao trocou a categoria de quem ja tinha uma.");
        assertEquals(10.0D, depois.auraPotential());
        assertEquals(Set.of(id("ten")), depois.unlockedTechniques());
        assertTrue(depois.temMarco(Marcos.DESPERTOU));
        assertTrue(depois.temMarco(Marcos.CATEGORIA_REVELADA));
    }

    @Test
    @DisplayName("revelar duas vezes devolve o MESMO objeto, sem regravar")
    void revelarEIdempotente() {
        PersistentNenData comCategoria =
                NenCategoryService.comCategoria(desperto(), NenCategory.EMISSION);
        PersistentNenData primeira = NenCategoryService.comRevelacao(comCategoria);
        PersistentNenData segunda = NenCategoryService.comRevelacao(primeira);

        assertSame(primeira, segunda);
    }

    @Test
    @DisplayName("perfil revelado mas sem o marco e consertado")
    void reveladoSemMarcoEConsertado() {
        // Pode acontecer com save anterior a este marco existir, ou com alguem
        // que ligou o booleano por outro caminho.
        PersistentNenData semMarco = new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, true, NenCategory.CONJURATION, true,
                0.0D, 0.0D, 0.0D, Map.of(), Set.of(), Set.of(), Set.of(Marcos.DESPERTOU));

        PersistentNenData depois = NenCategoryService.comRevelacao(semMarco);

        assertTrue(depois.temMarco(Marcos.CATEGORIA_REVELADA),
                "revelado=true sem o marco e um estado pela metade. Tratar como"
                        + " 'ja sabia' deixaria o marco faltando para sempre, e a"
                        + " quest que depende dele nunca completaria.");
        assertTrue(depois.categoryRevealed());
    }

    @Test
    @DisplayName("marco presente sem o booleano tambem e consertado")
    void marcoSemBooleanoEConsertado() {
        PersistentNenData soMarco = new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, true, NenCategory.CONJURATION, false,
                0.0D, 0.0D, 0.0D, Map.of(), Set.of(), Set.of(),
                Set.of(Marcos.DESPERTOU, Marcos.CATEGORIA_REVELADA));

        PersistentNenData depois = NenCategoryService.comRevelacao(soMarco);

        assertTrue(depois.categoryRevealed(),
                "O marco dizia que a revelacao aconteceu e o booleano dizia que"
                        + " nao. O booleano e o que a projecao le: sem conserto, o"
                        + " jogador com o marco continuaria vendo Undetermined.");
    }

    // ------------------------------------------- a ordem entre as duas

    @Test
    @DisplayName("atribuir depois de revelar NAO troca a categoria")
    void atribuirNaoTrocaDeQuemJaTem() {
        PersistentNenData revelado = NenCategoryService.comRevelacao(
                NenCategoryService.comCategoria(desperto(), NenCategory.EMISSION));

        // A mutacao pura e usada so pelo servico, que ja recusa antes de chegar
        // aqui (JA_TINHA). O teste existe porque a mutacao e package-private e
        // acessivel: se alguem a chamar direto num dia de pressa, o resultado
        // tem de ser visivel, e nao uma troca silenciosa de categoria.
        PersistentNenData depois =
                NenCategoryService.comCategoria(revelado, NenCategory.ENHANCEMENT);

        assertSame(NenCategory.ENHANCEMENT, depois.category(),
                "A mutacao pura troca o que mandarem; quem protege e o servico."
                        + " Se este teste falhar, a protecao mudou de lugar e a"
                        + " documentacao do servico precisa mudar junto.");
        assertTrue(depois.categoryRevealed(),
                "A troca nao pode desligar a revelacao pelo caminho.");
    }
}
