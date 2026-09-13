package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A regua do custo da aura.
 *
 * <p>O erro que estes testes existem para impedir e especifico e bobo: fechar
 * desenho e particula na MESMA janela. Desenho acontece por quadro e particula
 * por tick, e com um fechamento so o contador de particulas piscaria entre o
 * valor certo e zero, dando a impressao de um emissor que falha de vez em
 * quando. Nao ha erro para procurar nesse caso -- so um numero que nao para
 * quieto.
 */
class MedidorDeVfxTest {

    @BeforeEach
    @AfterEach
    void semEstadoEntreTestes() {
        MedidorDeVfx.limpar();
    }

    @Test
    @DisplayName("o que se le e o quadro FECHADO, e nao o que esta em curso")
    void leOQuadroFechado() {
        MedidorDeVfx.chamadaDeDesenho();
        MedidorDeVfx.chamadaDeDesenho();
        assertEquals(0, MedidorDeVfx.chamadasDeDesenho(),
                "ler o quadro em andamento daria um numero diferente conforme o instante"
                        + " em que o overlay desenhasse");

        MedidorDeVfx.fecharQuadro();
        assertEquals(2, MedidorDeVfx.chamadasDeDesenho());
    }

    @Test
    @DisplayName("fechar o quadro NAO zera as particulas")
    void quadroNaoZeraParticula() {
        MedidorDeVfx.particulas(7);
        MedidorDeVfx.fecharTick();
        assertEquals(7, MedidorDeVfx.particulas());

        // Tres quadros sem tick nenhum -- o caso normal a 60 fps com 20 ticks.
        MedidorDeVfx.fecharQuadro();
        MedidorDeVfx.fecharQuadro();
        MedidorDeVfx.fecharQuadro();

        assertEquals(7, MedidorDeVfx.particulas(),
                "com um fechamento so, dois de cada tres quadros leriam zero particulas");
    }

    @Test
    @DisplayName("fechar o tick NAO zera o desenho")
    void tickNaoZeraDesenho() {
        MedidorDeVfx.chamadaDeDesenho();
        MedidorDeVfx.filamento();
        MedidorDeVfx.fecharQuadro();

        MedidorDeVfx.fecharTick();

        assertEquals(1, MedidorDeVfx.chamadasDeDesenho());
        assertEquals(1, MedidorDeVfx.filamentos());
    }

    @Test
    @DisplayName("custo zero e zero de verdade, e nao 'quase zero'")
    void custoZero() {
        MedidorDeVfx.fecharQuadro();
        assertTrue(MedidorDeVfx.custoZero(),
                "a assercao do AV8 e 'nenhuma aura visivel: custo zero, nao custo pequeno'");

        MedidorDeVfx.filamento();
        MedidorDeVfx.fecharQuadro();
        assertFalse(MedidorDeVfx.custoZero());
    }

    @Test
    @DisplayName("limpar apaga o que estava em curso tambem")
    void limparApagaOEmCurso() {
        MedidorDeVfx.chamadaDeDesenho();
        MedidorDeVfx.particulas(5);
        MedidorDeVfx.limpar();

        MedidorDeVfx.fecharQuadro();
        MedidorDeVfx.fecharTick();

        assertEquals(0, MedidorDeVfx.chamadasDeDesenho(),
                "um contador em curso que sobrevivesse ao logout somaria o mundo anterior"
                        + " no primeiro quadro do proximo");
        assertEquals(0, MedidorDeVfx.particulas());
    }

    @Test
    @DisplayName("contagem negativa de particula e ignorada")
    void ignoraNegativo() {
        MedidorDeVfx.particulas(-3);
        MedidorDeVfx.fecharTick();
        assertEquals(0, MedidorDeVfx.particulas());
    }
}
