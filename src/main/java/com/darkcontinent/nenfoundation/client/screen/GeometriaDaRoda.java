package com.darkcontinent.nenfoundation.client.screen;

import java.util.OptionalInt;

/**
 * Onde o mouse esta apontando, na roda.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. E MATEMATICA PURA, sem {@code Minecraft}, sem {@code GuiGraphics} e sem
 * estado. Selecao de menu radial erra de um jeito especifico -- a fatia de cima
 * partida ao meio, o indice deslocado em um, o miolo selecionando a fatia da
 * direita -- e todos esses erros sao invisiveis lendo o codigo e obvios num
 * teste. Separado assim, o teste roda sem o jogo de pe.
 *
 * <p>2. A ZONA MORTA EXISTE E DEVOLVE VAZIO. Sem ela, o mouse parado no centro
 * "seleciona" a fatia da direita -- porque o angulo de (0,0) e zero --, e o
 * jogador ativa uma tecnica que nunca apontou. Vazio nao e erro: e "ainda nao
 * escolheu".
 *
 * <p>3. O ZERO E EM CIMA, e o sentido e horario. E o que a referencia visual
 * mostra e o que todo menu radial faz. Comecar na direita, como o atan2 cru
 * faria, giraria a roda inteira um quarto de volta em relacao ao desenho.
 */
public final class GeometriaDaRoda {

    private GeometriaDaRoda() {
    }

    /**
     * Qual fatia o ponto aponta, ou vazio dentro da zona morta.
     *
     * @param dx           deslocamento horizontal do centro, em pixels
     * @param dy           deslocamento vertical do centro, em pixels, com Y
     *                     crescendo PARA BAIXO como em toda tela
     * @param fatias       quantas fatias a roda tem; zero devolve vazio
     * @param raioDaZonaMorta raio do miolo que nao seleciona nada
     */
    public static OptionalInt fatiaEm(double dx, double dy, int fatias, double raioDaZonaMorta) {
        if (fatias <= 0) {
            return OptionalInt.empty();
        }
        if (!Double.isFinite(dx) || !Double.isFinite(dy)) {
            // Nao deveria acontecer com coordenada de tela, mas NaN aqui
            // viraria um indice negativo la na frente, e o sintoma seria uma
            // excecao no meio do render.
            return OptionalInt.empty();
        }
        double distancia = Math.hypot(dx, dy);
        if (distancia < Math.max(0.0D, raioDaZonaMorta)) {
            return OptionalInt.empty();
        }

        // atan2(dx, -dy) da ZERO PARA CIMA e cresce no sentido horario, que e
        // como a roda e desenhada. O atan2(dy, dx) usual daria zero a direita
        // e cresceria anti-horario -- a roda inteira girada e espelhada.
        double angulo = Math.atan2(dx, -dy);
        if (angulo < 0.0D) {
            angulo += 2.0D * Math.PI;
        }

        double porFatia = 2.0D * Math.PI / fatias;

        // METADE DE UMA FATIA DE DESLOCAMENTO. Sem isso, a fronteira entre duas
        // fatias cai exatamente em cima, e a fatia do topo -- a mais obvia de
        // apontar -- fica partida ao meio entre a primeira e a ultima.
        double deslocado = (angulo + porFatia / 2.0D) % (2.0D * Math.PI);

        int indice = (int) (deslocado / porFatia);
        // Cinto de seguranca contra a borda: com `angulo` muito perto de 2*PI,
        // o arredondamento de ponto flutuante pode entregar exatamente `fatias`.
        return OptionalInt.of(Math.min(indice, fatias - 1));
    }

    /**
     * O angulo do CENTRO de uma fatia, em radianos, com zero em cima e sentido
     * horario. Para desenhar.
     */
    public static double anguloCentralDaFatia(int indice, int fatias) {
        if (fatias <= 0) {
            throw new IllegalArgumentException("uma roda sem fatias nao tem centro de fatia");
        }
        return (2.0D * Math.PI / fatias) * indice;
    }
}
