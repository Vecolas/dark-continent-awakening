package com.darkcontinent.nenfoundation.enemy.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Valida referências de datapack que o codec estrutural não consegue conhecer. */
public final class EnemyDefinitionValidator {
    private EnemyDefinitionValidator() { }

    /** Retorna todos os problemas para que uma recarga não exija várias tentativas. */
    public static List<String> problemas(EnemyDefinition definition) {
        List<String> problemas = new ArrayList<>();
        for (String tag : definition.spawnRule().biomeTags()) {
            if (!tag.startsWith("#") || ResourceLocation.tryParse(tag.substring(1)) == null) {
                problemas.add("biome tag invalida: " + tag);
            }
        }
        for (String dimension : definition.spawnRule().dimensions()) {
            if (ResourceLocation.tryParse(dimension) == null) {
                problemas.add("dimension invalida: " + dimension);
            }
        }
        if (definition.audioId().getPath().isBlank()) {
            problemas.add("audio_id sem path");
        }
        return List.copyOf(problemas);
    }
}
