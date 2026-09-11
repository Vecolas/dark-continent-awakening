package com.darkcontinent.nenfoundation.client.screen;

import com.darkcontinent.nenfoundation.client.NenClientCache;
import com.darkcontinent.nenfoundation.client.keybind.NenKeybinds;
import com.darkcontinent.nenfoundation.network.payload.AtivarTecnicaC2S;
import com.darkcontinent.nenfoundation.network.payload.DesativarTecnicaC2S;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A roda de Nen: aponta com o mouse, clica para ligar ou desligar.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. AS FATIAS SAO DERIVADAS do que o jogador realmente tem, e nunca de uma
 * lista fixa. A referencia visual mostra dez tecnicas e nove nao existem --
 * desenhar as dez faria a roda oferecer coisas que o jogador nunca vai
 * conseguir usar. Um menu que mente e pior que um menu pequeno.
 *
 * <p>2. NAO HA MUTACAO OTIMISTA. Clicar manda INTENCAO; a fatia so muda de
 * aparencia quando o snapshot do servidor chega. Um cliente que adivinha o
 * resultado mostra ligado o que o servidor recusou, e o jogador age com base
 * numa tela que mente.
 *
 * <p>3. NAO HA LIMITE DE SLOTS, de proposito. Quantas tecnicas ficam ligadas e
 * o que a Aura sustenta: cada uma cobra manutencao por tick, e as que nao
 * conseguem pagar caem sozinhas. Um teto de slots aqui seria uma segunda regra
 * dizendo a mesma coisa pior, e as duas divergiriam.
 *
 * <p>4. Ela fecha ao SOLTAR a tecla, verificado no tick. Nao da para escutar
 * "soltou" dentro de uma tela; perguntar todo tick e o que existe.
 *
 * <p>PONTO CEGO DECLARADO: <b>a roda nasce com texto, sem icone.</b> A
 * referencia tem um simbolo por tecnica, e arte autoral ainda nao existe --
 * o ADR-007 proibe extrair da obra, e arte fraca no repositorio e pior que
 * arte que ainda nao existe.
 */
public final class RodaDeNen extends Screen {

    private static final double RAIO_INTERNO = 34.0D;
    private static final double RAIO_EXTERNO = 92.0D;
    private static final double RAIO_DO_ROTULO = 68.0D;

    private static final int COR_FUNDO = 0xC0_04_0A_10;
    private static final int COR_FATIA = 0x80_0E_1C_24;
    private static final int COR_FATIA_APONTADA = 0xC0_10_54_60;
    private static final int COR_ATIVA = 0xFF_3A_E8_D0;
    private static final int COR_TEXTO = 0xFF_D8_F4_FF;
    private static final int COR_TEXTO_APAGADO = 0xFF_6A_8A_96;

    private final NenClientCache cache;
    private List<ResourceLocation> fatias = List.of();
    private OptionalInt apontada = OptionalInt.empty();

    public RodaDeNen(NenClientCache cache) {
        super(Component.translatable("nenfoundation.roda.titulo"));
        this.cache = cache;
    }

    @Override
    protected void init() {
        // A lista e relida na abertura, e nao guardada entre aberturas: um
        // snapshot pode ter chegado desde a ultima vez, e uma roda com a lista
        // velha ofereceria uma tecnica que o jogador perdeu.
        // Lido do SNAPSHOT, que e o estado autoritativo que chegou do
        // servidor. Sem snapshot ainda, a roda nasce vazia e diz isso.
        this.fatias = this.cache.snapshot()
                .map(s -> List.copyOf(s.tecnicasDesbloqueadas()))
                .orElse(List.of());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        // Soltar a tecla fecha. Nao existe evento de "soltou" dentro de uma
        // tela; perguntar a cada tick e o caminho que existe.
        if (!NenKeybinds.RODA_DE_NEN.isDown()) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float parcial) {
        super.render(g, mouseX, mouseY, parcial);

        int cx = this.width / 2;
        int cy = this.height / 2;

        this.apontada = GeometriaDaRoda.fatiaEm(
                mouseX - (double) cx, mouseY - (double) cy, this.fatias.size(), RAIO_INTERNO);

        if (this.fatias.isEmpty()) {
            desenharVazia(g, cx, cy);
            return;
        }
        desenharFatias(g, cx, cy);
        desenharCentro(g, cx, cy);
        desenharRodape(g, cx, cy);
    }

    /**
     * A roda sem nenhuma tecnica DIZ isso.
     *
     * <p>Uma roda vazia e silenciosa e indistinguivel de uma roda quebrada, e o
     * relato de bug vira "abri e nao apareceu nada".
     */
    private void desenharVazia(GuiGraphics g, int cx, int cy) {
        anelDeFundo(g, cx, cy);
        centralizado(g, Component.translatable("nenfoundation.roda.vazia"), cx, cy - 4, COR_TEXTO);
        centralizado(g, Component.translatable("nenfoundation.roda.vazia_dica"),
                cx, cy + 8, COR_TEXTO_APAGADO);
    }

