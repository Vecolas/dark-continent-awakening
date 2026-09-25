package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.AparenciaDeTecnica;
import com.darkcontinent.nenfoundation.client.hud.EstadoDeNenNaHud;
import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import com.darkcontinent.nenfoundation.client.hud.PaletaDaHud;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * O chip de estado: uma etiqueta curta com a tecnica que manda agora.
 *
 * <p>ELE SUBSTITUIU O BADGE PROVISORIO, que desenhava o ideograma 念 e nao
 * dizia nada sobre o estado -- era decoracao ocupando o unico ponto da HUD que
 * o olho procura depois do retrato. O plano pede que o estado de Nen vire chip,
 * e nao barra, porque estado nao tem quantidade.
 *
 * <p>ELE ENCOLHE PARA O TEXTO, e nao o contrario. "TEN" e "ZETSU" tem larguras
 * bem diferentes, e um chip de largura fixa deixaria o primeiro nadando num
 * retangulo. O layout reserva a faixa; o chip ocupa a direita dela.
 *
 * <p>A COR VEM DE {@link AparenciaDeTecnica}, que e a mesma fonte da roda e dos
 * indicadores. Um terceiro lugar decidindo a cor de Ren e a terceira chance de
 * as tres discordarem.
 */
public final class ChipDeEstadoRenderer {

    /** Respiro horizontal dentro do chip, de cada lado. */
    private static final int PADDING = 4;

    /**
     * Desenha o chip, ou nada quando nao ha estado a anunciar.
     *
     * <p>SILENCIO E RESPOSTA VALIDA: sem tecnica ligada nao ha estado, e um
     * chip escrito "NENHUM" seria ruido permanente dizendo que nao ha o que
     * dizer.
     */
    public void desenhar(GuiGraphics g, NenHudLayout.Retangulo area,
            Optional<ResourceLocation> dominante, float opacidade) {
        if (dominante.isEmpty()) {
            return;
        }
        // O PISO NAO E ZERO. Com opacidade nula o chip some por quatro ticks, e
        // um elemento que desaparece le como defeito -- nao como transicao. Ele
        // nasce fraco e acende; ele nunca deixa de existir.
        float alpha = 0.25F + 0.75F * Math.clamp(opacidade, 0.0F, 1.0F);
        ResourceLocation id = dominante.orElseThrow();
        var fonte = Minecraft.getInstance().font;
        Component nome = Component.translatable(EstadoDeNenNaHud.chaveDeNome(id));
        int cor = AparenciaDeTecnica.de(id).cor();

        int larguraDoTexto = fonte.width(nome);
        int largura = Math.min(area.largura(), larguraDoTexto + PADDING * 2);
        int x = area.fimX() - largura;
        int y = area.y();
        int fimY = area.fimY();

        // O fundo do chip e a cor da tecnica muito apagada: ele tinge sem
        // acender, e continua legivel sobre neve e sobre caverna.
        g.fill(x, y, area.fimX(), fimY, PaletaDaHud.comAlpha(cor, 0.16F * alpha));
        // A barra vertical na esquerda e o unico traco em cor cheia. E o que
        // faz o chip parecer uma leitura de instrumento, e nao um botao.
        g.fill(x, y, x + 1, fimY, PaletaDaHud.comAlpha(cor, alpha));

        g.drawString(fonte, nome,
                x + PADDING, y + (area.altura() - fonte.lineHeight) / 2 + 1,
                PaletaDaHud.comAlpha(cor, alpha), false);
    }
}
