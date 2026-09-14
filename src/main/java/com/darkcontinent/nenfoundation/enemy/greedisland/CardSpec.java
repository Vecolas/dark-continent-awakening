package com.darkcontinent.nenfoundation.enemy.greedisland;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * A ficha de um card: o que ele representa e quantos existem.
 *
 * <p><b>{@code copiasNoMundo} nao e balanceamento -- e a regra de Greed Island.</b>
 * Na obra, cada card tem um numero finito de copias na ilha, e e isso que
 * transforma a coleta em disputa. Tratar esse numero como infinito nao daria erro
 * nenhum: daria um jogo onde ninguem precisa correr, e a mecanica inteira viraria
 * enfeite. Zero significa ILIMITADO, e e assim que um card comum se declara.</p>
 *
 * <p>O id do card e o id da CRIATURA de proposito. Dois ids separados seriam duas
 * fontes para a mesma verdade, e a divergencia apareceria como um card que nao
 * casa com o bicho que o soltou -- sem erro, so com uma colecao que nao fecha.</p>
 *
 * @param monsterId id da criatura que este card representa
 * @param rank letra do card na obra: S, A, B, C, D, E, F, G, H
 * @param copiasNoMundo quantas copias existem; 0 e ilimitado
 */
public record CardSpec(ResourceLocation monsterId, String rank, int copiasNoMundo) {

    public static final Codec<CardSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("monster_id").forGetter(CardSpec::monsterId),
            Codec.STRING.fieldOf("rank").forGetter(CardSpec::rank),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("copias_no_mundo")
                    .forGetter(CardSpec::copiasNoMundo))
            .apply(instance, CardSpec::new));

    /** As letras que a obra usa. Fora desta lista, o card nao existe no catalogo. */
    private static final String RANKS = "SABCDEFGH";

    public CardSpec {
        if (monsterId == null) throw new NullPointerException("card sem criatura");
        if (rank == null || rank.length() != 1 || RANKS.indexOf(rank.charAt(0)) < 0) {
            throw new IllegalArgumentException("rank de card invalido: '" + rank + "'. Os ranks"
                    + " sao " + RANKS + " -- um valor fora disso passaria pelo codec e cairia"
                    + " numa ordenacao que ninguem consegue explicar.");
        }
        if (copiasNoMundo < 0) throw new IllegalArgumentException("copias negativas");
    }

    public boolean ilimitado() { return copiasNoMundo == 0; }

    /** Ainda cabe uma copia? Quem conta as emitidas e o servico, nao o record. */
    public boolean cabeMaisUma(int jaEmitidas) {
        if (jaEmitidas < 0) throw new IllegalArgumentException("contagem de copias negativa");
        return ilimitado() || jaEmitidas < copiasNoMundo;
    }
}
