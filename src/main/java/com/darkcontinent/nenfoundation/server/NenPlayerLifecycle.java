package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.technique.StopReason;
import com.darkcontinent.nenfoundation.network.handler.PedidosC2S;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Garante que todo perfil carregado ou clonado passe pelo servico. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class NenPlayerLifecycle {

    private NenPlayerLifecycle() {
    }

    /**
     * Desliga as tecnicas ativas sem deixar o ciclo de vida quebrar por causa
     * delas.
     *
     * <p>Um jogador SEM sessao de runtime e caso normal aqui: o evento de
     * logout pode chegar para quem nunca teve uma, e `estadoDe` lanca de
     * proposito nesse caso. Deixar a excecao subir abortaria o resto do
     * tratamento de saida -- inclusive o encerramento da sessao de pedidos.
     */
    private static void desligarTecnicasEmSilencio(ServerPlayer jogador, StopReason motivo) {
        try {
            NenTechniqueService.desligarTodas(jogador, motivo);
        } catch (IllegalStateException semSessao) {
            // Sem runtime nao ha tecnica ativa: nao ha o que desligar.
        }
    }

    /** Cria o perfil neutro, ou migra o perfil lido do disco, no login. */
    @SubscribeEvent
    public static void aoEntrar(PlayerEvent.PlayerLoggedInEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            PersistentNenData perfil = NenProfileService.ler(jogador);
            NenRuntimeService.iniciarSessao(jogador);
            PedidosC2S.iniciar(jogador.connection.getConnection());
            NenSyncService.enviarSnapshot(jogador, perfil);
        }
    }

    /**
     * Migra o attachment depois de o NeoForge aplicar a politica de copia.
     *
     * <p>LOWEST e deliberado: o handler interno do NeoForge que executa
     * {@code copyOnDeath()} usa a prioridade normal. Este handler nao copia o
     * dado de novo; apenas le o perfil ja copiado pelo servico.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void aoClonar(PlayerEvent.Clone evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            NenProfileService.ler(jogador);

            // DESLIGA ANTES DE REINICIAR, e a ordem e o ponto.
            //
            // `reiniciar` troca o RuntimeNenState por um novo -- as tecnicas
            // somem da lista sozinhas. Mas sumir da lista NAO e desligar:
            // `onDeactivate` nunca rodaria, e todo modificador, listener ou
            // entidade que a tecnica criou ficaria ligado para sempre, sem
            // dono e sem erro. E o erro numero 3 do CLAUDE.md.
            //
            // USA O JOGADOR NOVO, e nao `getOriginal()`. Duas razoes, e as
            // duas importam:
            //
            //   - o runtime e indexado por UUID, e a morte nao troca o UUID.
            //     O estado alcancado aqui e exatamente o mesmo que o jogador
            //     anterior tinha;
            //   - `NenProfileBoundaryTest` proibe `getOriginal()` neste
            //     handler, porque quem copia dado de perfil e o copyOnDeath()
            //     do NeoForge. O portao nao distingue "copiar perfil" de "ler
            //     a entidade antiga", e esta certo em nao distinguir: o
            //     caminho seguro e nao precisar dela.
            //
            // A entidade antiga esta sendo descartada de qualquer jeito; o que
            // precisa de limpeza e o que vive por UUID.
            desligarTecnicasEmSilencio(jogador,
                    evento.isWasDeath() ? StopReason.DEATH : StopReason.DIMENSION_CHANGE);
            NenRuntimeService.reiniciar(jogador);
        }
    }

    /** Invalida na conclusao do respawn, com o listener associado a nova entidade. */
    @SubscribeEvent
    public static void aoRenascer(PlayerEvent.PlayerRespawnEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            PedidosC2S.invalidar(jogador.connection.getConnection());
        }
    }

    /**
     * Quem comeca a enxergar alguem recebe o sinal de aura atual dele.
     *
     * <p>SEM ISTO O BUG E CHATO DE ACHAR: o anuncio so acontece quando o sinal
     * MUDA, entao quem chega perto de alguem ja em Ren nao ve nada. A aura so
     * apareceria quando a outra pessoa alternasse a tecnica, e o relato viraria
     * "as vezes a aura do outro jogador nao aparece".
     */
    @SubscribeEvent
    public static void aoComecarARastrear(PlayerEvent.StartTracking evento) {
        if (evento.getEntity() instanceof ServerPlayer observador
                && evento.getTarget() instanceof ServerPlayer alvo) {
            NenPresencaService.anunciarPara(observador, alvo);
        }
    }

    /** Nenhum estado de combate sobrevive ao logout. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void aoSair(PlayerEvent.PlayerLoggedOutEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            desligarTecnicasEmSilencio(jogador, StopReason.LOGOUT);
            NenRuntimeService.encerrarSessao(jogador);
            NenPresencaService.esquecer(jogador);
            PedidosC2S.encerrar(jogador.connection.getConnection());
        }
    }

    /**
     * Politica M1: troca de dimensao encerra toda tecnica, recarga e
     * canalizacao temporaria. Estado permitido sera revalidado em marco futuro.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void aoTrocarDimensao(PlayerEvent.PlayerChangedDimensionEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            desligarTecnicasEmSilencio(jogador, StopReason.DIMENSION_CHANGE);
            NenRuntimeService.reiniciar(jogador);
            PedidosC2S.invalidar(jogador.connection.getConnection());
        }
    }
}
