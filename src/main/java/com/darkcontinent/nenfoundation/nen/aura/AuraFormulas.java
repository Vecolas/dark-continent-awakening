package com.darkcontinent.nenfoundation.nen.aura;

import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;

/**
 * Formulas minimas da M2. Reserva e output sao independentes; controle e
 * afinidade ganham consumidores nas tecnicas. Nenhum resultado fica congelado.
 */
public final class AuraFormulas {
    /** Unidade de simulacao vanilla, nao compensacao de lag de parede. */
    private static final double TICKS_POR_SEGUNDO = 20.0D;

    private AuraFormulas() { }

    public static double maxima(PersistentNenData perfil, ParametrosDeAura parametros) {
        if (!perfil.awakened()) return 0.0D;
        return somar(parametros.maximaBase(), perfil.auraPotential());
    }

    public static double output(PersistentNenData perfil, ParametrosDeAura parametros) {
        if (!perfil.awakened()) return 0.0D;
        return somar(parametros.outputBase(), perfil.output());
    }

    /** A regeneracao base, sem nenhuma tecnica ativa. */
    public static double regeneracaoPorTick(ParametrosDeAura parametros) {
        return validar(parametros.regeneracaoPorSegundo()) / TICKS_POR_SEGUNDO;
    }

    /**
     * A regeneracao com o estado de Nen ativo levado em conta (ADR-010).
     *
     * <p>O multiplicador e LIMITADO pelo teto da config antes de multiplicar, e
     * nao depois: limitar depois esconderia que o produto estourou, porque o
     * resultado final pareceria plausivel. Limitando antes, o teto e uma
     * propriedade do multiplicador, e da para perguntar qual foi usado.
     *
     * <p>O piso e zero. Multiplicador negativo faria a "regeneracao" DRENAR
     * aura por um caminho que ninguem chamaria de dreno -- e o sintoma seria
     * aura sumindo sem nenhum gasto registrado.
     */
    public static double regeneracaoPorTick(ParametrosDeAura parametros, double multiplicador) {
        return regeneracaoPorTick(parametros)
                * limitarMultiplicador(parametros, multiplicador);
    }

    /**
     * O multiplicador que o motor de fato usa: finito, nao negativo e sob o teto.
     *
     * <p>Publico porque diagnostico e teste precisam perguntar QUAL foi
     * aplicado. Sem isso, "a regeneracao esta estranha" nao tem como ser
     * respondido sem recalcular a mao.
     */
    public static double limitarMultiplicador(ParametrosDeAura parametros, double multiplicador) {
        double teto = validar(parametros.multiplicadorMaximoDeRegeneracao());
        if (!Double.isFinite(multiplicador)) {
            throw new IllegalArgumentException(
                    "multiplicador de regeneracao precisa ser finito, e veio " + multiplicador);
        }
        return Math.max(0.0D, Math.min(multiplicador, teto));
    }

    private static double somar(double base, double progresso) {
        return validar(validar(base) + validar(progresso));
    }

    private static double validar(double valor) {
        // O delta M1 usa float. Double finito que vira Infinity na rede e invalido.
        if (!Double.isFinite(valor) || valor < 0.0D || valor > Float.MAX_VALUE) {
            throw new IllegalArgumentException("grandeza de aura fora do intervalo finito da rede");
        }
        return valor;
    }
}
