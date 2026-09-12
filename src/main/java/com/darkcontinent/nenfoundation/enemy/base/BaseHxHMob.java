package com.darkcontinent.nenfoundation.enemy.base;

import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.api.HxHEnemy;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/** Base comum sem regras de dano; subclasses fornecem sensores e goals. */
public abstract class BaseHxHMob extends PathfinderMob implements HxHEnemy {
    private final EnemyMetadata metadata;
    private final EnemyBrain brain;
    private EnemyCombatState combatState = EnemyCombatState.IDLE;

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
}
