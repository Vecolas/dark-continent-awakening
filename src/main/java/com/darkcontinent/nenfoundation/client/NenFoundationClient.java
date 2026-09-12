package com.darkcontinent.nenfoundation.client;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.client.keybind.NenKeybinds;
import com.darkcontinent.nenfoundation.client.screen.OverlayDeDebug;
import com.darkcontinent.nenfoundation.client.screen.OverlayDeAura;
import com.darkcontinent.nenfoundation.client.screen.TelaDoJogador;
import com.darkcontinent.nenfoundation.client.render.EnemyRenderers;
import com.darkcontinent.nenfoundation.client.hud.AparenciaDeTecnica;
import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.client.vfx.AuraRenderLod;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualState;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualSystem;
import com.darkcontinent.nenfoundation.client.vfx.render.AuraRenderRegistro;
import com.darkcontinent.nenfoundation.client.vfx.EstadoVisualDeTerceiro;
import com.darkcontinent.nenfoundation.client.vfx.AuraDistribution;
import com.darkcontinent.nenfoundation.client.vfx.EmissorDeParticulasDeAura;
import com.darkcontinent.nenfoundation.client.vfx.ModoVisualDeTecnica;
import com.darkcontinent.nenfoundation.client.vfx.SessaoDeVfxDeAura;
import com.darkcontinent.nenfoundation.config.NenClientConfig;
import net.neoforged.fml.config.ModConfig;
import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.network.handler.Recebedores;
import com.darkcontinent.nenfoundation.network.payload.AjustarOutputC2S;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.network.PacketDistributor;
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

    /** Instancia unica; o Minecraft so carrega um mod client por JVM. */
    private static NenFoundationClient INSTANCIA;

    private final NenClientCache cache;
    private final OverlayDeDebug overlay;
    private final OverlayDeAura auraHud;
    private int errosExibidos;
    private final SessaoDeVfxDeAura vfx;
    private long ticksDaSessao;

    public NenFoundationClient(IEventBus modEventBus, ModContainer modContainer) {
        INSTANCIA = this;
        // O contador de ticks do cliente, perguntado na hora do uso.
        this.cache = new NenClientCache(
                () -> this.ticksDaSessao, NenConfig::devModeAtivo,
                NenConfig::interpolacaoDeAura, NenConfig::interpolacaoDeOutput);
        this.overlay = new OverlayDeDebug(this.cache);
        this.auraHud = new OverlayDeAura(this.cache);
        this.vfx = new SessaoDeVfxDeAura();

        Recebedores.registrar(this.cache);

        modContainer.registerConfig(ModConfig.Type.CLIENT, NenClientConfig.SPEC);

        modEventBus.addListener(NenKeybinds::registrar);
        modEventBus.addListener(EnemyRenderers::registrar);
        modEventBus.addListener(AuraRenderRegistro::registrarDefinicoes);
        modEventBus.addListener(AuraRenderRegistro::adicionarLayers);

        // A LAYER NAO CONHECE CACHE NEM REDE. Ela pergunta ao
        // AuraVisualSystem, e quem sabe responder e este objeto -- que tem a
        // sessao do jogador local e o cache dos sinais dos outros.
        AuraVisualSystem.ligar(this::estadoVisualDe);

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
        // QUEM LIGA, DESLIGA, e no MESMO ponto de saida que ja existia. Sem
        // esta linha a aura do mundo anterior continuaria desenhada ate o
        // primeiro delta do servidor novo chegar -- e, se ele nunca chegar
        // porque o jogador nao despertou la, para sempre.
        this.vfx.limpar();
        // A FONTE DO AuraVisualSystem NAO E DESLIGADA AQUI, e isso e
        // deliberado. `ligar` acontece uma vez, no construtor, e este objeto
        // vive tanto quanto o mod; desligar no logout deixaria a aura morta
        // para sempre a partir do segundo servidor -- sem erro nenhum. O que
        // precisa ser limpo por sessao e o ESTADO, e ele acabou de ser: o
        // cache acima e a sessao de vfx na linha anterior.
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
        this.tickDaAura(mc);
        while (NenKeybinds.OVERLAY_DE_DEBUG.consumeClick()) {
            this.overlay.alternar();
            LOG.debug("Overlay de debug: {}", this.overlay.visivel() ? "ligado" : "desligado");
        }
        // A RODA ABRE NO PRIMEIRO FRAME EM QUE A TECLA ESTA DESCIDA, e fecha
        // sozinha quando ela sobe -- a propria tela pergunta isso no tick.
        // `consumeClick` nao serve aqui: ele conta pressionadas, e o gesto e
        // "enquanto segurada". E a leitura e da tecla FISICA, e nao do
        // KeyMapping: abrir a tela chama KeyMapping.releaseAll(), entao o
        // KeyMapping mente justamente enquanto a roda esta aberta.
        if (com.darkcontinent.nenfoundation.client.screen.RodaDeNen.teclaDaRodaDescida()
                && mc.screen == null) {
            com.darkcontinent.nenfoundation.client.screen.RodaDeNen.abrir(this.cache);
        }

        while (NenKeybinds.AJUSTAR_OUTPUT.consumeClick()) {
            if (mc.player != null && mc.level != null && mc.screen == null) {
                float variacao = Screen.hasShiftDown() ? -0.10F : +0.10F;
                PacketDistributor.sendToServer(new AjustarOutputC2S(variacao));
            }
        }
    }

    /**
     * Um tick da aura visual do jogador LOCAL.
     *
     * <p>SO O JOGADOR LOCAL, e isso e limite de dado e nao escolha de escopo: o
     * servidor manda o delta de runtime apenas ao dono do perfil, por decisao
     * de privacidade declarada em {@code NenSyncService}. O cliente nao tem
     * como saber que tecnica o jogador do lado esta usando -- e nem deveria,
     * enquanto In e Zetsu existirem. Ver o relato da issue #99.
     *
     * <p>A INTENSIDADE VEM DO OUTPUT, e nao da aura atual. Aura e combustivel;
     * output e o quanto esta sendo liberado. Ligar o brilho a aura faria a aura
     * caindo APAGAR o efeito justamente enquanto o jogador esta gastando.
     */
    private void tickDaAura(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        var delta = this.cache.delta();
        var ativas = delta.map(d -> d.tecnicasAtivas()).orElse(java.util.Set.of());
        float output = delta.map(d -> d.outputPercent()).orElse(0.0F);
        int cor = ModoVisualDeTecnica.dominante(ativas)
                .map(id -> AparenciaDeTecnica.de(id).cor())
                .orElse(0xFFFFFFFF);

        // A DISTRIBUICAO VEM DO DELTA, e nao de um palpite do cliente: ela e
        // derivada no servidor a partir das tecnicas ativas (ADR-014).
        var distribuicao = delta
                .map(d -> AuraDistribution.daAlocacao(d.alocacao()))
                .orElseGet(AuraDistribution::uniforme);
        this.vfx.aoTick(ativas, output, cor, NenClientConfig.passoDeTransicao(), distribuicao);
        double densidade = NenClientConfig.densidadeDeParticulas();
        EmissorDeParticulasDeAura.emitir(mc.level, mc.player, this.vfx.estado(), densidade);

        this.tickDaAuraDosOutros(mc, densidade);
    }

    /**
     * O estado visual de um jogador, para quem vai DESENHAR.
     *
     * <p>E a fonte que o {@link AuraVisualSystem} entrega a layer, e a razao de
     * ela existir: a layer nao pode conhecer cache nem rede, senao cada renderer
     * novo reimplementaria esta regra e os dois divergiriam.
     *
     * <p>DOIS CAMINHOS, e eles nao sao simetricos de proposito. O jogador local
     * tem um interpolador com transicao; os outros sao DERIVADOS do sinal, sem
     * estado guardado -- porque estado por entidade precisaria ser limpo quando
     * ela sai do alcance, desloga, morre ou troca de dimensao, e um desses
     * caminhos sempre fica para tras.
     *
     * <p>O cliente nao sabe que tecnica o vizinho ligou. Ele recebe um sinal de
     * tres valores ja filtrado pelo servidor, e quem esta em Zetsu chega como
     * NENHUM -- igual a quem nunca despertou.
     */
    private AuraVisualState estadoVisualDe(net.minecraft.world.entity.player.Player jogador) {
        Minecraft mc = Minecraft.getInstance();
        if (jogador == null || mc.player == null) {
            return AuraVisualState.desligado();
        }
        if (jogador == mc.player) {
            return this.vfx.estado();
        }
        var sinal = this.cache.presencaDe(jogador.getId());
        if (sinal == SinalDeAura.NENHUM) {
            return AuraVisualState.desligado();
        }
        return EstadoVisualDeTerceiro.de(sinal,
                AuraRenderLod.porDistancia(mc.player.distanceTo(jogador)));
    }

    /**
     * A aura de QUEM ESTA POR PERTO.
     *
     * <p>O cliente nao sabe -- e nao deve saber -- que tecnicas os outros
     * ligaram. Ele recebe um sinal de tres valores ja filtrado pelo servidor,
     * e quem esta em Zetsu chega como NENHUM, igual a quem nunca despertou.
     *
     * <p>SEM CONTROLADOR POR JOGADOR, de proposito. Guardar um interpolador
     * para cada pessoa em volta cria estado por entidade que alguem precisa
     * limpar quando ela sai do alcance -- e esse alguem sempre esquece um
     * caminho. O estado visual dos outros e derivado do sinal, e some sozinho
     * quando o sinal some.
     */
    private void tickDaAuraDosOutros(Minecraft mc, double densidade) {
        for (var outro : mc.level.players()) {
            if (outro == mc.player) {
                continue;
            }
            var sinal = this.cache.presencaDe(outro.getId());
            if (sinal == SinalDeAura.NENHUM) {
                continue;
            }

            // AQUI O LOD FINALMENTE RECEBE DISTANCIA DE VERDADE. Para o proprio
            // jogador ela e sempre zero, entao ate agora ele so tinha teste
            // unitario -- o corte por distancia nunca mordia em jogo.
            AuraRenderLod lod = AuraRenderLod.porDistancia(mc.player.distanceTo(outro));
            if (lod == AuraRenderLod.HIDDEN) {
                continue;
            }

            var estado = EstadoVisualDeTerceiro.de(sinal, lod);
            EmissorDeParticulasDeAura.emitir(mc.level, outro, estado, densidade);
        }
    }
}