    private void desenharFatias(GuiGraphics g, int cx, int cy) {
        anelDeFundo(g, cx, cy);

        for (int i = 0; i < this.fatias.size(); i++) {
            ResourceLocation id = this.fatias.get(i);
            boolean ativa = estaAtiva(id);
            boolean sobEsta = this.apontada.isPresent() && this.apontada.getAsInt() == i;

            double angulo = GeometriaDaRoda.anguloCentralDaFatia(i, this.fatias.size());
            int x = cx + (int) Math.round(Math.sin(angulo) * RAIO_DO_ROTULO);
            int y = cy - (int) Math.round(Math.cos(angulo) * RAIO_DO_ROTULO);

            // Um retangulo por fatia, centrado no angulo dela. Nao e um setor
            // de circulo: desenhar setor exige malha propria, e a issue diz
            // que icone e arte ficam fora. A SELECAO e radial de verdade; o
            // que e aproximado e so o desenho.
            int meiaLargura = 46;
            int meiaAltura = 11;
            g.fill(x - meiaLargura, y - meiaAltura, x + meiaLargura, y + meiaAltura,
                    sobEsta ? COR_FATIA_APONTADA : COR_FATIA);
            if (ativa) {
                g.renderOutline(x - meiaLargura, y - meiaAltura,
                        meiaLargura * 2, meiaAltura * 2, COR_ATIVA);
            }

            centralizado(g, nomeDe(id), x, y - 4, ativa ? COR_ATIVA : COR_TEXTO);
        }
    }

    private void desenharCentro(GuiGraphics g, int cx, int cy) {
        centralizado(g, Component.translatable("nenfoundation.roda.centro"),
                cx, cy - 4, COR_TEXTO);
    }

    private void desenharRodape(GuiGraphics g, int cx, int cy) {
        int y = cy + (int) RAIO_EXTERNO + 14;
        if (this.apontada.isEmpty()) {
            centralizado(g, Component.translatable("nenfoundation.roda.aponte"),
                    cx, y, COR_TEXTO_APAGADO);
            return;
        }
        ResourceLocation id = this.fatias.get(this.apontada.getAsInt());
        boolean ativa = estaAtiva(id);
        centralizado(g, Component.translatable(
                        ativa ? "nenfoundation.roda.clique_desligar" : "nenfoundation.roda.clique_ligar",
                        nomeDe(id)),
                cx, y, COR_TEXTO);
    }

    private void anelDeFundo(GuiGraphics g, int cx, int cy) {
        int r = (int) RAIO_EXTERNO + 22;
        g.fill(cx - r, cy - r, cx + r, cy + r, COR_FUNDO);
    }

    private void centralizado(GuiGraphics g, Component texto, int cx, int y, int cor) {
        g.drawString(this.font, texto,
                cx - this.font.width(texto) / 2, y, cor, false);
    }

    /**
     * O nome visivel de uma tecnica.
     *
     * <p>Chave derivada do id, e nao uma tabela a parte: uma tabela divergiria
     * do registro no dia em que alguem acrescentasse uma tecnica, e o sintoma
     * seria a chave crua na tela sem erro nenhum no log.
     */
    private static Component nomeDe(ResourceLocation id) {
        return Component.translatable("nenfoundation.tecnica." + id.getPath());
    }

    @Override
    public boolean mouseClicked(double x, double y, int botao) {
        if (botao != 0 || this.apontada.isEmpty() || this.fatias.isEmpty()) {
            return super.mouseClicked(x, y, botao);
        }
        ResourceLocation id = this.fatias.get(this.apontada.getAsInt());

        // ALTERNA. O estado lido e o do servidor, nao um palpite local -- e o
        // que sai daqui e INTENCAO. Se o servidor recusar, a fatia nao muda, e
        // o feedback de erro chega pelo caminho normal.
        if (estaAtiva(id)) {
            PacketDistributor.sendToServer(new DesativarTecnicaC2S(id));
        } else {
            PacketDistributor.sendToServer(new AtivarTecnicaC2S(id));
        }
        return true;
    }

    /**
     * Se a tecnica esta ativa SEGUNDO O SERVIDOR.
     *
     * <p>Vem do delta, e nao de um palpite local. E o que sustenta a regra de
     * nao haver mutacao otimista: a fatia so muda quando o servidor confirma.
     */
    private boolean estaAtiva(ResourceLocation id) {
        return this.cache.delta()
                .map(d -> d.tecnicasAtivas().contains(id))
                .orElse(false);
    }

    /** Abre a roda, se houver jogador e nenhuma outra tela no caminho. */
    public static void abrir(NenClientCache cache) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && mc.screen == null) {
            mc.setScreen(new RodaDeNen(cache));
        }
    }
}
