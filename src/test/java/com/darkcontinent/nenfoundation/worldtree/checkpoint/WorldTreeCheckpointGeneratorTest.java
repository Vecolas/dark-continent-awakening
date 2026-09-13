package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayoutGenerator;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class WorldTreeCheckpointGeneratorTest {
    @Test
    void anchorUsaChunkDeterministicoDaAltitude() {
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(42L, 0, 0);
        int radius = Math.max(18, (int) Math.ceil(layout.trunk().radiusAt(400)));

        ChunkPos chunk = new ChunkPos(new net.minecraft.core.BlockPos(radius + 1, 400, 0));
        assertEquals(1, chunk.x);
        assertEquals(0, chunk.z);
    }
}
