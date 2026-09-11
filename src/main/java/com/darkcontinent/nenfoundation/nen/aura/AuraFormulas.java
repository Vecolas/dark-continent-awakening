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

    public static double regeneracaoPorTick(ParametrosDeAura parametros) {
        return validar(parametros.regeneracaoPorSegundo()) / TICKS_POR_SEGUNDO;
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
