package com.darkcontinent.nenfoundation.enemy.encounter;

/**
 * Regras da PESCA -- a mecanica principal do master of the swamp.
 *
 * <p>DECISAO CENTRAL QUE ESTE RECORD CARREGA: este mob nao e um inimigo que se
 * MATA, e um que se FISGA. O combate dele e secundario de proposito -- existe
 * para quem insistir em bater, e e chato por decisao, nao por descuido. O
 * encontro bom termina com o peixe recolhido, e quem decide se isso acontece
 * sao as quatro perguntas deste arquivo: a isca merece investigacao, quanta
 * tensao a linha esta levando, a linha arrebentou, e o peixe ja cansou.</p>
 *
 * <p>POR QUE ELAS MORAM JUNTAS E FORA DA ENTIDADE. Espalhadas pelo tick do
 * peixe e pela Goal que persegue a isca, cada uma viraria um {@code if}
 * diferente, e a divergencia entre eles nao daria erro nenhum: apareceria como
 * uma linha que arrebenta cedo demais num dia e nunca no outro -- o pior
 * relato de bug que existe. Aqui elas sao uma fonte so, sem mundo, sem
 * entidade e sem numero proprio: todos os numeros chegam do perfil de
 * balanceamento.</p>
 *
 * <p>O CABO DE GUERRA, em uma frase: puxar demais (afastar-se do peixe) SOBE a
 * tensao ate arrebentar; acompanhar o peixe (aproximar-se) ALIVIA, e o tempo
 * acumulado de linha presa e o que cansa o bicho. As duas pontas usam a mesma
 * medida -- a variacao de distancia entre o peixe e quem segura a vara.</p>
 *
 * @param raioDaIsca distancia maxima, em blocos, para o peixe se interessar
 *     por um anzol na agua
 * @param ticksParaCansar quanto tempo de linha presa esgota o peixe
 * @param tensaoMaxima a partir daqui a linha arrebenta
 * @param tensaoPorAfastamento tensao ganha por bloco de AFASTAMENTO
 * @param alivioPorAproximacao tensao perdida por bloco de APROXIMACAO
 */
public record RegrasDeFisgada(double raioDaIsca, int ticksParaCansar, double tensaoMaxima,
        double tensaoPorAfastamento, double alivioPorAproximacao) {

    public RegrasDeFisgada {
        if (!Double.isFinite(raioDaIsca) || !Double.isFinite(tensaoMaxima)
                || !Double.isFinite(tensaoPorAfastamento) || !Double.isFinite(alivioPorAproximacao)
                || raioDaIsca <= 0.0D || ticksParaCansar < 1 || tensaoMaxima <= 0.0D
                || tensaoPorAfastamento <= 0.0D || alivioPorAproximacao <= 0.0D) {
            throw new IllegalArgumentException("regras de fisgada invalidas");
        }
    }

    /**
     * So investiga isca NA AGUA, com a espera zerada e dentro do raio.
     *
     * <p>As tres condicoes juntas sao o que impede o peixe de sair atras de um
     * anzol pendurado numa arvore (fora d'agua), de morder o mesmo jogador tres
     * vezes seguidas depois de escapar (espera), e de atravessar o pantano
     * inteiro por causa de uma boia que ele nao poderia ter percebido (raio).</p>
     *
     * <p>Distancia nao finita REPROVA: um NaN vindo de um anzol em estado
     * estranho nao pode virar uma fisgada de graca, e comparacao com NaN ja e
     * falsa nas duas pontas.</p>
     */
    public boolean investigaIsca(boolean iscaNaAgua, double distanciaDaIsca, int esperaRestante) {
        if (!Double.isFinite(distanciaDaIsca)) return false;
        return iscaNaAgua && esperaRestante <= 0 && distanciaDaIsca <= raioDaIsca;
    }

    /**
     * A tensao do proximo tick, a partir da tensao atual e da VARIACAO de
     * distancia entre o peixe e quem segura a vara.
     *
     * <p>Variacao POSITIVA quer dizer que os dois se afastaram -- alguem puxou.
     * Variacao NEGATIVA quer dizer que se aproximaram -- alguem acompanhou. A
     * tensao nunca desce abaixo de zero: linha frouxa e linha frouxa, e um
     * saldo negativo guardado viraria um credito invisivel que compraria um
     * puxao gratis depois.</p>
     *
     * <p>VARIACAO NAO FINITA DEVOLVE A TENSAO ATUAL INTACTA, e esta e a regra
     * que mais importa deste metodo. O primeiro tick de linha presa nao tem
     * medida anterior para comparar, e um jogador que troca de dimensao ou
     * morre produz distancias que nao querem dizer nada. Tratar esse caso como
     * "zero" aliviaria de graca, e trata-lo como "muito" arrebentaria a linha
     * por um dado que nunca existiu -- nenhum dos dois daria erro.</p>
     */
    public double tensao(double tensaoAtual, double variacaoDeDistancia) {
        if (!Double.isFinite(variacaoDeDistancia)) return tensaoAtual;
        double nova = variacaoDeDistancia > 0.0D
                ? tensaoAtual + tensaoPorAfastamento * variacaoDeDistancia
                : tensaoAtual - alivioPorAproximacao * Math.abs(variacaoDeDistancia);
        return Math.max(0.0D, nova);
    }

    /** A linha arrebentou; quem chamou perde o peixe e o peixe mergulha. */
    public boolean arrebenta(double tensao) {
        return tensao >= tensaoMaxima;
    }

    /**
     * O peixe esgotou e para de resistir -- e so agora ele pode ser recolhido.
     *
     * <p>E o tempo de linha PRESA que conta, nao o tempo de encontro: quem
     * deixa a linha arrebentar e fisga de novo recomeca do zero, e e por isso
     * que a resposta ensinada e acompanhar, nao puxar.</p>
     */
    public boolean cansou(int ticksFisgado) {
        return ticksFisgado >= ticksParaCansar;
    }
}
