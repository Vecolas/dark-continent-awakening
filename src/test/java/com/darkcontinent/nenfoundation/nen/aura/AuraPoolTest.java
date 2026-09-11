package com.darkcontinent.nenfoundation.nen.aura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.aura.AuraPool.DirtyFlag;
import com.darkcontinent.nenfoundation.nen.aura.AuraPool.Parametros;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes dos invariantes do AuraPool.
 * Nao carregam o jogo; usam Parametros.de para injetar valores.
 */
class AuraPoolTest {

    private static final PersistentNenData PERFIL_ZERO = new PersistentNenData(
            1, false, NenCategory.UNDETERMINED, false,
            0.0D, 0.0D, 0.0D, Map.of(), Set.of(), Set.of(), Set.of());

    private static final PersistentNenData PERFIL_POTENCIAL_1 = new PersistentNenData(
            1, false, NenCategory.UNDETERMINED, false,
            1.0D, 0.0D, 0.0D, Map.of(), Set.of(), Set.of(), Set.of());

    private static final Parametros PARAMS = Parametros.de(100.0D, 1.0D, 0.10D);

    private AuraPool pool;

    @BeforeEach
    void novoPool() {
        this.pool = new AuraPool();
    }

    @Test
    @DisplayName("pool novo comeca com aura zero, output 100% e flag LIMPO")
    void poolNovoComecaZeradoEOutputCheio() {
        assertEquals(0.0D, pool.auraAtual());
        assertEquals(1.0F, pool.outputPercent());
        assertEquals(DirtyFlag.LIMPO, pool.dirty());
    }

    @Test
    @DisplayName("resetar para maximo preenche a aura, reseta output e levanta dirty AMBOS")
    void resetarPreencheAura() {
        pool.ajustarOutput(0.5F); // Altera o output antes do reset
        pool.resetarParaMaximo(PERFIL_ZERO, PARAMS);
        assertEquals(100.0D, pool.auraAtual(), 1e-9);
        assertEquals(1.0F, pool.outputPercent()); // Volta para 100%
        assertEquals(DirtyFlag.AMBOS, pool.dirty());
    }

    @Test
    @DisplayName("aura nunca ultrapassa o maximo apos regen")
    void regenNaoUltrapassaMaximo() {
        pool.resetarParaMaximo(PERFIL_ZERO, PARAMS);
        pool.marcarSincronizado();
        pool.tick(PERFIL_ZERO, PARAMS);
        assertEquals(100.0D, pool.auraAtual(), 1e-9);
        assertEquals(DirtyFlag.LIMPO, pool.dirty(), "regen em pool cheio nao deve sujar o flag");
    }

    @Test
    @DisplayName("aura nunca fica negativa ao gastar mais do que o saldo")
    void auraNuncaNegativa() {
        pool.resetarParaMaximo(PERFIL_ZERO, PARAMS);
        boolean resultado = pool.gastarAura(200.0D, PERFIL_ZERO, PARAMS);
        assertFalse(resultado, "gasto maior que saldo deve retornar false");
        assertEquals(100.0D, pool.auraAtual(), 1e-9, "aura nao deve mudar se o gasto foi negado");
    }

    @Test
    @DisplayName("gastar aura com saldo suficiente desconta e levanta dirty AURA")
    void gastarAuraComSaldo() {
        pool.resetarParaMaximo(PERFIL_ZERO, PARAMS);
        pool.marcarSincronizado();
        boolean resultado = pool.gastarAura(30.0D, PERFIL_ZERO, PARAMS);
        assertTrue(resultado);
        assertEquals(70.0D, pool.auraAtual(), 1e-9);
        assertEquals(DirtyFlag.AURA, pool.dirty());
    }

    @Test
    @DisplayName("gastar aura com custo zero retorna true sem sujar o flag")
    void gastarAuraCustoZeroNaoSuja() {
        pool.resetarParaMaximo(PERFIL_ZERO, PARAMS);
        pool.marcarSincronizado();
        assertTrue(pool.gastarAura(0.0D, PERFIL_ZERO, PARAMS));
        assertEquals(DirtyFlag.LIMPO, pool.dirty());
    }

