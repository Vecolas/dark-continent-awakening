package com.darkcontinent.nenfoundation.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Portão que impede a animação do Dummy de terminar antes da janela server-side. */
class DummyEnemyAnimationContractTest {
    private static final String RESOURCE =
            "/assets/nenfoundation/animations/entity/dummy_enemy.animation.json";

    @Test
    void clipesDeCombateCobremAsJanelasDaTimeline() {
        JsonObject animations = ler().getAsJsonObject("animations");
        var ataque = HunterExamProfiles.dummyEnemyStrike();
        Map<String, Integer> janelas = Map.of(
                "windup", ataque.windupTicks(),
                "active", ataque.activeTicks(),
                "recovery", ataque.recoveryTicks());

        for (var janela : janelas.entrySet()) {
            JsonObject clipe = animations.getAsJsonObject("animation.dummy_enemy." + janela.getKey());
            double duracao = clipe.get("animation_length").getAsDouble();
            assertTrue(duracao >= janela.getValue() / 20.0D,
                    janela.getKey() + " dura " + duracao + "s, mas a janela server-side exige "
                            + janela.getValue() + " ticks");
            assertFalse(clipe.has("loop") && clipe.get("loop").getAsBoolean(),
                    janela.getKey() + " nao pode reiniciar enquanto a timeline server-side avanca");
        }
    }

    private static JsonObject ler() {
        try (var stream = DummyEnemyAnimationContractTest.class.getResourceAsStream(RESOURCE)) {
            assertTrue(stream != null, "asset de animação do Dummy ausente: " + RESOURCE);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (Exception e) {
            throw new AssertionError("asset de animação do Dummy inválido", e);
        }
    }
}
