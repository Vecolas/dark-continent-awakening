package com.darkcontinent.nenfoundation.client.vfx;

import com.darkcontinent.nenfoundation.api.SinalDeAura;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Detecta as bordas de aura dos outros jogadores sem inventar estado de Nen.
 *
 * <p>UM DETECTOR SO PARA TODAS AS BORDAS. O AV4 acrescentou Ren -- entrada e
 * saida --, e a tentacao era um segundo detector ao lado. Dois mapas do mesmo
 * "qual era o sinal anterior deste jogador" sao duas verdades: bastaria um
 * {@code reterSomente} esquecido num deles para as bordas de Ren continuarem
 * sendo detectadas para alguem que ja saiu do alcance -- e o sintoma seria um
 * zumbido tocando sem dono.
 */
final class DetectorDeAtivacaoDeTen {

    private final Map<Integer, SinalDeAura> anteriores = new HashMap<>();

    /**
     * Registra o sinal atual e devolve o ANTERIOR.
     *
     * <p>{@code null} na primeira observacao, e isso e o contrato que impede o
     * som fantasma: aproximar-se de alguem que ja estava em Ten nao pode ser
     * ouvido como uma ativacao que nunca aconteceu. Quem chama trata o
     * {@code null} como "sem borda".
     */
    SinalDeAura registrar(int entidadeId, SinalDeAura atual) {
        return this.anteriores.put(entidadeId, atual);
    }

    /**
     * A primeira observacao estabelece a base e nao toca som.
     *
     * <p>Sem essa regra, aproximar-se de alguem que ja estava em Ten seria
     * ouvido como uma ativacao que nunca aconteceu.
     */
    boolean atualizar(int entidadeId, SinalDeAura atual) {
        SinalDeAura anterior = registrar(entidadeId, atual);
        return anterior == SinalDeAura.NENHUM && atual == SinalDeAura.TEN;
    }

    /** Se este par de sinais e uma ENTRADA em aura liberada. */
    static boolean entrouEmLiberacao(SinalDeAura anterior, SinalDeAura atual) {
        return anterior != null && !liberada(anterior) && liberada(atual);
    }

    /** Se este par de sinais e uma SAIDA de aura liberada. */
    static boolean saiuDeLiberacao(SinalDeAura anterior, SinalDeAura atual) {
        return anterior != null && liberada(anterior) && !liberada(atual);
    }

    /**
     * Ren e Ken contam como aura LIBERADA.
     *
     * <p>Os dois sao envelopes grandes de aura solta, e o cliente nao tem preset
     * proprio para Ken -- {@code EstadoVisualDeTerceiro} ja desenha os dois como
     * REN, separados apenas pela cor. Tratar so REN aqui deixaria Ken mudo, e um
     * Ken silencioso ao lado de um Ren sonoro seria lido como bug de audio.
     */
    static boolean liberada(SinalDeAura sinal) {
        return sinal == SinalDeAura.REN || sinal == SinalDeAura.KEN;
    }

    void reterSomente(Set<Integer> presentes) {
        this.anteriores.keySet().retainAll(presentes);
    }

    void limpar() {
        this.anteriores.clear();
    }
}
