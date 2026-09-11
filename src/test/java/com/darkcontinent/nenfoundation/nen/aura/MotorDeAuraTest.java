package com.darkcontinent.nenfoundation.nen.aura;

import static org.junit.jupiter.api.Assertions.*;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Compara controles e muta a fonte de tuning com o MESMO jogador vivo. */
class MotorDeAuraTest {
    private static class Ajustes implements ParametrosDeAura {
        double maxima = 100, regen = 20, output = 10;
        public double maximaBase() { return maxima; }
        public double regeneracaoPorSegundo() { return regen; }
        public double outputBase() { return output; }
    }
    private static PersistentNenData perfil(double potencial, double output) {
        return new PersistentNenData(1, true, NenCategory.UNDETERMINED, false,
                potencial, 0, output, Map.of(), Set.of(), Set.of(), Set.of());
    }

    @Test void recargaAlteraJogadorExistenteSemCriarAuraPorAumentoDoMaximo() {
        var ajustes = new Ajustes();
        var estado = new RuntimeNenState();
        var motor = new MotorDeAura(estado, ajustes, () -> true);
        var perfil = perfil(5, 0);
        for (int i = 0; i < 20; i++) motor.tick(perfil);
        assertEquals(20, estado.auraAtual());
        ajustes.regen = 40;
        motor.tick(perfil);
        assertEquals(22, estado.auraAtual());
        ajustes.maxima = 200;
        motor.atualizar(perfil);
        assertEquals(205, estado.auraMaxima());
        assertEquals(22, estado.auraAtual());
        ajustes.maxima = 0;
        motor.atualizar(perfil);
        assertEquals(5, estado.auraAtual());
        assertEquals(22, motor.medida().auraRecuperada());
    }

    @Test void reservaAltaNaoIgnoraOutputENegativasNaoGeramCredito() {
        var estado = new RuntimeNenState(100);
        var motor = new MotorDeAura(estado, new Ajustes(), () -> true);
        var perfil = perfil(0, 0);
        assertEquals(MotorDeAura.Gasto.PERMITIDO, motor.gastar(perfil, 8, 10));
        assertEquals(MotorDeAura.Gasto.OUTPUT_EXCEDIDO, motor.gastar(perfil, 3, 10));
        assertEquals(92, estado.auraAtual());
        assertEquals(MotorDeAura.Gasto.PERMITIDO, motor.gastar(perfil, 2, 10));
        assertEquals(MotorDeAura.Gasto.PERMITIDO, motor.gastar(perfil, 10, 11));
        for (double valor : new double[]{-1, 0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertEquals(MotorDeAura.Gasto.INVALIDO, motor.gastar(perfil, valor, 12));
        }
        assertEquals(80, estado.auraAtual());
        assertEquals(3, motor.medida().gastos());
        assertEquals(6, motor.medida().recusas());
    }

    @Test void neutralidadeExaustaoEIsolamentoDeSessao() {
        var a = new RuntimeNenState();
        var b = new RuntimeNenState();
        var motorA = new MotorDeAura(a, new Ajustes(), () -> true);
        var motorB = new MotorDeAura(b, new Ajustes(), () -> false);
        motorA.tick(PersistentNenData.NAO_DESPERTADO);
        assertEquals(0, a.auraMaxima());
        assertFalse(a.exausto());
        motorA.tick(perfil(0, 0));
        assertEquals(MotorDeAura.Gasto.PERMITIDO, motorA.gastar(perfil(0, 0), 1, 1));
        assertTrue(a.exausto());
        assertEquals(1, motorA.medida().exaustoes());
        motorB.tick(perfil(0, 0));
        assertEquals(1, b.auraAtual());
        assertEquals(0, motorB.medida().ticks());
        assertEquals(0, a.auraAtual());
    }

    @Test void perfilNaoRepresentavelNaRedeFalhaAntesDeAlterarPool() {
        var a = new RuntimeNenState();
        var motor = new MotorDeAura(a, new Ajustes(), () -> true);
        for (double valor : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1, Double.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> motor.atualizar(perfil(valor, 0)));
            assertEquals(0, a.auraMaxima());
        }
    }
}
