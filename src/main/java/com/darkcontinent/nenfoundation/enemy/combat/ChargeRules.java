package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * Regras da carga telegrafada, sem mundo e sem entidade.
 *
 * <p>Carrega a decisao de que "quando a carga pode comecar", "quando bater na
 * parede atordoa" e "quando o corpo machuca" sao UMA fonte so, testavel sozinha.
 * Espalhadas pela Goal, cada condicao viraria um {@code if} diferente e a
 * divergencia entre elas nao daria erro nenhum: apareceria como uma carga que
 * as vezes acerta duas vezes.</p>
 *
 * <p>Os numeros sao injetados pelo perfil de balanceamento; este record nao
 * conhece nenhum deles.</p>
 */
public record ChargeRules(double distanciaMinima, double distanciaMaxima, int ticksDeEspera,
        double multiplicadorDeVelocidade, int ticksDeAtordoamento) {
    public ChargeRules {
        if (!Double.isFinite(distanciaMinima) || !Double.isFinite(distanciaMaxima)
                || !Double.isFinite(multiplicadorDeVelocidade)
                || distanciaMinima < 0.0D || distanciaMaxima <= distanciaMinima
                || ticksDeEspera < 0 || multiplicadorDeVelocidade <= 1.0D
                || ticksDeAtordoamento < 0) {
            throw new IllegalArgumentException("regras de carga invalidas");
        }
    }

    /**
     * So comeca com alvo visivel, espera zerada e distancia dentro da faixa
     * (inclusiva nas duas pontas). Distancia nao finita reprova por comparacao.
     */
    public boolean podeIniciar(boolean alvoVisivel, double distancia, int esperaRestante) {
        return alvoVisivel && esperaRestante == 0
                && distancia >= distanciaMinima && distancia <= distanciaMaxima;
    }

    /** Bater na parede so atordoa DURANTE a carga; fora dela e so andar contra um bloco. */
    public boolean atordoa(AttackPhase fase, boolean colidiuHorizontalmente) {
        return fase == AttackPhase.ACTIVE && colidiuHorizontalmente;
    }

    /** Uma vitima por carga: a janela fecha no primeiro acerto, nao no fim da fase. */
    public boolean janelaDeDano(AttackPhase fase, boolean jaAcertouNestaCarga) {
        return fase == AttackPhase.ACTIVE && !jaAcertouNestaCarga;
    }
}
