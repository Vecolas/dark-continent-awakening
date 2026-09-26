package com.darkcontinent.nenfoundation.enemy.greedisland;

import java.util.List;

/**
 * A macrogeografia CONGELADA de Greed Island. Fase G0.
 *
 * <p>Fonte: {@code docs/planos/greed_island_macrogeografia_completa_codex.txt},
 * o documento de epic escrito pelo dono do projeto. Este arquivo e a
 * transcricao dele para dado que o codigo consegue ler -- e nada aqui e
 * invencao minha.
 *
 * <p><b>POR QUE CONSTANTE, E NAO CONFIG NEM DATAPACK.</b> A regra de ouro do
 * documento e que o layout macro seja IGUAL EM TODO SERVIDOR: Masadora precisa
 * continuar sendo Masadora naquele lugar, e um guia da ilha tem de valer em
 * qualquer mundo. Config torna isso ajustavel por servidor, que e exatamente o
 * que nao pode acontecer. E datapack seria pior: um pack mal instalado moveria
 * uma cidade sem ninguem notar.
 *
 * <p><b>TUDO AQUI E {@code ORIGINAL_COMPATIBLE}</b>, e o documento e explicito:
 * nao ha coordenada canonica de Greed Island. Mapas que circulam derivam do
 * <i>Battle Collection</i>, e nao de cartografia do manga. Estas posicoes sao
 * design do mod, e sao rotuladas como tal para ninguem as defender como cânone
 * numa discussao futura.
 *
 * <p>ARQUIVO HOSTIL A MERGE: uma pessoa por vez.
 */
public final class GreedIslandConstants {

    private GreedIslandConstants() {
    }

    // ------------------------------------------------------------------
    // ESCALA  (secoes 3, 4, 6 do documento)
    // ------------------------------------------------------------------

    /** Extensao leste-oeste da caixa da ilha, em blocos. */
    public static final int EXTENSAO_LESTE_OESTE = 80_000;

    /** Extensao norte-sul. */
    public static final int EXTENSAO_NORTE_SUL = 70_000;

    /**
     * O centro logico e a ORIGEM, e isso e decisao do documento.
     *
     * <p>Facilita mapa, debug, viagem rapida, busca de regiao, ancora de cidade
     * e hidrografia -- todos passam a medir a partir de zero, sem carregar um
     * deslocamento que alguem uma hora esquece de aplicar.
     */
    public static final int CENTRO_X = 0;

    /** Idem. */
    public static final int CENTRO_Z = 0;

    // ------------------------------------------------------------------
    // ORCAMENTO VERTICAL  (secoes 7, 17)
    // ------------------------------------------------------------------

    /**
     * O nivel do mar.
     *
     * <p><b>ELE VOLTOU PARA 63, e a mudanca precisa de explicacao.</b> Numa
     * conversa anterior o pedido foi "trazer o topo do mundo para baixo,
     * aproximadamente -20", para dar altura as montanhas, e foi isso que a
     * versao de andaime implementou: mar em -20, piso em -128.
     *
     * <p>A secao 17 do documento fecha a escada inteira -- mar 63, planicie
     * 66-90, colina 90-130, planalto 130-180, montanha 180-260, picos 260-310
     * -- e ela cabe num mundo padrao de -64 a 320. O OBJETIVO do pedido antigo
     * (montanha alta e definida, com caverna sobrando embaixo) fica MELHOR
     * atendido assim: 247 blocos de subida e 127 de caverna, contra 340 e 108
     * do andaime. O documento e mais novo e mais pensado; ele ganha.
     */
    public static final int NIVEL_DO_MAR = 63;

    /** Piso do mundo. Compativel com o Overworld, por escolha do documento. */
    public static final int PISO_DO_MUNDO = -64;

    /** Altura total. Teto em 320. */
    public static final int ALTURA_DO_MUNDO = 384;

    /** As faixas de relevo da secao 17, em Y. */
    public static final int TOPO_DA_PLANICIE = 90;

