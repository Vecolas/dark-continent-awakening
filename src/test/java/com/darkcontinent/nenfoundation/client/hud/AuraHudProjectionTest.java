package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.client.NenClientCache;
import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuraHudProjectionTest {

    @Test
    void ausenciaNaoEConfundidaComAuraZero() {
        NenClientCache cache = new NenClientCache(() -> 0L, () -> false);
        AuraHudProjection ausente = AuraHudProjection.de(cache);
        assertFalse(ausente.disponivel());
        assertFalse(ausente.exausto());

        cache.aoReceberDelta(new DeltaDeRuntimeS2C(0.0F, 100.0F, 1.0F, Set.of(), Map.of()));
        AuraHudProjection exausto = AuraHudProjection.de(cache);
        assertTrue(exausto.disponivel());
        assertTrue(exausto.exausto());
        assertEquals(0.0F, exausto.fracao());
    }

    @Test
    void fracaoFicaEntreZeroEUm() {
        NenClientCache cache = new NenClientCache(() -> 0L, () -> false);
        cache.aoReceberDelta(new DeltaDeRuntimeS2C(100.0F, 100.0F, 1.0F, Set.of(), Map.of()));
        assertEquals(1.0F, AuraHudProjection.de(cache).fracao());
    }

    @Test
    void outputVisualAnimaSemAlterarValorConfirmado() {
        long[] tick = {0L};
        NenClientCache cache = new NenClientCache(() -> tick[0], () -> false, () -> 5, () -> 2);
        cache.aoReceberDelta(new DeltaDeRuntimeS2C(100.0F, 100.0F, 0.5F, Set.of(), Map.of()));
        tick[0] = 1;
        cache.aoReceberDelta(new DeltaDeRuntimeS2C(100.0F, 100.0F, 1.0F, Set.of(), Map.of()));
        tick[0] = 2;

        AuraHudProjection projecao = AuraHudProjection.de(cache);
        assertEquals(1.0F, projecao.outputPercent());
        assertEquals(0.75F, projecao.outputVisual());
    }
}
