package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.BatScoutEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Regressoes do caminho de voo do Bat Scout. */
@GameTestHolder(NenFoundation.MOD_ID)
public final class BatScoutGameTest {
    private BatScoutGameTest() { }

    /** O controle de voo precisa encontrar FLYING_SPEED durante o tick real. */
    @GameTest(template = "empty", timeoutTicks = 80)
    @PrefixGameTestTemplate(false)
    public static void batScoutPodeSerSpawnadoEVoar(GameTestHelper helper) {
        BatScoutEntity morcego = helper.spawn(
                BatScoutEntity.registeredType(), new BlockPos(2, 3, 2));

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(morcego.getAttribute(Attributes.FLYING_SPEED) != null,
                            "o Bat Scout foi criado sem FLYING_SPEED, usado pelo FlyingMoveControl.");
                    helper.assertTrue(morcego.isAlive(),
                            "o Bat Scout morreu durante o tick de voo.");
                })
                .thenSucceed();
    }
}
