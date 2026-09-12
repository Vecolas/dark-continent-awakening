package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.entity.FrogInWaitingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * O Frog-In-Waiting com o jogo de pe.
 *
 * <p>O QUE SO EXISTE AQUI: a vitima montando de verdade, a desmontagem sendo
 * RECUSADA pelo evento, e a soltura acontecendo por cada um dos caminhos. Nada
 * disso e alcancavel por teste puro -- {@code GrabRules} responde "deve
 * soltar?", e nao "soltou?".
 *
 * <p>A VITIMA E UM GOLEM DE FERRO. Tres motivos, todos concretos:
 * {@code makeMockServerPlayerInLevel()} devolve um jogador com
 * {@code isCreative()} cravado em {@code true}, e o sapo recusa presa em modo
 * criativo; dano de mob contra JOGADOR e escalado pela dificuldade do servidor,
 * entao o resultado dependeria da sessao; e o golem tem 100 de vida, o
 * suficiente para sobreviver ao agarrao inteiro e provar a soltura POR TEMPO em
 * vez de por morte. Golem de ferro nao caca criatura pacifica, entao ele nao
 * revida e nao compra a propria soltura no dano.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class FrogInWaitingGameTest {

    private static final String ARENA = "arena";

    private FrogInWaitingGameTest() { }

    /** O sapo nasce escondido; um sapo que aparece andando nunca emboscou ninguem. */
    @GameTest(template = ARENA, timeoutTicks = 60)
    @PrefixGameTestTemplate(false)
    public static void oSapoNasceEnterrado(GameTestHelper helper) {
        FrogInWaitingEntity sapo = helper.spawn(
                FrogInWaitingEntity.registeredType(), new BlockPos(5, 1, 5));
        helper.assertTrue(sapo.estaEnterrado(),
                "o sapo nasceu fora da terra: a emboscada comeca entregue.");
        helper.assertFalse(sapo.estaAgarrando(),
                "sapo recem-nascido ja dizia estar agarrando alguem.");
        helper.succeed();
    }

    /**
     * Quem pisa em cima e agarrado -- e NAO sai apertando shift.
     *
     * <p>A recusa de desmontagem e a unica coisa que faz a janela de escape
     * existir. Sem ela bastaria agachar, e o agarrao inteiro viraria enfeite --
     * sem erro nenhum no log, porque desmontar e uma acao perfeitamente valida.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    @PrefixGameTestTemplate(false)
    public static void quemFoiEngolidoNaoSaiApertandoShift(GameTestHelper helper) {
        FrogInWaitingEntity sapo = helper.spawn(
                FrogInWaitingEntity.registeredType(), new BlockPos(5, 1, 5));
        IronGolem presa = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(6, 1, 5));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(sapo.estaAgarrando(),
                        "o sapo nunca agarrou quem estava em cima da toca, dentro do raio de"
                                + " gatilho do perfil."))
                .thenExecute(() -> {
                    helper.assertTrue(presa.getVehicle() == sapo,
                            "o sapo diz que agarrou, mas a vitima nao esta montada nele.");
                    helper.assertFalse(sapo.estaEnterrado(),
                            "o sapo agarrou sem emergir: o jogador nao teve aviso nenhum.");
                    presa.stopRiding();
                    helper.assertTrue(presa.getVehicle() == sapo,
                            "a vitima se desmontou sozinha. A saida legitima e por dano ou por"
                                    + " tempo; se agachar bastasse, a janela de escape nao existiria.");
                })
                .thenSucceed();
    }

    /**
     * O agarrao sempre solta -- e aqui ele solta pelo RELOGIO.
     *
     * <p>E a prova de que nao ha caminho de saida esquecido no caso mais chato:
     * ninguem bateu, ninguem morreu, e mesmo assim a vitima sai. Um agarrao que
     * so termina em morte nao daria erro: daria um jogador preso dentro de um
     * sapo ate o servidor reiniciar.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void oAgarraoSoltaQuandoOTempoAcaba(GameTestHelper helper) {
        FrogInWaitingEntity sapo = helper.spawn(
                FrogInWaitingEntity.registeredType(), new BlockPos(5, 1, 5));
        IronGolem presa = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(6, 1, 5));
        int duracao = HunterExamProfiles.frogGrabRules().ticksMaximos();

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(sapo.estaAgarrando(),
                        "o sapo nunca agarrou; o teste nao chegou a medir a soltura."))
                .thenExecuteAfter(duracao + 20, () -> {
                    helper.assertTrue(presa.isAlive(),
                            "a vitima morreu antes de o relogio acabar: este teste precisa provar"
                                    + " a soltura por TEMPO, e nao por morte.");
                    helper.assertFalse(sapo.estaAgarrando(),
                            "passados " + duracao + " ticks o sapo ainda diz estar agarrando.");
                    helper.assertTrue(presa.getVehicle() == null,
                            "o relogio acabou e a vitima continua montada no sapo.");
                    helper.assertTrue(presa.getHealth() < presa.getMaxHealth(),
                            "a vitima saiu do agarrao sem um arranhao: os pulsos de dano nunca"
                                    + " aconteceram, e o agarrao virou abraco.");
                })
                .thenSucceed();
    }

    /**
     * Matar o sapo solta quem estava dentro dele.
     *
     * <p>Morte e o ponto de saida que mais costuma ficar de fora, porque a
     * entidade some e parece que o problema sumiu junto. Nao some: a vitima
     * fica montada num veiculo morto.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    @PrefixGameTestTemplate(false)
    public static void matarOSapoSoltaAVitima(GameTestHelper helper) {
        FrogInWaitingEntity sapo = helper.spawn(
                FrogInWaitingEntity.registeredType(), new BlockPos(5, 1, 5));
        IronGolem presa = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(6, 1, 5));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(sapo.estaAgarrando(),
                        "o sapo nunca agarrou; o teste nao chegou a medir a soltura por morte."))
                .thenExecute(() -> sapo.hurt(helper.getLevel().damageSources().genericKill(), 1000.0F))
                .thenExecuteAfter(5, () -> {
                    helper.assertFalse(sapo.isAlive(), "o sapo sobreviveu a 1000 de dano.");
                    helper.assertTrue(presa.getVehicle() == null,
                            "o sapo morreu e a vitima continuou montada nele.");
                })
                .thenSucceed();
    }
}
