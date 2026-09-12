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
 * Portao da mutacao de despertar.
 *
 * <p>Ele cobre a parte PURA: o que o despertar faz com o perfil. O ciclo
 * completo -- eventos, cancelamento, sync -- depende de um servidor de pe e
 * esta no gametest.
 *
 * <p>A mutacao foi extraida justamente para isso. Logica que so pode ser
 * exercitada com o Minecraft aberto nao e exercitada.
 */
class NenAwakeningServiceTest {

    private static ResourceLocation id(String caminho) {
        return ResourceLocation.fromNamespaceAndPath("nenfoundation", caminho);
    }

    @Test
    @DisplayName("despertar liga awakened e poe o marco")
    void despertarLigaEMarca() {
        PersistentNenData depois =
                NenAwakeningService.comDespertar(PersistentNenData.NAO_DESPERTADO);

        assertTrue(depois.awakened());
        assertTrue(depois.temMarco(Marcos.DESPERTOU),
                "O marco e o que quests e conteudo consultam sem conhecer a"
                        + " forma do perfil. Sem ele, o booleano fica sozinho.");
    }

    @Test
    @DisplayName("despertar NAO atribui categoria")
    void despertarNaoAtribuiCategoria() {
        PersistentNenData depois =
                NenAwakeningService.comDespertar(PersistentNenData.NAO_DESPERTADO);

        assertSame(NenCategory.UNDETERMINED, depois.category(),
                "Despertar e saber a propria categoria sao dois fatos"
                        + " diferentes. Se o despertar ja sorteasse a categoria, a"
                        + " Water Divination nao descobriria nada -- ela criaria.");
        assertFalse(depois.categoryRevealed());
    }

    @Test
    @DisplayName("despertar preserva todo o resto do perfil")
    void despertarNaoPisaNoResto() {
        PersistentNenData antes = new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, false,
                NenCategory.EMISSION, true,
                42.5D, 0.75D, 1.25D,
                Map.of(id("ten"), 0.4D),
                Set.of(id("ten")),
                Set.of(id("disparo_de_aura")),
                Set.of(id("outro_marco")));

        PersistentNenData depois = NenAwakeningService.comDespertar(antes);

        assertTrue(depois.awakened());
        assertSame(NenCategory.EMISSION, depois.category(), "category");
        assertTrue(depois.categoryRevealed(), "category_revealed");
        assertEquals(42.5D, depois.auraPotential(), "aura_potential");
        assertEquals(0.75D, depois.control(), "control");
        assertEquals(1.25D, depois.output(), "output");
        assertEquals(Map.of(id("ten"), 0.4D), depois.techniqueProficiency());
        // DESPERTAR LIBERA TEN E REN, e a tecnica que ja estava continua la.
        // Sem isto o jogador desperta sem tecnica nenhuma e a roda nasce vazia.
        // Nao e progressao: progressao e o M6, com requisito e treino.
        assertEquals(Set.of(id("ten"), id("ren"), id("zetsu"), id("gyo")),
                depois.unlockedTechniques());
        assertEquals(Set.of(id("disparo_de_aura")), depois.unlockedAbilities());
        assertTrue(depois.temMarco(id("outro_marco")),
                "Marco que ja existia nao pode sumir ao despertar.");
        assertTrue(depois.temMarco(Marcos.DESPERTOU));
    }

    @Test
    @DisplayName("despertar duas vezes devolve o MESMO objeto, sem regravar")
    void despertarEIdempotente() {
        PersistentNenData primeira =
                NenAwakeningService.comDespertar(PersistentNenData.NAO_DESPERTADO);
        PersistentNenData segunda = NenAwakeningService.comDespertar(primeira);

        assertSame(primeira, segunda,
                "Devolver um objeto novo e igual faria o servico de perfil"
                        + " comparar, achar igual e nao gravar -- funcionaria. Mas"
                        + " devolver o MESMO deixa a intencao explicita: nao ha o"
                        + " que fazer aqui.");
    }

    @Test
    @DisplayName("perfil com awakened mas sem o marco e consertado")
    void perfilPelaMetadeEConsertado() {
        // Pode acontecer com save antigo, ou com alguem que ligou o booleano
        // por outro caminho. O servico nao assume que os dois andam juntos.
        PersistentNenData semMarco = new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, true, NenCategory.UNDETERMINED, false,
                0.0D, 0.0D, 0.0D, Map.of(), Set.of(), Set.of(), Set.of());

        PersistentNenData depois = NenAwakeningService.comDespertar(semMarco);

        assertTrue(depois.temMarco(Marcos.DESPERTOU),
                "awakened=true sem o marco e um estado pela metade. Tratar como"
                        + " 'ja despertou' deixaria o marco faltando para sempre, e"
                        + " a quest que depende dele nunca completaria.");
    }
}
