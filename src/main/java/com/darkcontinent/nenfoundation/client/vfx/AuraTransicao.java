package com.darkcontinent.nenfoundation.client.vfx;

/**
 * Quanto tempo cada troca de estado leva, e com que curva.
 *
 * <p>CADA TROCA TEM O PROPRIO TEMPO, e essa e a razao desta classe existir.
 * Antes havia um numero so -- {@code vfx.ticksDeTransicao} -- valendo para
 * tudo, e com ele ligar Ten e explodir em Ren levavam exatamente o mesmo tempo.
 * O resultado nao e um bug: e uma aura em que nenhuma troca tem PESO.
 *
 * <p>AS DURACOES SAIRAM DA DIRECAO DE ARTE, e estao em
 * {@code docs/vfx/perfis-visuais.md}: ligar em ~350 ms, subir para Ren em
 * ~900 ms, voltar em ~700 ms, suprimir entre 200 e 500 ms. Vinte ticks e um
 * segundo.
 *
 * <p>SUPRIMIR E A MAIS RAPIDA, de proposito. Zetsu e uma decisao de sumir, e uma
 * supressao lenta e uma supressao que nao salva ninguem.
 *
 * <p>SUBIR PARA REN E A MAIS LENTA. A direcao de arte pede que a BORDA
 * ultrapasse o alvo antes de assentar -- o detalhe que faz Ren parecer
 * LIBERACAO em vez de interpolacao --, mas essa ultrapassagem pertence ao
 * material da borda, e nao ao progresso da transicao. Ver o javadoc de
 * {@link #ELEVAR}.
 */
public enum AuraTransicao {

    /** Do nada para Ten. Sem explosao: a pelicula so aparece. */
    LIGAR(AuraVisualMode.OFF, AuraVisualMode.TEN, 7, Curva.SUAVE, AuraTransicao::ligar),

    /**
     * Ten para Ren. A mais lenta da tabela.
     *
     * <p>ELA NAO USA {@code OVERSHOOT}, e a razao e um defeito que este PR
     * cometeu e desfez. A direcao de arte pede que a BORDA ultrapasse o alvo em
     * 10-15% antes de assentar -- e eu apliquei a ultrapassagem a interpolacao
     * INTEIRA. O efeito colateral: a intensidade geral batia no maximo na
     * METADE da transicao, porque a curva passa de 1 ali. Nada lancava; Ren so
     * chegava cedo demais.
     *
     * <p>A ultrapassagem pertence ao MATERIAL da borda, e nao ao progresso da
     * transicao. <b>ESSE LUGAR EXISTE A PARTIR DO AV4:</b> e a janela
     * {@code BORDA} de {@link #fases()}, que sobe a 1,12 e volta a 1,00 sem
     * tocar no progresso geral. {@code SAIDA_SUAVE} continua regendo a
     * intensidade, que e uma subida honesta.
     */
    ELEVAR(AuraVisualMode.TEN, AuraVisualMode.REN, 18, Curva.SAIDA_SUAVE,
            AuraTransicao::elevar),

    /** Ren para Ten. Mais suave que a subida: a pressao cai antes do resto. */
    BAIXAR(AuraVisualMode.REN, AuraVisualMode.TEN, 14, Curva.SAIDA_SUAVE, AuraTransicao::baixar),

    /**
     * Qualquer coisa para Zetsu. A mais rapida que existe, e continua sendo.
     *
     * <p>SEIS TICKS -- 300 ms --, dentro da faixa de 200 a 500 da direcao de
     * arte. As tres fases de {@link #fechar} sao escritas em FRACAO da duracao,
     * e nao nos milissegundos absolutos do documento: aqueles descrevem a forma
     * num total de 400 ms, e copia-los para ca criaria a segunda fonte da
     * duracao -- com a ultima janela terminando depois do fim da propria
     * transicao. As tres fases continuam sendo tres.
     *
     * <p>ELA PRECISA CONTINUAR SENDO A MAIS RAPIDA DA TABELA, e ha teste
     * fixando isso. Nao e detalhe de tuning: Zetsu e uma decisao de sumir, e uma
     * supressao mais lenta que o gesto de LIGAR Ten e uma supressao que nao
     * salva ninguem.
     */
    SUPRIMIR(null, AuraVisualMode.ZETSU, 6, Curva.ENTRADA_RAPIDA, AuraTransicao::fechar),

    /**
     * Qualquer coisa para nada.
     *
     * <p>A MESMA LINHA DO TEMPO DE {@link #SUPRIMIR}, e de proposito: o gesto e
     * o mesmo -- sumir --, e duas linhas do tempo para o mesmo gesto divergiriam
     * no primeiro ajuste. O que separa os dois e o DESTINO, e nao o desenho.
     */
    DESLIGAR(null, AuraVisualMode.OFF, 6, Curva.SAIDA_SUAVE, AuraTransicao::fechar),

