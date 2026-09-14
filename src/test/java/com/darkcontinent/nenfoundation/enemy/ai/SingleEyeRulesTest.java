package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.perception.VisionCone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sustenta que um bicho de UM olho tem flanco cego, e que o flanco cego CUSTA.
 *
 * <p>As tres coisas que este portao segura nao levantam excecao em lugar nenhum
 * do jogo: um cone largo demais (que apaga o ponto cego), um arco de olho mais
 * largo que o arco de visao (que paga critico a quem o bicho nao enxerga) e um
 * olho na metade de baixo do corpo (que faz o modelo desenhado discordar da
 * regra). Todas as tres saem verdes em compilacao, em spawn e em jogo -- e todas
 * apagam o encontro.</p>
 */
class SingleEyeRulesTest {

    /** As regras do Cyclops, em graus, para nao depender do pacote de conteudo. */
    private static final SingleEyeRules OLHO = SingleEyeRules.deGraus(60.0D, 35.0D, 0.84D);

    /**
     * Meia-abertura de um mob COMUM deste repositorio, em graus.
     *
     * <p>Copiada de {@code DummyEnemyEntity.ABERTURA_DA_VISAO}, e ela esta aqui
     * como REGUA DE COMPARACAO, nao como configuracao: o valor do cone estreito e
     * relativo, e "60 graus" so quer dizer alguma coisa ao lado dos 75 que o
     * resto do bestiario usa.</p>
     */
    private static final double MEIA_ABERTURA_COMUM_EM_GRAUS = 75.0D;

    /** Cosseno do olhar para um alvo a N graus do eixo -- 1 de frente, -1 atras. */
    private static double aGraus(double graus) {
        return Math.cos(Math.toRadians(graus));
    }

    @Test
    @DisplayName("de frente ele enxerga, e o olho so fica exposto num arco mais estreito")
    void oCasoNormal() {
        assertTrue(OLHO.enxerga(aGraus(0.0D)), "bem de frente ele enxerga");
        assertTrue(OLHO.enxerga(aGraus(50.0D)), "50 graus ainda esta dentro dos 60");
        assertTrue(OLHO.enxerga(aGraus(60.0D)), "o limite e inclusivo");

        assertTrue(OLHO.cossenoDoOlho() > OLHO.cossenoDoCampoDeVisao(),
                "cosseno maior e arco mais estreito: o olho tem de ser um alvo mais dificil do que"
                        + " simplesmente estar visivel");
        assertEquals(60.0D, OLHO.meiaAberturaDaVisaoEmGraus(), 1.0E-6D);
        assertEquals(35.0D, OLHO.meiaAberturaDoOlhoEmGraus(), 1.0E-6D);
    }

    @Test
    @DisplayName("o caso RECUSADO: a 70 graus ele nao ve -- e um mob comum veria")
    void oFlancoCegoTemConsequenciaObservavel() {
        double noFlanco = aGraus(70.0D);

        assertFalse(OLHO.enxerga(noFlanco), "70 graus esta FORA dos 60 do olho unico");
        assertTrue(OLHO.noPontoCego(noFlanco), "e por isso ele esta no ponto cego");

        // A MESMA geometria, medida pelo cone de um mob comum. Se este assert
        // deixar de passar, o "cone estreito" parou de ser estreito e o mob
        // continuaria funcionando exatamente igual -- sem erro, sem log, e sem
        // nada para o jogador aprender.
        VisionCone comum = VisionCone.deGraus(32.0D, MEIA_ABERTURA_COMUM_EM_GRAUS);
        assertTrue(comum.enxerga(10.0D, noFlanco, true),
                "um mob de cone normal enxergaria esse mesmo alvo: e essa diferenca que o encontro"
                        + " ensina, e ela precisa existir em NUMERO, nao so em documento");

        assertFalse(OLHO.enxerga(aGraus(180.0D)), "pelas costas, obviamente, nao ve");
        assertTrue(OLHO.noPontoCego(aGraus(60.5D)),
                "meio grau fora do cone ja e ponto cego -- noPontoCego e o complemento ESTRITO de"
                        + " enxerga, e nao uma segunda comparacao escrita a mao");
    }

