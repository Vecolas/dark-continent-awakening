package com.darkcontinent.nenfoundation.worldtree;

import java.util.List;

/**
 * ONDE fica a cabana de um checkpoint, decidido antes de existir chunk.
 *
 * <p><b>POR QUE A PRATELEIRA SAIU.</b> O checkpoint era uma ancora sobre um disco
 * de lenho de um bloco de espessura, saindo do tronco. Consertar a posicao dela
 * -- que era o defeito anterior, e era real -- nao consertou a leitura: uma
 * tabua nua projetada de uma parede de casca continua parecendo pedaco de mundo
 * quebrado, e nao lugar construido. O pedido foi explicito: cabana, num galho.
 *
 * <p><b>E A CABANA RESOLVE UM PROBLEMA QUE A PRATELEIRA NAO TINHA COMO
 * RESOLVER.</b> A prateleira so preenchia AR -- ela era educada com o que ja
 * estivesse no lugar, e por isso saia furada. Uma construcao afirma o proprio
 * volume: o interior e ESVAZIADO e as paredes sao escritas por cima do que
 * houver. Nao ha estado do mundo que produza uma cabana pela metade.
 *
 * <p><b>SEM MINECRAFT, DE PROPOSITO</b>: a escolha de qual galho hospeda cada
 * checkpoint precisa ser feita -- e medida em vinte seeds -- antes de qualquer
 * bloco.
 *
 * <h2>A trava que decide tudo</h2>
 *
 * <p>{@code WorldTreeCheckpoint.nearest(y)} mapeia a altura da ancora de volta
 * para o checkpoint com tolerancia de <b>24 blocos</b>. Uma cabana posta na
 * altura do galho, e nao na do checkpoint, sai dessa janela e a ancora deixa de
 * ser reconhecida -- o jogador clica e recebe "este anchor nao pertence a rota".
 *
 * <p>Por isso a cabana NAO segue a altura do galho: o piso fica sempre em
 * {@code checkpoint.y() - 1}, e quem sobe ate ela e um PILAR de lenho a partir
 * do galho. Uma casa sobre estacas e uma construcao; uma casa na altura errada e
 * um checkpoint quebrado.
 *
 * <h2>Nem todo checkpoint tem galho, e isso esta medido</h2>
 *
 * <p>As cinco zonas de galho vao de y=320 a y=1480. BASE (48) e LOWER (260) ficam
 * <b>abaixo da primeira</b>: nas vinte seeds, o galho mais proximo de BASE esta a
 * 297 blocos e o de LOWER a 85. Nao ha o que escolher -- para estes dois a cabana
 * encosta no TRONCO, e isto esta dito aqui em vez de ser disfarcado com um galho
 * distante.
 *
 * <p>Para os outros cinco ha galho em 20 de 20 seeds.
 */
