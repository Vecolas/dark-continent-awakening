package com.darkcontinent.nenfoundation.nen.profile;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * O progresso de Nen de um jogador que sobrevive a logout, restart e morte.
 *
 * <p>CONTRATO CONGELADO — schema v1 (plano tecnico, secao 22; ADR-002).
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. Aqui NAO mora estado de combate. Aura atual, cooldown, tecnica ativa e
 * canalizacao vivem em {@code RuntimeNenState}, que nao e serializado. A razao
 * e concreta: gravar aura no attachment persistido significa gravar em disco a
 * cada tick, e significa que um crash congela o jogador com a aura que ele
 * tinha no ultimo save. Ver ADR-002.
 *
 * <p>2. Todo campo nasce NEUTRO. {@code awakened=false},
 * {@link NenCategory#UNDETERMINED}, conjuntos vazios. Campo com valor util por
 * padrao faz todo jogador nunca-tocado responder "sim" a qualquer pergunta do
 * tipo "esta pessoa tem Nen?".
 *
 * <p>3. {@link #schemaVersion()} e o PRIMEIRO campo e existe desde a v1, antes
 * de haver qualquer migracao. Acrescentar versionamento depois de saves
 * existirem no mundo exige adivinhar de que versao cada save veio.
 *
 * <p>4. O record e imutavel e as colecoes sao copiadas na entrada. Mutar o
 * profile de um jogador a partir de uma referencia guardada em outro lugar e a
 * forma mais barata de vazar progresso entre jogadores sem nenhum erro no log.
 *
 * @param schemaVersion         versao do formato; ver {@link NenProfileMigrator}
 * @param awakened              se o jogador ja despertou Nen
 * @param category              categoria oficial; UNDETERMINED enquanto nao sorteada
 * @param categoryRevealed      se o JOGADOR ja sabe qual e a sua categoria
 * @param auraPotential         base de reserva de aura
 * @param control               eficiencia de controle
 * @param output                limite de saida
 * @param techniqueProficiency  proficiencia por tecnica
 * @param unlockedTechniques    tecnicas disponiveis
 * @param unlockedAbilities     habilidades/Hatsu liberados
 * @param progressionFlags      marcos de gameplay proprios do mod
 */
public record PersistentNenData(
        int schemaVersion,
        boolean awakened,
        NenCategory category,
        boolean categoryRevealed,
        double auraPotential,
        double control,
        double output,
        Map<ResourceLocation, Double> techniqueProficiency,
        Set<ResourceLocation> unlockedTechniques,
        Set<ResourceLocation> unlockedAbilities,
        Set<ResourceLocation> progressionFlags) {

    /** Versao do schema que este codigo ESCREVE. Ver {@link NenProfileMigrator}. */
    public static final int SCHEMA_ATUAL = 1;

    private static final Codec<Set<ResourceLocation>> CONJUNTO_DE_IDS =
            ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf);

    public static final Codec<PersistentNenData> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    Codec.INT.optionalFieldOf("schema_version", SCHEMA_ATUAL)
                            .forGetter(PersistentNenData::schemaVersion),
                    Codec.BOOL.optionalFieldOf("awakened", false)
                            .forGetter(PersistentNenData::awakened),
                    NenCategory.CODEC.optionalFieldOf("category", NenCategory.UNDETERMINED)
                            .forGetter(PersistentNenData::category),
                    Codec.BOOL.optionalFieldOf("category_revealed", false)
                            .forGetter(PersistentNenData::categoryRevealed),
                    Codec.DOUBLE.optionalFieldOf("aura_potential", 0.0D)
                            .forGetter(PersistentNenData::auraPotential),
                    Codec.DOUBLE.optionalFieldOf("control", 0.0D)
                            .forGetter(PersistentNenData::control),
                    Codec.DOUBLE.optionalFieldOf("output", 0.0D)
                            .forGetter(PersistentNenData::output),
                    Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE)
                            .optionalFieldOf("technique_proficiency", Map.of())
                            .forGetter(PersistentNenData::techniqueProficiency),
                    CONJUNTO_DE_IDS.optionalFieldOf("unlocked_techniques", Set.of())
                            .forGetter(PersistentNenData::unlockedTechniques),
                    CONJUNTO_DE_IDS.optionalFieldOf("unlocked_abilities", Set.of())
                            .forGetter(PersistentNenData::unlockedAbilities),
                    CONJUNTO_DE_IDS.optionalFieldOf("progression_flags", Set.of())
                            .forGetter(PersistentNenData::progressionFlags)
            ).apply(inst, PersistentNenData::new));

    /**
     * O estado de um jogador que nunca tocou em Nen. Todo campo neutro.
     *
     * <p>NAO e um "perfil vazio para preencher": e o valor padrao do attachment.
     * Qualquer jogador do servidor responde a este objeto enquanto nao despertar.
     */
    public static final PersistentNenData NAO_DESPERTADO = new PersistentNenData(
            SCHEMA_ATUAL, false, NenCategory.UNDETERMINED, false,
            0.0D, 0.0D, 0.0D, Map.of(), Set.of(), Set.of(), Set.of());

    public PersistentNenData {
        techniqueProficiency = Map.copyOf(techniqueProficiency);
        unlockedTechniques = Set.copyOf(unlockedTechniques);
        unlockedAbilities = Set.copyOf(unlockedAbilities);
        progressionFlags = Set.copyOf(progressionFlags);
    }

    /**
     * A categoria que a INTERFACE pode mostrar.
     *
     * <p>Valor derivado, lido na hora. Guardar "a categoria que o HUD mostra"
     * num campo separado criaria a segunda fonte da mesma verdade, e ela
     * divergiria no instante em que a revelacao acontecesse.
     */
    public NenCategory categoriaVisivel() {
        return this.categoryRevealed ? this.category : NenCategory.UNDETERMINED;
    }

    public boolean conheceTecnica(ResourceLocation tecnica) {
        return this.unlockedTechniques.contains(tecnica);
    }

    public boolean conheceHabilidade(ResourceLocation habilidade) {
        return this.unlockedAbilities.contains(habilidade);
    }

    public boolean temMarco(ResourceLocation marco) {
        return this.progressionFlags.contains(marco);
    }

    /**
     * Proficiencia numa tecnica; {@code 0.0} para tecnica nunca treinada.
     *
     * <p>O piso e explicito porque o consumidor divide por este valor em algumas
     * formulas de progressao, e ausencia lida como {@code null} viraria NPE
     * dentro de um tick de servidor.
     */
    public double proficiencia(ResourceLocation tecnica) {
        return this.techniqueProficiency.getOrDefault(tecnica, 0.0D);
    }
}
