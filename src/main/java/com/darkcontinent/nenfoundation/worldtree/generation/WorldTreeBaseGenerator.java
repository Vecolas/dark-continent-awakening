package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeAnchorPlatform;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeTrunkSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Parcela idempotente da base Overworld pertencente ao chunk atual. */
public final class WorldTreeBaseGenerator {
    private static final int BASE_Y = 64;

    /**
     * O raio nominal do fuste do Overworld na altura da ancora.
     *
     * <p>Ele SAI do mesmo par de numeros que {@code WorldTreeTrunkGenerator}
     * usa para desenhar o fuste -- 48 na base, 30 a 220 blocos de altura. Uma
     * constante propria aqui divergiria no dia em que o fuste mudasse, e o
     * sintoma seria a ancora voltando a flutuar.
     */
    static final double RAIO_BASE = 48.0;
    static final double RAIO_TOPO = 30.0;
    static final int ALTURA_DO_FUSTE = 220;

    /**
     * Quantos blocos acima do plano da base a ancora nasce.
     *
     * <p><b>DOIS, FIXO -- E O HEIGHTMAP FOI RECUSADO DE PROPOSITO.</b> A primeira
     * versao deste conserto perguntava a altura do TERRENO, que parece o certo:
     * assim a varanda acompanha o relevo em vez de flutuar.
     *
     * <p>So que o heightmap so responde por colunas do chunk ATUAL, e a varanda
     * tem 13 por 9 blocos -- ela cruza fronteira de chunk quase sempre. Chunks
     * vizinhos, gerados em momentos diferentes, leriam alturas diferentes e cada
     * um desenharia o seu pedaco num nivel proprio: uma plataforma PARTIDA EM
     * DEGRAUS na fronteira. Seria trocar "flutuando" por "quebrada", e a segunda
     * e mais dificil de atribuir a causa.
     *
     * <p>Fixo, a varanda pousa sobre a saia da base do fuste -- que o proprio
     * gerador escreve ate y=65 em toda a volta, e portanto e solida e
     * deterministica. Custo declarado: num relevo alto o balcao pode nascer
     * dentro da encosta. Esta em o-que-nao-provamos.md.
     */
    private static final int ALTURA_MINIMA = 2;

    private WorldTreeBaseGenerator() {
    }

    public static void generateChunk(ChunkAccess chunk, WorldTreeLayout layout) {
        WorldTreeTrunkGenerator.generateBase(chunk, layout, BASE_Y);
        WorldTreeRootGenerator.generate(chunk, layout, BASE_Y);
        generateClimbingAnchor(chunk, layout);
    }

    /**
     * A ancora do pe da arvore, presa na casca.
     *
     * <p><b>O QUE HAVIA AQUI, e por que nao dava erro.</b> A ancora era posta em
     * {@code (origemX + 49, 64 + 16, origemZ)} -- tres numeros cravados -- com UM
     * bloco de lenho morto embaixo. Dezesseis blocos acima do plano da base, a um
     * deslocamento lateral que nao consulta o tronco: dependendo da seed ela
     * nascia enterrada no fuste ou boiando metros longe dele, sempre com um unico
     * bloco pendurado por baixo. Em jogo, "flutuando e com a base bugada".
     *
     * <p>Agora as duas coordenadas vem de onde deveriam:
     *
     * <ul>
     *   <li><b>o x</b> da casca de verdade ({@link WorldTreeTrunkSurface}), a
     *       mesma conta que o escritor do fuste usa -- e nao de um 49;</li>
     *   <li><b>o y</b> de um degrau fixo acima do plano da base -- e nao de um
     *       16 cravado. Por que nao do heightmap, que seria o obvio, esta escrito
     *       em {@link #ALTURA_MINIMA}.</li>
     * </ul>
     *
     * <p>E ela ganha a mesma varanda dos checkpoints, pelo mesmo motivo: e a
     * plataforma que atravessa para dentro da madeira que impede a ancora de
     * ficar solta. Um bloco de lenho embaixo nao e base, e um pingente.
     */
    static void generateClimbingAnchor(ChunkAccess chunk, WorldTreeLayout layout) {
        int anchorY = BASE_Y + ALTURA_MINIMA;
        double nominal = raioNominalEm(anchorY);
        double casca = WorldTreeTrunkSurface.naDirecaoX(nominal, anchorY, layout.seed());
        BlockPos anchor = new BlockPos(
                layout.overworldOriginX() + (int) Math.ceil(casca) + 1,
                anchorY,
                layout.overworldOriginZ());

        if (!plataformaTocaOChunk(chunk, anchor)) {
            return;
        }
        // A MESMA VARANDA DOS CHECKPOINTS, literalmente: a forma mora em
        // WorldTreeAnchorPlatform. Antes, este lugar tinha UM bloco de lenho
        // morto embaixo da ancora -- que e a duplicacao ja divergida.
        for (int dx = -WorldTreeAnchorPlatform.ALCANCE_PARA_DENTRO;
                dx <= WorldTreeAnchorPlatform.ALCANCE_PARA_FORA; dx++) {
            for (int dz = -WorldTreeAnchorPlatform.ALCANCE_LATERAL;
                    dz <= WorldTreeAnchorPlatform.ALCANCE_LATERAL; dz++) {
                if (!WorldTreeAnchorPlatform.contem(dx, dz)) {
                    continue;
                }
                BlockPos apoio = anchor.below().offset(dx, 0, dz);
                if (dentroDoChunk(chunk, apoio) && chunk.getBlockState(apoio).isAir()) {
                    chunk.setBlockState(apoio,
                            WorldTreeBlocks.WORLD_TREE_DEADWOOD.get().defaultBlockState(), false);
                }
            }
        }
        if (chunk.getPos().equals(new ChunkPos(anchor))) {
            chunk.setBlockState(anchor, WorldTreeBlocks.HUNTER_CLIMBING_ANCHOR.get()
                    .defaultBlockState(), false);
        }
    }

    /**
     * O raio nominal do fuste do Overworld naquela altura.
     *
     * <p>PUBLICO para o portao, que mora noutro pacote e precisa perguntar a
     * mesma coisa que este gerador pergunta. Uma copia da conta la seria a
     * divergencia de sempre: o teste aprovaria uma ancora que o gerador poe
     * noutro lugar.
     */
    public static double raioNominalEm(int y) {
        double t = Math.max(0.0, Math.min(1.0,
                (double) (y - BASE_Y) / ALTURA_DO_FUSTE));
        return RAIO_BASE + (RAIO_TOPO - RAIO_BASE) * t;
    }

    private static boolean dentroDoChunk(ChunkAccess chunk, BlockPos pos) {
        return chunk.getPos().equals(new ChunkPos(pos));
    }

    private static boolean plataformaTocaOChunk(ChunkAccess chunk, BlockPos anchor) {
        int minX = WorldTreeAnchorPlatform.pontaInterna(anchor.getX());
        int maxX = anchor.getX() + WorldTreeAnchorPlatform.ALCANCE_PARA_FORA;
        int minZ = anchor.getZ() - WorldTreeAnchorPlatform.ALCANCE_LATERAL;
        int maxZ = anchor.getZ() + WorldTreeAnchorPlatform.ALCANCE_LATERAL;
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        return chunkMinX <= maxX && chunkMinX + 15 >= minX
                && chunkMinZ <= maxZ && chunkMinZ + 15 >= minZ;
    }
}