    /** Idem. */
    public static final int TOPO_DAS_COLINAS = 130;

    /** Idem. */
    public static final int TOPO_DO_PLANALTO = 180;

    /** Idem. */
    public static final int TOPO_DA_MONTANHA = 260;

    /** O pico mais alto permitido. Acima disto o relevo encosta no teto. */
    public static final int PICO_MAXIMO = 310;

    // ------------------------------------------------------------------
    // BARREIRA  (secao 8)
    // ------------------------------------------------------------------

    /**
     * Raio da barreira do mundo: 48.000 blocos.
     *
     * <p>Deixa oceano navegavel em volta da ilha e evita exploracao infinita de
     * mar irrelevante. O ANDAIME usava 880 -- duas ordens de grandeza a menos --
     * porque a ilha dele tinha 1.760 de diametro.
     */
    public static final int RAIO_DA_BARREIRA = 48_000;

    // ------------------------------------------------------------------
    // COSTA  (secoes 13, 14, 15)
    // ------------------------------------------------------------------

    /** Um ponto macro no plano XZ. */
    public record Ponto(int x, int z) {
    }

    /**
     * As treze ancoras de costa, no sentido do relogio a partir do noroeste.
     *
     * <p><b>A SPLINE PASSA PERTO, E NAO EXATAMENTE POR TODAS.</b> O documento e
     * explicito: elas sao macro-outline, e nao pontos de interpolacao rigida.
     * Amarrar a costa a cada uma produziria um poligono, e poligono le como
     * mapa de estrategia -- nao como ilha.
     *
     * <p>A ORDEM E O CONTRATO. Elas formam um anel fechado, e o calculo de
     * dentro/fora depende disso. Reordenar uma vira a costa do avesso naquele
     * trecho, sem erro nenhum.
     */
    public static final List<Ponto> ANCORAS_DE_COSTA = List.of(
            new Ponto(-31_000, -24_000),   // C01 noroeste
            new Ponto(-16_000, -33_000),   // C02
            new Ponto(2_000, -32_000),     // C03 norte
            new Ponto(20_000, -29_000),    // C04
            new Ponto(35_000, -20_000),    // C05 nordeste
            new Ponto(39_000, -4_000),     // C06 leste
            new Ponto(34_000, 14_000),     // C07
            new Ponto(27_000, 30_000),     // C08 sudeste
            new Ponto(6_000, 34_000),      // C09 sul
            new Ponto(-13_000, 32_000),    // C10
            new Ponto(-29_000, 25_000),    // C11 sudoeste
            new Ponto(-38_000, 9_000),     // C12 oeste
            new Ponto(-39_000, -8_000));   // C13

    /**
     * Uma saliencia ou reentrancia da costa.
     *
     * <p><b>ELA E ELIPTICA, e o mapa exportado e quem exigiu isso.</b> A
     * primeira versao usava lobos circulares, e o resultado foi honesto e
     * errado: o portao passou e a ilha continuou lendo como um blob. Peninsula
     * circular e um inchaco -- peninsula de verdade tem EIXO e COMPRIMENTO.
     *
     * @param alcance    o raio no eixo curto
     * @param alongamento quantas vezes o eixo longo e maior que o curto
     * @param anguloGraus a direcao do eixo longo, 0 = leste, cresce para o sul
     */
    public record Lobo(String nome, int x, int z, int alcance, double forca,
            double alongamento, double anguloGraus) {

        /** Lobo redondo: alongamento 1. Serve para enseada pequena. */
        public Lobo(String nome, int x, int z, int alcance, double forca) {
            this(nome, x, z, alcance, forca, 1.0D, 0.0D);
        }
    }

