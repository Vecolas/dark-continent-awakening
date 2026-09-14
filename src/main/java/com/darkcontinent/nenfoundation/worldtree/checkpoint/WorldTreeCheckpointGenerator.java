package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeClimbingPost;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Constroi a CABANA de cada checkpoint, no chunk que a toca.
 *
 * <p><b>O QUE HAVIA AQUI, e por que trocar a posicao nao bastou.</b> O checkpoint
 * era uma ancora sobre um disco de lenho de um bloco de espessura, saindo do
 * tronco. A rodada anterior consertou a POSICAO dele -- ele media o raio nominal
 * do tronco e a casca real e outra --, e o resultado em jogo continuou ruim: uma
 * tabua nua projetada de uma parede de casca le como mundo quebrado, e nao como
 * lugar. O pedido foi cabana, num galho.
 *
 * <p><b>A CABANA AFIRMA O PROPRIO VOLUME, e a prateleira nao afirmava.</b> Aquele
 * laco so preenchia AR: ele era educado com tudo o que ja estivesse no lugar, e
 * por isso saia furado quando qualquer outro gerador tivesse escrito ali. Aqui o
 * interior e <b>esvaziado</b> e as paredes sao escritas <b>por cima</b> do que
 * houver. Nao existe estado do mundo que produza meia cabana.
 *
 * <p>Isso nao dispensa a ordem em {@code buildSurface} -- o checkpoint continua
 * vindo antes da copa, porque a copa so entra em ar e uma cabana ja construida a
 * faz contornar. O que muda e que agora a cabana tambem sobrevive ao contrario.
 *
 * <p><b>CADA CHUNK ESCREVE A PROPRIA FATIA.</b> A pegada e 7x7 e cruza fronteira
 * quase sempre; nenhuma coordenada aqui depende do chunk, so do layout.
 */
public final class WorldTreeCheckpointGenerator {
    private WorldTreeCheckpointGenerator() {
    }

    public static void generate(ChunkAccess chunk, WorldTreeLayout layout) {
        // AS SETE VEM DO CACHE, e nao de sete buscas por chunk.
        //
        // `forCheckpoint` varre os 109 galhos em 41 amostras cada para achar o
        // apoio: ~4.500 avaliacoes de spline, vezes sete checkpoints, em TODO
        // chunk da dimensao -- 31 mil, sempre com o mesmo resultado. Era o mesmo
        // defeito que o cache do plano de copa ja tinha matado, cometido de novo
        // uma camada ao lado, porque conta pura parece de graca.
        List<WorldTreeClimbingPost> postos = WorldTreeClimbingPosts.of(layout);
        WorldTreeCheckpoint[] checkpoints = WorldTreeCheckpoint.values();
        for (int indice = 0; indice < checkpoints.length; indice++) {
            build(chunk, postos.get(indice),
                    checkpoints[indice] == WorldTreeCheckpoint.SUMMIT);
        }
    }

    /**
     * Levanta a cabana, na parte dela que cai neste chunk.
     *
     * @param nobre se usa cerne no lugar de lenho morto -- o SUMMIT e o fim da
     *              rota, e vale ser diferente
     */
    public static void build(ChunkAccess chunk, WorldTreeClimbingPost post, boolean nobre) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        if (!post.tocaChunk(minX, minZ)) {
            return;
        }
        BlockState piso = nobre
                ? WorldTreeBlocks.WORLD_TREE_HEARTWOOD.get().defaultBlockState()
                : WorldTreeBlocks.WORLD_TREE_DEADWOOD.get().defaultBlockState();
        BlockState parede = WorldTreeBlocks.WORLD_TREE_BARK.get().defaultBlockState();
        BlockState canto = WorldTreeBlocks.WORLD_TREE_BARK_DARK.get().defaultBlockState();
        BlockState teto = WorldTreeBlocks.WORLD_TREE_SAPWOOD.get().defaultBlockState();
        BlockState lampada = WorldTreeBlocks.WORLD_TREE_LEAVES_LUMINOUS.get().defaultBlockState();
        BlockState ar = Blocks.AIR.defaultBlockState();

