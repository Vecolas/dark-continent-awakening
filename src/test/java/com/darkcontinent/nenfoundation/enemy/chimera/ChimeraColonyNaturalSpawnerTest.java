package com.darkcontinent.nenfoundation.enemy.chimera;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class ChimeraColonyNaturalSpawnerTest {
    @Test
    void candidatoEDeterministicoERaro() {
        ChunkPos candidato = new ChunkPos(0, 0);
        assertTrue(ChimeraColonyNaturalSpawner.isCandidate(candidato));
        assertFalse(ChimeraColonyNaturalSpawner.isCandidate(new ChunkPos(0, 1)));
        assertFalse(ChimeraColonyNaturalSpawner.isCandidate(new ChunkPos(1, 0)));
        assertTrue(ChimeraColonyNaturalSpawner.isCandidate(candidato));
    }

    @Test
    void proximidadeDeColoniaTemTetoQuadratico() {
        var dados = new ChimeraColonySavedData();
        dados.registrar(new ChimeraColony(java.util.UUID.randomUUID(), new BlockPos(10, 64, 10)));
        assertTrue(dados.existePerto(new BlockPos(100, 64, 10), 90));
        assertFalse(dados.existePerto(new BlockPos(101, 64, 10), 90));
    }
}