    /**
     * As tres peninsulas principais (secao 14).
     *
     * <p>Elas EMPURRAM a costa para fora. Sem elas a silhueta seria uma elipse
     * amassada, e o documento pede assimetria com peninsulas de verdade.
     */
    public static final List<Lobo> PENINSULAS = List.of(
            // Apontam PARA FORA da ilha, cada uma na sua diagonal: e o eixo que
            // as transforma de inchaco em dedo de terra.
            new Lobo("nordeste", 33_000, -17_000, 5_500, 11_000.0D, 2.6D, -28.0D),
            new Lobo("sudeste", 30_000, 25_000, 6_000, 12_000.0D, 2.8D, 38.0D),
            new Lobo("sudoeste", -28_000, 25_000, 5_500, 10_000.0D, 2.4D, 140.0D));

    /**
     * As quatro baias principais (secao 15).
     *
     * <p>Elas PUXAM a costa para dentro -- forca negativa. Uma baia e o que
     * impede uma peninsula vizinha de virar so um inchaco.
     */
    public static final List<Lobo> BAIAS = List.of(
            // Elas mordem PARA DENTRO, transversais a costa -- e e a mordida
            // que separa uma peninsula da vizinha em vez de as fundir.
            new Lobo("soufrabi", 25_000, 17_000, 6_000, -11_000.0D, 2.2D, 20.0D),
            new Lobo("ocidental", -33_000, 4_000, 6_500, -10_000.0D, 2.0D, 95.0D),
            new Lobo("nordeste", 26_000, -21_000, 6_000, -9_500.0D, 2.0D, -60.0D),
            new Lobo("estuario_sul", 1_000, 31_000, 7_000, -10_000.0D, 2.4D, 82.0D));

    /**
     * Enseadas menores, entre as baias principais.
     *
     * <p><b>O PORTAO PEDIU ESTAS.</b> Com as quatro baias do documento no
     * tamanho original, a costa se desviava so 6% do circulo medio -- e um
     * desvio desse tamanho ainda le como disco amassado. O documento nao lista
     * enseadas uma a uma; ele pede, na secao 12, "costa recortada",
     * "enseadas", "estuarios" e uma silhueta assimetrica. Estas sao a leitura
     * disso, e sao ORIGINAL_COMPATIBLE como todo o resto.
     *
     * <p>Elas PUXAM, e nunca empurram: aumentar a irregularidade empurrando
     * faria a ilha crescer para fora da caixa declarada e encostar na barreira.
     */
    public static final List<Lobo> ENSEADAS = List.of(
            new Lobo("norte_central", -6_000, -30_000, 5_000, -7_000.0D, 2.0D, 75.0D),
            new Lobo("leste_alta", 36_000, 2_000, 4_500, -6_000.0D, 1.8D, 5.0D),
            new Lobo("sudoeste_rasa", -21_000, 28_000, 5_000, -6_000.0D, 1.9D, 115.0D),
            new Lobo("noroeste_funda", -33_000, -17_000, 5_000, -7_000.0D, 2.0D, 40.0D),
            new Lobo("sul_larga", 14_000, 31_000, 5_000, -6_000.0D, 1.9D, 70.0D),
            // O NORTE TEM DE FICAR ESTREITO (secao 12): duas mordidas grandes,
            // uma de cada lado, e o que afina a ilha ali sem mover as ancoras.
            new Lobo("norte_oeste", -22_000, -28_000, 6_500, -8_500.0D, 2.2D, 55.0D),
            new Lobo("norte_leste", 14_000, -29_000, 6_500, -8_500.0D, 2.2D, -50.0D));

    /**
     * As ilhotas costeiras e os dois pequenos arquipelagos (secao 9).
     *
     * <p>Elas sao LOBOS SOLTOS NO MAR: empurram a mascara acima de zero num
     * ponto onde o poligono ja dava oceano. Sem elas a costa termina seca, e o
     * documento pede 7-14 ilhas menores mais dois arquipelagos.
     */
    public static final List<Lobo> ILHOTAS = List.of(
            new Lobo("arq_sudoeste_a", -34_000, 29_000, 2_600, 4_200.0D),
            new Lobo("arq_sudoeste_b", -37_500, 26_000, 2_000, 3_600.0D),
            new Lobo("arq_sudoeste_c", -31_000, 33_000, 1_800, 3_200.0D),
            new Lobo("arq_nordeste_a", 40_000, -22_000, 2_400, 4_000.0D),
            new Lobo("arq_nordeste_b", 43_000, -17_000, 1_900, 3_400.0D),
            new Lobo("costeira_norte", 4_000, -37_000, 2_200, 3_800.0D),
            new Lobo("costeira_leste", 44_000, 2_000, 2_300, 3_900.0D),
            new Lobo("costeira_sul", 8_000, 39_000, 2_100, 3_600.0D),
            new Lobo("costeira_oeste", -43_000, -2_000, 2_000, 3_500.0D));

