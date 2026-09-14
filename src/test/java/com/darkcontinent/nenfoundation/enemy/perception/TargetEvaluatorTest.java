package com.darkcontinent.nenfoundation.enemy.perception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.FactionRelation;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da escolha de alvo por FACCAO (issue #111).
 *
 * <p>A falha que ele cobre e de omissao: um par de faccoes que ninguem declarou.
 * Se o padrao fosse HOSTILE, esquecer uma linha transformaria fauna em massacre;
 * o padrao e NEUTRAL, e o preco -- um inimigo passivo demais -- esta declarado no
 * javadoc de {@link TargetEvaluator} e cobrado aqui.</p>
 */
class TargetEvaluatorTest {

    private static final UUID A = UUID.nameUUIDFromBytes("a".getBytes());
    private static final UUID B = UUID.nameUUIDFromBytes("b".getBytes());
    private static final UUID C = UUID.nameUUIDFromBytes("c".getBytes());

    private static TargetCandidate candidato(UUID id, EnemyFaction faccao, double distancia,
            boolean territorio, boolean feriu) {
        return new TargetCandidate(id, faccao, distancia, 1.0D, true, true, territorio, feriu);
    }

    private static TargetEvaluator avaliador(EnemyFaction minha, boolean territorial) {
        return new TargetEvaluator(FactionRelations.padrao(), minha, 32.0D, territorial);
    }

    @Test
    @DisplayName("relacao ausente e NEUTRAL, e neutro nao vira alvo sem territorio")
    void relacaoAusenteNaoInventaHostilidade() {
        TargetEvaluator semTerritorio = avaliador(EnemyFaction.WILDLIFE, false);
        assertEquals(FactionRelation.NEUTRAL,
                semTerritorio.relacaoCom(EnemyFaction.HUNTER_ASSOCIATION));
        assertFalse(semTerritorio.elegivel(candidato(A, EnemyFaction.HUNTER_ASSOCIATION, 5, false, false)),
                "Um par nao declarado nao pode virar hostil por acidente: seria fauna atacando"
                        + " o mundo inteiro porque alguem esqueceu uma linha.");
    }

    @Test
    @DisplayName("territorio autoriza a resposta, mas NAO torna o mob hostil fora dele")
    void territorioNaoEHostilidade() {
        TargetEvaluator territorial = avaliador(EnemyFaction.WILDLIFE, true);
        assertTrue(territorial.elegivel(candidato(A, EnemyFaction.CIVILIAN, 5, true, false)));
        assertFalse(territorial.elegivel(candidato(A, EnemyFaction.CIVILIAN, 5, false, false)),
                "Fora do territorio nada acontece -- e exatamente isso que o mob ensina.");
    }

    @Test
    @DisplayName("quem feriu o mob passa na frente de quem esta mais perto")
    void agressaoTemPrioridadeSobreDistancia() {
        TargetEvaluator territorial = avaliador(EnemyFaction.WILDLIFE, true);
        List<TargetCandidate> candidatos = List.of(
                candidato(A, EnemyFaction.CIVILIAN, 2.0D, true, false),
                candidato(B, EnemyFaction.CIVILIAN, 20.0D, false, true));
        assertEquals(B, territorial.melhor(candidatos).orElseThrow().id(),
                "Largar quem esta batendo para perseguir um passante mais perto nao da erro --"
                        + " da uma IA que parece burra, e ninguem abre bug sobre isso.");
    }

    @Test
    @DisplayName("hostil vem antes de presa, e presa antes de neutro")
    void ordemDePrioridadeEntreRelacoes() {
        TargetEvaluator formiga = avaliador(EnemyFaction.CHIMERA_ANT, false);
        List<TargetCandidate> candidatos = List.of(
                candidato(A, EnemyFaction.CIVILIAN, 4.0D, false, false),
                candidato(B, EnemyFaction.HUNTER_ASSOCIATION, 20.0D, false, false));
        assertEquals(FactionRelation.PREY, formiga.relacaoCom(EnemyFaction.CIVILIAN));
        assertEquals(FactionRelation.HOSTILE, formiga.relacaoCom(EnemyFaction.HUNTER_ASSOCIATION));
        assertEquals(B, formiga.melhor(candidatos).orElseThrow().id());
    }

    @Test
    @DisplayName("dimensao diferente e alcance excedido encerram a candidatura")
    void dimensaoEAlcanceEncerramACandidatura() {
        TargetEvaluator formiga = avaliador(EnemyFaction.CHIMERA_ANT, false);
        TargetCandidate outraDimensao = new TargetCandidate(C, EnemyFaction.CIVILIAN, 3.0D, 1.0D,
                true, false, false, false);
        assertFalse(formiga.elegivel(outraDimensao),
                "Alvo em outra dimensao continua 'perto' em numero, e perseguir um numero e"
                        + " como o mob fica preso olhando para o nada.");
        assertFalse(formiga.elegivel(candidato(C, EnemyFaction.CIVILIAN, 64.0D, false, false)));
    }

    @Test
    @DisplayName("assimetria intencional: a formiga cace o civil, o civil nao e hostil a ela")
    void assimetriaEIntencional() {
        FactionRelations relacoes = FactionRelations.padrao();
        assertEquals(FactionRelation.PREY,
                relacoes.relation(EnemyFaction.CHIMERA_ANT, EnemyFaction.CIVILIAN));
        assertEquals(FactionRelation.NEUTRAL,
                relacoes.relation(EnemyFaction.CIVILIAN, EnemyFaction.CHIMERA_ANT));
    }

    @Test
    @DisplayName("faccao com relacao propria declarada substitui o padrao neutro")
    void relacaoCustomizadaEExtensivelSemSwitch() {
        FactionRelations custom = new FactionRelations(Map.of(
                EnemyFaction.CUSTOM, Map.of(EnemyFaction.WILDLIFE, FactionRelation.HOSTILE)));
        TargetEvaluator avaliador = new TargetEvaluator(custom, EnemyFaction.CUSTOM, 32.0D, false);
        assertTrue(avaliador.elegivel(candidato(A, EnemyFaction.WILDLIFE, 5.0D, false, false)));
    }
}
