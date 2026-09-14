package com.darkcontinent.nenfoundation.worldtree;

/**
 * ONDE a casca do tronco esta, de verdade.
 *
 * <p><b>POR QUE ESTE ARQUIVO EXISTE.</b> A forma da superficie do tronco vivia
 * dentro do laco que escreve blocos, e quem precisava ENCOSTAR alguma coisa nela
 * -- a ancora de escalada dos checkpoints -- usava o raio NOMINAL do perfil.
 *
 * <p>Os dois numeros nao sao o mesmo, e a diferenca nao e pequena: o tronco tem
 * lobos de ate 4,5 blocos e ruido de ate 2, entao a casca real fica entre 6,5
 * blocos para dentro e 6,5 para fora do nominal. A ancora era posta a
 * {@code nominal + 2}, e o resultado em jogo era exatamente o que se espera de
 * uma conta que ignora a outra: as vezes ENTERRADA na madeira, as vezes
 * FLUTUANDO a metros da casca. Nunca deu erro.
 *
 * <p>Era o erro numero 7 do CLAUDE.md na forma geometrica: duas fontes para a
 * mesma verdade, uma delas escondida dentro de um laco de desenho.
 *
 * <p><b>SEM MINECRAFT, DE PROPOSITO.</b> Quem encosta na casca precisa
 * perguntar onde ela esta ANTES de existir chunk -- e o portao precisa medir
 * isso em vinte seeds, em JUnit.
 */
public final class WorldTreeTrunkSurface {

    /**
     * Quantas vezes iterar o ponto fixo do raio.
     *
     * <p>A conta e circular por construcao: o raio da casca depende de {@code x},
     * e {@code x} e o raio. O termo que cria a circularidade e o ruido, com
     * periodo de ~33 blocos e amplitude 2 -- um bloco de erro em {@code x} move o
     * resultado menos de 0,4. Tres passos levam o residuo para baixo de 0,01.
     */
    private static final int PASSOS = 3;

    private WorldTreeTrunkSurface() {
    }

    /**
     * O raio da casca num ponto, com lobos e ruido.
     *
     * <p>E a MESMA funcao que {@code WorldTreeTrunkGenerator} usa para decidir
     * onde para a madeira. Ela mudou de casa e nao foi copiada: uma copia
     * divergiria na primeira correcao de forma, e o sintoma seria a ancora
     * saindo do lugar sem ninguem ter tocado nela.
     */
    public static double irregularRadius(double baseRadius, int x, int y, int z, long seed) {
        double angle = Math.atan2(z, x);
        double lobes = Math.sin(angle * 3.0 + y * 0.013) * 3.0
                + Math.sin(angle * 7.0 - y * 0.021) * 1.5;
        double noise = Math.sin(x * 0.19 + y * 0.037 + seed * 0.0000017)
                * Math.cos(z * 0.17 - y * 0.029 + seed * 0.0000011) * 2.0;
        return Math.max(1.0, baseRadius + lobes + noise);
    }

    /** Onde o lider central comeca e acaba. Os mesmos numeros do desenho dele. */
    public static final int LIDER_BASE = 1100;
    public static final int LIDER_TOPO = 1450;

    /**
     * O eixo do lider central naquela altura.
     *
     * <p>MUDOU DE CASA por exatamente o mesmo motivo que a casca do tronco: quem
     * precisa encostar alguma coisa no lider -- a cabana dos checkpoints acima de
     * y=1200, onde o tronco ja acabou -- precisa perguntar isto antes de existir
     * chunk. Enquanto morou so no gerador da coroa, a alternativa era chutar.
     */
    public static WorldTreePoint leaderCenter(int y, long seed) {
        double t = Math.max(0.0, Math.min(1.0, (y - LIDER_BASE)
                / (double) (LIDER_TOPO - LIDER_BASE)));
        return new WorldTreePoint(
                Math.sin(y * 0.018 + seed * 0.0000011) * (1.5 + t * 2.5),
                y,
                Math.cos(y * 0.015 - seed * 0.0000017) * (1.5 + t * 2.0));
    }

    /** A grossura do lider central naquela altura. */
    public static double leaderRadius(int y) {
        double t = Math.max(0.0, Math.min(1.0, (y - LIDER_BASE)
                / (double) (LIDER_TOPO - LIDER_BASE)));
        return 19.0 - t * 12.0;
    }

    /**
     * Ate onde a casca chega na direcao +X, na altura {@code y}.
     *
     * <p>E o numero que qualquer coisa presa ao tronco precisa: o eixo +X e onde
     * os checkpoints moram, por decisao antiga (z = 0).
     *
     * @param centerRadius o raio NOMINAL do perfil naquela altura
     */
    public static double naDirecaoX(double centerRadius, int y, long seed) {
        double raio = centerRadius;
        for (int passo = 0; passo < PASSOS; passo++) {
            raio = irregularRadius(centerRadius, (int) Math.round(raio), y, 0, seed);
        }
        return raio;
    }
}
