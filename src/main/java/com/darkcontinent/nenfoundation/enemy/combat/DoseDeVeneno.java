package com.darkcontinent.nenfoundation.enemy.combat;

import java.util.Objects;

/**
 * O que uma ferroada manda aplicar no alvo -- e o motivo, quando nao manda nada.
 *
 * <p><b>Ela nao toca em entidade nenhuma.</b> Quem tem o mundo e que aplica o
 * efeito; este record so carrega a conta ja feita. E isso que permite testar a
 * regra de veneno inteira sem servidor, e e o que impede a decisao de acumulo de
 * nascer dentro de uma entidade, onde ela viraria um {@code if} espalhado que o
 * proximo mob venenoso copiaria errado.</p>
 *
 * @param decisao o que aconteceu, incluindo as duas recusas
 * @param duracaoEmTicks duracao TOTAL que o efeito passa a ter, ja somada e
 *        limitada pelo teto -- nao o incremento
 * @param amplificador nivel do efeito vanilla (0 = nivel I), derivado da duracao
 */
public record DoseDeVeneno(DecisaoDeVeneno decisao, int duracaoEmTicks, int amplificador) {

    public DoseDeVeneno {
        Objects.requireNonNull(decisao, "decisao de veneno ausente");
        if (duracaoEmTicks < 0 || amplificador < 0) {
            throw new IllegalArgumentException("dose de veneno negativa: duracao=" + duracaoEmTicks
                    + " amplificador=" + amplificador + ". Duracao negativa vira um efeito que o"
                    + " vanilla remove no mesmo tick, e o ferrao passaria a nao fazer nada sem"
                    + " nenhum erro no log.");
        }
        // Recusa com dose e o defeito silencioso que este record existe para
        // fechar: quem chama olharia so a duracao, aplicaria o efeito, e a recusa
        // viraria uma aplicacao normal com uma mensagem contraditoria ao lado.
        if (decisao == DecisaoDeVeneno.ALVO_IMUNE && (duracaoEmTicks != 0 || amplificador != 0)) {
            throw new IllegalArgumentException("dose de veneno para alvo imune traz duracao "
                    + duracaoEmTicks + " e amplificador " + amplificador + ": quem chama aplicaria"
                    + " o efeito assim mesmo e a recusa viraria uma aplicacao comum");
        }
    }

    /** A recusa de alvo que nao sofre veneno, escrita uma vez so. */
    public static DoseDeVeneno imune() {
        return new DoseDeVeneno(DecisaoDeVeneno.ALVO_IMUNE, 0, 0);
    }

    /** Ha efeito para aplicar? Falso nas recusas que nao mandam aplicar nada. */
    public boolean aplicavel() { return duracaoEmTicks > 0; }
}