    /**
     * O que sobrar.
     *
     * <p>EXISTE PARA QUE UMA TECNICA NOVA NAO PRECISE MEXER AQUI para funcionar.
     * Ela ganha uma troca razoavel de graca, e so entra na tabela quando alguem
     * decidir que ela merece tempo proprio.
     */
    PADRAO(null, null, 10, Curva.SUAVE, AuraTransicao::neutra);

    /** Milissegundos por tick. Vinte ticks e um segundo. */
    public static final float MS_POR_TICK = 50.0F;

    /**
     * O quanto a shell contrai antes de crescer, ao subir para Ren.
     *
     * <p>QUATRO E MEIO POR CENTO -- o meio da faixa de 3 a 5% da direcao de
     * arte. E a inspiracao antes do golpe, e e o detalhe que faz a subida ser
     * lida como LIBERACAO. Sem ela a transicao e uma rampa, e uma rampa e lida
     * como interpolacao.
     */
    private static final float CONTRACAO = 0.955F;

    /**
     * O pico da ultrapassagem da borda.
     *
     * <p>DOZE POR CENTO, no meio da faixa de 10 a 15%. E um NUMERO, e nao o que
     * uma formula de easing produzir -- e por isso o teste consegue fixa-lo sem
     * reimplementar a formula.
     */
    private static final float ULTRAPASSAGEM = 1.12F;

    private final AuraVisualMode origem;
    private final AuraVisualMode destino;
    private final int ticks;
    private final Curva curva;
    private final AuraTransitionProfile fases;

    AuraTransicao(AuraVisualMode origem, AuraVisualMode destino, int ticks, Curva curva,
            java.util.function.Function<AuraTransitionProfile.Construtor,
                    AuraTransitionProfile> linhaDoTempo) {
        this.origem = origem;
        this.destino = destino;
        this.ticks = ticks;
        this.curva = curva;
        // A DURACAO VEM DOS TICKS, e nao de um segundo numero na linha do tempo.
        // Duas fontes para "quanto dura a subida para Ren" divergiriam no
        // primeiro ajuste, e o sintoma seria uma pressao de chao que entra
        // depois de a transicao ter acabado -- sem erro nenhum.
        this.fases = linhaDoTempo.apply(AuraTransitionProfile.de(origem, destino,
                ticks * MS_POR_TICK));
    }

    /**
     * A linha do tempo desta troca, componente por componente.
     *
     * <p>ELA E O AV4 INTEIRO. Ate aqui a transicao movia a intensidade como um
     * bloco so; com ela, a shell contrai antes de crescer, a borda estoura por
     * um instante, as colunas entram depois das ribbons e a pressao de chao
     * entra por ultimo.
     */
    public AuraTransitionProfile fases() {
        return this.fases;
    }

    /** Os pesos de cada componente no progresso pedido, de 0 a 1. */
    public AuraTransitionSample amostrar(float progresso) {
        return this.fases.amostrar(progresso);
    }

    private static AuraTransitionProfile ligar(AuraTransitionProfile.Construtor c) {
        var curva = Curva.SUAVE;
        return c
                // A PELICULA APARECE PRIMEIRO, e os filamentos depois. Os dois
                // ao mesmo tempo produzem um "poof" -- que e a leitura de
                // invocacao, e nao de tecnica sustentada.
                .fase(AuraTransitionProfile.Componente.SHELL,
                        new AuraTransitionProfile.Janela(0.0F, 150.0F, 0.0F, 1.0F, curva))
                .fase(AuraTransitionProfile.Componente.BORDA,
                        new AuraTransitionProfile.Janela(0.0F, 150.0F, 0.0F, 1.0F, curva))
                .fase(AuraTransitionProfile.Componente.FILAMENTOS,
                        new AuraTransitionProfile.Janela(150.0F, 350.0F, 0.0F, 1.0F, curva))
                .constante(AuraTransitionProfile.Componente.COLUNAS, 0.0F)
                .constante(AuraTransitionProfile.Componente.PRESSAO, 0.0F)
                .constante(AuraTransitionProfile.Componente.FLASH, 0.0F)
                .montar();
    }

