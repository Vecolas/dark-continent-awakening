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
    /**
     * Ganho do ripple sobre a intensidade da regiao.
     *
     * <p>LIMITE DE DESENHO. Sem ele, um soco de mao vazia somava 0,45 a uma
     * regiao que ja valia 1,0 -- e como o alpha da borda do Ten e 0,20, isso
     * movia a tela de 0,200 para 0,290. Quarenta e cinco por cento parece
     * muito escrito assim; na tela sao nove centesimos de alpha num invólucro
     * translucido, decaindo em 0,6 s. O relato foi exato: <i>"o F6 mostra o
     * ripple subindo, so nao da para ver no personagem"</i>.
     */
    private static final float GANHO_DO_RIPPLE = 2.5F;

    /**
     * A distribuicao com o RIPPLE do impacto (#103).
     *
     * <p>O ripple entra POR AQUI, e nao por um campo novo em
     * {@code AuraVisualState}: a intensidade por regiao ja atravessa o renderer,
     * o emissor de particulas e o overlay. Um campo novo obrigaria os tres a
     * aprender o conceito de impacto.
     *
     * <p><b>ELE MULTIPLICA, e nao soma -- e essa foi a terceira tentativa.</b>
     * A segunda somava um realce ao corpo inteiro, e um teste que ja existia
     * reprovou dizendo por que: <i>"quem estivesse em Ko perderia a concentracao
     * NA TELA sem ter perdido nada no jogo"</i>. Somar o MESMO valor a um braco
     * em 1,0 e a um tronco em 0,2 achata a razao entre eles de 5:1 para 1,9:1 --
     * a concentracao some durante o flash. Multiplicar acende tudo e preserva a
     * razao exatamente.
     *
     * <p><b>E ELE ACENDE O CORPO INTEIRO, por igual.</b> A primeira versao
     * acendia so o TRONCO, porque o cliente nao sabe onde o golpe acertou e
     * TRONCO era o palpite menos errado. Era inventar localizacao -- e ilegivel,
     * porque o tronco e a regiao mais larga e menos definida da silhueta.
     * {@link AuraImpactState#region()} continua existindo para o dia em que o
     * servidor souber dizer onde bateu (Gyo, Ko); ate la, ninguem finge saber.
     *
     * <p><b>SEM TETO.</b> Ten desenha com {@link #uniforme()} -- 1.0 em todas as
     * regioes --, e um teto de 1.0 tornava o ripple um no-op. O renderer usa o
     * valor como MULTIPLICADOR de alpha: acima de 1.0 ele clareia, que e o que
     * um impacto precisa fazer.
     *
     * <p>O ripple DECAI porque {@link AuraImpactState#progresso()} decai -- com
     * um PLATO no pico, para o flash durar mais que um quadro.
     */
    public AuraDistribution comImpacto(AuraImpactState impacto) {
        if (impacto == null || !impacto.ativo()) {
            return this;
        }
        float fator = 1.0F + impacto.strength() * impacto.progresso() * GANHO_DO_RIPPLE;
        if (!(fator > 1.0F)) {
            return this;
        }
        return new AuraDistribution(head * fator, torso * fator, leftArm * fator,
                rightArm * fator, leftLeg * fator, rightLeg * fator);
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
