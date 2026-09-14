package com.darkcontinent.nenfoundation.enemy.encounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.content.GreedIslandProfiles;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao das RECEITAS de encontro.
 *
 * <p>A falha que ele pega e de omissao e nao da erro: uma criatura registrada,
 * com perfil, loot, tradução, voz e corpo -- e sem receita de encontro. Como ela
 * e {@code ENCOUNTER_ONLY}, o mundo nunca a coloca em lugar nenhum, e o unico
 * sintoma e que ela nao existe. Nenhum outro portao percebe: todos os que a
 * varrem so conferem que ela esta completa.</p>
 */
class EncounterBlueprintsTest {

    private static final net.minecraft.resources.ResourceLocation ALGUM_TIPO =
            net.minecraft.resources.ResourceLocation
                    .fromNamespaceAndPath("nenfoundation", "cyclops");

    @Test
    @DisplayName("PORTAO: toda criatura de Greed Island tem receita de encontro")
    void todaCriaturaDaIlhaTemReceita() {
        Set<String> semReceita = new TreeSet<>();
        for (String id : GreedIslandProfiles.publicados().keySet()) {
            if (EncounterBlueprints.de("nenfoundation:" + id).isEmpty()) semReceita.add(id);
        }
        assertTrue(semReceita.isEmpty(),
                "Criaturas de Greed Island sem receita de encontro: " + semReceita + ". Elas sao"
                        + " ENCOUNTER_ONLY: sem receita, o mundo nunca as coloca em lugar nenhum,"
                        + " e o unico sintoma e que elas NAO EXISTEM. Os outros portoes nao"
                        + " percebem -- eles so conferem que a ficha esta completa.");
    }

    @Test
    @DisplayName("PORTAO: nenhuma receita aponta para um id que ninguem publica")
    void nenhumaReceitaOrfa() {
        Set<String> publicados = new TreeSet<>(GreedIslandProfiles.publicados().keySet());
        publicados.add("dummy_enemy");

        Set<String> orfas = new TreeSet<>();
        EncounterBlueprints.todas().keySet().forEach(id -> {
            if (!publicados.contains(id.getPath())) orfas.add(id.toString());
        });
        assertTrue(orfas.isEmpty(),
                "Receitas apontando para criaturas que nao existem: " + orfas + ". Elas nunca"
                        + " disparam, e o arquivo fica parecendo que cobre mais do que cobre.");
    }

    @Test
    @DisplayName("a quantidade e parte da IDENTIDADE do encontro")
    void quantidadeEIdentidade() {
        // Quatro lobos sao uma matilha; um lobo e um lobo. Mudar este numero nao
        // deixa o encontro mais dificil -- deixa OUTRO encontro no lugar dele.
        assertEquals(4, EncounterBlueprints.de("nenfoundation:wolf_pack_hunter")
                .orElseThrow().quantidade(),
                "A matilha deixou de ser matilha.");
        assertEquals(1, EncounterBlueprints.de("nenfoundation:cyclops").orElseThrow().quantidade(),
                "Dois ciclopes nao sao um encontro mais dificil: sao outro encontro.");
        assertTrue(EncounterBlueprints.de("nenfoundation:hyper_puffball")
                .orElseThrow().quantidade() > 1,
                "Um puffball sozinho nao ensina que eles estouram em cadeia.");
    }

    @Test
    @DisplayName("receita sem espalhamento ou sem quantidade reprova")
    void receitaImpossivelReprova() {
        IllegalArgumentException semEspaco = assertThrows(IllegalArgumentException.class,
                () -> new EncounterBlueprints.Receita(ALGUM_TIPO, 2, 0.0D));
        assertTrue(semEspaco.getMessage().contains("empurrao"),
                "Empilhados no mesmo bloco, os bichos sao arremessados pela colisao -- e isso"
                        + " le como bug, e nao como chegada.");
        assertThrows(IllegalArgumentException.class,
                () -> new EncounterBlueprints.Receita(ALGUM_TIPO, 0, 3.0D));
    }

    @Test
    @DisplayName("id malformado devolve vazio em vez de estourar")
    void idMalformadoNaoEstoura() {
        assertTrue(EncounterBlueprints.de("isto nao e um id").isEmpty(),
                "Um id torto vindo de save ou de datapack nao pode derrubar o tick do"
                        + " servidor: vazio significa 'o episodio nao comecou', que e a"
                        + " resposta recuperavel.");
        assertFalse(EncounterBlueprints.todas().isEmpty(),
                "Nenhuma receita: a varredura deste portao olharia para o vazio.");
    }
}