public record WorldTreeClimbingPost(
        int centerX, int floorY, int centerZ,
        WorldTreeClimbingPost.Suporte suporte,
        int topoDoSuporte) {

    /** O que segura a cabana. */
    public enum Suporte {
        /** Um galho, com um pilar de lenho entre ele e o piso. */
        GALHO,
        /** O tronco, quando nao ha galho naquela altitude. */
        TRONCO
    }

    /**
     * Meia largura da cabana, sem contar a parede.
     *
     * <p>Tres da um interior de 5x5 e uma pegada de 7x7 com as paredes. Menor que
     * isso nao cabe uma pessoa e a ancora com folga; maior comeca a competir com
     * a grossura do galho que a sustenta.
     */
    public static final int RAIO = 3;

    /** Altura livre por dentro. Tres e o minimo para nao dar claustrofobia. */
    public static final int ALTURA_INTERNA = 3;

    /**
     * O quanto a altura do galho pode divergir da do checkpoint.
     *
     * <p>Quarenta -- e note que ele e MAIOR que os 24 de
     * {@code WorldTreeCheckpoint.nearest}. Nao ha contradicao: a cabana fica na
     * altura do checkpoint de qualquer jeito, e este numero limita so a altura do
     * galho que a sustenta pelo pilar. Acima de quarenta o pilar fica alto demais
     * para ler como apoio, e vira poste.
     */
    public static final int DESVIO_MAXIMO_DO_GALHO = 40;

    /**
     * Grossura minima de galho que hospeda uma cabana.
     *
     * <p>Tres e o {@code MIN_TIP_RADIUS} da rede de galhos: abaixo disso o galho
     * nao existe como volume desenhado.
     */
    public static final double GALHO_MINIMO = 3.0;

    /** Amostras por galho ao procurar o ponto de apoio. Fixo, para ser deterministico. */
    private static final int AMOSTRAS = 40;

    public WorldTreeClimbingPost {
        if (floorY <= 0) {
            throw new IllegalArgumentException("piso de cabana invalido: " + floorY);
        }
    }

    /**
     * Onde o jogador pousa ao viajar para ca: dentro da cabana, AO LADO da
     * ancora.
     *
     * <p>Um bloco ao lado, e nao em cima dela: teleportar para dentro do proprio
     * bloco de ancora empurra o jogador para fora da cabana pela primeira face
     * livre que o motor achar.
     */
    public double spawnX() {
        return centerX + 1.5;
    }

    public double spawnZ() {
        return centerZ + 0.5;
    }

    /** O y da ancora: um bloco acima do piso, dentro da cabana. */
    public int anchorY() {
        return floorY + 1;
    }

    /**
     * A cabana de um checkpoint.
     *
     * <p>Escolhe o galho mais proximo em altura que esteja ABAIXO do piso -- a
     * cabana pousa sobre ele. Um galho acima do piso atravessaria o interior, e
     * como o interior e esvaziado, isso abriria um buraco no proprio galho.
     */
    public static WorldTreeClimbingPost forCheckpoint(WorldTreeLayout layout, int checkpointY) {
        // O PISO NUNCA FICA ABAIXO DO PE DA ARVORE, e este caso e o BASE.
        //
        // BASE mora em y=48 e o tronco COMECA em y=48: o piso em `y - 1` caia em
        // 47, um bloco abaixo de onde a arvore existe. Abaixo disso a dimensao e
        // vazio -- nao ha chao, nao ha tronco, nao ha nada em que encostar. A
        // cabana nasceria boiando no nada, que e o defeito desta rodada de volta,
        // so que no unico checkpoint onde nenhum portao olhava.
        //
        // Subir um bloco cabe de sobra na janela de 24 de
        // `WorldTreeCheckpoint.nearest`, e o portao daqui confere isso.
        int floorY = Math.max(checkpointY - 1, layout.trunk().baseY() + 1);
        List<WorldTreeBranchNode> nodes = layout.branchNodes();

        double melhorDesvio = Double.MAX_VALUE;
        WorldTreePoint melhor = null;
        double melhorRaio = 0.0;
        for (int indice = 0; indice < nodes.size(); indice++) {
            WorldTreeSpline spline = nodes.get(indice).spline();
            for (int amostra = 0; amostra <= AMOSTRAS; amostra++) {
                double t = (double) amostra / AMOSTRAS;
                double raio = spline.radiusAt(t);
                if (raio < GALHO_MINIMO) {
                    continue;
                }
                WorldTreePoint ponto = spline.pointAt(t);
                double topo = ponto.y() + raio;
                // O TOPO DO GALHO PODE ENCOSTAR NO PISO, e nao mais que isso.
                //
                // A primeira versao exigia o galho inteiro ABAIXO do piso, o que
                // parece o certo -- a cabana pousa em cima dele. Nas vinte seeds
                // isso jogou o SUMMIT para o eixo central: os galhos da zona do
                // topo NASCEM em y=1450, que e a altura do proprio checkpoint, e
                // com raio 6,7 o topo deles fica oito blocos acima do piso.
                //
                // Um bloco de tolerancia deixa o piso RASPAR o galho em vez de
                // pousar sobre ele -- que e como uma cabana de arvore de verdade
                // se apoia. Mais que isso e o interior esvaziado abrindo um
                // buraco no galho, e ai o conserto vira outro defeito.
                if (topo > floorY + 1) {
                    continue;
                }
                double desvio = Math.abs(floorY - topo);
                if (desvio > DESVIO_MAXIMO_DO_GALHO) {
                    continue;
                }
                // EMPATE RESOLVIDO PELA ORDEM DA LISTA, e nao por sorteio: dois
                // chunks calculam isto em momentos diferentes e precisam chegar
                // na mesma cabana.
                if (desvio < melhorDesvio) {
                    melhorDesvio = desvio;
                    melhor = ponto;
                    melhorRaio = raio;
                }
            }
        }

        if (melhor != null) {
            // O PILAR NUNCA SOBE: se o galho raspa o piso, ele tem comprimento
            // zero. `topoDoSuporte` acima do piso faria o laco do pilar escrever
            // dentro do comodo.
            int topoDoSuporte = Math.min((int) Math.floor(melhor.y() + melhorRaio), floorY);
            return new WorldTreeClimbingPost(
                    (int) Math.round(melhor.x()), floorY, (int) Math.round(melhor.z()),
                    Suporte.GALHO, topoDoSuporte);
        }
        return noEixoCentral(layout, floorY);
    }

    /**
     * A cabana encostada no eixo central, para as altitudes sem galho.
     *
     * <p>O centro fica a {@code casca + RAIO}, o que poe a parede de dentro
     * exatamente sobre a casca -- a cabana ENCOSTA, e o piso dela atravessa para
     * dentro da madeira.
     *
     * <p><b>E O EIXO CENTRAL NAO E SEMPRE O TRONCO.</b> Ele acaba em y=1200; do
     * y=1100 ao 1450 quem sobe e o LIDER, com outro eixo e outro raio. A primeira
     * versao disto fixava o y no topo do tronco e usava o raio de la para
     * qualquer altura -- acima de 1200 a cabana teria nascido no ar, no raio
     * errado, que e literalmente o defeito que esta rodada veio consertar.
     */
    private static WorldTreeClimbingPost noEixoCentral(WorldTreeLayout layout, int floorY) {
        if (floorY > layout.trunk().topY()) {
            WorldTreePoint eixo = WorldTreeTrunkSurface.leaderCenter(floorY, layout.seed());
            double raio = WorldTreeTrunkSurface.leaderRadius(floorY);
            return new WorldTreeClimbingPost(
                    (int) Math.ceil(eixo.x() + raio) + RAIO, floorY,
                    (int) Math.round(eixo.z()), Suporte.TRONCO, floorY);
        }
        int y = Math.max(layout.trunk().baseY(), floorY);
        double casca = WorldTreeTrunkSurface.naDirecaoX(
                layout.trunk().radiusAt(y), y, layout.seed());
        return new WorldTreeClimbingPost(
                (int) Math.ceil(casca) + RAIO, floorY, 0, Suporte.TRONCO, floorY);
    }

    /**
     * A cabana do pe da arvore, no Overworld.
     *
     * <p>Mesma construcao dos checkpoints, pelo mesmo motivo: o que havia la era
     * a ancora sobre UM bloco de lenho, dezesseis blocos no ar.
     *
     * @param floorY altura do piso, fixa -- ver a nota sobre o heightmap em
     *               {@code WorldTreeBaseGenerator}
     */
    public static WorldTreeClimbingPost atTreeFoot(WorldTreeLayout layout,
            double cascaDoFuste, int floorY) {
        return new WorldTreeClimbingPost(
                layout.overworldOriginX() + (int) Math.ceil(cascaDoFuste) + RAIO,
                floorY,
                layout.overworldOriginZ(),
                Suporte.TRONCO, floorY);
    }

    /** Se este bloco pertence a pegada da cabana (paredes incluidas). */
    public boolean naPegada(int x, int z) {
        return Math.abs(x - centerX) <= RAIO && Math.abs(z - centerZ) <= RAIO;
    }

    /** Se este bloco e parede, e nao interior. */
    public boolean naParede(int x, int z) {
        return naPegada(x, z)
                && (Math.abs(x - centerX) == RAIO || Math.abs(z - centerZ) == RAIO);
    }

    /** O y do teto: acima da altura livre. */
    public int roofY() {
        return floorY + ALTURA_INTERNA + 1;
    }

    /** Se a cabana encosta neste chunk. */
    public boolean tocaChunk(int minX, int minZ) {
        return minX <= centerX + RAIO && minX + 15 >= centerX - RAIO
                && minZ <= centerZ + RAIO && minZ + 15 >= centerZ - RAIO;
    }
}
