package com.darkcontinent.nenfoundation.client.screen;

import com.darkcontinent.nenfoundation.client.NenClientCache;
import com.darkcontinent.nenfoundation.client.hud.AuraHudProjection;
import com.darkcontinent.nenfoundation.client.hud.EstadoDeNenNaHud;
import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import com.darkcontinent.nenfoundation.client.hud.NenHudVisibility;
import com.darkcontinent.nenfoundation.client.hud.PaletaDaHud;
import com.darkcontinent.nenfoundation.client.hud.ProjecaoDeVida;
import com.darkcontinent.nenfoundation.client.hud.animation.AnimacoesDaHud;
import com.darkcontinent.nenfoundation.client.hud.component.BarraDeStatusRenderer;
import com.darkcontinent.nenfoundation.client.hud.component.ChipDeEstadoRenderer;
import com.darkcontinent.nenfoundation.client.hud.component.LinhaDeStatusRenderer;
import com.darkcontinent.nenfoundation.client.hud.component.PainelAngular;
import com.darkcontinent.nenfoundation.client.hud.component.PlayerHeadRenderer;
import com.darkcontinent.nenfoundation.client.hud.component.TecnicasAtivasRenderer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Orquestra a HUD; visibilidade e composicao moram aqui, desenho nos componentes.
 *
 * <p>A ORDEM DE DESENHO E A ORDEM DE PROFUNDIDADE: painel, depois retrato,
 * depois conteudo. Inverter poria a moldura por cima do texto, que e o defeito
 * que a versao anterior evitava desenhando a moldura no meio da sequencia --
 * uma ordem que so funcionava porque a moldura era um PNG com buracos.
 */
public final class OverlayDeAura {

    /** Quantos ticks o pulso de Ren leva para ir e voltar. */
    private static final float CICLO_DO_PULSO = 34.0F;

    private final NenClientCache cache;
    private final AnimacoesDaHud animacoes = new AnimacoesDaHud();
    private final PlayerHeadRenderer retrato = new PlayerHeadRenderer();
    private final ChipDeEstadoRenderer chip = new ChipDeEstadoRenderer();
    private final TecnicasAtivasRenderer tecnicas = new TecnicasAtivasRenderer();

    public OverlayDeAura(NenClientCache cache) {
        this.cache = cache;
    }

    /**
     * Se a HUD de Nen esta na tela AGORA.
     *
     * <p>EXISTE PARA A OCULTACAO DO VANILLA, e e recalculada em vez de guardada
     * do quadro anterior: a camada de vida do vanilla e desenhada ANTES desta
     * HUD no mesmo quadro, entao um flag escrito aqui estaria sempre um quadro
     * atrasado -- e no quadro em que a HUD aparece os coracoes piscariam.
     *
     * <p>As duas condicoes sao as mesmas de {@link #aoRenderizar}: modo jogavel,
     * e delta recebido. Se elas divergirem, o jogador fica sem os dois
     * indicadores ao mesmo tempo, que e exatamente o que nao pode acontecer.
     */
    public boolean naTela() {
        Minecraft mc = Minecraft.getInstance();
        return NenHudVisibility.deveRenderizar(mc.options.hideGui, mc.player != null,
                        mc.player != null && mc.player.isSpectator())
                && this.cache.delta().isPresent();
    }

    public void aoRenderizar(RenderGuiEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        if (!NenHudVisibility.deveRenderizar(mc.options.hideGui, mc.player != null,
                mc.player != null && mc.player.isSpectator())) {
            return;
        }
        float parcial = evento.getPartialTick().getGameTimeDeltaPartialTick(false);
        AuraHudProjection aura = AuraHudProjection.de(this.cache, parcial);
        GuiGraphics g = evento.getGuiGraphics();

        if (!aura.disponivel()) {
            // SEM O DELTA NAO HA PAINEL. Desenhar a moldura com as barras
            // vazias afirmaria que a reserva e zero, e ela e DESCONHECIDA --
            // a distincao que `AuraHudProjection.disponivel` existe para manter.
            g.drawString(mc.font, Component.translatable("nenfoundation.hud.aguardando"),
                    NenHudLayout.MARGEM, NenHudLayout.MARGEM, PaletaDaHud.TEXTO_FRACO, true);
            return;
        }

        List<ResourceLocation> ativas = tecnicasAtivas();
        boolean comFluxo = NenHudVisibility.deveMostrarFluxo(ativas.size());
        NenHudLayout layout = NenHudLayout.para(g.guiWidth(), comFluxo);
        Optional<ResourceLocation> dominante = EstadoDeNenNaHud.dominante(Set.copyOf(ativas));

        // O RELOGIO E LIDO UMA VEZ e passado adiante. Cada animacao perguntando
        // as horas por conta propria daria quadros em que o flash e o fade
        // discordam sobre que instante e agora.
        double tick = mc.level == null ? 0.0D : mc.level.getGameTime() + parcial;
        this.animacoes.observar(tick, mc.player.getHealth(), dominante);

        PainelAngular.desenhar(g, layout.moldura(), NenHudLayout.CORTE_DIAGONAL);
        this.retrato.desenhar(g, layout.retrato(), mc.player);
        desenharNome(g, mc, layout);
        this.chip.desenhar(g, layout.chip(), dominante, this.animacoes.fadeDoChip(tick));
        desenharVida(g, mc, layout, this.animacoes.flashDeDano(tick));
        desenharAura(g, layout, aura, dominante, tick, this.animacoes.supressao(tick));

        if (comFluxo) {
            BarraDeStatusRenderer.desenhar(g, layout.barraDeFluxo(),
                    aura.outputVisual(), PaletaDaHud.FLUXO);
        }
        if (NenHudVisibility.deveMostrarFilaDeTecnicas(ativas.size())) {
            this.tecnicas.desenhar(g, layout.tecnicasAtivas(), ativas);
        }
    }

