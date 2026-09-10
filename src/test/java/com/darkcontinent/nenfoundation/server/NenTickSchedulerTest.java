package com.darkcontinent.nenfoundation.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NenTickSchedulerTest {

    @Test
    @DisplayName("subsistema registrado recebe tick sem criar laco proprio")
    void subsistemaRegistradoRecebeTick() {
        NenTickScheduler.Despachante<String> scheduler = new NenTickScheduler.Despachante<>();
        RuntimeNenState estado = new RuntimeNenState();
        AtomicInteger chamadas = new AtomicInteger();
        AtomicReference<String> jogadorRecebido = new AtomicReference<>();
        AtomicReference<RuntimeNenState> estadoRecebido = new AtomicReference<>();

        NenTickScheduler.Registro registro = scheduler.registrar((jogador, runtime) -> {
            chamadas.incrementAndGet();
            jogadorRecebido.set(jogador);
            estadoRecebido.set(runtime);
        });

        scheduler.executar("jogador", estado);

        assertEquals(1, chamadas.get());
        assertEquals("jogador", jogadorRecebido.get());
        assertSame(estado, estadoRecebido.get());

        registro.close();
        scheduler.executar("jogador", estado);
        assertEquals(1, chamadas.get(), "registro fechado nao pode continuar recebendo tick");
    }

    @Test
    @DisplayName("um despacho entrega o mesmo tick a todos os subsistemas")
    void despachoAtendeTodosOsSubsistemas() {
        NenTickScheduler.Despachante<String> scheduler = new NenTickScheduler.Despachante<>();
        AtomicInteger chamadas = new AtomicInteger();
        scheduler.registrar((jogador, estado) -> chamadas.incrementAndGet());
        scheduler.registrar((jogador, estado) -> chamadas.incrementAndGet());

        scheduler.executar("jogador", new RuntimeNenState());

        assertEquals(2, chamadas.get());
    }
}
