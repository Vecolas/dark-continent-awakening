package com.darkcontinent.nenfoundation.client.vfx;

/**
 * A regua do que a aura desenhou no ultimo quadro.
 *
 * <p>A REGUA NASCE JUNTO DO SISTEMA QUE ELA MEDE. Sem ela, "quantas chamadas de
 * desenho a aura custa" e uma opiniao -- e o AV8 vai pedir um numero, nao uma
 * opiniao. Medir cedo tambem evita o caminho contrario, que e otimizar o que
 * nunca foi o gargalo.
 *
 * <p>DOIS CONTADORES POR NUMERO, e e por isso que ele funciona. O quadro em
 * andamento escreve em {@code emCurso}; quem le pega {@code fechado}, que e o
 * quadro anterior inteiro. Ler o contador em andamento daria um numero
 * diferente conforme o instante do quadro em que o overlay desenhasse -- e o
 * valor pularia sem nada estar errado.
 *
 * <p>ELE CONTA SEMPRE, e nao so com o overlay ligado. Duas somas de inteiro por
 * jogador com aura sao mais baratas que o desvio que perguntaria se vale a pena
 * contar; e um contador que so existe em modo de depuracao mede um jogo que
 * ninguem joga.
 *
 * <p>O QUE ELE NAO MEDE esta declarado: o tempo de quadro e a memoria dos alvos
 * de render nao passam por aqui. Isso e {@code spark}, no AV8, e confundir as
 * duas coisas produziria a pior versao de um numero -- o que parece medicao e
 * nao e.
 *
 * <p>CLIENT-ONLY, e sem sincronizacao: tudo isto acontece na thread de render.
 */
public final class MedidorDeVfx {

    private static int chamadasEmCurso;
    private static int filamentosEmCurso;
    private static int colunasEmCurso;
    private static int particulasEmCurso;
    private static int detritosEmCurso;
    private static int jogadoresEmCurso;
    private static int aneisEmCurso;

    private static int chamadasFechadas;
    private static int filamentosFechados;
    private static int colunasFechadas;
    private static int particulasFechadas;
    private static int detritosFechados;
    private static int jogadoresFechados;
    private static int aneisFechados;

    private MedidorDeVfx() {
    }

    /** Uma chamada de desenho da shell. Chamado por quem descarrega o lote. */
    public static void chamadaDeDesenho() {
        chamadasEmCurso++;
    }

    /** Um filamento de CORPO montado neste quadro. */
    public static void filamento() {
        filamentosEmCurso++;
    }

    /**
     * Uma COLUNA de Ren montada neste quadro.
     *
     * <p>CONTADOR SEPARADO, e nao somado ao de filamentos. A issue #189 pede
     * explicitamente as duas contagens lado a lado, e a razao e pratica: coluna
     * e ribbon de corpo custam vertices diferentes -- doze nos contra nove -- e
     * saem por LOD em momentos diferentes. Um numero so esconderia qual dos dois
     * cresceu.
     */
    public static void coluna() {
        colunasEmCurso++;
    }

    /** Um anel de pressao desenhado neste quadro. */
    public static void anelDePressao() {
        aneisEmCurso++;
    }

    /** Detritos vivos neste tick. */
    public static void detritos(int quantos) {
        if (quantos > 0) {
            detritosEmCurso += quantos;
        }
    }

    /** Particulas emitidas neste tick. */
    public static void particulas(int quantas) {
        if (quantas > 0) {
            particulasEmCurso += quantas;
        }
    }

    /** Um jogador com aura visivel neste quadro. */
    public static void jogadorComAura() {
        jogadoresEmCurso++;
    }

    /**
     * Fecha o QUADRO: desenho vira o numero que se le.
     *
     * <p>Chamado uma vez por quadro renderizado. Se ninguem chamar, os numeros
     * congelam -- e congelado e melhor que somando para sempre, que e o que
     * aconteceria com um contador sem zeragem.
     */
    public static void fecharQuadro() {
        chamadasFechadas = chamadasEmCurso;
        filamentosFechados = filamentosEmCurso;
        colunasFechadas = colunasEmCurso;
        aneisFechados = aneisEmCurso;
        jogadoresFechados = jogadoresEmCurso;
        chamadasEmCurso = 0;
        filamentosEmCurso = 0;
        colunasEmCurso = 0;
        aneisEmCurso = 0;
        jogadoresEmCurso = 0;
    }

    /**
     * Fecha o TICK: particula vira o numero que se le.
     *
     * <p><b>DOIS FECHAMENTOS, E NAO UM.</b> Desenho acontece por quadro (60 por
     * segundo, tipicamente) e emissao de particula acontece por tick (20 por
     * segundo). Zerar as particulas junto do quadro faria dois de cada tres
     * quadros lerem <b>zero particulas</b> -- e o numero piscaria entre o valor
     * certo e zero, sem nada estar errado. Quem lesse concluiria que o emissor
     * falha de forma intermitente.
     */
    public static void fecharTick() {
        particulasFechadas = particulasEmCurso;
        detritosFechados = detritosEmCurso;
        particulasEmCurso = 0;
        detritosEmCurso = 0;
    }

    /** Quem liga, desliga: no logout, os numeros do mundo anterior somem. */
    public static void limpar() {
        chamadasEmCurso = 0;
        filamentosEmCurso = 0;
        colunasEmCurso = 0;
        particulasEmCurso = 0;
        detritosEmCurso = 0;
        jogadoresEmCurso = 0;
        aneisEmCurso = 0;
        chamadasFechadas = 0;
        filamentosFechados = 0;
        colunasFechadas = 0;
        particulasFechadas = 0;
        detritosFechados = 0;
        jogadoresFechados = 0;
        aneisFechados = 0;
    }

    /** Colunas de Ren desenhadas no ultimo quadro fechado. */
    public static int colunas() {
        return colunasFechadas;
    }

    /** Aneis de pressao desenhados no ultimo quadro fechado. */
    public static int aneisDePressao() {
        return aneisFechados;
    }

    /** Detritos vivos no ultimo tick fechado. */
    public static int detritos() {
        return detritosFechados;
    }

    /** Chamadas de desenho de aura no ultimo quadro fechado. */
    public static int chamadasDeDesenho() {
        return chamadasFechadas;
    }

    /** Filamentos desenhados no ultimo quadro fechado. */
    public static int filamentos() {
        return filamentosFechados;
    }

    /** Particulas emitidas no ultimo quadro fechado. */
    public static int particulas() {
        return particulasFechadas;
    }

    /** Jogadores com aura visivel no ultimo quadro fechado. */
    public static int jogadoresComAura() {
        return jogadoresFechados;
    }

    /**
     * Se o ultimo quadro fechado nao desenhou aura nenhuma.
     *
     * <p>E a assercao do AV8 "nenhuma aura visivel: custo zero, nao custo
     * pequeno" em forma de pergunta que da para fazer na hora.
     */
    public static boolean custoZero() {
        return chamadasFechadas == 0 && filamentosFechados == 0 && colunasFechadas == 0
                && aneisFechados == 0;
    }
}
