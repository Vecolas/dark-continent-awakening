package com.darkcontinent.nenfoundation.enemy.faction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.FactionRelation;
import org.junit.jupiter.api.Test;

class FactionRelationsTest {
    @Test
    void relacaoPadraoNaoTransformaTodaFaunaEmHostil() {
        FactionRelations relations = FactionRelations.padrao();
        assertEquals(FactionRelation.ALLY, relations.relation(EnemyFaction.WILDLIFE, EnemyFaction.WILDLIFE));
        assertEquals(FactionRelation.NEUTRAL, relations.relation(EnemyFaction.WILDLIFE, EnemyFaction.CIVILIAN));
        assertEquals(FactionRelation.PREY, relations.relation(EnemyFaction.CHIMERA_ANT, EnemyFaction.CIVILIAN));
        assertEquals(FactionRelation.HOSTILE, relations.relation(EnemyFaction.CHIMERA_ANT, EnemyFaction.HUNTER_ASSOCIATION));
        assertEquals(FactionRelation.HOSTILE,
                relations.relation(EnemyFaction.WILDLIFE, EnemyFaction.HUNTER_ASSOCIATION));
    }
}
