package com.darkcontinent.nenfoundation.worldtree;

import java.util.ArrayList;
import java.util.List;

/**
 * ONDE ha copa, decidido antes de existir chunk.
 *
 * <p><b>POR QUE ESTE ARQUIVO EXISTE.</b> Ate aqui, a geometria da folhagem so
 * existia dentro do laco que escrevia blocos. Consequencia: nao havia como medir
 * cobertura, sobreposicao, ancoragem ou volume sem subir o jogo -- e por isso os
 * quatro defeitos que faziam a copa sumir nao tinham regua nenhuma. Um deles
 * (prateleira nascendo dentro da madeira) sobreviveu a varias tentativas de
 * conserto porque ninguem conseguia OBSERVAR o problema.
 *
 * <p><b>SEM MINECRAFT, DE PROPOSITO.</b> Nao ha um unico tipo do jogo aqui. E o
 * que permite ao portao rodar as vinte seeds em JUnit, em milissegundos.
 *
 * <p><b>DETERMINISTICO.</b> Mesma seed, mesmo plano, bit a bit. Um chunk nao sabe
 * o que o vizinho gerou; a unica coisa que os mantem coerentes e a seed. Qualquer
 * sorteio nao semeado aqui produziria copa cortada na fronteira de chunk.
 */
public final class WorldTreeFoliagePlan {

    /**
     * Prateleiras por galho maior.
     *
     * <p>SETE, E NAO CINCO. Com cinco, o portao da luva reprovou: num galho de
     * 200 blocos, as prateleiras ficavam a ~32 blocos uma da outra com raio ~20,
     * e a sobreposicao caia para 0,20 -- dedos. A contagem e o que controla o
     * espacamento, porque os t sao distribuidos entre o primeiro e o ultimo.
     */
    private static final int PRATELEIRAS_POR_GALHO_MAIOR = 9;

    /**
     * Prateleiras por subgalho.
     *
     * <p>QUATRO, e o numero saiu do portao da luva -- nao de gosto. Com tres, um
     * subgalho de 78 blocos espacava os discos em ~22 com raio ~13 cada, e a
     * sobreposicao caia para 0,30. Cada contagem aqui e um espacamento, porque os
     * t sao distribuidos entre o primeiro e o ultimo.
     */
    private static final int PRATELEIRAS_POR_SUBGALHO = 4;

    /**
     * Onde a primeira prateleira do galho maior nasce.
     *
     * <p>NAO E ZERO: perto do tronco o galho e grosso demais e a copa taparia o
     * proprio eixo. Comeca depois do primeiro terco, que e onde a referencia
     * mostra a folhagem descolando da madeira.
     */
    private static final double PRIMEIRO_T_MAIOR = 0.34;
    private static final double ULTIMO_T_MAIOR = 0.98;
    private static final double PRIMEIRO_T_SUB = 0.42;
    private static final double ULTIMO_T_SUB = 1.0;

    /**
     * Onde a copa comeca a existir de verdade, e onde ela chega ao maximo.
     *
     * <p><b>A SILHUETA ERA DE PINHEIRO, e so a projecao lateral mostrou.</b> Os
     * galhos nascem distribuidos por cinco zonas, de y=320 a y=1480 -- e uma
     * prateleira igual em cada uma produz um cone de andares uniformes, que le
     * como conifera. A referencia e o oposto: tronco quase limpo embaixo, e uma
     * coroa LARGA concentrada no terco superior.
     *
     * <p>Nao da para consertar isso mexendo no galho sem mexer nos checkpoints e
     * na ecologia, que leem as mesmas splines. Da para consertar na FOLHAGEM: os
     * galhos de baixo ficam com madeira quase nua -- o que a referencia tambem
     * mostra --, e a massa vai para o alto.
     */
    private static final double ALTURA_DO_PE_DA_COPA = 420.0;
    private static final double ALTURA_DO_AUGE = 1240.0;

    /** Quanto a prateleira encolhe no pe da copa, e cresce no auge. */
    private static final double ESCALA_NO_PE = 0.30;
    private static final double ESCALA_NO_AUGE = 2.45;

