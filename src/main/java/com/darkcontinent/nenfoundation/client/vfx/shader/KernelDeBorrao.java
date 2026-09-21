package com.darkcontinent.nenfoundation.client.vfx.shader;

/**
 * Os pesos do desfoque, calculados na CPU e passados prontos.
 *
 * <p><b>UMA VEZ POR MUDANCA DE RAIO, e nao por pixel.</b> Recalcular uma
 * gaussiana dentro do fragmento e gastar ALU para reproduzir oito numeros que
 * nao mudam -- multiplicado pela quantidade de pixels da tela, vezes dois
 * passes, vezes sessenta quadros por segundo. O desfoque ja e o segundo item
 * mais caro da ordem de custos do AV8; nao precisa de ajuda.
 *
 * <p>OITO PESOS, E NAO UM ARRAY. O {@code Uniform} do Minecraft para em quatro
 * componentes, entao eles viajam como dois {@code vec4}. O tap zero e o centro;
 * os sete restantes sao simetricos e o shader soma dos dois lados -- quinze
 * amostras no total.
 *
 * <p><b>OS PESOS SOMAM 1.</b> Sem a normalizacao, mudar o raio mudaria tambem o
 * BRILHO -- e o sintoma seria "o halo fica mais forte quando eu aumento o raio",
 * que e verdade e nao deveria ser: raio e tamanho, forca e forca. As duas coisas
 * tem botoes diferentes de proposito.
 *
 * <p>PURA E SEM MINECRAFT: da para provar sem subir o jogo, e e a unica parte do
 * passe de brilho que tem aritmetica de verdade.
 */
public final class KernelDeBorrao {

    /** Quantos pesos o shader consome: o centro mais sete laterais. */
    public static final int TAPS = 8;

    /**
     * Abaixo deste raio, o desfoque e apenas o centro.
     *
     * <p>Um raio de meio texel nao tem o que espalhar, e uma gaussiana tao
     * estreita produz pesos laterais na casa de 1e-8 -- quinze amostras de
     * textura para somar zero.
     */
    private static final float RAIO_MINIMO = 0.35F;

    private KernelDeBorrao() {
    }

    /**
     * Os oito pesos para um raio, em TEXELS do alvo.
     *
     * <p>O raio e tratado como o desvio-padrao vezes dois, que e a convencao
     * usual: com {@code sigma = raio / 2}, o tap no proprio raio ja vale cerca
     * de 14% do centro, e alem dele a contribuicao e visualmente nula.
     */
    public static float[] pesos(float raioEmTexels) {
        float[] pesos = new float[TAPS];
        if (!Float.isFinite(raioEmTexels) || raioEmTexels < RAIO_MINIMO) {
            // SEM RAIO, O CENTRO LEVA TUDO. Devolver zeros deixaria a imagem
            // PRETA em vez de apenas nao borrada -- e "a aura sumiu" e um relato
            // muito pior que "a aura nao borrou".
            pesos[0] = 1.0F;
            return pesos;
        }
        float sigma = raioEmTexels * 0.5F;
        float doisSigmaQuadrado = 2.0F * sigma * sigma;

        float total = 0.0F;
        for (int i = 0; i < TAPS; i++) {
            // ALEM DO RAIO, PESO ZERO -- e nao um valor pequeno. Um tap com peso
            // 1e-6 ainda custa duas amostras de textura por pixel, e o shader
            // pula o tap justamente quando o peso e zero.
            if (i > Math.ceil(raioEmTexels)) {
                pesos[i] = 0.0F;
                continue;
            }
            pesos[i] = (float) Math.exp(-(i * i) / doisSigmaQuadrado);
            // O CENTRO CONTA UMA VEZ; OS LATERAIS, DUAS. O shader soma o tap i
            // de cada lado, entao a normalizacao precisa contar os dois -- senao
            // a soma passa de 1 e o halo clareia sozinho conforme o raio cresce.
            total += i == 0 ? pesos[i] : pesos[i] * 2.0F;
        }
        if (total <= 0.0F) {
            pesos[0] = 1.0F;
            return pesos;
        }
        for (int i = 0; i < TAPS; i++) {
            pesos[i] /= total;
        }
        return pesos;
    }

    /**
     * Quanto os pesos somam, contando os laterais duas vezes.
     *
     * <p>Existe para o teste: e a propriedade que mantem raio e forca
     * independentes um do outro.
     */
    public static float soma(float[] pesos) {
        float total = pesos[0];
        for (int i = 1; i < pesos.length; i++) {
            total += pesos[i] * 2.0F;
        }
        return total;
    }
}
