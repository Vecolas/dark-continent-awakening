package com.darkcontinent.nenfoundation.enemy.ai;

import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadOrder;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * As regras do LOBO em cima da ordem que o BANDO deu -- e so isso.
 *
 * <p><b>Onde termina o Squad e onde comeca este arquivo.</b> {@code SquadController}
 * decide o que vale para o bando INTEIRO: qual e o alvo, se o bando recua, se ele
 * reagrupa. Aqui mora o que e do lobo: a que distancia ele morde, onde ele se
 * posta no anel, e a unica regra que o bando nao tem como saber -- <b>um lobo
 * sozinho nao avanca</b>.</p>
 *
 * <p>Essa divisao nao e cerimonia. Escrever "sozinho ele recua" dentro do
 * {@code SquadController} faria TODA familia que usar squad herdar a covardia de
 * um bicho de HP 26; escrever o alvo compartilhado aqui faria cada lobo escolher
 * o proprio, e o cerco nunca fecharia. Os dois erros dao o mesmo sintoma: um
 * bando que se comporta como uma multidao, sem uma linha de log.</p>
 *
 * <p><b>Este record nao conhece mundo, entidade nem Minecraft.</b> Ele recebe
 * numeros ja medidos pelo servidor e devolve decisoes. E o que permite provar em
 * teste unitario a promocao no mesmo tick, o teto de reforco e o cerco que nao
 * empilha -- coisas que so um gametest veria se elas morassem na entidade.</p>
 *
 * @param bando as regras do grupo, de onde saem o teto, o espacamento, a moral
 *        minima e o raio de reforco. Recebidas, e nao copiadas: repetir o teto
 *        aqui criaria dois numeros para a mesma coisa, e girar um deles numa
 *        sessao de balanceamento deixaria o outro mandando
 * @param membrosParaAvancar quantos precisam estar vivos para o lobo avancar
 * @param distanciaDeInvestida distancia (centro a centro) em que ele decide morder
 * @param raioDoCerco raio do anel em que os membros que nao investem se postam
 */
