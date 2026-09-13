package com.darkcontinent.nenfoundation.structure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class PostoAvancadoNaturalSpawnerTest {
    @Test
    void candidatosSaoDeterministicosEEsparsos() {
        assertTrue(PostoAvancadoNaturalSpawner.isCandidate(new ChunkPos(0, 0)));
        assertFalse(PostoAvancadoNaturalSpawner.isCandidate(new ChunkPos(0, 1)));
        assertTrue(PostoAvancadoNaturalSpawner.isCandidate(new ChunkPos(32, 0)));
    }

    @Test
    void candidatosMaterializadosPersistemEntreReinicios() {
        var data = new PostoAvancadoSpawnData();
        long candidato = new ChunkPos(32, 0).toLong();
        data.registrar(candidato);

        var salvo = data.save(new CompoundTag(), null);
        var recarregado = PostoAvancadoSpawnData.carregar(salvo, null);

        assertTrue(recarregado.contem(candidato));
    }
}
