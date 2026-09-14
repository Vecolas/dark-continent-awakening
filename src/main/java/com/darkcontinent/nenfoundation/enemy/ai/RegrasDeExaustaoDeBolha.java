package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * Quando o Bubble Horse exaure, e quando a janela de captura fecha.
 *
 * <p>Carrega a mecanica inteira do bicho: <b>ele so vale VIVO.</b> A condicao de
 * card dele e {@code CaptureCondition.porEnfraquecimento()} -- um quarto de vida
 * e <em>nao pode morrer</em> --, e a janela que este record mede e o intervalo em
 * que essa condicao pode ser colhida. Quem continuar batendo dentro dela mata o
 * premio.</p>
 *
 * <p><b>Por que a janela FECHA.</b> Uma janela aberta para sempre seria mais
 * simples e apagaria o mob: o jogador acharia o cavalo parado a qualquer momento
 * depois da briga, e a decisao de parar de bater na hora certa -- que e a unica
 * coisa que este bicho ensina -- nunca precisaria ser tomada. O prazo e o que
 * transforma a captura numa escolha.</p>
 *
 * <p><b>Por que existe FOLEGO depois que ela fecha.</b> A vida nao sobe quando a
 * janela expira: ela continua abaixo do limiar. Sem uma carencia, o proximo tick
 * reabriria a janela imediatamente e o cavalo nunca voltaria a fugir. Isso nao
 * levanta excecao nenhuma -- e um mob permanentemente parado, com a fase de
 * ataque correta, o cooldown correto e o log limpo.</p>
 *
 * <p><b>A fracao NAO e um numero proprio deste record em producao.</b> Quem a
 * fornece e {@code BubbleHorseTuning}, lendo-a de
 * {@code GreedIslandProfiles.capturas()}. Repetida aqui, ela seria a segunda
 * fonte para a mesma verdade: alguem ajustaria a condicao de card e a janela
 * passaria a abrir num ponto que nao paga card nenhum -- o jogador veria o cavalo
 * parar, pararia de bater, e nao receberia nada.</p>
 *
 * <p>Ela nao consulta mundo: vida, morte e contadores chegam ja medidos pelo
 * servidor.</p>
 *
 * @param fracaoDeVidaDoColapso fracao da vida maxima em que ou abaixo da qual ele
 *        para de fugir
 * @param ticksDaJanela quanto tempo a janela de captura fica aberta
 * @param ticksDeFolego carencia antes de ele poder exaurir de novo
 */
public record RegrasDeExaustaoDeBolha(double fracaoDeVidaDoColapso, int ticksDaJanela,
        int ticksDeFolego) {

    public RegrasDeExaustaoDeBolha {
        if (!Double.isFinite(fracaoDeVidaDoColapso) || fracaoDeVidaDoColapso <= 0.0D) {
            throw new IllegalArgumentException("fracao de colapso invalida: "
                    + fracaoDeVidaDoColapso + ". Com zero ou menos ele so pararia depois de morto,"
                    + " e a janela de captura nunca existiria.");
        }
        // Fracao 1 e um cavalo que nasce exausto: ele nunca foge, nunca salta e o
        // encontro inteiro vira um bicho parado no mato. Nao da erro, e o portao
        // de perfil continua verde -- a ficha (HP, dano, velocidade) nao mudou.
        if (fracaoDeVidaDoColapso >= 1.0D) {
            throw new IllegalArgumentException("fracao de colapso " + fracaoDeVidaDoColapso
                    + ": o bicho nasceria exausto, nunca fugiria e o encontro viraria um cavalo"
                    + " parado -- com a ficha inteira ainda parecendo certa.");
        }
        if (ticksDaJanela < 1) {
            throw new IllegalArgumentException("ticks de janela invalidos: " + ticksDaJanela
                    + ". Janela de zero tick e uma captura que nunca da para pegar, e o jogador"
                    + " culparia a propria mira por um card que o servidor nunca oferece.");
        }
        if (ticksDeFolego < 1) {
            throw new IllegalArgumentException("ticks de folego invalidos: " + ticksDeFolego
                    + ". A vida continua abaixo do limiar depois que a janela fecha: sem carencia"
                    + " ela reabre no tick seguinte, e o cavalo nunca volta a fugir.");
        }
    }

    /**
     * A UNICA decisao sobre a janela. Ela e pura: quem chama aplica, nao decide.
     *
     * <p>A ordem das perguntas e a ordem da HISTORIA. Morte vem primeiro porque
     * nenhuma outra pergunta importa depois dela -- um cadaver com a janela aberta
     * ainda e um card perdido. Depois vem o estado em que ele ja esta (a janela
     * corre), depois a carencia, e so entao a vida.</p>
     *
     * @param vidaAtual vida APOS o dano ter sido aplicado; ler a vida de antes
     *        adiaria o colapso por um golpe inteiro, e esse golpe inteiro e
     *        exatamente o que mata um bicho de 30 de vida
     * @param vidaMaxima vida maxima da entidade
     * @param morreu se o bicho chegou a morrer
     * @param exausto o estado publicado agora
     * @param ticksNaJanela ticks decorridos desde que a janela abriu
     * @param folegoRestante ticks de carencia que ainda faltam
     */
    public DecisaoDeExaustaoDeBolha decidir(float vidaAtual, float vidaMaxima, boolean morreu,
            boolean exausto, int ticksNaJanela, int folegoRestante) {
        if (!Float.isFinite(vidaMaxima) || vidaMaxima <= 0.0F || !Float.isFinite(vidaAtual)) {
            throw new IllegalArgumentException("vida invalida para decidir exaustao");
        }
        if (ticksNaJanela < 0 || folegoRestante < 0) {
            throw new IllegalArgumentException("contador de exaustao negativo");
        }
        if (morreu) return DecisaoDeExaustaoDeBolha.MORREU_SEM_CARD;
        if (exausto) {
            return ticksNaJanela >= ticksDaJanela
                    ? DecisaoDeExaustaoDeBolha.JANELA_FECHOU
                    : DecisaoDeExaustaoDeBolha.JANELA_ABERTA;
        }
        if (folegoRestante > 0) return DecisaoDeExaustaoDeBolha.RECUPERANDO_O_FOLEGO;
        if (vidaAtual > vidaMaxima * fracaoDeVidaDoColapso) {
            return DecisaoDeExaustaoDeBolha.ACIMA_DO_LIMIAR;
        }
        return DecisaoDeExaustaoDeBolha.EXAURE;
    }
}