    // ------------------------------------------------------------------
    // CIDADES  (secao 37)
    // ------------------------------------------------------------------

    /** Uma ancora de cidade: o ponto fixo em torno do qual ela nasce. */
    public record AncoraDeCidade(String id, int x, int z) {
    }

    /**
     * As oito ancoras, congeladas na versao 1 do layout.
     *
     * <p>Mover qualquer uma depois de um mundo existir quebra o mapa daquele
     * mundo -- e por isso {@link GreedIslandLayoutVersion} existe.
     */
    public static final List<AncoraDeCidade> CIDADES = List.of(
            new AncoraDeCidade("shiso_tree", -18_000, 1_000),
            new AncoraDeCidade("antokiba", -14_500, 5_500),
            new AncoraDeCidade("rubicuta", -5_000, 8_500),
            new AncoraDeCidade("masadora", 4_500, -2_000),
            new AncoraDeCidade("aiai", 15_500, 5_500),
            new AncoraDeCidade("dorias", 8_500, 17_000),
            new AncoraDeCidade("soufrabi", 27_000, 22_000),
            new AncoraDeCidade("limeiro", -1_000, 27_000));

    // ------------------------------------------------------------------
    // RELEVO  (secoes 18-23)
    // ------------------------------------------------------------------

    /**
     * Uma cordilheira: uma POLILINHA de nos, e nao um blob.
     *
     * <p>A secao 18 e explicita -- "cada um e um graph de ridges, nao um biome
     * com noise alto". A diferenca aparece no mapa: ruido alto da montanhas
     * espalhadas sem direcao; uma polilinha da uma cadeia que se atravessa por
     * um passo e se contorna pela base, e e isso que torna a ilha memorizavel.
     */
    public record Cordilheira(String id, List<Ponto> nos, int alturaMinima,
            int alturaMaxima, int larguraBase) {
    }

    /** M1, M2 e M3, com os nos exatos das secoes 19, 20 e 21. */
    public static final List<Cordilheira> CORDILHEIRAS = List.of(
            new Cordilheira("north_crown", List.of(
                    new Ponto(-24_000, -25_000), new Ponto(-16_000, -28_000),
                    new Ponto(-7_000, -27_000), new Ponto(2_000, -25_000),
                    new Ponto(11_000, -26_000), new Ponto(23_000, -22_000)),
                    190, 280, 9_000),
            new Cordilheira("western_spine", List.of(
                    new Ponto(-27_000, -18_000), new Ponto(-29_000, -8_000),
                    new Ponto(-27_000, 2_000), new Ponto(-24_000, 10_000),
                    new Ponto(-20_000, 20_000)),
                    160, 260, 7_500),
            new Cordilheira("central_southeast", List.of(
                    new Ponto(-2_000, 2_000), new Ponto(4_000, 8_000),
                    new Ponto(10_000, 10_000), new Ponto(16_000, 16_000),
                    new Ponto(24_000, 23_000)),
                    130, 230, 7_000));

    /** Um planalto: mesa alta e larga, com borda. */
    public record Planalto(String id, int x, int z, int raio, int altura) {
    }

    /** P1 e P2 (secao 22). */
    public static final List<Planalto> PLANALTOS = List.of(
            new Planalto("central", -3_000, -2_000, 10_000, 125),
            new Planalto("eastern_tableland", 19_000, -5_000, 7_500, 135));

