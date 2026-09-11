package com.darkcontinent.nenfoundation.client.screen;

import com.darkcontinent.nenfoundation.client.NenClientCache;
import com.darkcontinent.nenfoundation.client.keybind.NenKeybinds;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Ficha aberta por V, separada do overlay tecnico e disponivel sem dev.enabled.
 * So desenha estado recebido. Nao envia pedidos, equipa nem concede Nen.
 * O mundo continua rodando; cada quadro consulta o cache e o jogador atuais.
 * A arte e geometria nativa, sem texturas extraidas da referencia.
 */
public final class TelaDoJogador extends Screen {
    private static final int CLARO = 0xFFE3EEE8;
    private static final int VERDE = 0xFF69E5AE;
    private static final int SUAVE = 0xFF9EB5AB;
    private static final int ALTURA_LINHA = 26;
    private final NenClientCache cache;
    private final DadosDaFicha dados;
    private boolean abaNen;
    private int pagina;
    private int esquerda;
    private int topo;
    private int largura;
    private int altura;
    private int coluna;
    private int larguraLista;
    private int inicioLista;
    private int fimLista;
    private int larguraRetrato;
    private Button anterior;
    private Button proxima;
    private BotaoDaFicha status;
    private BotaoDaFicha nen;

    public TelaDoJogador(NenClientCache cache) {
        super(texto("titulo"));
        this.cache = cache;
        this.dados = new DadosDaFicha(cache);
    }

    @Override
    protected void init() {
        this.largura = Math.min(580, this.width - 16);
        this.altura = Math.min(350, this.height - 16);
        this.esquerda = (this.width - this.largura) / 2;
        this.topo = (this.height - this.altura) / 2;
        // Em GUI pequena, o retrato cede lugar ao conteudo, nunca ao contrario.
        this.larguraRetrato = this.largura >= 390 ? 100 : 0;
        this.coluna = this.esquerda + 12 + this.larguraRetrato;
        this.larguraLista = this.largura - 24 - this.larguraRetrato;
        this.inicioLista = this.topo + 88;
        this.fimLista = this.topo + this.altura - 36;
        int larguraAba = (this.largura - 28) / 2;
        this.status = addRenderableWidget(new BotaoDaFicha(this.esquerda + 12, this.topo + 40,
                larguraAba, 22, texto("status"), b -> trocarAba(false)));
        this.nen = addRenderableWidget(new BotaoDaFicha(this.esquerda + 16 + larguraAba, this.topo + 40,
                larguraAba, 22, texto("nen"), b -> trocarAba(true)));
        addRenderableWidget(new BotaoDaFicha(this.esquerda + this.largura - 30, this.topo + 9,
                20, 20, Component.literal("X"), b -> onClose())
                .comDescricao(texto("fechar")));
        int y = this.topo + this.altura - 27;
        this.anterior = addRenderableWidget(new BotaoDaFicha(this.coluna, y, 24, 18,
                Component.literal("<"), b -> mudarPagina(-1)).comDescricao(texto("anterior")));
        this.proxima = addRenderableWidget(new BotaoDaFicha(this.coluna + this.larguraLista - 24, y, 24, 18,
                Component.literal(">"), b -> mudarPagina(1)).comDescricao(texto("proxima")));
        atualizarBotoes(linhas());
    }

    private void trocarAba(boolean nen) {
        this.abaNen = nen;
        this.pagina = 0;
        atualizarBotoes(linhas());
    }

    private PaginacaoDaFicha paginacao(List<Linha> linhas) {
        return new PaginacaoDaFicha(linhas.size(), Math.max(1, (this.fimLista - this.inicioLista) / ALTURA_LINHA), this.pagina);
    }

    private void mudarPagina(int direcao) {
        this.pagina += direcao;
        atualizarBotoes(linhas());
    }

