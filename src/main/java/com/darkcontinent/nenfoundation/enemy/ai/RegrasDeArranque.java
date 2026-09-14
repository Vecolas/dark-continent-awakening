package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * O arranque com FADIGA: quando ele pode sair, quanto dura e quanto custa.
 *
 * <p><b>O que este record NAO e.</b> Ele nao e {@code ChargeRules}. Aquele
 * descreve uma CARGA -- um ataque que atropela, medido dentro de uma
 * {@code AttackPhase}, que atordoa na parede e machuca com o corpo. Este aqui
 * nao toca em ataque nenhum: e locomocao pura, e a fase dele corre em paralelo a
 * do golpe. Fundir os dois faria a fadiga do corredor virar recuperacao de
 * ataque, e um bicho interrompido no golpe voltaria tambem a ter o arranque de
 * volta -- sem erro nenhum, e com "interromper" passando a valer o dobro do que
 * foi desenhado.</p>
 *
 * <p><b>Por que a fadiga existe, escrito uma vez.</b> Um bicho mais rapido que o
 * jogador e uma perseguicao que NUNCA termina: nao ha recuo possivel, nao ha
 * recarga de pocao, nao ha volta para casa. Isso nao da erro -- da um encontro
 * sem fim que so acaba quando alguem morre, e o jogador nao consegue nomear o
 * que esta errado. A janela de fadiga e a resposta dele, e e por isso que
 * {@link #FADIGA_MINIMA} e cobrada no construtor em vez de ficar num
 * comentario.</p>
 *
 * <p><b>{@link #FADIGA_MINIMA} e {@link #TETO_DA_MEDIA_DO_CICLO} nao sao botoes
 * de balanceamento.</b> Sao LIMITES DE DESIGN. Abaixo de vinte ticks (um
 * segundo) o jogador nao tem tempo de fechar distancia e encaixar um golpe, e a
 * fadiga passa a existir so no servidor. E acima de media 1.0 por ciclo o
 * arranque deixa de ter preco: ele vira um bonus de velocidade permanente com
 * uma animacao de cansaco em cima. Girar qualquer um dos dois numa sessao de
 * balanceamento nao ajustaria dificuldade -- apagaria a defesa do jogador.</p>
 *
 * <p><b>Este record nao conhece mundo, entidade nem Minecraft.</b> Ele recebe
 * numeros ja medidos e devolve uma {@link DecisaoDeArranque}; quem tem o mundo e
 * que transforma o multiplicador em velocidade de navegacao. Quem guarda a fase
 * e {@link EstadoDeArranque}, uma instancia por bicho.</p>
 *
 * @param ticksDeArranque quanto dura a corrida acelerada
 * @param ticksDeFadiga quanto dura a lentidao que a paga -- a janela do jogador
 * @param ticksDeRecarga quanto tempo, depois da fadiga, o arranque segue negado
 * @param multiplicadorDeArranque fator sobre a velocidade base durante o arranque
 * @param multiplicadorDeFadiga fator sobre a velocidade base durante a fadiga
 * @param distanciaMinima abaixo disso o arranque nao viaja nada de util
 * @param distanciaMaxima acima disso ele chega fatigado, ou nem chega
 * @param distanciaDeAbertura a que distancia do alvo um aliado ja conta como
 *        "o combate esta aberto"
 */
public record RegrasDeArranque(int ticksDeArranque, int ticksDeFadiga, int ticksDeRecarga,
        double multiplicadorDeArranque, double multiplicadorDeFadiga,
        double distanciaMinima, double distanciaMaxima, double distanciaDeAbertura) {

    /**
     * Ticks minimos de fadiga. LIMITE DE DESIGN, e por isso mora no codigo.
     *
     * <p>Vinte ticks sao um segundo. Abaixo disso o jogador nao fecha distancia
     * nem encaixa um golpe antes de a lentidao acabar: a fadiga acontece, o
     * servidor a registra, e para quem esta olhando ela nunca existiu.</p>
     */
    public static final int FADIGA_MINIMA = 20;

    /**
     * Media maxima de velocidade ao longo de um ciclo arranque + fadiga.
     *
     * <p>Um, ou seja: ao longo do ciclo inteiro o bicho nao pode andar MAIS do
     * que andaria sem arranque nenhum. O arranque redistribui velocidade no
     * tempo -- ele nao a cria. Acima de um, a fadiga vira encenacao: o bicho
     * ganha terreno liquido a cada ciclo, alcanca qualquer um e nada no log
     * explica por que.</p>
     */
    public static final double TETO_DA_MEDIA_DO_CICLO = 1.0D;

    public RegrasDeArranque {
        if (ticksDeArranque < 1) {
            throw new IllegalArgumentException("arranque de " + ticksDeArranque + " ticks: um"
                    + " arranque que dura menos de um tick nunca sai, e o bicho continua nascendo,"
                    + " perseguindo e mordendo -- so que sem a unica coisa que a ficha dele"
                    + " promete, e nada reprova");
        }
        if (ticksDeFadiga < FADIGA_MINIMA) {
            throw new IllegalArgumentException("fadiga de " + ticksDeFadiga + " ticks; o minimo e "
                    + FADIGA_MINIMA + ". Abaixo disso o jogador nao tem tempo de fechar distancia"
                    + " nem de encaixar um golpe antes de a lentidao acabar: a janela de resposta"
                    + " existe no servidor e nao existe para quem esta olhando, e a perseguicao"
                    + " passa a nao ter fim");
        }
        if (ticksDeRecarga < ticksDeArranque) {
            throw new IllegalArgumentException("recarga de " + ticksDeRecarga + " ticks para um"
                    + " arranque de " + ticksDeArranque + ": o tempo em que ele NAO pode arrancar"
                    + " ficaria menor que o proprio arranque. O arranque passa a estar mais"
                    + " disponivel do que e caro, e 'arrancar' vira a velocidade normal do bicho"
                    + " com um soluco no meio");
        }
        if (!Double.isFinite(multiplicadorDeArranque) || multiplicadorDeArranque <= 1.0D) {
            throw new IllegalArgumentException("multiplicador de arranque " + multiplicadorDeArranque
                    + ": um arranque que nao passa de 1.0 nao acelera nada. O telegrafo sai, a"
                    + " fadiga vem depois, e o bicho paga um preco por uma vantagem que ele nunca"
                    + " teve");
        }
        if (!Double.isFinite(multiplicadorDeFadiga) || multiplicadorDeFadiga <= 0.0D
                || multiplicadorDeFadiga >= 1.0D) {
            throw new IllegalArgumentException("multiplicador de fadiga " + multiplicadorDeFadiga
                    + ": fora de (0, 1) ele deixa de ser fadiga. Em zero o bicho CONGELA no meio do"
                    + " campo, o que le como travamento e nao como cansaco; em um ou mais, a fadiga"
                    + " nao custa nada e o arranque fica de graca");
        }
        if (!Double.isFinite(distanciaMinima) || distanciaMinima <= 0.0D
                || !Double.isFinite(distanciaMaxima) || distanciaMaxima <= distanciaMinima) {
            throw new IllegalArgumentException("a faixa do arranque e [" + distanciaMinima + ", "
                    + distanciaMaxima + "] e esta vazia ou invertida: nenhuma distancia satisfaz as"
                    + " duas pontas, entao o arranque NUNCA sai e nada reprova");
        }
        if (!Double.isFinite(distanciaDeAbertura) || distanciaDeAbertura <= 0.0D
                || distanciaDeAbertura > distanciaMinima) {
            throw new IllegalArgumentException("distancia de abertura " + distanciaDeAbertura
                    + " contra distancia minima " + distanciaMinima + ": o anel em que um aliado ja"
                    + " conta como 'combate aberto' ficaria mais largo que a distancia em que o"
                    + " lider arranca. Qualquer companheiro parado ao lado dele cancelaria o"
                    + " arranque, e a ficha do bicho desligaria em silencio");
        }

        double andadoNoCiclo = ticksDeArranque * multiplicadorDeArranque
                + ticksDeFadiga * multiplicadorDeFadiga;
        double semArranque = (ticksDeArranque + ticksDeFadiga) * TETO_DA_MEDIA_DO_CICLO;
        if (andadoNoCiclo > semArranque + 1.0E-9D) {
            throw new IllegalArgumentException("no ciclo de " + (ticksDeArranque + ticksDeFadiga)
                    + " ticks ele anda o equivalente a " + andadoNoCiclo + " e sem arranque andaria "
                    + semArranque + ": o arranque passa a CRIAR velocidade em vez de redistribui-la."
                    + " A fadiga vira encenacao, o bicho ganha terreno liquido a cada ciclo, e"
                    + " nenhum log explica por que ninguem consegue fugir dele");
        }
    }

    /** Ticks do ciclo inteiro: arranque, fadiga e recarga. */
    public int ticksDoCiclo() {
        return ticksDeArranque + ticksDeFadiga + ticksDeRecarga;
    }

    /**
     * Quanto o bicho anda, em media, por tick do par arranque + fadiga.
     *
     * <p>Publico porque e a conta que o construtor cobra, e uma regua cujo
     * resultado nao pode ser lido e uma regua que ninguem consegue conferir.</p>
     */
    public double mediaDoCiclo() {
        return (ticksDeArranque * multiplicadorDeArranque
                + ticksDeFadiga * multiplicadorDeFadiga)
                / (double) (ticksDeArranque + ticksDeFadiga);
    }

    /**
     * O fator de velocidade da fase -- DERIVADO, e nunca guardado.
     *
     * <p>Congelar este valor num campo no instante do arranque e o primeiro erro
     * da lista do CLAUDE.md: o bicho ficaria com o multiplicador da fase em que
     * ele estava quando alguem leu, e a fadiga nao apareceria na tela. Aqui ele
     * e lido na hora, sempre, dos dois lados -- servidor e cliente.</p>
     */
    public double multiplicadorDe(FaseDoArranque fase) {
        if (fase == null) {
            throw new IllegalArgumentException("fase de arranque ausente: sem fase nao ha"
                    + " multiplicador, e devolver 1.0 por padrao esconderia a ausencia atras de"
                    + " um bicho que anda normalmente");
        }
        return switch (fase) {
            case ARRANCANDO -> multiplicadorDeArranque;
            case FATIGADA -> multiplicadorDeFadiga;
            case PRONTA, EM_RECARGA -> 1.0D;
        };
    }

    /** Quantos ticks a fase dura. PRONTA nao tem prazo: ela espera uma decisao. */
    public int duracaoDe(FaseDoArranque fase) {
        if (fase == null) {
            throw new IllegalArgumentException("fase de arranque ausente");
        }
        return switch (fase) {
            case ARRANCANDO -> ticksDeArranque;
            case FATIGADA -> ticksDeFadiga;
            case EM_RECARGA -> ticksDeRecarga;
            case PRONTA -> Integer.MAX_VALUE;
        };
    }

    /**
     * O arranque sai neste tick? E, se nao, por que.
     *
     * <p>A ordem das recusas e a ordem do CUSTO e da AUTORIDADE. Primeiro a
     * fase, que nao depende de medir nada e vence tudo: um bicho fatigado nao
     * arranca nem que o alvo esteja perfeito. Depois os fatos do alvo, e so por
     * ultimo a geometria e o esquadrao. Invertida, um bicho em fadiga ainda
     * gastaria a medida de distancia e a varredura de aliados todo tick -- e isso
     * nao da erro, so custa, por bicho, numa colonia inteira.</p>
     *
     * @param fase em que ponto do ciclo ele esta agora
     * @param distanciaAoAlvo centro a centro, em blocos, ja medida pelo servidor
     * @param distanciaDoAliadoAoAlvo do membro de esquadrao mais perto do alvo ate
     *        o alvo; {@link Double#POSITIVE_INFINITY} quando nao ha nenhum
     * @param temAlvo o servidor reconhece um alvo hostil agora
     * @param alvoVisivel ha linha de visao ate ele
     */
    public DecisaoDeArranque decidir(FaseDoArranque fase, double distanciaAoAlvo,
            double distanciaDoAliadoAoAlvo, boolean temAlvo, boolean alvoVisivel) {
        if (fase == null) {
            throw new IllegalArgumentException("fase de arranque ausente");
        }
        if (Double.isNaN(distanciaAoAlvo) || distanciaAoAlvo < 0.0D
                || Double.isNaN(distanciaDoAliadoAoAlvo) || distanciaDoAliadoAoAlvo < 0.0D) {
            throw new IllegalArgumentException("medida de arranque invalida: alvo "
                    + distanciaAoAlvo + ", aliado " + distanciaDoAliadoAoAlvo + ". NaN aqui nao"
                    + " pode virar 'arranca': toda comparacao com NaN e falsa, entao a medida"
                    + " quebrada passaria por TODAS as recusas e chegaria ao fim como autorizacao."
                    + " O lado seguro de uma medida quebrada e o bicho nao sair do lugar");
        }
        if (fase == FaseDoArranque.ARRANCANDO) return DecisaoDeArranque.EM_ANDAMENTO;
        if (fase == FaseDoArranque.FATIGADA) return DecisaoDeArranque.FATIGADA;
        if (fase == FaseDoArranque.EM_RECARGA) return DecisaoDeArranque.EM_RECARGA;
        if (!temAlvo) return DecisaoDeArranque.SEM_ALVO;
        if (!alvoVisivel) return DecisaoDeArranque.SEM_VISAO;
        if (distanciaAoAlvo < distanciaMinima) return DecisaoDeArranque.PERTO_DEMAIS;
        if (distanciaAoAlvo > distanciaMaxima) return DecisaoDeArranque.LONGE_DEMAIS;
        if (distanciaDoAliadoAoAlvo <= distanciaDeAbertura) {
            return DecisaoDeArranque.ALIADO_JA_ABRIU;
        }
        return DecisaoDeArranque.ARRANCAR;
    }
}