    /** Uma bacia: terreno rebaixado onde a agua junta e a cidade cabe. */
    public record Bacia(String id, int x, int z, int raio, int rebaixamento) {
    }

    /**
     * As quatro bacias (secao 23).
     *
     * <p>ELAS REBAIXAM O TERRENO, e nao so nomeiam a regiao: e o rebaixamento
     * que faz o rio querer passar ali e a cidade ter onde caber. Uma bacia que
     * fosse so um rotulo deixaria Antokiba num morro.
     */
    public static final List<Bacia> BACIAS = List.of(
            new Bacia("antokiba", -14_000, 5_000, 9_000, 28),
            new Bacia("masadora", 4_000, -2_000, 8_000, 24),
            new Bacia("aiai", 15_000, 6_000, 7_000, 22),
            new Bacia("southern_wetlands", -8_000, 24_000, 10_000, 34));

    // ------------------------------------------------------------------
    // HIDROGRAFIA  (secoes 25-33)
    // ------------------------------------------------------------------

    /** Um sistema hidrografico: nascente, curso e foz. */
    public record Rio(String id, List<Ponto> curso, int larguraNaFoz) {
    }

    /**
     * Os sete sistemas das secoes 26 a 32.
     *
     * <p>O CURSO E UMA POLILINHA DA NASCENTE A FOZ, e os pontos intermediarios
     * sao leitura das descricoes -- "fluxo sul/sudoeste", "passa proximo a
     * Antokiba", "mouth: Great South Estuary". O documento da nascente e
     * direcao; o caminho exato e {@code ORIGINAL_COMPATIBLE}.
     *
     * <p>TODO RIO TERMINA NO MAR OU NUM LAGO. Um curso que acaba em terra seca
     * nao da erro -- ele so desenha uma vala que nao leva a lugar nenhum.
     */
    // AS FOZES FORAM ESTENDIDAS PELO PORTAO. O documento da nascente, direcao
    // e -- em dois casos -- mouth; o resto do curso e leitura. Quatro das sete
    // paravam de 4.600 a 9.100 blocos TERRA ADENTRO, cada uma virando uma vala
    // que nao leva a lugar nenhum. `RelevoEHidrografiaTest` mede contra a
    // mascara e nomeia o rio que morreu seco.
    public static final List<Rio> RIOS = List.of(
            new Rio("northfall", List.of(
                    new Ponto(-8_000, -26_000), new Ponto(-9_000, -18_000),
                    new Ponto(-12_000, -10_000), new Ponto(-16_000, -2_000),
                    // A FOZ FOI ESTENDIDA ATE O MAR pelo portao: a primeira
                    // versao parava em (-30.000, 8.000), que a mascara diz
                    // estar 7.157 blocos TERRA ADENTRO. O documento da a
                    // nascente e a direcao ("sul/sudoeste"), e nao a foz -- o
                    // ponto final e leitura, e leitura tem de bater com a costa.
                    new Ponto(-22_000, 4_000), new Ponto(-30_000, 8_000),
                    new Ponto(-36_000, 11_000), new Ponto(-40_600, 12_900)), 26),
            new Rio("westreach", List.of(
                    new Ponto(-25_000, -8_000), new Ponto(-29_000, -4_000),
                    new Ponto(-32_000, 0), new Ponto(-35_000, 5_000)), 20),
            new Rio("antokiba", List.of(
                    new Ponto(-10_000, -4_000), new Ponto(-12_000, 1_000),
                    new Ponto(-13_200, 5_500), new Ponto(-16_000, 12_000),
                    new Ponto(-20_000, 19_000), new Ponto(-24_000, 26_000),
                    new Ponto(-26_300, 30_000)), 24),
            new Rio("masadora", List.of(
                    new Ponto(3_000, -7_000), new Ponto(3_000, -1_000),
                    new Ponto(6_000, 5_000), new Ponto(9_000, 13_000),
                    new Ponto(11_000, 22_000), new Ponto(12_000, 31_000)), 28),
            new Rio("eastflow", List.of(
                    new Ponto(18_000, -9_000), new Ponto(24_000, -11_000),
                    new Ponto(30_000, -9_000), new Ponto(36_000, -5_000),
                    new Ponto(42_000, -1_000)), 22),
            new Rio("southern_marsh", List.of(
                    new Ponto(-6_000, 14_000), new Ponto(-8_000, 22_000),
                    new Ponto(-3_000, 25_000), new Ponto(1_000, 31_000)), 30),
            new Rio("soufrabi", List.of(
                    new Ponto(20_000, 16_000), new Ponto(23_000, 18_000),
                    new Ponto(26_000, 21_000), new Ponto(29_000, 26_000),
                    new Ponto(31_800, 30_700)), 18));

