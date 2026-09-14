package com.darkcontinent.nenfoundation.enemy.ai;

import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadOrder;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import java.util.Objects;

/**
 * As regras do FLANQUEADOR em cima da ordem que o esquadrao deu -- e so isso.
 *
 * <p><b>Onde termina o Squad e onde comeca este arquivo.</b>
 * {@code SquadController} decide o que vale para o grupo INTEIRO: qual e o alvo,
 * se ele recua, se reagrupa. Aqui mora o que e do flanqueador: por onde ele
 * chega, quando ele para de contornar, e a unica regra que o grupo nao tem como
 * saber -- <b>sozinho, ele nao flanqueia coisa nenhuma</b>.</p>
 *
 * <p><b>Flanquear nao e perseguir, e a diferenca e mecanica.</b> Perseguir e ir
 * pelo caminho mais curto; flanquear e ir para um POSTO medido a partir do olhar
 * do alvo, e so fechar depois de ter saido do arco frontal dele. Escrever isso
 * dentro do {@code SquadController} faria toda familia que usa squad passar a
 * contornar -- inclusive o frontliner, cuja funcao e justamente ficar na cara do
 * alvo para que o flanqueador consiga dar a volta. As duas metades so funcionam
 * porque sao diferentes.</p>
 *
 * <p><b>Por que existem DOIS angulos.</b> {@code anguloDeFlancoEmGraus} e para
 * onde o bicho ANDA; {@code arcoFrontalDoAlvoEmGraus} e o que o alvo enxerga e,
 * portanto, onde o bote e proibido. Os dois precisam de um do outro: com o posto
 * DENTRO do arco frontal, o flanqueador anda ate um lugar de onde ele nunca vai
 * receber permissao para investir, e o resultado e um mob que gira em volta do
 * jogador para sempre e nunca ataca. Nao levanta excecao, nao reprova portao
 * nenhum, e o unico sinal e um inimigo inofensivo. O construtor cobra isso.</p>
 *
 * <p><b>Este record nao conhece mundo, entidade nem Minecraft.</b> Ele recebe
 * numeros ja medidos pelo servidor e devolve decisoes.</p>
 *
 * @param bando as regras do grupo, de onde saem o teto e o espacamento.
 *        Recebidas, e nao copiadas: repetir o espacamento aqui criaria dois
 *        numeros para a mesma coisa, e girar um deles deixaria o outro mandando
 * @param membrosParaFlanquear quantos precisam estar vivos para ele avancar
 * @param anguloDeFlancoEmGraus posto do flanco, medido a partir do OLHAR do alvo
 * @param arcoFrontalDoAlvoEmGraus meia-abertura em que o alvo enxerga o
 *        flanqueador; dentro dela o bote e recusado
 * @param distanciaDeInvestida distancia (centro a centro) em que ele fecha
 * @param raioDoContorno raio do arco em que ele circula antes de fechar
 */
