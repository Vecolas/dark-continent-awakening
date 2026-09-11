package com.darkcontinent.nenfoundation.client.vfx;

/**
 * Funcao barata e deterministica para posicionar filamentos proximos da
 * silhueta. O renderer pode usa-la sem alocar uma nuvem de particulas.
 */
public final class AuraFlowPattern {
    private AuraFlowPattern() {}

    public static AuraFlowSample sample(long entitySeed, int index, float age, float intensity) {
        if (index < 0 || !Float.isFinite(age) || !Float.isFinite(intensity)
                || intensity < 0.0F || intensity > 1.0F) {
            throw new IllegalArgumentException("parametros de fluxo invalidos");
        }
        long bits = entitySeed * 31L + index * 0x9E3779B97F4A7C15L;
        float fase = (float) ((bits ^ (bits >>> 33)) & 0xFFFFL) / 65535.0F;
        float angulo = fase * 6.2831855F + age * (0.035F + intensity * 0.08F);
        float raio = 0.18F + 0.08F * (float) Math.sin(age * 0.11F + fase * 9.0F);
        float altura = 0.15F + ((index % 5) * 0.18F) + age * (0.004F + intensity * 0.006F);
        float amplitude = intensity * (0.55F + 0.45F * (float) Math.sin(age * 0.19F + fase * 5.0F));
        return new AuraFlowSample((float) Math.cos(angulo) * raio, altura,
                (float) Math.sin(angulo) * raio, Math.max(0.0F, amplitude), fase);
    }
}
