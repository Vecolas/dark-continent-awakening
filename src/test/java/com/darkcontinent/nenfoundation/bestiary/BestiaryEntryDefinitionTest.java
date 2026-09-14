package com.darkcontinent.nenfoundation.bestiary;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class BestiaryEntryDefinitionTest {
    @Test
    void categoriaDesconhecidaViraErroDeCodec() {
        var json = definicao("NOT_A_CATEGORY");

        var resultado = BestiaryEntryDefinition.CODEC.parse(JsonOps.INSTANCE, json);

        assertTrue(resultado.error().isPresent(), "JSON inválido deve ser rejeitado sem exceção");
    }

    @Test
    void fichasDataDrivenDoModSaoTodasAceitasPeloMesmoCodec() throws IOException {
        var nomes = List.of("dummy_enemy", "foxbear", "frog_in_waiting", "great_stamp", "kiriko",
                "man_faced_ape", "master_of_the_swamp", "spider_eagle");

        for (var nome : nomes) {
            var caminho = "/data/nenfoundation/bestiary/" + nome + ".json";
            try (var recurso = getClass().getResourceAsStream(caminho)) {
                assertTrue(recurso != null, "ficha ausente: " + caminho);
                var json = JsonParser.parseReader(new InputStreamReader(recurso, StandardCharsets.UTF_8));
                assertTrue(BestiaryEntryDefinition.CODEC.parse(JsonOps.INSTANCE, json).result().isPresent(),
                        "ficha inválida: " + caminho);
            }
        }
    }

    @Test
    void progressoRejeitaEnumsDesconhecidos() {
        var nivelInvalido = JsonParser.parseString("{\"knowledge_level\":\"CORRUPTED\"}");
        var nenInvalido = JsonParser.parseString("{\"nen_status\":\"CORRUPTED\"}");

        assertTrue(BestiaryProgress.CODEC.parse(JsonOps.INSTANCE, nivelInvalido).error().isPresent());
        assertTrue(BestiaryProgress.CODEC.parse(JsonOps.INSTANCE, nenInvalido).error().isPresent());
    }

    private static JsonObject definicao(String categoria) {
        var json = new JsonObject();
        json.addProperty("entity_type", "nenfoundation:foxbear");
        json.addProperty("category", categoria);
        json.addProperty("threat", 2);
        json.addProperty("habitat", "nenfoundation.bestiary.habitat.foxbear");
        json.addProperty("summary", "nenfoundation.bestiary.summary.foxbear");
        json.addProperty("behavior", "nenfoundation.bestiary.behavior.foxbear");
        json.addProperty("combat", "nenfoundation.bestiary.combat.foxbear");
        return json;
    }
}
