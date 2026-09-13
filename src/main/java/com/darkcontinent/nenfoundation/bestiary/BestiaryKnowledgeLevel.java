package com.darkcontinent.nenfoundation.bestiary;

/** Escada narrativa do conhecimento, sem expor números de combate ao jogador. */
public enum BestiaryKnowledgeLevel {
    UNKNOWN,
    OBSERVED,
    FOUGHT,
    STUDIED,
    MASTERED;

    public boolean atLeast(BestiaryKnowledgeLevel other) {
        return ordinal() >= other.ordinal();
    }
}
