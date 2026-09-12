package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * Regras do agarrao: quanto tempo a vitima fica presa, de quanto em quanto
 * tempo ela apanha, e quanto dano no PREDADOR compra a soltura.
 *
 * <p>Carrega a decisao central do mob 04: engolir NAO MATA NA HORA. Existe uma
 * janela em que a vitima bate para escapar, ou um aliado bate por ela. Sem
 * essa janela o sapo vira morte instantanea sem aviso.</p>
 *
 * <p>Logica pura: sem mundo, sem entidade, sem numero proprio. Os numeros
 * chegam do perfil de balanceamento.</p>
 */
public record GrabRules(int ticksMaximos, int intervaloDeDano, float danoPorPulso, float danoParaEscapar) {
    public GrabRules {
        if (ticksMaximos < 1 || intervaloDeDano < 1
                || !Float.isFinite(danoPorPulso) || danoPorPulso < 0.0F
                || !Float.isFinite(danoParaEscapar) || danoParaEscapar <= 0.0F) {
            throw new IllegalArgumentException("regras de agarrao invalidas");
        }
    }

    /**
     * Pulso de dano a cada {@code intervaloDeDano} ticks de agarrao.
     *
     * <p>O tick 0 NAO pulsa: engolir nao machuca no mesmo tick em que agarra.
     * Sem essa excecao a vitima levaria o primeiro pulso antes de ter um unico
     * tick para reagir, e a janela de escape comecaria ja com dano no caixa.</p>
     */
    public boolean aplicaDano(int ticksAgarrado) {
        return ticksAgarrado > 0 && ticksAgarrado % intervaloDeDano == 0;
    }

    /** A vitima que nao reagiu sai viva mesmo assim -- cuspida, nao digerida. */
    public boolean soltaPorTempo(int ticksAgarrado) {
        return ticksAgarrado >= ticksMaximos;
    }

    /** Dano acumulado NO PREDADOR desde o agarrao; e isto que faz bater funcionar. */
    public boolean soltaPorDano(float danoAcumulado) {
        return danoAcumulado >= danoParaEscapar;
    }

    /**
     * UNICA funcao que decide soltar.
     *
     * <p>As quatro razoes -- tempo, dano, vitima morta, predador morto -- moram
     * aqui juntas de proposito. Espalhar a decisao pelos pontos de saida (um
     * {@code if} na morte, outro no tick, outro na remocao) e exatamente como o
     * agarrao fica ligado para sempre em UM deles: o caminho esquecido nao da
     * erro nenhum, so deixa um jogador preso dentro de um sapo ate o servidor
     * reiniciar.</p>
     */
    public boolean solta(int ticksAgarrado, float danoAcumulado, boolean vitimaMorta, boolean predadorMorto) {
        return soltaPorTempo(ticksAgarrado) || soltaPorDano(danoAcumulado) || vitimaMorta || predadorMorto;
    }
}
