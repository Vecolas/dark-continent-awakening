package com.darkcontinent.nenfoundation.client.vfx.ribbon;

/**
 * A curva de um filamento, em unidade de modelo e no espaco da parte.
 *
 * <p>DETERMINISTICA A PARTIR DE UMA SEMENTE, e nunca sorteada por quadro. Um
 * sorteio a sessenta hertz nao e "organico": e ruido branco, e ruido branco e a
 * assinatura do raio eletrico que a direcao de arte reprova. A mesma semente
 * devolve a mesma curva, entao uma captura pode ser comparada com a seguinte.
 *
 * <p>A SEMENTE E {@code hash(UUID + ancora + indice + ciclo)}. Trocar de ciclo
 * troca a curva; e por isso que o filamento parece renascer sem que nada seja
 * alocado.
 *
 * <p>PURA E SEM MINECRAFT: nao ha nenhum tipo do jogo nesta classe, entao a
 * geometria da para provar sem subir nada.
 */
public final class AuraCurve {

    /** Unidades de modelo por bloco. A curva vive em unidade; quem desenha converte. */
    public static final float UNIDADES_POR_BLOCO = 16.0F;

    private AuraCurve() {
    }

    /**
     * Quantos nos esta curva tem.
     *
     * <p>Entre cinco e nove: menos que cinco nao curva, mais que nove gasta
     * vertice para uma diferenca que ninguem ve a dois blocos.
     */
    public static int nos(float comprimentoEmBlocos) {
        int calculado = 5 + (int) (comprimentoEmBlocos / 0.20F);
        return Math.clamp(calculado, 5, 9);
    }

    /**
     * Escreve os nos da curva em {@code destino}, como triplas (x, y, z).
     *
     * <p>SEM ALOCAR: o vetor vem de fora e e reaproveitado entre quadros. Vinte
     * e oito ribbons vezes nove nos vezes sessenta quadros por segundo e lixo
     * demais para um efeito cosmetico.
     *
     * @param destino      vetor com pelo menos {@code nos * 3} posicoes
     * @param folgaBase    distancia da superficie em que o no zero pousa, em unidades
     * @param comprimento  comprimento do filamento, em BLOCOS
     * @return quantos nos foram escritos
     */
    public static int pontos(float[] destino, AuraAnchor ancora, boolean slim, long semente,
            float folgaBase, float comprimento) {
        int n = nos(comprimento);
        float comprimentoEmUnidades = comprimento * UNIDADES_POR_BLOCO;

        // OS SORTEIOS SAIEM NUMA ORDEM FIXA, e essa ordem e contrato: trocar
        // duas linhas daqui muda TODAS as curvas do jogo de uma vez.
        long estado = semente;
        float giro = 1.4F + proximo(estado = mistura(estado)) * 3.6F;
        float sinal = proximo(estado = mistura(estado)) < 0.5F ? -1.0F : 1.0F;
        float jitterPsi = (proximo(estado = mistura(estado)) - 0.5F) * 0.9F;
        float expansao = 0.4F + proximo(estado = mistura(estado)) * 1.2F;
        float ondulacao = 0.3F + proximo(estado = mistura(estado)) * 1.1F;
        float ondas = 1.0F + (float) ((int) (proximo(estado = mistura(estado)) * 3.0F));
        float fase = proximo(estado = mistura(estado)) * 6.2831855F;
        float jitterY = (proximo(estado = mistura(estado)) - 0.5F) * 1.6F;

        float psi0 = ancora.psiInicial();
        float cx = ancora.centroX(slim);
        float rx = ancora.raioX(slim);
        float rz = ancora.raioZ();
        float y0 = ancora.alturaBase() + jitterY;

        for (int i = 0; i < n; i++) {
            float s = i / (float) (n - 1);
            int base = i * 3;

            if (ancora.familia() == AuraAnchor.Familia.AXIAL) {
                // Sobe reto, vagando de leve. `deriva` reaproveita `expansao`:
                // um sorteio a menos, e o papel e o mesmo -- quanto ele se abre.
                float deriva = expansao;
                destino[base] = cx + deriva * sin(ondas * 3.1415927F * s + fase) * s;
                // SUBTRAI porque +Y aponta para baixo.
                destino[base + 1] = y0 - (folgaBase + comprimentoEmUnidades * pow115(s));
                destino[base + 2] = deriva * cos(ondas * 3.1415927F * s + fase + 1.9F) * s;
                continue;
            }

            // `s^1.3` no giro: a volta ACELERA. Giro linear desenha helice de
            // passo constante, que o olho le como mola de caderno.
            float psi = psi0 + jitterPsi + sinal * giro * pow13(s);
            // `* s` na ondulacao zera o desvio em s = 0: o no zero pousa
            // exatamente no ponto de superficie. E o criterio "os filamentos
            // nascem na superficie, e nao no ar" virando aritmetica.
            float e = folgaBase + expansao * s + ondulacao * sin(ondas * 3.1415927F * s + fase) * s;

            destino[base] = cx + (rx + e) * cos(psi);
            destino[base + 1] = y0 - comprimentoEmUnidades * pow115(s);
            destino[base + 2] = (rz + e) * sin(psi);
        }
        return n;
    }

    /**
     * Quantos nos uma COLUNA tem.
     *
     * <p>MAIS QUE UMA RIBBON DE CORPO, e por uma razao de forma: a coluna sobe
     * ate 2,5 blocos, e nove nos nesse comprimento dao segmentos de quase um
     * terco de bloco -- longos o bastante para a curva virar uma sequencia de
     * retas visiveis. Doze e o teto, e ele bate com o buffer de
     * {@link AuraRibbonBatch}.
     */
    public static int nosDeColuna(float alturaEmBlocos) {
        int calculado = 6 + (int) (alturaEmBlocos / 0.28F);
        return Math.clamp(calculado, 6, 12);
    }

