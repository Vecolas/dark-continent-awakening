package com.darkcontinent.nenfoundation.client;

import static org.junit.jupiter.api.Assertions.*;

import com.darkcontinent.nenfoundation.client.screen.PaginacaoDaFicha;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Limites de pagina cobrados em varias capacidades, sem depender da resolucao da maquina. */
class PaginacaoDaFichaTest {
    @Test
    void todoItemEAlcancavelUmaVezSemVazarDoPainel() {
        for (int capacidade : new int[] {1, 3, 4, 8, 20}) {
            Set<Integer> vistos = new HashSet<>();
            var primeira = new PaginacaoDaFicha(513, capacidade, 0);
            for (int pagina = 0; pagina < primeira.paginas(); pagina++) {
                var p = new PaginacaoDaFicha(513, capacidade, pagina);
                assertTrue(p.fim() - p.inicio() <= capacidade);
                for (int i = p.inicio(); i < p.fim(); i++) assertTrue(vistos.add(i));
            }
            assertEquals(513, vistos.size());
            assertTrue(vistos.contains(0));
            assertTrue(vistos.contains(512));
        }
    }

    @Test
    void lockEResizeRecolocamPaginaDentroDaLista() {
        assertEquals(0, new PaginacaoDaFicha(0, 4, 50).pagina());
        assertEquals(0, new PaginacaoDaFicha(20, 4, -1).pagina());
        assertEquals(1, new PaginacaoDaFicha(5, 4, 50).pagina());
        assertEquals(1, new PaginacaoDaFicha(0, 4, 0).paginas());
        assertEquals(0, new PaginacaoDaFicha(0, 4, 0).fim());
        assertEquals(Integer.MAX_VALUE, new PaginacaoDaFicha(Integer.MAX_VALUE, 8, Integer.MAX_VALUE).fim());
    }

    @Test
    void capacidadeInvalidaNaoViraDivisaoPorZero() {
        assertThrows(IllegalArgumentException.class, () -> new PaginacaoDaFicha(1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PaginacaoDaFicha(-1, 1, 0));
    }
}
