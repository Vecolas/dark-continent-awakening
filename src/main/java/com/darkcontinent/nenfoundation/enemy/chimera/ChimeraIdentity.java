package com.darkcontinent.nenfoundation.enemy.chimera;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * A identidade GERADA de uma formiga -- o que ela e, e nao o que ela esta fazendo.
 *
 * <p><b>Ela e gerada uma vez e persiste para sempre, e por isso e versionada.</b>
 * Rank, morfologia e traits nao sao escolhidos por quem cria a entidade: eles
 * saem do gene pool da colonia no momento do nascimento. Se isso nao sobrevivesse
 * ao save, cada relogada produziria uma formiga diferente com o mesmo nome --
 * sem erro nenhum, e com o jogador jurando que o bicho mudou de cor.</p>
 *
 * <p><b>colonyId e squadId sao OPCIONAIS, e a diferenca importa.</b> Uma formiga
 * pode existir sem colonia (spawn de teste, comando de dev, colonia destruida) e
 * sem esquadrao (ainda nao alistada). Tratar ausencia como zero ou como UUID nulo
 * faria a busca pela colonia devolver "existe" para um id que nao existe, e a
 * formiga orfa passaria a alimentar uma colonia fantasma.</p>
 *
 * <p><b>O que NAO mora aqui:</b> vida, alvo, estado de combate e posicao. Isso e
 * runtime e morre com a entidade; misturar os dois faria o save guardar coisas
 * que nao deveriam sobreviver -- exatamente a separacao do ADR-002.</p>
 */
