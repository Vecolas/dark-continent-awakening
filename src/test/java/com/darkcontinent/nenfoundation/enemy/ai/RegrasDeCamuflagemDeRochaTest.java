package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sustenta a camuflagem do Melanin Lizard sem servidor de pe.
 *
 * <p>As quatro coisas que esta regra decide -- quem observa, quando quebra,
 * quando ele pode se mexer e quando volta a ser pedra -- nao produzem erro
 * nenhum quando saem erradas. Elas produzem um bicho que se arrasta enquanto o
 * servidor recusa a mira dele, ou uma pedra que quebra por qualquer um que
 * passe, ou uma que some com um jogador preso na boca. Nada disso aparece no
 * log, e nada disso aparece num teste de mundo: aparece na tela de quem joga.</p>
 */
class RegrasDeCamuflagemDeRochaTest {

    /** As regras reais do mob, escritas aqui para o teste nao depender do tuning. */
    private static final RegrasDeCamuflagemDeRocha REGRAS =
            new RegrasDeCamuflagemDeRocha(4.5D, 0.55D, 120);

    // ------------------------------------------------------------ o caso normal

    @Test
    @DisplayName("perto e olhando em cheio: a pedra acorda")
    void olharDePertoQuebra() {
        assertTrue(REGRAS.observado(true, 3.0D, 0.9D));
        assertTrue(REGRAS.quebra(true, REGRAS.observado(true, 3.0D, 0.9D), false),
                "Encarar o bicho de perto e a UNICA pista que o jogador tem para aprender."
                        + " Sem a quebra, a camuflagem nunca deixaria de valer e o mob seria"
                        + " decoracao permanente.");
    }

    @Test
    @DisplayName("dano acorda a pedra mesmo sem ninguem olhando")
    void danoQuebraSemObservador() {
        assertTrue(REGRAS.quebra(true, false, true),
                "Quem acertou o lagarto de longe ja provou que ele nao e pedra. Sem esta saida"
                        + " ele continuaria 'invisivel' para a propria IA e apanharia parado ate"
                        + " morrer -- que nao e camuflagem, e paralisia.");
    }

    // ---------------------------------------------------------- os casos recusados

    @Test
    @DisplayName("perto, mas de costas: NAO quebra")
    void olharParaOutroLadoNaoQuebra() {
        assertFalse(REGRAS.observado(true, 2.0D, -0.9D));
        assertFalse(REGRAS.observado(true, 2.0D, 0.2D),
                "0.2 e olhar de esguelha. Se isso contasse como encarar, passar ao lado da pedra"
                        + " ja a acordaria, e a mecanica inteira viraria um mob que agride quem"
                        + " passa perto.");
        assertFalse(REGRAS.quebra(true, false, false));
    }

    @Test
    @DisplayName("olhando em cheio, mas longe: NAO quebra")
    void olharDeLongeNaoQuebra() {
        assertFalse(REGRAS.observado(true, 9.0D, 1.0D));
    }

    @Test
    @DisplayName("sem linha de visao nao ha observacao, por mais perto que esteja")
    void semLinhaDeVisaoNaoQuebra() {
        assertFalse(REGRAS.observado(false, 0.5D, 1.0D),
                "Quem esta do outro lado de uma parede nao viu nada. Ignorar a visibilidade"
                        + " faria a pedra acordar com alguem passando no corredor de baixo.");
    }

    @Test
    @DisplayName("medida nao finita NAO observa -- NaN nao pode abrir a emboscada de graca")
    void naoFinitoNaoObserva() {
        assertFalse(REGRAS.observado(true, Double.NaN, 1.0D));
        assertFalse(REGRAS.observado(true, 1.0D, Double.NaN),
                "Vetor degenerado (observador exatamente em cima do bicho) produz NaN, e toda"
                        + " comparacao com NaN e falsa. Deixar o NaN escapar para o lado do 'sim'"
                        + " derrubaria a camuflagem justamente no caso em que o jogador encostou"
                        + " na pedra sem perceber nada.");
        assertFalse(REGRAS.observado(true, Double.POSITIVE_INFINITY, 1.0D));
    }

    @Test
    @DisplayName("ja revelado, nao ha o que quebrar")
    void reveladoNaoQuebraDeNovo() {
        assertFalse(REGRAS.quebra(false, true, true));
    }

