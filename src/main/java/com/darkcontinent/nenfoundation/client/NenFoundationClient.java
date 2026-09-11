package com.darkcontinent.nenfoundation.client;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.client.keybind.NenKeybinds;
import com.darkcontinent.nenfoundation.client.screen.OverlayDeDebug;
import com.darkcontinent.nenfoundation.client.screen.OverlayDeAura;
import com.darkcontinent.nenfoundation.client.screen.TelaDoJogador;
import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.network.handler.Recebedores;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ponto de entrada CLIENT-ONLY.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. A fronteira client/server e imposta por {@code dist = Dist.CLIENT}. Uma
 * classe client-only alcancada pelo servidor dedicado nao falha na compilacao,
 * nao falha em singleplayer e nao falha em {@code runClient}. Ela falha no
 * servidor de verdade, com {@code NoClassDefFoundError}, na frente dos
 * jogadores.
 *
 * <p>2. QUEM LIGA, DESLIGA. O cache e registrado como
 * {@code RecebedorDeNen} aqui, e o par — {@link Recebedores#limpar()} — mora no
 * ciclo de vida DESTE objeto, e nao espalhado pelos varios lugares de onde se
 * pode sair de um servidor.
 *
 * <p>3. O cache e limpo no LOGOUT, nao no login. Limpar no login deixa o
 * intervalo entre sair de um servidor e entrar em outro com o perfil do
 * anterior visivel — dado de outro mundo na tela, sem nada acusar.
 *
 * <p>4. A animacao consulta o relogio da sessao, nao o tempo do mundo que
 * pode saltar quando chega um pacote de sincronizacao ou muda a dimensao.
 *
 * <p>REGRA DE DEPENDENCIA: {@code nen/*}, {@code api/*}, {@code network/*} e
 * {@code server/*} NUNCA importam nada de {@code client/*}. A seta aponta so
 * para um lado, e o portao {@code PacotesDeclaradosTest} reprova quem inverter.
 */
@Mod(value = NenFoundation.MOD_ID, dist = Dist.CLIENT)
public final class NenFoundationClient {

    private static final Logger LOG = LoggerFactory.getLogger(NenFoundationClient.class);

    private final NenClientCache cache;
    private final OverlayDeDebug overlay;
    private final OverlayDeAura auraHud;
    private int errosExibidos;
    private long ticksDaSessao;

    public NenFoundationClient(IEventBus modEventBus, ModContainer modContainer) {
        // O contador de ticks do cliente, perguntado na hora do uso.
        this.cache = new NenClientCache(
                () -> this.ticksDaSessao, NenConfig::devModeAtivo, NenConfig::interpolacaoDeAura);
        this.overlay = new OverlayDeDebug(this.cache);
        this.auraHud = new OverlayDeAura(this.cache);

        Recebedores.registrar(this.cache);

        modEventBus.addListener(NenKeybinds::registrar);

        NeoForge.EVENT_BUS.addListener(this::aoSairDoServidor);
        NeoForge.EVENT_BUS.addListener(this::aoTickDoCliente);
        NeoForge.EVENT_BUS.addListener(this.overlay::aoRenderizar);
        NeoForge.EVENT_BUS.addListener(this.auraHud::aoRenderizar);

        LOG.debug("Camada de cliente do Nen Foundation carregada.");
    }

    /**
     * Ao sair de um servidor, o cliente esquece o que sabia.
     *
     * <p>Sem isto, entrar em outro servidor mostra o perfil do anterior ate o
     * primeiro snapshot chegar. E enquanto o servidor novo nao mandar nada —
     * porque o jogador nao despertou, por exemplo — ele mostra para sempre.
     */
    private void aoSairDoServidor(ClientPlayerNetworkEvent.LoggingOut evento) {
        this.cache.limpar();
        this.errosExibidos = 0;
        this.ticksDaSessao = 0;
    }

    private void aoTickDoCliente(ClientTickEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && !mc.isPaused()) this.ticksDaSessao++;
        while (NenKeybinds.FICHA_DO_JOGADOR.consumeClick()) {
            // Nunca roubar teclas do chat, inventario ou de outra tela.
            if (mc.player != null && mc.level != null && mc.screen == null) {
                mc.setScreen(new TelaDoJogador(this.cache));
            }
        }
        // A ultima recusa fica na action bar mesmo com overlay de debug desligado.
        // Rajadas recebidas no mesmo tick substituem o texto, sem inundar o chat.
        if (this.errosExibidos != this.cache.errosRecebidos()
                && Minecraft.getInstance().player != null) {
            this.cache.ultimoErro().ifPresent(chave -> Minecraft.getInstance().player
                    .displayClientMessage(net.minecraft.network.chat.Component.translatable(chave), true));
            this.errosExibidos = this.cache.errosRecebidos();
        }
        while (NenKeybinds.OVERLAY_DE_DEBUG.consumeClick()) {
            this.overlay.alternar();
            LOG.debug("Overlay de debug: {}", this.overlay.visivel() ? "ligado" : "desligado");
        }
    }
}
