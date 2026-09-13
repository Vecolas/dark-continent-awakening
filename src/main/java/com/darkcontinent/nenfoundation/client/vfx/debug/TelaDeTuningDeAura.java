package com.darkcontinent.nenfoundation.client.vfx.debug;

import com.darkcontinent.nenfoundation.client.vfx.AuraVisualMode;
import com.darkcontinent.nenfoundation.client.vfx.SobreposicaoDeVfx;
import com.darkcontinent.nenfoundation.client.vfx.SobreposicaoDeVfx.AjusteDePerfil;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfis;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Os sliders: direcao de arte com o mouse, com o jogo rodando atras.
 *
 * <p>POR QUE UMA TELA, E NAO SO COMANDOS. Achar o alpha de borda certo e um
 * movimento continuo -- sobe um pouco, desce um pouco, para onde parece certo.
 * Por comando, cada tentativa e uma linha digitada no chat, e depois de dez
 * tentativas ninguem sabe mais de onde partiu. Com slider, a mesma busca leva
 * segundos, e e por isso que o roteiro do gate chama isso de a diferenca entre
 * uma tarde e uma semana.
 *
 * <p><b>O QUE SAI DAQUI NAO PERSISTE, E ISSO E O PONTO.</b> Esta tela nao
 * escreve o JSON de perfil de volta em disco. Um numero achado aqui e uma
 * HIPOTESE; ele vira decisao quando alguem o escreve no
 * {@code assets/nenfoundation/nen_vfx/ten.json}, com o resto do time podendo
 * ver o diff. Sem essa separacao, o perfil do repositorio e o que o jogo mostra
 * viram duas fontes para a mesma verdade.
 *
 * <p>O SLIDER NASCE NO VALOR QUE ESTA VALENDO, e nao no meio da faixa: comecar
 * no meio joga fora o ajuste anterior no primeiro clique, e o valor que estava
 * na tela some sem ninguem ter pedido.
 *
 * <p>NAO HA SLIDER DE BLOOM. O passe de pos-processamento nasce no AV5
 * (ADR-016); um slider que nao gira nada e um botao morto, e botao morto e o
 * erro numero 7 do {@code CLAUDE.md}.
 *
 * <p>CLIENT-ONLY.
 */
public final class TelaDeTuningDeAura extends Screen {

    private static final int LARGURA = 220;
    private static final int ALTURA_DO_WIDGET = 20;
    private static final int ESPACO = 4;

    /**
     * Pedido pendente de abertura.
     *
     * <p>O comando nao abre a tela direto: fechar o chat depois de abrir uma
     * {@code Screen} fecha a tela recem-aberta junto. O tick abre no quadro
     * seguinte, com o chat ja fora do caminho.
     */
    private static volatile boolean aberturaPedida;

    public TelaDeTuningDeAura() {
        super(Component.translatable("nenfoundation.vfx.tuning_titulo"));
    }

    /** Chamado pelo comando. O tick do cliente consome. */
    public static void pedirAbertura() {
        aberturaPedida = true;
    }

    /** Devolve e zera o pedido. Consumir exatamente uma vez e o contrato. */
    public static boolean consumirPedidoDeAbertura() {
        if (!aberturaPedida) {
            return false;
        }
        aberturaPedida = false;
        return true;
    }

    /** Quem liga, desliga: um pedido pendente nao sobrevive ao logout. */
    public static void limpar() {
        aberturaPedida = false;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - LARGURA / 2;
        int y = 40;
        AuraPerfilVisual perfil = AuraPerfis.de(AuraVisualMode.TEN);
        for (AjusteDePerfil ajuste : AjusteDePerfil.values()) {
            this.addRenderableWidget(new SliderDeAjuste(x, y, ajuste, valorAtual(ajuste, perfil)));
            y += ALTURA_DO_WIDGET + ESPACO;
        }
        this.addRenderableWidget(new SliderDeRibbons(x, y, SobreposicaoDeVfx.ribbonsForcadas()));
        y += ALTURA_DO_WIDGET + ESPACO * 2;

        this.addRenderableWidget(Button.builder(
                Component.translatable("nenfoundation.vfx.tuning_restaurar"), botao -> {
                    SobreposicaoDeVfx.limpar();
                    this.rebuildWidgets();
                }).bounds(x, y, LARGURA, ALTURA_DO_WIDGET).build());
    }