    /**
     * Quanto o raio da prateleira encolhe da base para a ponta do galho.
     *
     * <p>SEM ISSO A COPA VIRA HALTERE: discos do mesmo tamanho ate a ponta leem
     * como uma barra com pesos, e nao como copa.
     */
    private static final double ENCOLHIMENTO_NA_PONTA = 0.45;

    /**
     * O quanto duas prateleiras vizinhas PRECISAM se sobrepor, em fracao da soma
     * dos raios.
     *
     * <p>ESTE NUMERO E O ANTIDOTO DA LUVA. Prateleiras enfileiradas sem
     * sobreposicao leem como dedos -- foi o resultado de duas tentativas
     * anteriores. Com 0.35, o disco seguinte comeca bem antes de o anterior
     * acabar, e a massa vira uma copa continua.
     */
    public static final double SOBREPOSICAO_MINIMA = 0.35;

    /**
     * Teto de prateleiras no plano inteiro.
     *
     * <p>TRAVA DE SEGURANCA, E NAO AJUSTE -- e o portao verifica que ela NUNCA E
     * ALCANCADA nas vinte seeds. A distincao importa: um teto que corta de
     * verdade truncaria a cauda da lista em silencio, e quem some primeiro e a
     * copa da coroa. Por isso a coroa entra PRIMEIRO na lista.
     */
    public static final int TETO_DE_PRATELEIRAS = 4000;

    /** Teto de vinhas, com a mesma regra: existe, e nao deve morder. */
    public static final int TETO_DE_VINHAS = 16000;

    private final List<WorldTreeFoliageShelf> shelves;
    private final List<WorldTreeVineStrand> vines;

    private WorldTreeFoliagePlan(List<WorldTreeFoliageShelf> shelves,
            List<WorldTreeVineStrand> vines) {
        this.shelves = List.copyOf(shelves);
        this.vines = List.copyOf(vines);
    }

    public List<WorldTreeFoliageShelf> shelves() {
        return shelves;
    }

    public List<WorldTreeVineStrand> vines() {
        return vines;
    }

    /**
     * O plano completo de um layout.
     *
     * <p>Recebe os galhos maiores E os derivados; quem chama junta as duas listas,
     * porque a derivacao mora no pacote de geracao e este arquivo nao depende
     * dele.
     */
    public static WorldTreeFoliagePlan of(WorldTreeLayout layout,
            List<WorldTreeSpline> derivedBranches) {
        List<WorldTreeFoliageShelf> shelves = new ArrayList<>();
        List<WorldTreeVineStrand> vines = new ArrayList<>();

        // A COROA ENTRA PRIMEIRO, e nao por estilo: se o teto de seguranca
        // algum dia morder, ele corta a CAUDA da lista. A copa do topo e a que
        // menos pode sumir, porque e a que a referencia mais mostra.
        addCrownShelves(shelves, layout, 0);

        int branchId = 1;
        for (WorldTreeSpline branch : layout.branches()) {
            addShelves(shelves, branch, layout.seed(), branchId++,
                    PRATELEIRAS_POR_GALHO_MAIOR, PRIMEIRO_T_MAIOR, ULTIMO_T_MAIOR, 1.0);
        }
        for (WorldTreeSpline branch : derivedBranches) {
            addShelves(shelves, branch, layout.seed(), branchId++,
                    PRATELEIRAS_POR_SUBGALHO, PRIMEIRO_T_SUB, ULTIMO_T_SUB, 1.0);
        }
        List<WorldTreeFoliageShelf> limitadas = shelves.size() > TETO_DE_PRATELEIRAS
                ? List.copyOf(shelves.subList(0, TETO_DE_PRATELEIRAS))
                : List.copyOf(shelves);
        for (int index = 0; index < limitadas.size(); index++) {
            addVines(vines, limitadas.get(index), layout.seed(), index);
        }
        List<WorldTreeVineStrand> vinhas = vines.size() > TETO_DE_VINHAS
                ? List.copyOf(vines.subList(0, TETO_DE_VINHAS))
                : List.copyOf(vines);
        return new WorldTreeFoliagePlan(limitadas, vinhas);
    }