    private void atualizarBotoes(List<Linha> linhas) {
        var p = paginacao(linhas);
        this.pagina = p.pagina();
        this.anterior.active = this.pagina > 0;
        this.proxima.active = this.pagina + 1 < p.paginas();
        this.status.selecionado = !this.abaNen;
        this.nen.selecionado = this.abaNen;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float parcial) {
        g.fill(0, 0, this.width, this.height, 0xB0101815);
        moldura(g, this.esquerda, this.topo, this.largura, this.altura, 0xFF091F17);
        for (int x = this.esquerda + 6; x < this.esquerda + this.largura - 5; x += 12) {
            g.fill(x, this.topo + 5, x + 1, this.topo + this.altura - 5, 0xFF102C21);
        }
        for (int y = this.topo + 6; y < this.topo + this.altura - 5; y += 12) {
            g.fill(this.esquerda + 5, y, this.esquerda + this.largura - 5, y + 1, 0xFF102C21);
        }
        g.fill(this.esquerda + 5, this.topo + 5, this.esquerda + this.largura - 5, this.topo + 35, 0xFF071912);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float parcial) {
        List<Linha> linhas = linhas();
        atualizarBotoes(linhas);
        super.render(g, mouseX, mouseY, parcial);
        g.drawString(this.font, this.title, this.esquerda + 13, this.topo + 10, VERDE, false);
        if (this.minecraft.player != null) {
            g.drawString(this.font, abreviar(this.minecraft.player.getName(), this.largura - 60),
                    this.esquerda + 13, this.topo + 23, CLARO, false);
        }
        Component aviso = this.abaNen ? texto("somente_desbloqueios") : texto("somente_leitura");
        g.drawString(this.font, abreviar(aviso, this.largura - 24), this.esquerda + 12, this.topo + 71, SUAVE, false);
        if (mouseY >= this.topo + 68 && mouseY < this.topo + 83
                && mouseX >= this.esquerda + 12 && mouseX < this.esquerda + this.largura - 12) {
            setTooltipForNextRenderPass(this.font.split(aviso, Math.min(280, this.width - 24)));
        }
        desenharRetrato(g, mouseX, mouseY);
        var p = paginacao(linhas);
        g.enableScissor(this.coluna, this.inicioLista, this.coluna + this.larguraLista, this.fimLista);
        for (int i = p.inicio(); i < p.fim(); i++) {
            Linha linha = linhas.get(i);
            int y = this.inicioLista + (i - p.inicio()) * ALTURA_LINHA;
            g.fill(this.coluna, y, this.coluna + this.larguraLista, y + ALTURA_LINHA - 2, 0xE00A2119);
            g.fill(this.coluna, y, this.coluna + 2, y + ALTURA_LINHA - 2, VERDE);
            g.drawString(this.font, abreviar(linha.rotulo(), this.larguraLista - 12), this.coluna + 7, y + 3, SUAVE, false);
            g.drawString(this.font, abreviar(linha.valor(), this.larguraLista - 12), this.coluna + 7, y + 13, CLARO, false);
            if (mouseX >= this.coluna && mouseX < this.coluna + this.larguraLista && mouseY >= y && mouseY < y + ALTURA_LINHA - 2) {
                setTooltipForNextRenderPass(this.font.split(linha.rotulo().copy().append(": ").append(linha.valor()),
                        Math.min(300, this.width - 24)));
            }
        }
        g.disableScissor();
        g.drawCenteredString(this.font, texto("pagina", p.pagina() + 1, p.paginas()),
                this.coluna + this.larguraLista / 2, this.topo + this.altura - 22, SUAVE);
    }

