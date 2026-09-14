package com.darkcontinent.nenfoundation.worldtree;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.worldtree.checkpoint.WorldTreeCheckpoint;
import com.darkcontinent.nenfoundation.worldtree.checkpoint.WorldTreePlayerProgressSavedData;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Prova de isolamento do progresso que o servidor usa para dois jogadores. */
class WorldTreeMultiplayerProgressTest {
    @Test
    void doisJogadoresNaoCompartilhamCheckpoints() {
        WorldTreePlayerProgressSavedData progress = new WorldTreePlayerProgressSavedData();
        UUID primeiro = UUID.randomUUID();
        UUID segundo = UUID.randomUUID();

        progress.unlock(primeiro, WorldTreeCheckpoint.BASE);
        progress.unlock(segundo, WorldTreeCheckpoint.CLOUD);

        assertTrue(progress.isUnlocked(primeiro, WorldTreeCheckpoint.BASE));
        assertFalse(progress.isUnlocked(primeiro, WorldTreeCheckpoint.CLOUD));
        assertTrue(progress.isUnlocked(segundo, WorldTreeCheckpoint.CLOUD));
        assertFalse(progress.isUnlocked(segundo, WorldTreeCheckpoint.BASE));
    }
}
