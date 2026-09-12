package com.darkcontinent.nenfoundation.client.vfx;

/**
 * Interpolador client-side. Recebe apenas intencao visual ja autorizada pelo
 * servidor; animacao local nao altera aura, output ou estado de Nen.
 */
public final class AuraVisualController {
    private AuraVisualState atual = AuraVisualState.desligado();

    /**
     * De onde a transicao atual PARTIU, congelado.
     *
     * <p>ELE EXISTE POR CAUSA DE UM DEFEITO REAL. Antes, {@code avancar}
     * interpolava de {@code atual} para {@code alvo} e reescrevia {@code atual}
     * -- ou seja, cada passo saia de um ponto que o passo anterior ja tinha
     * movido. O resultado nao era a curva suave: era um arrasto lento seguido
     * de um ESTALO no ultimo tick, quando {@code t} chega a 1 e cobre de uma vez
     * tudo o que faltava.
     *
     * <p>Os testes nao pegavam porque so usavam passos de 0.5 e 1.0, onde a
     * diferenca nao aparece. Com vinte passos de 0.05 -- que e o caso real de
     * uma transicao de um segundo -- ela aparece inteira.
     */
    private AuraVisualState origem = AuraVisualState.desligado();
    private AuraVisualState alvo = AuraVisualState.desligado();
    private float transicao;

    public AuraVisualState atual() { return atual; }

    public void receber(AuraVisualMode modo, float intensidade) {
        receber(modo, intensidade, AuraDistribution.uniforme());
    }

    public void receber(AuraVisualMode modo, float intensidade, AuraDistribution distribuicao) {
        receber(modo, intensidade, distribuicao, 0xFFFFFFFF, 0xFFFFFFFF);
    }

    /**
     * Idem, com a cor vinda de fora.
     *
     * <p>A COR NAO MORA AQUI, e nao deve morar. Ela e a mesma que identifica a
     * tecnica no HUD, e ter duas fontes para "de que cor e Ren" garante que um
     * dia as duas discordem -- o erro numero 7 da lista do CLAUDE.md. Quem
     * chama sabe qual tecnica esta ligada e passa a cor dela.
     *
     * <p>Antes desta sobrecarga o controlador gravava {@code 0xFFFFFFFF} fixo e
     * {@link AuraVisualProfile} carregava campos de cor que ninguem lia.
     */
    public void receber(AuraVisualMode modo, float intensidade, AuraDistribution distribuicao,
            int corPrimaria, int corSecundaria) {
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
                corPrimaria, corSecundaria);
        this.origem = this.atual;
        this.transicao = 0.0F;
    }

    /** Avanca a animacao sem criar objetos extras por frame alem do snapshot imutavel. */
    public AuraVisualState avancar(float passo) {
        if (!Float.isFinite(passo) || passo < 0.0F) throw new IllegalArgumentException("passo invalido");
        this.transicao = Math.min(1.0F, this.transicao + passo);
        float t = suavizar(this.transicao);
        // DE `origem` PARA `alvo`, e nunca de `atual`: interpolar a partir do
        // valor ja movido transforma a curva num arrasto com estalo no fim.
        this.atual = interpolar(this.origem, this.alvo, t);
        return this.atual;
    }

    public void limpar() {
        this.atual = AuraVisualState.desligado();
        this.alvo = this.atual;
        this.origem = this.atual;
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