        BlockPos.MutableBlockPos posicao = new BlockPos.MutableBlockPos();
        int raio = WorldTreeClimbingPost.RAIO;
        int deX = Math.max(minX, post.centerX() - raio);
        int ateX = Math.min(minX + 16, post.centerX() + raio + 1);
        int deZ = Math.max(minZ, post.centerZ() - raio);
        int ateZ = Math.min(minZ + 16, post.centerZ() + raio + 1);

        for (int x = deX; x < ateX; x++) {
            for (int z = deZ; z < ateZ; z++) {
                int dx = Math.abs(x - post.centerX());
                int dz = Math.abs(z - post.centerZ());
                boolean parede_ = dx == raio || dz == raio;
                boolean cantoDaPlanta = dx == raio && dz == raio;

                // O PILAR, antes de tudo: e ele que liga a cabana ao galho.
                //
                // Sem ele a cabana fica no ar sobre o galho -- que e o defeito
                // anterior de volta, so que maior. Ele so existe quando ha vao;
                // encostada no tronco, a cabana nao precisa de estaca.
                if (dx <= 1 && dz <= 1) {
                    for (int y = post.topoDoSuporte(); y < post.floorY(); y++) {
                        escrever(chunk, posicao, x, y, z, piso);
                    }
                }

                escrever(chunk, posicao, x, post.floorY(), z, piso);

                for (int nivel = 1; nivel <= WorldTreeClimbingPost.ALTURA_INTERNA; nivel++) {
                    int y = post.floorY() + nivel;
                    if (!parede_) {
                        // O INTERIOR E ESVAZIADO, e este e o ponto da reforma. Se
                        // a cabana cair sobre madeira de galho -- e ela cai, ela
                        // mora em cima de um --, so esvaziar produz comodo.
                        escrever(chunk, posicao, x, y, z, ar);
                        continue;
                    }
                    escrever(chunk, posicao, x, y, z, cantoDaPlanta ? canto : parede);
                }

                // OS QUATRO CANTOS DO TELHADO SAO FAROL. O poco abre o ceu em
                // cima da cabana; sao estes quatro blocos que fazem o buraco ter
                // uma luz no fundo em vez de ser so um buraco.
                escrever(chunk, posicao, x, post.roofY(), z,
                        cantoDaPlanta ? lampada : teto);
            }
        }

        // A PORTA fica na parede voltada para o eixo da arvore: quem chega vem de
        // la. Dois blocos de altura, no meio da parede.
        int portaX = post.centerX() + (post.centerX() >= 0 ? -raio : raio);
        for (int nivel = 1; nivel <= 2; nivel++) {
            escrever(chunk, posicao, portaX, post.floorY() + nivel, post.centerZ(), ar);
        }
        // UMA JANELA em cada uma das outras duas faces, para a cabana nao ser uma
        // caixa fechada vista de fora.
        escrever(chunk, posicao, post.centerX(), post.floorY() + 2, post.centerZ() - raio, ar);
        escrever(chunk, posicao, post.centerX(), post.floorY() + 2, post.centerZ() + raio, ar);

        // A LUZ, e ela e a folha luminosa de proposito: e o unico bloco que emite
        // luz nesta arvore, e a cabana e onde se olha para ele de perto.
        escrever(chunk, posicao, post.centerX(), post.roofY() - 1, post.centerZ(), lampada);

        escrever(chunk, posicao, post.centerX(), post.anchorY(), post.centerZ(),
                WorldTreeBlocks.HUNTER_CLIMBING_ANCHOR.get().defaultBlockState());
    }

    /**
     * Escreve um bloco, se ele cair neste chunk.
     *
     * <p><b>SEM PERGUNTAR SE HA AR.</b> Era assim que a prateleira antiga
     * trabalhava, e era por isso que ela saia furada. Uma construcao que respeita
     * o que encontra nao e uma construcao.
     */
    private static void escrever(ChunkAccess chunk, BlockPos.MutableBlockPos posicao,
            int x, int y, int z, BlockState estado) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        if (x < minX || x >= minX + 16 || z < minZ || z >= minZ + 16) {
            return;
        }
        if (y < chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight()) {
            return;
        }
        posicao.set(x, y, z);
        chunk.setBlockState(posicao, estado, false);
    }
}
