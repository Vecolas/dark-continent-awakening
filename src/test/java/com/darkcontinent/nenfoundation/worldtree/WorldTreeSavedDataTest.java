package com.darkcontinent.nenfoundation.worldtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class WorldTreeSavedDataTest {
    @Test
    void origemEUnicaEDeterministica() {
        WorldTreeSavedData primeiro = new WorldTreeSavedData();
        WorldTreeSavedData segundo = new WorldTreeSavedData();

        primeiro.initialize(123L);
        segundo.initialize(123L);

        assertTrue(primeiro.treeGenerated());
        assertEquals(primeiro.overworldOriginX(), segundo.overworldOriginX());
        assertEquals(primeiro.overworldOriginZ(), segundo.overworldOriginZ());
        assertTrue(Math.hypot(primeiro.overworldOriginX(), primeiro.overworldOriginZ()) >= 2500);
    }

    @Test
    void roundTripPreservaOrigemEFlags() {
        WorldTreeSavedData original = new WorldTreeSavedData();
        original.initialize(987L);
        original.markDiscovered();
        original.markCloudLayerReached();
        original.markCrownReached();
        original.markSummitReached();
        assertTrue(original.markBaseChunkGenerated(123L));
        assertTrue(!original.markBaseChunkGenerated(123L));

        WorldTreeSavedData carregado = WorldTreeSavedData.load(original.save(new CompoundTag(), null), null);

        assertEquals(original.overworldOriginX(), carregado.overworldOriginX());
        assertEquals(original.overworldOriginZ(), carregado.overworldOriginZ());
        assertTrue(carregado.discovered());
        assertTrue(carregado.firstReachedCloudLayer());
        assertTrue(carregado.firstReachedCrown());
        assertTrue(carregado.summitReached());
        assertTrue(carregado.isBaseChunkGenerated(123L));
    }

    @Test
    void versaoFuturaERejeitada() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("generation_version", WorldTreeSavedData.CURRENT_VERSION + 1);

        assertThrows(IllegalArgumentException.class, () -> WorldTreeSavedData.load(tag, null));
    }

    @Test
    void saveLegadoSemVersaoEAtualizado() {
        CompoundTag legado = new CompoundTag();
        legado.putBoolean("tree_generated", true);

        WorldTreeSavedData carregado = WorldTreeSavedData.load(legado, null);

        assertEquals(WorldTreeSavedData.CURRENT_VERSION, carregado.generationVersion());
        assertTrue(carregado.treeGenerated());
    }
}
