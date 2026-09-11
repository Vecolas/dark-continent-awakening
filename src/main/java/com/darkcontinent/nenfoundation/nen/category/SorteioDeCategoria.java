package com.darkcontinent.nenfoundation.nen.category;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * De qual categoria um jogador e, dado um mundo e uma identidade.
 *
 * <p>Funcao PURA: mesma semente e mesmo jogador, mesma categoria, sempre.
 * Nenhum estado, nenhum {@code Random} guardado, nenhuma leitura de relogio.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. DETERMINISMO E REQUISITO, e nao elegancia. Um bug de categoria
 * ("a Adivinhacao da Agua mostrou Emission e a ficha diz Conjuration")
 * so e investigavel se o relator conseguir dizer em que mundo ele
 * aconteceu e nos conseguirmos reproduzir a mesma categoria. Com sorteio
 * aleatorio, a primeira pergunta da investigacao -- "aconteceu de novo?" --
 * nao tem resposta.
 *
 * <p>2. A SEMENTE DO MUNDO ENTRA NA CONTA. Sem ela, todo mundo de todo
 * servidor daria a mesma categoria para o mesmo UUID, e um site com a tabela
 * pronta tornaria a Adivinhacao da Agua decorativa no dia seguinte ao
 * lancamento.
 *
 * <p>3. NAO USAMOS {@link java.util.Random}. O contrato do LCG do Java e
 * estavel, mas ele nao e o ponto: o ponto e que a mistura fique ESCRITA aqui,
 * onde se pode ler, em vez de depender do que uma classe da plataforma promete
 * hoje. A funcao abaixo e o finalizador do SplitMix64 -- quatro linhas,
 * difusao boa o bastante para seis baldes, e nenhuma dependencia.
 *
 * <p>4. {@link NenCategory#UNDETERMINED} NUNCA SAI DAQUI. O sorteio percorre
 * {@link NenCategory#REAIS}, derivada do enum. Uma lista literal de seis nomes
 * neste arquivo divergiria do enum no dia em que alguem acrescentasse uma
 * categoria -- e o sintoma seria uma categoria que existe e nunca e sorteada,
 * sem nenhuma linha no log.
 *
 * <p>O QUE ISTO NAO E: nao e a afinidade. Categoria e o que o jogador E; a
 * afinidade por categoria (100/80/60/40) deriva dela e tem servico proprio.
 * E nao ha viés por personalidade -- o teste do Hisoka e explicitamente
 * nao-confiavel no cânone, e nao vira regra de atribuicao.
 */
public final class SorteioDeCategoria {

    /**
     * Constante de mistura do SplitMix64 (o "golden gamma", 2^64 / phi).
     *
     * <p>Ela nao e um botao de ajuste: mudar este numero muda a categoria de
     * todo jogador de todo mundo ja criado, em silencio. Se algum dia ela
     * precisar mudar, isso e migracao de save, nao tuning.
     */
    private static final long GAMMA = 0x9E3779B97F4A7C15L;

    private SorteioDeCategoria() {
    }

    /**
     * A categoria deste jogador neste mundo.
     *
     * @param sementeDoMundo semente do overworld; ver
     *                       {@code NenCategoryService} para de onde ela sai
     * @param jogador        UUID do jogador. Com {@code online-mode=false} ele
     *                       deriva do nome, e isso e proposital: em instancia
     *                       de teste, entrar como "Gon" devolve sempre a mesma
     *                       categoria
     * @return uma das seis categorias reais; jamais {@link NenCategory#UNDETERMINED}
     */
    public static NenCategory sortear(long sementeDoMundo, UUID jogador) {
        Objects.requireNonNull(jogador, "jogador");

        // Os dois metades do UUID entram em rodadas SEPARADAS. Combina-las
        // com um XOR unico faria dois jogadores com metades trocadas caírem
        // no mesmo balde -- improvavel de acontecer, impossivel de descobrir
        // depois.
        long h = misturar(sementeDoMundo);
        h = misturar(h + jogador.getMostSignificantBits());
        h = misturar(h + jogador.getLeastSignificantBits());

        List<NenCategory> reais = NenCategory.REAIS;

        // floorMod, e nao %. O resto de um negativo em Java e negativo, e um
        // indice negativo aqui seria uma excecao para metade dos jogadores.
        return reais.get(Math.floorMod(h, reais.size()));
    }

    /** Finalizador do SplitMix64. Difunde cada bit de entrada por todos os 64. */
    private static long misturar(long valor) {
        long x = valor + GAMMA;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }
}
