package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;
import java.util.function.Predicate;

/**
 * Uma sessao de transporte. Confirma somente apos entrega aceita e compara o
 * float realmente enviado: variacao invisivel na rede nao merece outro pacote.
 */
final class ControleDeSync {
    private DeltaDeRuntimeS2C ultimo;
    private long tickDoEnvio;

    boolean enviar(RuntimeNenState estado, long tick, int intervalo,
            Predicate<DeltaDeRuntimeS2C> transporte) {
        if (intervalo < 1) throw new IllegalArgumentException("intervalo deve ser positivo");
        if (!estado.auraSuja()) return false;
        long revisao = estado.revisao();
        DeltaDeRuntimeS2C delta = NenSyncService.criarDelta(estado);
        if (delta.equals(this.ultimo)) {
            estado.confirmarSincronizacao(revisao);
            return false;
        }
        if (this.ultimo != null && tick >= this.tickDoEnvio
                && tick - this.tickDoEnvio < intervalo) return false;
        if (!transporte.test(delta)) return false;
        this.ultimo = delta;
        this.tickDoEnvio = tick;
        estado.confirmarSincronizacao(revisao);
        return true;
    }
}
