package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Por que o servidor recusou.
 *
 * <p>DECISAO: viaja uma CHAVE DE TRADUCAO, nunca o texto pronto. Texto na rede
 * sai no idioma do servidor, e o jogador le em outro.
 *
 * <p>DECISAO: o motivo nunca revela estado alheio. "Aura insuficiente" e sobre
 * quem pediu; "alvo protegido por Ten" conta ao atacante algo que ele nao
 * deveria saber. A regra nao e verificavel por portao — ela mora na revisao de
 * cada mensagem nova.
 *
 * <p>Recusa silenciosa produz o pior relato de bug que existe: "aperto a tecla
 * e nao acontece nada", sem nada no log dizendo qual das oito validacoes
 * reprovou.
 */
public record FeedbackDeErroS2C(String chaveDeTraducao)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FeedbackDeErroS2C> TYPE =
            new CustomPacketPayload.Type<>(NenFoundation.id("nen_error_feedback"));

    public static final StreamCodec<ByteBuf, FeedbackDeErroS2C> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, FeedbackDeErroS2C::chaveDeTraducao,
                    FeedbackDeErroS2C::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