    @Test
    @DisplayName("servidor nega gasto invalido: retorna false, aura intacta")
    void servidorNegaGastoInvalido() {
        boolean resultado = pool.gastarAura(1.0D, PERFIL_ZERO, PARAMS);
        assertFalse(resultado);
        assertEquals(0.0D, pool.auraAtual());
    }

    @Test
    @DisplayName("regen acumula corretamente em varias chamadas de tick")
    void regenAcumulaEmVariasTicks() {
        pool.tick(PERFIL_ZERO, PARAMS);
        pool.tick(PERFIL_ZERO, PARAMS);
        pool.tick(PERFIL_ZERO, PARAMS);
        assertEquals(3.0D, pool.auraAtual(), 1e-9);
    }

    @Test
    @DisplayName("auraMaxima escala com auraPotential do perfil")
    void auraMaximaEscalaComPotencial() {
        double maxZero = AuraPool.auraMaxima(PERFIL_ZERO, PARAMS);
        double maxPot1 = AuraPool.auraMaxima(PERFIL_POTENCIAL_1, PARAMS);
        assertEquals(100.0D, maxZero, 1e-9);
        assertEquals(200.0D, maxPot1, 1e-9);
    }

    @Test
    @DisplayName("marcarSincronizado limpa o flag")
    void marcarSincronizadoLimpaFlag() {
        pool.resetarParaMaximo(PERFIL_ZERO, PARAMS);
        assertTrue(pool.dirty() != DirtyFlag.LIMPO);
        pool.marcarSincronizado();
        assertEquals(DirtyFlag.LIMPO, pool.dirty());
    }

    @Test
    @DisplayName("exaustao detectada abaixo do limiar (10%)")
    void exaustaoDetectada() {
        pool.resetarParaMaximo(PERFIL_ZERO, PARAMS);
        pool.gastarAura(95.0D, PERFIL_ZERO, PARAMS);
        assertTrue(pool.emExaustaoDeAura(PERFIL_ZERO, PARAMS));
    }

    @Test
    @DisplayName("sem exaustao acima do limiar")
    void semExaustaoAcimaDoLimiar() {
        pool.resetarParaMaximo(PERFIL_ZERO, PARAMS);
        pool.gastarAura(50.0D, PERFIL_ZERO, PARAMS);
        assertFalse(pool.emExaustaoDeAura(PERFIL_ZERO, PARAMS));
    }

    @Test
    @DisplayName("ajustar output levanta dirty OUTPUT e clampa o valor")
    void ajustarOutputClampaESuja() {
        pool.marcarSincronizado();
        pool.ajustarOutput(0.5F);
        assertEquals(0.5F, pool.outputPercent());
        assertEquals(DirtyFlag.OUTPUT, pool.dirty());

        pool.marcarSincronizado();
        pool.ajustarOutput(1.5F); // maior que 1.0
        assertEquals(1.0F, pool.outputPercent());
        
        pool.ajustarOutput(-0.5F); // menor que 0.0
        assertEquals(0.0F, pool.outputPercent());
    }

    @Test
    @DisplayName("dirty flag unirCom e comutativo")
    void dirtyFlagUnirComComutativo() {
        assertEquals(DirtyFlag.OUTPUT, DirtyFlag.LIMPO.unirCom(DirtyFlag.OUTPUT));
        assertEquals(DirtyFlag.OUTPUT, DirtyFlag.OUTPUT.unirCom(DirtyFlag.LIMPO));
        assertEquals(DirtyFlag.AMBOS, DirtyFlag.AURA.unirCom(DirtyFlag.OUTPUT));
        assertEquals(DirtyFlag.AMBOS, DirtyFlag.OUTPUT.unirCom(DirtyFlag.AURA));
        assertEquals(DirtyFlag.AURA, DirtyFlag.AURA.unirCom(DirtyFlag.AURA));
    }
}