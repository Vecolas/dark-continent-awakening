package com.darkcontinent.nenfoundation.bestiary;

import com.darkcontinent.nenfoundation.NenFoundation;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;
import net.minecraft.resources.ResourceLocation;

/** Catálogo inicial; o reload de JSON entra na próxima fatia sem mudar a ficha. */
public final class BestiaryRegistry {
    public static final ResourceLocation FOXBEAR_ID = NenFoundation.id("foxbear");
    private static final Map<ResourceLocation, BestiaryEntryDefinition> DEFAULT_ENTRIES = Map.of(
            FOXBEAR_ID, new BestiaryEntryDefinition(FOXBEAR_ID,
                    NenFoundation.id("foxbear"), BestiaryCategory.WILDLIFE, 2,
                    "nenfoundation.bestiary.habitat.foxbear",
                    "nenfoundation.bestiary.summary.foxbear",
                    "nenfoundation.bestiary.behavior.foxbear",
                    "nenfoundation.bestiary.combat.foxbear"));

    private static final Map<ResourceLocation, BestiaryEntryDefinition> ENTRIES =
            new LinkedHashMap<>(DEFAULT_ENTRIES);
    private BestiaryRegistry() { }
    public static synchronized BestiaryEntryDefinition get(ResourceLocation id) { return ENTRIES.get(id); }
    public static synchronized List<BestiaryEntryDefinition> entries() { return List.copyOf(ENTRIES.values()); }
    public static synchronized void replaceAll(Collection<BestiaryEntryDefinition> definitions) {
        var novo = new LinkedHashMap<ResourceLocation, BestiaryEntryDefinition>();
        definitions.forEach(definition -> {
            if (novo.put(definition.id(), definition) != null) {
                throw new IllegalArgumentException("entrada duplicada de bestiario: " + definition.id());
            }
        });
        if (novo.isEmpty()) throw new IllegalArgumentException("catalogo de bestiario vazio");
        ENTRIES.clear();
        ENTRIES.putAll(novo);
    }
}
