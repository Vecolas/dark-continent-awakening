package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.KirikoEntity;
import com.darkcontinent.nenfoundation.registry.NenItems;
import com.darkcontinent.nenfoundation.server.BestiaryPlayerService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Prova em jogo do julgamento: paciência aprova e registra a captura narrativa. */
@GameTestHolder(NenFoundation.MOD_ID)
public final class KirikoGameTest {
    private static final String ARENA = "arena";
    private static final String LOTE_JULGAMENTO = "kiriko_julgamento";
    private static final String LOTE_INVENTARIO_CHEIO = "kiriko_julgamento_inventario";

    private KirikoGameTest() { }

    /** O candidato permanece visível; o Kiriko revela a forma verdadeira e aprova. */
    @GameTest(template = ARENA, timeoutTicks = 300, batch = LOTE_JULGAMENTO)
    @PrefixGameTestTemplate(false)
    public static void pacienciaAprovaERegistraNoBestiario(GameTestHelper helper) {
        KirikoEntity kiriko = helper.spawn(
                KirikoEntity.registeredType(), new BlockPos(4, 2, 5));
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        BlockPos posicao = helper.absolutePos(new BlockPos(7, 2, 5));
        jogador.teleportTo(posicao.getX() + 0.5D, posicao.getY(), posicao.getZ() + 0.5D);

        helper.startSequence()
                .thenExecuteFor(231, () -> jogador.teleportTo(
                        posicao.getX() + 0.5D, posicao.getY(), posicao.getZ() + 0.5D))
                .thenExecute(() -> {
                    helper.assertFalse(kiriko.estaDisfarcado(),
                            "o Kiriko ficou disfarçado depois da janela de aprovação");
                    helper.assertFalse(kiriko.foiReprovado(),
                            "um jogador que esperou em paz foi reprovado");
                    helper.assertTrue(BestiaryPlayerService.ler(jogador)
                                    .progress(NenFoundation.id("kiriko"))
                                    .captureFlags().contains("approved_by_patience"),
                            "a aprovação não registrou a condição narrativa no bestiário");
                    helper.assertTrue(jogador.getInventory().contains(
                                    new net.minecraft.world.item.ItemStack(NenItems.KIRIKO_FIELD_NOTE.get())),
                            "a aprovação não entregou a nota de campo própria da Kiriko");
                    kiriko.discard();
                })
                .thenSucceed();
    }

    /** Inventário cheio não apaga a recompensa: ela cai no chão no servidor. */
    @GameTest(template = ARENA, timeoutTicks = 300, batch = LOTE_INVENTARIO_CHEIO)
    @PrefixGameTestTemplate(false)
    public static void recompensaCaiComInventarioCheio(GameTestHelper helper) {
        KirikoEntity kiriko = helper.spawn(
                KirikoEntity.registeredType(), new BlockPos(4, 2, 5));
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        BlockPos posicao = helper.absolutePos(new BlockPos(7, 2, 5));
        jogador.teleportTo(posicao.getX() + 0.5D, posicao.getY(), posicao.getZ() + 0.5D);
        for (int slot = 0; slot < jogador.getInventory().getContainerSize(); slot++) {
            jogador.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        helper.assertTrue(jogador.getInventory().getFreeSlot() == -1,
                "o cenário não conseguiu deixar o inventário cheio");

        helper.startSequence()
                .thenExecuteFor(200, () -> jogador.teleportTo(
                        posicao.getX() + 0.5D, posicao.getY(), posicao.getZ() + 0.5D))
                .thenWaitUntil(() -> helper.assertFalse(kiriko.estaDisfarcado(),
                        "o Kiriko não concluiu a aprovação no prazo"))
                .thenExecute(() -> {
                    helper.assertTrue(BestiaryPlayerService.ler(jogador)
                                    .progress(NenFoundation.id("kiriko"))
                                    .captureFlags().contains("approved_by_patience"),
                            "a aprovação não chegou ao serviço de bestiário");
                    boolean notaNoInventario = jogador.getInventory().contains(
                            new ItemStack(NenItems.KIRIKO_FIELD_NOTE.get()));
                    boolean notaNoChao = !helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                                    new AABB(jogador.position(), jogador.position()).inflate(8.0D),
                                    item -> item.getItem().is(NenItems.KIRIKO_FIELD_NOTE.get()))
                            .isEmpty();
                    helper.assertTrue(notaNoInventario || notaNoChao,
                            "a recompensa da Kiriko sumiu com o inventário cheio; itemEntities="
                                    + helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                                    new AABB(jogador.position(), jogador.position()).inflate(64.0D),
                                    item -> true).size());
                    kiriko.discard();
                })
                .thenSucceed();
    }
}
