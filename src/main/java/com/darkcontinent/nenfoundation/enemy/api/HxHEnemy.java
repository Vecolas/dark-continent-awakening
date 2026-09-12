package com.darkcontinent.nenfoundation.enemy.api;

/** Contrato minimo para entidades HxH; nao expõe implementacao de IA ao cliente. */
public interface HxHEnemy {
    EnemyMetadata enemyMetadata();
    EnemyCombatState combatState();
    EnemyAwarenessState awarenessState();

    default boolean isTerritorial() {
        return enemyMetadata().territorial();
    }
}
