package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O portao da ocultacao do vanilla.
 *
 * <p>O caso que ele existe para impedir nao levanta excecao: com a opcao
 * ligada e a HUD de Nen ainda nao na tela, o jogador ficaria sem NENHUM
 * indicador de vida -- e o relato seria "minha vida sumiu".
 */
class OcultacaoDoVanillaTest {

    private static final ResourceLocation OUTRA =
            ResourceLocation.withDefaultNamespace("hotbar");

    @Test
    @DisplayName("sem a HUD de Nen na tela, nada e ocultado -- nem com tudo ligado")
    void semSubstitutoOsCoracoesVoltam() {
        assertFalse(OcultacaoDoVanilla.deveOcultar(VanillaGuiLayers.PLAYER_HEALTH,
                        true, true, false),
                "ocultar sem substituto deixa o jogador sem indicador de vida nenhum");
        assertFalse(OcultacaoDoVanilla.deveOcultar(VanillaGuiLayers.FOOD_LEVEL,
                true, true, false));
    }

    @Test
    @DisplayName("com a HUD na tela, cada chave oculta so a sua camada")
    void cadaChaveOcultaAPropriaCamada() {
        assertTrue(OcultacaoDoVanilla.deveOcultar(VanillaGuiLayers.PLAYER_HEALTH,
                true, false, true));
        assertFalse(OcultacaoDoVanilla.deveOcultar(VanillaGuiLayers.FOOD_LEVEL,
                        true, false, true),
                "a chave de vida nao pode levar a fome junto: a HUD de Nen nao "
                        + "desenha fome, entao ali nao ha duplicacao a remover");

        assertTrue(OcultacaoDoVanilla.deveOcultar(VanillaGuiLayers.FOOD_LEVEL,
                false, true, true));
        assertFalse(OcultacaoDoVanilla.deveOcultar(VanillaGuiLayers.PLAYER_HEALTH,
                false, true, true));
    }

    @Test
    @DisplayName("com as duas desligadas, o vanilla fica intacto")
    void oPadraoNaoMexeEmNada() {
        assertFalse(OcultacaoDoVanilla.deveOcultar(VanillaGuiLayers.PLAYER_HEALTH,
                false, false, true));
        assertFalse(OcultacaoDoVanilla.deveOcultar(VanillaGuiLayers.FOOD_LEVEL,
                false, false, true));
    }

    @Test
    @DisplayName("nenhuma outra camada do vanilla e tocada")
    void aRegraNaoAlcancaORestoDaTela() {
        assertFalse(OcultacaoDoVanilla.deveOcultar(OUTRA, true, true, true),
                "cancelar uma camada que nao foi pedida e como se rouba a hotbar "
                        + "de alguem sem querer");
        assertFalse(OcultacaoDoVanilla.deveOcultar(VanillaGuiLayers.EXPERIENCE_BAR,
                true, true, true));
    }
}
