package com.darkcontinent.nenfoundation.worldtree;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registro da infraestrutura da dimensao World Tree.
 *
 * <p>DECISAO DE M0: o gerador nasce como codec registrado e e consumido pelo
 * JSON da dimensao. A geometria ainda nao existe nesta etapa; manter a casca
 * data-driven permite validar o ciclo de vida da dimensao antes de acoplar
 * worldgen pesado.
 */
public final class WorldTreeRegistries {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, NenFoundation.MOD_ID);

    static {
        CHUNK_GENERATORS.register("world_tree", () -> WorldTreeChunkGenerator.CODEC);
    }

    public static void register(IEventBus eventBus) {
        CHUNK_GENERATORS.register(eventBus);
    }

    private WorldTreeRegistries() {
    }
}
