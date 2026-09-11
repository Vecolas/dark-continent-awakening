package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.nen.aura.AuraPool;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import net.minecraft.server.level.ServerPlayer;

/**
 * Subsistema que regenera Aura e Vigor a cada tick e envia o delta quando sujo.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. A LEITURA DA CONFIG ACONTECE AQUI, UMA VEZ POR TICK. Passar
 * Parametros.daConfig() por tick tem custo de alocacao aceitavel - o GC
 * lida bem com objetos de vida curta. A alternativa (guardar em campo)
 * ignoraria recarregamentos de config ate o servidor reiniciar.
 *
 * <p>2. O DIRTY FLAG DETERMINA SE O SYNC ACONTECE. Se o tick nao mudar
 * nenhum pool, nenhum pacote e enviado.
 *
 * <p>3. marcarSincronizado() E CHAMADO DEPOIS DO ENVIO, NUNCA ANTES.
 * Se limpar o flag antes do envio falhar, o cliente fica desatualizado
 * em silencio. Limpar depois gera um retry no proximo tick.
 */
public final class AuraTickSubsystem implements NenTickSubsystem {

    @Override
    public void serverTick(ServerPlayer jogador, RuntimeNenState estado) {
        PersistentNenData perfil = NenProfileService.ler(jogador);
        AuraPool.Parametros p = AuraPool.Parametros.daConfig();
        AuraPool pool = estado.pool();

        pool.tick(perfil, p);

        if (pool.dirty() != AuraPool.DirtyFlag.LIMPO) {
            NenSyncService.enviarDelta(jogador);
            pool.marcarSincronizado();
        }
    }
}