package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A regra de altitude, comando e mergulho do Avian Commander.
 *
 * <p>Os numeros daqui sao do TESTE, e nao os de producao: quem prova os de
 * producao e {@code AvianCommanderTuningTest}. Separados, esta bateria continua
 * medindo a REGRA quando alguem girar a ficha do bicho -- juntos, uma mudanca de
 * balanceamento apagaria a prova da regra sem que nada acusasse.</p>
 */
class RegrasDeComandoAereoTest {

    /** comando a 8, mergulho a partir de 8, abandono em 2, subida de 20 ticks, alcance 16. */
    private static RegrasDeComandoAereo regras() {
        return new RegrasDeComandoAereo(8.0D, 8.0D, 2.0D, 20, 16.0D);
    }

    /** Situacao ja "fria": o relogio da subida estourado, sem mergulho em curso. */
    private static SituacaoDeComandoAereo fria(double altura, double distancia, boolean temAlvo) {
        return new SituacaoDeComandoAereo(altura, distancia, temAlvo, false, 999, false);
    }

    // -------------------------------------------------------------- o normal

    @Test
    @DisplayName("no alto, com alvo longe, ela COMANDA")
    void noAltoComandaEmVezDeAtacar() {
        assertEquals(PosturaDeComando.COMANDAR, regras().decidir(fria(10.0D, 30.0D, true)));
    }

    @Test
    @DisplayName("no alto, com alvo ao alcance, ela MERGULHA")
    void doAltoOMergulhoSai() {
        assertEquals(PosturaDeComando.MERGULHAR, regras().decidir(fria(10.0D, 12.0D, true)));
    }

    @Test
    @DisplayName("o mergulho continua ate raspar o chao, e ai vira subida")
    void oMergulhoTerminaSozinho() {
        RegrasDeComandoAereo r = regras();
        // Em curso, ainda alto: continua descendo.
        assertEquals(PosturaDeComando.MERGULHAR,
                r.decidir(new SituacaoDeComandoAereo(5.0D, 3.0D, true, true, 999, false)));
        // Em curso, ja embaixo: a descida acabou.
        assertEquals(PosturaDeComando.RECOLHER,
                r.decidir(new SituacaoDeComandoAereo(2.0D, 1.0D, true, true, 999, false)));
        assertTrue(r.mergulhoTerminou(2.0D),
                "A mesma comparacao tem de responder igual nos dois lados: se decidir() diz que"
                        + " acabou e mergulhoTerminou() diz que nao, o consumidor nunca zera o"
                        + " relogio da subida e a comandante fica em RECOLHER para sempre.");
    }

    @Test
    @DisplayName("a janela de subida vence um alvo colado -- ela nao emenda mergulhos")
    void aJanelaDeSubidaVemAntesDoAlvo() {
        // Alta, alvo dentro do alcance: seria mergulho, se nao fosse o relogio.
        assertEquals(PosturaDeComando.RECOLHER,
                regras().decidir(new SituacaoDeComandoAereo(10.0D, 2.0D, true, false, 19, false)));
        assertEquals(PosturaDeComando.MERGULHAR,
                regras().decidir(new SituacaoDeComandoAereo(10.0D, 2.0D, true, false, 20, false)));
    }

    // ------------------------------------------------------------ a recusa

    @Test
    @DisplayName("embaixo ela SOBE, e nao comanda -- e o que o jogador aprende a forcar")
    void embaixoElaNaoComanda() {
        PosturaDeComando postura = regras().decidir(fria(1.0D, 30.0D, true));
        assertEquals(PosturaDeComando.SUBIR, postura);
        assertTrue(!postura.comanda(),
                "Uma comandante no chao que continua comandando apaga a unica tatica que o"
                        + " encontro ensina, e nada no jogo acusa isso.");
    }

    @Test
    @DisplayName("embaixo e com alvo colado ela AINDA sobe: nao ha mergulho de meia altura")
    void naoHaMergulhoDeMeiaAltura() {
        assertEquals(PosturaDeComando.SUBIR, regras().decidir(fria(3.0D, 1.0D, true)));
    }

