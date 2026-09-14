package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeFoliagePlan;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeFoliageShelf;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeVineStrand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Escreve no chunk a copa que {@link WorldTreeFoliagePlan} decidiu.
 *
 * <p><b>ELE NAO DECIDE MAIS NADA DE GEOMETRIA.</b> Antes, a forma da folhagem
 * vivia dentro deste laco -- e por isso nenhum dos defeitos dela tinha regua. O
 * plano e puro e testavel; aqui so sobra a traducao para bloco.
 *
 * <p>DUAS REGRAS QUE PARECEM DETALHE E NAO SAO:
 *
 * <ol>
 *   <li><b>Folha nao sobrescreve madeira.</b> Ela so entra em ar, e por isso a
 *       ordem no gerador de chunk importa: galhos e coroa primeiro, copa depois.
 *       Invertido, a copa seria apagada pela madeira e ninguem veria erro.</li>
 *   <li><b>A densidade vem da PROFUNDIDADE, e nao de um sorteio por bloco.</b> A
 *       versao anterior descartava 62% do volume com ruido branco uniforme, o
 *       que produz confete em vez de folhagem. Aqui o miolo e solido e so a
 *       casca esgarca.</li>
 * </ol>
 */
public final class WorldTreeCanopyGenerator {

    /**
     * Onde a massa comeca a esgarcar.
     *
     * <p>Abaixo desta profundidade normalizada a folha e solida. Acima, o ruido
     * decide -- e e so nessa faixa fina que ele decide, o que da borda irregular
     * sem furar o miolo.
     */
    private static final double INICIO_DA_CASCA = 0.72;

    /**
     * O ultimo plano calculado, guardado pela seed.
     *
     * <p><b>SEM ISTO, O PLANO INTEIRO ERA REFEITO A CADA CHUNK.</b> Sao ~700
     * prateleiras e ~3000 vinhas, mais a rede de subgalhos, reconstruidas do zero
     * para cada um dos milhares de chunks da copa -- e nada disso depende do
     * chunk. Era, de longe, o maior custo da geracao da copa, e nao aparecia como
     * erro: aparecia como o mundo demorando para carregar.
     *
     * <p>UMA ENTRADA SO, e nao um mapa: existe uma World Tree por mundo. Um mapa
     * aqui seria um vazamento lento, porque nada o esvaziaria.
     *
     * <p>{@code volatile} porque a geracao de chunk e paralela. A corrida
     * possivel e benigna -- duas threads calculam o mesmo plano deterministico e
     * uma sobrescreve a outra com um valor igual.
     */
    private static volatile long planoDaSeed;
    private static volatile boolean planoValido;
    private static volatile WorldTreeFoliagePlan planoEmCache;

    private WorldTreeCanopyGenerator() {
    }

    /** O plano desta seed, calculado uma vez. */
    static WorldTreeFoliagePlan planFor(WorldTreeLayout layout) {
        if (planoValido && planoDaSeed == layout.seed()) {
            return planoEmCache;
        }
        WorldTreeFoliagePlan plan = WorldTreeFoliagePlan.of(layout,
                WorldTreeBranchNetwork.secondaryAndTertiary(layout));
        planoEmCache = plan;
        planoDaSeed = layout.seed();
        planoValido = true;
        return plan;
    }

    /**
     * Esquece o plano.
     *
     * <p>QUEM LIGA, DESLIGA. Sem isto, sair de um mundo e entrar em outro com
     * seed diferente ainda funcionaria -- a seed nao bateria e o cache seria
     * refeito --, mas o plano do mundo anterior ficaria retido na memoria pelo
     * resto da sessao, e um plano tem milhares de objetos.
     */
    public static void forget() {
        planoValido = false;
        planoEmCache = null;
    }

    public static void generate(ChunkAccess chunk, WorldTreeLayout layout) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

        WorldTreeFoliagePlan plan = planFor(layout);

