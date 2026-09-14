package com.darkcontinent.nenfoundation.enemy.combat;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Uma SEQUENCIA de golpes encadeados -- a forma de um combo, sem relogio proprio.
 *
 * <p><b>O que ela e, e o que ela nao e.</b> Ela e a DECLARACAO de um combo:
 * quantos golpes, que janelas cada um tem, que alcance cada um reivindica e
 * quantos ticks da recuperacao de um golpe passam antes de o proximo comecar.
 * Ela nao conta tempo, nao sabe que fase esta correndo e nao aplica dano -- quem
 * mede a janela continua sendo o {@link AttackController}, e
 * {@link EstadoDaSequencia} apenas decide QUAL golpe ele deve iniciar em seguida.
 * Um segundo relogio de ataque ao lado do primeiro e o erro mais caro que este
 * arquivo poderia cometer: os dois avancariam um tick por tick, discordariam por
 * um tick depois da primeira interrupcao, e a diferenca e invisivel em teste e
 * visivel na tela.</p>
 *
 * <p><b>As quatro regras que o construtor cobra sao a leitura do combo.</b> Cada
 * uma fecha uma falha que nao levanta excecao em lugar nenhum:</p>
 *
 * <ol>
 *   <li><b>Todo golpe tem telegrafo proprio, e nenhum e curto demais.</b> Uma
 *       sequencia com um aviso de dois ticks no meio nao e lida: o jogador reage
 *       ao golpe, e nao ao aviso, e a resposta vira sorte.</li>
 *   <li><b>O ULTIMO telegrafo e o mais longo.</b> E ele que ensina a esperar. Com
 *       o ultimo igual aos outros, o jogador nao tem como saber que a sequencia
 *       acabou, e passa a punir no meio dela -- exatamente onde nao ha janela.</li>
 *   <li><b>A ULTIMA recuperacao e a mais longa.</b> Ela e a janela de punicao. Se
 *       o ultimo golpe fosse tao seguro quanto os encadeados, "esperar a ultima"
 *       deixaria de pagar, e a licao inteira desapareceria sem nenhum sintoma.</li>
 *   <li><b>O alcance nao encolhe.</b> Com o alcance caindo ao longo da sequencia,
 *       recuar um passo depois do primeiro golpe tira o jogador de todos os
 *       seguintes -- e o telegrafo longo do ultimo, que existe para ser punido de
 *       perto, nunca chega a ser visto.</li>
 * </ol>
 *
 * <p><b>E a quinta, que e de mecanica e nao de leitura:</b> a emenda tem de caber
 * DENTRO da recuperacao de todo golpe que nao seja o ultimo. O
 * {@link AttackController} arma a recarga no instante em que a linha do tempo
 * chega a {@code COMPLETE}; uma emenda maior que a recuperacao faria o encadeado
 * chegar sempre tarde demais, e o mob dispararia UM golpe e ficaria parado o
 * tempo inteiro da recarga. Nao ha erro nisso: ha um combo que nunca acontece.</p>
 *
 * <p>Este record nao conhece mundo, entidade nem Minecraft fora dos dois records
 * de combate. E por isso que ele e testavel sem servidor.</p>
 *
 * @param golpes os golpes na ordem em que saem; o ultimo e o que ensina
 * @param ticksDeEmenda ticks da recuperacao de um golpe antes de o proximo comecar
 */
public record SequenciaDeGolpes(List<GolpeEncadeado> golpes, int ticksDeEmenda) {

    /**
     * Menos de dois golpes nao e sequencia.
     *
     * <p>Limite de DESIGN: um "combo" de um golpe e um ataque comum, e todo o
     * aparato de encadeamento passaria a existir para nada. Pior, ele passaria a
     * existir SEM APARECER -- o mob atacaria normalmente e ninguem notaria que a
     * sequencia sumiu.</p>
     */
    public static final int MINIMO_DE_GOLPES = 2;

