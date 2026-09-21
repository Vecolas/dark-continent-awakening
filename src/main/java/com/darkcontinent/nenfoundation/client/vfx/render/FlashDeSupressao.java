package com.darkcontinent.nenfoundation.client.vfx.render;

import com.darkcontinent.nenfoundation.client.vfx.PulsoDeSupressao;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Desenha o retorno de input de Zetsu -- e nada mais.
 *
 * <p><b>ELE E HUD, E NAO AURA.</b> A distincao e o AV6 inteiro: Zetsu e ausencia
 * total no MUNDO, e qualquer coisa desenhada sobre o corpo -- contorno, halo,
 * cintilacao -- seria vista por um observador e entregaria justamente quem esta
 * se escondendo. Na interface, o pulso existe apenas na tela de quem apertou a
 * tecla, e nao ha por onde vazar: ele nao passa pelo perfil, nao entra no alvo
 * de brilho, nao e geometria e nao atravessa a rede.
 *
 * <p>UM ESCURECIMENTO NAS BORDAS, e nao um clarao. Zetsu FECHA; um flash branco
 * seria lido como dano recebido, e "algo me acertou" e a ultima coisa que
 * suprimir deveria comunicar.
 *
 * <p>QUATRO RETANGULOS, e nao um gradiente por pixel. O gesto dura meio segundo
 * e ocupa a periferia da visao, onde a diferenca entre uma faixa e um degrade
 * nao se nota -- e quatro chamadas de {@code fill} custam menos que uma textura
 * carregada para isso.
 */
public final class FlashDeSupressao {

    /** Que fracao da tela a faixa ocupa em cada lado. */
    private static final float FAIXA = 0.16F;

    /** A cor do fechamento: quase preta, levemente fria. */
    private static final int COR = 0x0A0E14;

    /** Desenha o pulso, se houver. */
    public void aoRenderizar(RenderGuiEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || !PulsoDeSupressao.ativo()) {
            return;
        }
        float intensidade = PulsoDeSupressao.intensidade();
        if (intensidade <= 0.002F) {
            return;
        }
        GuiGraphics g = evento.getGuiGraphics();
        int largura = g.guiWidth();
        int altura = g.guiHeight();
        int faixaX = Math.max(1, Math.round(largura * FAIXA));
        int faixaY = Math.max(1, Math.round(altura * FAIXA));
        int argb = (Math.round(Math.clamp(intensidade, 0.0F, 1.0F) * 255.0F) << 24) | COR;

        g.fill(0, 0, faixaX, altura, argb);
        g.fill(largura - faixaX, 0, largura, altura, argb);
        g.fill(faixaX, 0, largura - faixaX, faixaY, argb);
        g.fill(faixaX, altura - faixaY, largura - faixaX, altura, argb);
    }
}
