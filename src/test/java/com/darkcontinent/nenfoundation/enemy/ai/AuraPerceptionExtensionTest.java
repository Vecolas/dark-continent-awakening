package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuraPerceptionExtensionTest {
    @Test
    void extensaoNasceInerteSemCriarAutoridadeDeNen() {
        assertTrue(AuraPerceptionExtension.inerte().canDetect(null, null));
    }
}
