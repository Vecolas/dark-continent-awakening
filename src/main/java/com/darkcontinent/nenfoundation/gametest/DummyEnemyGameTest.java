package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.entity.DummyEnemyEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Fluxo mínimo do EN1: spawn, percepção, ação sincronizada, hit server-side e morte. */
@GameTestHolder(NenFoundation.MOD_ID)
public final class DummyEnemyGameTest {
    private DummyEnemyGameTest() { }

    @GameTest(template = "arena", timeoutTicks = 120)
    @PrefixGameTestTemplate(false)
    public static void dummyPercebeAtacaPublicaAcaoEDesaparece(GameTestHelper helper) {
        DummyEnemyEntity dummy = helper.spawnWithNoFreeWill(DummyEnemyEntity.registeredType(),
                new BlockPos(5, 2, 5));
        IronGolem alvo = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(5, 2, 7));
        alvo.moveTo(dummy.getX(), dummy.getY(), dummy.getZ() + 1.7D, 180.0F, 0.0F);
        dummy.setTarget(alvo);
        float vidaInicial = alvo.getHealth();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(dummy.actionId() > 0,
                        "o DummyEnemy nao publicou action id ao selecionar um alvo"))
                .thenExecute(() -> helper.assertTrue(dummy.attackPhase() == AttackPhase.WINDUP
                        || dummy.attackPhase() == AttackPhase.ACTIVE,
                        "a entidade selecionou alvo sem publicar WINDUP/ACTIVE"))
                .thenWaitUntil(() -> helper.assertTrue(alvo.getHealth() < vidaInicial,
                        "a janela ACTIVE nao aplicou dano no servidor"))
                .thenExecute(() -> helper.assertTrue(dummy.actionId() > 0,
                        "action id perdeu a identidade durante a mesma acao"))
                .thenExecute(() -> dummy.kill())
                .thenWaitUntil(() -> helper.assertTrue(!dummy.isAlive(),
                        "a entidade nao encerrou seu ciclo de vida"))
                .thenSucceed();
    }

    @GameTest(template = "arena", timeoutTicks = 80)
    @PrefixGameTestTemplate(false)
    public static void dummyMantemAlvoServerSideComDoisJogadores(GameTestHelper helper) {
        DummyEnemyEntity dummy = helper.spawnWithNoFreeWill(DummyEnemyEntity.registeredType(),
                new BlockPos(5, 2, 5));
        ServerPlayer alvo = helper.makeMockServerPlayerInLevel();
        ServerPlayer observador = helper.makeMockServerPlayerInLevel();
        alvo.moveTo(dummy.getX(), dummy.getY(), dummy.getZ() + 1.7D, 180.0F, 0.0F);
        observador.moveTo(dummy.getX() + 20.0D, dummy.getY(), dummy.getZ(), 0.0F, 0.0F);
        dummy.setTarget(alvo);

        helper.assertTrue(!alvo.getUUID().equals(observador.getUUID()),
                "os dois jogadores de teste precisam ter identidades distintas");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(dummy.actionId() > 0,
                        "o servidor nao iniciou a acao para o alvo escolhido"))
                .thenExecute(() -> {
                    helper.assertTrue(dummy.getTarget() == alvo,
                            "o alvo foi trocado por uma decisao de cliente ou por outro jogador");
                    helper.assertTrue(dummy.actionId() > 0,
                            "a acao autoritativa perdeu seu id durante a janela");
                    dummy.discard();
                    alvo.discard();
                    observador.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = "arena", timeoutTicks = 60)
    @PrefixGameTestTemplate(false)
    public static void dummyEntraESaiDeStaggerSemKnockback(GameTestHelper helper) {
        DummyEnemyEntity dummy = helper.spawnWithNoFreeWill(DummyEnemyEntity.registeredType(),
                new BlockPos(5, 2, 5));
        dummy.hurt(helper.getLevel().damageSources().generic(), 4.0F);
        helper.assertTrue(dummy.staggered(),
                "impacto acima do limiar nao publicou stagger no servidor");
        helper.runAtTickTime(21, () -> helper.assertTrue(!dummy.staggered(),
                "stagger nao terminou pela duracao declarada"));
        helper.runAtTickTime(22, () -> {
            dummy.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 40)
    @PrefixGameTestTemplate(false)
    public static void dummyResolveWeakPointNoServidor(GameTestHelper helper) {
        DummyEnemyEntity dummy = helper.spawnWithNoFreeWill(DummyEnemyEntity.registeredType(),
                new BlockPos(5, 2, 5));
        IronGolem atacante = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM,
                new BlockPos(5, 5, 8));
        atacante.setYRot(180.0F);
        atacante.setXRot(25.0F);
        float antes = dummy.getHealth();

        dummy.hurt(helper.getLevel().damageSources().mobAttack(atacante), 1.0F);

        helper.assertTrue(antes - dummy.getHealth() >= 1.9F,
                "o acerto alto e frontal nao recebeu o multiplicador do weak point");
        dummy.discard();
        atacante.discard();
        helper.succeed();
    }
}
