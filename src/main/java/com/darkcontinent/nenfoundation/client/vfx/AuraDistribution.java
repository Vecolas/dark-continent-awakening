package com.darkcontinent.nenfoundation.client.vfx;

import java.util.EnumMap;
import java.util.Map;

/**
 * A distribuicao que o SERVIDOR mandou, pronta para desenhar.
 *
 * <p>ELA E PROJECAO, e nao fonte (ADR-014, item 5). A alocacao autoritativa
 * vive em {@code nen.aura.AlocacaoDeAura} e e derivada das tecnicas ativas; o
 * que chega aqui e o resultado, ja decidido.
 *
 * <p>Os construtores proprios continuam existindo para o VISUAL que nao vem do
 * servidor -- a transicao interpola entre duas distribuicoes, e Zetsu zera
 * tudo. O que nao pode e alguem inventar aqui uma distribuicao de GAMEPLAY: se
 * o numero influencia dano, custo ou alcance, ele nasce no servidor.
 */
public record AuraDistribution(float head, float torso, float leftArm, float rightArm,
        float leftLeg, float rightLeg) {
    public AuraDistribution {
        validar(head, "head"); validar(torso, "torso"); validar(leftArm, "leftArm");
        validar(rightArm, "rightArm"); validar(leftLeg, "leftLeg"); validar(rightLeg, "rightLeg");
    }

    /**
     * Converte a alocacao autoritativa na projecao de desenho.
     *
     * <p>A CONVERSAO MORA AQUI, num lugar so. Espalhada pelos renderers, cada
     * um teria a propria ideia de como mapear regiao para intensidade, e as
     * duas ideias divergiriam sem ninguem notar.
     *
     * <p>AS FRACOES SAO NORMALIZADAS PELO MAIOR, e nao usadas cruas. A alocacao
     * soma 1.0, entao em repouso cada regiao vale 0,167 -- e uma intensidade de
     * 0,167 em tudo desenharia uma aura fraquissima onde deveria estar normal.
     * O que importa para a tela e a PROPORCAO entre regioes, nao o valor
     * absoluto.
     */
    public static AuraDistribution daAlocacao(
            com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura alocacao) {
        if (alocacao == null) {
            return uniforme();
        }
        float maior = alocacao.em(alocacao.maisConcentrada());
        if (!(maior > 0.0F)) {
            return zetsu();
        }
        return new AuraDistribution(
                fracao(alocacao, com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo.CABECA, maior),
                fracao(alocacao, com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo.TRONCO, maior),
                fracao(alocacao, com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo.BRACO_ESQUERDO, maior),
                fracao(alocacao, com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo.BRACO_DIREITO, maior),
                fracao(alocacao, com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo.PERNA_ESQUERDA, maior),
                fracao(alocacao, com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo.PERNA_DIREITA, maior));
    }

    private static float fracao(com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura alocacao,
            com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo regiao, float maior) {
        return alocacao.em(regiao) / maior;
    }

    public static AuraDistribution uniforme() {
        return new AuraDistribution(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static AuraDistribution zetsu() {
        return new AuraDistribution(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    /**
     * A distribuicao com o RIPPLE do impacto somado na regiao atingida (#103).
     *
     * <p>O ripple entra POR AQUI, e nao por um campo novo em
     * {@code AuraVisualState}, e a escolha e o que torna a mudanca pequena: a
     * intensidade por regiao ja atravessa o renderer, o emissor de particulas e
     * o overlay de debug. Um campo novo obrigaria os tres a aprender o conceito
     * de impacto; somado aqui, os tres ja sabem desenhar "esta regiao esta mais
     * acesa agora".
     *
     * <p><b>SOMA SEM TETO, e nao substituicao.</b> Trocar o valor apagaria a
     * distribuicao que o servidor mandou -- alguem em Ko no braco levaria uma
     * pancada e perderia a concentracao na TELA, sem ter perdido nada no jogo.
     *
     * <p><b>O TETO DE 1.0 EXISTIU, E TORNAVA O RIPPLE IMPOSSIVEL.</b> Ten desenha
     * com {@link #uniforme()} -- 1.0 em TODAS as regiao --, entao
     * {@code min(1.0, 1.0 + realce)} devolvia exatamente 1.0 e o efeito nao
     * existia. Nao lancava, os onze testes passavam (eles mediam uma
     * distribuicao zerada), e o relato de jogo foi seco: <i>"nao acende
     * nunca"</i>. O teto era invencao minha: {@code validar} so recusa negativo
     * e nao-finito, e o renderer usa o valor como MULTIPLICADOR de alpha
     * ({@code alphaDoPasse * intensidade(regiao)}) -- acima de 1.0 ele clareia,
     * que e precisamente o que um ripple precisa fazer.
     *
     * <p>O ripple DECAI porque {@link AuraImpactState#progresso()} decai: quem
     * chama passa o impacto ja avancado, e um impacto morto devolve esta mesma
     * distribuicao, sem copia.
     */
    public AuraDistribution comImpacto(AuraImpactState impacto) {
        if (impacto == null || !impacto.ativo()) {
            return this;
        }
        float realce = impacto.strength() * impacto.progresso();
        if (!(realce > 0.0F)) {
            return this;
        }
        return new AuraDistribution(
                somar(head, realce, impacto.region() == AuraBodyRegion.HEAD),
                somar(torso, realce, impacto.region() == AuraBodyRegion.TORSO),
                somar(leftArm, realce, impacto.region() == AuraBodyRegion.LEFT_ARM),
                somar(rightArm, realce, impacto.region() == AuraBodyRegion.RIGHT_ARM),
                somar(leftLeg, realce, impacto.region() == AuraBodyRegion.LEFT_LEG),
                somar(rightLeg, realce, impacto.region() == AuraBodyRegion.RIGHT_LEG));
    }

    private static float somar(float base, float realce, boolean atingida) {
        return atingida ? base + realce : base;
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
