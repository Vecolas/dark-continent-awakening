package com.darkcontinent.nenfoundation.nen.aura;

import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import java.util.Objects;

/**
 * Formulas de aura que ligam configuracao recarregavel ao pool de um jogador.
 *
 * <p>A maxima so existe para quem despertou: conceder reserva a um perfil
 * neutro faria o primeiro valor util do sistema nascer antes do despertar. O
 * potencial persistente e um acrescimo; a base continua sendo um botao de
 * ajuste fora do codigo. A regeneracao e exposta por tick para que o consumidor
 * nao congele um valor lido durante o login.
 */
public final class AuraFormulas {

    private static final double TICKS_POR_SEGUNDO = 20.0D;

    private AuraFormulas() {
    }

    public static double maxima(PersistentNenData perfil) {
        Objects.requireNonNull(perfil, "perfil");
        if (!perfil.awakened()) {
            return 0.0D;
        }
        validarPotencial(perfil.auraPotential());
        return maxima(perfil, NenConfig.auraMaximaBase());
    }

    /** Variante pura para testes e simuladores que fornecem a configuracao. */
    public static double maxima(PersistentNenData perfil, double maximaBase) {
        Objects.requireNonNull(perfil, "perfil");
        validarBase(maximaBase);
        if (!perfil.awakened()) {
            return 0.0D;
        }
        validarPotencial(perfil.auraPotential());
        return maximaBase + perfil.auraPotential();
    }

    /** Valor lido na hora para cada tick, sem copia congelada no runtime. */
    public static double regeneracaoPorTick() {
        return regeneracaoPorTick(NenConfig.auraRegeneracaoPorSegundo());
    }

    /** Variante pura para testes e simuladores que fornecem a configuracao. */
    public static double regeneracaoPorTick(double porSegundo) {
        validarBase(porSegundo);
        return porSegundo / TICKS_POR_SEGUNDO;
    }

    private static void validarPotencial(double potencial) {
        if (!Double.isFinite(potencial) || potencial < 0.0D) {
            throw new IllegalArgumentException("auraPotential deve ser finito e nao negativo");
        }
    }

    private static void validarBase(double valor) {
        if (!Double.isFinite(valor) || valor < 0.0D) {
            throw new IllegalArgumentException("valor de aura deve ser finito e nao negativo");
        }
    }
}
