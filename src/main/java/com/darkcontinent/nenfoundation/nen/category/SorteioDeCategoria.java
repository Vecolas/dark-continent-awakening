package com.darkcontinent.nenfoundation.nen.category;

import java.util.Objects;
import java.util.UUID;

/**
 * De qual categoria um jogador e, dado um mundo e um jogador.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. E DETERMINISTICO, e essa e a razao de existir. Um sorteio com
 * {@code Random} sem semente daria uma categoria diferente a cada execucao, e
 * um relato de bug do tipo "Conjuration nao funciona" viraria impossivel de
 * reproduzir: a proxima tentativa sortearia outra coisa. Aqui, o mesmo mundo
 * mais o mesmo jogador dao sempre a mesma categoria, e o QA consegue voltar ao
 * caso exato.
 *
 * <p>2. A MISTURA E EXPLICITA, e nao {@code new Random(semente).nextInt(6)}. O
 * embaralhamento interno do {@code java.util.Random} e um detalhe da JDK; o
 * dia em que ele mudar, todo jogador de todo mundo existente troca de
 * categoria em silencio. O finalizador do SplitMix64 abaixo esta escrito aqui,
 * nao herdado: ele so muda se alguem editar este arquivo.
 *
 * <p>3. AS SEIS TEM O MESMO PESO. Isso e uma SIMPLIFICACAO DECLARADA, nao uma
 * leitura do cânone: a skill de lore diz que habilidades de Specialization
 * devem ser raras. Peso por categoria e decisao de balanceamento, e numero de
 * balanceamento nao nasce cravado no codigo -- ele nasce em config, com a
 * regua junto. Enquanto nao existe consumidor de afinidade (M4), um peso aqui
 * seria um botao que ninguem consegue medir. Fica como pendencia escrita, e
 * nao como constante escondida.
 *
 * <p>4. Ele NAO decide QUANDO alguem ganha categoria, nem se pode. Isso e do
 * servico. Aqui so mora a funcao pura, para que ela possa ser exercitada sem
 * um servidor de pe.
 */
public final class SorteioDeCategoria {

    private SorteioDeCategoria() {
    }

    /**
     * A categoria correspondente a uma semente. Sempre uma das seis REAIS.
     *
     * <p>O vies de modulo existe e e desprezivel: {@code 2^64 % 6 == 4}, o que
     * favorece quatro das seis em cerca de uma parte em {@code 3e18}. Dito em
     * voz alta para que ninguem "descubra" isso depois e ache que e bug.
     */
    public static NenCategory sortear(long semente) {
        long misturada = misturar(semente);
        int indice = (int) Math.floorMod(misturada, (long) NenCategory.REAIS.size());
        return NenCategory.REAIS.get(indice);
    }

    /**
     * A semente de um jogador num mundo.
     *
     * <p>POR QUE OS DOIS, e nao so o UUID: com so o UUID, a mesma pessoa teria
     * a mesma categoria em todo mundo que ela entrasse, para sempre. Com so a
     * semente do mundo, todos os jogadores de um servidor teriam a MESMA
     * categoria -- e ninguem notaria ate o segundo jogador despertar.
     *
     * <p>A combinacao e uma multiplicacao por um primo impar antes de somar,
     * para que trocar a ordem dos dois nao produza a mesma semente.
     */
    public static long sementeDe(long sementeDoMundo, UUID jogador) {
        Objects.requireNonNull(jogador, "jogador");
        long combinada = sementeDoMundo;
        combinada = combinada * 6364136223846793005L + jogador.getMostSignificantBits();
        combinada = combinada * 6364136223846793005L + jogador.getLeastSignificantBits();
        return combinada;
    }

    /** Atalho para o caso normal: mundo mais jogador. */
    public static NenCategory sortear(long sementeDoMundo, UUID jogador) {
        return sortear(sementeDe(sementeDoMundo, jogador));
    }

    /**
     * O finalizador do SplitMix64.
     *
     * <p>Espalha cada bit da entrada por toda a saida. Sem ele, sementes
     * vizinhas -- que e exatamente o que UUIDs sequenciais de teste produzem --
     * cairiam em indices vizinhos, e o sorteio pareceria funcionar em producao
     * enquanto o teste com dez jogadores devolveria as seis na ordem.
     */
    private static long misturar(long x) {
        x += 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }
}
