package com.darkcontinent.nenfoundation.nen.aura;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuraFormulasTest {

    @Test
    void perfilNaoDespertadoNaoRecebeReserva() {
        assertEquals(0.0D, AuraFormulas.maxima(PersistentNenData.NAO_DESPERTADO));
    }

    @Test
    void potencialPersistenteEAdicionadoSomenteAQuemDespertou() {
        PersistentNenData perfil = new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, true, NenCategory.ENHANCEMENT, true,
                42.5D, 0.0D, 0.0D, Map.of(), Set.of(), Set.of(), Set.of());

        assertEquals(142.5D, AuraFormulas.maxima(perfil, 100.0D), 0.000001D);
    }

    @Test
    void regeneracaoEExpressaNaUnidadeDoScheduler() {
        assertEquals(0.05D, AuraFormulas.regeneracaoPorTick(1.0D), 0.000001D);
    }
}
