package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * Quando um salto de aproximacao pode sair -- e quanto ele empurra.
 *
 * <p><b>Um salto SEM TELEGRAFO nao e um salto: e um teleporte com dano.</b> O
 * servidor aplica um impulso unico e o corpo viaja quatro blocos em meio
 * segundo. Se nada na tela avisou antes, o jogador ve o bicho a distancia num
 * quadro e em cima dele no seguinte -- dano certo, cooldown certo, log limpo, e
 * a unica leitura que ele tinha quebrada. Por isso {@link #TELEGRAFO_MINIMO}
 * existe e e cobrado no construtor, e nao deixado num comentario.</p>
 *
 * <p><b>{@link #TELEGRAFO_MINIMO} nao e botao de balanceamento.</b> E um limite
 * de LEITURA: abaixo de 0.4 segundo o olho nao separa o agachamento do salto, e
 * o aviso deixa de existir mesmo estando la. Girar este numero numa sessao de
 * balanceamento nao ajustaria dificuldade -- apagaria a defesa do jogador.</p>
 *
 * <p><b>As duas estimativas balisticas sao APROXIMACOES declaradas.</b> Elas
 * usam a gravidade e o arrasto horizontal do Minecraft e ignoram arrasto
 * vertical, colisao com bloco e efeitos de status, entao produzem um LIMITE
 * SUPERIOR do que o salto alcanca. Serve para o que elas existem: reprovar a
 * configuracao grosseiramente impossivel -- um salto que nem sequer cruza a
 * propria distancia minima, ou que aceita um desnivel que ele nunca sobe.
 * Configuracao apertada no limite elas NAO pegam, e isso esta escrito aqui em
 * vez de ficar implicito.</p>
 *
 * <p><b>Este record nao conhece mundo, entidade nem Minecraft.</b> Ele recebe
 * numeros ja medidos e devolve uma {@link DecisaoDeSalto}; quem tem o mundo e
 * que transforma o impulso em vetor.</p>
 *
 * @param ticksDeTelegrafo ticks de aviso antes de o impulso sair
 * @param alcanceMinimo abaixo disso o salto nao serve -- o golpe comum ja chega
 * @param alcanceMaximo acima disso o salto pousa no vazio
 * @param desnivelMaximo quanto o alvo pode estar acima, em blocos
 * @param impulsoHorizontal velocidade inicial para a frente, em blocos por tick
 * @param impulsoVertical velocidade inicial para cima, em blocos por tick
 */
public record RegrasDeSalto(int ticksDeTelegrafo, double alcanceMinimo, double alcanceMaximo,
        double desnivelMaximo, double impulsoHorizontal, double impulsoVertical) {

    /**
     * Ticks minimos de aviso. LIMITE DE DESIGN, e por isso mora no codigo.
     *
     * <p>Oito ticks sao 0.4 segundo. Abaixo disso o agachamento e o salto ocupam
     * a mesma fracao de segundo na tela e o olho nao os separa: o aviso existe no
     * servidor e nao existe para o jogador.</p>
     */
    public static final int TELEGRAFO_MINIMO = 8;

    /** Gravidade de entidade viva, em blocos por tick ao quadrado. Fato do jogo. */
    public static final double GRAVIDADE_POR_TICK = 0.08D;

    /** Arrasto horizontal no ar, por tick. Fato do jogo. */
    public static final double ARRASTO_HORIZONTAL = 0.91D;

    public RegrasDeSalto {
        if (ticksDeTelegrafo < TELEGRAFO_MINIMO) {
            throw new IllegalArgumentException("telegrafo de " + ticksDeTelegrafo + " ticks; o"
                    + " minimo e " + TELEGRAFO_MINIMO + ". Abaixo disso o agachamento e o salto"
                    + " ocupam a mesma fracao de segundo na tela, o aviso deixa de ser legivel, e"
                    + " o salto vira um teleporte com dano -- sem erro nenhum para procurar");
        }
        if (!Double.isFinite(alcanceMinimo) || alcanceMinimo <= 0.0D
                || !Double.isFinite(alcanceMaximo) || alcanceMaximo <= 0.0D) {
            throw new IllegalArgumentException("alcances de salto invalidos");
        }
        if (alcanceMaximo <= alcanceMinimo) {
            throw new IllegalArgumentException("a faixa do salto e [" + alcanceMinimo + ", "
                    + alcanceMaximo + "] e esta vazia: nenhuma distancia satisfaz as duas pontas,"
                    + " entao o salto NUNCA sai. O bicho continua nascendo, perseguindo e mordendo"
                    + " -- so que sem a unica coisa que a ficha dele promete, e nada reprova");
        }
        if (!Double.isFinite(desnivelMaximo) || desnivelMaximo <= 0.0D) {
            throw new IllegalArgumentException("desnivel maximo invalido: " + desnivelMaximo
                    + ". Com zero, qualquer degrau de meio bloco recusa o salto e ele so funciona"
                    + " em terreno plano de arena");
        }
        if (!Double.isFinite(impulsoHorizontal) || impulsoHorizontal <= 0.0D
                || !Double.isFinite(impulsoVertical) || impulsoVertical <= 0.0D) {
            throw new IllegalArgumentException("impulsos de salto invalidos: horizontal "
                    + impulsoHorizontal + ", vertical " + impulsoVertical + ". Zero em qualquer um"
                    + " deles da um 'salto' que nao sai do lugar ou que nao sai do chao");
        }

        double alturaEstimada = impulsoVertical * impulsoVertical / (2.0D * GRAVIDADE_POR_TICK);
        if (alturaEstimada < desnivelMaximo) {
            throw new IllegalArgumentException("o salto sobe no maximo " + alturaEstimada
                    + " bloco(s) e aceita alvo ate " + desnivelMaximo + " acima: o bicho se"
                    + " compromete com um salto que ele nao alcanca, bate na parede e cai de volta"
                    + " com a recarga gasta. Nada acusa -- ele so parece estar tentando");
        }
        double ticksNoAr = 2.0D * impulsoVertical / GRAVIDADE_POR_TICK;
        double alcanceEstimado = impulsoHorizontal
                * (1.0D - Math.pow(ARRASTO_HORIZONTAL, ticksNoAr)) / (1.0D - ARRASTO_HORIZONTAL);
        if (alcanceEstimado < alcanceMinimo) {
            throw new IllegalArgumentException("o salto cobre no maximo " + alcanceEstimado
                    + " bloco(s) e so e autorizado a partir de " + alcanceMinimo + ": ele sempre"
                    + " pousa CURTO. O telegrafo sai, o impulso sai, o cooldown sai, e o alvo"
                    + " continua fora de alcance -- o jogador aprende que o salto e inofensivo");
        }
    }

    /** Altura maxima estimada do salto, em blocos. Limite superior; ver o javadoc da classe. */
    public double alturaEstimada() {
        return impulsoVertical * impulsoVertical / (2.0D * GRAVIDADE_POR_TICK);
    }

    /** Alcance horizontal estimado, em blocos. Limite superior; ver o javadoc da classe. */
    public double alcanceHorizontalEstimado() {
        double ticksNoAr = 2.0D * impulsoVertical / GRAVIDADE_POR_TICK;
        return impulsoHorizontal
                * (1.0D - Math.pow(ARRASTO_HORIZONTAL, ticksNoAr)) / (1.0D - ARRASTO_HORIZONTAL);
    }

    /**
     * O salto sai neste tick? E, se nao, por que.
     *
     * <p>A ordem das recusas e a ordem do custo: primeiro o que nao depende de
     * medir nada (recarga, apoio), depois a geometria. Invertida, um bicho em
     * recarga ainda gastaria a medida de distancia todo tick -- e isso nao da
     * erro, so custa, por bicho, numa colonia inteira.</p>
     *
     * @param distanciaAoAlvo centro a centro, em blocos, ja medida pelo servidor
     * @param desnivel quanto o alvo esta ACIMA, em blocos; negativo quando abaixo
     * @param noChao o servidor confirmou que os pes estao apoiados
     * @param recargaPronta o controlador de ataque autoriza comecar
     */
    public DecisaoDeSalto decidir(double distanciaAoAlvo, double desnivel, boolean noChao,
            boolean recargaPronta) {
        if (!Double.isFinite(distanciaAoAlvo) || distanciaAoAlvo < 0.0D
                || !Double.isFinite(desnivel)) {
            throw new IllegalArgumentException("medida de salto invalida: distancia "
                    + distanciaAoAlvo + ", desnivel " + desnivel + ". NaN aqui nao pode virar"
                    + " 'salta': o lado seguro de uma medida quebrada e o bicho nao decolar");
        }
        if (!recargaPronta) return DecisaoDeSalto.EM_RECARGA;
        if (!noChao) return DecisaoDeSalto.SEM_APOIO;
        if (distanciaAoAlvo < alcanceMinimo) return DecisaoDeSalto.PERTO_DEMAIS;
        if (distanciaAoAlvo > alcanceMaximo) return DecisaoDeSalto.LONGE_DEMAIS;
        if (desnivel > desnivelMaximo) return DecisaoDeSalto.SUBIDA_DEMAIS;
        return DecisaoDeSalto.SALTAR;
    }
}
