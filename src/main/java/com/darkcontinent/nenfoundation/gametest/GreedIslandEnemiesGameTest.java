package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Smoke test dos sete corpos de Greed Island no servidor. */
@GameTestHolder(NenFoundation.MOD_ID)
public final class GreedIslandEnemiesGameTest {
    private static final String ARENA = "arena";

    private GreedIslandEnemiesGameTest() { }

    /** Todos os tipos reais nascem e atravessam ticks sem excecao de setup. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void seteCriaturasDeGreedIslandInicializam(GameTestHelper helper) {
        EntityType<?>[] tipos = {
                EnemyEntityTypes.CYCLOPS.get(),
                EnemyEntityTypes.HYPER_PUFFBALL.get(),
                EnemyEntityTypes.MELANIN_LIZARD.get(),
                EnemyEntityTypes.RADIO_RAT.get(),
                EnemyEntityTypes.BUBBLE_HORSE.get(),
                EnemyEntityTypes.KING_WHITE_STAG_BEETLE.get(),
                EnemyEntityTypes.WOLF_PACK_HUNTER.get()
        };

        List<LivingEntity> criadas = new ArrayList<>();
        for (int i = 0; i < tipos.length; i++) {
            LivingEntity criatura = (LivingEntity) tipos[i].spawn(
                    helper.getLevel(), helper.absolutePos(new BlockPos(2 + i * 2, 2, 5)),
                    MobSpawnType.COMMAND);
            helper.assertTrue(criatura != null,
                    "o tipo de Greed Island nao criou entidade no indice " + i);
            criadas.add(criatura);
        }

        helper.startSequence().thenIdle(40).thenExecute(() -> {
            for (LivingEntity criatura : criadas) {
                helper.assertTrue(criatura.isAlive(),
                        "uma das sete criaturas deixou de existir durante o smoke test: "
                                + criatura.getType().getDescriptionId());
            }
            helper.succeed();
        });
    }
}
