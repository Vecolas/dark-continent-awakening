package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do relogio de animacao sob {@code /nenvfx freeze} (AV2, #181).
 *
 * <p>POR QUE ELE EXISTE. O congelamento parava a INTERPOLACAO DE ESTADO -- modo,
 * intensidade, distribuicao --, e o javadoc dele prometia mais do que entregava:
 * <i>"dois quadros seguidos da mesma coisa so se comparam se a coisa parou de
 * mudar entre eles"</i>. O {@code tempo} que move o FLUXO do shader e o CICLO
 * dos filamentos continuava correndo.
 *
 * <p><b>O AV2 cobra exatamente isso.</b> Duas das treze verificacoes dele --
 * {@code av2_frame_congelado_1} e {@code _2} -- <b>tem de ser identicas</b>, e
 * o {@code LEIA-ME} do gate diz o que uma divergencia significaria: <i>"a curva
 * do filamento esta lendo algo que muda entre quadros, e o defeito aparece em
 * jogo como cintilacao que ninguem consegue reproduzir"</i>.
 *
 * <p>Sem esta correcao a verificacao reprovaria <b>sem haver defeito na
 * curva</b>, e alguem gastaria uma sessao cacando um bug inexistente enquanto a
 * causa real era o congelamento nao alcancar o relogio.
 *
 * <p><b>PONTO CEGO DECLARADO.</b> Ele prova que o NUMERO fica parado. Nao prova
 * que a TELA fica parada: qualquer outra fonte de variacao por quadro -- uma
 * textura animada, um uniforme de shader lido de outro lugar, o proprio
 * {@code partialTick} chegando a um caminho que nao passa por aqui -- continua
 * invisivel para esta regua. Isso e do AV2, e do olho.
 */
class RelogioCongeladoTest {

    @BeforeEach
    @AfterEach
    void limpar() {
        // A SOBREPOSICAO E ESTATICA, e um teste que a deixe ligada contamina o
        // proximo -- que passaria ou reprovaria por causa deste.
        SobreposicaoDeVfx.limpar();
    }

    @Test
    @DisplayName("sem congelar, o tempo ANDA com a idade da entidade")
    void semCongelarOTempoAnda() {
        float antes = SobreposicaoDeVfx.tempoDeAnimacao(100.0F);
        float depois = SobreposicaoDeVfx.tempoDeAnimacao(140.0F);

        assertNotEquals(antes, depois,
                "O tempo parou sem ninguem ter congelado nada. O fluxo e o ciclo dos filamentos"
                        + " ficariam imoveis no jogo normal.");
        assertEquals(5.0F, antes, 1.0E-4F, "100 ticks deveriam dar 5 segundos.");
    }

    @Test
    @DisplayName("congelado, DOIS QUADROS do mesmo tick dao o MESMO tempo")
    void doisQuadrosDoMesmoTickSaoIguais() {
        SobreposicaoDeVfx.congelar(true);

        // O mesmo tick, dois quadros: `idadeEmTicks` traz o partialTick, que
        // muda DENTRO do tick. Era isto que fazia dois quadros diferirem.
        float quadroA = SobreposicaoDeVfx.tempoDeAnimacao(200.10F);
        float quadroB = SobreposicaoDeVfx.tempoDeAnimacao(200.85F);

        assertEquals(quadroA, quadroB, 1.0E-6F,
                "Dois quadros do MESMO tick deram tempos diferentes. O partialTick vazou para o"
                        + " relogio, e uma comparacao byte a byte das duas capturas do AV2"
                        + " falharia por uma diferenca que nao e da curva.");
    }

    @Test
    @DisplayName("congelado, o tempo nao anda nem depois de TICKS passarem")
    void congeladoOTempoNaoAnda() {
        SobreposicaoDeVfx.congelar(true);
        float noCongelamento = SobreposicaoDeVfx.tempoDeAnimacao(200.0F);

        // Dez ticks depois: `idadeEmTicks` subiu 10, e o relogio do congelamento
        // tambem -- o desconto mantem o valor.
        for (int i = 0; i < 10; i++) {
            SobreposicaoDeVfx.aoTick();
        }
        float dezTicksDepois = SobreposicaoDeVfx.tempoDeAnimacao(210.0F);

        assertEquals(noCongelamento, dezTicksDepois, 1.0E-4F,
                "O tempo andou durante o congelamento. O fluxo do shader e o ciclo dos"
                        + " filamentos continuariam se movendo, e as duas capturas do AV2"
                        + " sairiam diferentes.");
    }

    @Test
    @DisplayName("a FASE de cada entidade e preservada -- o congelamento nao as sincroniza")
    void aFaseDeCadaEntidadeSobrevive() {
        // Duas entidades nascidas em momentos diferentes: fases diferentes.
        float faseA = SobreposicaoDeVfx.tempoDeAnimacao(200.0F);
        float faseB = SobreposicaoDeVfx.tempoDeAnimacao(233.0F);
        float distanciaAntes = faseB - faseA;

        SobreposicaoDeVfx.congelar(true);
        for (int i = 0; i < 5; i++) {
            SobreposicaoDeVfx.aoTick();
        }
        float congeladaA = SobreposicaoDeVfx.tempoDeAnimacao(205.0F);
        float congeladaB = SobreposicaoDeVfx.tempoDeAnimacao(238.0F);

        assertEquals(distanciaAntes, congeladaB - congeladaA, 1.0E-4F,
                "O congelamento aproximou as fases. Prender um valor GLOBAL faria todos os"
                        + " jogadores compartilharem a mesma fase do fluxo -- e a fase por"
                        + " entidade e deliberada: sincronia acidental e a coisa mais artificial"
                        + " que um efeito organico pode fazer.");
    }

    @Test
    @DisplayName("descongelar devolve o tempo ao lugar, sem salto para tras")
    void descongelarNaoSalta() {
        SobreposicaoDeVfx.congelar(true);
        for (int i = 0; i < 40; i++) {
            SobreposicaoDeVfx.aoTick();
        }
        SobreposicaoDeVfx.congelar(false);

        assertEquals(12.0F, SobreposicaoDeVfx.tempoDeAnimacao(240.0F), 1.0E-4F,
                "Depois de descongelar o tempo nao voltou a ser a idade da entidade.");
    }

    @Test
    @DisplayName("congelar DUAS vezes nao desconta o congelamento anterior")
    void congelarDuasVezesNaoAcumula() {
        SobreposicaoDeVfx.congelar(true);
        for (int i = 0; i < 30; i++) {
            SobreposicaoDeVfx.aoTick();
        }
        SobreposicaoDeVfx.congelar(false);

        SobreposicaoDeVfx.congelar(true);
        float logoApos = SobreposicaoDeVfx.tempoDeAnimacao(300.0F);

        assertTrue(logoApos > 14.0F,
                "O segundo congelamento descontou o tempo do primeiro (" + logoApos + "s para"
                        + " uma entidade de 15s). A aura saltaria para tras ao congelar de novo"
                        + " -- e numa sessao de captura isso e uma imagem que nao corresponde a"
                        + " nada.");
        assertEquals(15.0F, logoApos, 1.0E-4F, "o contador nao zerou ao ligar de novo");
    }
}
