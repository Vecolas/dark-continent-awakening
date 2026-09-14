package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeClimbingPost;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeTrunkSurface;
import com.darkcontinent.nenfoundation.worldtree.checkpoint.WorldTreeCheckpointGenerator;
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
     * A cabana do pe da arvore.
     *
     * <p><b>O QUE HAVIA AQUI, e por que nao dava erro.</b> A ancora era posta em
     * {@code (origemX + 49, 64 + 16, origemZ)} -- tres numeros cravados -- com UM
     * bloco de lenho morto embaixo. Dezesseis blocos acima do plano da base, a um
     * deslocamento lateral que nao consulta o tronco.
     *
     * <p>A rodada anterior consertou a posicao e trocou o bloco solto por uma
     * varanda. Nao bastou: uma tabua saindo da casca continua lendo como mundo
     * quebrado. Agora e a MESMA cabana dos checkpoints -- literalmente a mesma
     * construcao, em {@code WorldTreeCheckpointGenerator.build}. Enquanto as duas
     * ficaram separadas elas ja tinham divergido, e a diferenca era um bloco
     * contra uma plataforma.
     *
     * <p>O {@code x} vem da casca de verdade ({@link WorldTreeTrunkSurface}), a
     * mesma conta que o escritor do fuste usa. O {@code y} e fixo, e por que nao
     * vem do heightmap esta em {@link #ALTURA_MINIMA}.
     */
    static void generateClimbingAnchor(ChunkAccess chunk, WorldTreeLayout layout) {
        WorldTreeCheckpointGenerator.build(chunk, postoDoPe(layout), false);
    }

    /**
     * Onde fica a cabana do pe da arvore.
     *
     * <p><b>PUBLICO PORQUE HAVIA TRES COPIAS DESTA POSICAO.</b> O gerador punha a
     * ancora em {@code origem + 49, y=80}; o teleporte de
     * {@code WorldTreeCheckpointService} mandava o jogador para
     * {@code origem + 49.5, y=80}; e a conferencia de proximidade repetia os
     * mesmos numeros uma terceira vez. Tres literais iguais, escritos em tres
     * arquivos -- e mover a cabana teria deixado duas delas apontando para o ar,
     * sem erro nenhum: o jogador so cairia.
     */
    public static WorldTreeClimbingPost postoDoPe(WorldTreeLayout layout) {
        int floorY = BASE_Y + ALTURA_MINIMA;
        double casca = WorldTreeTrunkSurface.naDirecaoX(
                raioNominalEm(floorY), floorY, layout.seed());
        return WorldTreeClimbingPost.atTreeFoot(layout, casca, floorY);
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
}