    /**
     * Mais de quatro golpes deixa de ser sequencia e vira prisao.
     *
     * <p>Limite de DESIGN com conta atras: cada golpe encadeado ocupa o jogador
     * por windup + janela + emenda. Com quatro golpes curtos e um final longo, a
     * sequencia inteira ja passa de quatro segundos em que ele so pode recuar. O
     * quinto nao quebra nada e nao reprova portao nenhum -- ele so transforma um
     * quebra-cabeca em um stunlock, e o relato que chega e "esse mob nao deixa
     * jogar".</p>
     */
    public static final int MAXIMO_DE_GOLPES = 4;

    /**
     * Telegrafo minimo de qualquer golpe da sequencia, em ticks.
     *
     * <p>Limite de LEITURA: meio segundo e o piso do que um jogador consegue ver,
     * interpretar e responder. Abaixo disso ele esta reagindo ao GOLPE e nao ao
     * aviso, e a diferenca entre acertar e apanhar deixa de ser habilidade. Nao e
     * botao de balanceamento -- girar este numero nao deixa o mob mais forte, deixa
     * o mob ilegivel.</p>
     */
    public static final int WINDUP_MINIMO = 10;

    public SequenciaDeGolpes {
        Objects.requireNonNull(golpes, "sequencia sem lista de golpes");
        if (golpes.size() < MINIMO_DE_GOLPES || golpes.size() > MAXIMO_DE_GOLPES) {
            throw new IllegalArgumentException("a sequencia tem " + golpes.size() + " golpe(s) e o"
                    + " permitido e de " + MINIMO_DE_GOLPES + " a " + MAXIMO_DE_GOLPES + ": menos"
                    + " que o minimo e um ataque comum com aparato de combo em volta, e mais que o"
                    + " maximo e um stunlock que o jogador le como 'esse mob nao deixa jogar'");
        }
        golpes = List.copyOf(golpes);

        Set<String> ids = new HashSet<>();
        for (GolpeEncadeado golpe : golpes) {
            Objects.requireNonNull(golpe, "golpe nulo na sequencia");
            if (!ids.add(golpe.definicao().id())) {
                throw new IllegalArgumentException("dois golpes da sequencia se chamam '"
                        + golpe.definicao().id() + "': o id e o que um relato de bug e uma tela de"
                        + " debug usam para dizer QUAL golpe acertou, e com ids repetidos a"
                        + " pergunta 'qual dos tres me pegou' deixa de ter resposta");
            }
            if (golpe.definicao().windupTicks() < WINDUP_MINIMO) {
                throw new IllegalArgumentException("o golpe '" + golpe.definicao().id() + "' avisa"
                        + " por " + golpe.definicao().windupTicks() + " ticks e o minimo e "
                        + WINDUP_MINIMO + ": abaixo disso o jogador reage ao golpe e nao ao aviso,"
                        + " e a sequencia inteira vira sorte");
            }
        }

        GolpeEncadeado ultimo = golpes.get(golpes.size() - 1);
        for (int i = 0; i < golpes.size() - 1; i++) {
            AttackDefinition anterior = golpes.get(i).definicao();
            if (anterior.windupTicks() >= ultimo.definicao().windupTicks()) {
                throw new IllegalArgumentException("o golpe '" + anterior.id() + "' avisa por "
                        + anterior.windupTicks() + " ticks e o ultimo ('" + ultimo.definicao().id()
                        + "') avisa por " + ultimo.definicao().windupTicks() + ": o telegrafo mais"
                        + " longo tem de ser o do ULTIMO golpe, porque e ele que diz ao jogador que"
                        + " a sequencia acabou. Sem esse degrau ele pune no meio dela, onde nao ha"
                        + " janela -- e nada acusa");
            }
            if (anterior.recoveryTicks() >= ultimo.definicao().recoveryTicks()) {
                throw new IllegalArgumentException("o golpe '" + anterior.id() + "' se recupera em "
                        + anterior.recoveryTicks() + " ticks e o ultimo em "
                        + ultimo.definicao().recoveryTicks() + ": a janela de punicao e a"
                        + " recuperacao do ULTIMO golpe, e se o ultimo for tao seguro quanto os"
                        + " encadeados, esperar por ele deixa de pagar e a licao desaparece");
            }
            if (ticksDeEmenda >= anterior.recoveryTicks()) {
                throw new IllegalArgumentException("a emenda e de " + ticksDeEmenda + " ticks e a"
                        + " recuperacao de '" + anterior.id() + "' dura "
                        + anterior.recoveryTicks() + ": o encadeado chegaria depois de a linha do"
                        + " tempo fechar em COMPLETE, que e quando a recarga e armada. O mob"
                        + " dispararia UM golpe e ficaria parado a recarga inteira -- sem erro"
                        + " nenhum, e com o combo simplesmente nunca acontecendo");
            }
        }

        if (ticksDeEmenda < 1) {
            throw new IllegalArgumentException("a emenda e de " + ticksDeEmenda + " ticks e precisa"
                    + " de pelo menos 1: sem nenhum quadro de recuperacao entre dois golpes, os"
                    + " dois leem como um movimento continuo e o jogador nao consegue contar"
                    + " quantos foram");
        }

        double alcanceAnterior = -1.0D;
        for (GolpeEncadeado golpe : golpes) {
            if (golpe.alcanceEmBlocos() < alcanceAnterior) {
                throw new IllegalArgumentException("o golpe '" + golpe.definicao().id() + "'"
                        + " alcanca " + golpe.alcanceEmBlocos() + " blocos e o anterior ja"
                        + " alcancava " + alcanceAnterior + ": com o alcance encolhendo, recuar um"
                        + " passo depois do primeiro golpe tira o jogador de todos os seguintes, e"
                        + " o telegrafo longo do ultimo nunca chega a ser visto");
            }
            alcanceAnterior = golpe.alcanceEmBlocos();
        }
    }