    // ------------------------------------------------------ as duas metades da regra

    @Test
    @DisplayName("camuflado ele nao se move E nao e alvo -- as duas juntas, sempre")
    void camufladoNaoSeMexeENaoEAlvo() {
        assertFalse(REGRAS.podeMover(true));
        assertFalse(REGRAS.podeSerAlvo(true));
        assertTrue(REGRAS.podeMover(false));
        assertTrue(REGRAS.podeSerAlvo(false));
        // Elas moram juntas porque os dois estados incoerentes possiveis -- parado e
        // mirable, ou andando e imune a mira -- nao dao erro nenhum, e o segundo o
        // jogador le como trapaca.
    }

    // ------------------------------------------------------------ recamuflagem

    @Test
    @DisplayName("volta a ser pedra so depois do prazo inteiro sem alvo")
    void recamuflaSoDepoisDoPrazo() {
        assertFalse(REGRAS.recamufla(false, false, false, 119));
        assertTrue(REGRAS.recamufla(false, false, false, 120));
    }

    @Test
    @DisplayName("com alvo vivo ele nao volta a ser pedra")
    void comAlvoNaoRecamufla() {
        assertFalse(REGRAS.recamufla(false, true, false, 10_000));
    }

    @Test
    @DisplayName("segurando alguem ele NUNCA recamufla")
    void agarrandoNaoRecamufla() {
        assertFalse(REGRAS.recamufla(false, false, true, 10_000),
                "Um lagarto que vira pedra com alguem preso some da mira de quem ia salvar a"
                        + " vitima, e a vitima fica presa a uma pedra. Nao da erro nenhum e e o"
                        + " pior relato de bug que este mob consegue produzir.");
    }

    @Test
    @DisplayName("ja camuflado nao recamufla")
    void jaCamufladoNaoRecamufla() {
        assertFalse(REGRAS.recamufla(true, false, false, 10_000));
    }

    // ------------------------------------------------- o que DEVE ser reprovado

    @Test
    @DisplayName("cosseno zero ou negativo reprova: seria camuflagem que nao esconde nada")
    void cossenoQueNaoEscondeReprova() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeCamuflagemDeRocha(4.5D, 0.0D, 120));
        assertTrue(erro.getMessage().contains("camuflagem"),
                "A recusa tem de dizer o que acontece em jogo, e nao so que o numero e invalido.");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeCamuflagemDeRocha(4.5D, -1.0D, 120));
    }

    @Test
    @DisplayName("cosseno acima de 1 reprova: nenhum olhar jamais alcancaria o limiar")
    void cossenoImpossivelReprova() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeCamuflagemDeRocha(4.5D, 1.5D, 120));
    }

    @Test
    @DisplayName("distancia e prazo invalidos reprovam")
    void geometriaInvalidaReprova() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeCamuflagemDeRocha(0.0D, 0.55D, 120));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeCamuflagemDeRocha(Double.NaN, 0.55D, 120));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeCamuflagemDeRocha(4.5D, 0.55D, 0),
                "Zero tick para recamuflar e uma pedra que pisca, e o jogador le isso como bug"
                        + " de renderizacao e nao como mecanica.");
    }

    @Test
    @DisplayName("a regua morde: com o cosseno do mob, olhar de esguelha nao acorda a pedra")
    void aReguaQueMedeARegua() {
        // Este caso existe para a bateria acima nao virar carimbo. Se alguem afrouxar
        // o cosseno para 0.05 "para o mob reagir melhor", o teste acima de 'de esguelha
        // nao quebra' passaria a medir uma regra que nao esconde mais ninguem -- entao
        // o numero do mob e conferido aqui, e nao so o comportamento dele.
        assertEquals(0.55D, REGRAS.cossenoDeObservacao(),
                "0.55 e ~57 graus: 'esta virado para ca', e nao 'passou o olho'.");
        assertTrue(REGRAS.distanciaDeQuebra() > 2.0D,
                "A distancia de quebra tem de ser MAIOR que o alcance do bote (2.0), senao o"
                        + " lagarto acorda ja dentro do alcance e o telegrafo nao tem espaco"
                        + " para acontecer -- o jogador perde o controle sem ter tido o que ler.");
    }
}