    /** Um lago: espelho d'agua com nivel proprio. */
    public record Lago(String id, int x, int z, int raio) {
    }

    /** L1 a L8 da secao 33. */
    public static final List<Lago> LAGOS = List.of(
            new Lago("northfall", -9_000, -18_000, 1_400),
            new Lago("masadora", 3_000, -1_000, 1_600),
            new Lago("mirror", 13_000, -8_000, 1_100),
            new Lago("west_basin", -24_000, 9_000, 1_300),
            new Lago("marsh_a", -8_000, 22_000, 900),
            new Lago("marsh_b", -3_000, 25_000, 1_000),
            new Lago("highland", 12_000, 13_000, 800),
            new Lago("soufrabi_reservoir", 23_000, 18_000, 1_200));

    // ------------------------------------------------------------------
    // ESTRADAS  (secoes 56, 58)
    // ------------------------------------------------------------------

    /**
     * Os quatro passos de montanha da secao 58.
     *
     * <p>O documento diz "road graph deve preferi-los", e o roteador desconta
     * o custo dentro deles. Sem passo declarado, a unica saida do A* seria
     * contornar a serra inteira -- e uma ilha onde nenhuma estrada cruza
     * montanha perde metade da geografia que as montanhas criam.
     */
    public static final List<Ponto> PASSOS = List.of(
            new Ponto(-4_000, -20_000),    // Pass N1
            new Ponto(-24_000, 1_000),     // Pass W1
            new Ponto(9_000, 8_000),       // Pass C1
            new Ponto(18_000, 16_000));    // Pass SE1

    /** Uma ligacao do grafo de estradas. */
    public record Ligacao(String de, String para) {
    }

    /**
     * As nove ligacoes da secao 56.
     *
     * <p>ELAS SAO O GRAFO, e nao o traçado: o caminho de cada uma sai do A*
     * sobre o terreno. Congelar o traçado aqui seria desenhar a estrada por
     * cima do mapa, que e exatamente o que o nao-negociavel 7 proibe.
     *
     * <p>NEM TODA CIDADE SE LIGA A TODA CIDADE, e isso e design: um grafo
     * completo faria toda viagem ser direta, e as cartas de transporte
     * perderiam a razao de existir.
     */
    public static final List<Ligacao> ESTRADAS = List.of(
            new Ligacao("shiso_tree", "antokiba"),
            new Ligacao("antokiba", "rubicuta"),
            new Ligacao("rubicuta", "masadora"),
            new Ligacao("masadora", "aiai"),
            new Ligacao("masadora", "dorias"),
            new Ligacao("dorias", "soufrabi"),
            new Ligacao("dorias", "limeiro"),
            new Ligacao("rubicuta", "limeiro"),
            new Ligacao("aiai", "soufrabi"));

    /** O hub de entrada. Onde o anel deposita quem chega. */
    public static final String CIDADE_INICIAL = "shiso_tree";

    /** A ancora de uma cidade pelo id, ou vazio quando o id nao existe. */
    public static java.util.Optional<AncoraDeCidade> cidade(String id) {
        return CIDADES.stream().filter(c -> c.id().equals(id)).findFirst();
    }
}
