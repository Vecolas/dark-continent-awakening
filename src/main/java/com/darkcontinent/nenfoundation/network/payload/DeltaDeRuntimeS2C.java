package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
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
 * <p>DECISAO: aura viaja como float, nao como double.
 * Sao valores de apresentacao - o HUD desenha e interpola entre deltas.
 *
 * <p>DECISAO: outputPercent viaja como float (0.0F a 1.0F).
 * Representa o AOP (Actual Aura Pop), influenciando o poder e os gastos de aura.
 *
 * <p>DECISAO: cooldowns e um mapa de id para TICKS RESTANTES, contados
 * pelo servidor. O cliente decrementa localmente so para animar.
 */
public record DeltaDeRuntimeS2C(
        float aura,
        float auraMaxima,
        float outputPercent,
        Set<ResourceLocation> tecnicasAtivas,
        Map<ResourceLocation, Integer> cooldowns,
        AlocacaoDeAura alocacao)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DeltaDeRuntimeS2C> TYPE =
            new CustomPacketPayload.Type<>(NenFoundation.id("nen_runtime_delta"));

    public static final StreamCodec<ByteBuf, DeltaDeRuntimeS2C> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, DeltaDeRuntimeS2C::aura,
                    ByteBufCodecs.FLOAT, DeltaDeRuntimeS2C::auraMaxima,
                    ByteBufCodecs.FLOAT, DeltaDeRuntimeS2C::outputPercent,
                    CodecsDePayload.CONJUNTO_DE_IDS, DeltaDeRuntimeS2C::tecnicasAtivas,
                    CodecsDePayload.ID_PARA_TICKS, DeltaDeRuntimeS2C::cooldowns,
                    // O SEXTO E ULTIMO PAR. `StreamCodec.composite` para aqui,
                    // e por isso a alocacao viaja como UM componente com as
                    // seis regioes dentro, e nao como seis floats soltos.
                    // Quem quiser o setimo campo vai ter de partir o payload.
                    CodecsDePayload.ALOCACAO, DeltaDeRuntimeS2C::alocacao,
                    DeltaDeRuntimeS2C::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}