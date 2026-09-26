package com.darkcontinent.nenfoundation.enemy.greedisland.layout;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants;
import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Lago;
import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Rio;

/**
 * Os sete sistemas hidrograficos e os oito lagos. Fase G3.
 *
 * <p>A secao 24 poe o principio: <i>nada de pequenos rios vanilla cortando o
 * mapa aleatoriamente</i>. Um rio aqui tem nascente, curso, largura que cresce
 * rio abaixo, lago e foz -- e a foz e no mar.
 *
 * <p><b>A LARGURA CRESCE DA NASCENTE A FOZ</b>, e isso nao e enfeite: e o que
 * permite ao jogador saber para que lado desce a agua sem bussola. Um rio de
 * largura constante nao informa nada, e a secao 34 lista a escada -- de 2-5
 * blocos na cabeceira ate dezenas na foz.
 *
 * <p><b>O RIO ESCAVA, E NAO PINTA.</b> Ele rebaixa o terreno ate abaixo do
 * nivel local, com margem. Um rio desenhado como bloco de agua sobre o relevo
 * corre morro acima em qualquer lugar onde o terreno suba -- e isso nao gera
 * erro, gera um rio subindo a montanha.
 *
 * <p>SEM MINECRAFT, como todo o pacote.
 */
public final class GreedIslandHydrologyField {

    /** Amostras por segmento ao seguir o curso. */
    private static final int AMOSTRAS = 16;

    /** Quanto o leito fica abaixo da margem, em blocos. */
    private static final int PROFUNDIDADE = 4;

    /** Largura da margem escavada, em multiplos da largura do rio. */
    private static final double MARGEM = 2.6D;

    /**
     * Os cursos ja amostrados, uma vez.
     *
     * <p><b>O SERVIDOR MORREU SEM ISTO.</b> A versao anterior reinterpolava a
     * Catmull-Rom de cada rio a cada consulta -- sete rios, ~6 segmentos, 16
     * amostras = mais de 670 contas de distancia POR PERGUNTA. E `alturaEm`
     * pergunta, e `cotaDe` chamava `alturaEm` mil vezes, e a feature chamava
     * `cotaDe` por bloco. O watchdog matou o servidor com um tick de 60 s.
     *
     * <p>A curva nao muda nunca: ela sai de constantes. Recalcular era
     * refazer, a cada bloco, uma conta cujo resultado ja se sabia.
     */
    private static final java.util.Map<String, double[][]> CURSOS = amostrarCursos();

    private static java.util.Map<String, double[][]> amostrarCursos() {
        var mapa = new java.util.HashMap<String, double[][]>();
        for (Rio rio : GreedIslandConstants.RIOS) {
            var nos = rio.curso();
            int total = (nos.size() - 1) * AMOSTRAS + 1;
            double[][] pontos = new double[total][2];
            int k = 0;
            for (int i = 0; i < nos.size() - 1; i++) {
                var p0 = nos.get(Math.max(0, i - 1));
                var p1 = nos.get(i);
                var p2 = nos.get(i + 1);
                var p3 = nos.get(Math.min(nos.size() - 1, i + 2));
                for (int a = 0; a < AMOSTRAS; a++) {
                    double t = a / (double) AMOSTRAS;
                    pontos[k][0] = GreedIslandRidgeField.catmull(
                            p0.x(), p1.x(), p2.x(), p3.x(), t);
                    pontos[k][1] = GreedIslandRidgeField.catmull(
                            p0.z(), p1.z(), p2.z(), p3.z(), t);
                    k++;
                }
            }
            var ultimo = nos.get(nos.size() - 1);
            pontos[k][0] = ultimo.x();
            pontos[k][1] = ultimo.z();
            mapa.put(rio.id(), pontos);
        }
        return java.util.Map.copyOf(mapa);
    }

    private GreedIslandHydrologyField() {
    }

    /** O que a hidrografia faz com um ponto: nada, margem, agua corrente ou lago. */
    public enum Agua {
        /** Terra seca. */
        NENHUMA,
        /** A margem escavada, ainda seca mas ja rebaixada. */
        MARGEM,
        /** O leito do rio. */
        RIO,
        /** Um lago. */
        LAGO
    }

