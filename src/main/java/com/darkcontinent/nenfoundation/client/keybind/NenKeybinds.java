package com.darkcontinent.nenfoundation.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * As teclas do mod.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. Uma categoria propria. Sem ela, os binds do Nen se espalham pela lista
 * de "Diversos" e ficam impossiveis de auditar contra o modpack — e colisao de
 * bind e um dos riscos que o plano nomeia.
 *
 * <p>2. O overlay de debug nasce SEM tecla padrao
 * ({@link InputConstants#UNKNOWN}). Ele e ferramenta de desenvolvimento; ocupar
 * uma tecla do jogador por padrao e gastar um recurso escasso com algo que a
 * maioria nunca vai usar. Quem precisa, atribui.
 *
 * <p>3. Todo bind e criado UMA vez, aqui, e o registro e explicito. Bind criado
 * em dois lugares vira dois bindings com o mesmo nome, e o segundo nunca
 * responde.
 */
public final class NenKeybinds {

    public static final String CATEGORIA = "key.categories.nenfoundation";

    /** Consulta do jogador; remapeavel nos controles, sem depender de modo dev. */
    public static final KeyMapping FICHA_DO_JOGADOR = new KeyMapping(
            "key.nenfoundation.ficha_do_jogador",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORIA);

    /** Liga e desliga o overlay tecnico. Sem tecla padrao, de proposito. */
    public static final KeyMapping OVERLAY_DE_DEBUG = new KeyMapping(
            "key.nenfoundation.overlay_de_debug",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORIA);

    /** Tecla para aumentar/diminuir AOP. C aumenta, Shift+C diminui. */
    public static final KeyMapping AJUSTAR_OUTPUT = new KeyMapping(
            "key.nenfoundation.ajustar_output",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORIA);

    private NenKeybinds() {
    }

    public static void registrar(RegisterKeyMappingsEvent evento) {
        evento.register(FICHA_DO_JOGADOR);
        evento.register(OVERLAY_DE_DEBUG);
        evento.register(AJUSTAR_OUTPUT);
    }
}
