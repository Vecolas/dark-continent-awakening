package com.darkcontinent.nenfoundation.client.vfx.ribbon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Quantos filamentos, de que tamanho, e de quanto em quanto tempo eles trocam.
 *
 * <p>REN E TEN COM MAIS DO MESMO. Mais filamentos, mais longos, trocando mais
 * rapido -- e nao um efeito diferente. O construtor recusa um perfil de Ren que
 * nao seja mais denso que o de Ten em nenhum eixo, porque um Ren mais fraco que
 * Ten passaria despercebido no codigo e seria obvio em jogo.
 *
 * <p><b>OS NUMEROS SAIRAM DAQUI.</b> Este javadoc dizia "saem quando o perfil
 * existir (#98)" -- ele existe, e eles sairam: hoje o bloco {@code filamentos}
 * de {@code assets/nenfoundation/nen_vfx/<modo>.json} e a unica fonte, e este
 * record e a FORMA deles, com as invariantes. Nao ha mais {@code ten()} nem
 * {@code ren()}: uma constante de codigo ao lado do dado seria a mesma divida
 * que o #98 pagou na shell, reaberta uma pasta ao lado.
 *
 * <p>AS INVARIANTES FICARAM AQUI, e nao viajaram para o carregador. Elas sao
 * sobre a FORMA de um perfil de filamento -- teto, largura que nao vira tubo,
 * minimo antes do maximo -- e valem para qualquer origem: JSON do mod, resource
 * pack de terceiro ou o perfil de emergencia.
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

    /**
     * Largura acima da qual o filamento vira tubo de neon.
     *
     * <p>CONSTANTE, E NAO CHAVE DE PERFIL: e um modo de falha da direcao visual,
     * e nao um gosto. A referencia e brilhante, e nao grossa.
     */
    public static final float LARGURA_MAXIMA = 0.05F;

    /**
     * O codec do bloco {@code filamentos}.
     *
     * <p>AS FAIXAS RECUSAM ANTES DE CONSTRUIR, pelo mesmo motivo do perfil de
     * shell: o construtor LANCA, e uma excecao dentro do reload de recursos
     * derruba o carregamento inteiro por causa de um resource pack torto. Com
     * {@code intRange}/{@code floatRange}, o mesmo arquivo vira erro com motivo.
     *
     * <p>A ORDEM DOS COMPRIMENTOS e conferida em {@code validate}, DEPOIS da
     * construcao: e uma regra entre DOIS campos, e so da para confiri-la com os
     * dois na mao.
     */
    public static final Codec<AuraRibbonProfile> CODEC = RecordCodecBuilder
            .<AuraRibbonProfile>create(i -> i.group(
                    Codec.intRange(0, TETO).fieldOf("quantidade")
                            .forGetter(AuraRibbonProfile::quantidade),
                    Codec.floatRange(1.0e-4F, 8.0F).fieldOf("comprimento_min")
                            .forGetter(AuraRibbonProfile::comprimentoMin),
                    Codec.floatRange(1.0e-4F, 8.0F).fieldOf("comprimento_max")
                            .forGetter(AuraRibbonProfile::comprimentoMax),
                    Codec.floatRange(1.0e-4F, LARGURA_MAXIMA).fieldOf("largura")
                            .forGetter(AuraRibbonProfile::largura),
                    Codec.floatRange(1.0e-2F, 30.0F).fieldOf("ciclo_segundos")
                            .forGetter(AuraRibbonProfile::cicloSegundos))
                    .apply(i, AuraRibbonProfile::new))
            .validate(AuraRibbonProfile::ordemDoComprimento);

    private static DataResult<AuraRibbonProfile> ordemDoComprimento(AuraRibbonProfile perfil) {
        if (perfil.comprimentoMin <= perfil.comprimentoMax) {
            return DataResult.success(perfil);
        }
        return DataResult.error(() -> "comprimento_min (" + perfil.comprimentoMin
                + ") e maior que comprimento_max (" + perfil.comprimentoMax + ")");
    }

    public AuraRibbonProfile {
        if (quantidade < 0 || quantidade > TETO) {
            throw new IllegalArgumentException(
                    "quantidade de ribbons fora do teto de " + TETO + ": " + quantidade);
        }
        positivo(comprimentoMin, "comprimentoMin");
        positivo(comprimentoMax, "comprimentoMax");
        positivo(largura, "largura");
        positivo(cicloSegundos, "cicloSegundos");
        // A ORDEM DOS COMPRIMENTOS NAO SE CONFERE AQUI, e isso e correcao de um
        // defeito que o portao novo encontrou antes de ele ir para a `main`.
        //
        // Ela estava neste construtor, e o construtor LANCA. Como o codec
        // constroi para depois validar, um resource pack com o minimo maior que
        // o maximo derrubava o RELOAD DE RECURSOS inteiro -- nao so a aura --
        // em vez de virar um erro com motivo. E a mesma armadilha que
        // `AuraPerfilVisual` documenta na ordem do Fresnel, e a saida e a mesma:
        // regra entre DOIS campos mora em `validate`, depois da construcao.
        //
        // O que fica aqui sao as checagens de UM campo so, que as faixas do
        // codec ja impedem de chegar -- cinto e suspensorio para quem construir
        // o record em Java.
        if (largura > LARGURA_MAXIMA) {
            // A referencia e brilhante, e nao grossa. Acima disto o filamento
            // vira tubo de neon, que e um dos modos de falha que reprovam.
            throw new IllegalArgumentException("largura de ribbon vira tubo neon: " + largura);
        }
    }

    /**
     * O mesmo perfil, sem nenhum filamento.
     *
     * <p>ELE E ESTRUTURA, e nao arte: Zetsu e ausencia total, e a ausencia
     * precisa estar escrita no perfil em vez de depender de uma guarda em quem
     * desenha. Os outros campos sobrevivem porque quantidade zero ja e o
     * suficiente -- e um comprimento inventado aqui quebraria as invariantes.
     */
    public AuraRibbonProfile semFilamentos() {
        return new AuraRibbonProfile(0, this.comprimentoMin, this.comprimentoMax,
                this.largura, this.cicloSegundos);
    }

    /**
     * O perfil a meio caminho entre dois.
     *
     * <p>A QUANTIDADE ARREDONDA, e nao trunca. Com truncamento, o ultimo passo
     * antes de chegar a Ren desenharia dezessete filamentos de dezoito -- e o
     * decimo oitavo apareceria de uma vez no quadro final. E o mesmo estalo que
     * a transicao em fases existe para nao produzir, numa escala menor e por
     * isso ainda mais dificil de atribuir a uma causa.
     *
     * <p>OS COMPRIMENTOS SOBREVIVEM A {@code semFilamentos}, e e por isso que
     * interpolar contra um perfil apagado funciona: a quantidade cai a zero, mas
     * as faixas continuam validas, e o construtor -- que RECUSA comprimento zero
     * -- nao e alcancado com um numero torto no meio do caminho.
     */
    public static AuraRibbonProfile interpolar(AuraRibbonProfile a, AuraRibbonProfile b, float t) {
        if (a == null || b == null) {
            throw new NullPointerException("interpolar exige os dois perfis");
        }
        float u = Math.clamp(t, 0.0F, 1.0F);
        return new AuraRibbonProfile(
                Math.round(a.quantidade + (b.quantidade - a.quantidade) * u),
                ler(a.comprimentoMin, b.comprimentoMin, u),
                ler(a.comprimentoMax, b.comprimentoMax, u),
                ler(a.largura, b.largura, u),
                ler(a.cicloSegundos, b.cicloSegundos, u));
    }

    private static float ler(float a, float b, float t) {
        return a + (b - a) * t;
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
