package com.darkcontinent.nenfoundation.client.vfx.model;

/**
 * A escada de espessuras que o AV4 precisou construir, e por que ela existe.
 *
 * <p><b>O PROBLEMA.</b> {@link net.minecraft.client.model.geom.builders.CubeDeformation}
 * entra na construcao da MALHA, e a malha e construida uma vez, no registro de
 * camada. Ou seja: a espessura da shell nao e um uniform que da para girar por
 * quadro -- ela e geometria assada. Ate o AV3 isso nao incomodava, porque a
 * malha era uma so, a de Ten, e Ren se contentava com alpha maior (limitacao
 * declarada em {@code AuraRenderRegistro}).
 *
 * <p>O AV4 acabou com essa folga. A direcao de arte pede que Ren seja MAIS
 * ESPESSO que Ten (0,072 contra 0,052 na borda) e que a shell CONTRAIA de 3 a 5%
 * nos primeiros 100 ms da subida. As duas coisas sao geometria, e nenhuma delas
 * cabe num alpha.
 *
 * <p><b>OS TRES CAMINHOS QUE NAO SERVEM, e por que.</b>
 *
 * <ul>
 *   <li>{@code poseStack.scale} -- proibido pela arquitetura. A escala acontece
 *       em torno do pivo de cada parte, entao os bracos se AFASTAM do corpo e a
 *       pelicula descola justamente nas articulacoes, que e onde a referencia
 *       depende de aderencia.</li>
 *   <li>Deslocar o vertice ao longo da NORMAL no shader -- as normais de uma
 *       caixa sao por face, nao suavizadas. Deslocar por elas abre FENDAS nas
 *       quinas, e uma fenda de 0,3 unidade e perfeitamente visivel de perto. O
 *       caminho tambem morreria no modo de degradacao, onde nao ha shader.</li>
 *   <li>Cruzar duas malhas com alpha complementar -- dobra o overdraw, que e o
 *       item numero UM da ordem de custos do AV8.</li>
 * </ul>
 *
 * <p><b>O QUE FICOU.</b> Assar uma escada de malhas e escolher o degrau mais
 * proximo por quadro. Nao ha alocacao, nao ha desenho a mais, e a troca de
 * degrau e invisivel porque os degraus da faixa de Ten a Ren distam 0,045
 * unidade de modelo -- menos de um vigesimo de pixel de textura.
 *
 * <p><b>O DEGRAU E INDEXADO PELA BORDA</b>, e nao por um fator abstrato. A borda
 * e a camada que carrega a leitura; o filme interno e o halo externo saem da
 * reta que liga Ten a Ren. Indexar pelo que se ve evita a pergunta "fator 1,4 de
 * que exatamente?" toda vez que alguem voltar aqui.
 *
 * <p>O custo esta declarado: sao {@code degraus x 3 passes x 2 modelos} malhas
 * assadas na entrada do cliente. Elas sao pequenas -- seis caixas cada -- e
 * nascem uma vez.
 */
public final class AuraGeometryLadder {

    /**
     * Onde cada degrau fica na reta que vai de Ten a Ren.
     *
     * <p>{@code 0} e Ten exato e {@code 1} e Ren exato -- os dois presetes da
     * direcao de arte caem em degraus, e nao entre eles. Abaixo de zero mora a
     * CONTRACAO da subida; acima de um, a folga que o AV7 usa para a aura passar
     * por fora da armadura.
     *
     * <p>A ESCADA E MAIS FINA ENTRE TEN E REN de proposito: e o trecho que a
     * transicao percorre em menos de um segundo, e e onde um degrau grosso
     * apareceria como salto. Acima de Ren a troca e rara -- vestir ou tirar
     * armadura --, e um degrau mais largo nao tem quem o veja.
     */
    private static final float[] POSICOES = {
            -0.12F, 0.00F, 0.14F, 0.28F, 0.43F, 0.57F, 0.71F, 0.86F, 1.00F,
            1.25F, 1.50F, 1.75F, 2.00F};

    /** Quantos degraus a escada tem. */
    public static final int DEGRAUS = POSICOES.length;

    /** O degrau de Ten exato. */
    public static final int DEGRAU_DE_TEN = 1;

    /** O degrau de Ren exato. */
    public static final int DEGRAU_DE_REN = 8;

    private static final AuraGeometryProfile TEN = AuraGeometryProfile.ten();
    private static final AuraGeometryProfile REN = AuraGeometryProfile.ren();

    private AuraGeometryLadder() {
    }

    /**
     * O perfil de um degrau.
     *
     * <p>EXTRAPOLA ALEM DE REN, e isso e intencional e limitado: o teto de
     * {@link AuraGeometryProfile#ESPESSURA_MAXIMA} continua valendo, e o
     * construtor do perfil RECUSA quem passar dele. O degrau mais alto da escada
     * foi escolhido para caber com folga.
     */
    public static AuraGeometryProfile perfilDe(int degrau) {
        float t = POSICOES[Math.clamp(degrau, 0, DEGRAUS - 1)];
        return new AuraGeometryProfile(
                naReta(TEN.espessuraInterna(), REN.espessuraInterna(), t),
                naReta(TEN.espessuraBorda(), REN.espessuraBorda(), t),
                naReta(TEN.espessuraExterna(), REN.espessuraExterna(), t));
    }

    /** A espessura da borda deste degrau, em blocos. */
    public static float espessuraDaBordaDe(int degrau) {
        return naReta(TEN.espessuraBorda(), REN.espessuraBorda(),
                POSICOES[Math.clamp(degrau, 0, DEGRAUS - 1)]);
    }

    /**
     * O degrau cuja borda mais se aproxima da espessura pedida, em blocos.
     *
     * <p>SATURA NAS PONTAS em vez de recusar. Quem chama e o caminho de
     * desenho, e uma excecao no tick de render derruba o mundo inteiro -- nao so
     * a aura. Uma espessura absurda vira o degrau mais grosso disponivel, que e
     * feio e nao e quebrado.
     *
     * <p>BUSCA LINEAR, e nao binaria: treze comparacoes de float por jogador com
     * aura sao mais baratas que o desvio de um laco esperto, e o codigo fica
     * legivel para quem voltar aqui com um perfil novo na mao.
     */
    public static int degrauPara(float espessuraDaBordaEmBlocos) {
        if (!Float.isFinite(espessuraDaBordaEmBlocos)) {
            return DEGRAU_DE_TEN;
        }
        int melhor = 0;
        float menorDistancia = Float.MAX_VALUE;
        for (int i = 0; i < DEGRAUS; i++) {
            float distancia = Math.abs(espessuraDaBordaDe(i) - espessuraDaBordaEmBlocos);
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                melhor = i;
            }
        }
        return melhor;
    }

    /** Onde este degrau fica na reta de Ten a Ren. Util para teste e para overlay. */
    public static float posicaoDe(int degrau) {
        return POSICOES[Math.clamp(degrau, 0, DEGRAUS - 1)];
    }

    private static float naReta(float ten, float ren, float t) {
        return ten + (ren - ten) * t;
    }
}
