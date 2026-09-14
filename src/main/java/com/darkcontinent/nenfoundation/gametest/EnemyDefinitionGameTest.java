package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.data.EnemyDefinitionReloadListener;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Prova que o servidor instala o catálogo EN1 carregado do datapack. */
@GameTestHolder(NenFoundation.MOD_ID)
public final class EnemyDefinitionGameTest {
    private static final Set<String> INIMIGOS_PUBLICADOS = Set.of(
            "foxbear", "frog_in_waiting", "great_stamp", "kiriko",
            "man_faced_ape", "master_of_the_swamp", "spider_eagle", "dummy_enemy");

    private EnemyDefinitionGameTest() { }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void catalogoDataDrivenCarregadoNoServidor(GameTestHelper helper) {
        var catalogo = EnemyDefinitionReloadListener.registry().snapshot();
        helper.assertTrue(catalogo.size() == INIMIGOS_PUBLICADOS.size(),
                "o reload EN1 carregou " + catalogo.size() + " definitions, esperado "
                        + INIMIGOS_PUBLICADOS.size());
        for (String inimigo : INIMIGOS_PUBLICADOS) {
            ResourceLocation id = NenFoundation.id(inimigo);
            helper.assertTrue(catalogo.containsKey(id),
                    "definition ausente do catálogo carregado: " + id);
        }
        helper.succeed();
    }
}