public record RegrasDeFlanco(SquadRules bando, int membrosParaFlanquear,
        double anguloDeFlancoEmGraus, double arcoFrontalDoAlvoEmGraus,
        double distanciaDeInvestida, double raioDoContorno) {

    public RegrasDeFlanco {
        Objects.requireNonNull(bando, "regras de bando ausentes");
        if (membrosParaFlanquear < 2) {
            throw new IllegalArgumentException("membrosParaFlanquear = " + membrosParaFlanquear
                    + ": um bicho que flanqueia sozinho nao flanqueia -- nao ha quem segure a"
                    + " frente, e o alvo simplesmente vira para ele. HP 28 e dano 7 so fazem"
                    + " sentido em grupo, e um flanqueador solitario agressivo morre de graca sem"
                    + " que nenhum portao reclame");
        }
        if (membrosParaFlanquear > bando.maximoDeMembros()) {
            throw new IllegalArgumentException("membrosParaFlanquear = " + membrosParaFlanquear
                    + " e o teto do esquadrao e " + bando.maximoDeMembros() + ": o grupo nunca"
                    + " alcanca o numero que autoriza avancar, e o bicho recua para sempre. Ele"
                    + " nasce, corre, foge e passa em tudo -- o unico sinal e um inimigo que"
                    + " nunca ataca");
        }
        if (!Double.isFinite(anguloDeFlancoEmGraus)
                || anguloDeFlancoEmGraus <= 0.0D || anguloDeFlancoEmGraus >= 180.0D) {
            throw new IllegalArgumentException("angulo de flanco fora de (0, 180): "
                    + anguloDeFlancoEmGraus + ". Zero e uma carga frontal com outro nome; 180 poe"
                    + " os dois flancos no mesmo ponto atras do alvo, e eles se empilham");
        }
        if (!Double.isFinite(arcoFrontalDoAlvoEmGraus)
                || arcoFrontalDoAlvoEmGraus <= 0.0D || arcoFrontalDoAlvoEmGraus >= 180.0D) {
            throw new IllegalArgumentException("arco frontal fora de (0, 180): "
                    + arcoFrontalDoAlvoEmGraus + ". Sem arco frontal nenhum, investir de frente"
                    + " passa a ser permitido e o flanco vira decoracao");
        }
        if (anguloDeFlancoEmGraus <= arcoFrontalDoAlvoEmGraus) {
            throw new IllegalArgumentException("o posto do flanco fica a "
                    + anguloDeFlancoEmGraus + " graus e o alvo enxerga ate "
                    + arcoFrontalDoAlvoEmGraus + ": o bicho anda ate um lugar de onde ele NUNCA"
                    + " recebe permissao para investir. Ele gira em volta do jogador para sempre"
                    + " e nunca ataca -- sem excecao, sem log, e com todos os outros portoes"
                    + " verdes");
        }
        if (!Double.isFinite(distanciaDeInvestida) || distanciaDeInvestida <= 0.0D
                || !Double.isFinite(raioDoContorno) || raioDoContorno <= 0.0D) {
            throw new IllegalArgumentException("distancias de flanco invalidas");
        }
        if (raioDoContorno <= distanciaDeInvestida) {
            throw new IllegalArgumentException("o raio do contorno (" + raioDoContorno + ") tem de"
                    + " ser MAIOR que a distancia de investida (" + distanciaDeInvestida + "):"
                    + " iguais, 'circular' e 'fechar' acontecem no mesmo lugar, os flanqueadores"
                    + " colam na cara do alvo e o contorno deixa de existir");
        }
        double corda = 2.0D * raioDoContorno
                * Math.sin(Math.toRadians(Math.min(anguloDeFlancoEmGraus, 90.0D)));
        if (corda < bando.espacamento()) {
            throw new IllegalArgumentException("com raio " + raioDoContorno + " e postos a +/-"
                    + anguloDeFlancoEmGraus + " graus, os dois flancos ficam a " + corda
                    + " blocos um do outro, e o espacamento exigido e " + bando.espacamento()
                    + ": o arco e apertado demais e os dois se empurram no mesmo ponto -- o"
                    + " jogador le isso como travamento, nunca como IA");
        }
    }

    /**
     * Quem contorna e quem segura a frente.
     *
     * <p>DOIS papeis contornam, e os outros nao. Se TODOS contornassem, ninguem
     * ficaria na cara do alvo: ele giraria livre, todo mundo continuaria dentro
     * do arco frontal de alguem, e o esquadrao inteiro circularia sem nunca
     * fechar. Isso nao da erro -- da um bando que parece indeciso.</p>
     */
    public static boolean papelContorna(SquadRole papel) {
        Objects.requireNonNull(papel, "papel ausente");
        return papel == SquadRole.FLANKER || papel == SquadRole.SCOUT;
    }

    /**
     * O posto deste membro, em graus a partir do olhar do alvo.
     *
     * <p>Quem segura a frente tem posto ZERO: ele e a razao de o flanco
     * funcionar. {@code pelaDireita} vem de um bit ESTAVEL de quem pergunta (o
     * uuid), e nao de um sorteio: sorteado a cada tick, o bicho trocaria de lado
     * no meio do contorno e andaria em zigue-zague na frente do jogador.</p>
     */
    public double anguloDePostoEmGraus(SquadRole papel, boolean pelaDireita) {
        if (!papelContorna(papel)) return 0.0D;
        return pelaDireita ? anguloDeFlancoEmGraus : -anguloDeFlancoEmGraus;
    }

    /** Cosseno do arco frontal; dentro dele o alvo enxerga e o bote e recusado. */
    public double cossenoDoArcoFrontal() {
        return Math.cos(Math.toRadians(arcoFrontalDoAlvoEmGraus));
    }

    /**
     * O flanqueador ja saiu do campo de visao do alvo?
     *
     * <p><b>A BORDA PERTENCE A QUEM ENXERGA</b> -- a comparacao e estrita. Quem
     * esta exatamente no limite do arco ainda e visto, e por isso ainda nao pode
     * fechar. Com {@code <=}, o arco declarado valeria um fio de grau a menos do
     * que o numero escrito, e a diferenca so apareceria como um bicho que as
     * vezes investe de frente -- que e indistinguivel de sorte para quem esta
     * olhando.</p>
     *
     * @param cossenoDoOlharDoAlvo 1 quando o alvo esta olhando direto para o
     *        flanqueador, -1 quando esta de costas para ele
     */
    public boolean foraDoArcoFrontal(double cossenoDoOlharDoAlvo) {
        return exigirCosseno(cossenoDoOlharDoAlvo) < cossenoDoArcoFrontal();
    }

    /**
     * O que este flanqueador faz neste tick de coordenacao.
     *
     * <p>A ordem das perguntas nao e arbitraria, e ela e a regra:</p>
     *
     * <ol>
     *   <li><b>o esquadrao mandou recuar</b> -- moral quebrada vence tudo,
     *       inclusive um alvo na cara;</li>
     *   <li><b>estou sozinho</b> -- regra propria deste bicho, e ela vem antes
     *       de qualquer coisa sobre o alvo. Quem checa a distancia antes de
     *       checar quantos sao ja esta comprometido quando descobre que esta
     *       so;</li>
     *   <li><b>nao ha alvo utilizavel</b> -- reagrupar ou alvo ausente viram
     *       espera, e nao corrida ate a ultima posicao conhecida;</li>
     *   <li><b>ainda estou longe</b> -- contorna. Este e o ramo que faz o bicho
     *       nao ir pela reta;</li>
     *   <li><b>estou perto, mas na cara dele</b> -- continua contornando. E aqui
     *       que "flanquear" deixa de ser um rotulo: chegar perto nao basta,
     *       tem de chegar perto POR FORA.</li>
     * </ol>
     *
     * @param ordem o que o {@code SquadController} decidiu para o grupo inteiro
     * @param membrosVivos quantos membros o esquadrao tem AGORA
     * @param distanciaAoAlvo centro a centro, ja medida pelo servidor
     * @param cossenoDoOlharDoAlvo o quanto o alvo esta virado para este bicho
     */
    public DecisaoDeFlanco decidir(SquadOrder ordem, int membrosVivos, double distanciaAoAlvo,
            double cossenoDoOlharDoAlvo) {
        Objects.requireNonNull(ordem, "ordem do esquadrao ausente");
        if (membrosVivos < 0) {
            throw new IllegalArgumentException("membros vivos negativo: " + membrosVivos);
        }
        if (!Double.isFinite(distanciaAoAlvo) || distanciaAoAlvo < 0.0D) {
            throw new IllegalArgumentException("distancia ao alvo invalida: " + distanciaAoAlvo
                    + ". NaN aqui nao pode virar 'investe': o lado seguro de uma medida quebrada e"
                    + " o bicho nao se comprometer");
        }
        if (ordem.retreat()) return DecisaoDeFlanco.RECUAR;
        if (membrosVivos < membrosParaFlanquear) return DecisaoDeFlanco.RECUAR;
        if (ordem.regroup() || ordem.target() == null) return DecisaoDeFlanco.AGUARDAR;
        if (distanciaAoAlvo > distanciaDeInvestida) return DecisaoDeFlanco.CONTORNAR;
        if (papelContorna(ordem.role()) && !foraDoArcoFrontal(cossenoDoOlharDoAlvo)) {
            return DecisaoDeFlanco.CONTORNAR;
        }
        return DecisaoDeFlanco.INVESTIR;
    }

    private static double exigirCosseno(double valor) {
        if (!Double.isFinite(valor) || valor < -1.0D || valor > 1.0D) {
            throw new IllegalArgumentException("cosseno do olhar fora de [-1, 1]: " + valor
                    + ". Um valor fora da faixa e uma medida quebrada, e medida quebrada nao pode"
                    + " virar permissao para investir");
        }
        return valor;
    }
}