    public int quantidade() { return golpes.size(); }

    public GolpeEncadeado golpe(int indice) {
        if (indice < 0 || indice >= golpes.size()) {
            throw new IndexOutOfBoundsException("golpe " + indice + " fora da sequencia de "
                    + golpes.size() + ": um indice solto aqui seria um combo que continua depois do"
                    + " fim, e o mob ficaria preso em WINDUP para sempre");
        }
        return golpes.get(indice);
    }

    /** O golpe que ENSINA: telegrafo mais longo, recuperacao mais longa. */
    public GolpeEncadeado ultimo() { return golpes.get(golpes.size() - 1); }

    public boolean ehOUltimo(int indice) { return indice == golpes.size() - 1; }

    /**
     * O alcance que a DECISAO de comecar tem de respeitar: o do PRIMEIRO golpe.
     *
     * <p>Nao e o maior nem a media. Comecar uma sequencia a uma distancia que so o
     * ultimo golpe alcanca faz os primeiros errarem sempre -- e como o mob trava a
     * navegacao durante o combo, ele gasta a sequencia inteira batendo no ar. O
     * sintoma e um oficial que "as vezes nao faz nada".</p>
     */
    public double alcanceParaComecarEmBlocos() { return golpes.get(0).alcanceEmBlocos(); }

    /**
     * Quanto a sequencia inteira tira de vida, se TODOS os golpes acertarem.
     *
     * <p>Ele existe para ser MEDIDO contra o golpe unico da ficha. Um combo cujo
     * total passa do razoavel nao da erro: da um oficial que mata em um encontro e
     * uma sessao de balanceamento inteira procurando o numero errado.</p>
     */
    public float danoTotal() {
        float total = 0.0F;
        for (GolpeEncadeado golpe : golpes) total += golpe.definicao().damage();
        return total;
    }

    /**
     * Ticks que a sequencia inteira ocupa, contando as emendas.
     *
     * <p>Os golpes encadeados gastam windup + janela + EMENDA, porque a
     * recuperacao deles e cortada pelo golpe seguinte; so o ultimo gasta a
     * recuperacao inteira. Contar a recuperacao completa de todos daria um numero
     * maior do que o combo de fato dura, e quem usasse esse numero para comparar
     * com a recarga acharia folga que nao existe.</p>
     */
    public int ticksTotais() {
        int total = 0;
        for (int i = 0; i < golpes.size(); i++) {
            AttackDefinition d = golpes.get(i).definicao();
            total += d.windupTicks() + d.activeTicks();
            total += ehOUltimo(i) ? d.recoveryTicks() : ticksDeEmenda;
        }
        return total;
    }
}
