package com.darkcontinent.nenfoundation.nen.aura;

/**
 * Reserva de aura autoritativa de uma unica sessao de jogador.
 *
 * <p>As invariantes ficam nesta fronteira para que nenhuma tecnica precise
 * repetir clamp ou validacao: maxima e atual sao sempre finitas, nao negativas
 * e atual nunca passa da maxima. Exaustao e derivada do valor atual, portanto
 * nao existe um segundo booleano capaz de ficar dessincronizado.
 * <p>O OUTPUT SAO TRES GRANDEZAS, e nao uma: o que o jogador SELECIONOU, o
 * MAXIMO que ele pode liberar agora, e o EFETIVO, que e o menor dos dois e e o
 * unico que o jogo consome. Colapsados num numero so, nao ha como representar
 * "ele pediu 100% e so consegue 60%" -- e o sintoma seria a interface mostrando
 * um valor que o servidor nao usa.
 *
 * <p>TERMINOLOGIA: este projeto chama isto de <b>Output</b> (sigla AOP, de
 * Actual Aura Power). <b>"Release" nao e adotado</b> como termo paralelo: duas
 * palavras para a mesma grandeza viram dois campos no primeiro refactor de
 * alguem apressado. Onde o cânone fala em liberar aura, aqui se le Output.
 *
 * <p>PONTO CEGO DECLARADO: <b>nada abaixa o maximo ainda.</b> Ele nasce em 100%
 * e nenhum codigo o reduz -- tecnica, exaustao e progressao do teto sao de
 * marcos futuros, e estao fora de escopo por decisao registrada na issue #70.
 * Ate la o efetivo e sempre igual ao selecionado, e o {@code min} nunca morde
 * em producao. Ele tem teste proprio para nao nascer quebrado.
 */
public final class AuraPool {

    /** Piso absoluto do output: 0%. */
    public static final float OUTPUT_MINIMO_ABSOLUTO = 0.0F;

    /** Teto absoluto do output: 100%. Nenhum maximo pode passar disto. */
    public static final float OUTPUT_MAXIMO_ABSOLUTO = 1.0F;

    /**
     * Quanto um passo do jogador move: 5 pontos percentuais.
     *
     * <p>E LIMITE DE DESENHO, e nao botao de balanceamento: ele define a
     * GRANULARIDADE que a interface oferece, e vinte posicoes numa barra ja e
     * mais do que alguem consegue mirar no meio de uma luta. Um numero de
     * config aqui produziria passos que nao fecham em 100% -- 0.07 leva a
     * 98%, e o jogador nunca alcanca o proprio teto.
     */
    public static final float PASSO_DE_OUTPUT = 0.05F;


    private double maxima;
    private double atual;
    /**
     * O que o JOGADOR escolheu liberar. Nasce em 100%.
     *
     * <p>Ele nao e o valor que o resto do jogo consome: ver
     * {@link #outputEfetivo()}.
     */
    private float outputSelecionado = OUTPUT_MAXIMO_ABSOLUTO;

    /**
     * O teto que o jogador pode liberar AGORA.
     *
     * <p>Nasce em 100%, e <b>nada o abaixa ainda</b>. Ele existe como costura
     * para o dia em que tecnica, exaustao ou progressao limitarem o output --
     * ver o ponto cego declarado no topo da classe.
     */
    private float outputMaximo = OUTPUT_MAXIMO_ABSOLUTO;

    /** Cria uma reserva cheia explicitamente; a sessao normal nasce neutra. */
    public AuraPool(double maxima) {
        validarNumero(maxima, "maxima");
        if (maxima < 0.0D) {
            throw new IllegalArgumentException("maxima nao pode ser negativa");
        }
        this.maxima = maxima;
        this.atual = maxima;
    }

    /** Cria uma reserva neutra, usada antes de a formula definir a maxima. */
    public AuraPool() {
        this(0.0D);
    }

    public double maxima() {
        return this.maxima;
    }

    public double atual() {
        return this.atual;
    }

    /** O que o jogador escolheu. Pode estar acima do maximo permitido agora. */
    public float outputSelecionado() {
        return this.outputSelecionado;
    }

    /** O teto atual. Nada o abaixa ainda; ver o ponto cego no topo da classe. */
    public float outputMaximo() {
        return this.outputMaximo;
    }

    /**
     * O valor que o jogo consome. <b>Derivado, nunca guardado.</b>
     *
     * <p>POR QUE DERIVADO: guardar "o efetivo" num terceiro campo criaria a
     * mesma verdade em duas fontes, e ela divergiria no instante em que o
     * maximo mudasse sem alguem lembrar de recalcular -- sem erro nenhum, com
     * o jogador liberando mais do que pode.
     *
     * <p>E por isso a escolha do jogador NAO e rebaixada quando o maximo cai:
     * se ela fosse, abaixar o teto apagaria a intencao dele, e ao voltar o
     * teto o jogador ficaria preso no valor reduzido sem entender por que.
     */
    public float outputEfetivo() {
        return Math.min(this.outputSelecionado, this.outputMaximo);
    }

