package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Sustenta que o ponto fraco e geometria decidida no servidor: so conta acerto alto E de frente.
 */
class WeakPointResolverTest {
    private static final WeakPointResolver RESOLVER = new WeakPointResolver("forehead", "body", 0.62D, 0.5D);

    @Test
    void soAcertoAltoEDeFrenteValeComoPontoFraco() {
        assertEquals("forehead", RESOLVER.resolver(0.9D, 0.9D), "alto e de frente e a testa");
        assertEquals("body", RESOLVER.resolver(0.9D, -0.9D), "alto pelas costas e corpo");
        assertEquals("body", RESOLVER.resolver(0.2D, 0.9D), "de frente mas baixo e corpo");
        assertEquals("body", RESOLVER.resolver(0.2D, -0.9D));
    }

    @Test
    void osLimitesDeAlturaECossenoSaoInclusivos() {
        assertEquals("forehead", RESOLVER.resolver(0.62D, 0.5D), "exatamente no limite ainda e testa");
        assertEquals("body", RESOLVER.resolver(0.619D, 0.5D), "um fio abaixo da altura minima ja e corpo");
        assertEquals("body", RESOLVER.resolver(0.62D, 0.499D), "um fio fora do cone frontal ja e corpo");
    }

    @Test
    void argumentosNaoNumericosSaoRejeitados() {
        assertThrows(IllegalArgumentException.class, () -> RESOLVER.resolver(Double.NaN, 0.9D));
        assertThrows(IllegalArgumentException.class, () -> RESOLVER.resolver(0.9D, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> RESOLVER.resolver(Double.POSITIVE_INFINITY, 0.9D));
        assertThrows(IllegalArgumentException.class, () -> RESOLVER.resolver(0.9D, Double.NEGATIVE_INFINITY));
    }

    @Test
    void resolversImpossiveisSaoRejeitados() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeakPointResolver("body", "body", 0.62D, 0.5D),
                "regiao vulneravel igual a padrao apagaria o ponto fraco em silencio");
        assertThrows(IllegalArgumentException.class,
                () -> new WeakPointResolver("forehead", "body", -0.1D, 0.5D));
        assertThrows(IllegalArgumentException.class,
                () -> new WeakPointResolver("forehead", "body", 1.5D, 0.5D));
        assertThrows(IllegalArgumentException.class,
                () -> new WeakPointResolver("forehead", "body", 0.62D, 1.5D));
        assertThrows(IllegalArgumentException.class,
                () -> new WeakPointResolver("forehead", "body", 0.62D, -1.5D));
        assertThrows(IllegalArgumentException.class,
                () -> new WeakPointResolver("forehead", "body", Double.NaN, 0.5D));
    }
}
