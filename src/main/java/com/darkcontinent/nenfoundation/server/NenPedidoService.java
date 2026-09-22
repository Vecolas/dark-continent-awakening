package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import com.darkcontinent.nenfoundation.network.handler.ValidacaoDePedido;
import com.darkcontinent.nenfoundation.network.handler.ValidacaoDePedido.Motivo;
import com.darkcontinent.nenfoundation.network.handler.ValidacaoDePedido.Recusa;
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
    private static Recusa ligar(ServerPlayer jogador, ResourceLocation id) {
        NenTechniqueService.Resultado r = NenTechniqueService.ativar(jogador, id);
        return switch (r.estado()) {
            // JA_ATIVA nao e recusa: o jogador apertou de novo, e o estado
            // final e o que ele queria. Responder erro aqui faria a roda
            // piscar uma mensagem por segurar a tecla.
            case ATIVOU, JA_ATIVA -> null;
            case DESCONHECIDA -> Recusa.de(Motivo.ID_INEXISTENTE);
            // A CHAVE DA TECNICA, e nao um motivo generico. Ate aqui isto era
            // `ESTADO_INVALIDO`, e o texto que a tecnica escreveu morria neste
            // switch: Shu recusa mao vazia com chave propria, o gametest
            // passava, e o jogador lia "seu estado atual nao permite esse
            // pedido" -- uma mensagem sobre outro problema.
            case RECUSADA -> chaveDe(r).map(Recusa::new)
                    .orElseGet(() -> Recusa.de(Motivo.ESTADO_INVALIDO));
        };
    }

    /**
     * A chave de traducao de dentro do motivo da tecnica.
     *
     * <p>O motivo viaja como {@code Component} porque {@code NenTechnique} e
     * contrato congelado (ADR-004) e nao da para trocar o tipo. Um
     * {@code Component} traduzivel guarda a chave; qualquer outro texto nao
     * tem chave nenhuma, e ai o motivo generico e a resposta honesta -- melhor
     * que mandar texto pronto no idioma do servidor.
     */
    private static java.util.Optional<String> chaveDe(NenTechniqueService.Resultado r) {
        return r.motivo()
                .map(net.minecraft.network.chat.Component::getContents)
                .filter(c -> c instanceof net.minecraft.network.chat.contents.TranslatableContents)
                .map(c -> ((net.minecraft.network.chat.contents.TranslatableContents) c).getKey());
    }

    /** Desliga a tecnica. Desligar o que ja esta desligado tambem nao e erro. */
    private static Recusa desligar(ServerPlayer jogador, ResourceLocation id) {
        NenTechniqueService.desligar(jogador, id,
                com.darkcontinent.nenfoundation.nen.technique.StopReason.PLAYER_REQUEST);
        return null;
    }

    public static Recusa validar(ServerPlayer jogador, CustomPacketPayload pedido) {
        ResourceLocation id;
        boolean habilidade = pedido instanceof AtivarHabilidadeC2S;
        if (pedido instanceof AtivarTecnicaC2S ativar) {
            id = ativar.tecnicaId();
        } else if (pedido instanceof DesativarTecnicaC2S desativar) {
            id = desativar.tecnicaId();
        } else if (pedido instanceof AtivarHabilidadeC2S ativar) {
            id = ativar.habilidadeId();
            // Nao ha slots equipados no M1; nao inventar um teto para o M5.
            if (ativar.slot() < 0) return Recusa.de(Motivo.PEDIDO_INVALIDO);
            if (ativar.posicaoCandidata().isPresent()) {
                var posicao = ativar.posicaoCandidata().get();
                if (!Double.isFinite(posicao.x) || !Double.isFinite(posicao.y)
                        || !Double.isFinite(posicao.z)) return Recusa.de(Motivo.ALVO_INVALIDO);
            }
            if (ativar.alvoIdCandidato().isPresent()) {
                var alvo = jogador.serverLevel().getEntity(ativar.alvoIdCandidato().getAsInt());
                if (alvo == null || alvo.isRemoved() || alvo.level() != jogador.level()) {
                    return Recusa.de(Motivo.ALVO_INVALIDO);
                }
            }
        } else if (pedido instanceof
                com.darkcontinent.nenfoundation.network.payload.EscolherFocoC2S foco) {
            // REGIAO NULA E PEDIDO INVALIDO, e nao um palpite. O codec devolve
            // null para ordinal fora da faixa -- cliente de outra versao ou
            // modificado --, e escolher uma regiao por ele seria aceitar um
            // pedido que ninguem fez.
            if (foco.regiao() == null) {
                return Recusa.de(Motivo.PEDIDO_INVALIDO);
            }
            com.darkcontinent.nenfoundation.server.NenGyoService.escolher(jogador, foco.regiao());
            // O RECALCULO E DO SERVICO DE TECNICA, e nao daqui: a alocacao e
            // DERIVADA das tecnicas ativas mais o foco, e recalcular a mao neste
            // ponto seria a segunda fonte da mesma conta.
            //
            // E ELE ACONTECE AGORA, e nao no proximo tick: sem isto a troca de
            // regiao so apareceria quando outra coisa disparasse o recalculo, e
            // o jogador veria a aura mudar de lugar segundos depois de pedir --
            // ou nunca, se nada mais acontecesse.
            NenTechniqueService.recalcularDerivados(
                    NenRuntimeService.estadoDe(jogador),
                    com.darkcontinent.nenfoundation.server.NenGyoService.focoDe(jogador));
            return null;
        } else if (pedido instanceof com.darkcontinent.nenfoundation.network.payload.AjustarOutputC2S ajustar) {
            // ESTE PAYLOAD CARREGA UM NUMERO DO CLIENTE, e por isso o numero e
            // conferido aqui -- do mesmo jeito que a posicao candidata de
            // AtivarHabilidadeC2S e conferida logo acima.
            //
            // Sem esta linha, NaN atravessava: o clamp em AuraPool.ajustarOutput
            // usa Math.min/Math.max, que propagam NaN em vez de segura-lo. O
            // resultado era NaN gravado no runtime e enviado no delta ao HUD --
            // sem excecao, sem log, e com a barra congelada para sempre.
            RuntimeNenState estado = NenRuntimeService.estadoDe(jogador);
            // AJUSTA O SELECIONADO, nao o efetivo: o efetivo e derivado e nao
            // tem setter. Somar sobre o efetivo faria a escolha do jogador ser
            // silenciosamente rebaixada toda vez que o maximo estivesse abaixo.
            //
            // O PASSO E DO SERVIDOR. O payload traz "para cima" ou "para baixo"
            // e mais nada; quem sabe de quanto e o dominio. Enquanto o cliente
            // mandava a variacao, um cliente modificado ia de zero a cem num
            // pacote so -- e o resultado ficava DENTRO da faixa, entao nada
            // parecia errado.
            if (ajustar.aumentar()) {
                estado.aumentarOutput();
            } else {
                estado.diminuirOutput();
            }
            NenSyncService.enviarDeltaSeAuraSuja(jogador);
            return null; // Sucesso, nao envia feedback de recusa
        } else {
            return Recusa.de(Motivo.PEDIDO_INVALIDO);
        }
        // O CATALOGO AGORA EXISTE para tecnica: `existe` deixa de ser sempre
        // falso e passa a perguntar ao registro. Ate o M4 nao havia registro,
        // e por isso todo id era desconhecido.
        boolean existe = !habilidade && NenTechniqueService.registro().porId(id).isPresent();

        Motivo recusa = ValidacaoDePedido.validar(id, habilidade, existe,
                jogador.isAlive() && !jogador.isRemoved() && !jogador.isSpectator(),
                NenProfileService.ler(jogador), NenRuntimeService.estadoDe(jogador));
        if (recusa != null) {
            return Recusa.de(recusa);
        }
        if (habilidade) {
            return null;
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
