package com.darkcontinent.nenfoundation.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NenSyncMetricsTest {

    @Test
    void contaSomenteDeltasEntreguesEReinicia() {
        NenSyncMetrics metricas = new NenSyncMetrics();

        assertEquals(0L, metricas.deltasEnviados());
        metricas.registrarDeltaEnviado();
        metricas.registrarDeltaEnviado();
        assertEquals(2L, metricas.deltasEnviados());
        metricas.limpar();
        assertEquals(0L, metricas.deltasEnviados());
    }
}
