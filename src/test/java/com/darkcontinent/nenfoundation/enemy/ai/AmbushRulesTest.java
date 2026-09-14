package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Sustenta que a emboscada e uma JANELA e nao um estado: o sapo so engole de dentro da
 * terra, com alvo, com a recarga zerada e com o alvo dentro do cilindro de gatilho.
 *
 * <p>Cada uma dessas condicoes solta num {@code if} da Goal viraria uma divergencia
 * silenciosa -- um sapo que agarra ja desenterrado, ou que agarra alguem duas plataformas
 * acima. Aqui elas sao uma fonte so, medivel sem mundo.</p>
 */
class AmbushRulesTest {
    /** Os mesmos numeros de {@code HunterExamProfiles.frogAmbushRules()}. */
    private static final AmbushRules REGRAS = new AmbushRules(2.5D, 2.0D, 100, 60);

    @Test
    void emboscadaExigeEstarEnterradoComAlvoESemRecargaPendente() {
        assertTrue(REGRAS.dispara(true, true, 1.0D, 0.0D, 0),
                "enterrado, com alvo em cima e sem recarga deveria disparar");
        assertFalse(REGRAS.dispara(false, true, 1.0D, 0.0D, 0),
                "ja desenterrado nao ha emboscada: o alvo esta vendo o sapo");
        assertFalse(REGRAS.dispara(true, false, 1.0D, 0.0D, 0),
                "sem alvo valido nao se engole ninguem");
        assertFalse(REGRAS.dispara(true, true, 1.0D, 0.0D, 1),
                "recarga ainda correndo bloqueia a emboscada em cadeia");
    }

    @Test
    void alvoForaDoCilindroDeGatilhoNaoDisparaEmboscada() {
        assertFalse(REGRAS.dispara(true, true, 2.51D, 0.0D, 0),
                "longe demais na horizontal: o sapo emergiria no vazio");
        assertFalse(REGRAS.dispara(true, true, 1.0D, 2.01D, 0),
                "alto demais acima do sapo esta fora do alcance da boca");
        assertFalse(REGRAS.dispara(true, true, 1.0D, -2.01D, 0),
                "diferenca NEGATIVA grande e o alvo POR BAIXO -- tambem esta fora, "
                        + "e e o caso que uma comparacao sem modulo deixa passar");
        assertFalse(REGRAS.dispara(true, true, 1.0D, -8.0D, 0),
                "alvo muito abaixo (fundo de caverna) nao pode ser engolido da superficie");
    }

    @Test
    void osLimitesDeRaioEDeAlturaSaoInclusivos() {
        assertTrue(REGRAS.dispara(true, true, 2.5D, 0.0D, 0),
                "exatamente o raio de gatilho ainda dispara");
        assertTrue(REGRAS.dispara(true, true, 0.0D, 2.0D, 0),
                "exatamente a altura de gatilho, por cima, ainda dispara");
        assertTrue(REGRAS.dispara(true, true, 0.0D, -2.0D, 0),
                "exatamente a altura de gatilho, por baixo, ainda dispara");
        assertTrue(REGRAS.dispara(true, true, 2.5D, 2.0D, 0),
                "as duas pontas juntas, na borda, continuam dentro do cilindro");
    }

    @Test
    void reenterrarExigeEstarForaDaTerraSemAlvoEDepoisDoTempo() {
        assertTrue(REGRAS.reenterra(false, false, 60),
                "fora da terra, sem alvo e com o tempo cumprido, o sapo volta a se esconder");
        assertFalse(REGRAS.reenterra(true, false, 60),
                "ja enterrado nao se enterra de novo");
        assertFalse(REGRAS.reenterra(false, true, 60),
                "com alvo por perto reenterrar abandonaria a caca no meio");
        assertFalse(REGRAS.reenterra(false, true, 9999),
                "nenhum tempo de espera autoriza reenterrar enquanto existe alvo");
        assertFalse(REGRAS.reenterra(false, false, 59),
                "um tick antes do tempo ainda nao reenterra");
    }

    @Test
    void oLimiteDeTempoParaReenterrarEInclusivo() {
        assertTrue(REGRAS.reenterra(false, false, 60), "exatamente o tempo cumprido ja reenterra");
    }

    @Test
    void regrasImpossiveisSaoRejeitadas() {
        assertThrows(IllegalArgumentException.class,
                () -> new AmbushRules(0.0D, 2.0D, 100, 60),
                "raio zero seria um emboscador que nunca emboscada -- e isso nao da erro nenhum");
        assertThrows(IllegalArgumentException.class,
                () -> new AmbushRules(-1.0D, 2.0D, 100, 60));
        assertThrows(IllegalArgumentException.class,
                () -> new AmbushRules(2.5D, 0.0D, 100, 60),
                "altura zero exigiria o alvo no pixel exato da boca");
        assertThrows(IllegalArgumentException.class,
                () -> new AmbushRules(2.5D, 2.0D, -1, 60),
                "recarga negativa e um numero que nenhum contador alcanca");
        assertThrows(IllegalArgumentException.class,
                () -> new AmbushRules(2.5D, 2.0D, 100, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new AmbushRules(Double.NaN, 2.0D, 100, 60));
        assertThrows(IllegalArgumentException.class,
                () -> new AmbushRules(2.5D, Double.NaN, 100, 60));
        assertThrows(IllegalArgumentException.class,
                () -> new AmbushRules(Double.POSITIVE_INFINITY, 2.0D, 100, 60));
    }
}
