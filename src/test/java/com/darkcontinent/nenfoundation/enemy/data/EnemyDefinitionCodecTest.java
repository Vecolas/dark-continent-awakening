package com.darkcontinent.nenfoundation.enemy.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class EnemyDefinitionCodecTest {
    private static final String JSON = """
            {
              "metadata": {"id":"example:field_beast","canon_level":"ORIGINAL_COMPATIBLE",
                "faction":"WILDLIFE","threat_tier":"LOW","territorial":false,"social":false,
                "visual_id":"example:field_beast"},
              "attributes": {"max_health":10.0,"movement_speed":0.2,"attack_damage":1.0,
                "armor":0.0,"follow_range":8.0,"knockback_resistance":0.0},
              "spawn": {"biome_tags":["#example:field_biomes"],"dimensions":["minecraft:overworld"],
                "min_light":0,"max_light":15,"require_ground":true,"allow_water":false,
                "require_sky":false,"max_nearby_same_faction":4},
              "audio_id":"example:entity/field_beast",
              "timings":{"strike":{"windup_ticks":8,"active_ticks":4,"recovery_ticks":12,
                "interruptible_windup":true,"interruptible_active":false,"interruptible_recovery":true}},
              "schema_version":1
            }
            """;

    @Test
    void codecLeMetadadosAtributosSpawnEAudio() {
        EnemyDefinition definition = EnemyDefinition.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString(JSON)).getOrThrow();

        assertEquals(ResourceLocation.parse("example:field_beast"), definition.metadata().id());
        assertEquals(10.0F, definition.attributes().maxHealth());
        assertTrue(definition.spawnRule().biomeTags().contains("#example:field_biomes"));
        assertEquals(ResourceLocation.parse("example:entity/field_beast"), definition.audioId());
        assertEquals(8, definition.timings().get("strike").windupTicks());
        assertEquals(1, definition.schemaVersion());
        assertTrue(EnemyDefinitionValidator.problemas(definition).isEmpty());
    }

    @Test
    void codecRecusaEnumInvalidoECampoObrigatorioAusente() {
        var enumInvalido = JsonParser.parseString(JSON.toString().replace("\"LOW\"", "\"UNKNOWN\""));
        var semAudio = JsonParser.parseString(JSON.replace(",\n  \"audio_id\":\"example:entity/field_beast\"", ""));
        var schemaFuturo = JsonParser.parseString(JSON.replace("\"schema_version\":1", "\"schema_version\":2"));

        assertFalse(EnemyDefinition.CODEC.parse(JsonOps.INSTANCE, enumInvalido).result().isPresent());
        assertFalse(EnemyDefinition.CODEC.parse(JsonOps.INSTANCE, semAudio).result().isPresent());
        assertFalse(EnemyDefinition.CODEC.parse(JsonOps.INSTANCE, schemaFuturo).result().isPresent());
    }

    @Test
    void validatorRecusaReferenciaDeTagEDeDimensaoMalformadas() {
        EnemyDefinition definition = new EnemyDefinition(
                new com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata(
                        ResourceLocation.parse("example:bad"),
                        com.darkcontinent.nenfoundation.enemy.api.CanonLevel.ORIGINAL_COMPATIBLE,
                        com.darkcontinent.nenfoundation.enemy.api.EnemyFaction.CUSTOM,
                        com.darkcontinent.nenfoundation.enemy.api.ThreatTier.LOW, false, false, "example:bad"),
                new EnemyAttributes(1, 0, 0, 0, 0, 0),
                new com.darkcontinent.nenfoundation.enemy.spawn.SpawnRule(
                        Set.of("example:missing_hash", "#bad tag"), Set.of("not a dimension"),
                        0, 15, false, false, false, 1),
                ResourceLocation.parse("example:entity/bad"));

        List<String> problemas = EnemyDefinitionValidator.problemas(definition);
        assertEquals(3, problemas.size());
    }

    @Test
    void catalogoDosSeteInimigosPublicadosEValido() throws Exception {
        Path pasta = Path.of("src/main/resources/data/nenfoundation/enemy_definitions");
        List<Path> arquivos = Files.list(pasta).sorted().toList();

        assertEquals(8, arquivos.size());
        for (Path arquivo : arquivos) {
            EnemyDefinition definition = EnemyDefinition.CODEC.parse(JsonOps.INSTANCE,
                    JsonParser.parseString(Files.readString(arquivo))).getOrThrow();
            assertEquals(0, EnemyDefinitionValidator.problemas(definition).size(), arquivo.toString());
            assertEquals(1, definition.schemaVersion());
            EnemyDefinition legado = HunterExamProfiles.publicados().get(
                    arquivo.getFileName().toString().replace(".json", ""));
            assertEquals(legado.metadata(), definition.metadata(), arquivo.toString());
            assertEquals(legado.attributes(), definition.attributes(), arquivo.toString());
            assertEquals(legado.spawnRule(), definition.spawnRule(), arquivo.toString());
        }
    }
}
