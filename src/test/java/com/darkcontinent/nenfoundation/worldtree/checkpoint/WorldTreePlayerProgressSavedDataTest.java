package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class WorldTreePlayerProgressSavedDataTest {
    @Test
    void unlockEIdempotenteEOPersiste() {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");
        WorldTreePlayerProgressSavedData original = new WorldTreePlayerProgressSavedData();

        assertTrue(original.unlock(player, WorldTreeCheckpoint.CLOUD));
        assertFalse(original.unlock(player, WorldTreeCheckpoint.CLOUD));
        assertTrue(original.isUnlocked(player, WorldTreeCheckpoint.CLOUD));

        WorldTreePlayerProgressSavedData loaded = WorldTreePlayerProgressSavedData.load(
                original.save(new CompoundTag(), null), null);
        assertTrue(loaded.isUnlocked(player, WorldTreeCheckpoint.CLOUD));
        assertFalse(loaded.isUnlocked(player, WorldTreeCheckpoint.SUMMIT));
    }
}
