package com.darkcontinent.nenfoundation.enemy.greedisland;

import com.darkcontinent.nenfoundation.NenFoundation;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Onde Greed Island COMECA e ACABA -- e a resposta vale para os dois lados.
 *
 * <p><b>A regra e simetrica de proposito.</b> "Nenhuma criatura de GI fora de
 * GI" e a metade obvia, e sozinha ela seria carimbo: a outra metade -- "nenhum
 * card de GI convertido fora de GI" -- e a que impede o atalho de levar o bicho
 * para o Overworld e converter la, onde as regras da ilha nao valem. As duas
 * falhas sao silenciosas: a primeira aparece como um Cyclops no pantano, e a
 * segunda como uma colecao completa que ninguem jogou a ilha para ter.</p>
 *
 * <p><b>Por que uma classe e nao um {@code if} em cada consumidor.</b> A
 * pergunta "isto e Greed Island?" vai ser feita pelo spawn, pelo encontro, pela
 * conversao de card e pelo bestiario. Quatro {@code if} iguais divergem no dia
 * em que a ilha ganhar uma segunda dimensao -- e o que diverge nao reclama, so
 * deixa um dos quatro caminhos aberto.</p>
 *
 * <p>A dimensao existe como datapack proprio e possui terreno dedicado. Esta
 * classe continua sendo a autoridade: ela responde "nao" para tudo que nao
 * seja a chave declarada, impedindo que o conteudo de GI escape para o
 * Overworld.</p>
 */
public final class GreedIslandRegion {

    private static final Set<String> CRIATURAS = Set.of(
            "cyclops", "hyper_puffball", "melanin_lizard", "radio_rat",
            "bubble_horse", "king_white_stag_beetle", "wolf_pack_hunter");

    /**
     * A dimensao da ilha. CONGELADA como identidade: ela vai para save, para
     * datapack e para o registro de encontro, e trocar depois do primeiro mundo
     * criado invalida os encontros salvos sem uma linha de erro.
     */
    public static final ResourceKey<Level> DIMENSAO =
            ResourceKey.create(Registries.DIMENSION, NenFoundation.id("greed_island"));

    private GreedIslandRegion() { }

    /** Esta dimensao e a ilha? */
    public static boolean dentro(ResourceKey<Level> dimensao) {
        return DIMENSAO.equals(Objects.requireNonNull(dimensao, "dimensao ausente"));
    }

    /**
     * Exige que a operacao esteja acontecendo DENTRO da ilha.
     *
     * @throws IllegalStateException com o motivo escrito -- recusa sem motivo
     *         produz o pior relato de bug que existe
     */
    public static void exigirDentro(ResourceKey<Level> dimensao, String operacao) {
        if (!dentro(dimensao)) {
            throw new IllegalStateException(operacao + " so acontece dentro de Greed Island."
                    + " Dimensao recebida: " + dimensao.location() + "; esperada: "
                    + DIMENSAO.location() + ". Permitir aqui abriria o atalho de levar a"
                    + " criatura para fora e converter onde as regras da ilha nao valem.");
        }
    }

    /** Identifica o catálogo fechado de criaturas que não pode escapar da ilha. */
    public static boolean eCriatura(ResourceLocation id) {
        Objects.requireNonNull(id, "id da criatura ausente");
        return NenFoundation.MOD_ID.equals(id.getNamespace()) && CRIATURAS.contains(id.getPath());
    }
}
