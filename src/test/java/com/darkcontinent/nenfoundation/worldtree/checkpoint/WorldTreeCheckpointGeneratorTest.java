package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayoutGenerator;
import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeCrownGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class WorldTreeCheckpointGeneratorTest {
    @Test
    void anchorUsaChunkDeterministicoDaAltitude() {
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(42L, 0, 0);
        int anchorX = WorldTreeCheckpointGenerator.anchorPosition(
                layout, WorldTreeCheckpoint.CLOUD).getX();

        ChunkPos chunk = new ChunkPos(new net.minecraft.core.BlockPos(anchorX, 400, 0));
        assertEquals(1, chunk.x);
        assertEquals(0, chunk.z);
    }

    @Test
    void plataformaDoSummitPodeAtravessarChunkSemSerDividida() {
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(42L, 0, 0);
        BlockPos anchor = WorldTreeCheckpointGenerator.anchorPosition(
                layout, WorldTreeCheckpoint.SUMMIT);
        WorldTreePoint leader = WorldTreeCrownGenerator.leaderCenter(1450, layout.seed());

        assertEquals((int) Math.ceil(leader.x()
                + WorldTreeCrownGenerator.leaderRadius(1450)) + 2, anchor.getX());
        assertEquals((int) Math.round(leader.z()), anchor.getZ());
        assertTrue(anchor.getX() - 6 < anchor.getX());
    }
}