    /**
     * O valor de partida de um slider.
     *
     * <p>Sobreposicao, se houver; senao o numero do perfil de Ten carregado do
     * resource pack. Nunca uma constante daqui -- ela seria uma segunda fonte
     * para um numero que ja tem dono.
     */
    private static float valorAtual(AjusteDePerfil ajuste, AuraPerfilVisual perfil) {
        float forcado = SobreposicaoDeVfx.valorDe(ajuste);
        if (!Float.isNaN(forcado)) {
            return forcado;
        }
        return switch (ajuste) {
            case ALPHA_INTERNO -> perfil.alphaInterno();
            case ALPHA_BORDA -> perfil.alphaBorda();
            case ALPHA_EXTERNO -> perfil.alphaExterno();
            // O FRESNEL E UM FATOR sobre o perfil, e nao um expoente: 1.0 e
            // "como esta no dado". Ver o javadoc de aplicarNoPerfil.
            case FRESNEL -> 1.0F;
            case FLUXO -> perfil.velocidadeDeFluxo();
            case RUIDO -> perfil.escalaDeRuido();
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float parcial) {
        super.render(g, mouseX, mouseY, parcial);
        g.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
        g.drawCenteredString(this.font,
                Component.translatable("nenfoundation.vfx.tuning_rodape"),
                this.width / 2, this.height - 22, 0xFFAAAAAA);
    }

    /**
     * O FUNDO NAO ESCURECE, de proposito.
     *
     * <p>Ajustar a aura olhando para ela atras de um veu preto e ajustar outra
     * coisa. Um perfil aprovado assim ficaria claro demais no jogo, e o erro so
     * apareceria na primeira captura -- que e tarde.
     */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float parcial) {
        // Sem chamada ao super: nenhum escurecimento.
    }

    @Override
    public boolean isPauseScreen() {
        // PAUSAR MATARIA O TUNING. Com o jogo parado, fluxo e pulsacao param, e
        // metade dos numeros desta tela deixa de ter efeito visivel.
        return false;
    }

    /** Um slider ligado a um numero de perfil. */
    private static final class SliderDeAjuste extends AbstractSliderButton {

        private final AjusteDePerfil ajuste;

        private SliderDeAjuste(int x, int y, AjusteDePerfil ajuste, float valor) {
            super(x, y, LARGURA, ALTURA_DO_WIDGET, Component.empty(),
                    normalizar(ajuste, valor));
            this.ajuste = ajuste;
            this.updateMessage();
        }

        private static double normalizar(AjusteDePerfil ajuste, float valor) {
            float faixa = ajuste.maximo() - ajuste.minimo();
            if (faixa <= 0.0F) {
                return 0.0D;
            }
            return Math.clamp((valor - ajuste.minimo()) / faixa, 0.0F, 1.0F);
        }

        private float valorReal() {
            return (float) (this.ajuste.minimo()
                    + this.value * (this.ajuste.maximo() - this.ajuste.minimo()));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(String.format(Locale.ROOT, "%s: %.3f",
                    this.ajuste.nome(), this.valorReal())));
        }

        @Override
        protected void applyValue() {
            SobreposicaoDeVfx.forcarNoPerfil(this.ajuste, this.valorReal());
        }
    }

    /** Um slider de contagem de filamentos. Zero e um valor legitimo aqui. */
    private static final class SliderDeRibbons extends AbstractSliderButton {

        private static final int MAXIMO = 32;

        private SliderDeRibbons(int x, int y, int quantidade) {
            super(x, y, LARGURA, ALTURA_DO_WIDGET, Component.empty(),
                    quantidade < 0 ? 1.0D : Math.clamp(quantidade / (double) MAXIMO, 0.0D, 1.0D));
            this.updateMessage();
        }

        private int quantidade() {
            return (int) Math.round(this.value * MAXIMO);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal("ribbons: " + this.quantidade()));
        }

        @Override
        protected void applyValue() {
            SobreposicaoDeVfx.forcarRibbons(this.quantidade());
        }
    }
}
