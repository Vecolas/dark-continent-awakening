package com.darkcontinent.nenfoundation.client.hud;

/**
 * Geometria da HUD de Nen em coordenadas GUI.
 *
 * <p>DECISAO: o layout nasce do tamanho logico da GUI, nunca da resolucao
 * fisica. Assim o proprio Minecraft aplica GUI Scale e a HUD permanece presa
 * ao canto superior esquerdo sem espalhar numeros pelos renderers.
 *
 * <p><b>O REDESENHO DE 2026-09-25.</b> A versao anterior era 250x63 com DUAS
 * barras (Aura e Output), ambas permanentes, e nenhuma barra de vida. Ela
 * gastava comprimento demais para a informacao que carregava, e o peso visual
 * morava no contorno. Esta versao e <b>200x44</b>, com Vida e Aura permanentes
 * e o Fluxo aparecendo so quando ha tecnica ligada.
 *
 * <p>AS COLUNAS SAO FIXAS, e e isso que faz a HUD parecer diagramada em vez de
 * montada: rotulo, barra e valor ocupam a MESMA faixa horizontal nas duas
 * linhas. Uma barra mais curta que a outra sem motivo e o defeito que mais
 * aparece em HUD de mod, e ele nao da erro nenhum -- so fica torto.
 *
 * <p>A ALTURA MUDA, e a largura nao. Quando o fluxo aparece, o painel cresce
 * para baixo em {@link #ALTURA_COM_FLUXO}; a borda direita fica onde estava.
 * Crescer para os lados faria a HUD "respirar" horizontalmente a cada
 * ativacao, e o olho segue movimento lateral muito mais do que vertical.
 */
