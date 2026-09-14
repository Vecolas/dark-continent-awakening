package com.darkcontinent.nenfoundation.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Dados compactos de uma faisca criada exclusivamente pelo cliente. */
public record AuraSparkParticleOptions(
        int ownerId, int argb, float scale, int maxAtivas) implements ParticleOptions {

    public static final MapCodec<AuraSparkParticleOptions> CODEC =
            RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.INT.fieldOf("owner_id").forGetter(AuraSparkParticleOptions::ownerId),
                    Codec.INT.fieldOf("argb").forGetter(AuraSparkParticleOptions::argb),
                    Codec.FLOAT.fieldOf("scale").forGetter(AuraSparkParticleOptions::scale),
                    Codec.INT.fieldOf("max_ativas").forGetter(AuraSparkParticleOptions::maxAtivas))
                    .apply(i, AuraSparkParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AuraSparkParticleOptions>
            STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.INT, AuraSparkParticleOptions::ownerId,
                    ByteBufCodecs.INT, AuraSparkParticleOptions::argb,
                    ByteBufCodecs.FLOAT, AuraSparkParticleOptions::scale,
                    ByteBufCodecs.INT, AuraSparkParticleOptions::maxAtivas,
                    AuraSparkParticleOptions::new);

    public AuraSparkParticleOptions {
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            throw new IllegalArgumentException("scale deve ser finita e positiva: " + scale);
        }
        if (maxAtivas < 1) {
            throw new IllegalArgumentException("maxAtivas deve ser positivo: " + maxAtivas);
        }
    }

    @Override
    public ParticleType<?> getType() {
        return NenParticleTypes.AURA_SPARK.get();
    }
}