    private static void addShelves(List<WorldTreeFoliageShelf> out, WorldTreeSpline branch,
            long seed, int branchId, int count, double firstT, double lastT, double scale) {
        double step = count > 1 ? (lastT - firstT) / (count - 1) : 0.0;
        for (int order = 0; order < count; order++) {
            double t = firstT + step * order;
            WorldTreePoint axis = branch.pointAt(t);
            double branchRadius = branch.radiusAt(t);
            long mixed = mix(seed, branchId, order);

            // O RAIO ENCOLHE EM DIRECAO A PONTA, e cresce com a grossura do
            // galho: galho grosso sustenta prateleira grande, e e isso que faz a
            // silhueta afunilar como arvore em vez de haltere.
            double taper = 1.0 - ENCOLHIMENTO_NA_PONTA * t;
            // A ALTITUDE MANDA NO TAMANHO. E o que separa "conifera de andares
            // iguais" de "coroa concentrada no alto", e foi a projecao lateral
            // que mostrou -- nenhum dos portoes numericos reprovava o pinheiro,
            // porque cada prateleira, sozinha, estava correta.
            double radius = (27.0 + branchRadius * 2.1 + unit(mixed) * 9.0)
                    * taper * scale * escalaPorAltitude(axis.y());

            // O PISO E RELATIVO AO GALHO, e nao um numero fixo -- e este foi o
            // erro que a escala por altitude introduziu.
            //
            // Encolher a prateleira para 30% no pe da copa produziu discos de
            // raio 14 sobre galhos de raio 10: a folha mal escapava da madeira, e
            // era o DEFEITO ORIGINAL de volta, so que agora limitado aos galhos
            // de baixo. Cinco portoes reprovaram de uma vez, e todos apontavam a
            // mesma coisa -- a prateleira parou de ser grande em relacao ao que a
            // sustenta.
            //
            // Um piso absoluto (`Math.max(8.0, ...)`, que era o que havia aqui)
            // nao resolve: ele nao sabe a grossura do galho, e por isso deixava
            // passar disco pequeno sobre galho grosso E achatava o taper nas
            // pontas finas, onde os dois extremos batiam no mesmo piso.
            double piso = branchRadius * 2.2 + 6.0;
            radius = Math.max(piso, radius);

            // A PRATELEIRA VESTE O GALHO, e nao pousa em cima dele.
            //
            // Aqui moravam DOIS defeitos opostos, e o segundo nasceu ao consertar
            // o primeiro. O original centrava a massa no eixo com raio vertical
            // MENOR que o do proprio galho: a prateleira inteira ficava dentro da
            // madeira e, como folha so entra em ar, nao virava bloco nenhum.
            //
            // A primeira correcao empurrou o centro para `raioDoGalho * 0.55`
            // acima do eixo -- e ai o portao acusou o inverso: com galho de 30 de
            // raio, a massa comecava 4 blocos ACIMA da madeira e ficava
            // pendurada, sem nada por baixo.
            //
            // A forma certa e a terceira: o centro sobe pouco, e a SAIA DESCE
            // ABAIXO DO EIXO. O galho atravessa a massa, e o anel de folha que
            // sobra por fora dele -- o raio e varias vezes maior -- e o que se ve.
            double lift = branchRadius * 0.35 + 1.5;
            double centerY = axis.y() + lift;

            // MAIS ESPESSA QUE ANTES, e por um motivo de leitura: com 0,26 as
            // prateleiras ficavam separadas por vao de ar e a copa se lia como
            // uma pilha de discos. A referencia le como MASSA continua com a
            // borda em degraus, e para isso os andares precisam quase se tocar.
            double topThickness = Math.max(3.0, radius * (0.29 + unit(mixed >>> 12) * 0.10));
            // A DE BAIXO PRECISA PASSAR DO EIXO. Zero aqui seria "folha so por
            // cima" -- copa como guarda-sol --, e qualquer valor menor que o
            // `lift` deixaria o galho para fora da massa por baixo.
            // O `Math.max` de tres termos guarda tres coisas diferentes, e cada
            // uma foi um teste reprovando: um piso absoluto, a proporcao com a
            // face de cima (senao a saia some nos galhos finos e a copa vira
            // guarda-sol), e a garantia de que a saia PASSA do eixo do galho.
            double bottomThickness = Math.max(Math.max(2.0, topThickness * 0.45),
                    lift + branchRadius * 0.30);

            // Um deslocamento lateral pequeno, para a fila de discos nao ficar
            // reta demais. Limitado a uma fracao do raio para nao criar ilha.
            double heading = branch.headingRadians();
            double lateral = signed(mixed >>> 24) * radius * 0.22;
            double centerX = axis.x() - Math.sin(heading) * lateral;
            double centerZ = axis.z() + Math.cos(heading) * lateral;

            out.add(new WorldTreeFoliageShelf(centerX, centerY, centerZ,
                    radius, topThickness, bottomThickness,
                    axis.x(), axis.y(), axis.z(), branchRadius,
                    WorldTreeFoliageShelf.Suporte.GALHO,
                    branchId, order, tierFor(centerY)));
        }
    }