public record RegrasDeMatilha(SquadRules bando, int membrosParaAvancar,
        double distanciaDeInvestida, double raioDoCerco) {

    /**
     * A escada de papeis de quem ENTRA, na ordem em que os postos sao ocupados.
     *
     * <p>{@code LEADER} nao esta aqui de proposito: {@code Squad.entrar} recusa
     * quem tenta entrar como lider, porque lider se promove e nao se declara --
     * dois lideres dariam duas ordens ao mesmo bando.</p>
     *
     * <p>A ordem importa e e fixa: ela decide qual posto do anel e ocupado
     * primeiro quando o bando cresce. Um sorteio faria dois servidores com o
     * mesmo save montarem cercos diferentes, e a divergencia so apareceria numa
     * investigacao de dessincronia.</p>
     */
    private static final List<SquadRole> PAPEIS_DE_ENTRADA =
            List.of(SquadRole.FRONTLINER, SquadRole.FLANKER, SquadRole.SCOUT,
                    SquadRole.RANGED, SquadRole.SUPPORT);

    /**
     * O posto de cada papel no anel, em graus a partir do OLHAR DO ALVO.
     *
     * <p>Relativo ao alvo, e nao ao mundo: postos em angulo de mundo poriam o
     * lider sempre ao norte do jogador, e o cerco deixaria de reagir a quem
     * gira. Zero e de frente para a cara do alvo; 180 e pelas costas dele.</p>
     *
     * <p>Um papel por posto, e e isso que faz "espacamento respeitado" ser
     * verdade: dois membros com o mesmo papel mirariam o mesmo ponto do anel, e o
     * cerco poria lobo dentro de lobo sem nada acusar. O construtor cobra que
     * haja papel distinto para todo membro que o teto permite.</p>
     */
    private static final Map<SquadRole, Double> ANGULO_DO_PAPEL = anguloDoPapel();

    private static Map<SquadRole, Double> anguloDoPapel() {
        Map<SquadRole, Double> mapa = new EnumMap<>(SquadRole.class);
        mapa.put(SquadRole.LEADER, 0.0D);        // encara o alvo: e quem investe primeiro
        mapa.put(SquadRole.FRONTLINER, 180.0D);  // fecha por tras: o segundo a investir
        mapa.put(SquadRole.FLANKER, 90.0D);      // flanco
        mapa.put(SquadRole.SCOUT, 270.0D);       // o outro flanco
        mapa.put(SquadRole.RANGED, 135.0D);      // so aparece em bando maior que quatro
        mapa.put(SquadRole.SUPPORT, 225.0D);
        return Map.copyOf(mapa);
    }

    /**
     * Quem se compromete com a mordida.
     *
     * <p>DOIS papeis, nunca todos. Se os quatro investissem juntos, o cerco
     * viraria uma trituradora: o jogador levaria quatro janelas ACTIVE no mesmo
     * instante, e a recuperacao longa -- que e o que espaca os golpes no tempo --
     * deixaria de valer para qualquer coisa. Isso nao da erro: da um encontro que
     * mata em um segundo e que ninguem consegue explicar sem medir.</p>
     */
    public static boolean papelInveste(SquadRole papel) {
        Objects.requireNonNull(papel, "papel ausente");
        return papel == SquadRole.LEADER || papel == SquadRole.FRONTLINER;
    }

    public RegrasDeMatilha {
        Objects.requireNonNull(bando, "regras de bando ausentes");
        if (membrosParaAvancar < 2) {
            throw new IllegalArgumentException("membrosParaAvancar = " + membrosParaAvancar
                    + ": um lobo que avanca sozinho apaga a ficha do bicho. HP 26 e dano 6 so"
                    + " fazem sentido em bando, e um lobo solitario agressivo morre de graca sem"
                    + " que nenhum portao reclame");
        }
        if (membrosParaAvancar > bando.maximoDeMembros()) {
            throw new IllegalArgumentException("membrosParaAvancar = " + membrosParaAvancar
                    + " e o teto do bando e " + bando.maximoDeMembros() + ": o bando nunca alcanca"
                    + " o numero que autoriza avancar, entao o lobo recua para sempre. Ele nasce,"
                    + " anda, foge e passa em tudo -- o unico sinal e um inimigo que nunca ataca");
        }
        if (!Double.isFinite(distanciaDeInvestida) || distanciaDeInvestida <= 0.0D
                || !Double.isFinite(raioDoCerco) || raioDoCerco <= 0.0D) {
            throw new IllegalArgumentException("distancias de matilha invalidas");
        }
        if (raioDoCerco <= distanciaDeInvestida) {
            throw new IllegalArgumentException("o raio do cerco (" + raioDoCerco + ") tem de ser"
                    + " MAIOR que a distancia de investida (" + distanciaDeInvestida + "): iguais,"
                    + " 'segurar o anel' e 'morder' acontecem no mesmo lugar, os quatro lobos"
                    + " colam na cara do alvo e o cerco vira a pilha que o espacamento existe para"
                    + " impedir");
        }
        int postosNecessarios = bando.maximoDeMembros() - 1;
        if (postosNecessarios > PAPEIS_DE_ENTRADA.size()) {
            throw new IllegalArgumentException("o teto do bando e " + bando.maximoDeMembros()
                    + " e so ha " + PAPEIS_DE_ENTRADA.size() + " papeis de entrada: dois membros"
                    + " receberiam o mesmo papel, mirariam o MESMO posto do anel e o cerco poria"
                    + " lobo dentro de lobo. Nao da erro -- da um cerco que o jogador le como"
                    + " travamento");
        }
        double abertura = menorAberturaEmGraus(bando.maximoDeMembros());
        double corda = 2.0D * raioDoCerco * Math.sin(Math.toRadians(abertura / 2.0D));
        if (corda < bando.espacamento()) {
            throw new IllegalArgumentException("com raio " + raioDoCerco + " e " + abertura
                    + " graus entre postos vizinhos, dois membros ficam a " + corda + " blocos um"
                    + " do outro, e o espacamento exigido e " + bando.espacamento() + ": o anel e"
                    + " apertado demais para o bando cheio, e 'espacamento respeitado' passa a ser"
                    + " uma frase falsa -- sem erro nenhum, so com bichos se empurrando");
        }
    }

    /**
     * O menor angulo entre dois postos vizinhos num bando CHEIO, em graus.
     *
     * <p>Medido sobre os papeis que um bando cheio de fato ocupa -- lider mais os
     * primeiros da escada de entrada -- e nao sobre os seis do mapa. Medir os seis
     * aprovaria um anel que so aperta quando o bando enche, que e exatamente
     * quando ele importa.</p>
     */
    private static double menorAberturaEmGraus(int maximoDeMembros) {
        Set<Double> angulos = new LinkedHashSet<>();
        angulos.add(ANGULO_DO_PAPEL.get(SquadRole.LEADER));
        for (int i = 0; i < maximoDeMembros - 1 && i < PAPEIS_DE_ENTRADA.size(); i++) {
            angulos.add(ANGULO_DO_PAPEL.get(PAPEIS_DE_ENTRADA.get(i)));
        }
        List<Double> ordenados = angulos.stream().sorted().toList();
        if (ordenados.size() < 2) return 360.0D;
        double menor = 360.0D - (ordenados.get(ordenados.size() - 1) - ordenados.get(0));
        for (int i = 1; i < ordenados.size(); i++) {
            menor = Math.min(menor, ordenados.get(i) - ordenados.get(i - 1));
        }
        return menor;
    }

    /** O posto deste papel no anel, em graus a partir do olhar do alvo. */
    public double anguloDeCercoEmGraus(SquadRole papel) {
        Objects.requireNonNull(papel, "papel ausente");
        Double angulo = ANGULO_DO_PAPEL.get(papel);
        if (angulo == null) {
            throw new IllegalArgumentException("o papel " + papel + " nao tem posto no anel: um"
                    + " membro sem posto iria para o centro, encostaria em quem ja esta la e o"
                    + " cerco deixaria de ter forma");
        }
        return angulo;
    }

    /**
     * O primeiro papel da escada que ainda esta VAGO no bando.
     *
     * <p>Vago, e nao "o proximo indice". Indice seria o tamanho do bando no
     * momento da entrada, e bastaria um membro do meio morrer para o proximo a
     * entrar receber um papel que outro ja tem -- dois lobos no mesmo posto do
     * anel, sem erro nenhum.</p>
     *
     * @param ocupados os papeis que o bando ja tem, normalmente {@code Squad.membros().values()}
     * @return vazio quando todos os postos estao tomados
     */
    public Optional<SquadRole> papelVago(Collection<SquadRole> ocupados) {
        Objects.requireNonNull(ocupados, "papeis ocupados ausentes");
        for (SquadRole papel : PAPEIS_DE_ENTRADA) {
            if (!ocupados.contains(papel)) return Optional.of(papel);
        }
        return Optional.empty();
    }

    /**
     * O que este lobo faz neste tick de coordenacao.
     *
     * <p>A ordem das perguntas nao e arbitraria, e ela e a regra:</p>
     *
     * <ol>
     *   <li><b>o bando mandou recuar</b> -- moral quebrada vence tudo, inclusive
     *       um alvo na cara. E isso que faz o bando recuar JUNTO em vez de cada
     *       um decidir sozinho e a fuga sair em fila indiana;</li>
     *   <li><b>estou sozinho</b> -- e a regra propria deste bicho, e ela vem antes
     *       de qualquer coisa sobre o alvo. Um lobo que checa a distancia antes de
     *       checar quantos sao ja esta comprometido quando descobre que esta so;</li>
     *   <li><b>nao ha alvo utilizavel</b> -- reagrupar ou alvo ausente viram
     *       espera, e nao corrida para a ultima posicao conhecida;</li>
     *   <li><b>investe ou cerca</b> -- so quem tem papel de investida se
     *       compromete, e so dentro da distancia de mordida.</li>
     * </ol>
     *
     * @param ordem o que o {@code SquadController} decidiu para o bando inteiro
     * @param membrosVivos quantos membros o bando tem AGORA
     * @param distanciaAoAlvo centro a centro, ja medida pelo servidor
     * @param papelDeInvestida {@link #papelInveste(SquadRole)} do papel deste lobo
     */
    public DecisaoDeMatilha decidir(SquadOrder ordem, int membrosVivos, double distanciaAoAlvo,
            boolean papelDeInvestida) {
        Objects.requireNonNull(ordem, "ordem do bando ausente");
        if (membrosVivos < 0) {
            throw new IllegalArgumentException("membros vivos negativo: " + membrosVivos);
        }
        if (!Double.isFinite(distanciaAoAlvo) || distanciaAoAlvo < 0.0D) {
            throw new IllegalArgumentException("distancia ao alvo invalida: " + distanciaAoAlvo
                    + ". NaN aqui nao pode virar 'avanca': o lado seguro de uma medida quebrada e"
                    + " o lobo nao se comprometer");
        }
        if (ordem.retreat()) return DecisaoDeMatilha.RECUAR;
        if (membrosVivos < membrosParaAvancar) return DecisaoDeMatilha.RECUAR;
        if (ordem.regroup() || ordem.target() == null) return DecisaoDeMatilha.AGUARDAR;
        if (papelDeInvestida && distanciaAoAlvo <= distanciaDeInvestida) {
            return DecisaoDeMatilha.INVESTIR;
        }
        return DecisaoDeMatilha.CERCAR;
    }

    /**
     * Este bando aceita mais um membro? E, se nao, POR QUE nao.
     *
     * <p>As tres recusas fecham falhas diferentes, e todas mudas. O teto e a que
     * impede o reforco de chamar reforco ate o chunk inteiro; a moral e a que
     * impede um bando em fuga de continuar recrutando; o raio e a que impede dois
     * combates a cinquenta blocos de distancia de virarem um bando so.</p>
     *
     * @param tamanhoAtual quantos membros o bando ja tem
     * @param moral a moral do bando agora
     * @param distanciaDoCandidato do candidato ate quem o convidou, em blocos
     */
    public DecisaoDeReforco decidirReforco(int tamanhoAtual, int moral,
            double distanciaDoCandidato) {
        if (tamanhoAtual < 0) {
            throw new IllegalArgumentException("tamanho de bando negativo: " + tamanhoAtual);
        }
        if (!Double.isFinite(distanciaDoCandidato) || distanciaDoCandidato < 0.0D) {
            throw new IllegalArgumentException("distancia de reforco invalida: "
                    + distanciaDoCandidato);
        }
        if (tamanhoAtual >= bando.maximoDeMembros()) return DecisaoDeReforco.BANDO_CHEIO;
        if (moral < bando.moralMinima()) return DecisaoDeReforco.BANDO_DESMORALIZADO;
        if (distanciaDoCandidato > bando.raioDeReforco()) return DecisaoDeReforco.LONGE_DEMAIS;
        return DecisaoDeReforco.CHAMA;
    }
}