    /** Que tipo de agua ha neste ponto. */
    public static Agua aguaEm(double x, double z) {
        for (Lago l : GreedIslandConstants.LAGOS) {
            if (Math.hypot(x - l.x(), z - l.z()) <= l.raio()) {
                return Agua.LAGO;
            }
        }
        double melhor = Double.MAX_VALUE;
        double larguraNoPonto = 0.0D;
        for (Rio r : GreedIslandConstants.RIOS) {
            double[] medida = medir(x, z, r);
            if (medida[0] < melhor) {
                melhor = medida[0];
                larguraNoPonto = medida[1];
            }
        }
        if (melhor <= larguraNoPonto / 2.0D) {
            return Agua.RIO;
        }
        if (melhor <= larguraNoPonto * MARGEM) {
            return Agua.MARGEM;
        }
        return Agua.NENHUMA;
    }

    /**
     * Quanto a hidrografia rebaixa o terreno neste ponto, em blocos.
     *
     * <p>A MARGEM DESCE SUAVE ate o leito. Sem isso, o rio vira uma vala de
     * paredes retas -- e o documento pede perfil de margem, na secao 35.
     */
    public static double escavacao(double x, double z) {
        double melhor = Double.MAX_VALUE;
        double larguraNoPonto = 0.0D;
        for (Rio r : GreedIslandConstants.RIOS) {
            double[] medida = medir(x, z, r);
            if (medida[0] < melhor) {
                melhor = medida[0];
                larguraNoPonto = medida[1];
            }
        }
        double alcance = larguraNoPonto * MARGEM;
        if (alcance <= 0.0D || melhor >= alcance) {
            return 0.0D;
        }
        double t = melhor / alcance;
        double perfil = 0.5D * (1.0D + Math.cos(Math.PI * t));
        return (PROFUNDIDADE + larguraNoPonto * 0.25D) * perfil * perfil;
    }

    /**
     * Distancia ate o curso e largura do rio naquele trecho.
     *
     * <p>Devolve os dois juntos porque a largura DEPENDE de onde no curso o
     * ponto mais proximo caiu -- calcular em duas passadas percorreria a
     * polilinha duas vezes e daria para os dois discordarem.
     *
     * <p>PUBLICA para o portao conferir a escada de largura da secao 34 sem
     * precisar reimplementar a interpolacao do curso -- um teste que refaz a
     * conta prova que a copia dele funciona, e nao que o codigo funciona.
     *
     * @return {@code [distancia, largura]}
     */
    public static double[] medir(double x, double z, Rio rio) {
        double[][] pontos = CURSOS.get(rio.id());
        double menor = Double.MAX_VALUE;
        double progresso = 0.0D;
        int passos = pontos.length - 1;

        for (int i = 0; i < pontos.length; i++) {
            double dx = x - pontos[i][0];
            double dz = z - pontos[i][1];
            // QUADRADO, e nao hipotenusa: a raiz e a conta mais cara do laco e
            // nao muda qual ponto e o mais proximo. Ela entra uma vez, no fim.
            double d2 = dx * dx + dz * dz;
            if (d2 < menor) {
                menor = d2;
                progresso = i / (double) passos;
            }
        }
        menor = Math.sqrt(menor);
        // A ESCADA DA SECAO 34: cabeceira estreita, foz larga. A raiz faz o rio
        // engrossar depressa no comeco e devagar depois, que e como bacia se
        // comporta -- linear faria a largura parecer um degrau constante.
        double largura = 4.0D + (rio.larguraNaFoz() - 4.0D) * Math.sqrt(progresso);
        return new double[] {menor, largura};
    }

    /**
     * A que distancia passa o curso de rio mais proximo, em blocos.
     *
     * <p>EXISTE PARA O MAPA, e o motivo e honesto: a 115 blocos por pixel, um
     * rio de 20 blocos de largura ocupa um sexto de pixel e SOME. O primeiro
     * mapa exportado com hidrografia mostrou os oito lagos e nenhum dos sete
     * rios.
     *
     * <p>Mapa e DIAGRAMA, e nao fotografia: carta nautica nenhuma desenha rio
     * em escala. Quem desenha decide a espessura minima; o que nao pode e o
     * MUNDO ficar mais largo por causa disso -- e por isso esta funcao devolve
     * distancia, e nao um "rio mais grosso".
     */
    public static double distanciaAoRio(double x, double z) {
        double menor = Double.MAX_VALUE;
        for (Rio r : GreedIslandConstants.RIOS) {
            menor = Math.min(menor, medir(x, z, r)[0]);
        }
        return menor;
    }

    /** Se este ponto e agua de qualquer tipo. */
    public static boolean molhado(double x, double z) {
        Agua a = aguaEm(x, z);
        return a == Agua.RIO || a == Agua.LAGO;
    }
}
