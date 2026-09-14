package com.darkcontinent.nenfoundation.enemy.ai;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Ciclo de percepção desacoplado da coleta de mundo.
 *
 * <p>O probe é fornecido pelo servidor e deve devolver um candidato já medido;
 * este controlador não varre entidades nem blocos. Sons entram por evento com
 * {@link #hear(UUID)}, e a memória expira mesmo quando nenhum scan ocorre.</p>
 */
public final class PerceptionController {
    private final TargetEvaluator evaluator;
    private final PerceptionBudget budget;
    private final int memoryDurationTicks;
    private UUID rememberedTarget;
    private int memoryRemaining;
    private UUID heardTarget;
    private PerceptionResult last = new PerceptionResult(Optional.empty(),
            new AwarenessInput(false, false, false, false, true, 0, false, false), false, false);

    public PerceptionController(TargetEvaluator evaluator, PerceptionBudget budget,
            int memoryDurationTicks) {
        if (memoryDurationTicks < 0) throw new IllegalArgumentException("memoria invalida");
        this.evaluator = java.util.Objects.requireNonNull(evaluator, "avaliador ausente");
        this.budget = java.util.Objects.requireNonNull(budget, "orcamento ausente");
        this.memoryDurationTicks = memoryDurationTicks;
    }

    /** Registra ruído relevante; não dispara busca global. */
    public void hear(UUID targetId) {
        heardTarget = java.util.Objects.requireNonNull(targetId, "id de som ausente");
    }

    public PerceptionResult tick(long gameTick, Supplier<PerceptionSnapshot> probe) {
        if (gameTick < 0 || probe == null) throw new IllegalArgumentException("tick ou probe invalido");
        boolean normal = gameTick % budget.normalIntervalTicks() == 0;
        boolean expensive = normal && gameTick % budget.expensiveIntervalTicks() == 0;
        if (!normal) {
            if (memoryRemaining > 0) memoryRemaining--;
            if (memoryRemaining == 0) rememberedTarget = null;
            UUID candidate = rememberedTarget != null ? rememberedTarget : heardTarget;
            last = new PerceptionResult(Optional.ofNullable(candidate),
                    new AwarenessInput(false, heardTarget != null, candidate != null,
                            false, candidate == null, memoryRemaining, false, false), false, false);
            return last;
        }
        PerceptionSnapshot snapshot = probe.get();
        TargetEvaluation evaluation = snapshot == null ? null : evaluator.evaluate(snapshot);
        boolean heardCandidate = evaluation != null && heardTarget != null
                && heardTarget.equals(evaluation.targetId());
        boolean signal = evaluation != null && evaluation.valid()
                && (evaluation.visible() || evaluation.audible() || heardCandidate);
        if (signal) {
            rememberedTarget = evaluation.targetId();
            memoryRemaining = memoryDurationTicks;
        } else if (memoryRemaining > 0) {
            memoryRemaining--;
            if (memoryRemaining == 0) rememberedTarget = null;
        } else {
            rememberedTarget = null;
        }
        boolean visible = evaluation != null && evaluation.valid() && evaluation.visible();
        boolean audible = (evaluation != null && evaluation.valid() && evaluation.audible())
                || heardTarget != null;
        boolean lost = rememberedTarget == null;
        last = new PerceptionResult(Optional.ofNullable(rememberedTarget),
                new AwarenessInput(visible, audible, rememberedTarget != null,
                        evaluation != null && evaluation.retreating(), lost,
                        memoryRemaining, false, false), true, expensive);
        heardTarget = null;
        return last;
    }

    public void reset() {
        rememberedTarget = null;
        memoryRemaining = 0;
        heardTarget = null;
        last = new PerceptionResult(Optional.empty(),
                new AwarenessInput(false, false, false, false, true, 0, false, false), false, false);
    }
}
