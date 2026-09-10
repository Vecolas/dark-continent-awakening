package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Som, particula e animacao de uma habilidade.
 *
 * <p>DECISAO: este payload NAO altera nenhuma logica no cliente. Um FX perdido
 * tem de produzir, no maximo, um efeito visual que faltou — nunca um estado
 * divergente. Se algum dia um handler de FX precisar mudar estado, o estado
 * esta no lugar errado.
 *
 * <p>{@code variante} e um discriminador pequeno (fase, elemento, direcao)
 * resolvido pelo SERVIDOR. O cliente escolhe o efeito, nao a regra.
 */
public record FxDeHabilidadeS2C(ResourceLocation habilidadeId,
        Vec3 posicao,
        int variante)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FxDeHabilidadeS2C> TYPE =
            new CustomPacketPayload.Type<>(NenFoundation.id("ability_fx_event"));

    public static final StreamCodec<ByteBuf, FxDeHabilidadeS2C> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, FxDeHabilidadeS2C::habilidadeId,
                    CodecsDePayload.VEC3, FxDeHabilidadeS2C::posicao,
                    ByteBufCodecs.VAR_INT, FxDeHabilidadeS2C::variante,
                    FxDeHabilidadeS2C::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
