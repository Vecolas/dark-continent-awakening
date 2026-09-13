package com.darkcontinent.nenfoundation.client.vfx;

/**
 * Snapshot visual interpolavel. Nao contem aura autoritativa nem regras de combate.
 *
 * <p><b>ELE NAO CARREGA MAIS NUMERO DE ARTE.</b> Ate o AV3 havia aqui um
 * {@code AuraVisualPreset} com sete campos -- e cinco deles (espessura da
 * shell, intensidade de borda, de fluxo, amplitude e frequencia de pulso) nao
 * tinham UM leitor sequer no repositorio. Eram o erro numero 7 do
 * {@code CLAUDE.md} em forma de record: o numero foi para o dado
 * ({@code assets/nenfoundation/nen_vfx/*.json}) e ficou tambem no codigo, onde
 * quem girasse o botao nao mudaria nada.
 *
 * <p>A DIVISAO QUE FICOU: aqui mora o que muda a cada tick e por jogador --
 * modo, intensidade, progresso da transicao, alocacao por regiao e as duas
 * cores. Os numeros de arte, que sao os mesmos para todos os jogadores no mesmo
 * modo, moram no perfil e se buscam com
 * {@code AuraPerfis.de(estado.mode())}.
 */
public record AuraVisualState(AuraVisualMode mode, float intensity,
        float transitionProgress, AuraDistribution distribution, int primaryColor,
        int secondaryColor) {
    public AuraVisualState {
        if (mode == null || distribution == null) {
            throw new NullPointerException("modo e distribuicao sao obrigatorios");
        }
        validar(intensity, "intensity");
        validar(transitionProgress, "transitionProgress");
    }

    public boolean enabled() { return mode != AuraVisualMode.OFF && intensity > 0.0F; }

    public static AuraVisualState desligado() {
        return new AuraVisualState(AuraVisualMode.OFF, 0.0F, 1.0F,
                AuraDistribution.zetsu(), 0xFFFFFFFF, 0xFFFFFFFF);
    }

    private static void validar(float valor, String nome) {
        if (!Float.isFinite(valor) || valor < 0.0F || valor > 1.0F) {
            throw new IllegalArgumentException(nome + " deve estar entre 0 e 1");
        }
    }
}