    @Test
    @DisplayName("cambaleando ela RECOLHE: interromper a comandante cala as ordens")
    void cambalearCalaOComando() {
        PosturaDeComando postura = regras().decidir(
                new SituacaoDeComandoAereo(10.0D, 2.0D, true, true, 999, true));
        assertEquals(PosturaDeComando.RECOLHER, postura);
        assertTrue(!postura.comanda(),
                "Se cambalear nao calasse o comando, acertar nela nao mudaria nada no bando e o"
                        + " jogador aprenderia a ignorar o unico mob em que interromper importa.");
    }

    @Test
    @DisplayName("sem alvo, ela fica no posto de comando em vez de mergulhar no vazio")
    void semAlvoNaoHaMergulho() {
        assertEquals(PosturaDeComando.COMANDAR, regras().decidir(fria(10.0D, 0.0D, false)));
    }

    // ------------------- os casos que DEVEM reprovar: a regua mordendo -----

    @Test
    @DisplayName("REPROVA: mergulho que abandona na altitude de comando -- ela nunca desce")
    void abandonoNaAltitudeDeComandoEhRecusado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeComandoAereo(8.0D, 8.0D, 8.0D, 20, 16.0D));
        assertTrue(erro.getMessage().contains("nunca desceria"),
                "A recusa tem de dizer O QUE acontece em jogo, e nao so que o numero e invalido."
                        + " Mensagem recebida: " + erro.getMessage());
    }

    @Test
    @DisplayName("REPROVA: mergulho que comeca abaixo da altitude de comando")
    void mergulhoDeMeiaAlturaEhRecusado() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeComandoAereo(8.0D, 4.0D, 2.0D, 20, 16.0D));
    }

    @Test
    @DisplayName("REPROVA: zero tick de subida -- o mergulho sairia de graca")
    void mergulhoSemPrecoEhRecusado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeComandoAereo(8.0D, 8.0D, 2.0D, 0, 16.0D));
        assertTrue(erro.getMessage().contains("lucro puro"), erro.getMessage());
    }

    @Test
    @DisplayName("REPROVA: altura NaN, que se acharia no alto para sempre")
    void alturaQuebradaEhRecusada() {
        assertThrows(IllegalArgumentException.class,
                () -> new SituacaoDeComandoAereo(Double.NaN, 1.0D, true, false, 99, false));
        assertThrows(IllegalArgumentException.class,
                () -> new SituacaoDeComandoAereo(-1.0D, 1.0D, true, false, 99, false));
    }

    @Test
    @DisplayName("REPROVA: relogio de subida negativo, que prende a comandante em RECOLHER")
    void relogioNegativoEhRecusado() {
        assertThrows(IllegalArgumentException.class,
                () -> new SituacaoDeComandoAereo(9.0D, 1.0D, true, false, -1, false));
    }

    @Test
    @DisplayName("REPROVA: mergulho em curso sem alvo -- os dois campos discordam")
    void mergulhoSemAlvoEhRecusado() {
        assertThrows(IllegalArgumentException.class,
                () -> new SituacaoDeComandoAereo(9.0D, 0.0D, false, true, 99, false));
    }

    @Test
    @DisplayName("a ORDEM das perguntas e a regra: o relogio vence o alvo, e o stagger vence tudo")
    void aOrdemDasPerguntasNaoEhArbitraria() {
        RegrasDeComandoAereo r = regras();
        // Com a janela aberta e o alvo ao alcance, so o stagger separa RECOLHER de
        // MERGULHAR. Se algum dia a checagem de stagger for movida para depois do
        // alvo, este caso passa a devolver MERGULHAR e o teste morde.
        assertNotEquals(
                r.decidir(new SituacaoDeComandoAereo(10.0D, 2.0D, true, false, 999, true)),
                r.decidir(new SituacaoDeComandoAereo(10.0D, 2.0D, true, false, 999, false)),
                "Cambalear e nao cambalear tem de dar posturas DIFERENTES com tudo o mais igual;"
                        + " iguais, o stagger virou decoracao e nada no jogo diria isso.");
    }
}
