package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.entity.SpiderEagleEntity;
import com.darkcontinent.nenfoundation.registry.NenBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * O Spider Eagle com o jogo de pe.
 *
 * <p>A ARENA E {@code nenfoundation:arena_alta}: 11x20x11. A arena de 6 de
 * altura nao serve para esta ave -- o telegrafo do mergulho E a subida, e um
 * cenario sem teto suficiente mediria o teto, nao o mob.
 *
 * <p><b>O PISO FICA EM y=1</b>, como na arena baixa: {@code prepareTestStructure}
 * cria o structure block um bloco ABAIXO do conteudo. Quem nasce em y=1 nasce
 * dentro da pedra. Todo spawn daqui usa y=2.
 *
 * <p>O INTRUSO E UM GOLEM DE FERRO, como nos outros tres mobs:
 * {@code makeMockServerPlayerInLevel()} devolve jogador criativo, e dano contra
 * jogador e escalado pela dificuldade do servidor.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class SpiderEagleGameTest {

    private static final String ARENA_ALTA = "arena_alta";

    private SpiderEagleGameTest() { }

    /** O spawn cria o ninho no mundo uma vez, sem reescrever um bloco existente. */
    @GameTest(template = ARENA_ALTA, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void oSpawnCriaONinho(GameTestHelper helper) {
        SpiderEagleEntity ave = helper.spawn(
                SpiderEagleEntity.registeredType(), new BlockPos(3, 2, 3));

        helper.startSequence()
                .thenExecuteAfter(3, () -> helper.assertTrue(
                        helper.getLevel().getBlockState(ave.ninho()).is(NenBlocks.SPIDER_EAGLE_NEST.get()),
                        "a ave ancorou a coordenada, mas nao criou o bloco de ninho no mundo."))
                .thenSucceed();
    }

    /**
     * O NINHO NAO ANDA. E o unico estado deste mob que precisa sobreviver ao save.
     *
     * <p>Sem NBT, recarregar o mundo re-ancoraria o ninho onde a ave estivesse
     * voando naquele instante, e a colonia migraria sozinha ao longo das
     * sessoes -- sem erro nenhum, sem log, sem ninguem conseguindo reproduzir.
     *
     * <p>O cenario nao reinicia servidor: ele faz a ida e volta pelo MESMO
     * caminho que o save usa ({@code saveWithoutId} / {@code load}), com a ave
     * ja longe do ninho. E o que da para provar sem restart -- e o restart de
     * verdade continua declarado como nao provado.
     */
    @GameTest(template = ARENA_ALTA, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void oNinhoNaoAndaQuandoAAveESalva(GameTestHelper helper) {
        SpiderEagleEntity ave = helper.spawn(
                SpiderEagleEntity.registeredType(), new BlockPos(3, 2, 3));

        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    BlockPos ninhoOriginal = ave.ninho();
                    helper.assertTrue(ninhoOriginal != null,
                            "a ave nao ancorou ninho nenhum depois de tres ticks de servidor.");

                    // Longe do ninho, que e exatamente a situacao em que re-ancorar
                    // pela posicao atual estragaria o mundo em silencio.
                    ave.setPos(ave.getX() + 6.0D, ave.getY() + 9.0D, ave.getZ() + 6.0D);

                    CompoundTag salvo = ave.saveWithoutId(new CompoundTag());
                    SpiderEagleEntity recarregada = SpiderEagleEntity.registeredType()
                            .create(helper.getLevel());
                    helper.assertTrue(recarregada != null,
                            "o EntityType nao conseguiu criar uma segunda ave para o teste.");
                    recarregada.load(salvo);
                    BlockPos ninhoDepois = recarregada.ninho();
                    recarregada.discard();

                    helper.assertTrue(ninhoOriginal.equals(ninhoDepois),
                            "o ninho era " + ninhoOriginal + " e voltou do save como " + ninhoDepois
                                    + ". A ave re-ancorou onde estava voando: o ninho anda sozinho"
                                    + " a cada recarga do mundo.");
                })
                .thenSucceed();
    }

    /**
     * O AVISO VEM ANTES DO BOTE -- e a fase volta para IDLE quando acaba.
     *
     * <p>Sao as duas metades do mesmo contrato. A primeira impede "estava parada
     * e instantaneamente causou dano". A segunda impede o oposto: um mergulho
     * que comeca e nunca termina nao aparece como erro, aparece como uma ave que
     * nunca mais ataca ninguem.
     */
    @GameTest(template = ARENA_ALTA, timeoutTicks = 400)
    @PrefixGameTestTemplate(false)
    public static void oAvisoVemAntesDoBoteEAFaseVolta(GameTestHelper helper) {
        SpiderEagleEntity ave = helper.spawn(
                SpiderEagleEntity.registeredType(), new BlockPos(5, 2, 5));
        IronGolem intruso = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(7, 2, 5));
        ave.setTarget(intruso);

        int ticksDeAviso = HunterExamProfiles.spiderEagleNest().ticksDeAviso();

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ave.estaAvisando(),
                        "o intruso esta dentro do raio de bote e a ave nunca chegou a avisar."))
                .thenExecute(() -> helper.assertTrue(ave.faseDeAtaque() == AttackPhase.IDLE,
                        "a ave avisou e mergulhou no MESMO tick: o telegrafo de " + ticksDeAviso
                                + " ticks nao existiu, e o jogador nao teve tempo de recuar."))
                .thenExecuteAfter(ticksDeAviso - 5, () -> helper.assertTrue(
                        ave.faseDeAtaque() == AttackPhase.IDLE,
                        "o mergulho comecou antes de completar os " + ticksDeAviso
                                + " ticks de aviso exigidos pelo perfil."))
                .thenWaitUntil(() -> helper.assertTrue(ave.faseDeAtaque() != AttackPhase.IDLE,
                        "passado o aviso inteiro, com o intruso parado dentro do raio de bote,"
                                + " o mergulho nunca comecou."))
                .thenExecuteAfter(80, () -> helper.assertTrue(ave.faseDeAtaque() == AttackPhase.IDLE,
                        "oitenta ticks depois do inicio a fase ainda e " + ave.faseDeAtaque()
                                + ". O mergulho inteiro dura 38 ticks: ele ficou preso."))
                .thenSucceed();
    }

    /**
     * QUEM RECUA E POUPADO. E a promessa do mob inteiro.
     *
     * <p>O intruso comeca dentro do raio de aviso e fora do raio de bote -- a
     * ave avisa e nao ataca. Ao sair da zona, ele deixa de ser alvo. Sem isto a
     * ave viraria mais um predador que persegue pelo mapa, e roubar os ovos sem
     * matar a mae -- o objetivo do encounter, na secao 40 do plano -- nunca
     * seria possivel.
     */
    @GameTest(template = ARENA_ALTA, timeoutTicks = 400)
    @PrefixGameTestTemplate(false)
    public static void quemRecuaEPoupado(GameTestHelper helper) {
        SpiderEagleEntity ave = helper.spawn(
                SpiderEagleEntity.registeredType(), new BlockPos(1, 2, 5));
        // Nove blocos: dentro do raio de aviso (16) e FORA do raio de bote (6).
        IronGolem intruso = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(10, 2, 5));
        ave.setTarget(intruso);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ave.estaAvisando(),
                        "a ave nunca avisou o intruso que entrou na zona do ninho."))
                .thenExecute(() -> {
                    helper.assertTrue(ave.faseDeAtaque() == AttackPhase.IDLE,
                            "a ave mergulhou num intruso que esta fora do raio de bote: o aviso"
                                    + " deixou de ser aviso.");
                    // O intruso recua para MUITO alem do raio de aviso.
                    intruso.teleportTo(ave.getX() + 30.0D, ave.getY(), ave.getZ());
                })
                .thenExecuteAfter(40, () -> {
                    helper.assertFalse(ave.estaAvisando(),
                            "o intruso saiu da zona do ninho ha quarenta ticks e a ave continua"
                                    + " em aviso.");
                    helper.assertTrue(ave.getTarget() == null,
                            "o intruso recuou e a ave continua com ele como alvo: a promessa de"
                                    + " que quem recua e poupado nao vale.");
                })
                .thenSucceed();
    }
}
