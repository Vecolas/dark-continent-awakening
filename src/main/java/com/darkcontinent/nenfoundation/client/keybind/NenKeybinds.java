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

    /**
     * Abre a roda de Nen enquanto SEGURADA.
     *
     * <p>Segurar, e nao alternar: a roda e um gesto, nao uma tela. Alternando,
     * o jogador esqueceria a roda aberta e levaria dano olhando um menu.
     */
    public static final KeyMapping RODA_DE_NEN = new KeyMapping(
            "key.nenfoundation.roda_de_nen",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORIA);

    /**
     * Liga e desliga o overlay de tuning do VISUAL da aura.
     *
     * <p>TECLA PROPRIA, e nao um modificador do overlay tecnico: os dois sao
     * lidos em sessoes diferentes e por motivos diferentes -- um durante uma
     * investigacao de rede, o outro durante uma sessao de direcao de arte.
     *
     * <p>{@code F6} porque esta livre no vanilla e fica ao lado do {@code F5}
     * da troca de camera, que e a tecla mais apertada numa sessao de captura.
     */
    public static final KeyMapping OVERLAY_DE_VFX = new KeyMapping(
            "key.nenfoundation.overlay_de_vfx",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            CATEGORIA);

    /**
     * Percorre a regiao onde a aura se concentra -- Gyo, e depois Ko.
     *
     * <p>PERCORRER, e nao uma roda de seis fatias. Gyo se usa no meio da briga,
     * e abrir menu para mirar e o oposto do gesto. Seis regioes numa tecla sao
     * duas apertadas no pior caso; uma roda seriam duas telas e uma pausa.
     *
     * <p>{@code G} de Gyo, e porque esta livre no vanilla.
     */
    public static final KeyMapping ESCOLHER_FOCO = new KeyMapping(
            "key.nenfoundation.escolher_foco",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORIA);

    private NenKeybinds() {
    }

    public static void registrar(RegisterKeyMappingsEvent evento) {
        evento.register(FICHA_DO_JOGADOR);
        evento.register(RODA_DE_NEN);
        evento.register(OVERLAY_DE_DEBUG);
        evento.register(AJUSTAR_OUTPUT);
        evento.register(OVERLAY_DE_VFX);
        evento.register(ESCOLHER_FOCO);
    }
}
