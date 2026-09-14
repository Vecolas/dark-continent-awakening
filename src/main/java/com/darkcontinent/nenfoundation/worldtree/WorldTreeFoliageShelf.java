package com.darkcontinent.nenfoundation.worldtree;

/**
 * Uma PRATELEIRA de folhagem: o disco achatado que a referencia mostra empilhado
 * sobre cada galho.
 *
 * <p><b>POR QUE PRATELEIRA, E NAO BOLHA.</b> O gerador anterior desenhava
 * elipsoides alongados no eixo do galho -- salsichas. Varias salsichas de raio
 * parecido, enfileiradas sem se sobrepor, leem como DEDOS: e de onde vinham as
 * "coberturas que pareciam luvas". A copa da referencia e feita de discos largos
 * em X e Z e finos em Y, empilhados em varios niveis.
 *
 * <p><b>O CENTRO FICA ACIMA DO EIXO DO GALHO</b>, e isto conserta o defeito que
 * fazia a folhagem sumir. Centrada no eixo, com raio vertical menor que o do
 * proprio galho, a prateleira inteira caia DENTRO da madeira -- e como folha so
 * entra em ar, nao virava bloco nenhum. O que escapava pela borda era a "folha
 * deslocada" que se via em jogo.
 *
 * <p><b>A ESPESSURA INFERIOR NUNCA E ZERO.</b> A referencia tem folha embaixo do
 * galho, e e ela que impede a copa de parecer um guarda-sol visto de baixo.
 *
 * @param centerX          centro do disco
 * @param centerY          JA deslocado para cima do eixo do galho
 * @param centerZ          centro do disco
 * @param radius           raio horizontal, em blocos
 * @param topThickness     quanto a massa sobe acima do centro
 * @param bottomThickness  quanto ela desce; menor que a de cima, e nunca zero
 * @param anchorX          o ponto do EIXO do galho que sustenta esta prateleira
 * @param anchorY          idem
 * @param anchorZ          idem
 * @param branchRadius     a grossura da MADEIRA no ponto da ancora
 * @param suporte          se a madeira embaixo e um tubo horizontal ou uma coluna
 * @param branchId         qual galho a sustenta; a ordem dentro do galho e {@code order}
 * @param order            posicao na sequencia do galho, da base para a ponta
 * @param tier             faixa de altitude, para escolher a folha
 */
