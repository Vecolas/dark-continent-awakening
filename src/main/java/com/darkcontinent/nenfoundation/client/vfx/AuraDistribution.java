package com.darkcontinent.nenfoundation.client.vfx;

import java.util.EnumMap;
import java.util.Map;

/** Distribuicao client-side; nao substitui a alocacao autoritativa do servidor. */
public record AuraDistribution(float head, float torso, float leftArm, float rightArm,
        float leftLeg, float rightLeg) {
    public AuraDistribution {
        validar(head, "head"); validar(torso, "torso"); validar(leftArm, "leftArm");
        validar(rightArm, "rightArm"); validar(leftLeg, "leftLeg"); validar(rightLeg, "rightLeg");
    }

    public static AuraDistribution uniforme() {
        return new AuraDistribution(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static AuraDistribution zetsu() {
        return new AuraDistribution(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    public float intensidade(AuraBodyRegion regiao) {
        return switch (regiao) {
            case HEAD -> head; case TORSO -> torso; case LEFT_ARM -> leftArm;
            case RIGHT_ARM -> rightArm; case LEFT_LEG -> leftLeg; case RIGHT_LEG -> rightLeg;
        };
    }

    public Map<AuraBodyRegion, Float> comoMapa() {
        EnumMap<AuraBodyRegion, Float> resultado = new EnumMap<>(AuraBodyRegion.class);
        for (AuraBodyRegion regiao : AuraBodyRegion.values()) resultado.put(regiao, intensidade(regiao));
        return Map.copyOf(resultado);
    }

    public AuraDistribution escalada(float fator) {
        validar(fator, "fator");
        return new AuraDistribution(head * fator, torso * fator, leftArm * fator,
                rightArm * fator, leftLeg * fator, rightLeg * fator);
    }

    private static void validar(float valor, String nome) {
        if (!Float.isFinite(valor) || valor < 0.0F) {
            throw new IllegalArgumentException(nome + " deve ser finito e nao negativo");
        }
    }
}
