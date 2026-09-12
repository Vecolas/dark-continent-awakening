package com.darkcontinent.nenfoundation.client.vfx.ribbon;

/**
 * Quantos filamentos, de que tamanho, e de quanto em quanto tempo eles trocam.
 *
 * <p>REN E TEN COM MAIS DO MESMO. Mais filamentos, mais longos, trocando mais
 * rapido -- e nao um efeito diferente. O construtor recusa um perfil de Ren que
 * nao seja mais denso que o de Ten em nenhum eixo, porque um Ren mais fraco que
 * Ten passaria despercebido no codigo e seria obvio em jogo.
 *
 * <p>NUMEROS DE DIRECAO DE ARTE, nunca medidos. Saem daqui quando o perfil em
 * datapack existir (#98).
 *
 * @param quantidade      quantos filamentos por jogador, no nivel de detalhe cheio
 * @param comprimentoMin  em BLOCOS
 * @param comprimentoMax  em BLOCOS
 * @param largura         em BLOCOS; fino de proposito -- brilhante nao e tubo neon
 * @param cicloSegundos   quanto tempo uma curva dura antes de ser trocada
 */
public record AuraRibbonProfile(int quantidade, float comprimentoMin, float comprimentoMax,
        float largura, float cicloSegundos) {

    /** Teto duro por jogador. Trava de seguranca, e nao botao de ajuste. */
    public static final int TETO = 28;

    public AuraRibbonProfile {
        if (quantidade < 0 || quantidade > TETO) {
            throw new IllegalArgumentException(
                    "quantidade de ribbons fora do teto de " + TETO + ": " + quantidade);
        }
        positivo(comprimentoMin, "comprimentoMin");
        positivo(comprimentoMax, "comprimentoMax");
        positivo(largura, "largura");
        positivo(cicloSegundos, "cicloSegundos");
        if (comprimentoMin > comprimentoMax) {
            throw new IllegalArgumentException("comprimento minimo maior que o maximo");
        }
        if (largura > 0.05F) {
            // A referencia e brilhante, e nao grossa. Acima disto o filamento
            // vira tubo de neon, que e um dos modos de falha que reprovam.
            throw new IllegalArgumentException("largura de ribbon vira tubo neon: " + largura);
        }
    }

    /** Ten: poucos, curtos, trocando devagar. */
    public static AuraRibbonProfile ten() {
        return new AuraRibbonProfile(8, 0.15F, 0.60F, 0.009F, 1.1F);
    }

    /** Ren: a MESMA linguagem, mais densa. */
    public static AuraRibbonProfile ren() {
        return new AuraRibbonProfile(18, 0.40F, 1.60F, 0.014F, 0.55F);
    }

    /** Nenhum filamento. Zetsu, e qualquer estado desligado. */
    public static AuraRibbonProfile zero() {
        return new AuraRibbonProfile(0, 0.15F, 0.60F, 0.009F, 1.1F);
    }

    /** O comprimento do filamento de indice {@code i}, espalhado na faixa. */
    public float comprimentoDe(long semente) {
        float t = ((semente >>> 40) & 0xFFFF) / 65535.0F;
        return this.comprimentoMin + (this.comprimentoMax - this.comprimentoMin) * t;
    }

    private static void positivo(float valor, String nome) {
        if (!Float.isFinite(valor) || valor <= 0.0F) {
            throw new IllegalArgumentException(nome + " deve ser finito e maior que zero: " + valor);
        }
    }
}
