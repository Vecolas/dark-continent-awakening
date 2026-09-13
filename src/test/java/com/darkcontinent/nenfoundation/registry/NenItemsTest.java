package com.darkcontinent.nenfoundation.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

class NenItemsTest {
    @Test
    void bestiarioTemUmaPaginaDeIntroducaoEUmaPorCriatura() {
        var conteudo = BestiarioContent.conteudo();

        assertEquals("Associação Hunter", conteudo.author());
        assertEquals(9, conteudo.pages().size());
        assertTrue(conteudo.pages().get(0).get(false).getString().contains("BESTIÁRIO HUNTER"));
        assertTrue(conteudo.pages().stream()
                .map(pagina -> pagina.get(false))
                .map(Component::getString)
                .anyMatch(pagina -> pagina.contains("MASTER OF THE SWAMP")));
    }
}
