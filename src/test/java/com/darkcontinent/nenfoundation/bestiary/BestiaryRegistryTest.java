package com.darkcontinent.nenfoundation.bestiary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class BestiaryRegistryTest {
    @Test
    void foxbearEUmaEntradaNarrativaDoCatalogo() {
        var foxbear = BestiaryRegistry.get(BestiaryRegistry.FOXBEAR_ID);

        assertNotNull(foxbear);
        assertEquals(BestiaryCategory.WILDLIFE, foxbear.category());
        assertEquals(2, foxbear.threat());
        assertEquals(3, foxbear.studiedAt());
        assertEquals(8, foxbear.masteredAt());
        assertEquals("nenfoundation.bestiary.behavior.foxbear", foxbear.behaviorKey());
    }
}
