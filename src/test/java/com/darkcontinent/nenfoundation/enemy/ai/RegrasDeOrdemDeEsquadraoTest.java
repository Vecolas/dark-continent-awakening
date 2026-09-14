package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Quando a comandante manda, o que ela manda, e por que ela foi recusada.
 *
 * <p>A parte mais util desta bateria e a das RECUSAS. Uma ordem que nao sai
 * parece, de fora, igual a uma ordem que nao era necessaria -- as duas dao
 * silencio. O que separa uma da outra e o motivo, e e por isso que ele e um
 * valor e nao um {@code Optional} vazio.</p>
 */
class RegrasDeOrdemDeEsquadraoTest {

    private static RegrasDeOrdemDeEsquadrao regras() {
        return new RegrasDeOrdemDeEsquadrao(SquadRules.esquadrao(), 3);
    }

    private static SituacaoDeOrdem situacao(boolean lidera, boolean noOrcamento,
            PosturaDeComando postura, boolean inedito, boolean espalhado, int membros) {
        return new SituacaoDeOrdem(lidera, membros > 0, noOrcamento, postura, inedito,
                espalhado, membros);
    }

    // -------------------------------------------------------------- o normal

    @Test
    @DisplayName("do alto, no orcamento, com alvo inedito: ela TROCA o alvo do bando")
    void oAlvoQueSoElaViuViraOAlvoDoBando() {
        assertEquals(OrdemDoComandante.TROCAR_ALVO, regras().decidir(
                situacao(true, true, PosturaDeComando.COMANDAR, true, true, 4)));
    }

    @Test
    @DisplayName("sem alvo novo e com o bando espalhado, ela manda REAGRUPAR")
    void bandoEspalhadoRecebeReagrupar() {
        assertEquals(OrdemDoComandante.REAGRUPAR, regras().decidir(
                situacao(true, true, PosturaDeComando.COMANDAR, false, true, 4)));
    }

    @Test
    @DisplayName("bando junto e sem alvo novo: silencio com o bando ok, e nao recusa")
    void bandoJuntoNaoRecebeOrdemRepetida() {
        OrdemDoComandante ordem = regras().decidir(
                situacao(true, true, PosturaDeComando.COMANDAR, false, false, 4));
        assertEquals(OrdemDoComandante.NADA_A_ORDENAR, ordem);
        assertFalse(ordem.saiuOrdem());
        assertFalse(ordem.recusada(),
                "'Nada a ordenar' NAO e recusa: confundir as duas faria um diagnostico ler"
                        + " 'a comandante esta bloqueada' onde o bando so estava em ordem.");
    }

    @Test
    @DisplayName("trocar o alvo vence reagrupar: e a unica ordem que so ela pode dar")
    void trocarAlvoVenceReagrupar() {
        assertEquals(OrdemDoComandante.TROCAR_ALVO, regras().decidir(
                situacao(true, true, PosturaDeComando.COMANDAR, true, true, 8)));
    }

    // ------------------------------------------------------------- a recusa

    @Test
    @DisplayName("fora da altitude de comando a recusa e BAIXA_DEMAIS, e nao FORA_DO_ORCAMENTO")
    void aAltitudeVemAntesDoRelogio() {
        for (PosturaDeComando postura : PosturaDeComando.values()) {
            if (postura.comanda()) continue;
            // Note o FALSE no orcamento: as duas recusas cabem, e a que tem de sair e
            // a da altitude. Se o orcamento vencesse, quem fosse diagnosticar "por que
            // o esquadrao parou de se reorganizar" leria o relogio em vez da altura, e
            // procuraria o defeito no lugar errado.
            assertEquals(OrdemDoComandante.BAIXA_DEMAIS, regras().decidir(
                    situacao(true, false, postura, true, true, 4)),
                    "postura " + postura);
        }
    }

    @Test
    @DisplayName("fora do tick de coordenacao nao sai ordem, mesmo la do alto")
    void aOrdemSaiNoOrcamento() {
        assertEquals(OrdemDoComandante.FORA_DO_ORCAMENTO, regras().decidir(
                situacao(true, false, PosturaDeComando.COMANDAR, true, true, 4)));
    }

