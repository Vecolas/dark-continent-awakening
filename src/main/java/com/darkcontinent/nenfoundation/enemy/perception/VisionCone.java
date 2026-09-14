package com.darkcontinent.nenfoundation.enemy.perception;

/**
 * Cone de visao: alcance e abertura, sem mundo e sem entidade.
 *
 * <p>A abertura e guardada como COSSENO, e nao como angulo, de proposito: o
 * consumidor ja tem o produto escalar de dois vetores normalizados na mao, e
 * converter para graus a cada teste seria um {@code Math.acos} por candidato por
 * varredura. O custo e pequeno uma vez e visivel quarenta vezes por tick.</p>
 */
public record VisionCone(double alcance, double cossenoDeAbertura) {
    public VisionCone {
        if (!Double.isFinite(alcance) || alcance <= 0.0D
                || !Double.isFinite(cossenoDeAbertura)
                || cossenoDeAbertura < -1.0D || cossenoDeAbertura > 1.0D) {
            throw new IllegalArgumentException("cone de visao invalido");
        }
    }

    /** Cone a partir de meia-abertura em graus -- usado por quem pensa em angulo. */
    public static VisionCone deGraus(double alcance, double meiaAberturaEmGraus) {
        if (!Double.isFinite(meiaAberturaEmGraus) || meiaAberturaEmGraus <= 0.0D
                || meiaAberturaEmGraus > 180.0D) {
            throw new IllegalArgumentException("abertura invalida: " + meiaAberturaEmGraus);
        }
        return new VisionCone(alcance, Math.cos(Math.toRadians(meiaAberturaEmGraus)));
    }

    /**
     * @param distancia distancia ate o candidato, em blocos
     * @param cossenoDoOlhar produto escalar entre o olhar horizontal do mob e a
     *        direcao horizontal ate o candidato; 1 e bem de frente
     * @param linhaDeVisao se o servidor confirmou que nao ha bloco no caminho
     */
    public boolean enxerga(double distancia, double cossenoDoOlhar, boolean linhaDeVisao) {
        if (!Double.isFinite(distancia) || !Double.isFinite(cossenoDoOlhar)) {
            throw new IllegalArgumentException("geometria de visao invalida");
        }
        return linhaDeVisao && distancia <= alcance && cossenoDoOlhar >= cossenoDeAbertura;
    }
}
