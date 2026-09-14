package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HunterExamProfilesTest {
    @Test
    void greatStampTemCargaEWeakPointSemVirarHPSponge() {
        var definition = HunterExamProfiles.greatStamp();
        assertEquals(70.0F, definition.attributes().maxHealth());
        assertEquals(4.0F, HunterExamProfiles.greatStampWeakPoints().multiplier("forehead"));
        assertTrue(HunterExamProfiles.greatStampCharge().windupTicks() > 0);
        assertTrue(HunterExamProfiles.greatStampCharge().recoveryTicks() > 0);
    }

    @Test
    void perfisIniciaisMantemFaccaoWildlife() {
        assertEquals("nenfoundation:frog_in_waiting", HunterExamProfiles.frogInWaiting().metadata().id().toString());
        assertEquals("nenfoundation:foxbear", HunterExamProfiles.foxbear().metadata().id().toString());
        assertEquals(com.darkcontinent.nenfoundation.enemy.api.EnemyFaction.WILDLIFE,
                HunterExamProfiles.foxbear().metadata().faction());
    }

    @Test
    void dummyTemWeakPointCoerenteComOResolver() {
        var resolver = HunterExamProfiles.dummyEnemyWeakPoint();
        assertEquals("eye", resolver.regiaoVulneravel());
        assertEquals(2.0F, HunterExamProfiles.dummyEnemyWeakPoints().multiplier("eye"));
        assertEquals(resolver.regiaoVulneravel(), resolver.resolver(0.9D, 1.0D));
        assertEquals(1.0F, HunterExamProfiles.dummyEnemyWeakPoints().multiplier(resolver.regiaoPadrao()));
    }
}
