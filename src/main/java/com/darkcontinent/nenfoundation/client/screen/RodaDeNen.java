package com.darkcontinent.nenfoundation.client.screen;

import com.darkcontinent.nenfoundation.client.NenClientCache;
import com.darkcontinent.nenfoundation.client.hud.AparenciaDeTecnica;
import com.darkcontinent.nenfoundation.client.keybind.NenKeybinds;
import com.darkcontinent.nenfoundation.network.payload.AtivarTecnicaC2S;
import com.darkcontinent.nenfoundation.network.payload.DesativarTecnicaC2S;
import java.util.List;
import java.util.OptionalInt;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
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
    private static final int COR_TEXTO = 0xFF_D8_F4_FF;
    private static final int COR_TEXTO_APAGADO = 0xFF_6A_8A_96;
    private static final int COR_MIOLO = 0xD0_02_08_0C;

    /** Fresta entre fatias, em radianos. Sem ela a roda vira um disco so. */
    private static final double FOLGA_ENTRE_FATIAS = 0.02D;

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
        // A TECLA E LIDA DO SISTEMA, e NAO do KeyMapping. Isto e a correcao de
        // um bug que so aparece em jogo:
        //
        // `Minecraft.setScreen` chama `KeyMapping.releaseAll()` ao abrir uma
        // tela. Entao `RODA_DE_NEN.isDown()` vira FALSE no instante em que a
        // roda abre -- e este tick a fechava na hora. Como a tecla fisica
        // continuava descida, o tick do cliente reabria, e `setScreen` chamava
        // `mouseHandler.releaseMouse()` de novo, que RECENTRA O CURSOR.
        //
        // O resultado era um laco abre-fecha por tick: o mouse puxado para o
        // meio o tempo todo, e o clique quase impossivel de acertar. Nada
        // disso da erro; so fica intragavel.
        if (!teclaDaRodaDescida()) {
            onClose();
        }
    }

    /**
     * Se a tecla da roda esta descida AGORA, perguntando ao sistema.
     *
     * <p>Trata tecla e botao de mouse, porque a ligacao e remapeavel e alguem
     * vai ligar isto num botao lateral do mouse.
     */
    public static boolean teclaDaRodaDescida() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) {
            return false;
        }
        InputConstants.Key tecla = NenKeybinds.RODA_DE_NEN.getKey();
        long janela = mc.getWindow().getWindow();

        return switch (tecla.getType()) {
            case KEYSYM -> tecla.getValue() != InputConstants.UNKNOWN.getValue()
                    && InputConstants.isKeyDown(janela, tecla.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(janela, tecla.getValue()) == GLFW.GLFW_PRESS;
            // SCANCODE nao tem consulta direta; cair no KeyMapping aqui e pior
            // que nada, entao a roda simplesmente nao fecha sozinha -- e ESC
            // continua funcionando.
            default -> true;
        };
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

        int total = this.fatias.size();
        double porFatia = 2.0D * Math.PI / total;

        for (int i = 0; i < total; i++) {
            ResourceLocation id = this.fatias.get(i);
            boolean ativa = estaAtiva(id);
            boolean sobEsta = this.apontada.isPresent() && this.apontada.getAsInt() == i;

            double centro = GeometriaDaRoda.anguloCentralDaFatia(i, total);

            // O SETOR E CENTRADO no mesmo angulo que a selecao usa, e vai de
            // meia fatia para cada lado. Qualquer outro recorte faria o desenho
            // discordar do clique.
            double de = centro - porFatia / 2.0D + FOLGA_ENTRE_FATIAS;
            double ate = centro + porFatia / 2.0D - FOLGA_ENTRE_FATIAS;

            int cor = sobEsta ? COR_FATIA_APONTADA : COR_FATIA;
            DesenhoDaRoda.setorDeAnel(g, cx, cy,
                    (float) RAIO_INTERNO, (float) RAIO_EXTERNO, de, ate, cor);

            // A COR DA TECNICA VEM DE `AparenciaDeTecnica`, e nao daqui. O
            // indicador da HUD pinta Ren de laranja; se esta tela pintasse de
            // outra cor, as duas discordariam sobre a mesma coisa e so quem
            // olhasse as duas ao mesmo tempo notaria.
            int corDaTecnica = AparenciaDeTecnica.de(id).cor();

            if (ativa) {
                // Uma faixa fina na borda externa marca a tecnica ligada.
                DesenhoDaRoda.setorDeAnel(g, cx, cy,
                        (float) (RAIO_EXTERNO - 4.0D), (float) RAIO_EXTERNO, de, ate,
                        corDaTecnica);
            }

            int x = cx + (int) Math.round(Math.sin(centro) * RAIO_DO_ROTULO);
            int y = cy - (int) Math.round(Math.cos(centro) * RAIO_DO_ROTULO);
            centralizado(g, nomeDe(id), x, y - 4, ativa ? corDaTecnica : COR_TEXTO);
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
        DesenhoDaRoda.anel(g, cx, cy,
                (float) (RAIO_INTERNO - 6.0D), (float) (RAIO_EXTERNO + 6.0D), COR_FUNDO);
        DesenhoDaRoda.disco(g, cx, cy, (float) (RAIO_INTERNO - 6.0D), COR_MIOLO);
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
