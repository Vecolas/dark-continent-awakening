package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * As regras da teia da Spider Webber: quando o fio sai, e onde ela fica.
 *
 * <p><b>A decisao que este arquivo carrega:</b> num inimigo de alcance, "de onde
 * ele atira" e "onde ele fica parado" nao sao dois assuntos. Sao o mesmo par de
 * distancias, e e essa identidade que faz o papel RANGED existir em jogo em vez
 * de existir no perfil. A faixa em que ela atira e EXATAMENTE a faixa em que ela
 * para; fora dela, ela se mexe.</p>
 *
 * <p><b>Por que os dois alcances moram no MESMO record.</b> Separados -- o limiar
 * do tiro num lugar e o limiar do recuo no outro -- nada impediria a aranha de
 * recuar ate uma distancia em que ela ainda se recusa a atirar. Isso nao daria
 * erro nenhum: daria um oficial andando para tras e para frente eternamente,
 * telegrafando nada, e o relato que chega e "a aranha bugou". Juntos, a
 * identidade e provada em {@link #espacamento(double)} contra
 * {@link #podeLancar}.</p>
 *
 * <p><b>A imobilizacao tem DUAS saidas, e as duas sao obrigatorias aqui.</b> O
 * relogio ({@code ticksDeImobilizacao}) e a saida de quem nao conseguiu fazer
 * nada; o dano na fiandeira ({@code danoQueLiberta}) e a saida de quem reagiu.
 * Com so o relogio, a resposta certa do jogador vira esperar -- e esperar e o que
 * o esquadrao quer. Com so o dano, quem ficou sozinho e sem arma de alcance
 * morre sem ter tido resposta. O construtor recusa zerar qualquer uma das duas.</p>
 *
 * <p>Este record nao conhece mundo, entidade nem Minecraft: ele recebe numeros ja
 * medidos pelo servidor e devolve decisoes nomeadas. Quem mede a distancia, quem
 * consulta a linha de visao e quem move os pes e a entidade, no lado
 * autoritativo -- e e isso que permite provar a faixa inteira sem servidor.</p>
 *
 * @param alcanceMinimo distancia (centro a centro, em blocos) abaixo da qual nao
 *        ha fio a esticar: a zona morta que o jogador ocupa de proposito
 * @param alcanceMaximo distancia acima da qual o fio nao chega
 * @param ticksDeImobilizacao quanto tempo a presa fica presa, no maximo
 * @param danoQueLiberta dano acumulado na fiandeira que rasga o fio antes do prazo
 */
public record RegrasDeTeia(double alcanceMinimo, double alcanceMaximo,
        int ticksDeImobilizacao, float danoQueLiberta) {

    /**
     * Largura minima, em blocos, da faixa em que a teia pode sair.
     *
     * <p>Limite de DESIGN com conta atras, e nao botao de balanceamento. A aranha
     * anda a 0.28 de velocidade base -- cerca de 0.14 bloco por tick em
     * perseguicao -- e o aviso da teia dura trinta ticks. Uma faixa mais estreita
     * que dois blocos e atravessada por um jogador andando durante o proprio
     * aviso: ela comecaria o telegrafo dentro da faixa e terminaria fora, cancelando
     * o tiro sozinha. Isso nao levanta excecao; levanta um oficial que nunca
     * consegue atirar em ninguem que esteja se mexendo.</p>
     */
    public static final double FAIXA_MINIMA_DE_TIRO = 2.0D;

    /**
     * Teto de imobilizacao, em ticks.
     *
     * <p>Limite de DESIGN: cinco segundos e o maximo que se pode tirar de um
     * jogador sem que a janela deixe de ser uma janela. Acima disso a teia deixa
     * de ser uma tatica de esquadrao e vira a briga inteira -- e o jogador que
     * assiste a propria morte nao tem nada para aprender na proxima tentativa.
     * Ele nao vai para config exatamente por isso: girar este botao numa sessao
     * de balanceamento mudaria o genero do encontro, e nao a dificuldade dele.</p>
     */
    public static final int TETO_DE_IMOBILIZACAO_EM_TICKS = 100;

    public RegrasDeTeia {
        if (!Double.isFinite(alcanceMinimo) || !Double.isFinite(alcanceMaximo)
                || !Float.isFinite(danoQueLiberta)) {
            throw new IllegalArgumentException("regras de teia com numero nao finito:"
                    + " min=" + alcanceMinimo + " max=" + alcanceMaximo
                    + " dano=" + danoQueLiberta);
        }
        if (alcanceMinimo <= 0.0D) {
            throw new IllegalArgumentException("alcance minimo da teia e " + alcanceMinimo
                    + ", e ele precisa ser maior que zero: sem zona morta, encostar na aranha"
                    + " deixa de ser resposta e ela vira um mob corpo-a-corpo que atira -- o"
                    + " jogador perde a unica saida que o encontro oferece, e nada acusa");
        }
        if (alcanceMaximo - alcanceMinimo < FAIXA_MINIMA_DE_TIRO) {
            throw new IllegalArgumentException("a faixa de tiro vai de " + alcanceMinimo + " a "
                    + alcanceMaximo + " (" + (alcanceMaximo - alcanceMinimo) + " bloco(s)) e o"
                    + " minimo e " + FAIXA_MINIMA_DE_TIRO + ": um alvo andando atravessa uma faixa"
                    + " assim durante o proprio aviso, e a aranha cancela o tiro sozinha para"
                    + " sempre -- sem erro, so um oficial que nunca ataca");
        }
        if (ticksDeImobilizacao <= 0) {
            throw new IllegalArgumentException("imobilizacao de " + ticksDeImobilizacao
                    + " ticks: teia que nao prende nada e enfeite, e o mob perde o unico papel"
                    + " que ele tem no esquadrao");
        }
        if (ticksDeImobilizacao > TETO_DE_IMOBILIZACAO_EM_TICKS) {
            throw new IllegalArgumentException("imobilizacao de " + ticksDeImobilizacao
                    + " ticks acima do teto de " + TETO_DE_IMOBILIZACAO_EM_TICKS + ": prender por"
                    + " mais tempo que isso e morte sem resposta, e o jogador assiste em vez de"
                    + " jogar");
        }
        if (danoQueLiberta <= 0.0F) {
            throw new IllegalArgumentException("dano que liberta e " + danoQueLiberta + ": com"
                    + " zero, qualquer respingo rasga o fio e a teia nunca segura ninguem; sem"
                    + " uma saida por DANO, a resposta certa do jogador passa a ser esperar --"
                    + " que e exatamente o que o esquadrao quer");
        }
    }

    /**
     * A teia pode sair agora? E se nao, POR QUE.
     *
     * <p><b>A ordem das recusas e deliberada.</b> Medida invalida vem primeiro
     * porque nada abaixo dela pode confiar no numero. Distancia vem ANTES de linha
     * de visao porque {@link RecusaDaTeia#PERTO_DEMAIS} e o unico motivo desta
     * lista que o jogador provoca de proposito, e e ele que precisa aparecer
     * quando os dois valem -- quem esta colado na aranha tambem costuma estar sem
     * linha de visao limpa, e reportar a parede esconderia a licao.</p>
     *
     * @param distancia centro a centro, em blocos, medida pelo servidor
     * @param linhaDeVisao fato medido pelo servidor, nunca afirmado pelo cliente
     * @param jaPrendendo ha alguem preso nesta fiandeira agora
     * @param prontoParaAtacar o controlador de ataque autoriza uma instancia nova
     */
    public RecusaDaTeia podeLancar(double distancia, boolean linhaDeVisao,
            boolean jaPrendendo, boolean prontoParaAtacar) {
        if (!Double.isFinite(distancia) || distancia < 0.0D) return RecusaDaTeia.MEDIDA_INVALIDA;
        if (jaPrendendo) return RecusaDaTeia.JA_PRENDENDO;
        if (!prontoParaAtacar) return RecusaDaTeia.EM_RECARGA;
        if (distancia < alcanceMinimo) return RecusaDaTeia.PERTO_DEMAIS;
        if (distancia > alcanceMaximo) return RecusaDaTeia.LONGE_DEMAIS;
        if (!linhaDeVisao) return RecusaDaTeia.SEM_LINHA_DE_VISAO;
        return RecusaDaTeia.NENHUMA;
    }

    /**
     * Onde ela fica: a MESMA faixa que autoriza o tiro.
     *
     * <p>Medida invalida devolve {@link DecisaoDeEspacamento#MANTER}, e a escolha
     * e o lado seguro: um NaN nao pode mandar o oficial correr para lugar nenhum.
     * O pior que "manter" faz e a aranha ficar parada um tick a mais; "aproximar"
     * com uma medida invalida a faria andar para dentro do alvo, que e o oposto do
     * papel dela e ninguem ligaria uma coisa a outra.</p>
     */
    public DecisaoDeEspacamento espacamento(double distancia) {
        if (!Double.isFinite(distancia) || distancia < 0.0D) return DecisaoDeEspacamento.MANTER;
        if (distancia < alcanceMinimo) return DecisaoDeEspacamento.RECUAR;
        if (distancia > alcanceMaximo) return DecisaoDeEspacamento.APROXIMAR;
        return DecisaoDeEspacamento.MANTER;
    }

    /**
     * Atalho de leitura: o alvo esta na faixa de tiro?
     *
     * <p>Os limites sao derivados de {@link #espacamento(double)} de proposito, e
     * nao escritos como uma terceira comparacao. Duas comparacoes independentes
     * divergem no dia em que um dos limites virar exclusivo de um lado, e a
     * divergencia e uma fresta de meio bloco em que ela para e nao atira.</p>
     *
     * <p>A medida invalida e o unico ponto em que as duas perguntas se separam, e
     * a separacao e escrita aqui em vez de ficar implicita: para "onde ela poe os
     * pes", o lado seguro de um NaN e PARAR; para "o alvo esta na faixa", o lado
     * seguro e NAO. Herdar o {@code MANTER} sem este guarda faria um NaN responder
     * "esta na faixa" e autorizar um tiro contra um alvo sem posicao.</p>
     */
    public boolean naFaixaDeTiro(double distancia) {
        return Double.isFinite(distancia) && distancia >= 0.0D
                && espacamento(distancia) == DecisaoDeEspacamento.MANTER;
    }

    /** A imobilizacao em segundos -- para relato humano, nunca para conta de tick. */
    public double segundosDeImobilizacao() {
        return ticksDeImobilizacao / 20.0D;
    }
}
