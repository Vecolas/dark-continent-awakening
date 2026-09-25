package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import com.darkcontinent.nenfoundation.client.hud.PaletaDaHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Uma linha da HUD: rotulo a esquerda, barra no meio, valor a direita.
 *
 * <p><b>ESTA CLASSE E O ALINHAMENTO.</b> O diagnostico da HUD anterior foi
 * "hierarquia fraca entre nome, barra, valor e estado", e a causa era estrutural:
 * cada renderer posicionava o proprio texto a partir da propria barra, entao a
 * Aura e o Output nunca ficavam exatamente no mesmo eixo. Aqui as tres colunas
 * chegam prontas do layout, e nenhuma linha pode sair do lugar sem que a outra
 * saia junto.
 *
 * <p>O VALOR E ALINHADO A DIREITA, e isso nao e gosto: "9/20" e "100/100" tem
 * larguras diferentes, e alinhados a esquerda eles fariam o ultimo digito
 * dancar a cada dano tomado. Pela direita, a unidade fica sempre na mesma
 * coluna.
 *
 * <p>O ROTULO E MAIS APAGADO QUE O VALOR de proposito. Os dois sao texto do
 * mesmo tamanho; o que cria hierarquia entre eles e o contraste. "VIDA" nunca
 * muda e nao precisa ser lido duas vezes.
 */
public final class LinhaDeStatusRenderer {

    private LinhaDeStatusRenderer() {
    }

    /**
     * Desenha a linha inteira.
     *
     * @param rotulo chave ja traduzida; curta, senao ela invade a barra
     * @param valor  texto ja formatado pelo chamador
     * @param cor    ARGB do preenchimento, ja tingido pelo estado de Nen
     */
    public static void desenhar(GuiGraphics g, NenHudLayout.Retangulo areaDoRotulo,
            NenHudLayout.Retangulo areaDaBarra, NenHudLayout.Retangulo areaDoValor,
            Component rotulo, String valor, float fracao, int cor) {
        var fonte = Minecraft.getInstance().font;

        // A LINHA DE BASE DO TEXTO E CALCULADA DA BARRA, e nao da area do
        // texto: as duas tem a mesma altura, mas a fonte tem nove pixels e a
        // barra sete. Centralizar pelo retangulo do texto poria o rotulo dois
        // pixels acima do preenchimento que ele descreve.
        int y = areaDaBarra.y() + (areaDaBarra.altura() - fonte.lineHeight) / 2;

        g.drawString(fonte, rotulo, areaDoRotulo.x(), y, PaletaDaHud.TEXTO_FRACO, false);
        BarraDeStatusRenderer.desenhar(g, areaDaBarra, fracao, cor);
        g.drawString(fonte, valor,
                areaDoValor.fimX() - fonte.width(valor), y, PaletaDaHud.TEXTO, false);
    }
}