    private void desenharRetrato(GuiGraphics g, int mouseX, int mouseY) {
        if (this.larguraRetrato == 0 || this.minecraft.player == null) return;
        int x = this.esquerda + 12;
        int w = this.larguraRetrato - 8;
        int h = this.fimLista - this.inicioLista;
        moldura(g, x, this.inicioLista, w, h, 0xFF081711);
        int escala = Math.min(55, Math.max(16, (h - 24) / 2));
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 4, this.inicioLista + 4,
                x + w - 4, this.fimLista - 4, escala, 0.0625F, mouseX, mouseY, this.minecraft.player);
    }

    private List<Linha> linhas() {
        List<Linha> linhas = new ArrayList<>();
        if (this.minecraft == null || this.minecraft.player == null) return linhas;
        if (this.abaNen) {
            if (this.dados.perfil().isEmpty()) {
                linhas.add(new Linha(texto("perfil"), texto("aguardando")));
            } else {
                // As tres secoes vazias tambem aparecem: ausencia nao vira habilidade ficticia.
                for (var tipo : DadosDaFicha.Tipo.values()) {
                    List<DadosDaFicha.Entrada> entradas = this.dados.entradas().stream().filter(e -> e.tipo() == tipo).toList();
                    if (entradas.isEmpty()) linhas.add(new Linha(Component.translatable(tipo.chave()), texto("nenhum")));
                    for (var entrada : entradas) {
                        linhas.add(new Linha(Component.translatable(tipo.chave()), Component.literal(entrada.id().toString())));
                    }
                }
            }
            return linhas;
        }
        var jogador = this.minecraft.player;
        linhas.add(new Linha(texto("vida"), Component.literal(numero(jogador.getHealth()) + " / " + numero(jogador.getMaxHealth()))));
        linhas.add(new Linha(texto("fome"), Component.literal(jogador.getFoodData().getFoodLevel() + " / 20")));
        linhas.add(new Linha(texto("armadura"), Component.literal(Integer.toString(jogador.getArmorValue()))));
        linhas.add(new Linha(texto("nivel"), Component.literal(Integer.toString(jogador.experienceLevel))));
        linhas.add(new Linha(texto("categoria"), this.dados.perfil()
                .<Component>map(p -> Component.translatable("nenfoundation.category." + p.categoriaVisivel().getSerializedName()))
                .orElseGet(() -> texto("aguardando"))));
        linhas.add(new Linha(texto("aura"), this.cache.delta()
                .<Component>map(d -> Component.literal(numero(d.aura()) + " / " + numero(d.auraMaxima())))
                .orElseGet(() -> texto("runtime_ausente"))));
        this.dados.perfil().ifPresent(p -> {
            linhas.add(new Linha(texto("tecnicas_total"), Component.literal(Integer.toString(p.tecnicasDesbloqueadas().size()))));
            linhas.add(new Linha(texto("habilidades_total"), Component.literal(Integer.toString(p.habilidadesDesbloqueadas().size()))));
            linhas.add(new Linha(texto("marcos_total"), Component.literal(Integer.toString(p.marcos().size()))));
        });
        return linhas;
    }

    private String abreviar(Component texto, int largura) {
        String valor = texto.getString();
        return this.font.width(valor) <= largura ? valor
                : this.font.plainSubstrByWidth(valor, Math.max(0, largura - this.font.width("..."))) + "...";
    }

    private static String numero(float valor) { return String.format(Locale.ROOT, "%.1f", valor); }
    private static Component texto(String chave, Object... argumentos) { return Component.translatable("nenfoundation.ficha." + chave, argumentos); }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NenKeybinds.FICHA_DO_JOGADOR.matches(keyCode, scanCode) && modificadorAtivo()) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            mudarPagina(keyCode == GLFW.GLFW_KEY_PAGE_DOWN ? 1 : -1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double x, double y, int botao) {
        if (NenKeybinds.FICHA_DO_JOGADOR.matchesMouse(botao) && modificadorAtivo()) {
            onClose();
            return true;
        }
        return super.mouseClicked(x, y, botao);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= this.coluna && x < this.coluna + this.larguraLista && y >= this.inicioLista && y < this.fimLista && vertical != 0) {
            mudarPagina(vertical < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override public boolean isPauseScreen() { return false; }

    private static boolean modificadorAtivo() {
        // O contexto IN_GAME nao esta ativo dentro da ficha; conferir so o
        // modificador evita que Ctrl+clique remapeado feche por clique simples.
        return NenKeybinds.FICHA_DO_JOGADOR.getKeyModifier().isActive(KeyConflictContext.GUI);
    }

    @Override
    public void tick() {
        if (this.minecraft.player == null || this.minecraft.level == null) onClose();
    }

    private static void moldura(GuiGraphics g, int x, int y, int w, int h, int fundo) {
        g.fill(x, y, x + w, y + h, 0xFF506760);
        g.fill(x, y, x + w - 1, y + 2, CLARO);
        g.fill(x, y, x + 2, y + h - 1, CLARO);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, fundo);
    }

    private record Linha(Component rotulo, Component valor) { }

    /** Widget vanilla preserva foco, clique e narracao; so a pintura e nossa. */
    private static final class BotaoDaFicha extends Button {
        private boolean selecionado;
        private Component descricao;

        BotaoDaFicha(int x, int y, int w, int h, Component texto, OnPress acao) {
            super(x, y, w, h, texto, acao, DEFAULT_NARRATION);
        }

        BotaoDaFicha comDescricao(Component descricao) {
            this.descricao = descricao;
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(descricao));
            return this;
        }

        @Override
        protected net.minecraft.network.chat.MutableComponent createNarrationMessage() {
            return this.descricao == null ? super.createNarrationMessage() : this.descricao.copy();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float parcial) {
            int fundo = this.selecionado ? 0xFF1D6048 : this.isHoveredOrFocused() ? 0xFF244D3D : 0xFF0A241A;
            moldura(g, getX(), getY(), getWidth(), getHeight(), fundo);
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2, this.active ? (this.selecionado ? VERDE : CLARO) : 0xFF60786D);
        }
    }
}
