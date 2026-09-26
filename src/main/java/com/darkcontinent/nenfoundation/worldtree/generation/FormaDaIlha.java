package com.darkcontinent.nenfoundation.worldtree.generation;

/**
 * A conta da ilha, sem uma linha de Minecraft.
 *
 * <p><b>ELA FOI SEPARADA DE {@code FuncaoDeIlha} PORQUE O PORTAO NAO CONSEGUIA
 * ENTRAR.</b> A classe de densidade constroi um {@code KeyDispatchDataCodec}
 * num campo estatico, e inicializar isso exige meia pilha do jogo carregada --
 * a suite JUnit deste projeto roda sem o Minecraft, de proposito. O teste
 * morria com {@code NoClassDefFoundError} antes de chegar na primeira
 * asserção, e o que estava sendo bloqueado era justamente a parte que tem
 * regra: a forma da costa.
 *
 * <p>Agora {@code FuncaoDeIlha} e uma casca -- ela adapta esta conta a
 * interface da vanilla e nao decide nada. A regra mora aqui, e da para
 * provar a curva inteira sem subir o jogo.
 *
 * <p><b>PONTO CEGO DECLARADO:</b> esta forma e um DISCO com borda suavizada.
 * A direcao de arte de 2026-09-26 pede o oposto -- "a costa nao pode ser um
 * circulo", com peninsulas, enseadas, falesias e ilhotas, vindas de uma
 * mascara desenhada mais SDF mais ruido de baixa frequencia. Esta conta e o
 * ANDAIME que tira Greed Island do superplano enquanto a macrogeografia
 * projetada nao existe. Ver {@code docs/greedisland/macrogeografia.md}.
 */
public final class FormaDaIlha {

    private FormaDaIlha() {
    }

    /**
     * Quanto aquela coluna e ilha, de {@code +1} (terra) a {@code -1} (mar).
     *
     * <p>A DISTANCIA E RADIAL, e nao a soma dos eixos: Manhattan daria um
     * losango, e um losango nao le como ilha.
     *
     * @param dx        deslocamento em X a partir do centro
     * @param dz        deslocamento em Z
     * @param raio      onde a terra e garantida
     * @param transicao a largura da costa, do fim da terra ao mar aberto
     */
    public static double valorEm(double dx, double dz, double raio, double transicao) {
        double distancia = Math.sqrt(dx * dx + dz * dz);
        if (distancia <= raio) {
            return 1.0D;
        }
        double alem = distancia - raio;
        return Math.clamp(1.0D - 2.0D * (alem / transicao), -1.0D, 1.0D);
    }

    /**
     * As medidas que fazem sentido, conferidas no lugar de supostas.
     *
     * <p>TRANSICAO ZERO SERIA UM PAREDAO: a costa viraria um degrau de pedra de
     * cem blocos e o mar comecaria num corte reto -- le como mundo cortado, e
     * nao como ilha.
     */
    public static void exigirMedidasValidas(double raio, double transicao) {
        if (!Double.isFinite(raio) || raio <= 0.0D) {
            throw new IllegalArgumentException("raio da ilha invalido: " + raio);
        }
        if (!Double.isFinite(transicao) || transicao <= 0.0D) {
            throw new IllegalArgumentException("transicao invalida: " + transicao);
        }
    }
}
