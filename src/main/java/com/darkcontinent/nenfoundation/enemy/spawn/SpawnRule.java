package com.darkcontinent.nenfoundation.enemy.spawn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;

/**
 * Regra configuravel que complementa Biome Modifiers e SpawnPlacements.
 *
 * <p>Desde a issue #112 ela carrega o {@link SpawnProfile}, e ele e OBRIGATORIO.
 * Um valor padrao aqui seria comodo e caro: quem escrevesse a regra de um chefe
 * sem pensar no assunto ganharia "natural" de graca, e o chefe entraria na lista
 * de bioma sem que nada acusasse -- o sintoma so apareceria como um encontro
 * unico nascendo pelo mundo inteiro.</p>
 */
public record SpawnRule(Set<String> biomeTags, Set<String> dimensions, int minLight,
        int maxLight, boolean requireGround, boolean allowWater, boolean requireSky,
        int maxNearbySameFaction, SpawnProfile profile, SpawnCaps caps) {
    private static final Codec<Set<String>> STRING_SET = Codec.STRING.listOf()
            .xmap(Set::copyOf, List::copyOf);

    public static final Codec<SpawnRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            STRING_SET.fieldOf("biome_tags").forGetter(SpawnRule::biomeTags),
            STRING_SET.fieldOf("dimensions").forGetter(SpawnRule::dimensions),
            Codec.INT.fieldOf("min_light").forGetter(SpawnRule::minLight),
            Codec.INT.fieldOf("max_light").forGetter(SpawnRule::maxLight),
            Codec.BOOL.fieldOf("require_ground").forGetter(SpawnRule::requireGround),
            Codec.BOOL.fieldOf("allow_water").forGetter(SpawnRule::allowWater),
            Codec.BOOL.fieldOf("require_sky").forGetter(SpawnRule::requireSky),
            Codec.INT.fieldOf("max_nearby_same_faction").forGetter(SpawnRule::maxNearbySameFaction),
            SpawnProfile.CODEC.fieldOf("profile").forGetter(SpawnRule::profile),
            SpawnCaps.CODEC.fieldOf("caps").forGetter(SpawnRule::caps))
            .apply(instance, SpawnRule::new));

    public SpawnRule {
        if (biomeTags == null || dimensions == null || minLight < 0 || maxLight > 15
                || minLight > maxLight || maxNearbySameFaction < 0) {
            throw new IllegalArgumentException("regra de spawn invalida");
        }
        if (profile == null) throw new NullPointerException("perfil de spawn ausente");
        if (caps == null) throw new NullPointerException("tetos de spawn ausentes");

        // Tag de bioma so faz sentido para quem ENTRA na lista de bioma. Exigir a
        // lista vazia no resto nao e purismo: uma tag declarada num perfil que
        // nunca a consulta e um alarme orfao -- ela passa a existir no datapack, o
        // portao a confere, e mesmo assim ela nao coloca o mob em lugar nenhum.
        if (profile.entraNaListaDeBioma()) {
            if (biomeTags.isEmpty()) {
                throw new IllegalArgumentException("perfil " + profile + " entra na lista de bioma"
                        + " e precisa de ao menos uma tag; sem tag ele nunca nasce, e isso nao"
                        + " aparece como erro -- aparece como um bioma vazio");
            }
        } else if (!biomeTags.isEmpty()) {
            throw new IllegalArgumentException("perfil " + profile + " NAO entra na lista de bioma,"
                    + " mas declarou as tags " + biomeTags + ". Ou o perfil esta errado, ou as"
                    + " tags sao alarme orfao; as duas leituras exigem uma decisao humana.");
        }
        if (dimensions.isEmpty()) throw new IllegalArgumentException("regra de spawn sem dimensao");

        biomeTags = Set.copyOf(biomeTags);
        dimensions = Set.copyOf(dimensions);
    }

    /** Atalho para quem audita biome modifier: so quem responde true pode ter um. */
    public boolean entraNaListaDeBioma() { return profile.entraNaListaDeBioma(); }

    public boolean permite(SpawnContext context) {
        if (context == null) throw new NullPointerException("contexto ausente");
        return biomeTags.contains(context.biomeTag()) && dimensions.contains(context.dimension())
                && context.light() >= minLight && context.light() <= maxLight
                && (!requireGround || context.onGround())
                && (allowWater || !context.inWater())
                && (!requireSky || context.skyVisible())
                && context.nearbySameFaction() < maxNearbySameFaction;
    }
}