public record WorldTreeFoliageShelf(
        double centerX, double centerY, double centerZ,
        double radius, double topThickness, double bottomThickness,
        double anchorX, double anchorY, double anchorZ, double branchRadius,
        WorldTreeFoliageShelf.Suporte suporte,
        int branchId, int order, int tier) {

    /**
     * A forma da madeira que sustenta a prateleira.
     *
     * <p><b>ELA EXISTE PORQUE UM PORTAO ESTAVA MEDINDO A COISA ERRADA.</b> A
     * regra "a folha tem de aparecer por fora da madeira" foi escrita pensando em
     * galho: um TUBO horizontal, de onde a folha escapa por cima e em volta. Ela
     * reprovou os discos do lider central -- e estava certa em reprovar pela
     * conta e errada na premissa, porque o lider e uma COLUNA vertical: ali nao
     * existe "acima da madeira", existe apenas "em volta dela".
     *
     * <p>Modelar os dois casos e mais honesto que afrouxar a regra para os dois.
     */
    public enum Suporte {
        /** Galho: tubo em volta de um eixo mais ou menos horizontal. */
        GALHO,
        /** Lider central: coluna vertical que atravessa a prateleira inteira. */
        LIDER
    }

    /*
     * O EXPOENTE VERTICAL DA SUPERELIPSE E QUATRO, e ele nao e uma constante
     * nomeada -- de proposito, e depois de ele ter sido uma e mentir.
     *
     * QUATRO, E NAO DOIS: com dois a formula e a de um elipsoide e a massa vira
     * bolha; com quatro, o topo e a base ficam CHATOS e so o ombro arredonda,
     * que e a leitura de prateleira. E a decisao de forma inteira deste arquivo.
     *
     * POR QUE NAO E UMA CONSTANTE. Havia aqui um `EXPOENTE_VERTICAL = 4.0`
     * publico. Quando `normalized` trocou `Math.pow(v, EXPOENTE_VERTICAL)` por
     * `v2 * v2` -- por custo --, a constante deixou de dirigir qualquer coisa e
     * virou DECORACAO: mudar o 4.0 para 2.0 nao mudava um bloco na tela. Foi o
     * exercicio de alimentar o portao com o defeito que revelou isso, e nao a
     * leitura do codigo. Uma constante que nao manda em nada e o pior tipo de
     * documentacao, porque parece codigo.
     *
     * O expoente aparece em DOIS lugares, e eles precisam concordar:
     *   - `normalized`, como `v2 * v2`;
     *   - `verticalSpanFactor`, como a raiz quarta (raiz da raiz).
     * Se um mudar sem o outro, o laco de desenho passa a cortar a massa antes da
     * borda -- folhagem com o topo raspado, sem erro nenhum. O teste
     * `spanConcordaComAForma` amarra os dois.
     */

    public WorldTreeFoliageShelf {
        if (!Double.isFinite(radius) || radius <= 0.0) {
            throw new IllegalArgumentException("raio de prateleira invalido: " + radius);
        }
        if (!Double.isFinite(topThickness) || topThickness <= 0.0) {
            throw new IllegalArgumentException("prateleira sem espessura em cima");
        }
        if (!Double.isFinite(bottomThickness) || bottomThickness <= 0.0) {
            // Zero aqui e o defeito "folha so por cima": a copa vira guarda-sol.
            throw new IllegalArgumentException("prateleira sem espessura embaixo");
        }
        if (order < 0 || branchId < 0) {
            throw new IllegalArgumentException("identificacao de prateleira invalida");
        }
        if (!Double.isFinite(branchRadius) || branchRadius < 0.0) {
            throw new IllegalArgumentException("raio de galho invalido: " + branchRadius);
        }
    }

    /**
     * Quao dentro da prateleira este ponto esta.
     *
     * <p>Devolve 0 no centro e 1 na casca; acima de 1 esta fora. Quem desenha usa
     * {@code 1 - valor} como PROFUNDIDADE, e e a profundidade -- e nao um sorteio
     * por bloco -- que decide a densidade. Ruido branco no volume inteiro produz
     * confete, que foi o segundo defeito da versao anterior.
     */
    public double normalized(double x, double y, double z) {
        double dx = x - centerX;
        double dz = z - centerZ;
        double dy = y - centerY;
        // SEM `Math.sqrt` E SEM `Math.pow`, e isto nao e microtuning gratuito:
        // este metodo roda milhoes de vezes por chunk na copa. O termo horizontal
        // ja era elevado ao quadrado logo em seguida -- a raiz era desfeita na
        // linha de baixo --, e a quarta potencia e o quadrado do quadrado.
        double horizontalSquared = (dx * dx + dz * dz) / (radius * radius);
        double vertical = dy >= 0.0 ? dy / topThickness : -dy / bottomThickness;
        double v2 = vertical * vertical;
        return horizontalSquared + v2 * v2;
    }

    public boolean contains(double x, double y, double z) {
        return normalized(x, y, z) <= 1.0;
    }

    /**
     * Quantos blocos, em fracao da espessura, a massa alcanca nesta coluna.
     *
     * <p><b>ESTA CONTA E O QUE TORNA A COPA BARATA.</b> Sem ela, quem desenha
     * varre a espessura INTEIRA em cada coluna -- inclusive nas colunas da borda,
     * onde a prateleira tem meio bloco de altura. Numa copa em que uma centena e
     * meia de prateleiras se sobrepoe sobre o mesmo chunk, essa diferenca e a
     * diferenca entre um engasgo ao voar e nada.
     *
     * <p>Sai de resolver a desigualdade da superelipse para o termo vertical: com
     * o horizontal ao quadrado valendo h, o vertical cabe ate a raiz QUARTA de
     * (1 - h). Fora do disco, devolve zero.
     *
     * <p><b>E ELA E A MESMA CONTA QUE A REGUA DE CUSTO USA.</b> Deliberado: se o
     * escritor e o medidor tivessem cada um a sua, o portao mediria um custo que
     * ninguem paga -- e passaria a mentir no dia em que so um dos dois mudasse.
     *
     * @param horizontalSquared a distancia horizontal ao quadrado, sobre o raio ao quadrado
     * @return fator de 0 a 1 a multiplicar pelas espessuras
     */
    public double verticalSpanFactor(double horizontalSquared) {
        if (horizontalSquared >= 1.0) {
            return 0.0;
        }
        // Raiz quarta = raiz da raiz. Mais barato e mais preciso que Math.pow.
        return Math.sqrt(Math.sqrt(1.0 - horizontalSquared));
    }

    /**
     * Quantos blocos o desenho visitaria nesta prateleira, dentro de um chunk.
     *
     * <p>E a soma do span vertical sobre as colunas do chunk que caem no disco. O
     * portao de custo soma isto sobre todas as prateleiras que tocam o chunk, e e
     * por isso que ele mede o que o gerador realmente paga.
     */
    public long estimatedVisitsInChunk(int minX, int minZ) {
        long visits = 0;
        double raio2 = radius * radius;
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                double dx = x - centerX;
                double dz = z - centerZ;
                double factor = verticalSpanFactor((dx * dx + dz * dz) / raio2);
                if (factor <= 0.0) {
                    continue;
                }
                visits += (long) Math.ceil(factor * (topThickness + bottomThickness)) + 1L;
            }
        }
        return visits;
    }

    /** Distancia horizontal entre o centro do disco e a ancora no galho. */
    public double anchorOffset() {
        double dx = centerX - anchorX;
        double dz = centerZ - anchorZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * O quanto da massa aparece POR FORA da madeira, na vertical.
     *
     * <p><b>ESTE NUMERO EXISTE PARA UM DEFEITO ESPECIFICO, e ele foi encontrado
     * alimentando o portao.</b> Zerar o deslocamento vertical da prateleira --
     * o defeito ORIGINAL desta trilha, que fazia a copa sumir -- passava por
     * todas as outras verificacoes: o disco continuava ancorado, chato, com as
     * duas faces e sobreposto ao vizinho. So que ele ficava DENTRO do galho, e
     * como folha nao sobrescreve madeira, nao virava bloco nenhum.
     *
     * <p>Nenhuma regua pegava, porque todas olhavam a prateleira sozinha. Esta
     * olha a prateleira CONTRA a madeira que ela veste.
     *
     * @return quantos blocos de folha sobram acima do topo da madeira
     */
    public double clearanceAboveWood() {
        return maxY() - (anchorY + branchRadius);
    }

    /**
     * Se a folha desta prateleira realmente aparece fora da madeira.
     *
     * <p>A pergunta e a mesma para os dois suportes -- "sobra folha visivel?" --,
     * mas a conta nao pode ser: num tubo horizontal a folha escapa por cima e em
     * volta; numa coluna vertical, so em volta.
     */
    public boolean escapesWood() {
        if (suporte == Suporte.LIDER) {
            return clearanceAroundWood() > branchRadius;
        }
        return clearanceAboveWood() > 1.0 && clearanceAroundWood() > branchRadius * 0.5;
    }

    /** Idem na horizontal: quanto o disco passa da grossura do galho. */
    public double clearanceAroundWood() {
        return radius - branchRadius;
    }

    /** Volume aproximado em blocos, para o teto de custo. */
    public double approximateVolume() {
        // Superelipse de expoente 4 no eixo vertical preenche ~0,84 do cilindro.
        return Math.PI * radius * radius * (topThickness + bottomThickness) * 0.84;
    }

    public double minY() {
        return centerY - bottomThickness;
    }

    public double maxY() {
        return centerY + topThickness;
    }
}
