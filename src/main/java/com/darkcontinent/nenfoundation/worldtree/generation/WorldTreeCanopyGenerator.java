package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeFoliagePlan;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeFoliageIndex;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeFoliageShelf;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeFoliageTexture;
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
     * Quantos blocos de profundidade a folha LUMINOSA alcanca, a partir de cada
     * face da massa.
     *
     * <p><b>DOIS, E O NUMERO E DE ILUMINACAO, NAO DE ESTILO.</b> A copa e macica
     * no miolo: luz nao atravessa bloco solido, entao uma folha luminosa
     * enterrada a dez blocos de profundidade nao clareia nada que alguem veja.
     * Ela continua custando uma propagacao de luz inteira na engine -- que e um
     * BFS por fonte --, e a copa tem dezenas de milhoes de folhas.
     *
     * <p>Dois e o que sobra quando se pergunta "quantas camadas um observador
     * chega a ver": a de fora, e a que aparece pelos buracos da erosao de casca.
     * A TERCEIRA nunca e vista e nunca ilumina.
     *
     * <p>Isto NAO mexe na cobertura: o campo que sorteia quais folhas brilham
     * continua o mesmo, e a mancha na superficie continua do tamanho que era. O
     * que sai e so o que estava enterrado.
     */
    private static final int PROFUNDIDADE_LUMINOSA = 2;

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
     * <p><b>UM CAMPO SO, E NAO TRES.</b> Havia aqui {@code planoDaSeed},
     * {@code planoValido} e {@code planoEmCache} em tres {@code volatile}
     * separados. Cada um era atomico sozinho, e o CONJUNTO nao: {@code forget()}
     * podia zerar o plano entre a leitura da bandeira e a leitura da referencia
     * na thread de worldgen, devolvendo {@code null} -- e {@code null} ali vira
     * NPE dentro da geracao de chunk, que nao e um efeito visual falhando, e sim
     * o chunk inteiro falhando.
     *
     * <p>Com um registro imutavel num campo so, a leitura e atomica por
     * construcao. A corrida que sobra e benigna: duas threads calculam o mesmo
     * plano deterministico e uma sobrescreve a outra com um valor igual.
     */
    private record PlanoEmCache(long seed, WorldTreeFoliagePlan plan) {
    }

    private static volatile PlanoEmCache cache;

    private WorldTreeCanopyGenerator() {
    }

    /** O plano desta seed, calculado uma vez. */
    static WorldTreeFoliagePlan planFor(WorldTreeLayout layout) {
        PlanoEmCache atual = cache;
        if (atual != null && atual.seed() == layout.seed()) {
            return atual.plan();
        }
        WorldTreeFoliagePlan plan = WorldTreeFoliagePlan.of(layout,
                WorldTreeBranchNetwork.secondaryAndTertiary(layout));
        cache = new PlanoEmCache(layout.seed(), plan);
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
        cache = null;
    }

    public static void generate(ChunkAccess chunk, WorldTreeLayout layout) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

        WorldTreeFoliagePlan plan = planFor(layout);

        // O INDICE RESPONDE QUEM ENCOSTA NESTE CHUNK, e este laco nao ve mais o
        // plano inteiro.
        //
        // O corte por chunk sempre existiu e sempre esteve CERTO -- so que era um
        // `if` dentro de um laco sobre as 549 prateleiras e as 3.070 vinhas. Todo
        // chunk da dimensao pagava 3.619 testes de caixa antes de descobrir que
        // nao tinha nada a desenhar, e um chunk a 5.000 blocos do tronco pagava
        // exatamente o mesmo que o chunk em cima dele.
        WorldTreeFoliageIndex index = plan.index();
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        for (int shelfIndex : index.prateleirasEm(chunkX, chunkZ)) {
            placeShelf(chunk, position, minX, minZ, maxX, maxZ,
                    plan.shelves().get(shelfIndex), layout.seed());
        }
        for (int vineIndex : index.vinhasEm(chunkX, chunkZ)) {
            placeVine(chunk, position, minX, minZ, maxX, maxZ,
                    plan.vines().get(vineIndex));
        }
    }

    private static void placeShelf(ChunkAccess chunk, BlockPos.MutableBlockPos position,
            int minX, int minZ, int maxX, int maxZ, WorldTreeFoliageShelf shelf, long seed) {
        int fromX = Math.max(minX, (int) Math.floor(shelf.centerX() - shelf.radius()));
        int toX = Math.min(maxX, (int) Math.ceil(shelf.centerX() + shelf.radius()) + 1);
        int fromZ = Math.max(minZ, (int) Math.floor(shelf.centerZ() - shelf.radius()));
        int toZ = Math.min(maxZ, (int) Math.ceil(shelf.centerZ() + shelf.radius()) + 1);
        double raio2 = shelf.radius() * shelf.radius();
        BlockState leaf = leafState(shelf.tier());
        BlockState luminous = WorldTreeBlocks.WORLD_TREE_LEAVES_LUMINOUS.get().defaultBlockState();
        // A coluna mais alta que esta prateleira pode produzir, com folga de dois
        // blocos para o arredondamento das bordas do laco de y.
        int[] coluna = new int[(int) Math.ceil(shelf.topThickness() + shelf.bottomThickness()) + 4];

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
                double horizontalSquared = (dx * dx + dz * dz) / raio2;
                double factor = shelf.verticalSpanFactor(horizontalSquared);
                if (factor <= 0.0) {
                    continue;
                }
                int columnTop = (int) Math.ceil(shelf.centerY() + factor * shelf.topThickness());
                int columnBottom = (int) Math.floor(shelf.centerY()
                        - factor * shelf.bottomThickness());
                int fromY = Math.max(chunk.getMinBuildHeight(), columnBottom);
                int toY = Math.min(chunk.getMaxBuildHeight(), columnTop + 1);

                // OS TERMOS QUE SO DEPENDEM DA COLUNA SAEM DO LACO DE y.
                //
                // Os dois campos de ruido tem um termo em (x, z) e dois que
                // envolvem y. O termo de coluna era recalculado em cada um dos
                // ~200 milhoes de blocos da copa, sempre com o mesmo resultado.
                // Nao ha uma segunda implementacao aqui: as funcoes de
                // conveniencia calculam este termo e delegam para as mesmas
                // linhas.
                double termoDaCasca = cascaNaColuna(x, z, seed);
                double termoDoBrilho = WorldTreeFoliageTexture.termoDaColuna(x, z, seed);

                // A COLUNA E COLETADA ANTES DE SER ESCRITA, e o motivo e a folha
                // luminosa.
                //
                // Ela so pode existir na CASCA da massa -- os dois blocos mais
                // externos de cada coluna. Luz 15 atravessa no maximo 15 blocos
                // de ar e para no primeiro solido: uma folha luminosa a dez
                // blocos de profundidade nao ilumina nada que alguem veja, e
                // mesmo assim paga uma propagacao inteira na engine.
                //
                // "Os dois mais externos" nao da para saber varrendo de baixo
                // para cima: quais blocos ficam depende de `keep`, que esgarca a
                // borda. Os limites geometricos da coluna nao servem -- eles sao
                // justamente onde a erosao morde mais. Entao a coluna e coletada
                // e so depois escrita.
                int quantos = 0;
                for (int y = fromY; y < toY; y++) {
                    double normalized = shelf.normalizedInColumn(horizontalSquared, y);
                    if (normalized > 1.0 || !keep(normalized, termoDaCasca, x, y, z, seed)) {
                        continue;
                    }
                    coluna[quantos++] = y;
                }
                for (int indice = 0; indice < quantos; indice++) {
                    int y = coluna[indice];
                    position.set(x, y, z);
                    if (!chunk.getBlockState(position).isAir()) {
                        continue;
                    }
                    boolean naCasca = indice < PROFUNDIDADE_LUMINOSA
                            || indice >= quantos - PROFUNDIDADE_LUMINOSA;
                    chunk.setBlockState(position,
                            naCasca && WorldTreeFoliageTexture.brilha(termoDoBrilho, x, y, z, seed)
                                    ? luminous : leaf,
                            false);
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
        return keep(normalized, cascaNaColuna(x, z, seed), x, y, z, seed);
    }

    /**
     * O termo do ruido de casca que so depende da coluna.
     *
     * <p>Existe para sair do laco de y, e nao para ser uma segunda versao da
     * conta: {@link #keep(double, int, int, int, long)} chama esta funcao e
     * delega. O portao {@code cascaNaColunaConcordaComKeep} amarra os dois.
     */
    static double cascaNaColuna(int x, int z, long seed) {
        return WorldTreeFoliageTexture.sin(x * 0.21 + z * 0.13 + seed * 0.0000013);
    }

    /** A mesma decisao, com o termo da coluna ja calculado. */
    static boolean keep(double normalized, double termoDaColuna,
            int x, int y, int z, long seed) {
        if (normalized <= INICIO_DA_CASCA) {
            return true;
        }
        double borda = (normalized - INICIO_DA_CASCA) / (1.0 - INICIO_DA_CASCA);
        // SOMA, E NAO PRODUTO -- e esta troca conserta um artefato visivel.
        //
        // O ruido era `sin(x) * cos(z) * sin(y)`: um produto SEPARAVEL. Quando o
        // primeiro fator passa por zero -- a cada ~15 blocos em X --, o produto
        // inteiro zera para TODO z e TODO y, e a borda da folhagem desaparece numa
        // LAJE inteira. Em tela isso le como costura de chunk, que e o pior
        // artefato possivel aqui: quem visse iria procurar o defeito na geracao
        // por chunk, que esta certa.
        //
        // Numa soma, um termo no zero nao apaga os outros. As frequencias sao
        // incomensuraveis de proposito, para o padrao nao se repetir em grade.
        //
        // O SENO E TABELADO, e a troca nao e so de custo: `Math.sin` so promete
        // 1 ulp e pode diferir entre JVMs, e 1 ulp perto do limiar troca a
        // decisao de um bloco. Ver WorldTreeFoliageTexture.
        double ruido = 0.5 + (termoDaColuna
                + WorldTreeFoliageTexture.sin(z * 0.19 - y * 0.11 - seed * 0.0000017)
                + WorldTreeFoliageTexture.sin(y * 0.27 + x * 0.057)) / 6.0;
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
