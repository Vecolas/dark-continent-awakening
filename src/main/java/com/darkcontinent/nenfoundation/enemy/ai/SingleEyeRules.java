package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * Regras de uma criatura de UM olho: o mesmo orgao e o sensor e o ponto fraco.
 *
 * <p><b>A decisao que este arquivo carrega:</b> num bicho de um olho so, "onde
 * ele enxerga" e "onde ele apanha" nao sao dois assuntos. Sao o mesmo cone, e a
 * relacao entre os dois e a licao do encontro inteiro -- o lugar que paga
 * critico e exatamente o lugar de onde ele te ve chegar. Circular por fora
 * funciona, correr de frente nao, e as duas metades dessa frase saem daqui.</p>
 *
 * <p><b>Por que as duas aberturas moram no MESMO record.</b> Separadas -- o cone
 * de visao num lugar e o cosseno do ponto fraco no outro -- nada impediria o
 * arco do olho de ficar MAIS LARGO que o arco da visao. Isso nao daria erro
 * nenhum: daria um gigante que paga critico a quem ele nao consegue ver, ou
 * seja, dano de graca sem risco nenhum, e o encontro inteiro se apagaria sem uma
 * linha de log. Juntas, o construtor cobra a continencia.</p>
 *
 * <p>Este record nao conhece mundo, entidade nem Minecraft: ele recebe numeros ja
 * medidos pelo servidor e devolve decisoes. Quem constroi o {@code VisionCone} e
 * o {@code WeakPointResolver} a partir daqui e o perfil da criatura -- assim os
 * dois nascem do mesmo par de numeros em vez de serem escritos duas vezes.</p>
 *
 * @param cossenoDoCampoDeVisao cosseno da meia-abertura do que ele ENXERGA
 * @param cossenoDoOlho cosseno da meia-abertura em que o olho fica EXPOSTO
 * @param alturaMinimaDoOlho altura relativa, em [0,1], a partir da qual o
 *        impacto conta como olho
 */
