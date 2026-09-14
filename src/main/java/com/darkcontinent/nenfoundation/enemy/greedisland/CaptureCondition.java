package com.darkcontinent.nenfoundation.enemy.greedisland;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * O que precisa ser verdade para um alvo virar card.
 *
 * <p><b>Ela e uma tabela de fatos, e nao um predicado de codigo.</b> Escrita
 * como lambda por criatura, a condicao de captura viraria dezesseis regras
 * espalhadas que so a leitura do Java revela -- e a decima seria escrita
 * levemente diferente. Como dado, ela e a mesma pergunta para todos, e o que
 * muda sao os numeros.</p>
 *
 * <p>Os campos se leem juntos: {@code vidaMaximaFracao} de 0 significa "tem de
 * morrer"; 1 significa "nao importa a vida". Exigir vida baixa E nao-letal ao
 * mesmo tempo e o caso interessante -- e o que faz o jogador ter de PARAR de
 * bater na hora certa em vez de matar.</p>
 *
 * @param vidaMaximaFracao fracao da vida maxima abaixo da qual a captura vale
 * @param exigeNaoLetal true quando matar CANCELA a captura
 * @param exigeObservacaoPrevia true quando o bestiario precisa ja conhecer o bicho
 * @param ticksMaximosDeCombate 0 desliga o limite; acima disso a captura expira
 */
public record CaptureCondition(double vidaMaximaFracao, boolean exigeNaoLetal,
        boolean exigeObservacaoPrevia, int ticksMaximosDeCombate) {

    public static final Codec<CaptureCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0.0D, 1.0D).fieldOf("vida_maxima_fracao")
                    .forGetter(CaptureCondition::vidaMaximaFracao),
            Codec.BOOL.fieldOf("exige_nao_letal").forGetter(CaptureCondition::exigeNaoLetal),
            Codec.BOOL.fieldOf("exige_observacao_previa")
                    .forGetter(CaptureCondition::exigeObservacaoPrevia),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("ticks_maximos_de_combate")
                    .forGetter(CaptureCondition::ticksMaximosDeCombate))
            .apply(instance, CaptureCondition::new));

    public CaptureCondition {
        if (!Double.isFinite(vidaMaximaFracao) || vidaMaximaFracao < 0.0D || vidaMaximaFracao > 1.0D) {
            throw new IllegalArgumentException("fracao de vida invalida: " + vidaMaximaFracao);
        }
        if (ticksMaximosDeCombate < 0) throw new IllegalArgumentException("limite de combate negativo");
        if (exigeNaoLetal && vidaMaximaFracao <= 0.0D) {
            throw new IllegalArgumentException("condicao impossivel: exige nao-letal E vida zero."
                    + " Nenhuma captura jamais aconteceria, e isso nao daria erro -- apareceria"
                    + " como um bicho que nunca vira card, e o jogador culparia a propria mira.");
        }
    }

    /** Captura por abate: qualquer morte serve. */
    public static CaptureCondition porAbate() {
        return new CaptureCondition(1.0D, false, false, 0);
    }

    /** Captura por enfraquecimento: um quarto de vida, e NAO pode morrer. */
    public static CaptureCondition porEnfraquecimento() {
        return new CaptureCondition(0.25D, true, false, 0);
    }

    /**
     * Todos os fatos ja medidos pelo servidor -- o record nao consulta nada.
     *
     * @param fracaoDeVida vida atual sobre vida maxima, no momento do desfecho
     * @param morreu se o alvo chegou a morrer
     * @param jaObservado se o bestiario do jogador ja tinha visto a especie
     * @param ticksDeCombate quanto durou o episodio
     */
    public boolean satisfeita(double fracaoDeVida, boolean morreu, boolean jaObservado,
            int ticksDeCombate) {
        if (!Double.isFinite(fracaoDeVida) || fracaoDeVida < 0.0D) {
            throw new IllegalArgumentException("fracao de vida invalida no desfecho");
        }
        if (exigeNaoLetal && morreu) return false;
        if (fracaoDeVida > vidaMaximaFracao) return false;
        if (exigeObservacaoPrevia && !jaObservado) return false;
        return ticksMaximosDeCombate <= 0 || ticksDeCombate <= ticksMaximosDeCombate;
    }
}
