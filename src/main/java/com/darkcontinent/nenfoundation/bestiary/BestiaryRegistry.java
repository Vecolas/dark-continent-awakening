package com.darkcontinent.nenfoundation.bestiary;

import com.darkcontinent.nenfoundation.NenFoundation;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Catálogo inicial; o reload de JSON entra na próxima fatia sem mudar a ficha. */
public final class BestiaryRegistry {
    public static final ResourceLocation FOXBEAR_ID = NenFoundation.id("foxbear");
    private static final Map<ResourceLocation, BestiaryEntryDefinition> ENTRIES = Map.of(
            FOXBEAR_ID, new BestiaryEntryDefinition(FOXBEAR_ID,
                    NenFoundation.id("foxbear"), BestiaryCategory.WILDLIFE, 2,
                    "nenfoundation.bestiary.habitat.foxbear",
                    "nenfoundation.bestiary.summary.foxbear",
                    "nenfoundation.bestiary.behavior.foxbear",
                    "nenfoundation.bestiary.combat.foxbear"));

    private BestiaryRegistry() { }
    public static BestiaryEntryDefinition get(ResourceLocation id) { return ENTRIES.get(id); }
    public static List<BestiaryEntryDefinition> entries() { return List.copyOf(ENTRIES.values()); }
}
