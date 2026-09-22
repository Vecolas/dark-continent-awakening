package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * O jogador escolhe ONDE concentrar a aura -- Gyo, e depois Ko (#162).
 *
 * <p>POR QUE ELE EXISTE. {@code NenGyoService} declarava o proprio buraco em
 * voz alta: <i>"o jogador ainda nao tem como escolher a regiao. Nao ha payload
 * nem interface para isso, entao todo mundo concentra no padrao. A tecnica
 * funciona e a alocacao muda de verdade; o que falta e o controle."</i>
 *
 * <p>Gyo ja concentrava -- na CABECA, fixo --, a alocacao ja atravessava o
 * {@code nen_runtime_delta}, e o renderer ja desenhava por regiao. <b>A unica
 * peca faltando era o jogador poder apontar.</b>
 *
 * <p><b>ELE CARREGA UMA INTENCAO, e nada mais.</b> Nenhuma fracao, nenhum
 * custo, nenhuma alocacao pronta -- o cliente diz apenas *onde*, e o servidor
 * decide se Gyo esta ligado, quanto ele concentra e o que isso faz com a
 * defesa. Mandar a alocacao pronta seria o cliente escrevendo estado do
 * servidor, que e a regra que o {@code ProtocoloCongeladoTest} existe para
 * defender.
 *
 * <p><b>A ESCOLHA VALE MESMO SEM GYO LIGADO</b>, e isso e deliberado: ela e
 * memoria de sessao, e nao efeito. Trocar de regiao com a tecnica desligada e
 * preparacao, do mesmo jeito que mirar antes de atirar. O ADR-002 cobre o
 * resto: ela morre no logout, como qualquer estado de combate.
 */
public record EscolherFocoC2S(RegiaoDoCorpo regiao) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EscolherFocoC2S> TYPE =
            new CustomPacketPayload.Type<>(NenFoundation.id("set_focus_region_request"));

    public static final StreamCodec<ByteBuf, EscolherFocoC2S> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BYTE, p -> (byte) p.regiao().ordinal(),
                    ordinal -> new EscolherFocoC2S(regiaoDe(ordinal)));

    /**
     * Ordinal invalido vira {@code null}, e NAO um palpite.
     *
     * <p>A diferenca com {@code ImpactoDeAuraS2C} e de direcao, e nao de gosto.
     * La o pacote vem do SERVIDOR e um valor estranho e corrupcao: cair numa
     * regiao neutra desenha algo aproximado, e ninguem se machuca. Aqui o
     * pacote vem do CLIENTE, e um valor fora da faixa e um cliente modificado
     * ou de outra versao -- escolher uma regiao por ele seria aceitar um pedido
     * que ninguem fez. O validador recusa, com motivo.
     */
    private static RegiaoDoCorpo regiaoDe(byte ordinal) {
        RegiaoDoCorpo[] valores = RegiaoDoCorpo.values();
        if (ordinal < 0 || ordinal >= valores.length) {
            return null;
        }
        return valores[ordinal];
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