    /**
     * Escreve os nos de uma COLUNA VERTICAL de Ren.
     *
     * <p>A DERIVA VERTICAL E DOMINANTE, e essa e a diferenca inteira para uma
     * ribbon de corpo. A ribbon ENROLA em volta de um eixo; a coluna SOBE, e so
     * vagueia de leve. Ondas horizontais fortes fazem o olho ler "vento" em vez
     * de "aura", e um zigue-zague faz ele ler "raio" -- e Nen generico nao e
     * eletricidade. Por isso aqui nao ha giro em volta do eixo e a frequencia
     * lateral fica entre uma e duas ondas no comprimento inteiro.
     *
     * <p>ELA NASCE NO ESPACO DA PARTE, como a ribbon, e e por isso que acompanha
     * o membro ao correr, pular, atacar e agachar. Uma coluna desenhada em
     * espaco de mundo ficaria para tras -- e "coluna flutuando solta, sem origem
     * visivel no corpo" e um dos criterios que reprovam.
     *
     * @param destino   vetor com pelo menos {@code nos * 3} posicoes
     * @param altura    altura da coluna, em BLOCOS
     * @return quantos nos foram escritos
     */
    public static int pontosDeColuna(float[] destino, AuraAnchor ancora, boolean slim,
            long semente, float folgaBase, float altura) {
        int n = nosDeColuna(altura);
        float alturaEmUnidades = altura * UNIDADES_POR_BLOCO;

        // A MESMA DISCIPLINA DA RIBBON: sorteios numa ordem fixa, que e
        // contrato. Trocar duas linhas daqui muda TODAS as colunas de uma vez.
        long estado = semente;
        float amplitude = 0.8F + proximo(estado = mistura(estado)) * 1.6F;
        float ondas = 1.0F + proximo(estado = mistura(estado)) * 1.0F;
        float fase = proximo(estado = mistura(estado)) * 6.2831855F;
        float inclinacao = (proximo(estado = mistura(estado)) - 0.5F) * 1.4F;
        float jitterPsi = (proximo(estado = mistura(estado)) - 0.5F) * 0.8F;

        float psi = ancora.psiInicial() + jitterPsi;
        float cx = ancora.centroX(slim) + (ancora.raioX(slim) + folgaBase) * (float) Math.cos(psi);
        float cz = (ancora.raioZ() + folgaBase) * (float) Math.sin(psi);
        float y0 = ancora.alturaBase();

        for (int i = 0; i < n; i++) {
            float s = i / (float) (n - 1);
            int base = i * 3;
            // `s^1.4` abre a deriva devagar perto do corpo: a coluna sai colada
            // e so se solta la em cima. Deriva linear desenha um cone, e cone e
            // a leitura de jato, nao de corrente.
            float abertura = amplitude * (float) Math.pow((double) s, 1.4D);
            destino[base] = cx + abertura * sin(ondas * 3.1415927F * s + fase)
                    + inclinacao * s;
            // SUBTRAI porque +Y aponta para baixo nesta pilha.
            destino[base + 1] = y0 - alturaEmUnidades * s;
            destino[base + 2] = cz + abertura * cos(ondas * 3.1415927F * s + fase + 2.4F);
        }
        return n;
    }

    /**
     * A folga com que o no zero nasce, derivada da espessura da BORDA da shell.
     *
     * <p>DERIVADA, E NAO CONSTANTE. Copiar um {@code 1.0} fixo aqui e o erro
     * numero 7 do {@code CLAUDE.md} numa variante nova: no dia em que alguem
     * engordar a shell de Ren no perfil, as ribbons passariam a nascer DENTRO
     * dela e sumiriam, sem erro nenhum.
     */
    public static float folgaBase(float espessuraDaBordaEmBlocos) {
        return espessuraDaBordaEmBlocos * UNIDADES_POR_BLOCO + 0.15F;
    }

    /** splitmix64: barato, determinista e bem espalhado. */
    static long mistura(long estado) {
        long z = estado + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** A semente de uma ribbon. Trocar o ciclo troca a curva. */
    public static long semente(long uuidMenosSignificativo, AuraAnchor ancora, int indice,
            int ciclo) {
        long s = uuidMenosSignificativo;
        s = mistura(s ^ (ancora.ordinal() * 0x2545F4914F6CDD1DL));
        s = mistura(s ^ (indice * 0x9E3779B97F4A7C15L));
        return mistura(s ^ (ciclo * 0xD1B54A32D192ED03L));
    }

    private static float proximo(long estado) {
        return (estado >>> 40) / (float) (1 << 24);
    }

    /**
     * {@code s^1.3}, escrito como {@code s * s^0.3}.
     *
     * <p>A primeira versao disto era {@code s * s * sqrt(s) * sqrt(sqrt(s))},
     * que vale {@code s^1.75} -- uma tentativa de trocar {@code Math.pow} por
     * raizes que errou o expoente. O erro nao lancaria nada: as ribbons so
     * enrolariam rapido demais perto da ponta. Micro-otimizacao que muda o
     * numero nao e otimizacao.
     */
    private static float pow13(float s) {
        return s * (float) Math.pow((double) s, 0.3D);
    }

    /** {@code s^1.15}: sobe devagar perto do corpo e acelera na ponta. */
    private static float pow115(float s) {
        return s * (float) Math.pow((double) s, 0.15D);
    }

    private static float sin(float a) {
        return (float) Math.sin((double) a);
    }

    private static float cos(float a) {
        return (float) Math.cos((double) a);
    }
}
