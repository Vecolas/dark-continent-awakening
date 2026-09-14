package com.darkcontinent.nenfoundation.enemy.spawn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Tetos de densidade: quantos cabem perto, e a que distancia o proximo grupo nasce.
 *
 * <p><b>O erro que isto impede nao e um crash, e uma paisagem.</b> Sem teto, a
 * lista de bioma continua valendo a cada tentativa de spawn e o resultado e uma
 * manada de quarenta great stamps num vale -- tudo dentro das regras, nada no
 * log, e o encontro que deveria ser tenso vira uma parede de carne. O inverso e
 * igualmente silencioso: teto baixo demais faz o bioma parecer vazio.</p>
 *
 * <p>Estes numeros SAO botao de balanceamento e por isso moram no dado, junto da
 * {@link SpawnRule}. O que nao e botao e a existencia do teto.</p>
 *
 * @param maximoPorChunk quantos desta especie podem coexistir no chunk de spawn
 * @param distanciaMinimaEntreGrupos blocos ate o grupo mais proximo da especie
 * @param distanciaMinimaDeJogador blocos ate o jogador mais proximo; zero libera
 */
public record SpawnCaps(int maximoPorChunk, int distanciaMinimaEntreGrupos,
        int distanciaMinimaDeJogador) {

    public static final Codec<SpawnCaps> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 64).fieldOf("max_por_chunk").forGetter(SpawnCaps::maximoPorChunk),
            Codec.intRange(0, 512).fieldOf("distancia_entre_grupos")
                    .forGetter(SpawnCaps::distanciaMinimaEntreGrupos),
            Codec.intRange(0, 256).fieldOf("distancia_de_jogador")
                    .forGetter(SpawnCaps::distanciaMinimaDeJogador))
            .apply(instance, SpawnCaps::new));

    public SpawnCaps {
        if (maximoPorChunk < 1) {
            throw new IllegalArgumentException("teto de " + maximoPorChunk + " por chunk impede o"
                    + " spawn inteiro; para desligar o mob use SpawnProfile.ENCOUNTER_ONLY, que"
                    + " diz a intencao em vez de escondê-la num zero");
        }
        if (distanciaMinimaEntreGrupos < 0 || distanciaMinimaDeJogador < 0) {
            throw new IllegalArgumentException("distancia de spawn negativa");
        }
    }

    /** Fauna comum: ate quatro por chunk, grupos a 48 blocos, longe do jogador. */
    public static SpawnCaps fauna() { return new SpawnCaps(4, 48, 24); }

    /** Encontro raro: um por chunk e um grupo a cada 160 blocos. */
    public static SpawnCaps raro() { return new SpawnCaps(1, 160, 32); }

    /**
     * Decide se mais um pode nascer AGORA.
     *
     * @param jaNoChunk quantos da especie o servidor contou no chunk
     * @param distanciaDoGrupoMaisProximo blocos, ou {@code Double.NaN} se nao ha grupo
     * @param distanciaDoJogadorMaisProximo blocos, ou {@code Double.NaN} se nao ha jogador
     */
    public boolean permite(int jaNoChunk, double distanciaDoGrupoMaisProximo,
            double distanciaDoJogadorMaisProximo) {
        if (jaNoChunk < 0) throw new IllegalArgumentException("contagem negativa no chunk");
        if (jaNoChunk >= maximoPorChunk) return false;
        if (!Double.isNaN(distanciaDoGrupoMaisProximo)
                && distanciaDoGrupoMaisProximo < distanciaMinimaEntreGrupos) {
            return false;
        }
        // NaN significa "nao ha jogador medido", e nao "esta longe". Deixar a
        // comparacao com NaN decidir sozinha devolveria false silenciosamente em
        // toda comparacao, e o mob nunca nasceria -- o bioma vazio de sempre.
        return Double.isNaN(distanciaDoJogadorMaisProximo)
                || distanciaDoJogadorMaisProximo >= distanciaMinimaDeJogador;
    }
}
