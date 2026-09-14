package com.darkcontinent.nenfoundation.enemy.ai.squad;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Os numeros de um bando: quantos, de quanto em quanto tempo, e a que distancia.
 *
 * <p><b>{@code ticksDeAtualizacao} nao e afinacao, e orcamento.</b> Coordenacao
 * de squad e a tentacao mais cara da IA de grupo: cada membro querendo saber onde
 * estao os outros multiplica consultas por membro ao quadrado. Com dez membros e
 * atualizacao por tick isso e cem consultas por tick, por bando -- e o sintoma
 * nao e erro, e TPS caindo devagar numa colonia. A issue #143 fixa o orcamento em
 * 10 ticks, e ele e VALIDADO aqui em vez de ficar num comentario.</p>
 *
 * <p><b>{@code espacamento} existe porque bando sem espacamento vira pilha.</b>
 * Todos perseguindo o mesmo alvo pelo caminho mais curto chegam no mesmo ponto, e
 * o resultado e uma coluna de bichos empurrando uns aos outros -- que o jogador
 * le como travamento, nunca como IA.</p>
 *
 * <p><b>{@code moralMinima} e o que faz "recuar junto" existir.</b> Sem moral, um
 * bando so recua quando cada membro decide sozinho, e o resultado e uma fuga em
 * fila indiana. Com moral compartilhada, o bando quebra de uma vez -- que e o que
 * o jogador reconhece.</p>
 */
public record SquadRules(int ticksDeAtualizacao, int maximoDeMembros, double espacamento,
        double raioDeReforco, int moralMinima) {

    /** O orcamento da issue #143, escrito uma vez e cobrado no construtor. */
    public static final int ATUALIZACAO_MINIMA = 5;
    public static final int ATUALIZACAO_MAXIMA = 40;

    public static final Codec<SquadRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(ATUALIZACAO_MINIMA, ATUALIZACAO_MAXIMA).fieldOf("ticks_de_atualizacao")
                    .forGetter(SquadRules::ticksDeAtualizacao),
            Codec.intRange(2, 32).fieldOf("maximo_de_membros").forGetter(SquadRules::maximoDeMembros),
            Codec.DOUBLE.fieldOf("espacamento").forGetter(SquadRules::espacamento),
            Codec.DOUBLE.fieldOf("raio_de_reforco").forGetter(SquadRules::raioDeReforco),
            Codec.intRange(0, 100).fieldOf("moral_minima").forGetter(SquadRules::moralMinima))
            .apply(instance, SquadRules::new));

    public SquadRules {
        if (ticksDeAtualizacao < ATUALIZACAO_MINIMA || ticksDeAtualizacao > ATUALIZACAO_MAXIMA) {
            throw new IllegalArgumentException("atualizacao de squad fora do orcamento ("
                    + ATUALIZACAO_MINIMA + ".." + ATUALIZACAO_MAXIMA + " ticks): "
                    + ticksDeAtualizacao + ". Coordenar todo tick custa consultas por membro ao"
                    + " quadrado, e o sintoma e TPS caindo devagar numa colonia -- nunca um erro.");
        }
        if (maximoDeMembros < 2) {
            throw new IllegalArgumentException("um bando de " + maximoDeMembros + " nao e bando;"
                    + " para um bicho solitario nao se cria squad, deixa-se sem");
        }
        if (!Double.isFinite(espacamento) || espacamento <= 0.0D
                || !Double.isFinite(raioDeReforco) || raioDeReforco <= 0.0D) {
            throw new IllegalArgumentException("distancias de squad invalidas");
        }
        if (moralMinima < 0 || moralMinima > 100) {
            throw new IllegalArgumentException("moral minima fora de 0..100");
        }
        if (raioDeReforco < espacamento) {
            throw new IllegalArgumentException("raio de reforco (" + raioDeReforco + ") menor que"
                    + " o espacamento (" + espacamento + "): os membros se afastariam para alem do"
                    + " raio em que chamam ajuda, e o bando nunca se reuniria -- sem erro nenhum,"
                    + " so com bichos brigando sozinhos a poucos blocos uns dos outros");
        }
    }

    /** Matilha pequena: quatro lobos, coordenacao a cada 10 ticks. */
    public static SquadRules matilha() {
        return new SquadRules(10, 4, 2.5D, 16.0D, 30);
    }

    /** Esquadrao de formiga: ate oito, mais frouxo no espacamento. */
    public static SquadRules esquadrao() {
        return new SquadRules(10, 8, 3.0D, 24.0D, 20);
    }

    /** Desfasa o bando pelo proprio id: dois bandos nao coordenam no mesmo tick. */
    public boolean atualizaNesteTick(int tickDoMundo, int desfasagem) {
        return Math.floorMod(tickDoMundo + desfasagem, ticksDeAtualizacao) == 0;
    }
}
