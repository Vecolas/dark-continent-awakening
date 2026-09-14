package com.darkcontinent.nenfoundation.worldtree;

/**
 * A VARANDA que segura uma ancora de escalada.
 *
 * <p><b>POR QUE ELA E UMA CLASSE, e nao dois lacos parecidos.</b> A ancora
 * aparece em dois lugares -- nos sete checkpoints da dimensao e no pe da arvore
 * no Overworld --, e os dois desenham a mesma varanda. Enquanto a forma morou
 * duplicada dentro dos dois lacos, ela ja tinha divergido: a do Overworld era um
 * unico bloco de lenho morto embaixo da ancora, e nem chegava perto de ser uma
 * plataforma.
 *
 * <p><b>E O PORTAO PRECISA LER OS MESMOS NUMEROS.</b> A primeira versao do teste
 * que mede "a varanda atravessa a casca" tinha uma CONSTANTE PROPRIA com o
 * alcance -- 9. Encurtar o laco do gerador de volta para 6 nao reprovava nada:
 * o teste continuava medindo o 9 dele contra a arvore, e a varanda em jogo
 * voltava a nascer solta. Uma regua com a copia do numero que ela deveria vigiar
 * nao vigia coisa nenhuma.
 *
 * <p><b>SEM MINECRAFT, DE PROPOSITO</b>: quem precisa saber se a varanda encosta
 * no tronco precisa saber ANTES de existir chunk.
 *
 * <h2>A forma NAO mudou, e vale dizer por que</h2>
 *
 * <p>Uma elipse deslocada para dentro da madeira: de -6 a +3, centro em -1,5. A
 * metade interna cai onde ja ha madeira -- e nao escreve nada, porque a varanda
 * so preenche ar --, e o que sobra do lado de fora e o balcao.
 *
 * <p>No conserto do "flutuando" eu ALARGUEI esta elipse para -9, achando que a
 * ponta interna afinada era parte do defeito. Alimentar o portao com a quebra
 * mostrou que nao era: com a ancora no lugar certo, seis blocos ja entram cinco
 * blocos casca adentro, e a elipse so afina onde nao se escreve nada. O portao
 * recusou a mudanca -- ele nao reprovava com -6 --, e a mudanca foi desfeita.
 *
 * <p>Registrado porque "a regua nao pegou" e "a mudanca nao era necessaria" sao
 * coisas diferentes, e confundir as duas afrouxa portao bom. Quem soltava a
 * varanda no ar era a ANCORA, sozinha: com ela em {@code nominal + 2}, a ponta
 * interna da plataforma parava FORA da casca.
 */
public final class WorldTreeAnchorPlatform {

    /** Quantos blocos a varanda avanca para dentro da madeira, a partir da ancora. */
    public static final int ALCANCE_PARA_DENTRO = 6;

    /** Quantos blocos ela avanca para fora -- o balcao propriamente dito. */
    public static final int ALCANCE_PARA_FORA = 3;

    /** Meia largura, para cada lado do eixo da ancora. */
    public static final int ALCANCE_LATERAL = 4;

    /** Onde o centro da elipse fica, em relacao a ancora: tronco adentro. */
    private static final double CENTRO_DX = -1.5;

    private static final double SEMI_EIXO_X = 5.5;
    private static final double SEMI_EIXO_Z = 4.25;

    private WorldTreeAnchorPlatform() {
    }

    /**
     * Se este deslocamento faz parte da varanda.
     *
     * @param dx deslocamento em X a partir da ancora; negativo aponta para dentro
     *           da madeira
     * @param dz deslocamento lateral
     */
    public static boolean contem(int dx, int dz) {
        if (dx < -ALCANCE_PARA_DENTRO || dx > ALCANCE_PARA_FORA
                || dz < -ALCANCE_LATERAL || dz > ALCANCE_LATERAL) {
            return false;
        }
        double normalizadoX = (dx - CENTRO_DX) / SEMI_EIXO_X;
        double normalizadoZ = dz / SEMI_EIXO_Z;
        return normalizadoX * normalizadoX + normalizadoZ * normalizadoZ <= 1.0;
    }

    /**
     * A ponta interna da varanda, em X, para uma ancora naquele X.
     *
     * <p>E o numero que decide se ela fica presa: ele precisa cair DENTRO da
     * casca do tronco, e o portao mede exatamente isso.
     */
    public static int pontaInterna(int anchorX) {
        return anchorX - ALCANCE_PARA_DENTRO;
    }
}
