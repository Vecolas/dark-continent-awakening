package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Intencao de subir ou descer um passo de AOP (Aura Output).
 *
 * <p>UM BOOLEANO, E NAO UM NUMERO -- e essa foi a mudanca. A versao anterior
 * carregava um {@code float} livre: o cliente dizia <i>quanto</i> mudar, e o
 * servidor so limitava o resultado entre 0 e 1. Um cliente modificado ia de
 * zero a cem num pacote so, e nada nisso parecia errado: o valor final estava
 * dentro da faixa, e nenhum portao olhava para o caminho.
 *
 * <p>O ADR-001 diz que o cliente manda intencao e o servidor aplica. "Aumente
 * um passo" e intencao; "aumente 0,37" e o cliente escolhendo o resultado.
 *
 * <p>O TAMANHO DO PASSO MORA NO DOMINIO ({@code AuraPool.PASSO_DE_OUTPUT}), e
 * agora mora em UM lugar so. Enquanto o payload carregava o numero, o cliente
 * mandava 0,10 e o dominio dizia 0,05 -- duas fontes para a mesma verdade,
 * discordando havia meses, e o botao que o jogador aperta andava o dobro do que
 * qualquer outra parte do jogo achava.
 */
public record AjustarOutputC2S(boolean aumentar) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AjustarOutputC2S> TYPE =
            new CustomPacketPayload.Type<>(NenFoundation.id("adjust_output_request"));

    public static final StreamCodec<ByteBuf, AjustarOutputC2S> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, AjustarOutputC2S::aumentar,
                    AjustarOutputC2S::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
