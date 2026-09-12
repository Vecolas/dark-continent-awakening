package com.darkcontinent.nenfoundation.nen.combat;

import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;

/**
 * Quanto a aura soma ao golpe.
 *
 * <p>E O ESPELHO DE {@link DefesaDeNen}, de proposito: a mesma forma, o mesmo
 * teto obrigatorio, a mesma normalizacao. Duas contas parecidas escritas de
 * jeitos diferentes divergem na primeira mudanca, e a divergencia aparece como
 * "o ataque ficou estranho" muito depois de alguem conseguir ligar as duas
 * coisas.
 *
 * <pre>
 *   dano final = dano x (1 + reforco)
 *   reforco    = reforcoDaTecnica x auraDoBraco x normalizacao
 * </pre>
 *
 * <p>O BRACO, E NAO A REGIAO ESCOLHIDA. Quem bate, bate com o braco -- entao e
 * a aura QUE ESTA NO BRACO que entra na conta. Um jogador em Gyo concentrando
 * na cabeca perde golpe, e isso nao e efeito colateral: e a troca que a
 * concentracao sempre prometeu e que ate agora nao cobrava nada.
 *
 * <p>HA UM TETO, e pelo mesmo motivo do outro lado: sem ele, numeros mal
 * escolhidos numa sessao de balanceamento produzem o golpe que mata tudo de
 * uma vez. Isso nao da erro -- da um jogo sem combate, e ninguem liga uma coisa
 * na outra.
 *
 * <p>FUNCAO PURA, sem jogador e sem evento, porque e a regra com maior chance
 * de deixar o combate trivial.
 */
public final class AtaqueDeNen {

    /**
     * O teto que nenhuma config atravessa: o golpe nao passa de quatro vezes.
     *
     * <p>Existe pelo mesmo motivo do teto absoluto da defesa. La o numero
     * proibido e 1.0, que e imortalidade; aqui nao ha um valor magico, e por
     * isso o limite e uma escolha de design escrita -- e nao um numero que
     * alguem gira sem perceber o que esta girando.
     */
    public static final double TETO_ABSOLUTO = 3.0D;

    private AtaqueDeNen() {
    }

    /**
     * A fracao que o golpe ganha, de 0 a {@code teto}.
     *
     * @param reforcoDaTecnica quanto a tecnica ativa reforca, de 0 para cima
     * @param alocacao         onde a aura esta (ADR-014)
     * @param braco            o braco que desfere o golpe
     * @param teto             o reforco maximo permitido
     */
    public static float reforco(double reforcoDaTecnica, AlocacaoDeAura alocacao,
            RegiaoDoCorpo braco, double teto) {

        if (!Double.isFinite(reforcoDaTecnica) || reforcoDaTecnica <= 0.0D
                || alocacao == null || braco == null) {
            return 0.0F;
        }
        double limite = limiteSeguro(teto);
        // NORMALIZACAO, igual a da defesa: a alocacao uniforme tem 1/6 em cada
        // regiao, e sem multiplicar pelo numero de regioes estar em repouso
        // valeria um sexto do reforco escrito na config -- um numero que nao
        // corresponde a nada e que ninguem conseguiria girar com sentido.
        double bruto = reforcoDaTecnica * alocacao.em(braco) * RegiaoDoCorpo.values().length;

        if (!Double.isFinite(bruto) || bruto <= 0.0D) {
            return 0.0F;
        }
        return (float) Math.min(bruto, limite);
    }

    /** O dano depois do reforco. Nunca menor que o original, nunca NaN. */
    public static float danoDepoisDoReforco(float dano, float reforco) {
        // NaN NAO PASSA POR `Math.max`, e este projeto ja caiu nisso uma vez:
        // `Math.max(0, NaN)` devolve NaN, e o dano atravessava inteiro.
        if (!Float.isFinite(dano)) {
            return 0.0F;
        }
        if (!Float.isFinite(reforco) || reforco <= 0.0F) {
            return Math.max(0.0F, dano);
        }
        float somado = dano * (1.0F + Math.min(reforco, (float) TETO_ABSOLUTO));
        if (!Float.isFinite(somado)) {
            return Math.max(0.0F, dano);
        }
        return Math.max(dano, somado);
    }

    private static double limiteSeguro(double teto) {
        if (!Double.isFinite(teto) || teto <= 0.0D) {
            return 0.0D;
        }
        return Math.min(teto, TETO_ABSOLUTO);
    }
}