    private static AuraTransitionProfile elevar(AuraTransitionProfile.Construtor c) {
        return c
                // 0-100 CONTRAI; 100-500 volta e passa. A contracao e o unico
                // trecho da aura inteira em que um peso fica ABAIXO de 1.
                .fase(AuraTransitionProfile.Componente.SHELL,
                        new AuraTransitionProfile.Janela(0.0F, 100.0F, 1.0F, CONTRACAO,
                                Curva.SAIDA_SUAVE),
                        new AuraTransitionProfile.Janela(100.0F, 500.0F, CONTRACAO, 1.0F,
                                Curva.SUAVE))
                // 220-500 cresce ALEM do alvo; 500-700 assenta.
                .fase(AuraTransitionProfile.Componente.BORDA,
                        new AuraTransitionProfile.Janela(220.0F, 500.0F, 1.0F, ULTRAPASSAGEM,
                                Curva.SAIDA_SUAVE),
                        new AuraTransitionProfile.Janela(500.0F, 700.0F, ULTRAPASSAGEM, 1.0F,
                                Curva.SUAVE))
                // OS FILAMENTOS JA ESTAO EM CENA. O que muda neles -- contagem,
                // comprimento, ciclo -- vem do PERFIL interpolado, e nao daqui:
                // um peso que tambem mexesse na contagem seria a segunda fonte
                // da mesma verdade.
                .constante(AuraTransitionProfile.Componente.FILAMENTOS, 1.0F)
                .fase(AuraTransitionProfile.Componente.COLUNAS,
                        new AuraTransitionProfile.Janela(350.0F, 700.0F, 0.0F, 1.0F, Curva.SUAVE))
                .fase(AuraTransitionProfile.Componente.PRESSAO,
                        new AuraTransitionProfile.Janela(500.0F, 900.0F, 0.0F, 1.0F, Curva.SUAVE))
                // O FLASH E CURTO DE PROPOSITO: 120 ms no total. Mais que isso
                // deixa de ser um estouro e vira um segundo estado.
                .fase(AuraTransitionProfile.Componente.FLASH,
                        new AuraTransitionProfile.Janela(100.0F, 160.0F, 0.0F, 1.0F,
                                Curva.ENTRADA_RAPIDA),
                        new AuraTransitionProfile.Janela(160.0F, 220.0F, 1.0F, 0.0F,
                                Curva.SAIDA_SUAVE))
                .montar();
    }

    private static AuraTransitionProfile baixar(AuraTransitionProfile.Construtor c) {
        return c
                .constante(AuraTransitionProfile.Componente.SHELL, 1.0F)
                .constante(AuraTransitionProfile.Componente.BORDA, 1.0F)
                .constante(AuraTransitionProfile.Componente.FILAMENTOS, 1.0F)
                // A PRESSAO CAI PRIMEIRO, e as colunas depois. Na ordem
                // inversa, o chao continuaria tremendo sob um jogador que ja
                // voltou a Ten -- e o olho le isso como efeito preso.
                .fase(AuraTransitionProfile.Componente.COLUNAS,
                        new AuraTransitionProfile.Janela(150.0F, 500.0F, 1.0F, 0.0F,
                                Curva.SAIDA_SUAVE))
                .fase(AuraTransitionProfile.Componente.PRESSAO,
                        new AuraTransitionProfile.Janela(0.0F, 300.0F, 1.0F, 0.0F,
                                Curva.SAIDA_SUAVE))
                .constante(AuraTransitionProfile.Componente.FLASH, 0.0F)
                .montar();
    }

    /**
     * As quatro fases da referencia D: filamentos, shell, borda, ZERO.
     *
     * <p>AS FRACOES SAO AS DO DOCUMENTO, normalizadas pelo total de 400 ms que
     * ele usa: 0 a 0,25 -- os filamentos retraem; 0,25 a 0,625 -- a shell perde
     * alpha; 0,625 a 1 -- a borda fecha no corpo. Depois disso, ZERO.
     *
     * <p>A ULTIMA FASE E A QUE IMPORTA, e ela e o AV6 inteiro: depois do fim nao
     * sobra CONTORNO, e nao sobra em valor nenhum -- nem 0,01. Num servidor com
     * dois clientes, brilho residual entrega justamente quem esta se escondendo.
     *
     * <p>A BORDA SAI POR ULTIMO aqui pelo mesmo motivo que ela e a ultima a sair
     * no LOD: e ela que carrega a leitura. Some primeiro o que e detalhe.
     */
    private static AuraTransitionProfile fechar(AuraTransitionProfile.Construtor c) {
        var rapida = Curva.SAIDA_SUAVE;
        return c
                .fase(AuraTransitionProfile.Componente.FILAMENTOS,
                        c.fracao(0.000F, 0.250F, 1.0F, 0.0F, rapida))
                .fase(AuraTransitionProfile.Componente.COLUNAS,
                        c.fracao(0.000F, 0.250F, 1.0F, 0.0F, rapida))
                .fase(AuraTransitionProfile.Componente.PRESSAO,
                        c.fracao(0.000F, 0.250F, 1.0F, 0.0F, rapida))
                .fase(AuraTransitionProfile.Componente.SHELL,
                        c.fracao(0.250F, 0.625F, 1.0F, 0.0F, Curva.SUAVE))
                .fase(AuraTransitionProfile.Componente.BORDA,
                        c.fracao(0.625F, 1.000F, 1.0F, 0.0F, Curva.SUAVE))
                .constante(AuraTransitionProfile.Componente.FLASH, 0.0F)
                .montar();
    }

