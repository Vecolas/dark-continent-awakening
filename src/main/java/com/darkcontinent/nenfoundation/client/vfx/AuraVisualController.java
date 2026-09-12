package com.darkcontinent.nenfoundation.client.vfx;

/**
 * Interpolador client-side. Recebe apenas intencao visual ja autorizada pelo
 * servidor; animacao local nao altera aura, output ou estado de Nen.
 */
public final class AuraVisualController {
    private AuraVisualState atual = AuraVisualState.desligado();
    private AuraVisualState alvo = AuraVisualState.desligado();
    private float transicao;

    public AuraVisualState atual() { return atual; }

    public void receber(AuraVisualMode modo, float intensidade) {
        receber(modo, intensidade, AuraDistribution.uniforme());
    }

    public void receber(AuraVisualMode modo, float intensidade, AuraDistribution distribuicao) {
        if (modo == null || distribuicao == null || !Float.isFinite(intensidade)
                || intensidade < 0.0F || intensidade > 1.0F) {
            throw new IllegalArgumentException("comando visual invalido");
        }
        AuraVisualPreset preset = switch (modo) {
            case TEN -> AuraVisualPreset.tenBasic();
            case REN -> AuraVisualPreset.renBasic();
            case ZETSU -> AuraVisualPreset.zetsu();
            case OFF -> AuraVisualPreset.off();
            case CUSTOM -> AuraVisualPreset.tenBasic();
        };
        this.alvo = new AuraVisualState(modo, preset, modo == AuraVisualMode.ZETSU ? 0.0F : intensidade,
                0.0F, modo == AuraVisualMode.ZETSU ? AuraDistribution.zetsu() : distribuicao,
                0xFFFFFFFF, 0xFFFFFFFF);
        this.transicao = 0.0F;
    }

    /** Avanca a animacao sem criar objetos extras por frame alem do snapshot imutavel. */
    public AuraVisualState avancar(float passo) {
        if (!Float.isFinite(passo) || passo < 0.0F) throw new IllegalArgumentException("passo invalido");
        this.transicao = Math.min(1.0F, this.transicao + passo);
        float t = suavizar(this.transicao);
        this.atual = interpolar(this.atual, this.alvo, t);
        return this.atual;
    }

    public void limpar() {
        this.atual = AuraVisualState.desligado();
        this.alvo = this.atual;
        this.transicao = 1.0F;
    }

    private static AuraVisualState interpolar(AuraVisualState a, AuraVisualState b, float t) {
        AuraDistribution d = new AuraDistribution(
                ler(a.distribution().head(), b.distribution().head(), t),
                ler(a.distribution().torso(), b.distribution().torso(), t),
                ler(a.distribution().leftArm(), b.distribution().leftArm(), t),
                ler(a.distribution().rightArm(), b.distribution().rightArm(), t),
                ler(a.distribution().leftLeg(), b.distribution().leftLeg(), t),
                ler(a.distribution().rightLeg(), b.distribution().rightLeg(), t));
        AuraVisualPreset p = t < 1.0F ? a.preset() : b.preset();
        return new AuraVisualState(t < 1.0F ? a.mode() : b.mode(), p,
                ler(a.intensity(), b.intensity(), t), t, d, b.primaryColor(), b.secondaryColor());
    }

    private static float ler(float a, float b, float t) { return a + (b - a) * t; }
    private static float suavizar(float t) { return t * t * (3.0F - 2.0F * t); }
}
