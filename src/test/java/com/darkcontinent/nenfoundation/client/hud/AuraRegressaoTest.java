package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.*;
import com.darkcontinent.nenfoundation.client.NenClientCache;
import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** Regressoes de lag, clock, reconexao, maximo zero e delta malformado. */
class AuraRegressaoTest {
    private static DeltaDeRuntimeS2C delta(float atual, float maxima) {
        return new DeltaDeRuntimeS2C(atual, maxima, 1.0F, Set.of(), Map.of());
    }

    @Test void fracaoDeQuadroERecargaDeConfigNaoAlteramDeltaBruto() {
        var tick = new AtomicLong(10);
        var tempo = new AtomicInteger(4);
        var cache = new NenClientCache(tick::get, () -> false, tempo::get);
        cache.aoReceberDelta(delta(100, 100));
        cache.aoReceberDelta(delta(20, 100));
        tick.set(11);
        assertEquals(70, cache.auraInterpolada(0.5F));
        assertEquals(20, cache.auraOuZero());
        tempo.set(0);
        assertEquals(20, cache.auraInterpolada());
        assertEquals(2, cache.deltasRecebidos());
    }

    @Test void deltaInvalidoNaoContaminaCacheNemAnimacao() {
        var cache = new NenClientCache(() -> 1, () -> false);
        cache.aoReceberDelta(delta(5, 10));
        for (var invalido : new DeltaDeRuntimeS2C[]{delta(Float.NaN, 10), delta(1, Float.POSITIVE_INFINITY),
                delta(-1, 10), delta(11, 10), delta(0, -1)}) {
            assertThrows(IllegalArgumentException.class, () -> cache.aoReceberDelta(invalido));
            assertEquals(5, cache.auraOuZero());
            assertEquals(1, cache.deltasRecebidos());
        }
    }

    @Test void semReservaNaoEExaustaoELimpezaEsqueceSessaoAnterior() {
        var cache = new NenClientCache(() -> 1, () -> false, () -> 5);
        cache.aoReceberDelta(delta(0, 0));
        assertFalse(AuraHudProjection.de(cache).exausto());
        cache.aoReceberDelta(delta(0, 10));
        assertTrue(AuraHudProjection.de(cache).exausto());
        cache.limpar();
        assertFalse(AuraHudProjection.de(cache).disponivel());
        assertEquals(0, cache.deltasRecebidos());
        cache.aoReceberDelta(delta(8, 10));
        assertEquals(8, cache.auraInterpolada());
    }

    @Test void novoDeltaDuranteAnimacaoELagConvergemSemPacotesExtras() {
        var i = new AuraInterpolation(4);
        i.receber(delta(100, 100), 0);
        i.receber(delta(20, 100), 1);
        assertEquals(60, i.valorAtual(3));
        i.receber(delta(80, 100), 3);
        assertEquals(60, i.valorAtual(3));
        assertEquals(80, i.valorAtual(100));
        i.receber(delta(5, 10), 101);
        assertTrue(i.valorAtual(101) <= 10);
        i.receber(delta(0, 10), 102);
        assertEquals(0, i.valorAtual(102));
        i.receber(delta(9, 10), 0);
        assertEquals(9, i.valorAtual(0));
    }
}