    /**
     * A linha do tempo do curinga.
     *
     * <p>TUDO EM CENA O TEMPO TODO, e a mudanca inteira fica por conta da
     * INTENSIDADE e do perfil interpolado. E a escolha conservadora: uma tecnica
     * nova que ainda nao tem linha do tempo propria aparece inteira, que e
     * visivel e portanto corrigivel. O contrario -- rampas inventadas para um
     * destino desconhecido -- apareceria como "a tecnica nova pisca", e ninguem
     * liga um pisco a uma tabela de fases.
     */
    private static AuraTransitionProfile neutra(AuraTransitionProfile.Construtor c) {
        return c
                .constante(AuraTransitionProfile.Componente.SHELL, 1.0F)
                .constante(AuraTransitionProfile.Componente.BORDA, 1.0F)
                .constante(AuraTransitionProfile.Componente.FILAMENTOS, 1.0F)
                .constante(AuraTransitionProfile.Componente.COLUNAS, 1.0F)
                .constante(AuraTransitionProfile.Componente.PRESSAO, 1.0F)
                .constante(AuraTransitionProfile.Componente.FLASH, 0.0F)
                .montar();
    }

    /**
     * A transicao de um par.
     *
     * <p>A ORDEM DA TABELA E A PRECEDENCIA: o primeiro casamento vence. Por isso
     * as entradas com origem definida vem ANTES das curingas -- senao
     * {@code TEN -> ZETSU} cairia em {@code SUPRIMIR} ou em {@code ELEVAR}
     * conforme a ordem de declaracao, e a diferenca so apareceria em jogo.
     */
    public static AuraTransicao de(AuraVisualMode origem, AuraVisualMode destino) {
        if (destino == null) {
            return PADRAO;
        }
        for (AuraTransicao t : values()) {
            if (t == PADRAO) {
                continue;
            }
            boolean origemCasa = t.origem == null || t.origem == origem;
            boolean destinoCasa = t.destino == destino;
            if (origemCasa && destinoCasa) {
                return t;
            }
        }
        return PADRAO;
    }

    /** Quanto a transicao avanca por tick, de 0 a 1, com a escala do jogador. */
    public float passoPorTick(float escala) {
        if (!Float.isFinite(escala) || escala <= 0.0F) {
            // Escala invalida NAO pode congelar a aura no primeiro quadro da
            // animacao. Cai para a escala neutra.
            escala = 1.0F;
        }
        return 1.0F / Math.max(1.0F, this.ticks * escala);
    }

    public int ticks() {
        return this.ticks;
    }

    public Curva curva() {
        return this.curva;
    }

    /** Como o tempo vira progresso. */
    public enum Curva {

        /** Progresso cru. Util para teste, raramente para arte. */
        LINEAR,

        /** Comeca e termina devagar. O padrao. */
        SUAVE,

        /** Sai rapido e assenta. Bom para o que precisa SUMIR. */
        SAIDA_SUAVE,

        /** Entra rapido. Bom para o que precisa APARECER e ja estar la. */
        ENTRADA_RAPIDA,

        /**
         * Ultrapassa o alvo e volta.
         *
         * <p>E o que faz Ren parecer liberacao. O valor PASSA de 1 no meio do
         * caminho -- de proposito --, e quem consome precisa aguentar isso: a
         * borda fica 10-15% mais forte antes de assentar.
         */
        OVERSHOOT;

        /** O progresso, dado o tempo normalizado. */
        public float aplicar(float t) {
            float x = Math.clamp(t, 0.0F, 1.0F);
            return switch (this) {
                case LINEAR -> x;
                case SUAVE -> x * x * (3.0F - 2.0F * x);
                case SAIDA_SUAVE -> 1.0F - (1.0F - x) * (1.0F - x);
                case ENTRADA_RAPIDA -> (float) Math.sqrt(x);
                case OVERSHOOT -> ultrapassar(x);
            };
        }

        /**
         * Ultrapassagem "back-out", com tensao calibrada em ~12%.
         *
         * <p>A CONTA TERMINA EXATAMENTE EM 1 quando {@code x == 1}, e ha teste
         * fixando isso. Uma curva de ultrapassagem que nao fecha deixa a aura
         * um pouco acima do alvo PARA SEMPRE -- e ninguem liga um brilho
         * levemente alto a uma formula de easing meses depois.
         */
        private static float ultrapassar(float x) {
            final float tensao = 1.70158F * 0.62F;
            float u = x - 1.0F;
            return 1.0F + u * u * ((tensao + 1.0F) * u + tensao);
        }
    }
}
