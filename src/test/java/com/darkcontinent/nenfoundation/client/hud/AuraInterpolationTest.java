package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuraInterpolationTest {

    @Test
    void primeiroDeltaNaoFazAnimacaoPartindoDoZero() {
        AuraInterpolation interpolacao = new AuraInterpolation(5L);
        interpolacao.receber(delta(100.0F), 20L);
        assertEquals(100.0F, interpolacao.valorAtual(20L));
    }

    @Test
    void novoDeltaChegaAoAlvoSemPedirNovoPacote() {
        AuraInterpolation interpolacao = new AuraInterpolation(5L);
        interpolacao.receber(delta(100.0F), 20L);
        interpolacao.receber(delta(50.0F), 25L);

        assertEquals(100.0F, interpolacao.valorAtual(25L));
        assertEquals(80.0F, interpolacao.valorAtual(27L));
        assertEquals(50.0F, interpolacao.valorAtual(30L));
    }

    private static DeltaDeRuntimeS2C delta(float aura) {
        return new DeltaDeRuntimeS2C(aura, 100.0F, Set.of(), Map.of());
    }
}
