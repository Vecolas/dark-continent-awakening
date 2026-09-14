package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuraVisualControllerTest {
    /**
     * O ESTADO NAO CARREGA MAIS NUMERO DE ARTE, e este teste mudou por causa
     * disso.
     *
     * <p>Ele conferia {@code AuraVisualPreset.tenBasic()} contra
     * {@code renBasic()} -- dois metodos que devolviam constantes de codigo. Dos
     * sete campos daquele record, CINCO nao tinham leitor nenhum: o teste
     * comparava numeros que nao chegavam a lugar algum, e teria continuado verde
     * com o efeito inteiro desligado.
     *
     * <p>A propriedade que importa -- Ten contido, Ren mais intenso -- continua
     * provada, agora sobre os arquivos que o jogo carrega de verdade. O que
     * sobrou aqui e o que o controlador ainda decide sozinho: o modo.
     */
    @Test
    void oEstadoNaoCarregaNumeroDeArte() {
        AuraVisualController c = new AuraVisualController();
        c.receber(AuraVisualMode.REN, 1.0F);
        for (int i = 0; i < 200; i++) {
            c.avancar(1.0F);
        }
        assertEquals(AuraVisualMode.REN, c.atual().mode());
        assertTrue(PerfilDoDisco.de(AuraVisualMode.REN).taxaDeFaiscas()
                        > PerfilDoDisco.de(AuraVisualMode.TEN).taxaDeFaiscas(),
                "o acabamento de Ren deixou de ser mais denso que o de Ten");
        assertEquals(0.0F, PerfilDoDisco.de(AuraVisualMode.ZETSU).taxaDeFaiscas(),
                "Zetsu com particula e o oposto de supressao");
    }

    @Test
    void transicaoTenParaRenNaoTrocaInstantaneamente() {
        AuraVisualController controller = new AuraVisualController();
        controller.receber(AuraVisualMode.TEN, 0.5F);
        concluir(controller);
        controller.receber(AuraVisualMode.REN, 1.0F);

        // METADE DA TRANSICAO, contada em TICKS da transicao em curso.
        AuraVisualState meio = avancarTicks(controller,
                AuraTransicao.de(AuraVisualMode.TEN, AuraVisualMode.REN).ticks() / 2);
        assertEquals(AuraVisualMode.TEN, meio.mode());
        assertTrue(meio.intensity() > 0.5F && meio.intensity() < 1.0F);
        assertTrue(meio.transitionProgress() < 1.0F);

        AuraVisualState finalizado = concluir(controller);
        assertEquals(AuraVisualMode.REN, finalizado.mode());
        assertEquals(1.0F, finalizado.intensity());
    }

    @Test
    void distribuicaoRejeitaValoresInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuraDistribution(Float.NaN, 1, 1, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AuraDistribution(-1, 1, 1, 1, 1, 1));
    }

    @Test
    void zetsuZeraVisualEContinuaSeguroParaRenderer() {
        AuraVisualController controller = new AuraVisualController();
        controller.receber(AuraVisualMode.TEN, 1.0F);
        concluir(controller);
        controller.receber(AuraVisualMode.ZETSU, 1.0F);
        AuraVisualState estado = concluir(controller);
        assertEquals(AuraVisualMode.ZETSU, estado.mode());
        assertEquals(0.0F, estado.intensity());
        assertTrue(!estado.enabled());
    }

    /**
     * Leva a transicao ate o fim, seja qual for a duracao dela.
     *
     * <p>AS DURACOES DEIXARAM DE SER UM NUMERO SO no AV3: cada troca tem o
     * proprio tempo, e {@code avancar} recebe uma ESCALA, e nao um passo. Os
     * testes que diziam {@code avancar(1.0F)} para "terminar agora" nao
     * terminavam mais nada -- eles pediam um tick de duracao normal.
     *
     * <p>O teto de cem ticks nao e paciencia: e a garantia de que um erro de
     * escala nunca vire um teste que nao termina.
     */
    private static AuraVisualState concluir(AuraVisualController c) {
        // AVANCO FIXO, e nao "ate o progresso chegar a 1". A primeira versao
        // disto olhava `transitionProgress() < 1`, e o estado inicial
        // (`desligado()`) ja NASCE com progresso 1 -- entao o laco nunca rodava
        // e todo teste via OFF. Quarenta ticks e mais que o dobro da transicao
        // mais longa da tabela.
        AuraVisualState estado = c.atual();
        for (int i = 0; i < 40; i++) {
            estado = c.avancar(1.0F);
        }
        return estado;
    }

    /** Avanca um numero exato de ticks. */
    private static AuraVisualState avancarTicks(AuraVisualController c, int ticks) {
        AuraVisualState estado = c.atual();
        for (int i = 0; i < ticks; i++) {
            estado = c.avancar(1.0F);
        }
        return estado;
    }
}
