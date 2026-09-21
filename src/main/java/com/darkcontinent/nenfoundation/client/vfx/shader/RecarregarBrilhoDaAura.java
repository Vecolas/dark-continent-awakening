package com.darkcontinent.nenfoundation.client.vfx.shader;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * O que acontece com o passe de brilho quando alguem aperta F3+T.
 *
 * <p><b>ELE EXISTE PORQUE FRAMEBUFFER NAO RECRIADO NAO DA ERRO.</b> O
 * [ADR-016](docs/adr/ADR-016-pos-processamento-proprio-da-aura.md) secao 5 chama
 * os dois caminhos pelo nome -- redimensionar a janela e recarregar recurso --,
 * e nenhum dos dois falha alto: o primeiro da tela que some, e o segundo da
 * memoria que sobe devagar ao longo de uma sessao. Os dois sao teste
 * obrigatorio, e os dois passam despercebidos quando esquecidos.
 *
 * <p>O REDIMENSIONAMENTO E TRATADO POR COMPARACAO DE TAMANHO a cada quadro, em
 * {@code AuraPostProcess}: duas comparacoes de inteiro, sem depender de um
 * gancho de evento que muda entre versoes. A RECARGA precisa de um gancho, e e
 * este.
 *
 * <p><b>A RECARGA TAMBEM ESQUECE O REBAIXAMENTO</b>, e essa e a metade que quase
 * se perde. Se a queda para {@code FAST} veio de um resource pack com shader
 * torto, remover o pack e recarregar precisa devolver o nivel escolhido. Sem
 * isso, a unica saida seria reiniciar o jogo -- e ninguem liga "o bloom nao
 * voltou" a uma variavel de sessao.
 *
 * <p>TUDO NA FASE DE APLICACAO, e nada na de preparacao. O trabalho e soltar
 * recurso de GPU, e recurso de GPU so pode ser tocado na thread de render --
 * fazer isso na fase paralela nao lanca de forma legivel: trava, ou corrompe
 * estado de driver.
 */
public final class RecarregarBrilhoDaAura implements PreparableReloadListener {

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barreira,
            ResourceManager gerenciador, ProfilerFiller perfiladorDePreparo,
            ProfilerFiller perfiladorDeAplicacao, Executor executorDePreparo,
            Executor executorDeAplicacao) {
        return barreira.wait(null).thenRunAsync(AuraPostProcess::aoRecarregarRecursos,
                executorDeAplicacao);
    }

    @Override
    public String getName() {
        return "nenfoundation:brilho_da_aura";
    }
}
