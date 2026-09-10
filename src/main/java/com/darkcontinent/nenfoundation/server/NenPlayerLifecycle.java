package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
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

    /** Cria o perfil neutro, ou migra o perfil lido do disco, no login. */
    @SubscribeEvent
    public static void aoEntrar(PlayerEvent.PlayerLoggedInEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            PersistentNenData perfil = NenProfileService.ler(jogador);
            NenRuntimeService.iniciarSessao(jogador);
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
            NenRuntimeService.reiniciar(jogador);
        }
    }

    /** Nenhum estado de combate sobrevive ao logout. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void aoSair(PlayerEvent.PlayerLoggedOutEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            NenRuntimeService.encerrarSessao(jogador);
        }
    }

    /**
     * Politica M1: troca de dimensao encerra toda tecnica, recarga e
     * canalizacao temporaria. Estado permitido sera revalidado em marco futuro.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void aoTrocarDimensao(PlayerEvent.PlayerChangedDimensionEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            NenRuntimeService.reiniciar(jogador);
        }
    }
}
