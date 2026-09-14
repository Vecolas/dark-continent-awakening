package com.darkcontinent.nenfoundation.enemy.chimera;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * O MOLDE de uma familia de formigas -- Crab Heavy, Bat Scout, Wolf Runner.
 *
 * <p><b>Molde nao e identidade.</b> {@link ChimeraIdentity} e o que UMA formiga
 * virou; isto e o que a familia dela permite. A separacao existe porque duas
 * formigas do mesmo molde precisam poder diferir (traits sorteados do gene pool)
 * sem que cada variacao vire um {@code EntityType} novo -- a issue #120 e
 * explicita: no maximo tres EntityTypes por rank, e nunca um por recolor.</p>
 *
 * <p><b>{@code traitsPossiveis} e um conjunto FECHADO.</b> Sortear trait de uma
 * lista aberta produziria combinacoes que a arte nao tem osso para mostrar, e o
 * sintoma seria um bicho com asas invisiveis -- nada no log, e o jogador
 * descrevendo "as vezes ele voa e as vezes nao".</p>
 *
 * @param id id do molde; e ele que a identidade guarda
 * @param rank posicao na colonia
 * @param morphology corpo base; decide o esqueleto, e por isso e fixo
 * @param traitsPossiveis o que o gene pool pode dar a esta familia
 * @param traitsGarantidos o que toda formiga desta familia tem, sem sorteio
 * @param papelNoSquad funcao padrao quando ela e alistada
 */
public record ChimeraDefinition(ResourceLocation id, ChimeraRank rank,
        ChimeraMorphology morphology, Set<ChimeraTrait> traitsPossiveis,
        Set<ChimeraTrait> traitsGarantidos,
        com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole papelNoSquad) {

    private static final Codec<Set<ChimeraTrait>> TRAITS = Codec.STRING
            .xmap(ChimeraTrait::valueOf, ChimeraTrait::name).listOf()
            .xmap(Set::copyOf, List::copyOf);

    public static final Codec<ChimeraDefinition> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("id").forGetter(ChimeraDefinition::id),
                    Codec.STRING.xmap(ChimeraRank::valueOf, ChimeraRank::name)
                            .fieldOf("rank").forGetter(ChimeraDefinition::rank),
                    Codec.STRING.xmap(ChimeraMorphology::valueOf, ChimeraMorphology::name)
                            .fieldOf("morphology").forGetter(ChimeraDefinition::morphology),
                    TRAITS.fieldOf("traits_possiveis").forGetter(ChimeraDefinition::traitsPossiveis),
                    TRAITS.fieldOf("traits_garantidos").forGetter(ChimeraDefinition::traitsGarantidos),
                    Codec.STRING.xmap(
                            com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole::valueOf,
                            Enum::name).fieldOf("papel_no_squad")
                            .forGetter(ChimeraDefinition::papelNoSquad))
                    .apply(instance, ChimeraDefinition::new));

    public ChimeraDefinition {
        Objects.requireNonNull(id, "molde sem id");
        Objects.requireNonNull(rank, "molde sem rank");
        Objects.requireNonNull(morphology, "molde sem morfologia");
        Objects.requireNonNull(papelNoSquad, "molde sem papel de squad");
        Objects.requireNonNull(traitsPossiveis, "traits possiveis ausentes");
        Objects.requireNonNull(traitsGarantidos, "traits garantidos ausentes");

        if (!traitsPossiveis.containsAll(traitsGarantidos)) {
            throw new IllegalArgumentException("o molde " + id + " garante traits que ele nao"
                    + " declara como possiveis: " + traitsGarantidos + " nao cabe em "
                    + traitsPossiveis + ". A divergencia nao daria erro -- daria um bicho com"
                    + " um trait que a arte da familia nao tem osso para mostrar.");
        }
        if (traitsGarantidos.size() > ChimeraIdentity.MAXIMO_DE_TRAITS) {
            throw new IllegalArgumentException("o molde " + id + " garante mais traits do que o"
                    + " teto de " + ChimeraIdentity.MAXIMO_DE_TRAITS + ": nenhuma formiga desta"
                    + " familia poderia ser criada, e isso so apareceria no primeiro spawn");
        }
        if (papelNoSquad == com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole.LEADER) {
            throw new IllegalArgumentException("o molde " + id + " nasce como LEADER: lideranca e"
                    + " PROMOCAO em Squad, e um molde que nasce lider daria dois lideres ao"
                    + " primeiro bando que alistasse duas dessas formigas");
        }
        traitsPossiveis = congelar(traitsPossiveis);
        traitsGarantidos = congelar(traitsGarantidos);
    }

    private static Set<ChimeraTrait> congelar(Set<ChimeraTrait> valores) {
        return valores.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(valores));
    }

    /**
     * Sorteia uma identidade a partir deste molde.
     *
     * <p><b>O sorteio e DETERMINISTICO por semente.</b> Duas formigas nascidas do
     * mesmo gene pool com a mesma semente tem de sair iguais -- senao um servidor
     * e seu backup divergem, e a divergencia so aparece numa investigacao de
     * dessincronia. A semente vem de quem chama (uuid da formiga, tick, seed do
     * mundo); este record nao inventa aleatoriedade.</p>
     *
     * @param semente entrada deterministica do sorteio
     */
    public ChimeraIdentity sortear(long semente) {
        Set<ChimeraTrait> escolhidos = EnumSet.noneOf(ChimeraTrait.class);
        escolhidos.addAll(traitsGarantidos);

        // Ordem estavel antes de sortear: iterar um Set sem ordem faria o mesmo
        // par (molde, semente) produzir traits diferentes entre execucoes.
        List<ChimeraTrait> candidatos = traitsPossiveis.stream()
                .filter(t -> !traitsGarantidos.contains(t))
                .sorted()
                .toList();

        long estado = semente;
        for (ChimeraTrait candidato : candidatos) {
            if (escolhidos.size() >= ChimeraIdentity.MAXIMO_DE_TRAITS) break;
            estado = estado * 6364136223846793005L + 1442695040888963407L;
            // Metade dos candidatos, em media. Um valor mais alto encheria toda
            // formiga ate o teto e apagaria a variacao que o gene pool existe para
            // produzir; mais baixo faria o teto nunca importar.
            if (((estado >>> 33) & 1L) == 1L) escolhidos.add(candidato);
        }
        return ChimeraIdentity.nova(id, rank, morphology, escolhidos);
    }
}
