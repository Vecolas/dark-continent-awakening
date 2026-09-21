package com.darkcontinent.nenfoundation.client.vfx;

import java.util.EnumMap;
import java.util.Map;

/**
 * A linha do tempo de uma troca de estado, componente por componente.
 *
 * <p><b>POR QUE ELA EXISTE.</b> Ate o AV3 a transicao era um progresso unico de
 * 0 a 1 movendo a intensidade inteira. Isso produz uma RAMPA, e uma rampa e lida
 * como interpolacao -- nunca como liberacao. A direcao de arte
 * ({@code docs/vfx/perfis-visuais.md} secao 6) pede outra coisa: ao subir para
 * Ren a shell CONTRAI antes de crescer, a borda estoura por um instante, as
 * colunas entram depois das ribbons e a pressao de chao entra por ultimo. Cada
 * componente tem a propria janela, e e isso que esta classe carrega.
 *
 * <p><b>A DURACAO NAO MORA AQUI.</b> Ela mora em {@link AuraTransicao#ticks()},
 * onde ja morava, e chega como argumento. Duas fontes para "quanto dura a subida
 * para Ren" divergiriam no primeiro ajuste -- e a divergencia apareceria como
 * uma pressao de chao que entra depois de a transicao ter acabado, sem erro
 * nenhum. As janelas abaixo estao em MILISSEGUNDOS DE PROJETO; com a escala do
 * jogador tudo estica junto, porque quem amostra passa o progresso relativo.
 *
 * <p><b>A ULTRAPASSAGEM DA BORDA E DUAS JANELAS MONOTONAS, e nao a curva
 * {@code OVERSHOOT}.</b> A diferenca nao e estetica. Com a curva, o pico fica
 * onde a formula de easing o colocar, e "entre 10 e 15%" vira uma propriedade
 * que ninguem consegue fixar em teste sem reimplementar a formula. Com duas
 * janelas -- sobe ate 1,12 e volta a 1,00 --, o pico e um NUMERO, o teste o le
 * direto, e a monotonicidade por fase e verdadeira por construcao. A curva
 * {@code OVERSHOOT} continua disponivel para uma janela que a queira.
 *
 * <p>SEM TIPO DE MINECRAFT: a tabela inteira se prova sem subir o jogo.
 */
public final class AuraTransitionProfile {

    /** O que cada janela move. */
    public enum Componente {
        /** O filme interno. Abaixo de 1, a aura esta contraindo. */
        SHELL,
        /** O contorno. E o unico que pode passar de 1. */
        BORDA,
        /** Os filamentos de corpo. */
        FILAMENTOS,
        /** As correntes verticais de Ren. */
        COLUNAS,
        /** O anel de chao e os detritos, que nascem dentro dele. */
        PRESSAO,
        /** O estouro curto de borda, so no instante da liberacao. */
        FLASH
    }

    /**
     * Um trecho da linha do tempo de um componente.
     *
     * <p>FORA DA JANELA ELA NAO INVENTA NADA: antes do inicio devolve
     * {@code de}, depois do fim devolve {@code para}. E o que permite escrever
     * uma linha do tempo com BURACOS -- a borda nao faz nada entre 0 e 220 ms --
     * sem que o buraco vire um salto para zero.
     *
     * @param inicioMs quando este trecho comeca, em ms de projeto
     * @param fimMs    quando termina
     * @param de       o valor no inicio
     * @param para     o valor no fim
     * @param curva    como o tempo vira progresso dentro do trecho
     */
    public record Janela(float inicioMs, float fimMs, float de, float para,
            AuraTransicao.Curva curva) {

        public Janela {
            if (!Float.isFinite(inicioMs) || !Float.isFinite(fimMs) || fimMs <= inicioMs) {
                throw new IllegalArgumentException(
                        "janela invalida: " + inicioMs + " -> " + fimMs);
            }
            if (!Float.isFinite(de) || !Float.isFinite(para) || de < 0.0F || para < 0.0F
                    || de > AuraTransitionSample.TETO_DE_ULTRAPASSAGEM
                    || para > AuraTransitionSample.TETO_DE_ULTRAPASSAGEM) {
                throw new IllegalArgumentException(
                        "valores de janela fora de faixa: " + de + " -> " + para);
            }
            if (curva == null) {
                throw new NullPointerException("curva obrigatoria");
            }
        }

        /** O valor deste trecho no instante pedido. */
        public float em(float ms) {
            if (ms <= this.inicioMs) {
                return this.de;
            }
            if (ms >= this.fimMs) {
                return this.para;
            }
            float t = (ms - this.inicioMs) / (this.fimMs - this.inicioMs);
            return this.de + (this.para - this.de) * this.curva.aplicar(t);
        }
    }