public record NenHudLayout(
        Retangulo moldura,
        Retangulo retrato,
        Retangulo nome,
        Retangulo chip,
        Retangulo rotuloDeVida,
        Retangulo barraDeVida,
        Retangulo valorDeVida,
        Retangulo rotuloDeAura,
        Retangulo barraDeAura,
        Retangulo valorDeAura,
        Retangulo barraDeFluxo,
        Retangulo plataforma,
        Retangulo trilho,
        Retangulo tecnicasAtivas) {

    /** Distancia do painel ate o canto da tela. */
    public static final int MARGEM = 8;

    /**
     * A largura do painel, e por que ela e esta.
     *
     * <p>A faixa pedida no plano e 180-240. Duzentos fica na metade de baixo
     * dela de proposito: em GUI Scale 2 numa tela de 1920 o espaco logico e de
     * 960, e cada pixel a mais aqui e um pixel a mais de topo ocupado o jogo
     * inteiro. O conteudo cabe com folga em 200, entao 216 seria largura
     * gasta com nada.
     */
    public static final int LARGURA_DA_MOLDURA = 200;

    /** Sem fluxo: retrato, nome, chip, Vida e Aura. E o estado padrao. */
    public static final int ALTURA_COMPACTA = 44;

    /**
     * Com a microbarra de fluxo. Sete pixels a mais, e nao uma linha inteira.
     *
     * <p>O NUMERO FOI CORRIGIDO PELO PORTAO. A primeira tentativa poe o fluxo em
     * {@code y=41}, terminando em 44 -- exatamente a altura compacta. Ele cabia
     * na moldura baixa, entao os pixels extras da moldura alta eram padding
     * puro, e a HUD crescia sem precisar. {@code NenHudLayoutTest} reprova
     * quando a barra contextual cabe na moldura que nao e a dela.
     */
    public static final int ALTURA_COM_FLUXO = 51;

    /** Respiro interno entre a borda do painel e o conteudo. */
    private static final int PADDING = 7;

    /** O corte diagonal das pontas. Geometria da moldura, nao do conteudo. */
    public static final int CORTE_DIAGONAL = 6;

    private static final int RETRATO = 26;
    private static final int LARGURA_DO_ROTULO = 24;
    private static final int LARGURA_DO_VALOR = 46;
    private static final int LARGURA_DO_CHIP = 40;
    /**
     * VIDA E MAIS BAIXA QUE AURA, e isso e linguagem visual e nao economia.
     *
     * <p>A leitura vital e compacta e firme; a de aura tem corpo e respira. Com
     * as duas na mesma altura elas leem como "duas barras iguais com cores
     * diferentes", que foi o diagnostico.
     */
    private static final int ALTURA_DA_VIDA = 6;

    private static final int ALTURA_DA_AURA = 8;

    private static final int ALTURA_DO_FLUXO = 3;

    /** Onde a espinha da HUD corre, medido do topo do painel. */
    private static final int X_DO_TRILHO = 36;

    /** O topo de cada linha, medido do topo do painel. */
    private static final int Y_DO_NOME = 6;
    private static final int Y_DA_VIDA = 20;
    private static final int Y_DA_AURA = 31;
    private static final int Y_DO_FLUXO = 42;

    public static NenHudLayout para(int larguraGui) {
        return para(larguraGui, false);
    }

    /**
     * O layout, sabendo se o fluxo entra.
     *
     * <p>O BOOLEANO ENTRA AQUI, e nao no renderer, porque ele muda a ALTURA da
     * moldura. Desenhar a moldura de um tamanho e o conteudo de outro e o jeito
     * mais barato de a barra vazar para fora do painel, e isso nao levanta
     * excecao nenhuma.
     */
    public static NenHudLayout para(int larguraGui, boolean comFluxo) {
        int largura = Math.min(LARGURA_DA_MOLDURA,
                Math.max(1, larguraGui - 2 * MARGEM));
        float escala = largura / (float) LARGURA_DA_MOLDURA;
        int altura = comFluxo ? ALTURA_COM_FLUXO : ALTURA_COMPACTA;

        // O CONTEUDO COMECA DEPOIS DO TRILHO, e nao depois do retrato: o vao
        // entre o nucleo e a espinha e o que faz as duas pecas lerem como
        // acopladas em vez de impressas no mesmo fundo.
        int xDoConteudo = X_DO_TRILHO + 6;
        int xDaBarra = xDoConteudo + LARGURA_DO_ROTULO;
        int xDoValor = LARGURA_DA_MOLDURA - PADDING - LARGURA_DO_VALOR;
        int larguraDaBarra = xDoValor - 4 - xDaBarra;
        int xDoChip = LARGURA_DA_MOLDURA - PADDING - LARGURA_DO_CHIP;

        return new NenHudLayout(
                new Retangulo(MARGEM, MARGEM, largura, escalar(altura, escala)),
                area(PADDING, 9, RETRATO, RETRATO, escala),
                area(xDoConteudo, Y_DO_NOME, xDoChip - xDoConteudo - 4, 9, escala),
                area(xDoChip, Y_DO_NOME - 1, LARGURA_DO_CHIP, 11, escala),
                area(xDoConteudo, Y_DA_VIDA, LARGURA_DO_ROTULO, ALTURA_DA_VIDA, escala),
                area(xDaBarra, Y_DA_VIDA, larguraDaBarra, ALTURA_DA_VIDA, escala),
                area(xDoValor, Y_DA_VIDA, LARGURA_DO_VALOR, ALTURA_DA_VIDA, escala),
                area(xDoConteudo, Y_DA_AURA, LARGURA_DO_ROTULO, ALTURA_DA_AURA, escala),
                area(xDaBarra, Y_DA_AURA, larguraDaBarra, ALTURA_DA_AURA, escala),
                area(xDoValor, Y_DA_AURA, LARGURA_DO_VALOR, ALTURA_DA_AURA, escala),
                area(xDaBarra, Y_DO_FLUXO, larguraDaBarra, ALTURA_DO_FLUXO, escala),
                // A PLATAFORMA COMECA NO TRILHO, e nao na borda do painel: o
                // nucleo fica sobre o mundo, sem chapa atras, e o vazio entre
                // os dois e o que os separa como pecas.
                new Retangulo(MARGEM + escalar(X_DO_TRILHO - 3, escala),
                        MARGEM + escalar(2, escala),
                        escalar(LARGURA_DA_MOLDURA - X_DO_TRILHO + 3, escala),
                        escalar(altura - 4, escala)),
                new Retangulo(MARGEM + escalar(X_DO_TRILHO, escala),
                        MARGEM + escalar(4, escala),
                        1, escalar(altura - 8, escala)),
                // ABAIXO DA MOLDURA, e nao dentro dela: a fila cresce para a
                // direita conforme o jogador liga mais tecnicas, e dentro do
                // painel ela colidiria com o valor da Aura no primeiro extra.
                // O Y acompanha a altura, senao ela invade o fluxo.
                new Retangulo(MARGEM + escalar(PADDING, escala),
                        MARGEM + escalar(altura + 3, escala),
                        escalar(72, escala), escalar(9, escala)));
    }

    private static Retangulo area(int x, int y, int largura, int altura, float escala) {
        return new Retangulo(MARGEM + escalar(x, escala), MARGEM + escalar(y, escala),
                escalar(largura, escala), escalar(altura, escala));
    }

    private static int escalar(int valor, float escala) {
        return Math.max(1, Math.round(valor * escala));
    }

    /** Preenchimento continuo; nenhuma decoracao de barra altera esta conta. */
    public static int preenchimento(int largura, float fracao) {
        if (largura <= 0 || !Float.isFinite(fracao)) return 0;
        return Math.round(largura * Math.clamp(fracao, 0.0F, 1.0F));
    }

    public record Retangulo(int x, int y, int largura, int altura) {
        public Retangulo {
            if (largura < 0 || altura < 0) {
                throw new IllegalArgumentException("dimensao negativa");
            }
        }

        /** A borda direita, exclusiva. Existe para nao repetir a soma em todo renderer. */
        public int fimX() {
            return this.x + this.largura;
        }

        /** A borda de baixo, exclusiva. */
        public int fimY() {
            return this.y + this.altura;
        }
    }
}
