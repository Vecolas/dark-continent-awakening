package com.darkcontinent.nenfoundation.server;

/** Contadores de transporte do servidor, reiniciados a cada instancia de server. */
public final class NenSyncMetrics {

    private long deltasEnviados;

    public void registrarDeltaEnviado() {
        this.deltasEnviados++;
    }

    public long deltasEnviados() {
        return this.deltasEnviados;
    }

    public void limpar() {
        this.deltasEnviados = 0L;
    }
}
