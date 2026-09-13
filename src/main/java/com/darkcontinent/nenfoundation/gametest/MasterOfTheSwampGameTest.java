package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.MasterOfTheSwampEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A pesca do Master of the Swamp, com o jogo de pe.
 *
 * <p>{@code RegrasDeFisgada} e pura e ja testada: ela responde "deve subir a
 * tensao?". O que so existe aqui e a outra metade -- o anzol DE VERDADE sendo
 * encontrado na agua, a bocada acontecendo, e a linha arrebentando quando o
 * pescador foge. Ate esta entrega, tudo isso era calculo verificado e nao
 * comportamento medido, e estava escrito assim em o-que-nao-provamos.md.
 *
 * <p>DUAS ARMADILHAS QUE ESTE CENARIO TEVE DE DESVIAR, e as duas fariam o teste
 * reprovar pelo motivo errado:
 *
 * <ul>
 *   <li>{@code FishingHook.tick()} se DESCARTA quando o dono nao esta segurando
 *       uma vara -- {@code shouldStopFishing} varre as duas maos. Sem dar a vara
 *       ao jogador de mentira, o anzol some no primeiro tick e o peixe fica
 *       "sem isca" por um motivo que nada tem a ver com o mob;
 *   <li>o mob so aceita isca {@code isInWater()}. A arena e de pedra seca, entao
 *       o cenario constroi a piscina antes de qualquer coisa.
 * </ul>
 *
 * <p>O jogador de mentira e CRIATIVO ({@code makeMockServerPlayerInLevel} crava
 * isso), e aqui isso nao atrapalha: diferente do sapo e do macaco, este mob nao
 * filtra presa por modo de jogo -- ele so quer um dono de anzol vivo e no mesmo
 * mundo.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class MasterOfTheSwampGameTest {

    private static final String ARENA = "arena";

    /**
     * UM LOTE POR CENARIO, e isto nao e cerimonia.
     *
     * <p>Cenarios do mesmo lote rodam AO MESMO TEMPO, em arenas vizinhas do mesmo
     * mundo, e o raio de isca deste mob e de 8 blocos -- ele nao sabe o que e uma
     * arena. Com os tres juntos, o peixe de um cenario fisgava o anzol do outro, a
     * guarda de "anzol ja fisgado" negava a bocada ao dono legitimo, e o cenario da
     * linha estourava o relogio esperando uma fisgada que outro peixe tinha levado.
     * A falha nao acusava nada disso: dizia so "timed out".</p>
     *
     * <p>O bando do man-faced ape ensinou a mesma licao por 12 blocos. Mob com
     * alcance em area precisa de lote proprio.</p>
     */
    private static final String LOTE_BOCADA = "master_of_the_swamp_bocada";
    private static final String LOTE_LINHA = "master_of_the_swamp_linha";
    private static final String LOTE_SECO = "master_of_the_swamp_seco";

    /** O piso do template chega em y=1; a agua vai de y=2 para cima. */
    private static final int FUNDO = 2;
    private static final int SUPERFICIE = 4;

    private MasterOfTheSwampGameTest() { }

    /**
     * A bocada acontece: anzol na agua, peixe perto, e ele fisga.
     *
     * <p>E o unico caminho de entrada do encontro inteiro. Se ele nao fechar,
     * tensao, captura e fuga sao codigo que nunca roda.
     */
    @GameTest(template = ARENA, timeoutTicks = 400, batch = LOTE_BOCADA)
    @PrefixGameTestTemplate(false)
    public static void oPeixeFisgaOAnzolQueCaiNaAgua(GameTestHelper helper) {
        encherPiscina(helper);
        MasterOfTheSwampEntity peixe = helper.spawn(
                MasterOfTheSwampEntity.registeredType(), new BlockPos(4, FUNDO, 5));
        ServerPlayer pescador = pescadorComVara(helper, 9, FUNDO, 5);
        // NA BOCA DE PROPOSITO. Este cenario mede a fisgada e nao a navegacao
        // aquatica: deixar a isca no bloco vizinho introduz uma corrida entre
        // movimento, colisao e o relogio da mordida. A distancia zero garante que
        // a entrada da bocada seja o comportamento observado, sem ampliar o raio
        // da mecanica real.
        FishingHook anzol = lancarAnzol(helper, pescador, 4, FUNDO, 5);
        BlockPos boia = helper.absolutePos(new BlockPos(4, FUNDO, 5));

        helper.startSequence()
                .thenExecute(() -> helper.assertFalse(anzol.isRemoved(),
                        "o anzol se descartou no primeiro tick -- o jogador de mentira esta sem"
                                + " vara na mao, e o cenario nao chegou a medir o mob."))
                .thenWaitUntil(() -> {
                    // A classe vanilla ainda pode atualizar a posicao do hook durante
                    // o tick mesmo com noPhysics. O fixture o ancora para que este
                    // teste meca somente descoberta, mordida e fisgada do mob.
                    if (!anzol.isRemoved()) {
                        anzol.moveTo(boia.getX() + 0.5D, boia.getY() + 0.5D,
                                boia.getZ() + 0.5D, 0.0F, 0.0F);
                        anzol.setDeltaMovement(0.0D, 0.0D, 0.0D);
                    }
                    helper.assertTrue(peixe.estaFisgado(),
                            "o anzol esta na agua, dentro do raio de isca do perfil, e o peixe nunca"
                                    + " fisgou. Sem esta bocada, o encontro inteiro e codigo morto.");
                })
                .thenExecute(() -> {
                    peixe.discard();
                    anzol.discard();
                })
                .thenSucceed();
    }

    /**
     * QUEM PUXA DEMAIS PERDE O PEIXE -- a metade que faz o cabo de guerra existir.
     *
     * <p>O pescador foge em linha reta, que e exatamente o que a regua do perfil
     * diz que tem de arrebentar a linha antes de o peixe cansar. Aqui isso deixa
     * de ser conta e vira comportamento.
     */
    @GameTest(template = ARENA, timeoutTicks = 600, batch = LOTE_LINHA)
    @PrefixGameTestTemplate(false)
    public static void fugirEmLinhaRetaArrebentaALinha(GameTestHelper helper) {
        encherPiscina(helper);
        MasterOfTheSwampEntity peixe = helper.spawn(
                MasterOfTheSwampEntity.registeredType(), new BlockPos(4, FUNDO, 5));
        ServerPlayer pescador = pescadorComVara(helper, 8, FUNDO, 5);
        FishingHook anzol = lancarAnzol(helper, pescador, 4, FUNDO, 5);
        BlockPos boia = helper.absolutePos(new BlockPos(4, FUNDO, 5));

        double[] partida = new double[1];

        helper.startSequence()
                .thenWaitUntil(() -> {
                    if (!anzol.isRemoved()) {
                        anzol.moveTo(boia.getX() + 0.5D, boia.getY() + 0.5D,
                                boia.getZ() + 0.5D, 0.0F, 0.0F);
                        anzol.setDeltaMovement(0.0D, 0.0D, 0.0D);
                    }
                    helper.assertTrue(peixe.estaFisgado(),
                            "o peixe nunca fisgou; o cenario nao chegou a medir a linha.");
                })
                .thenExecute(() -> partida[0] = pescador.distanceTo(peixe))
                .thenExecuteFor(120, () -> {
                    // Foge um pouco a cada tick, SEMPRE para longe do peixe: e o gesto de
                    // "puxar demais". teleportTo em vez de setDeltaMovement porque o peixe
                    // empurra o pescador, e um empurrao competindo com a fuga mediria a
                    // briga de forcas, e nao a regra.
                    pescador.teleportTo(pescador.getX() + 0.35D, pescador.getY(), pescador.getZ());
                })
                .thenExecute(() -> {
                    helper.assertFalse(peixe.estaFisgado(),
                            "o pescador fugiu " + (pescador.distanceTo(peixe) - partida[0])
                                    + " blocos em linha reta e a linha nao arrebentou. Se puxar"
                                    + " demais nao custa nada, o cabo de guerra vira espera.");
                    helper.assertFalse(peixe.estaCansado(),
                            "a linha arrebentou E o peixe ficou cansado: quem fugiu nao pode"
                                    + " ganhar a janela de captura de brinde.");
                    peixe.discard();
                    anzol.discard();
                })
                .thenSucceed();
    }

    /** Isca fora d'agua nao e isca: o anzol no seco nunca vira bocada. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = LOTE_SECO)
    @PrefixGameTestTemplate(false)
    public static void anzolNoSecoNaoFisga(GameTestHelper helper) {
        encherPiscina(helper);
        MasterOfTheSwampEntity peixe = helper.spawn(
                MasterOfTheSwampEntity.registeredType(), new BlockPos(3, FUNDO, 5));
        ServerPlayer pescador = pescadorComVara(helper, 8, SUPERFICIE + 1, 8);
        // PEDESTAL, e nao "um canto seco". A primeira versao pos o anzol num canto da
        // arena e o cenario reprovou: AGUA CORRE. A piscina espalha pelo piso inteiro e
        // afoga qualquer canto no mesmo nivel. O unico seco garantido e ACIMA da
        // superficie, e agua nao sobe.
        helper.setBlock(new BlockPos(8, SUPERFICIE + 1, 8), Blocks.STONE);
        FishingHook anzol = lancarAnzol(helper, pescador, 8, SUPERFICIE + 2, 8);

        helper.startSequence()
                .thenExecuteAfter(60, () -> {
                    helper.assertFalse(anzol.isInWater(),
                            "o anzol deste cenario precisa estar FORA da agua; se ele caiu na"
                                    + " piscina, o teste nao esta medindo o que diz medir.");
                    helper.assertFalse(peixe.estaFisgado(),
                            "o peixe fisgou um anzol que nao esta na agua.");
                    peixe.discard();
                    anzol.discard();
                })
                .thenSucceed();
    }

    // ---------------------------------------------------------------- cenario

    /** A arena nasce seca; sem piscina, nada deste mob acontece. */
    private static void encherPiscina(GameTestHelper helper) {
        for (int x = 1; x <= 9; x++) {
            for (int z = 1; z <= 9; z++) {
                for (int y = FUNDO; y <= SUPERFICIE; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }
    }

    private static ServerPlayer pescadorComVara(GameTestHelper helper, int x, int y, int z) {
        ServerPlayer pescador = helper.makeMockServerPlayerInLevel();
        BlockPos onde = helper.absolutePos(new BlockPos(x, y, z));
        pescador.teleportTo(onde.getX() + 0.5D, onde.getY(), onde.getZ() + 0.5D);
        // SEM A VARA O ANZOL SE DESCARTA: FishingHook.shouldStopFishing varre as duas
        // maos e some com o anzol no primeiro tick.
        pescador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FISHING_ROD));
        return pescador;
    }

    private static FishingHook lancarAnzol(GameTestHelper helper, ServerPlayer pescador,
            int x, int y, int z) {
        FishingHook anzol = new FishingHook(pescador, helper.getLevel(), 0, 0);
        BlockPos onde = helper.absolutePos(new BlockPos(x, y, z));
        anzol.moveTo(onde.getX() + 0.5D, onde.getY() + 0.5D, onde.getZ() + 0.5D, 0.0F, 0.0F);
        anzol.setDeltaMovement(0.0D, 0.0D, 0.0D);
        // Os cenarios aquaticos medem a regra de fisgada; a fisica de voo do
        // FishingHook nao pode deslocar a isca para fora da janela do teste.
        anzol.noPhysics = y == FUNDO;
        helper.getLevel().addFreshEntity(anzol);
        // A vara vanilla tambem registra o anzol no jogador. Como este fixture cria
        // o FishingHook diretamente, precisa reproduzir esse contrato explicitamente.
        pescador.fishing = anzol;
        return anzol;
    }

}
