package com.darkcontinent.nenfoundation.client.vfx;

import com.darkcontinent.nenfoundation.api.SinalDeAura;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Detecta a borda de Ten dos outros jogadores sem inventar estado de Nen. */
final class DetectorDeAtivacaoDeTen {

    private final Map<Integer, SinalDeAura> anteriores = new HashMap<>();

    /**
     * A primeira observacao estabelece a base e nao toca som.
     *
     * <p>Sem essa regra, aproximar-se de alguem que ja estava em Ten seria
     * ouvido como uma ativacao que nunca aconteceu.
     */
    boolean atualizar(int entidadeId, SinalDeAura atual) {
        SinalDeAura anterior = this.anteriores.put(entidadeId, atual);
        return anterior == SinalDeAura.NENHUM && atual == SinalDeAura.TEN;
    }

    void reterSomente(Set<Integer> presentes) {
        this.anteriores.keySet().retainAll(presentes);
    }

    void limpar() {
        this.anteriores.clear();
    }
}
