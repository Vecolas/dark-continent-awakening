package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * Acumulo, teto, duracao e decaimento do veneno -- a regra inteira, e PURA.
 *
 * <p><b>A DURACAO E O ACUMULADOR, e essa e a decisao central deste arquivo.</b>
 * Cada ferroada soma {@code duracaoPorFerroadaEmTicks} ao que ainda resta no
 * alvo, limitado por {@code tetoDeDuracaoEmTicks}; o nivel do efeito e DERIVADO
 * do total acumulado. O decaimento nao e escrito aqui porque ele ja existe: e a
 * propria duracao do efeito escorrendo um tick por tick.</p>
 *
 * <p><b>Por que nao um contador proprio por vitima guardado na formiga.</b> Duas
 * razoes, e as duas sao falhas silenciosas:</p>
 *
 * <ul>
 *   <li><b>o teto deixaria de ser teto.</b> Um esquadrao tem mais de uma formiga
 *       venenosa. Com o acumulo guardado em cada uma, tres ferroadas de tres
 *       formigas somariam tres vezes ate o teto de CADA UMA -- e o jogador
 *       morreria de dano continuo que nenhuma das tres, sozinha, era capaz de
 *       causar. O log fica limpo e a ficha de cada bicho continua certa;</li>
 *   <li><b>o mapa vazaria.</b> Um {@code Map<UUID, Integer>} por formiga cresce
 *       com cada alvo tocado e nao encolhe sozinho; o sintoma nao e erro, e
 *       memoria subindo devagar num servidor com colonia.</li>
 * </ul>
 *
 * <p>Lendo o que o alvo JA carrega, ha uma unica fonte de verdade, ela sobrevive
 * a morte da formiga -- o veneno nao para porque quem ferroou morreu -- e o teto
 * vale para o encontro inteiro.</p>
 *
 * <p><b>Ela nao conhece entidade, efeito nem servidor.</b> Recebe dois fatos ja
 * medidos e devolve a conta feita. Quem aplica e quem tem o mundo.</p>
 *
 * @param duracaoPorFerroadaEmTicks quanto cada ferroada SOMA a duracao restante
 * @param tetoDeDuracaoEmTicks duracao maxima acumulada, para o encontro inteiro
 * @param amplificadorMaximo nivel maximo do efeito vanilla (0 = nivel I)
 */
public record RegrasDeVeneno(int duracaoPorFerroadaEmTicks, int tetoDeDuracaoEmTicks,
        int amplificadorMaximo) {

    /**
     * Teto de design do amplificador, e nao botao de tuning.
     *
     * <p>O intervalo de dano do veneno vanilla e 25 ticks e ele e DIVIDIDO PELA
     * METADE a cada nivel: 25, 12, 6, 3. A partir do nivel IV o efeito drena a
     * barra mais depressa do que qualquer golpe corpo-a-corpo do bicho, e
     * "acumular" deixaria de ser pressao para virar execucao -- por uma fonte de
     * dano que nao aparece na tela e que o jogador nao consegue apontar. Girar
     * este numero nao ajusta dificuldade: troca o que o veneno E.</p>
     */
    public static final int AMPLIFICADOR_LIMITE = 3;

    public RegrasDeVeneno {
        if (duracaoPorFerroadaEmTicks < 1) {
            throw new IllegalArgumentException("duracao por ferroada invalida: "
                    + duracaoPorFerroadaEmTicks + ". Veneno que some no tick seguinte nao e"
                    + " veneno: e um segundo tipo de dano direto, com telegrafo caro e sem"
                    + " consequencia nenhuma. A DURACAO e o que faz o veneno existir.");
        }
        if (tetoDeDuracaoEmTicks <= duracaoPorFerroadaEmTicks) {
            throw new IllegalArgumentException("teto de " + tetoDeDuracaoEmTicks + " tick(s) com"
                    + " ferroada de " + duracaoPorFerroadaEmTicks + ": a primeira ferroada ja"
                    + " chega ao teto, a segunda nao acumula nada, e o acumulo declarado na ficha"
                    + " do bicho vira ficcao -- sem que nada reprove.");
        }
        if (amplificadorMaximo < 0 || amplificadorMaximo > AMPLIFICADOR_LIMITE) {
            throw new IllegalArgumentException("amplificador maximo " + amplificadorMaximo
                    + " fora de 0.." + AMPLIFICADOR_LIMITE + ". Acima do limite o intervalo de"
                    + " dano do veneno vanilla cai para tres ticks e o acumulo vira execucao por"
                    + " uma fonte que o jogador nao consegue apontar.");
        }
    }

    /**
     * A UNICA decisao sobre veneno. Pura: quem chama aplica, nao decide.
     *
     * <p>A ordem das perguntas e a ordem da HISTORIA. A imunidade vem primeiro
     * porque nenhuma conta adiante importa depois dela -- somar duracao para um
     * morto-vivo produziria um numero correto que ninguem pode usar.</p>
     *
     * @param alvoPodeSerEnvenenado medido pelo servidor no alvo real; nunca
     *        deduzido aqui, porque quem sabe disso e o jogo e nao a regra
     * @param ticksRestantes o que AINDA resta do veneno no alvo, lido do proprio
     *        efeito; zero quando nao ha veneno nenhum
     */
    public DoseDeVeneno aplicar(boolean alvoPodeSerEnvenenado, int ticksRestantes) {
        if (ticksRestantes < 0) {
            throw new IllegalArgumentException("ticks restantes negativos: " + ticksRestantes
                    + ". Duracao negativa nao existe no jogo; recebe-la aqui significa que quem"
                    + " chamou leu o efeito errado, e a conta sairia plausivel demais para"
                    + " alguem notar.");
        }
        if (!alvoPodeSerEnvenenado) return DoseDeVeneno.imune();

        int total = Math.min(tetoDeDuracaoEmTicks, ticksRestantes + duracaoPorFerroadaEmTicks);
        DecisaoDeVeneno decisao = total > ticksRestantes
                ? DecisaoDeVeneno.APLICOU : DecisaoDeVeneno.NO_TETO;
        return new DoseDeVeneno(decisao, total, amplificadorPara(total));
    }

    /**
     * O nivel do efeito para uma duracao acumulada.
     *
     * <p>DERIVADO, e nunca um contador paralelo. Um campo "nivel" ao lado da
     * duracao seria a segunda fonte para a mesma verdade, e as duas divergiriam
     * na primeira vez que o efeito expirasse sozinho: a duracao voltaria a zero e
     * o nivel continuaria onde estava, deixando a proxima ferroada aplicar veneno
     * de nivel alto em quem nao tinha nenhum.</p>
     *
     * <p>O degrau e a propria dose: uma ferroada e nivel I, duas sao nivel II. E
     * como a duracao escorre sozinha, quem esperar o veneno baixar volta ao
     * degrau de baixo -- este e o decaimento, e ele nao precisa de codigo porque
     * o relogio do efeito ja o faz.</p>
     */
    public int amplificadorPara(int ticksAcumulados) {
        if (ticksAcumulados <= 0) return 0;
        return Math.min(amplificadorMaximo,
                (ticksAcumulados - 1) / duracaoPorFerroadaEmTicks);
    }

    /** Quantas ferroadas cabem ate o teto -- para relato e teste, nunca para gameplay. */
    public int ferroadasAteOTeto() {
        return (tetoDeDuracaoEmTicks + duracaoPorFerroadaEmTicks - 1) / duracaoPorFerroadaEmTicks;
    }
}
