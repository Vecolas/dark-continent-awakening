package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * O que mudou no estado de combate. Nunca o perfil inteiro.
 *
 * <p>DECISAO: aura viaja como {@code float}, e nao {@code double}. E valor de
 * apresentacao — o HUD desenha e interpola entre deltas. A conta autoritativa
 * e do servidor e usa a precisao dele.
 *
 * <p>Isso e uma armadilha declarada: se algum dia alguem quiser comparar aura
 * no cliente para decidir alguma coisa, o tipo estreito e o aviso de que esse
 * numero nao serve para decidir nada. O cliente nunca calcula regra.
 *
 * <p>DECISAO: {@code cooldowns} e um mapa de id para TICKS RESTANTES, contados
 * pelo servidor. O cliente decrementa localmente so para animar, e volta a
 * obedecer ao proximo delta.
 */
public record DeltaDeRuntimeS2C(float aura,
        float auraMaxima,
        Set<ResourceLocation> tecnicasAtivas,
        Map<ResourceLocation, Integer> cooldowns)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DeltaDeRuntimeS2C> TYPE =
            new CustomPacketPayload.Type<>(NenFoundation.id("nen_runtime_delta"));

    public static final StreamCodec<ByteBuf, DeltaDeRuntimeS2C> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, DeltaDeRuntimeS2C::aura,
                    ByteBufCodecs.FLOAT, DeltaDeRuntimeS2C::auraMaxima,
                    CodecsDePayload.CONJUNTO_DE_IDS, DeltaDeRuntimeS2C::tecnicasAtivas,
                    CodecsDePayload.ID_PARA_TICKS, DeltaDeRuntimeS2C::cooldowns,
                    DeltaDeRuntimeS2C::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