        for (WorldTreeFoliageShelf shelf : plan.shelves()) {
            if (!shelfIntersectsChunk(minX, maxX, minZ, maxZ, shelf)) {
                continue;
            }
            placeShelf(chunk, position, minX, minZ, maxX, maxZ, shelf, layout.seed());
        }
        for (WorldTreeVineStrand vine : plan.vines()) {
            placeVine(chunk, position, minX, minZ, maxX, maxZ, vine);
        }
    }

    /**
     * Se vale a pena olhar esta prateleira neste chunk.
     *
     * <p>SEM ESTE CORTE, cada chunk percorreria as centenas de prateleiras do
     * plano inteiro bloco a bloco. Com ele, so as que encostam no chunk.
     */
    static boolean shelfIntersectsChunk(int minX, int maxX, int minZ, int maxZ,
            WorldTreeFoliageShelf shelf) {
        double r = shelf.radius() + 1.0;
        return minX <= shelf.centerX() + r && maxX >= shelf.centerX() - r
                && minZ <= shelf.centerZ() + r && maxZ >= shelf.centerZ() - r;
    }

    private static void placeShelf(ChunkAccess chunk, BlockPos.MutableBlockPos position,
            int minX, int minZ, int maxX, int maxZ, WorldTreeFoliageShelf shelf, long seed) {
        int fromX = Math.max(minX, (int) Math.floor(shelf.centerX() - shelf.radius()));
        int toX = Math.min(maxX, (int) Math.ceil(shelf.centerX() + shelf.radius()) + 1);
        int fromZ = Math.max(minZ, (int) Math.floor(shelf.centerZ() - shelf.radius()));
        int toZ = Math.min(maxZ, (int) Math.ceil(shelf.centerZ() + shelf.radius()) + 1);
        double raio2 = shelf.radius() * shelf.radius();
        BlockState leaf = leafState(shelf.tier());

        for (int x = fromX; x < toX; x++) {
            for (int z = fromZ; z < toZ; z++) {
                // O SPAN VERTICAL SAI DA COLUNA, e nao da espessura maxima.
                //
                // Varrer a espessura inteira em toda coluna era o que fazia a
                // copa custar caro: na borda do disco a prateleira tem meio bloco
                // de altura, e o laco visitava quarenta. Com uma centena e meia
                // de prateleiras sobre o mesmo chunk, a diferenca e engasgo.
                //
                // A conta mora em WorldTreeFoliageShelf e e a MESMA que o portao
                // de custo usa; duas versoes dela mediriam coisas diferentes.
                double dx = x - shelf.centerX();
                double dz = z - shelf.centerZ();
                double factor = shelf.verticalSpanFactor((dx * dx + dz * dz) / raio2);
                if (factor <= 0.0) {
                    continue;
                }
                int columnTop = (int) Math.ceil(shelf.centerY() + factor * shelf.topThickness());
                int columnBottom = (int) Math.floor(shelf.centerY()
                        - factor * shelf.bottomThickness());
                int fromY = Math.max(chunk.getMinBuildHeight(), columnBottom);
                int toY = Math.min(chunk.getMaxBuildHeight(), columnTop + 1);

                for (int y = fromY; y < toY; y++) {
                    double normalized = shelf.normalized(x, y, z);
                    if (normalized > 1.0 || !keep(normalized, x, y, z, seed)) {
                        continue;
                    }
                    position.set(x, y, z);
                    if (chunk.getBlockState(position).isAir()) {
                        chunk.setBlockState(position, leaf, false);
                    }
                }
            }
        }
    }

    /**
     * Se este bloco fica.
     *
     * <p>O MIOLO NUNCA E PERFURADO. Abaixo de {@link #INICIO_DA_CASCA} a resposta
     * e sim, sem consultar ruido nenhum -- e e essa decisao que separa "folhagem
     * com borda irregular" de "confete". O ruido so opera na faixa externa, e
     * ainda assim de forma CORRELACIONADA no espaco: senoides de periodo longo, e
     * nao um hash por bloco. Hash por bloco nao produz borda: produz chuvisco.
     */
    static boolean keep(double normalized, int x, int y, int z, long seed) {
        if (normalized <= INICIO_DA_CASCA) {
            return true;
        }
        double borda = (normalized - INICIO_DA_CASCA) / (1.0 - INICIO_DA_CASCA);
        double ruido = 0.5 + 0.5 * (Math.sin(x * 0.21 + seed * 0.0000013)
                * Math.cos(z * 0.19 - seed * 0.0000017)
                * Math.sin(y * 0.27 + x * 0.05));
        return ruido > borda * 0.92;
    }

    /**
     * Desenha uma cortina.
     *
     * <p>ELA NAO PARA NO PRIMEIRO BLOCO SOLIDO -- ela PULA. Parar transformaria
     * uma vinha que passa raspando num galho em duas cortinas cortadas; pular
     * mantem a leitura de cortina continua atravessando a folhagem.
     */
    private static void placeVine(ChunkAccess chunk, BlockPos.MutableBlockPos position,
            int minX, int minZ, int maxX, int maxZ, WorldTreeVineStrand vine) {
        BlockState state = vine.thick()
                ? WorldTreeBlocks.WORLD_TREE_THICK_VINE.get().defaultBlockState()
                : WorldTreeBlocks.WORLD_TREE_VINE.get().defaultBlockState();
        for (int step = 0; step < vine.length(); step++) {
            int x = (int) Math.floor(vine.xAt(step));
            int z = (int) Math.floor(vine.zAt(step));
            if (x < minX || x >= maxX || z < minZ || z >= maxZ) {
                continue;
            }
            int y = (int) Math.floor(vine.originY()) - step;
            if (y < chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight()) {
                continue;
            }
            position.set(x, y, z);
            if (chunk.getBlockState(position).isAir()) {
                chunk.setBlockState(position, state, false);
            }
        }
    }

    static BlockState leafState(int tier) {
        return switch (tier) {
            case 2 -> WorldTreeBlocks.WORLD_TREE_LEAVES_PALE.get().defaultBlockState();
            case 1 -> WorldTreeBlocks.WORLD_TREE_LEAVES_DENSE.get().defaultBlockState();
            default -> WorldTreeBlocks.WORLD_TREE_LEAVES.get().defaultBlockState();
        };
    }
}
