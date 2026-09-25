package com.darkcontinent.nenfoundation.client.hud;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Tira da tela o que a HUD de Nen ja desenha.
 *
 * <p>O plano observa que Vida aparecendo nos coracoes E na barra de Nen e a
 * mesma informacao duas vezes. Este arquivo resolve isso -- e resolve como
 * OPCAO, porque a duplicacao e o preco de a HUD de Nen nao existir antes do
 * despertar.
 *
 * <p><b>A REGRA QUE IMPORTA: so oculta quando ha substituto na tela.</b> Nao
 * basta a config estar ligada; a HUD de Nen tem de estar sendo desenhada
 * naquele quadro. Com F1, em espectador, ou antes do primeiro delta do
 * servidor, os coracoes voltam sozinhos. Sem essa condicao, um jogador com a
 * opcao ligada ficaria sem indicador de vida nenhum durante o carregamento --
 * e o relato seria "minha vida sumiu", sem erro nenhum para procurar.
 *
 * <p>FOME NAO E VIDA. A HUD de Nen nao desenha fome, entao ocultar a barra de
 * fome nao remove duplicacao: remove informacao. Ela tem chave propria, e o
 * texto da config diz isso por escrito, para ninguem ligar as duas achando que
 * sao a mesma limpeza.
 */
public final class OcultacaoDoVanilla {

    private OcultacaoDoVanilla() {
    }

    /**
     * A regra pura, sem evento e sem config: da para provar a tabela inteira.
     *
     * @param camada        a camada vanilla que o jogo esta prestes a desenhar
     * @param ocultarVida   a preferencia do jogador para coracoes
     * @param ocultarFome   a preferencia do jogador para fome
     * @param hudDeNenNaTela se a HUD de Nen esta sendo desenhada neste quadro
     */
    public static boolean deveOcultar(ResourceLocation camada, boolean ocultarVida,
            boolean ocultarFome, boolean hudDeNenNaTela) {
        if (!hudDeNenNaTela) {
            return false;
        }
        if (ocultarVida && VanillaGuiLayers.PLAYER_HEALTH.equals(camada)) {
            return true;
        }
        return ocultarFome && VanillaGuiLayers.FOOD_LEVEL.equals(camada);
    }

    /**
     * O handler.
     *
     * <p>Ele nao decide nada: le a config, pergunta ao supplier se a HUD esta
     * na tela e delega. A decisao mora na funcao pura acima, que tem teste.
     */
    public static void aoRenderizarCamada(RenderGuiLayerEvent.Pre evento,
            java.util.function.BooleanSupplier hudDeNenNaTela) {
        if (deveOcultar(evento.getName(),
                com.darkcontinent.nenfoundation.config.NenClientConfig.ocultarVidaVanilla(),
                com.darkcontinent.nenfoundation.config.NenClientConfig.ocultarFomeVanilla(),
                hudDeNenNaTela.getAsBoolean())) {
            evento.setCanceled(true);
        }
    }
}
