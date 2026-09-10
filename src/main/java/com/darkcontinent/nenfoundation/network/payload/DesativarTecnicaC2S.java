package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * O jogador pediu para desligar uma tecnica.
 *
 * <p>DECISAO: e um pedido, nao uma ordem. Nem toda tecnica pode ser desligada
 * a vontade — Zetsu pode ter periodo minimo, uma tecnica pode estar travada
 * por efeito. O servidor decide.
 */
public record DesativarTecnicaC2S(ResourceLocation tecnicaId)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DesativarTecnicaC2S> TYPE =
            new CustomPacketPayload.Type<>(NenFoundation.id("deactivate_technique_request"));

    public static final StreamCodec<ByteBuf, DesativarTecnicaC2S> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, DesativarTecnicaC2S::tecnicaId,
                    DesativarTecnicaC2S::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
