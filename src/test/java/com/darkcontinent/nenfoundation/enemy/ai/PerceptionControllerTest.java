package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PerceptionControllerTest {
    private static final UUID TARGET = UUID.randomUUID();
    private static final TargetEvaluator EVALUATOR = new TargetEvaluator(
            EnemyFaction.CHIMERA_ANT, "minecraft:overworld", 16.0D, 60.0D,
            FactionRelations.padrao());

    @Test
    void visaoExigeFacaoDimensaoDistanciaConeELos() {
        PerceptionSnapshot boa = snapshot(true, 0.9D, true, false);
        TargetEvaluation result = EVALUATOR.evaluate(boa);
        assertTrue(result.valid());
        assertTrue(result.visible());
        assertFalse(EVALUATOR.evaluate(snapshot(true, 0.2D, true, false)).visible());
        assertFalse(EVALUATOR.evaluate(snapshot(true, 0.9D, false, false)).visible());
        assertFalse(EVALUATOR.evaluate(new PerceptionSnapshot(TARGET, "minecraft:the_nether", 2,
                1, true, false, false, true, true, EnemyFaction.CIVILIAN)).valid());
    }

    @Test
    void memoriaExpiraEAudicaoNaoFazScanGlobal() {
        PerceptionController controller = new PerceptionController(EVALUATOR,
                new PerceptionBudget(5, 20), 2);
        assertTrue(controller.tick(0, () -> snapshot(true, 1, true, false)).normalScan());
        controller.hear(TARGET);
        PerceptionResult ouvido = controller.tick(1,
                () -> { throw new AssertionError("scan indevido"); });
        assertFalse(ouvido.normalScan());
        assertTrue(ouvido.awareness().targetAudible());
        assertTrue(controller.tick(5, () -> null).awareness().targetLost());
    }

    @Test
    void budgetDistingueScanCaroEEntradasInvalidas() {
        PerceptionController controller = new PerceptionController(EVALUATOR,
                new PerceptionBudget(5, 20), 2);
        assertTrue(controller.tick(20, () -> snapshot(true, 1, true, false)).expensiveScan());
        assertThrows(IllegalArgumentException.class, () -> new PerceptionBudget(0, 20));
        assertThrows(IllegalArgumentException.class, () -> new PerceptionBudget(4, 20));
        assertThrows(IllegalArgumentException.class, () -> new PerceptionBudget(5, 19));
        assertThrows(IllegalArgumentException.class, () -> new PerceptionBudget(10, 41));
        assertThrows(IllegalArgumentException.class, () -> new PerceptionSnapshot(TARGET,
                "minecraft:overworld", 1, 2, true, false, false, true, true, EnemyFaction.CIVILIAN));
    }

    @Test
    void candidatoAtrasDaParedeNaoRenovaMemoriaSemSinal() {
        PerceptionController controller = new PerceptionController(EVALUATOR,
                new PerceptionBudget(5, 20), 2);
        controller.tick(0, () -> snapshot(true, 1, true, false));
        PerceptionResult oculto = controller.tick(1, () -> snapshot(true, 1, false, false));
        assertTrue(oculto.awareness().memoryTicksRemaining() == 1,
                "LOS ausente nao deveria renovar a memoria");
        assertTrue(controller.tick(2, () -> null).awareness().targetLost(),
                "memoria sem visao/audicao deveria expirar");
    }

    @Test
    void ruidoPreservaUuidAteOProximoScanNormal() {
        PerceptionController controller = new PerceptionController(EVALUATOR,
                new PerceptionBudget(5, 20), 2);
        controller.hear(TARGET);
        PerceptionResult ouvido = controller.tick(1, () -> {
            throw new AssertionError("ruido nao deve varrer o mundo");
        });
        assertTrue(ouvido.targetId().orElseThrow().equals(TARGET));
        assertTrue(ouvido.awareness().targetAudible());
    }

    @Test
    void resetLimpaMemoriaEEventosParaUnloadOuDimensao() {
        PerceptionController controller = new PerceptionController(EVALUATOR,
                new PerceptionBudget(5, 20), 20);
        controller.tick(0, () -> snapshot(true, 1, true, false));
        controller.hear(TARGET);
        controller.reset();
        PerceptionResult vazio = controller.tick(1, () -> {
            throw new AssertionError("reset nao deve forcar scan");
        });
        assertTrue(vazio.targetId().isEmpty());
        assertTrue(vazio.awareness().targetLost());
    }

    @Test
    void sessentaEQuatroMobsNaoVarremTodosOsTicks() {
        var controllers = java.util.stream.IntStream.range(0, 64)
                .mapToObj(i -> new PerceptionController(EVALUATOR,
                        new PerceptionBudget(5, 20), 40)).toList();
        AtomicInteger scans = new AtomicInteger();
        for (long tick = 0; tick < 40; tick++) {
            for (PerceptionController controller : controllers) {
                controller.tick(tick, () -> {
                    scans.incrementAndGet();
                    return null;
                });
            }
        }
        assertEquals(64 * 8, scans.get(),
                "o orçamento normal de 5 ticks não pode virar scan por entidade a cada tick");
        assertTrue(scans.get() < 64 * 40,
                "o cenário de 64 inimigos não pode degenerar em 2560 scans por janela");
    }

    private static PerceptionSnapshot snapshot(boolean alive, double dot, boolean los, boolean retreating) {
        return new PerceptionSnapshot(TARGET, "minecraft:overworld", 8.0D, dot, los, false,
                retreating, alive, true, EnemyFaction.CIVILIAN);
    }
}
