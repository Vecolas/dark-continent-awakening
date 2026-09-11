package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Pedido do cliente para aumentar ou diminuir seu AOP (Aura Output).
 *
 * <p>O cliente envia a variacao (ex: +0.10F ou -0.10F). O servidor
 * soma isso ao valor atual no AuraPool e limita entre 0.0F e 1.0F.
 */
public record AjustarOutputC2S(float variacao) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AjustarOutputC2S> TYPE =
            new CustomPacketPayload.Type<>(NenFoundation.id("adjust_output_request"));

    public static final StreamCodec<ByteBuf, AjustarOutputC2S> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, AjustarOutputC2S::variacao,
                    AjustarOutputC2S::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
