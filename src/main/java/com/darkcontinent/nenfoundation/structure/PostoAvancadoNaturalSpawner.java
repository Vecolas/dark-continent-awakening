package com.darkcontinent.nenfoundation.structure;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Spawn natural determinístico e persistente do posto, sem duplicação em reload. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class PostoAvancadoNaturalSpawner {
    private static final String DATA_ID = "hunter_outpost_spawns";
    private static final int PERIOD = 32;

    private PostoAvancadoNaturalSpawner() { }

    @SubscribeEvent
    public static void aoCarregarChunk(ChunkEvent.Load evento) {
        if (!(evento.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        ChunkPos chunk = evento.getChunk().getPos();
        if (!isCandidate(chunk)) {
            return;
        }
        var data = level.getDataStorage().computeIfAbsent(
                PostoAvancadoSpawnData.factory(), DATA_ID);
        long chave = chunk.toLong();
        if (data.contem(chave)) {
            return;
        }
        // COLOCAR AQUI DENTRO TRAVA O SERVIDOR. O posto e maior que um chunk: o
        // setBlock do placer alcanca blocos vizinhos, e pedir um chunk vizinho DE
        // DENTRO de ChunkEvent.Load faz a Server thread esperar por ela mesma --
        // ServerChunkCache.getChunk -> managedBlock, parado para sempre. Nao e
        // excecao nem log: o servidor dedicado simplesmente nunca termina de
        // carregar o mundo, e o runGameTestServer fica pendurado sem reprovar.
        //
        // E PRECISA SER tell(), NAO execute(). BlockableEventLoop.execute so enfileira
        // quando scheduleExecutables() e verdadeiro; dentro do carregamento o servidor
        // esta reentrante, o metodo devolve falso e a tarefa roda INLINE -- ou seja,
        // execute() daqui trava igualzinho, so que com mais uma linha na pilha. O
        // primeiro conserto desta linha foi exatamente esse, e o thread dump mostrou
        // BlockableEventLoop.execute chamando o lambda na mesma pilha.
        MinecraftServer servidor = level.getServer();
        servidor.tell(new TickTask(servidor.getTickCount() + 1,
                () -> colocarQuandoOChunkEstiverEmPe(level, chunk, chave)));
    }

    /**
     * Segunda metade do spawn, ja fora do carregamento.
     *
     * <p>As duas guardas sao necessarias e por motivos diferentes: o chunk pode ter
     * sido descarregado entre o agendamento e a execucao, e o mesmo chunk pode ter
     * carregado duas vezes antes de a fila girar -- entao a checagem de duplicata e
     * refeita aqui, e nao so la em cima.</p>
     */
    private static void colocarQuandoOChunkEstiverEmPe(ServerLevel level, ChunkPos chunk, long chave) {
        if (!level.hasChunk(chunk.x, chunk.z)) {
            return;
        }
        var data = level.getDataStorage().computeIfAbsent(
                PostoAvancadoSpawnData.factory(), DATA_ID);
        if (data.contem(chave)) {
            return;
        }
        int centroX = chunk.getMinBlockX() + 8;
        int centroZ = chunk.getMinBlockZ() + 8;
        var resultado = PostoAvancadoWorldPlacer.colocar(level, centroX, centroZ,
                PostoAvancadoPlacementTransform.Rotacao.values()[Math.floorMod(chunk.x + chunk.z, 4)]);
        if (!resultado.rejeitado()) {
            data.registrar(chave);
        }
    }

    static boolean isCandidate(ChunkPos chunk) {
        return Math.floorMod(chunk.x * 31 + chunk.z, PERIOD) == 0;
    }
}