    @Test
    @DisplayName("quem nao lidera nao manda, nem do alto nem no orcamento")
    void quemNaoLideraNaoManda() {
        assertEquals(OrdemDoComandante.NAO_LIDERA, regras().decidir(
                situacao(false, true, PosturaDeComando.COMANDAR, true, true, 4)));
    }

    @Test
    @DisplayName("sem bando nao ha o que ordenar -- e a recusa e alcancavel")
    void semBandoNaoHaOrdem() {
        assertEquals(OrdemDoComandante.SEM_BANDO, regras().decidir(
                new SituacaoDeOrdem(false, false, true, PosturaDeComando.COMANDAR,
                        true, false, 0)));
        // Bando que existe no indice e esta vazio: a inconsistencia que a faxina de
        // SquadRegistry existe para fechar. A recusa aqui e o que impede a comandante
        // de escrever alvo num bando que nao tem ninguem para receber.
        assertEquals(OrdemDoComandante.SEM_BANDO, regras().decidir(
                new SituacaoDeOrdem(true, true, true, PosturaDeComando.COMANDAR,
                        true, false, 0)));
    }

    @Test
    @DisplayName("bando pequeno demais nao recebe reagrupar: a ordem nao mudaria nada")
    void bandoPequenoNaoReagrupa() {
        assertEquals(OrdemDoComandante.NADA_A_ORDENAR, regras().decidir(
                situacao(true, true, PosturaDeComando.COMANDAR, false, true, 2)));
    }

    @Test
    @DisplayName("o orcamento e o de SquadRules, e nao um numero proprio")
    void oOrcamentoNaoEhRedeclarado() {
        RegrasDeOrdemDeEsquadrao r = regras();
        SquadRules bando = SquadRules.esquadrao();
        assertEquals(bando, r.bando());
        int coordenacoes = 0;
        for (int tick = 0; tick < 100; tick++) {
            if (r.noOrcamento(tick, 0)) coordenacoes++;
        }
        assertEquals(100 / bando.ticksDeAtualizacao(), coordenacoes,
                "A contagem tem de bater com ticksDeAtualizacao de SquadRules. Um numero proprio"
                        + " aqui venceria em metade dos caminhos e o de la na outra metade, e a"
                        + " sessao de balanceamento giraria um botao morto.");
    }

    // ------------------- os casos que DEVEM reprovar: a regua mordendo -----

    @Test
    @DisplayName("REPROVA: reagrupar um bando de um, que e ordem que nao muda nada")
    void reagruparUmBandoDeUmEhRecusado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeOrdemDeEsquadrao(SquadRules.esquadrao(), 1));
        assertTrue(erro.getMessage().contains("nao muda nada"), erro.getMessage());
    }

    @Test
    @DisplayName("REPROVA: minimo de reagrupamento acima do teto -- a ordem nunca sairia")
    void minimoAcimaDoTetoEhRecusado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeOrdemDeEsquadrao(SquadRules.esquadrao(),
                        SquadRules.esquadrao().maximoDeMembros() + 1));
        assertTrue(erro.getMessage().contains("nunca alcanca"), erro.getMessage());
    }

    @Test
    @DisplayName("REPROVA: lider de bando nenhum, que e a entrada morta de indice")
    void liderSemBandoEhRecusado() {
        assertThrows(IllegalArgumentException.class,
                () -> new SituacaoDeOrdem(true, false, true, PosturaDeComando.COMANDAR,
                        false, false, 0));
    }

    @Test
    @DisplayName("REPROVA: membros sem bando -- os dois campos discordam")
    void membrosSemBandoEhRecusado() {
        assertThrows(IllegalArgumentException.class,
                () -> new SituacaoDeOrdem(false, false, true, PosturaDeComando.COMANDAR,
                        false, false, 3));
    }

    @Test
    @DisplayName("REPROVA: postura nula, que estouraria no meio do tick do servidor")
    void posturaNulaEhRecusada() {
        assertThrows(NullPointerException.class,
                () -> new SituacaoDeOrdem(true, true, true, null, false, false, 2));
    }
}
