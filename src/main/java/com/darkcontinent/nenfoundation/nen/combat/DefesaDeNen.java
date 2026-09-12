package com.darkcontinent.nenfoundation.nen.combat;

import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;

/**
 * Quanto dano a aura segura.
 *
 * <p>A CONTA INTEIRA MORA AQUI, e e por isso que ela e uma classe e nao tres
 * linhas dentro do handler. O {@code package-info} deste pacote abre com a
 * regra: <i>existe UMA ordem documentada de aplicacao de modificadores, e ela
 * mora num lugar so -- multiplicador aplicado em dois event handlers
 * multiplica duas vezes, e o numero final e plausivel demais para alguem
 * notar</i>.
 *
 * <pre>
 *   dano final = dano x (1 - reducao)
 *   reducao    = protecaoDaTecnica x auraDaFaixa
 * </pre>
 *
 * <p>FUNCAO PURA: sem {@code ServerPlayer}, sem evento, sem registro. A regra
 * que decide quanto dano alguem toma da para provar inteira sem subir o jogo --
 * e ela e a regra com maior chance de deixar o jogo trivial ou injusto.
 *
 * <p>HA UM TETO, e ele nao e opcional. Sem ele, uma combinacao de tecnicas com
 * numeros mal escolhidos chega a reducao de 100% -- imortalidade, que nao da
 * erro e nao aparece em teste nenhum que nao a procure.
 */
public final class DefesaDeNen {

    private DefesaDeNen() {
    }

    /**
     * A fracao de dano que a aura segura, de 0 a {@code teto}.
     *
     * @param protecaoDaTecnica quanto a tecnica ativa protege, de 0 para cima.
     *     Zetsu e zero; sem tecnica tambem e zero
     * @param alocacao          onde a aura esta (ADR-014)
     * @param faixa             onde o golpe acertou
     * @param teto              a reducao maxima permitida, de 0 a 1
     */
    public static float reducao(double protecaoDaTecnica, AlocacaoDeAura alocacao,
            FaixaDoCorpo faixa, double teto) {

        if (!Double.isFinite(protecaoDaTecnica) || protecaoDaTecnica <= 0.0D
                || alocacao == null || faixa == null) {
            return 0.0F;
        }
        double limite = limiteSeguro(teto);
        double bruta = protecaoDaTecnica * faixa.auraDefendendo(alocacao);

        if (!Double.isFinite(bruta) || bruta <= 0.0D) {
            return 0.0F;
        }
        return (float) Math.min(bruta, limite);
    }

    /** O dano que sobra depois da aura. Nunca negativo, nunca maior que o original. */
    public static float danoDepoisDaAura(float dano, float reducao) {
        if (!Float.isFinite(dano)) {
            // NAO E `Math.max(0, dano)`: `Math.max` PROPAGA NaN em vez de
            // segura-lo, e dano NaN atravessaria inteiro. Foi o proprio teste
            // desta classe que pegou isso -- a primeira versao usava o max.
            return 0.0F;
        }
        if (dano <= 0.0F) {
            return Math.max(0.0F, dano);
        }
        if (!Float.isFinite(reducao) || reducao <= 0.0F) {
            return dano;
        }
        float sobra = dano * (1.0F - Math.min(reducao, 1.0F));
        // CURAR NAO E DEFENDER. Uma reducao acima de 1 viraria dano negativo, e
        // dano negativo no Minecraft CURA -- a tecnica de defesa passaria a
        // regenerar quem apanha, e o relato seria "estou ficando mais forte
        // quando me batem".
        return Math.max(0.0F, sobra);
    }

    /**
     * O teto, preso entre 0 e um limite absoluto.
     *
     * <p>NAO EXISTE 100%. Mesmo com a config pedindo, a reducao para antes da
     * imortalidade: quem quiser um jogador imune tem de dizer isso em outro
     * lugar, de proposito, e nao girando um numero de balanceamento ate o fim
     * da barra.
     */
    private static double limiteSeguro(double teto) {
        if (!Double.isFinite(teto) || teto <= 0.0D) {
            return 0.0D;
        }
        return Math.min(teto, TETO_ABSOLUTO);
    }

    /** Nenhuma combinacao de tecnicas passa daqui. */
    public static final double TETO_ABSOLUTO = 0.90D;
}