    /**
     * O nome, cortado quando nao cabe.
     *
     * <p>CORTAR E MELHOR QUE ENCOLHER A FONTE: a HUD tem uma escala de texto so,
     * e um nome em corpo menor leria como outro tipo de informacao. Nomes do
     * Minecraft vao a dezesseis caracteres, e a faixa reservada nao cabe todos.
     */
    private static void desenharNome(GuiGraphics g, Minecraft mc, NenHudLayout layout) {
        var fonte = mc.font;
        String nome = mc.player.getGameProfile().getName();
        NenHudLayout.Retangulo area = layout.nome();
        g.drawString(fonte, fonte.plainSubstrByWidth(nome, area.largura()),
                area.x(), area.y(), PaletaDaHud.TEXTO, false);
    }

    /**
     * A linha de Vida, com o flash de dano.
     *
     * <p>O FLASH CLAREIA, e nao pisca de vermelho. Vermelho ja e a cor do alerta
     * de aura zerada nesta HUD, e usa-lo tambem no dano faria os dois estados
     * -- "levei pancada" e "estou sem aura" -- lerem igual pelo canto do olho.
     */
    private static void desenharVida(GuiGraphics g, Minecraft mc, NenHudLayout layout,
            float flash) {
        ProjecaoDeVida vida = ProjecaoDeVida.de(mc.player.getHealth(),
                mc.player.getMaxHealth());
        int cor = PaletaDaHud.clarear(PaletaDaHud.VIDA, 0.55F * flash);
        LinhaDeStatusRenderer.desenhar(g, layout.rotuloDeVida(), layout.barraDeVida(),
                layout.valorDeVida(), Component.translatable("nenfoundation.hud.vida"),
                vida.texto(), vida.fracao(), cor);
    }

    /**
     * A linha de Aura, ja tingida pelo estado.
     *
     * <p>O ALERTA SUBSTITUI A COR, e nao acrescenta um texto embaixo. A versao
     * anterior escrevia "Aura exausta" numa linha extra, que empurrava o layout
     * e so aparecia no pior momento possivel -- justamente quando o jogador
     * precisa de menos coisa nova na tela, e nao de mais.
     */
    private static void desenharAura(GuiGraphics g, NenHudLayout layout,
            AuraHudProjection aura, Optional<ResourceLocation> dominante, double tick,
            float supressao) {
        EstadoDeNenNaHud.Tratamento tratamento = EstadoDeNenNaHud.tratamentoDe(dominante);
        // A SUPRESSAO TEM RAMPA PROPRIA; o pulso e continuo. Sao as duas unicas
        // intensidades que existem, e cada tratamento consome a sua.
        float intensidade = tratamento == EstadoDeNenNaHud.Tratamento.SUPRIMIDO
                ? supressao : pulso(tick);
        int cor = aura.exausto() || aura.maxima() == 0
                ? PaletaDaHud.ALERTA
                : tratamento.aplicar(PaletaDaHud.AURA, intensidade);
        String valor = String.format(Locale.ROOT, "%.0f/%.0f", aura.atual(), aura.maxima());
        LinhaDeStatusRenderer.desenhar(g, layout.rotuloDeAura(), layout.barraDeAura(),
                layout.valorDeAura(), Component.translatable("nenfoundation.hud.aura"),
                valor, aura.fracao(), cor);
    }

    /**
     * O pulso, entre 0 e 1, derivado do relogio do mundo.
     *
     * <p>LIDO NA HORA, e nunca guardado num campo: um pulso com estado proprio
     * precisaria ser reiniciado ao trocar de mundo, e esse e o tipo de limpeza
     * que sempre falta num dos caminhos de saida.
     */
    private static float pulso(double tick) {
        return (float) ((1.0D - Math.cos(tick / CICLO_DO_PULSO * 2.0D * Math.PI)) / 2.0D);
    }

    /**
     * As tecnicas ligadas, em ordem ESTAVEL.
     *
     * <p>O delta traz um conjunto, e conjunto nao tem ordem. Desenhar na ordem
     * de iteracao faria a fila embaralhar entre ticks sem nada ter mudado -- o
     * jogador veria os indicadores trocando de lugar sozinhos, e nao daria para
     * olhar de relance. Ordenar por id e arbitrario e, principalmente, igual
     * toda vez.
     */
    private List<ResourceLocation> tecnicasAtivas() {
        return this.cache.delta()
                .map(d -> d.tecnicasAtivas().stream()
                        .sorted(Comparator.comparing(ResourceLocation::toString))
                        .toList())
                .orElse(List.of());
    }
}
