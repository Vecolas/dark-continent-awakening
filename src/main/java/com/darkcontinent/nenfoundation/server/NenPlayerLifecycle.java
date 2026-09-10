package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.NenFoundation;
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
            NenProfileService.ler(jogador);
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
        }
    }
}
