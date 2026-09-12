package com.darkcontinent.nenfoundation.enemy.chimera;

import java.util.Set;

/** Genoma curado: traits variam, mas a morfologia continua animavel. */
public record ChimeraGenome(String primarySpecies, String secondarySpecies,
        ChimeraMorphology morphology, Set<ChimeraTrait> traits, float humanInfluence,
        float intelligence, float individuality, float nenPotential) {
    public ChimeraGenome {
        if (primarySpecies == null || primarySpecies.isBlank() || secondarySpecies == null
                || secondarySpecies.isBlank() || morphology == null || traits == null
                || !faixa(humanInfluence) || !faixa(intelligence) || !faixa(individuality)
                || !faixa(nenPotential)) throw new IllegalArgumentException("genoma quimera invalido");
        traits = Set.copyOf(traits);
    }

    private static boolean faixa(float value) { return Float.isFinite(value) && value >= 0 && value <= 1; }
}
