package com.darkcontinent.nenfoundation.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Dados de um detrito cosmetico de Ren, criado exclusivamente pelo cliente.
 *
 * <p><b>ELE NUNCA ATRAVESSA A REDE.</b> O codec e o stream codec existem porque
 * {@link ParticleOptions} os exige -- e nao porque alguem va serializar isto.
 * Posicao de particula nao se sincroniza, e um detrito que viajasse pelo
 * protocolo seria trafego por quadro para um efeito que o cliente ja sabe
 * desenhar sozinho.
 *
 * <p>A COR VEM DO BLOCO DE BAIXO, e nao da aura: o fragmento e materia
 * levantada, e materia levantada tem a cor do chao. E o detalhe que separa
 * "fragmento" de "faisca colorida".
 *
 * @param ownerId    a entidade que levantou o detrito; ele morre com ela
 * @param argb       a cor amostrada do bloco abaixo, com o alpha inicial
 * @param scale      o lado do cubo, em blocos
 * @param maxAtivos  teto de detritos vivos deste dono
 */
public record AuraDebrisParticleOptions(
        int ownerId, int argb, float scale, int maxAtivos) implements ParticleOptions {

    public static final MapCodec<AuraDebrisParticleOptions> CODEC =
            RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.INT.fieldOf("owner_id").forGetter(AuraDebrisParticleOptions::ownerId),
                    Codec.INT.fieldOf("argb").forGetter(AuraDebrisParticleOptions::argb),
                    Codec.FLOAT.fieldOf("scale").forGetter(AuraDebrisParticleOptions::scale),
                    Codec.INT.fieldOf("max_ativos")
                            .forGetter(AuraDebrisParticleOptions::maxAtivos))
                    .apply(i, AuraDebrisParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AuraDebrisParticleOptions>
            STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.INT, AuraDebrisParticleOptions::ownerId,
                    ByteBufCodecs.INT, AuraDebrisParticleOptions::argb,
                    ByteBufCodecs.FLOAT, AuraDebrisParticleOptions::scale,
                    ByteBufCodecs.INT, AuraDebrisParticleOptions::maxAtivos,
                    AuraDebrisParticleOptions::new);

    public AuraDebrisParticleOptions {
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            throw new IllegalArgumentException("scale deve ser finita e positiva: " + scale);
        }
        if (maxAtivos < 1) {
            throw new IllegalArgumentException("maxAtivos deve ser positivo: " + maxAtivos);
        }
    }

    @Override
    public ParticleType<?> getType() {
        return NenParticleTypes.AURA_DEBRIS.get();
    }
}