public record SingleEyeRules(double cossenoDoCampoDeVisao, double cossenoDoOlho,
        double alturaMinimaDoOlho) {

    /**
     * Altura relativa abaixo da qual "olho" deixa de significar cabeca.
     *
     * <p>Limite de DESIGN, e nao botao de balanceamento: um olho colocado na
     * metade de baixo do corpo nao e mais um olho, e o modelo desenhado passaria
     * a discordar da regra sem que nada acusasse. Quem quiser um ponto fraco
     * baixo -- a barriga do besouro, por exemplo -- nao usa este record.</p>
     */
    public static final double ALTURA_MINIMA_ACEITAVEL = 0.5D;

    /**
     * Meia-abertura maxima que um bicho de um olho so pode ter.
     *
     * <p>Limite de DESIGN com conta atras: 85 graus para cada lado deixam um
     * flanco cego de 10 graus no total. Um alvo de 0.6 bloco de largura so cabe
     * em 10 graus a partir de uns 3.4 blocos de distancia -- ou seja, encostado
     * no bicho o ponto cego e mais estreito que o proprio jogador, e "circular"
     * deixa de ser possivel. O mob continua nascendo, atacando e passando em todo
     * portao; o que some e a unica coisa que ele ensina.</p>
     */
    public static final double MEIA_ABERTURA_MAXIMA_EM_GRAUS = 85.0D;

    /** O piso correspondente, em cosseno -- derivado, para nao haver dois numeros. */
    private static final double COSSENO_MINIMO_DO_CAMPO =
            Math.cos(Math.toRadians(MEIA_ABERTURA_MAXIMA_EM_GRAUS));

    public SingleEyeRules {
        if (!Double.isFinite(cossenoDoCampoDeVisao) || !Double.isFinite(cossenoDoOlho)
                || !Double.isFinite(alturaMinimaDoOlho)) {
            throw new IllegalArgumentException("regras de olho unico com numero nao finito");
        }
        // Cone largo demais e cone sem flanco cego utilizavel. O bicho continua
        // funcionando, continua verde em todo portao, e perde a UNICA coisa que
        // ele ensina -- ninguem descobre isso sem jogar.
        if (cossenoDoCampoDeVisao < COSSENO_MINIMO_DO_CAMPO || cossenoDoCampoDeVisao >= 1.0D) {
            throw new IllegalArgumentException("campo de visao de um olho so tem de ter"
                    + " meia-abertura entre 0 e " + MEIA_ABERTURA_MAXIMA_EM_GRAUS + " graus (cosseno"
                    + " em [" + COSSENO_MINIMO_DO_CAMPO + ", 1)), e veio " + cossenoDoCampoDeVisao
                    + ": sem flanco cego em que um jogador CAIBA, o encontro inteiro deixa de"
                    + " ensinar o que foi projetado");
        }
        if (cossenoDoOlho > 1.0D) {
            throw new IllegalArgumentException("cosseno do olho invalido: " + cossenoDoOlho);
        }
        // A CONTINENCIA. Cosseno maior e arco mais estreito: o olho tem de ser
        // um alvo mais dificil do que simplesmente estar no campo de visao.
        if (cossenoDoOlho <= cossenoDoCampoDeVisao) {
            throw new IllegalArgumentException("o arco do olho (cos " + cossenoDoOlho + ") tem de"
                    + " ser mais ESTREITO que o campo de visao (cos " + cossenoDoCampoDeVisao
                    + "): mais largo, o bicho paga critico a quem ele nao enxerga, e isso e dano de"
                    + " graca sem risco -- sem erro, sem log, e com o encontro apagado");
        }
        if (alturaMinimaDoOlho <= ALTURA_MINIMA_ACEITAVEL || alturaMinimaDoOlho >= 1.0D) {
            throw new IllegalArgumentException("altura do olho fora de ("
                    + ALTURA_MINIMA_ACEITAVEL + ", 1): " + alturaMinimaDoOlho
                    + " -- olho na metade de baixo do corpo nao e cabeca, e o modelo desenhado"
                    + " passaria a discordar da regra em silencio");
        }
    }

    /** Constroi a partir das meias-aberturas em GRAUS, que e como um humano pensa. */
    public static SingleEyeRules deGraus(double meiaAberturaDaVisao, double meiaAberturaDoOlho,
            double alturaMinimaDoOlho) {
        if (!Double.isFinite(meiaAberturaDaVisao) || !Double.isFinite(meiaAberturaDoOlho)
                || meiaAberturaDaVisao <= 0.0D || meiaAberturaDoOlho <= 0.0D) {
            throw new IllegalArgumentException("meia-abertura invalida");
        }
        return new SingleEyeRules(Math.cos(Math.toRadians(meiaAberturaDaVisao)),
                Math.cos(Math.toRadians(meiaAberturaDoOlho)), alturaMinimaDoOlho);
    }

    /**
     * Ele ENXERGA quem esta dentro do cone.
     *
     * <p>Geometria nao finita nao enxerga, e nao o contrario: um NaN vindo de um
     * alvo em estado estranho nao pode acender a percepcao de graca. O lado
     * seguro de uma medida invalida e o bicho continuar sem ver.</p>
     *
     * @param cossenoDoOlhar 1 quando o alvo esta bem de frente, -1 quando esta atras
     */
    public boolean enxerga(double cossenoDoOlhar) {
        if (!Double.isFinite(cossenoDoOlhar)) return false;
        return cossenoDoOlhar >= cossenoDoCampoDeVisao;
    }

    /**
     * O complemento ESTRITO de {@link #enxerga}, e por isso ele existe.
     *
     * <p>Quem precisa do ponto cego -- a Goal que recusa o golpe, o teste que
     * prova a consequencia -- poderia escrever {@code cosseno < limite} no lugar
     * dela. Duas comparacoes independentes divergem no dia em que o limite virar
     * exclusivo de um lado, e a divergencia e uma fresta de um grau em que o
     * bicho nao ve o alvo mas ataca assim mesmo. Nao da erro; da um mob que
     * "as vezes" acerta de costas.</p>
     */
    public boolean noPontoCego(double cossenoDoOlhar) {
        return !enxerga(cossenoDoOlhar);
    }

    /** Meia-abertura do campo de visao, em graus -- para relato e para diagnostico. */
    public double meiaAberturaDaVisaoEmGraus() {
        return Math.toDegrees(Math.acos(cossenoDoCampoDeVisao));
    }

    /** Meia-abertura em que o olho fica exposto, em graus. */
    public double meiaAberturaDoOlhoEmGraus() {
        return Math.toDegrees(Math.acos(cossenoDoOlho));
    }
}