    /**
     * A copa do lider central, em degraus que estreitam ate o topo.
     *
     * <p>Ela NAO usa spline: o lider e vertical, e uma prateleira por faixa de
     * altura le melhor do que discos pendurados num galho imaginario.
     */
    private static void addCrownShelves(List<WorldTreeFoliageShelf> out,
            WorldTreeLayout layout, int branchId) {
        int bottom = 1120;
        int top = 1460;
        int steps = 10;
        for (int order = 0; order < steps; order++) {
            double t = (double) order / (steps - 1);
            double y = bottom + (top - bottom) * t;
            long mixed = mix(layout.seed(), branchId, order);
            double radius = (168.0 - 118.0 * t) * (0.9 + unit(mixed) * 0.2);
            double topThickness = Math.max(3.0, radius * 0.22);
            double bottomThickness = Math.max(2.5, topThickness * 0.66);
            double centerX = signed(mixed >>> 16) * 4.0;
            double centerZ = signed(mixed >>> 32) * 4.0;
            // O LIDER CENTRAL tem raio proprio, que estreita com a altura --
            // o mesmo perfil que WorldTreeCrownGenerator desenha em madeira.
            double leaderRadius = 19.0 - 12.0 * Math.max(0.0, (y - 1100.0) / 350.0);
            out.add(new WorldTreeFoliageShelf(centerX, y, centerZ,
                    radius, topThickness, bottomThickness,
                    0.0, y, 0.0, Math.max(0.0, leaderRadius),
                    WorldTreeFoliageShelf.Suporte.LIDER,
                    branchId, order, tierFor(y)));
        }
    }

    private static void addVines(List<WorldTreeVineStrand> out, WorldTreeFoliageShelf shelf,
            long seed, int shelfIndex) {
        // Quantas cortinas cabem: proporcional ao raio, com teto.
        int count = (int) Math.min(9.0, 2.0 + shelf.radius() / 7.0);
        for (int strand = 0; strand < count; strand++) {
            long mixed = mix(seed ^ 0x5DEECE66DL, shelfIndex, strand);
            // NASCEM PERTO DA BORDA, e nao no centro: cortina saindo do meio da
            // massa fica escondida dentro da propria prateleira.
            double angle = unit(mixed) * Math.PI * 2.0;
            double fraction = 0.55 + unit(mixed >>> 12) * 0.4;
            double distance = shelf.radius() * fraction;
            double originX = shelf.centerX() + Math.cos(angle) * distance;
            double originZ = shelf.centerZ() + Math.sin(angle) * distance;

            // A FACE DE BAIXO NAQUELE PONTO, e nao a do centro: no anel externo a
            // prateleira e mais fina, e uma vinha nascendo na altura do centro
            // comecaria dentro da folhagem em vez de pendurada nela.
            // A ALTURA DA FACE DE BAIXO SAI DA MESMA FUNCAO que o desenho usa.
            // Ela tinha uma copia da conta aqui, com `Math.pow(..., 1/4)`; duas
            // versoes da mesma curva divergem, e a divergencia apareceria como
            // vinha nascendo dentro da folhagem em vez de pendurada nela.
            double halfHeight = shelf.bottomThickness()
                    * shelf.verticalSpanFactor(fraction * fraction);
            double originY = shelf.centerY() - halfHeight;

            int length = WorldTreeVineStrand.COMPRIMENTO_MINIMO
                    + (int) (unit(mixed >>> 24)
                            * (WorldTreeVineStrand.COMPRIMENTO_MAXIMO
                                    - WorldTreeVineStrand.COMPRIMENTO_MINIMO));
            boolean thick = Math.floorMod(mixed >>> 40, 7L) == 0L;
            out.add(new WorldTreeVineStrand(originX, originY, originZ, length,
                    signed(mixed >>> 44) * 2.5, signed(mixed >>> 48) * 2.5, thick, shelfIndex));
        }
    }