public record ChimeraIdentity(ResourceLocation definitionId, ChimeraRank rank,
        ChimeraMorphology morphology, Set<ChimeraTrait> traits, UUID colonyId,
        UUID squadId, ChimeraNenStatus nen, int version) {

    /** Versao do schema desta identidade. Ver {@link #carregar}. */
    public static final int SCHEMA_VERSION = 1;

    private static final String DEFINICAO = "definition";
    private static final String RANK = "rank";
    private static final String MORFOLOGIA = "morphology";
    private static final String TRAITS = "traits";
    private static final String COLONIA = "colony";
    private static final String SQUAD = "squad";
    private static final String NEN = "nen";
    private static final String VERSAO = "version";

    /**
     * Quantos traits uma formiga pode carregar.
     *
     * <p>NAO e botao de balanceamento: e o limite do que a ARTE consegue mostrar.
     * A issue #120 proibe skeleton procedural em runtime, entao cada trait
     * visivel precisa existir como osso no modelo -- e um bicho com dez traits
     * teria de ter dez conjuntos de ossos prontos. Quatro e o que cabe.</p>
     */
    public static final int MAXIMO_DE_TRAITS = 4;

    public ChimeraIdentity {
        Objects.requireNonNull(definitionId, "formiga sem definitionId: ela carregaria do save e"
                + " nao saberia que bicho e, e o sintoma seria um modelo vazio");
        Objects.requireNonNull(rank, "formiga sem rank");
        Objects.requireNonNull(morphology, "formiga sem morfologia");
        Objects.requireNonNull(nen, "formiga sem estagio de Nen");
        Objects.requireNonNull(traits, "traits ausentes");
        if (traits.size() > MAXIMO_DE_TRAITS) {
            throw new IllegalArgumentException("formiga com " + traits.size() + " traits; o teto"
                    + " e " + MAXIMO_DE_TRAITS + ". O limite e da ARTE, nao do balanceamento:"
                    + " trait visivel precisa de osso no modelo, e a #120 proibe skeleton"
                    + " procedural em runtime.");
        }
        if (version != SCHEMA_VERSION) {
            throw new IllegalArgumentException("schema de identidade nao suportado: " + version);
        }
        traits = traits.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(traits));
    }

    /** Formiga recem-nascida: sem colonia, sem esquadrao e sem Nen. */
    public static ChimeraIdentity nova(ResourceLocation definitionId, ChimeraRank rank,
            ChimeraMorphology morphology, Set<ChimeraTrait> traits) {
        return new ChimeraIdentity(definitionId, rank, morphology, traits, null, null,
                ChimeraNenStatus.DORMANT, SCHEMA_VERSION);
    }

    public Optional<UUID> colonia() { return Optional.ofNullable(colonyId); }
    public Optional<UUID> squad() { return Optional.ofNullable(squadId); }
    public boolean orfa() { return colonyId == null; }

    public ChimeraIdentity comColonia(UUID colonia) {
        return new ChimeraIdentity(definitionId, rank, morphology, traits, colonia, squadId,
                nen, version);
    }

    public ChimeraIdentity comSquad(UUID squad) {
        return new ChimeraIdentity(definitionId, rank, morphology, traits, colonyId, squad,
                nen, version);
    }

    /** Avanca o estagio de Nen. A progressao e monotona -- ver {@link ChimeraNenStatus}. */
    public ChimeraIdentity comNen(ChimeraNenStatus proximo) {
        return new ChimeraIdentity(definitionId, rank, morphology, traits, colonyId, squadId,
                nen.avancarPara(proximo), version);
    }

    /**
     * Desliga da colonia SEM apagar a formiga.
     *
     * <p>Colonia destruida nao mata quem estava fora do ninho. A formiga vira
     * orfa e continua existindo -- e e por isso que {@link #orfa()} existe: quem
     * varre colonias precisa distinguir "sem dona" de "dona que sumiu do mapa".
     * Apagar a formiga junto pareceria mais limpo e faria entidades desaparecerem
     * da frente do jogador sem motivo visivel.</p>
     */
    public ChimeraIdentity semColonia() {
        return new ChimeraIdentity(definitionId, rank, morphology, traits, null, squadId,
                nen, version);
    }

    // ------------------------------------------------------------ persistencia

    public CompoundTag salvar() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSAO, SCHEMA_VERSION);
        tag.putString(DEFINICAO, definitionId.toString());
        tag.putString(RANK, rank.name());
        tag.putString(MORFOLOGIA, morphology.name());
        ListTag lista = new ListTag();
        // Ordenado pelo ordinal: um Set nao tem ordem, e salvar em ordem variavel
        // faria o mesmo bicho produzir NBT diferente a cada save. Nao daria erro --
        // deixaria todo diff de mundo cheio de ruido, e a mudanca de verdade
        // passaria despercebida.
        traits.stream().sorted().forEach(t -> lista.add(StringTag.valueOf(t.name())));
        tag.put(TRAITS, lista);
        if (colonyId != null) tag.putUUID(COLONIA, colonyId);
        if (squadId != null) tag.putUUID(SQUAD, squadId);
        tag.putString(NEN, nen.name());
        return tag;
    }

    /**
     * Le do save. Versao desconhecida REPROVA.
     *
     * <p>Ler pela metade um formato que nao se conhece produz uma formiga com
     * identidade plausivel e errada -- rank de peon com corpo de oficial, por
     * exemplo -- e isso atravessa todos os portoes.</p>
     */
    public static ChimeraIdentity carregar(CompoundTag tag) {
        Objects.requireNonNull(tag, "nbt ausente");
        int versao = tag.getInt(VERSAO);
        if (versao != SCHEMA_VERSION) {
            throw new IllegalArgumentException("identidade de chimera com schema " + versao
                    + "; suportado: " + SCHEMA_VERSION + ". Ler assim mesmo produziria uma"
                    + " formiga plausivel e errada, que passa por todos os portoes.");
        }
        Set<ChimeraTrait> traits = EnumSet.noneOf(ChimeraTrait.class);
        ListTag lista = tag.getList(TRAITS, Tag.TAG_STRING);
        for (int i = 0; i < lista.size(); i++) {
            traits.add(ChimeraTrait.valueOf(lista.getString(i)));
        }
        return new ChimeraIdentity(
                ResourceLocation.parse(tag.getString(DEFINICAO)),
                ChimeraRank.valueOf(tag.getString(RANK)),
                ChimeraMorphology.valueOf(tag.getString(MORFOLOGIA)),
                traits,
                tag.hasUUID(COLONIA) ? tag.getUUID(COLONIA) : null,
                tag.hasUUID(SQUAD) ? tag.getUUID(SQUAD) : null,
                ChimeraNenStatus.valueOf(tag.getString(NEN)),
                SCHEMA_VERSION);
    }
}
