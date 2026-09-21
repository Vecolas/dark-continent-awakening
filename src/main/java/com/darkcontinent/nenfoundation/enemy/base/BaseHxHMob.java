package com.darkcontinent.nenfoundation.enemy.base;

import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyPerceptionService;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyPerceptionSnapshot;
import com.darkcontinent.nenfoundation.enemy.ai.PerceptionGeometry;
import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.api.HxHEnemy;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Base comum sem regras de dano; subclasses fornecem sensores e goals. */
public abstract class BaseHxHMob extends PathfinderMob implements HxHEnemy {
    private final EnemyMetadata metadata;
    private final EnemyBrain brain;
    private final EnemyPerceptionService perception = new EnemyPerceptionService();
    private EnemyCombatState combatState = EnemyCombatState.IDLE;
    private Vec3 territoryCenter;
    private EnemyPerceptionSnapshot perceptionSnapshot = EnemyPerceptionSnapshot.empty();

    protected BaseHxHMob(EntityType<? extends PathfinderMob> type, Level level,
            EnemyMetadata metadata, AwarenessTuning tuning) {
        super(type, level);
        this.metadata = metadata;
        this.brain = new EnemyBrain(tuning);
    }

    @Override public final EnemyMetadata enemyMetadata() { return metadata; }
    protected final EnemyBrain enemyBrain() { return brain; }
    protected final void combatState(EnemyCombatState state) { this.combatState = state; }
    @Override public final EnemyCombatState combatState() { return combatState; }
    @Override public final EnemyAwarenessState awarenessState() { return brain.state(); }

    /**
     * A aquisicao de alvo acontece aqui, antes dos sensores especificos da
     * especie. Novas aquisicoes passam pelo servico; alvos ja atribuidos por
     * dano/Goal sao rechecados por dimensao e alcance, enquanto os sensores
     * especificos continuam lendo cone e linha de visao.
     */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        if (territoryCenter == null) territoryCenter = position();
        perceptionSnapshot = perception.tick(this, metadata, territoryCenter);
        LivingEntity target = getTarget();
        var tuning = NenConfig.enemyPerceptionTuning();
        double targetRange = Math.max(tuning.visionRange(), tuning.hearingRange());
        if (target != null && (!target.isAlive() || target.level() != level()
                || distanceTo(target) > targetRange)) {
            setTarget(null);
        }
        if (perceptionSnapshot.scannedThisTick()) {
            if (perceptionSnapshot.target() != null) {
                setTarget(perceptionSnapshot.target());
            }
        }
    }

    protected final EnemyPerceptionSnapshot perceptionSnapshot() { return perceptionSnapshot; }

    protected final boolean isPerceivedVisible(LivingEntity target) {
        if (target == null || !target.isAlive() || target.level() != level()) return false;
        if (perceptionSnapshot.target() == target) return perceptionSnapshot.targetVisible();
        var tuning = NenConfig.enemyPerceptionTuning();
        return distanceTo(target) <= tuning.visionRange()
                && PerceptionGeometry.insideCone(getEyePosition(), getLookAngle(),
                target.getEyePosition(), tuning.visionRange(), tuning.visionHalfAngleDegrees())
                && hasLineOfSight(target);
    }

    protected final boolean isPerceivedAudible(LivingEntity target) {
        return target != null && perceptionSnapshot.target() == target
                && perceptionSnapshot.targetAudible();
    }
}
