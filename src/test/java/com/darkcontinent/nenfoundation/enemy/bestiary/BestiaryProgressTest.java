package com.darkcontinent.nenfoundation.enemy.bestiary;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BestiaryProgressTest {
    @Test
    void descobertaAvancaSemRegredir() {
        BestiaryProgress progress = new BestiaryProgress()
                .advance("great_stamp", BestiaryStatus.OBSERVED)
                .advance("great_stamp", BestiaryStatus.FOUGHT)
                .advance("great_stamp", BestiaryStatus.OBSERVED)
                .advance("great_stamp", BestiaryStatus.STUDIED);
        assertEquals(BestiaryStatus.STUDIED, progress.status("great_stamp"));
        assertEquals(BestiaryStatus.UNKNOWN, progress.status("unknown"));
    }
}
