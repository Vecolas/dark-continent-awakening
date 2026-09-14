package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * Quando o Scorpion Leader usa a pinca e quando usa o ferrao.
 *
 * <p><b>Ela carrega a licao do bicho.</b> O ferrao e o unico ataque que envenena,
 * e ele avisa por mais do que o dobro do tempo da pinca. Para o jogador aprender
 * O QUE evitar, os dois precisam aparecer: um golpe comum frequente, contra o
 * qual o telegrafo longo se destaca. Uma formiga que so ferroa nao ensina nada --
 * o aviso longo vira o unico aviso, e o jogador passa a le-lo como "o ataque
 * dela" em vez de "o caro".</p>
 *
 * <p><b>Por que o ferrao tem alcance MAIOR que a pinca, e por que isso e cobrado
 * no construtor.</b> Invertidos, a formiga sempre entraria na distancia da pinca
 * antes da do ferrao, a pinca dispararia primeiro a cada recarga e a ferroada
 * nunca sairia. Nada levantaria excecao: o veneno continuaria implementado,
 * testado e documentado, e simplesmente nao aconteceria em jogo.</p>
 *
 * <p><b>A guarda do ferrao e o que ESPACA o veneno no tempo.</b> Sem ela, toda
 * recarga terminaria numa ferroada, o acumulo bateria no teto e ficaria la, e a
 * decisao do jogador -- recuar e deixar o veneno escorrer -- deixaria de existir.</p>
 *
 * <p>Ela nao consulta mundo: distancia, visao, cambaleio e recarga chegam ja
 * medidos pelo servidor.</p>
 *
 * @param alcanceDaPinca distancia de centro a centro em que o golpe curto vale
 * @param alcanceDoFerrao distancia de centro a centro em que a ferroada vale
 * @param ticksDeGuardaDoFerrao intervalo minimo entre duas ferroadas
 */
public record RegrasDeFerrao(double alcanceDaPinca, double alcanceDoFerrao,
        int ticksDeGuardaDoFerrao) {

    public RegrasDeFerrao {
        if (!Double.isFinite(alcanceDaPinca) || alcanceDaPinca <= 0.0D
                || !Double.isFinite(alcanceDoFerrao) || alcanceDoFerrao <= 0.0D) {
            throw new IllegalArgumentException("alcance invalido: pinca=" + alcanceDaPinca
                    + " ferrao=" + alcanceDoFerrao + ". Alcance zero ou negativo produz um mob"
                    + " que nunca decide atacar e fica encarando o alvo -- sem erro nenhum.");
        }
        if (alcanceDoFerrao <= alcanceDaPinca) {
            throw new IllegalArgumentException("o ferrao alcanca " + alcanceDoFerrao + " e a pinca"
                    + " alcanca " + alcanceDaPinca + ": a formiga entraria na distancia da pinca"
                    + " antes da do ferrao, a pinca dispararia primeiro a cada recarga e a"
                    + " ferroada NUNCA sairia. O veneno continuaria implementado, testado e"
                    + " documentado, e simplesmente nao aconteceria em jogo.");
        }
        if (ticksDeGuardaDoFerrao < 1) {
            throw new IllegalArgumentException("guarda do ferrao invalida: "
                    + ticksDeGuardaDoFerrao + ". Sem intervalo entre ferroadas, toda recarga"
                    + " terminaria em veneno, o acumulo ficaria colado no teto e o golpe comum"
                    + " -- que e o que da ao jogador com o que comparar o telegrafo longo --"
                    + " nunca apareceria.");
        }
    }

    /**
     * A UNICA escolha de ataque. Pura: quem chama executa, nao decide.
     *
     * <p>A ordem das perguntas nao e arbitraria. O cambaleio vem primeiro porque
     * ele e o unico estado que tambem manda PARAR o caminho; depois vem o que
     * torna qualquer ataque impossivel (sem alvo, em recarga); so entao a postura,
     * que e uma escolha e nao um impedimento; e por ultimo a distancia, que e a
     * unica pergunta que separa os dois golpes.</p>
     *
     * @param distancia centro a centro, ja medida pelo servidor
     * @param alvoVisivel ha alvo vivo e com linha de visao
     * @param cambaleando o stagger esta em curso
     * @param recargaPronta o {@code AttackController} aceita comecar um golpe
     * @param ticksDesdeAUltimaFerroada contador proprio da formiga; grande no
     *        comeco da vida dela, para que a primeira ferroada nao espere
     * @param guardaFechada a postura traduzida da intencao de Nen
     */
    public DecisaoDeFerrao decidir(double distancia, boolean alvoVisivel, boolean cambaleando,
            boolean recargaPronta, int ticksDesdeAUltimaFerroada, boolean guardaFechada) {
        if (!Double.isFinite(distancia) || distancia < 0.0D) {
            throw new IllegalArgumentException("distancia invalida: " + distancia);
        }
        if (ticksDesdeAUltimaFerroada < 0) {
            throw new IllegalArgumentException("contador de ferroada negativo: "
                    + ticksDesdeAUltimaFerroada);
        }
        if (cambaleando) return DecisaoDeFerrao.CAMBALEANDO;
        if (!alvoVisivel || !recargaPronta) return DecisaoDeFerrao.AGUARDAR;
        if (guardaFechada) return DecisaoDeFerrao.SEGURAR_A_GUARDA;
        if (distancia > alcanceDoFerrao) return DecisaoDeFerrao.AGUARDAR;
        if (ticksDesdeAUltimaFerroada >= ticksDeGuardaDoFerrao) {
            return DecisaoDeFerrao.ARMAR_O_FERRAO;
        }
        return distancia <= alcanceDaPinca
                ? DecisaoDeFerrao.GOLPEAR_COM_PINCA : DecisaoDeFerrao.AGUARDAR;
    }
}
