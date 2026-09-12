package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.FoxbearEntity;
import com.darkcontinent.nenfoundation.enemy.FoxbearState;
import com.darkcontinent.nenfoundation.registry.EnemyEntityTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Prova o caminho real de registro, atributos e estado inicial do Foxbear. */
@GameTestHolder(NenFoundation.MOD_ID)
public final class FoxbearGameTest {
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void foxbearRegistradoComAtributosEEstadoNeutro(GameTestHelper helper) {
        FoxbearEntity foxbear = EnemyEntityTypes.FOXBEAR.get().create(helper.getLevel());
        helper.assertTrue(foxbear != null, "registro do Foxbear devolveu entidade nula");
        helper.assertTrue(foxbear.getMaxHealth() == 44.0F, "vida prototipo incorreta");
        helper.assertTrue(foxbear.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                == 6.0D, "dano prototipo incorreto");
        helper.assertTrue(foxbear.state() == FoxbearState.ROAM, "estado inicial nao e neutro");
        helper.succeed();
    }
}
