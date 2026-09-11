package com.darkcontinent.nenfoundation.nen.aura;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuraFormulasTest {
    private static final ParametrosDeAura PARAMETROS = new ParametrosDeAura() {
        public double maximaBase() { return 100; }
        public double regeneracaoPorSegundo() { return 1; }
        public double outputBase() { return 10; }
        @Override public double multiplicadorMaximoDeRegeneracao() { return 3.0D; }
    };

    @Test
    void perfilNaoDespertadoNaoRecebeReserva() {
        assertEquals(0.0D, AuraFormulas.maxima(PersistentNenData.NAO_DESPERTADO, PARAMETROS));
    }

    @Test
    void potencialPersistenteEAdicionadoSomenteAQuemDespertou() {
        PersistentNenData perfil = new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, true, NenCategory.ENHANCEMENT, true,
                42.5D, 0.0D, 0.0D, Map.of(), Set.of(), Set.of(), Set.of());

        assertEquals(PARAMETROS.maximaBase() + perfil.auraPotential(), AuraFormulas.maxima(perfil, PARAMETROS));
    }

    @Test
    void regeneracaoEExpressaNaUnidadeDoScheduler() {
        assertEquals(PARAMETROS.regeneracaoPorSegundo(), AuraFormulas.regeneracaoPorTick(PARAMETROS) * 20);
    }
}
