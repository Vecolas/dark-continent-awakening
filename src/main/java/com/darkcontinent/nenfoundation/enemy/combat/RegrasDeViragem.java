package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * As regras do besouro de carapaca: quando ele vira, e onde ele paga.
 *
 * <p><b>A decisao que este arquivo carrega.</b> Um bicho de armadura 9 nao tem
 * ponto fraco alcancavel na pose normal -- bater de frente na carapaca e a coisa
 * mais natural do mundo e e exatamente a coisa que nao funciona. O encontro,
 * entao, nao e sobre MIRAR: e sobre fazer o bicho APRESENTAR o lado mole. Ele
 * apresenta em duas situacoes, e em nenhuma outra:</p>
 *
 * <ol>
 *   <li><b>empinado</b>, durante o {@link AttackPhase#WINDUP} da investida: ele
 *       sobe no par traseiro de pernas e vira a frente do ventre para quem
 *       estiver na frente;</li>
 *   <li><b>de costas</b>, depois de virar: o ventre fica para cima e nao ha
 *       angulo nenhum que o esconda.</li>
 * </ol>
 *
 * <p><b>O PROBLEMA DO "POR BAIXO", E COMO ELE FOI RESOLVIDO.</b> O
 * {@link WeakPointResolver} deste repositorio compara duas coisas: altura
 * RELATIVA do impacto (com piso, nunca teto) e cosseno FRONTAL (tambem com
 * piso). Ele sabe dizer "acima de tal altura e de frente"; ele nao sabe dizer
 * "por baixo". Um ventre que exigisse ser atingido por baixo precisaria de um
 * TETO de altura, e escrever {@code alturaMinima = 0.0} com um teto implicito na
 * cabeca de quem leu seria a pior saida: o resolver aprovaria o bicho inteiro, e
 * todo golpe viraria critico sem que uma linha de log mudasse.</p>
 *
 * <p>A saida foi nao precisar de "por baixo". Nas DUAS posturas em que o ventre
 * conta, ele esta em cima:</p>
 *
 * <ul>
 *   <li><b>de costas</b>, o ventre literalmente aponta para o ceu -- o modelo gira
 *       180 graus e a placa que estava no chao passa a ser a parte alta da caixa
 *       de colisao. O piso de altura funciona como escrito, e o piso de cosseno e
 *       neutralizado em -1: um besouro de pernas para o ar nao tem frente, e
 *       exigir angulo obrigaria o jogador a procurar uma "frente" que deixou de
 *       existir enquanto a janela -- que e curta e silenciosa -- queima;</li>
 *   <li><b>empinado</b>, o que fica virado para o jogador e a frente inteira do
 *       bicho: cabeca, chifres e a boca do ventre. Ali os dois pisos servem, e
 *       servem juntos -- so paga quem esta na frente E acima da linha das pernas
 *       plantadas.</li>
 * </ul>
 *
 * <p><b>O QUE FICA SEM COBRIR, dito com todas as letras.</b> O resolver nao tem
 * teto, entao:</p>
 *
 * <ul>
 *   <li>de costas, TUDO que estiver acima do limiar conta como ventre -- e isso
 *       inclui as pernas sacudindo no ar e o alto do torax, que na altura
 *       espelhada ficam junto com a placa. Isso e aceito: de costas, a face
 *       exposta e a de baixo inteira, e separar perna de placa exigiria uma
 *       geometria de regiao que este repositorio nao tem;</li>
 *   <li>empinado, um golpe frontal alto pega a aresta da carapaca erguida pelo
 *       mesmo preco do ventre. Tambem e aceito: empinado, a carapaca esta virada
 *       para cima e para tras, e quem consegue acertar a aresta dela de frente ja
 *       esta dentro do alcance dos chifres;</li>
 *   <li><b>nao existe acerto "por baixo" com o bicho de pe.</b> Um jogador num
 *       buraco embaixo do besouro batendo para cima recebe a regiao comum, e isso
 *       e deliberado -- se um dia alguem quiser pagar por esse angulo, precisa de
 *       um resolver com teto, e nao de um limiar torcido aqui.</li>
 * </ul>
 *
 * <p>Este record nao conhece mundo, entidade nem Minecraft: recebe numeros ja
 * medidos PELO SERVIDOR e devolve decisoes. A geometria de impacto nunca vem do
 * cliente -- ver {@link WeakPointResolver}.</p>
 *
 * @param ventreEmpinado resolve a regiao enquanto ele esta erguido para investir
 * @param ventreDeCostas resolve a regiao enquanto ele esta virado
 * @param ticksDeCostas quanto tempo a janela de costas dura, do tombo ao fim
 * @param ticksParaLevantar quantos ticks FINAIS dessa janela ele gasta se
 *        endireitando -- o aviso visivel de que ela esta acabando
 */
public record RegrasDeViragem(WeakPointResolver ventreEmpinado, WeakPointResolver ventreDeCostas,
        int ticksDeCostas, int ticksParaLevantar) {

    /**
     * Altura relativa abaixo da qual "ventre de costas" deixaria de significar
     * ventre.
     *
     * <p>Limite de DESIGN, e nao botao de balanceamento. Com o bicho virado, a
     * carapaca ocupa a METADE DE BAIXO da caixa de colisao. Um limiar em 0.5 ou
     * menos passaria a pagar multiplicador por acertar a carapaca encostada no
     * chao -- ou seja, exatamente a superficie que o encontro inteiro existe para
     * dizer que nao paga. Isso nao levanta erro nenhum: da um besouro que morre
     * rapido demais de costas por um motivo que ninguem consegue nomear.</p>
     */
    public static final double ALTURA_MINIMA_DE_COSTAS_ACEITAVEL = 0.5D;

    public RegrasDeViragem {
        if (ventreEmpinado == null || ventreDeCostas == null) {
            throw new IllegalArgumentException("regras de viragem sem resolver de ventre");
        }
        // As duas posturas tem de nomear as MESMAS regioes. Nomes diferentes nao
        // dariam erro: o WeakPointRegistry devolveria multiplicador 1.0 para o id
        // que ele nao conhece, e uma das duas janelas de vulnerabilidade pararia
        // de pagar em silencio -- provavelmente a mais rara, que e a que ninguem
        // testa a mao.
        if (!ventreEmpinado.regiaoVulneravel().equals(ventreDeCostas.regiaoVulneravel())
                || !ventreEmpinado.regiaoPadrao().equals(ventreDeCostas.regiaoPadrao())) {
            throw new IllegalArgumentException("as duas posturas nomeiam regioes diferentes ("
                    + ventreEmpinado.regiaoVulneravel() + "/" + ventreEmpinado.regiaoPadrao()
                    + " contra " + ventreDeCostas.regiaoVulneravel() + "/"
                    + ventreDeCostas.regiaoPadrao() + "): o catalogo so conhece um id, e a postura"
                    + " que usar o outro deixa de pagar sem levantar erro nenhum");
        }
        if (ventreDeCostas.alturaMinima() <= ALTURA_MINIMA_DE_COSTAS_ACEITAVEL) {
            throw new IllegalArgumentException("o ventre de costas exige altura minima acima de "
                    + ALTURA_MINIMA_DE_COSTAS_ACEITAVEL + " e veio " + ventreDeCostas.alturaMinima()
                    + ": com o bicho virado a carapaca ocupa a metade de baixo da caixa, e um"
                    + " limiar ai embaixo passa a pagar critico justamente na superficie que o"
                    + " encontro ensina a nao atacar");
        }
        // De costas NAO ha angulo, e isso e cobrado e nao combinado. Qualquer
        // piso de cosseno acima de -1 obrigaria o jogador a procurar a frente de
        // um bicho de pernas para o ar; ele procuraria, a janela acabaria, e o
        // relato seria "as vezes o ventre paga e as vezes nao".
        if (ventreDeCostas.cossenoMinimo() > -1.0D) {
            throw new IllegalArgumentException("o ventre de costas exige cosseno minimo -1 e veio "
                    + ventreDeCostas.cossenoMinimo() + ": um besouro virado nao tem frente, e"
                    + " cobrar angulo faria a janela queimar enquanto o jogador circula");
        }
        // Empinado, ao contrario, o angulo e a metade da regra: e a frente do
        // bicho que fica exposta. Cosseno <= 0 significaria meia-abertura de 90
        // graus ou mais, ou seja, pagar critico para quem esta no flanco de um
        // bicho que esta olhando para outro lugar.
        if (ventreEmpinado.cossenoMinimo() <= 0.0D) {
            throw new IllegalArgumentException("o ventre empinado exige cosseno minimo positivo e"
                    + " veio " + ventreEmpinado.cossenoMinimo() + ": sem exigir frente, o critico"
                    + " passa a pagar pelo flanco e a licao de ler o telegrafo de frente some");
        }
        if (ticksDeCostas < 1 || ticksParaLevantar < 1) {
            throw new IllegalArgumentException("janela de costas invalida: " + ticksDeCostas
                    + "/" + ticksParaLevantar);
        }
        // O levantar e o AVISO de que a janela acaba, e aviso que ocupa a janela
        // inteira nao e aviso. Com ticksParaLevantar >= ticksDeCostas o besouro
        // comecaria a se endireitar no mesmo tick em que caiu, e o jogador nunca
        // veria a pose de costas -- a janela existiria, pagaria, e seria
        // ilegivel.
        if (ticksParaLevantar >= ticksDeCostas) {
            throw new IllegalArgumentException("o endireitar leva " + ticksParaLevantar
                    + " dos " + ticksDeCostas + " ticks de costas: ele comecaria a se levantar"
                    + " antes de o jogador ver que caiu, e a janela pagaria sem ser legivel");
        }
    }

    /**
     * A UNICA decisao de virar. Ela e pura: quem chama aplica, nao decide.
     *
     * <p>A ordem das perguntas e a ordem da HISTORIA, e nao uma otimizacao: quem
     * ja esta de costas nao vira de novo por nenhum motivo; a investida errada
     * derruba independentemente de alguem ter batido; e so entao o tranco decide,
     * e so na pose que se derruba.</p>
     *
     * @param fase a fase do ataque NO INSTANTE DO EVENTO -- ver o aviso abaixo
     * @param staggerDisparou o acumulador de stagger acabou de estourar o limiar
     * @param investidaFechouSemAcertar a janela ACTIVE terminou naturalmente e
     *        {@code tryHit} nao encostou em ninguem
     * @param jaDeCostas o besouro ja esta virado
     *
     * <p><b>Cuidado com {@code fase}.</b> Ela tem de ser lida ANTES de
     * {@code EnemyRuntime.sofrerStagger}, que reseta o controlador de ataque. Lida
     * depois, ela vale sempre {@link AttackPhase#IDLE}, {@link #VIRA_PELO_TRANCO}
     * nunca acontece e o bicho simplesmente deixa de poder ser derrubado a
     * pancada -- sem excecao, sem log, com a metade do encontro apagada.</p>
     */
    public DecisaoDeViragem decidir(AttackPhase fase, boolean staggerDisparou,
            boolean investidaFechouSemAcertar, boolean jaDeCostas) {
        if (fase == null) throw new IllegalArgumentException("fase de ataque ausente");
        if (jaDeCostas) return DecisaoDeViragem.JA_ESTA_DE_COSTAS;
        if (investidaFechouSemAcertar) return DecisaoDeViragem.VIRA_PELA_INVESTIDA;
        if (!staggerDisparou) return DecisaoDeViragem.SEGUE_DE_PE;
        return fase == AttackPhase.WINDUP
                ? DecisaoDeViragem.VIRA_PELO_TRANCO
                : DecisaoDeViragem.SO_CAMBALEIA;
    }

    /**
     * A regiao atingida, escolhida pela POSTURA e nunca pelo cliente.
     *
     * <p>A postura e DERIVADA aqui, dos dois estados que o servidor ja tem, em
     * vez de guardada num campo proprio. Um campo "empinado" precisaria ser
     * escrito quando o windup comeca e apagado em cada um dos caminhos de saida
     * -- morte, stagger, interrupcao, unload -- e o caminho esquecido deixaria o
     * besouro pagando critico de pe para sempre.</p>
     *
     * @param fase fase do ataque no instante do impacto
     * @param deCostas o besouro esta virado
     * @param alturaRelativa altura do impacto normalizada pela caixa, em [0,1]
     * @param cossenoDeFrente 1 quando o atacante esta bem na frente, -1 atras
     */
    public String regiao(AttackPhase fase, boolean deCostas, double alturaRelativa,
            double cossenoDeFrente) {
        if (fase == null) throw new IllegalArgumentException("fase de ataque ausente");
        if (deCostas) return ventreDeCostas.resolver(alturaRelativa, cossenoDeFrente);
        if (fase == AttackPhase.WINDUP) {
            return ventreEmpinado.resolver(alturaRelativa, cossenoDeFrente);
        }
        // De pe e sem empinar nao ha ponto fraco NENHUM, e o resolver nem e
        // consultado. Consulta-lo aqui pagaria critico por qualquer acerto alto e
        // frontal na carapaca -- que e a superficie de armadura 9, ou seja, o
        // oposto exato do que o bicho ensina.
        return ventreDeCostas.regiaoPadrao();
    }

    /**
     * De cabeca para baixo ele NAO ataca, e esta e a unica fonte dessa frase.
     *
     * <p>A janela de costas so vale alguma coisa porque ela e segura: e o tempo
     * em que o jogador cobra o preco sem pagar nenhum. Um besouro que golpeasse
     * de costas transformaria o premio de ter desviado numa troca de dano, e a
     * unica coisa que mudaria no log seria nada.</p>
     */
    public boolean podeAtacar(boolean deCostas) {
        return !deCostas;
    }
}
