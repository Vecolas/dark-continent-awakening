package com.darkcontinent.nenfoundation.structure;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class PostoAvancadoLootTest {
    @Test
    void tabelaDeSuprimentosDoPostoEstaDistribuida() {
        assertNotNull(PostoAvancadoLootTest.class.getResource(
                "/data/nenfoundation/loot_table/chests/hunter_outpost_supply.json"));
        assertNotNull(PostoAvancadoLootTest.class.getResource(
                "/data/nenfoundation/loot_table/chests/hunter_outpost_secret.json"));
    }
}
