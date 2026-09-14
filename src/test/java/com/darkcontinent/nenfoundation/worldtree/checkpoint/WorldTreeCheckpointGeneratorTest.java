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

        // ERA `+ 2`, E ESTE TESTE COPIAVA O NUMERO. Vale registrar o que isso
        // significa: ele nao media a arvore, media a aritmetica contra ela mesma
        // -- e por isso ficou verde durante todo o tempo em que a ancora nasceu
        // flutuando. A pergunta que importa ("ela encosta na madeira?") passou a
        // ter portao proprio em AncoraEncostaNaMadeiraTest.
        assertEquals((int) Math.ceil(leader.x()
                + WorldTreeCrownGenerator.leaderRadius(1450)) + 1, anchor.getX());
        assertEquals((int) Math.round(leader.z()), anchor.getZ());
        // A plataforma alcanca nove blocos para DENTRO da madeira; era seis, e
        // afinando justo na ponta interna.
        assertTrue(anchor.getX() - 9 < anchor.getX());
    }
}
