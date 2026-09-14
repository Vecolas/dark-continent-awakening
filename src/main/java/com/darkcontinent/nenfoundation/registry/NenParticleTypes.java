package com.darkcontinent.nenfoundation.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Tipos de particula do mod; o provider visual e registrado apenas no cliente. */
public final class NenParticleTypes {

    public static final DeferredRegister<ParticleType<?>> TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, NenFoundation.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<AuraSparkParticleOptions>>
            AURA_SPARK = TYPES.register("aura_spark",
                    () -> new ParticleType<AuraSparkParticleOptions>(false) {
                        @Override
                        public MapCodec<AuraSparkParticleOptions> codec() {
                            return AuraSparkParticleOptions.CODEC;
                        }

                        @Override
                        public StreamCodec<RegistryFriendlyByteBuf, AuraSparkParticleOptions>
                                streamCodec() {
                            return AuraSparkParticleOptions.STREAM_CODEC;
                        }
                    });

    private NenParticleTypes() {
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