    private final AuraVisualMode origem;
    private final AuraVisualMode destino;
    private final float duracaoMs;
    private final Map<Componente, Janela[]> fases;

    private AuraTransitionProfile(AuraVisualMode origem, AuraVisualMode destino, float duracaoMs,
            Map<Componente, Janela[]> fases) {
        this.origem = origem;
        this.destino = destino;
        this.duracaoMs = duracaoMs;
        this.fases = fases;
    }

    /** De onde esta troca parte. {@code null} e curinga: vale de qualquer estado. */
    public AuraVisualMode origem() {
        return this.origem;
    }

    /** Para onde esta troca vai. */
    public AuraVisualMode destino() {
        return this.destino;
    }

    /** Quanto a troca dura no tempo de projeto, em milissegundos. */
    public float duracaoMs() {
        return this.duracaoMs;
    }

    /**
     * Os pesos no instante {@code progresso} da transicao.
     *
     * @param progresso de 0 a 1; e o progresso CRU, e nao o curvado
     */
    public AuraTransitionSample amostrar(float progresso) {
        float t = Math.clamp(progresso, 0.0F, 1.0F);
        float ms = t * this.duracaoMs;
        return new AuraTransitionSample(
                valorDe(Componente.SHELL, ms), valorDe(Componente.BORDA, ms),
                valorDe(Componente.FILAMENTOS, ms), valorDe(Componente.COLUNAS, ms),
                valorDe(Componente.PRESSAO, ms), valorDe(Componente.FLASH, ms));
    }

    /**
     * O peso de um componente num instante, em ms de projeto.
     *
     * <p>NO BURACO ENTRE DUAS JANELAS, O VALOR E O FIM DA ANTERIOR. Sem esta
     * regra, a shell voltaria a zero entre a contracao e o crescimento -- um
     * pisco de um quadro que ninguem consegue atribuir a uma causa.
     */
    public float valorDe(Componente componente, float ms) {
        Janela[] janelas = this.fases.get(componente);
        if (janelas == null || janelas.length == 0) {
            return 0.0F;
        }
        if (ms <= janelas[0].inicioMs()) {
            return janelas[0].de();
        }
        for (Janela janela : janelas) {
            if (ms < janela.fimMs()) {
                return janela.em(ms);
            }
        }
        return janelas[janelas.length - 1].para();
    }

    /** Comeca a montar uma linha do tempo. */
    public static Construtor de(AuraVisualMode origem, AuraVisualMode destino, float duracaoMs) {
        return new Construtor(origem, destino, duracaoMs);
    }

    /** Montagem, para que a tabela de {@link AuraTransicao} fique legivel. */
    public static final class Construtor {

        private final AuraVisualMode origem;
        private final AuraVisualMode destino;
        private final float duracaoMs;
        private final EnumMap<Componente, Janela[]> fases = new EnumMap<>(Componente.class);

