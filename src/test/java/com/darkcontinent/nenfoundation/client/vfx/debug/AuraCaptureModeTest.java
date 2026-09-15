package com.darkcontinent.nenfoundation.client.vfx.debug;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.client.player.Input;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuraCaptureModeTest {

    @Test
    @DisplayName("modo ligado neutraliza todo input que anima movimento")
    void neutralizaMovimentoParaPoseIdle() {
        Input input = inputAtivo();

        boolean alterado = AuraCaptureMode.neutralizarSeLigado(true, input);

        assertAll(
                () -> assertTrue(alterado),
                () -> assertEquals(0.0F, input.leftImpulse),
                () -> assertEquals(0.0F, input.forwardImpulse),
                () -> assertFalse(input.up),
                () -> assertFalse(input.down),
                () -> assertFalse(input.left),
                () -> assertFalse(input.right),
                () -> assertFalse(input.jumping),
                () -> assertFalse(input.shiftKeyDown));
    }

    @Test
    @DisplayName("modo desligado nao toca no input nem na distancia")
    void desligadoNaoInterfereNoCliente() {
        Input input = inputAtivo();

        boolean alterado = AuraCaptureMode.neutralizarSeLigado(false, input);

        assertAll(
                () -> assertFalse(alterado),
                () -> assertEquals(0.75F, input.leftImpulse),
                () -> assertEquals(1.0F, input.forwardImpulse),
                () -> assertTrue(input.up),
                () -> assertTrue(input.down),
                () -> assertTrue(input.left),
                () -> assertTrue(input.right),
                () -> assertTrue(input.jumping),
                () -> assertTrue(input.shiftKeyDown),
                () -> assertEquals(7.5F,
                        AuraCaptureMode.distanciaSolicitada(false, 7.5F)));
    }

    @Test
    @DisplayName("modo ligado pede quatro blocos sem burlar o raycast posterior")
    void fixaDistanciaSolicitada() {
        assertEquals(4.0F, AuraCaptureMode.distanciaSolicitada(true, 7.5F));
    }

    private static Input inputAtivo() {
        Input input = new Input();
        input.leftImpulse = 0.75F;
        input.forwardImpulse = 1.0F;
        input.up = true;
        input.down = true;
        input.left = true;
        input.right = true;
        input.jumping = true;
        input.shiftKeyDown = true;
        return input;
    }
}
