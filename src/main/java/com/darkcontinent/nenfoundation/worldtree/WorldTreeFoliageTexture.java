package com.darkcontinent.nenfoundation.worldtree;

/**
 * QUAIS folhas brilham.
 *
 * <p>Uma fracao da copa e {@code world_tree_leaves_luminous} em vez de folha
 * comum. A decisao mora aqui, e nao dentro do laco que escreve blocos, pelo
 * mesmo motivo que fez {@link WorldTreeFoliagePlan} existir: o que so existe
 * dentro do laco nao tem regua.
 *
 * <p><b>SEM MINECRAFT, DE PROPOSITO</b>, pelo mesmo motivo do plano: o portao
 * roda as vinte seeds em JUnit, em milissegundos.
 *
 * <h2>Por que o campo e AGRUPADO, e nao um sorteio por bloco</h2>
 *
 * <p>Por leitura: folha luminosa espalhada bloco a bloco e chuvisco -- o mesmo
 * defeito 1.2 de {@code docs/worldtree/copa-e-folhagem.md} que fez a folhagem
 * anterior parecer confete. O que a referencia sugere e mancha: tufos que
 * acendem.
 *
 * <p>E por CUSTO, que aqui e o argumento mais forte. Cada bloco luminoso e uma
 * fonte de luz para a engine, e a propagacao de luz e um BFS por fonte. Fontes
 * encostadas umas nas outras compartilham quase toda a propagacao; fontes
 * espalhadas pagam cada uma a sua. A mesma quantidade de folha luminosa custa
 * ordens de grandeza a mais espalhada do que agrupada.
 *
 * <h2>Soma de senos, e nao produto</h2>
 *
 * <p>Pela licao que o ruido da casca ja aprendeu: um produto separavel zera para
 * uma laje inteira quando um fator passa por zero, e em tela isso le como
 * costura de chunk -- quem visse iria procurar o defeito na geracao por chunk,
 * que esta certa. As frequencias sao incomensuraveis para o padrao nao se
 * repetir em grade.
 *
 * <h2>Por que a tabela de seno</h2>
 *
 * <p>Este campo e avaliado uma vez por bloco de folha -- dezenas de milhoes de
 * vezes por mundo. A tabela troca {@code Math.sin} por uma interpolacao linear.
 * Dois efeitos, e o segundo importa mais que o primeiro:
 *
 * <ol>
 *   <li>e mais barata;</li>
 *   <li>e mais DETERMINISTICA. {@code Math.sin} so promete 1 ulp e pode diferir
 *       entre JVMs e plataformas -- e 1 ulp perto do limiar troca a decisao de um
 *       bloco. A tabela e aritmetica exata sobre valores tabelados: mesma
 *       entrada, mesmo bit, em qualquer lugar.</li>
 * </ol>
 */
public final class WorldTreeFoliageTexture {

    /**
     * O limiar do brilho.
     *
     * <p><b>ELE FOI CALIBRADO, E NAO ESCOLHIDO.</b> O pedido era 15% das folhas.
     * Medido sobre as vinte seeds, este valor devolve 0,148. Mais alto, menos
     * folha luminosa; a curva e suave. A regua e
     * {@code WorldTreeFoliageTextureTest#quinzePorCentoDasFolhasBrilham}, e ela
     * reprova se alguem mexer aqui sem refazer a conta.
     */
    private static final double LIMIAR_DE_BRILHO = 1.295;

    /**
     * Tamanho da tabela de seno.
     *
     * <p>4096 entradas com interpolacao linear erram ~1e-7 -- ordens de grandeza
     * abaixo da menor diferenca que mudaria a decisao de um bloco no limiar
     * acima.
     */
    private static final int AMOSTRAS = 4096;

    private static final double POR_RADIANO = AMOSTRAS / (2.0 * Math.PI);

    /** Uma entrada a mais, para a interpolacao nao precisar de modulo na ponta. */
    private static final double[] SENO = new double[AMOSTRAS + 1];

    static {
        for (int i = 0; i <= AMOSTRAS; i++) {
            SENO[i] = Math.sin(i * 2.0 * Math.PI / AMOSTRAS);
        }
    }

    private WorldTreeFoliageTexture() {
    }

    /**
     * Seno tabelado.
     *
     * <p>E a UNICA implementacao: o gerador e os testes passam todos por aqui.
     * Duas versoes disto divergiriam exatamente onde importa -- no limiar --, e a
     * divergencia apareceria como folha trocando de tipo entre uma geracao e
     * outra da mesma seed.
     *
     * <p>PUBLICO porque o ruido de casca do gerador tambem passa por ele, e pelo
     * mesmo motivo -- e ele mora noutro pacote.
     */
    public static double sin(double radianos) {
        double escalado = radianos * POR_RADIANO;
        // `Math.floor` e nao cast: cast trunca em direcao a zero, e metade da
        // copa esta em coordenada negativa. Truncar produziria uma
        // descontinuidade em x=0 -- uma emenda reta atravessando a arvore.
        double piso = Math.floor(escalado);
        double fracao = escalado - piso;
        int indice = (int) Math.floorMod((long) piso, (long) AMOSTRAS);
        return SENO[indice] + (SENO[indice + 1] - SENO[indice]) * fracao;
    }

    /**
     * O termo do campo que so depende da coluna.
     *
     * <p>Sai do laco de y por economia: e o mesmo valor para todos os blocos da
     * coluna, e o laco de y e o mais interno da geracao da copa. Quem chama passa
     * o resultado para {@link #brilha(double, int, int, int, long)}.
     *
     * <p><b>ISTO NAO E UMA SEGUNDA IMPLEMENTACAO.</b> A sobrecarga de
     * conveniencia {@link #brilha(int, int, int, long)} calcula este termo e
     * delega -- ha um caminho so, e o portao compara os dois para garantir que
     * continue assim.
     */
    public static double termoDaColuna(int x, int z, long seed) {
        return sin(x * 0.281 - z * 0.203 + seed * 2.0e-6);
    }

    /** Se esta folha e luminosa. */
    public static boolean brilha(int x, int y, int z, long seed) {
        return brilha(termoDaColuna(x, z, seed), x, y, z, seed);
    }

    /** A mesma decisao, com o termo da coluna ja calculado. */
    public static boolean brilha(double termoDaColuna, int x, int y, int z, long seed) {
        double s = seed * 2.0e-6;
        return termoDaColuna
                + sin(y * 0.167 + x * 0.089 - s)
                + sin(z * 0.247 + y * 0.109) > LIMIAR_DE_BRILHO;
    }
}