        private Construtor(AuraVisualMode origem, AuraVisualMode destino, float duracaoMs) {
            if (!Float.isFinite(duracaoMs) || duracaoMs <= 0.0F) {
                throw new IllegalArgumentException("duracao invalida: " + duracaoMs);
            }
            this.origem = origem;
            this.destino = destino;
            this.duracaoMs = duracaoMs;
        }

        /** Quanto esta troca dura, em ms de projeto. */
        public float duracaoMs() {
            return this.duracaoMs;
        }

        /**
         * Uma janela dada em FRACAO da duracao, e nao em milissegundos.
         *
         * <p>ELA EXISTE PARA AS LINHAS DO TEMPO QUE PRECISAM ESCALAR. As fases
         * de supressao da direcao de arte estao escritas para 400 ms -- 0 a 100,
         * 100 a 250, 250 a 400 --, mas a duracao real e a de
         * {@link AuraTransicao#ticks()}, e ela e mais curta de proposito: Zetsu
         * lento nao salva ninguem. Escrever os milissegundos absolutos ali
         * criaria a segunda fonte da duracao, e a ultima janela terminaria
         * DEPOIS do fim da transicao -- que o construtor recusa, e com razao.
         *
         * <p>Em fracao, as tres fases continuam sendo as mesmas TRES FASES,
         * qualquer que seja a duracao.
         */
        public Janela fracao(float inicio, float fim, float de, float para,
                AuraTransicao.Curva curva) {
            return new Janela(inicio * this.duracaoMs, fim * this.duracaoMs, de, para, curva);
        }

        /** Um componente que nao muda: fica parado no valor dado o tempo todo. */
        public Construtor constante(Componente componente, float valor) {
            this.fases.put(componente, new Janela[] {
                    new Janela(0.0F, this.duracaoMs, valor, valor, AuraTransicao.Curva.LINEAR)});
            return this;
        }

        /**
         * As janelas de um componente, em ordem.
         *
         * <p>A ORDEM E CONFERIDA: janelas fora de ordem ou sobrepostas fazem
         * {@link #valorDe} devolver a primeira que casar, e o resultado seria uma
         * linha do tempo que nao e a escrita. O defeito nao lanca -- so desenha
         * errado.
         */
        public Construtor fase(Componente componente, Janela... janelas) {
            if (janelas == null || janelas.length == 0) {
                throw new IllegalArgumentException("componente " + componente + " sem janela");
            }
            for (int i = 1; i < janelas.length; i++) {
                if (janelas[i].inicioMs() < janelas[i - 1].fimMs()) {
                    throw new IllegalArgumentException("janelas de " + componente
                            + " fora de ordem ou sobrepostas: " + janelas[i - 1].fimMs()
                            + " e " + janelas[i].inicioMs());
                }
            }
            if (janelas[janelas.length - 1].fimMs() > this.duracaoMs) {
                throw new IllegalArgumentException("a ultima janela de " + componente
                        + " termina em " + janelas[janelas.length - 1].fimMs()
                        + " ms, depois do fim da transicao (" + this.duracaoMs + " ms)");
            }
            this.fases.put(componente, janelas.clone());
            return this;
        }

        /**
         * Fecha a linha do tempo.
         *
         * <p>EXIGE OS SEIS COMPONENTES. Um componente esquecido devolveria zero
         * em {@link #valorDe}, e o sintoma seria uma parte da aura que
         * simplesmente nao aparece naquela transicao -- visivel em jogo, invisivel
         * em revisao de codigo.
         */
        public AuraTransitionProfile montar() {
            for (Componente componente : Componente.values()) {
                if (!this.fases.containsKey(componente)) {
                    throw new IllegalStateException("linha do tempo sem o componente "
                            + componente + "; um componente omitido desenha ZERO, e zero"
                            + " por esquecimento e indistinguivel de zero de proposito");
                }
            }
            return new AuraTransitionProfile(this.origem, this.destino, this.duracaoMs,
                    new EnumMap<>(this.fases));
        }
    }
}
