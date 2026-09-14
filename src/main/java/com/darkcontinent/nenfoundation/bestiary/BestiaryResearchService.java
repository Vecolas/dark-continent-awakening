package com.darkcontinent.nenfoundation.bestiary;

/** Regra pura de pesquisa; não conhece jogador, item, rede ou tela. */
public final class BestiaryResearchService {
    private BestiaryResearchService() { }

    public static BestiaryProgress aplicar(BestiaryProgress atual, BestiaryEntryDefinition entry,
            int pontos, BestiaryKnowledgeLevel minimo) {
        var resultado = atual.withResearchPoints(pontos,
                minimo == BestiaryKnowledgeLevel.STUDIED ? atual.knowledgeLevel() : minimo);
        if (resultado.researchPoints() >= entry.masteredAt()) {
            return resultado.withResearchPoints(0, BestiaryKnowledgeLevel.MASTERED);
        }
        if (resultado.researchPoints() >= entry.studiedAt()) {
            return resultado.withResearchPoints(0, BestiaryKnowledgeLevel.STUDIED);
        }
        return resultado;
    }
}