    /** Aura zerada: a tecnica nao deve iniciar nem continuar neste estado. */
    public boolean exausto() {
        return this.atual == 0.0D;
    }

    /**
     * Troca a maxima sem criar aura acima dela. O valor atual e reduzido apenas
     * quando necessario; aumentar a maxima nao concede aura automaticamente.
     */
    public void definirMaxima(double maxima) {
        validarNumero(maxima, "maxima");
        if (maxima < 0.0D) {
            throw new IllegalArgumentException("maxima nao pode ser negativa");
        }
        this.maxima = maxima;
        this.atual = Math.min(this.atual, maxima);
    }

    /** Define a aura atual somente dentro dos limites do pool. */
    public void definirAtual(double atual) {
        validarNumero(atual, "atual");
        if (atual < 0.0D || atual > this.maxima) {
            throw new IllegalArgumentException("atual deve estar entre zero e a maxima");
        }
        this.atual = atual;
    }

    /**
     * Ajusta o output limitando entre 0.0 (0%) e 1.0 (100%). Retorna se houve
     * mudanca.
     *
     * <p>POR QUE A VALIDACAO VEM ANTES DO CLAMP, e nao depois: o clamp NAO
     * segura NaN. {@code Math.min(NaN, 1.0F)} devolve NaN, e
     * {@code Math.max(0.0F, NaN)} tambem -- entao um clamp que parece defensivo
     * deixa NaN passar inteiro para o campo.
     *
     * <p>Isso importa porque este valor chega de um payload C2S: o cliente
     * manda a variacao, o servidor soma e ajusta. Um cliente modificado
     * enviando NaN gravava NaN no runtime, e dali ele saia no delta para o HUD.
     * NaN atravessa multiplicacao sem erro -- o sintoma nao e uma excecao, e um
     * numero que some da tela e uma barra que nunca mais se mexe.
     *
     * <p>A recusa e uma EXCECAO, e nao um {@code return false}, pelo mesmo
     * motivo de {@link #gastar}: um valor invalido chegando aqui significa que
     * alguem falhou em validar antes, e engolir isso em silencio esconde o
     * chamador errado. A camada de rede recusa primeiro, com motivo; esta aqui
     * e a rede de baixo.
     */
    public boolean definirOutputSelecionado(float novoPercent) {
        validarNumero(novoPercent, "novoPercent");
        float ajustado = limitarAFaixa(novoPercent);
        if (ajustado != this.outputSelecionado) {
            this.outputSelecionado = ajustado;
            return true;
        }
        return false;
    }

    /**
     * Troca o teto permitido. Devolve se houve mudanca.
     *
     * <p>Ela NAO mexe no selecionado, de proposito: ver {@link #outputEfetivo}.
     */
    public boolean definirOutputMaximo(float novoMaximo) {
        validarNumero(novoMaximo, "novoMaximo");
        float ajustado = limitarAFaixa(novoMaximo);
        if (ajustado != this.outputMaximo) {
            this.outputMaximo = ajustado;
            return true;
        }
        return false;
    }

    /** Um passo para cima. Devolve se houve mudanca. */
    public boolean aumentarOutput() {
        return definirOutputSelecionado(this.outputSelecionado + PASSO_DE_OUTPUT);
    }

    /** Um passo para baixo. Devolve se houve mudanca. */
    public boolean diminuirOutput() {
        return definirOutputSelecionado(this.outputSelecionado - PASSO_DE_OUTPUT);
    }

    /**
     * O clamp da faixa, num lugar so.
     *
     * <p>Ele nao valida numero: quem chama ja validou. Separar os dois e o que
     * impede a armadilha de achar que o clamp protege -- ele nao protege, e
     * {@code Math.min(NaN, 1.0F)} devolve NaN.
     */
    private static float limitarAFaixa(float valor) {
        return Math.max(OUTPUT_MINIMO_ABSOLUTO, Math.min(valor, OUTPUT_MAXIMO_ABSOLUTO));
    }

    /**
     * Debita tudo ou nada. Quantidade zero nao e um gasto e valores invalidos
     * sao recusados para nao transformar NaN em uma aura aparentemente valida.
     */
    public boolean gastar(double quantidade) {
        validarNumero(quantidade, "quantidade");
        if (quantidade <= 0.0D || quantidade > this.atual) {
            return false;
        }
        this.atual -= quantidade;
        return true;
    }

    /** Recupera aura sem ultrapassar a maxima. */
    public double recuperar(double quantidade) {
        validarNumero(quantidade, "quantidade");
        if (quantidade < 0.0D) {
            throw new IllegalArgumentException("quantidade nao pode ser negativa");
        }
        double antes = this.atual;
        // Soma so o espaco disponivel: duas entradas finitas podem somar infinito.
        this.atual += Math.min(this.maxima - this.atual, quantidade);
        return this.atual - antes;
    }

    private static void validarNumero(double valor, String nome) {
        if (!Double.isFinite(valor)) {
            throw new IllegalArgumentException(nome + " deve ser finito");
        }
    }
}
