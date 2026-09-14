package com.darkcontinent.nenfoundation.enemy.perception;

import java.util.UUID;

/**
 * Som que chegou ao mob, com a fonte JA identificada pelo servidor.
 *
 * <p>Audicao aqui e EVENTO, nunca varredura. A tentacao e implementar "ouvir"
 * como um scan por entidades barulhentas a cada tick -- e isso custa o mesmo que
 * a visao, sem nenhum dos limites dela. Quem produz este record e o lado que ja
 * estava tratando o acontecimento (um golpe, um passo, um bloco quebrado); o mob
 * so consome.</p>
 *
 * @param fonte quem fez o som; nunca nulo, porque memoria de ameaca sem dono nao
 *        reconstroi alvo nenhum
 * @param distancia distancia medida pelo servidor, em blocos
 * @param intensidade 0..1 -- um passo agachado e fraco, uma explosao e 1
 */
public record HearingEvent(UUID fonte, double distancia, double intensidade) {
    public HearingEvent {
        if (fonte == null) throw new NullPointerException("som sem fonte identificada");
        if (!Double.isFinite(distancia) || distancia < 0.0D
                || !Double.isFinite(intensidade) || intensidade <= 0.0D || intensidade > 1.0D) {
            throw new IllegalArgumentException("evento de audicao invalido");
        }
    }

    /**
     * Alcance efetivo: som fraco nao viaja tanto quanto som forte.
     *
     * <p>A conta e uma multiplicacao de proposito. Atenuacao logaritmica seria
     * mais fiel e ninguem conseguiria prever de quanto longe um mob ouve -- e um
     * numero que a proxima pessoa vai querer girar precisa ser previsivel.</p>
     */
    public boolean audivel(double alcanceMaximo) {
        if (!Double.isFinite(alcanceMaximo) || alcanceMaximo <= 0.0D) {
            throw new IllegalArgumentException("alcance de audicao invalido");
        }
        return distancia <= alcanceMaximo * intensidade;
    }
}
