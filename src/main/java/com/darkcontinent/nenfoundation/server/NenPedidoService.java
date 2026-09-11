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

    /** Liga a tecnica e traduz o resultado do motor em recusa, ou {@code null}. */
    private static Motivo ligar(ServerPlayer jogador, ResourceLocation id) {
        NenTechniqueService.Resultado r = NenTechniqueService.ativar(jogador, id);
        return switch (r.estado()) {
            // JA_ATIVA nao e recusa: o jogador apertou de novo, e o estado
            // final e o que ele queria. Responder erro aqui faria a roda
            // piscar uma mensagem por segurar a tecla.
            case ATIVOU, JA_ATIVA -> null;
            case DESCONHECIDA -> Motivo.ID_INEXISTENTE;
            case RECUSADA -> Motivo.ESTADO_INVALIDO;
        };
    }

    /** Desliga a tecnica. Desligar o que ja esta desligado tambem nao e erro. */
    private static Motivo desligar(ServerPlayer jogador, ResourceLocation id) {
        NenTechniqueService.desligar(jogador, id,
                com.darkcontinent.nenfoundation.nen.technique.StopReason.PLAYER_REQUEST);
        return null;
    }

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
        // O CATALOGO AGORA EXISTE para tecnica: `existe` deixa de ser sempre
        // falso e passa a perguntar ao registro. Ate o M4 nao havia registro,
        // e por isso todo id era desconhecido.
        boolean existe = !habilidade && NenTechniqueService.registro().porId(id).isPresent();

        Motivo recusa = ValidacaoDePedido.validar(id, habilidade, existe,
                jogador.isAlive() && !jogador.isRemoved() && !jogador.isSpectator(),
                NenProfileService.ler(jogador), NenRuntimeService.estadoDe(jogador));
        if (recusa != null || habilidade) {
            return recusa;
        }

        // VALIDAR E EXECUTAR ESTAO NO MESMO METODO, e isso e uma divida que
        // este arquivo ja carregava: o ramo de AjustarOutputC2S faz o mesmo.
        // O nome `validar` deixou de descrever o que o metodo faz. Esta
        // registrado na issue #71; nao foi consertado aqui para nao misturar
        // um refactor de fluxo com a entrega da roda.
        return pedido instanceof DesativarTecnicaC2S
                ? desligar(jogador, id)
                : ligar(jogador, id);
    }
}
