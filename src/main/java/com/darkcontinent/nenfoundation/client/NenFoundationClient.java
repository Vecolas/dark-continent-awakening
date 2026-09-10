package com.darkcontinent.nenfoundation.client;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ponto de entrada CLIENT-ONLY.
 *
 * <p>DECISAO QUE ESTE ARQUIVO CARREGA: a fronteira client/server e imposta
 * por {@code dist = Dist.CLIENT} desde o primeiro dia, quando ainda nao ha nada
 * de cliente para carregar. Ele existe vazio de proposito.
 *
 * <p>O motivo: uma classe client-only alcancada pelo servidor dedicado nao
 * falha na compilacao, nao falha em singleplayer e nao falha em
 * {@code runClient}. Ela falha no servidor de verdade, com
 * {@code NoClassDefFoundError}, na frente dos jogadores. Descobrir isso no M8
 * significa desmontar a integracao entre HUD e nucleo depois de pronta;
 * descobrir no M0 nao custa nada.
 *
 * <p>REGRA DE DEPENDENCIA: {@code nen/*}, {@code api/*} e {@code network/*}
 * NUNCA importam nada de {@code client/*}. A seta aponta so para um lado. O
 * gate que verifica isso e o {@code runServer} do perfil dev-minimal, na lista
 * de smoke test em {@code docs/testing/qa-matrix.md}.
 */
@Mod(value = NenFoundation.MOD_ID, dist = Dist.CLIENT)
public final class NenFoundationClient {

    private static final Logger LOG = LoggerFactory.getLogger(NenFoundationClient.class);

    public NenFoundationClient(IEventBus modEventBus, ModContainer modContainer) {
        LOG.debug("Camada de cliente do Nen Foundation carregada.");
    }
}