    @Test
    @DisplayName("geometria nao finita nao enxerga -- o lado seguro e continuar cego")
    void medidaInvalidaNaoAcendeAPercepcao() {
        assertFalse(OLHO.enxerga(Double.NaN));
        assertFalse(OLHO.enxerga(Double.POSITIVE_INFINITY));
        assertFalse(OLHO.enxerga(Double.NEGATIVE_INFINITY));
        assertTrue(OLHO.noPontoCego(Double.NaN),
                "um NaN vindo de um alvo em estado estranho nao pode acender a percepcao de graca");
    }

    @Test
    @DisplayName("O CASO QUE DEVE REPROVAR: olho mais largo que a visao e critico de graca")
    void arcoDoOlhoMaisLargoQueOCampoDeVisaoERecusado() {
        // Olho de 70 graus dentro de uma visao de 60: existe uma faixa de 10 graus
        // em que o jogador cobra multiplicador de um gigante que nao o enxerga.
        // Nada nisso levanta excecao em jogo; o mob so morre rapido demais.
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> SingleEyeRules.deGraus(60.0D, 70.0D, 0.84D));
        assertTrue(erro.getMessage().contains("dano de graca"),
                "a recusa tem de dizer o que acontece em jogo, e nao so que o numero e invalido;"
                        + " veio: " + erro.getMessage());

        assertThrows(IllegalArgumentException.class,
                () -> SingleEyeRules.deGraus(60.0D, 60.0D, 0.84D),
                "arcos IGUAIS tambem sao recusados: o critico viraria a recompensa de simplesmente"
                        + " estar visivel, e o olho deixaria de ser um alvo");
    }

    @Test
    @DisplayName("sem flanco cego em que um jogador caiba nao ha encontro")
    void campoDeVisaoLargoDemaisERecusado() {
        assertThrows(IllegalArgumentException.class,
                () -> SingleEyeRules.deGraus(90.0D, 35.0D, 0.84D),
                "meia-abertura de 90 graus deixa cego so o que esta exatamente atras, e circular"
                        + " deixa de funcionar");
        assertThrows(IllegalArgumentException.class,
                () -> SingleEyeRules.deGraus(140.0D, 35.0D, 0.84D));
        assertThrows(IllegalArgumentException.class,
                () -> SingleEyeRules.deGraus(SingleEyeRules.MEIA_ABERTURA_MAXIMA_EM_GRAUS + 0.5D,
                        35.0D, 0.84D),
                "meio grau acima do limite ja e recusado: o piso e uma regua, nao uma sugestao");
        assertTrue(SingleEyeRules.deGraus(SingleEyeRules.MEIA_ABERTURA_MAXIMA_EM_GRAUS, 35.0D, 0.84D)
                        .noPontoCego(aGraus(89.0D)),
                "exatamente no limite ainda e aceito, e ainda sobra ponto cego");
    }

    @Test
    @DisplayName("olho na metade de baixo do corpo nao e cabeca")
    void alturaForaDaFaixaERecusada() {
        assertThrows(IllegalArgumentException.class,
                () -> SingleEyeRules.deGraus(60.0D, 35.0D, SingleEyeRules.ALTURA_MINIMA_ACEITAVEL));
        assertThrows(IllegalArgumentException.class,
                () -> SingleEyeRules.deGraus(60.0D, 35.0D, 0.2D));
        assertThrows(IllegalArgumentException.class,
                () -> SingleEyeRules.deGraus(60.0D, 35.0D, 1.0D),
                "altura 1.0 fica no topo exato da caixa e nenhum impacto alcanca: o ponto fraco"
                        + " existiria e nunca pagaria");
        assertThrows(IllegalArgumentException.class,
                () -> new SingleEyeRules(0.5D, Double.NaN, 0.84D));
    }
}
