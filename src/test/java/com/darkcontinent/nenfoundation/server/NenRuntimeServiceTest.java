package com.darkcontinent.nenfoundation.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NenRuntimeServiceTest {

    @BeforeEach
    @AfterEach
    void limparRegistro() {
        NenRuntimeService.encerrarTodasAsSessoes();
    }

    @Test
    @DisplayName("logout remove a sessao da colecao global sem atingir outro jogador")
    void logoutRemoveSessaoDaColecao() {
        UUID primeiro = UUID.randomUUID();
        UUID segundo = UUID.randomUUID();
        RuntimeNenState estadoDoSegundo = NenRuntimeService.iniciarSessao(segundo);
        NenRuntimeService.iniciarSessao(primeiro);
        assertEquals(2, NenRuntimeService.quantidadeDeSessoes());

        NenRuntimeService.encerrarSessao(primeiro);

        assertEquals(1, NenRuntimeService.quantidadeDeSessoes(),
                "jogador removido nao pode continuar na colecao global");
        assertThrows(IllegalStateException.class,
                () -> NenRuntimeService.estadoDe(primeiro));
        assertSame(estadoDoSegundo, NenRuntimeService.estadoDe(segundo));
    }

    @Test
    @DisplayName("respawn ou dimensao substitui o estado por outro neutro")
    void resetSubstituiEstadoSemDuplicarSessao() {
        UUID jogador = UUID.randomUUID();
        RuntimeNenState anterior = NenRuntimeService.iniciarSessao(jogador);
        anterior.definirAuraMaxima(40.0D);
        anterior.definirAuraAtual(30.0D);

        RuntimeNenState reiniciado = NenRuntimeService.iniciarSessao(jogador);

        assertNotSame(anterior, reiniciado);
        assertEquals(0.0D, reiniciado.auraAtual());
        assertEquals(1, NenRuntimeService.quantidadeDeSessoes());
    }

    @Test
    @DisplayName("estado ausente nao e recriado silenciosamente pelo tick")
    void estadoAusenteFalhaVisivelmente() {
        UUID ausente = UUID.randomUUID();

        assertThrows(IllegalStateException.class,
                () -> NenRuntimeService.estadoDe(ausente));
        assertEquals(0, NenRuntimeService.quantidadeDeSessoes());
    }

    // ------------------------------------ a politica de reset do Output (#70)

    /**
     * A POLITICA, escrita em teste porque texto nao reprova:
     *
     * <p>O Output -- selecionado e maximo -- e estado de RUNTIME, e some em
     * morte, logout, troca de dimensao e restart do servidor. Nada dele e
     * persistido. Isso nao e esquecimento: e o ADR-002, que separa progresso
     * de estado de combate. Persistir o Output exigiria revisao daquele ADR
     * com aprovacao das duas pessoas, e a issue #70 nomeia esse bloqueio.
     *
     * <p>Os quatro eventos passam pelo mesmo caminho -- um RuntimeNenState
     * NOVO --, entao um teste que prove o reset em um deles prova nos quatro.
     * Os tres primeiros chamam {@code iniciarSessao}; o restart e o processo
     * morrendo com o mapa em memoria.
     */
    @Test
    @DisplayName("morte, dimensao e logout devolvem o Output ao padrao")
    void outputNaoSobreviveAoCicloDeSessao() {
        UUID jogador = UUID.nameUUIDFromBytes("output-reset".getBytes(
                java.nio.charset.StandardCharsets.UTF_8));

        RuntimeNenState antes = NenRuntimeService.iniciarSessao(jogador);
        antes.definirOutputSelecionado(0.25F);
        antes.definirOutputMaximo(0.5F);
        assertEquals(0.25F, antes.outputEfetivo(), 1.0E-6F);

        // Morte ou troca de dimensao: o ciclo de vida chama reiniciar, que e
        // este mesmo caminho.
        RuntimeNenState depois = NenRuntimeService.iniciarSessao(jogador);
        assertEquals(1.0F, depois.outputSelecionado(), 1.0E-6F,
                "O Output selecionado sobreviveu ao reset de sessao. Ele e"
                        + " estado de combate: persistir exigiria revisao do"
                        + " ADR-002 com aprovacao das duas pessoas.");
        assertEquals(1.0F, depois.outputMaximo(), 1.0E-6F,
                "O teto sobreviveu ao reset.");

        // E o objeto antigo nao pode continuar sendo alcancavel pelo servico.
        assertNotSame(antes, NenRuntimeService.estadoDe(jogador));

        NenRuntimeService.encerrarSessao(jogador);
        RuntimeNenState novaSessao = NenRuntimeService.iniciarSessao(jogador);
        assertEquals(1.0F, novaSessao.outputEfetivo(), 1.0E-6F,
                "O Output atravessou um logout.");
    }
}
