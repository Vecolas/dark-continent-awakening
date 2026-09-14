package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * A memoria de RASTRO: ate quando vale correr atras de quem sumiu.
 *
 * <p><b>Onde termina a {@code ThreatMemory} e onde comeca este arquivo.</b> A
 * memoria de ameaca responde <i>de quem</i> o bicho ainda se lembra; ela guarda
 * um uuid e um prazo, e e da percepcao. Aqui mora outra pergunta, que e de
 * caca: <i>ainda vale ir ate la?</i> As duas sao prazos e por isso parecem a
 * mesma coisa -- e e exatamente por parecerem que este record RECEBE o prazo da
 * memoria de ameaca em vez de supor um. Ver a cobranca no construtor.</p>
 *
 * <p><b>O prazo e o que separa "tenso" de "injusto".</b> Curto demais, fugir
 * resolve o encontro: bastaria dobrar uma esquina e o bicho esqueceria no mesmo
 * tick. Longo demais, o mob atravessa o mapa atras de uma memoria, nunca volta
 * para casa e nunca desengaja -- e isso nao aparece como erro, aparece como um
 * jogador dizendo que o mob "nao larga" e como TPS caindo devagar numa colonia
 * inteira perseguindo fantasmas.</p>
 *
 * <p><b>Chegar ao fim do rastro tambem encerra.</b> Sem essa segunda saida, o
 * bicho que chega ao ponto e nao acha ninguem fica parado ali ate o prazo
 * vencer, olhando para o chao. Ele nao trava, nao da erro, e o jogador ve um
 * inimigo que "bugou".</p>
 *
 * <p><b>Este record nao conhece mundo, entidade nem Minecraft.</b> Ele recebe
 * numeros ja medidos pelo servidor e devolve decisoes -- e e o que permite
 * provar em teste unitario que dentro do prazo ele vai atras e que depois do
 * prazo ele desiste, sem abrir um jogo.</p>
 *
 * @param ticksDeRastro quanto tempo o rastro continua valendo apos o ultimo
 *        contato
 * @param raioDeChegada a que distancia do ponto do rastro o bicho considera que
 *        chegou, em blocos
 * @param ticksDeMemoriaDoAlvo o prazo da {@code ThreatMemory} do mesmo mob,
 *        recebido e nao copiado: e contra ele que o rastro e cobrado
 */
public record RegrasDeRastro(int ticksDeRastro, double raioDeChegada, int ticksDeMemoriaDoAlvo) {

    public RegrasDeRastro {
        if (ticksDeRastro < 1) {
            throw new IllegalArgumentException("rastro de " + ticksDeRastro + " ticks: um rastro"
                    + " que nasce vencido faz o bicho desistir no mesmo tick em que perde o alvo"
                    + " de vista, e fugir dele passa a ser dobrar uma esquina -- o encontro"
                    + " inteiro some sem que nada reprove");
        }
        if (ticksDeMemoriaDoAlvo < 1) {
            throw new IllegalArgumentException("memoria de alvo de " + ticksDeMemoriaDoAlvo
                    + " ticks: sem memoria nenhuma nao ha de quem seguir rastro");
        }
        if (ticksDeRastro > ticksDeMemoriaDoAlvo) {
            throw new IllegalArgumentException("o rastro dura " + ticksDeRastro + " ticks e a"
                    + " memoria da ameaca dura " + ticksDeMemoriaDoAlvo + ": o bicho seguiria o"
                    + " rastro de alguem de quem ele ja esqueceu. Nao da erro -- da um mob"
                    + " correndo em linha reta ate um ponto vazio, sem alvo, e parando la sem"
                    + " motivo visivel");
        }
        if (!Double.isFinite(raioDeChegada) || raioDeChegada <= 0.0D) {
            throw new IllegalArgumentException("raio de chegada invalido: " + raioDeChegada
                    + ". Com zero, o bicho nunca 'chega' ao ponto do rastro -- a navegacao para a"
                    + " poucos centimetros dele e ele fica parado ali ate o prazo vencer");
        }
    }

    /** O prazo venceu? E a pergunta que {@link DecisaoDeRastro#DESISTIR} responde. */
    public boolean expirou(int ticksDesdeOContato) {
        return exigirTicks(ticksDesdeOContato) >= ticksDeRastro;
    }

    /**
     * O que fazer neste tick.
     *
     * <p>A ordem das perguntas e a regra, e nenhuma delas e trocavel:</p>
     *
     * <ol>
     *   <li><b>estou vendo</b> -- vencendo tudo. Seguir rastro com o alvo a
     *       vista faria o bicho correr para onde o alvo ESTAVA, e ele nunca
     *       alcancaria ninguem que anda em linha reta;</li>
     *   <li><b>o prazo venceu</b> -- antes de qualquer coisa sobre distancia.
     *       Perguntar a distancia primeiro deixaria um rastro vencido ainda
     *       mandar o bicho andar mais um passo, e um passo por tick e uma
     *       perseguicao que nunca termina;</li>
     *   <li><b>cheguei e nao ha ninguem</b> -- o rastro acabou. Sem esta saida o
     *       bicho fica parado sobre o ponto ate o prazo vencer.</li>
     * </ol>
     *
     * @param alvoVisivel o servidor VE o alvo agora
     * @param ticksDesdeOContato quantos ticks desde o ultimo contato; zero
     *        enquanto ha contato
     * @param distanciaAteORastro do bicho ate o ponto do rastro, em blocos
     */
    public DecisaoDeRastro decidir(boolean alvoVisivel, int ticksDesdeOContato,
            double distanciaAteORastro) {
        exigirTicks(ticksDesdeOContato);
        if (!Double.isFinite(distanciaAteORastro) || distanciaAteORastro < 0.0D) {
            throw new IllegalArgumentException("distancia ate o rastro invalida: "
                    + distanciaAteORastro + ". NaN aqui nao pode virar 'segue': uma medida"
                    + " quebrada mandaria o bicho perseguir um ponto que nao existe, e a unica"
                    + " pista disso seria um mob andando para o nada");
        }
        if (alvoVisivel) return DecisaoDeRastro.PERSEGUIR_A_VISTA;
        if (expirou(ticksDesdeOContato)) return DecisaoDeRastro.DESISTIR;
        if (distanciaAteORastro <= raioDeChegada) return DecisaoDeRastro.DESISTIR;
        return DecisaoDeRastro.SEGUIR_O_RASTRO;
    }

    private int exigirTicks(int ticksDesdeOContato) {
        if (ticksDesdeOContato < 0) {
            throw new IllegalArgumentException("ticks desde o contato negativo: "
                    + ticksDesdeOContato + ". Negativo aqui seria um rastro do futuro, e ele"
                    + " nunca expiraria");
        }
        return ticksDesdeOContato;
    }
}
