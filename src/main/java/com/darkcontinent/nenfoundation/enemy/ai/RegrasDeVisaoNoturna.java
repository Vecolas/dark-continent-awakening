package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * O que o trait {@code NIGHT_VISION} FAZ -- sem mundo, sem entidade, sem luz real.
 *
 * <p><b>Por que esta regra existe separada.</b> {@code ChimeraTrait.NIGHT_VISION}
 * e, sozinho, uma palavra num {@code Set}. Um trait que nao muda numero nenhum e
 * decoracao: ele aparece no save, aparece no debug, e o bicho se comporta
 * exatamente igual ao vizinho que nao o tem. Isso nao da erro -- da uma tabela de
 * genes inteira que o jogador nunca consegue observar, e ninguem descobre porque
 * nao ha nada para descobrir.</p>
 *
 * <p><b>A consequencia escolhida e o ALCANCE DE PERCEPCAO, e ela e a do batedor.</b>
 * Um mob comum enxerga menos na penumbra; quem tem visao noturna nao perde nada.
 * Para o Bat Scout isso e a ficha inteira: o valor dele e ver primeiro, e um
 * batedor que so serve de dia deixaria a colonia cega metade do tempo -- sem que
 * nada indicasse o motivo.</p>
 *
 * <p><b>E um DEGRAU, e nao uma rampa.</b> Interpolar o alcance entre a luz 0 e a
 * luz 15 seria mais fiel e ninguem conseguiria prever de quanto longe um mob
 * enxerga. Vale aqui a mesma razao que {@code HearingEvent.audivel} escreve para
 * a atenuacao do som: numero que a proxima pessoa vai querer girar precisa ser
 * previsivel. Com degrau, "abaixo de {@code luzDePenumbra} ele enxerga X" e uma
 * frase que cabe num relato de bug.</p>
 *
 * <p><b>O que ela NAO faz:</b> nao le {@code Level}, nao chama
 * {@code getMaxLocalRawBrightness} e nao conhece entidade. Quem tem o mundo mede
 * a luz e o trait e passa os dois; e por isso que esta regra roda em teste
 * unitario, sem servidor, e que nenhum caminho de cliente consegue alimenta-la.</p>
 *
 * @param fatorSemVisaoNoturna fracao do alcance que SOBRA no escuro para quem nao
 *        tem o trait; em (0, 1]
 * @param luzDePenumbra nivel de luz (0..15) a partir do qual ninguem perde alcance
 */
public record RegrasDeVisaoNoturna(double fatorSemVisaoNoturna, int luzDePenumbra) {

    /** O teto do nivel de luz do jogo. Nao e balanceamento: e o formato do dado. */
    public static final int LUZ_MAXIMA = 15;

    public RegrasDeVisaoNoturna {
        if (!Double.isFinite(fatorSemVisaoNoturna) || fatorSemVisaoNoturna <= 0.0D
                || fatorSemVisaoNoturna > 1.0D) {
            throw new IllegalArgumentException("fator no escuro (" + fatorSemVisaoNoturna
                    + ") fora de (0, 1]. Zero cega o mob por completo e ele para de reagir sem"
                    + " nenhum erro; acima de 1 o escuro passaria a AUMENTAR o alcance, e a"
                    + " consequencia do trait viraria o contrario do que ele diz.");
        }
        if (luzDePenumbra < 0 || luzDePenumbra > LUZ_MAXIMA) {
            throw new IllegalArgumentException("luz de penumbra (" + luzDePenumbra + ") fora de"
                    + " 0.." + LUZ_MAXIMA + ". Com 0 nenhum mob perde alcance nunca e a regra"
                    + " inteira vira enfeite; acima de " + LUZ_MAXIMA + " todo mob sem o trait"
                    + " enxerga menos em pleno dia, e nenhum dos dois levanta excecao em jogo.");
        }
    }

    /**
     * O alcance que este mob realmente tem AGORA.
     *
     * <p>Para quem tem o trait a resposta e sempre {@code alcanceBase} -- inclusive
     * no escuro total. Isso e a promessa inteira, e e o que o teste cobra varrendo
     * os dezesseis niveis de luz: uma implementacao que aplicasse o fator "so um
     * pouquinho" para todo mundo passaria em qualquer teste de um nivel so.</p>
     *
     * @param alcanceBase o alcance declarado pelo perfil (FOLLOW_RANGE), em blocos
     * @param nivelDeLuz luz medida pelo servidor no bloco do mob, 0..15
     * @param temVisaoNoturna o mob carrega {@code ChimeraTrait.NIGHT_VISION}
     */
    public double alcanceEfetivo(double alcanceBase, int nivelDeLuz, boolean temVisaoNoturna) {
        if (!Double.isFinite(alcanceBase) || alcanceBase <= 0.0D) {
            throw new IllegalArgumentException("alcance base invalido: " + alcanceBase
                    + ". Um alcance zero ou NaN produziria um cone de visao que nao enxerga nada,"
                    + " e o sintoma seria um mob parado ao lado do jogador -- sem log nenhum.");
        }
        if (nivelDeLuz < 0 || nivelDeLuz > LUZ_MAXIMA) {
            // Recusa com motivo, e nao um clamp silencioso: 0..15 e o contrato do
            // proprio jogo, e um valor fora disso significa que quem chamou mediu
            // outra coisa (luz de ceu, luz de bloco somada duas vezes). Clampar
            // esconderia a medida errada e a percepcao passaria a variar por um
            // motivo que ninguem consegue rastrear.
            throw new IllegalArgumentException("nivel de luz (" + nivelDeLuz + ") fora de 0.."
                    + LUZ_MAXIMA + ": quem chamou nao mediu luz de bloco");
        }
        if (temVisaoNoturna || nivelDeLuz >= luzDePenumbra) return alcanceBase;
        return alcanceBase * fatorSemVisaoNoturna;
    }

    /**
     * O escuro cobra alguma coisa deste mob?
     *
     * <p>Existe para quem precisa EXPLICAR a decisao -- um comando de debug, um
     * relato -- sem refazer a conta e arriscar refazer diferente. Duas contas para
     * a mesma verdade divergem na primeira correcao, e a divergencia apareceria
     * como uma ferramenta de diagnostico que discorda do jogo.</p>
     */
    public boolean oEscuroCobra(int nivelDeLuz, boolean temVisaoNoturna) {
        return alcanceEfetivo(1.0D, nivelDeLuz, temVisaoNoturna) < 1.0D;
    }
}