    /**
     * O quanto a copa cresce nesta altitude.
     *
     * <p>Sobe do pe ao auge e volta a cair um pouco no topo, porque a coroa da
     * referencia arredonda em vez de terminar num prato. A curva e suave de
     * proposito: um degrau aqui apareceria como um andar deslocado dos vizinhos.
     */
    static double escalaPorAltitude(double y) {
        if (y <= ALTURA_DO_PE_DA_COPA) {
            return ESCALA_NO_PE;
        }
        if (y >= ALTURA_DO_AUGE) {
            // Acima do auge, encolhe devagar: e a cupula fechando.
            // A CUPULA FECHA DE VERDADE. Com queda de 45% o topo saia quase tao
            // largo quanto o auge, e a copa terminava num PRATO -- da para ver na
            // projecao lateral. A referencia arredonda: o ultimo terco estreita
            // rapido, e e o que da a leitura de domo em vez de mesa.
            double alem = Math.min(1.0, (y - ALTURA_DO_AUGE) / 240.0);
            return ESCALA_NO_AUGE * (1.0 - 0.78 * alem * alem);
        }
        double t = (y - ALTURA_DO_PE_DA_COPA) / (ALTURA_DO_AUGE - ALTURA_DO_PE_DA_COPA);
        // Curva em S: o pe fica limpo por mais tempo e a copa "abre" no alto.
        double suave = t * t * (3.0 - 2.0 * t);
        return ESCALA_NO_PE + (ESCALA_NO_AUGE - ESCALA_NO_PE) * suave;
    }

    /** A faixa de altitude, que escolhe qual das tres folhas registradas usar. */
    public static int tierFor(double y) {
        if (y >= 1340.0) {
            return 2;
        }
        return y >= 820.0 ? 1 : 0;
    }

    /**
     * Quantas prateleiras encostam num chunk de 16x16 em {@code (chunkX, chunkZ)}.
     *
     * <p>ESTA E A REGUA DE CUSTO QUE IMPORTA, e ela substituiu a soma de volumes.
     * Somar o volume de cada prateleira conta a sobreposicao duas vezes -- e a
     * copa e feita de sobreposicao de proposito --, entao aquele numero media uma
     * coisa que o gerador nunca executa. O que ele executa e: para cada chunk,
     * varrer a caixa de cada prateleira que o toca.
     */
    /**
     * Quantos blocos o gerador visitaria para desenhar a copa neste chunk.
     *
     * <p>SUBSTITUIU A SOMA DE VOLUMES. A copa e feita de sobreposicao de
     * proposito, entao somar o volume de cada prateleira contava o mesmo bloco
     * varias vezes e media algo que ninguem executa. Isto conta visitas, que e o
     * que custa -- e usa a mesma conta de span do escritor.
     */
    public long estimatedVisitsForChunk(int chunkX, int chunkZ) {
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        long visits = 0;
        for (WorldTreeFoliageShelf shelf : shelves) {
            double r = shelf.radius() + 1.0;
            if (minX <= shelf.centerX() + r && maxX >= shelf.centerX() - r
                    && minZ <= shelf.centerZ() + r && maxZ >= shelf.centerZ() - r) {
                visits += shelf.estimatedVisitsInChunk(minX, minZ);
            }
        }
        return visits;
    }

    public int shelvesTouchingChunk(int chunkX, int chunkZ) {
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        int count = 0;
        for (WorldTreeFoliageShelf shelf : shelves) {
            double r = shelf.radius() + 1.0;
            if (minX <= shelf.centerX() + r && maxX >= shelf.centerX() - r
                    && minZ <= shelf.centerZ() + r && maxZ >= shelf.centerZ() - r) {
                count++;
            }
        }
        return count;
    }

    static double unit(long value) {
        return (value & 0xFFFFL) / 65535.0;
    }

    static double signed(long value) {
        return unit(value) * 2.0 - 1.0;
    }

    static long mix(long seed, int id, int index) {
        long value = seed ^ ((long) id * 0x9E3779B97F4A7C15L)
                ^ ((long) index * 0xBF58476D1CE4E5B9L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
