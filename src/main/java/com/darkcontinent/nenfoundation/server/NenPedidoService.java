package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import com.darkcontinent.nenfoundation.network.handler.ValidacaoDePedido;
import com.darkcontinent.nenfoundation.network.handler.ValidacaoDePedido.Motivo;
import com.darkcontinent.nenfoundation.network.payload.AtivarHabilidadeC2S;
import com.darkcontinent.nenfoundation.network.payload.AtivarTecnicaC2S;
import com.darkcontinent.nenfoundation.network.payload.DesativarTecnicaC2S;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Consulta autoridade server-side na thread do jogo. M1 nao tem registro de
 * tecnicas/habilidades executaveis: qualquer id e desconhecido, mesmo liberado
 * via debug. M4/M5 conectarao seus registros e motores, com custo/alvo do spec.
 */
public final class NenPedidoService {
    private NenPedidoService() { }

    public static Motivo validar(ServerPlayer jogador, CustomPacketPayload pedido) {
        ResourceLocation id;
        boolean habilidade = pedido instanceof AtivarHabilidadeC2S;
        if (pedido instanceof AtivarTecnicaC2S ativar) {
            id = ativar.tecnicaId();
        } else if (pedido instanceof DesativarTecnicaC2S desativar) {
            id = desativar.tecnicaId();
        } else if (pedido instanceof AtivarHabilidadeC2S ativar) {
            id = ativar.habilidadeId();
            // Nao ha slots equipados no M1; nao inventar um teto para o M5.
            if (ativar.slot() < 0) return Motivo.PEDIDO_INVALIDO;
            if (ativar.posicaoCandidata().isPresent()) {
                var posicao = ativar.posicaoCandidata().get();
                if (!Double.isFinite(posicao.x) || !Double.isFinite(posicao.y)
                        || !Double.isFinite(posicao.z)) return Motivo.ALVO_INVALIDO;
            }
            if (ativar.alvoIdCandidato().isPresent()) {
                var alvo = jogador.serverLevel().getEntity(ativar.alvoIdCandidato().getAsInt());
                if (alvo == null || alvo.isRemoved() || alvo.level() != jogador.level()) {
                    return Motivo.ALVO_INVALIDO;
                }
            }
        } else if (pedido instanceof com.darkcontinent.nenfoundation.network.payload.AjustarOutputC2S ajustar) {
            // ESTE PAYLOAD CARREGA UM NUMERO DO CLIENTE, e por isso o numero e
            // conferido aqui -- do mesmo jeito que a posicao candidata de
            // AtivarHabilidadeC2S e conferida logo acima.
            //
            // Sem esta linha, NaN atravessava: o clamp em AuraPool.ajustarOutput
            // usa Math.min/Math.max, que propagam NaN em vez de segura-lo. O
            // resultado era NaN gravado no runtime e enviado no delta ao HUD --
            // sem excecao, sem log, e com a barra congelada para sempre.
            if (!Float.isFinite(ajustar.variacao())) return Motivo.PEDIDO_INVALIDO;

            RuntimeNenState estado = NenRuntimeService.estadoDe(jogador);
            // AJUSTA O SELECIONADO, nao o efetivo: o efetivo e derivado e nao
            // tem setter. Somar sobre o efetivo faria a escolha do jogador ser
            // silenciosamente rebaixada toda vez que o maximo estivesse abaixo.
            //
            // PONTO CEGO: este caminho ainda soma uma VARIACAO ARBITRARIA do
            // cliente, em vez de pedir um passo. O passo de 5 pontos ja existe
            // no dominio (AuraPool.PASSO_DE_OUTPUT), mas trocar o formato do
            // payload e a issue #71 -- que esta bloqueada por exigir ADR de
            // descongelamento do protocolo (ADR-004).
            estado.definirOutputSelecionado(estado.outputSelecionado() + ajustar.variacao());
            NenSyncService.enviarDeltaSeAuraSuja(jogador);
            return null; // Sucesso, nao envia feedback de recusa
        } else {
            return Motivo.PEDIDO_INVALIDO;
        }
        return ValidacaoDePedido.validar(id, habilidade, false,
                jogador.isAlive() && !jogador.isRemoved() && !jogador.isSpectator(),
                NenProfileService.ler(jogador), NenRuntimeService.estadoDe(jogador));
    }
}
