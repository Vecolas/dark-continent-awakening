package com.darkcontinent.nenfoundation.client.vfx.debug;

import com.darkcontinent.nenfoundation.client.vfx.SobreposicaoDeVfx;
import java.io.File;
import java.time.LocalDate;
import java.util.function.Consumer;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.Input;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * O modo de captura: tudo que precisa estar igual entre duas imagens, travado
 * de uma vez so.
 *
 * <p>POR QUE ELE EXISTE. O protocolo A/B de
 * {@code docs/testing/av-aura-visual.md} secao 3 exige que duas capturas
 * comparadas tenham o mesmo local, o mesmo FOV, a mesma hora, a mesma skin, a
 * mesma distancia de camera e a mesma versao de assets. Sem um modo que trave
 * isso, cada imagem sai com uma hora diferente e uma pose diferente -- e a
 * comparacao, que e o metodo de aprovacao desta trilha inteira, vira achismo.
 *
 * <p><b>A HORA E TRAVADA NO VALOR EM QUE ESTAVA</b>, e nao num meio-dia fixo. A
 * matriz pede {@code ten_dia}, {@code ten_noite} e {@code ten_caverna}: um
 * meio-dia embutido aqui tornaria duas dessas tres impossiveis. Quem quer noite
 * poe a noite e SO ENTAO liga o modo.
 *
 * <p>QUEM LIGA, DESLIGA. O tipo de camera e o estado do HUD sao guardados na
 * ativacao e devolvidos na saida -- inclusive quando a saida e o logout. Sem
 * isso, sair do modo deixaria o jogo sem HUD e em terceira pessoa, e o sintoma
 * pareceria um bug do mod para quem nao lembra de ter ligado nada.
 *
 * <p>ELE NAO TOCA NO SERVIDOR. Hora e clima sao escritos no {@code ClientLevel}
 * -- sao o que ESTE cliente desenha, e nao o que o mundo e. O servidor continua
 * mandando o horario dele; enquanto o modo estiver ligado, o valor travado e
 * reescrito a cada tick por cima. Nenhum jogador do lado ve diferenca.
 *
 * <p>A POSE IDLE E TRAVADA neutralizando o input de movimento local. Isso nao
 * transforma o cliente em autoridade: o modo nao teleporta, nao congela a
 * entidade no servidor e nao altera hitbox. Ele apenas deixa de pedir
 * movimento enquanto a bancada esta ligada.
 *
 * <p>A CAMERA pede sempre quatro blocos de recuo, mas a colisao vanilla roda
 * depois e continua autorizada a aproxima-la. Ignorar parede aqui seria
 * oferecer visao atraves de blocos em nome de uma captura. A arena precisa ter
 * espaco livre atras do jogador para a distancia real continuar comparavel.
 *
 * <p>CLIENT-ONLY.
 */
public final class AuraCaptureMode {

    private static final Logger LOG = LoggerFactory.getLogger(AuraCaptureMode.class);

    /** Onde as capturas caem, dentro de {@code screenshots/}. */
    public static final String PASTA = "nenfoundation-av";

    /** Recuo pedido antes de a colisao vanilla limitar a camera. */
    static final float DISTANCIA_DA_CAMERA = 4.0F;

    private static final AuraCaptureMode INSTANCIA = new AuraCaptureMode();

    private final RoteiroDeCaptura roteiro = new RoteiroDeCaptura();

    private boolean ligado;
    private long horaTravada;
    private CameraType cameraAnterior;
    private boolean hudAnterior;

    /**
     * O prefixo do lote em andamento -- {@code dia}, {@code noite},
     * {@code caverna}, {@code correndo}.
     *
     * <p>ELE E O EIXO QUE O LOTE NAO CONSEGUE PERCORRER SOZINHO. Ambiente e
     * pose exigem mover o jogador ou o mundo, e o lote nao faz isso; o que ele
     * faz e carimbar nas seis imagens de qual condicao elas sao. Sem isso, duas
     * rodadas em lugares diferentes produzem arquivos com o MESMO nome, e a
     * segunda sobrescreve a primeira em silencio.
     */
    private String etiqueta = "captura";

    private AuraCaptureMode() {
    }

    public static AuraCaptureMode instancia() {
        return INSTANCIA;
    }

    public boolean ligado() {
        return this.ligado;
    }

    public boolean emLote() {
        return this.roteiro.ativo();
    }

    /** Quantos passos do lote ja sairam, e de quantos. Para o overlay. */
    public String progressoDoLote() {
        return this.roteiro.concluidos() + "/" + this.roteiro.total();
    }

    /**
     * Liga, guardando o que vai precisar ser devolvido.
     *
     * @return falso se ja estava ligado -- e quem chama recusa COM MOTIVO
     */
    public boolean ligar(Minecraft mc) {
        if (this.ligado || mc.level == null) {
            return false;
        }
        this.horaTravada = mc.level.getDayTime();
        this.cameraAnterior = mc.options.getCameraType();
        this.hudAnterior = mc.options.hideGui;
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        mc.options.hideGui = true;
        this.ligado = true;
        LOG.info("Modo de captura LIGADO: hora travada em {}, HUD escondido, terceira pessoa.",
                this.horaTravada);
        return true;
    }

    /**
     * Desliga e devolve tudo.
     *
     * <p>Chamado pelo comando E pelo logout. Ser chamado duas vezes nao pode
     * doer: a segunda nao encontra nada para devolver e sai calada.
     */
    public void desligar(Minecraft mc) {
        if (!this.ligado) {
            return;
        }
        this.roteiro.cancelar();
        if (this.cameraAnterior != null) {
            mc.options.setCameraType(this.cameraAnterior);
        }
        mc.options.hideGui = this.hudAnterior;
        this.cameraAnterior = null;
        this.ligado = false;
        LOG.info("Modo de captura DESLIGADO: camera e HUD devolvidos.");
    }

    /** Comeca o lote sob uma etiqueta. Devolve falso se o modo nao estiver ligado. */
    public boolean iniciarLote(String etiqueta) {
        if (!this.ligado) {
            return false;
        }
        this.etiqueta = NomeDeCaptura.sanear(etiqueta, 24, "captura");
        this.roteiro.iniciar();
        return true;
    }

    /**
     * Um tick do modo: reaplica os travamentos e anda com o lote.
     *
     * <p>REAPLICA TODO TICK, e nao uma vez na ativacao. O servidor continua
     * mandando a hora dele, e o clima muda sozinho: um valor escrito so na
     * ativacao seria sobrescrito em segundos, e o sintoma seria "o modo de
     * captura nao funciona" sem nenhum erro para procurar.
     */
    public void aoTick(Minecraft mc) {
        if (!this.ligado || mc.level == null) {
            return;
        }
        mc.level.setDayTime(this.horaTravada);
        mc.level.setRainLevel(0.0F);
        mc.level.setThunderLevel(0.0F);
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        mc.options.hideGui = true;

        RoteiroDeCaptura.Resultado passo = this.roteiro.aoQuadro();
        switch (passo.acao()) {
            case APLICAR -> aplicar(passo.passo());
            case CAPTURAR -> capturar(mc, this.etiqueta + "_" + passo.passo().nome());
            case ENCERRAR -> {
                // O LOTE DEVOLVE O CONTROLE AO SERVIDOR ao terminar. Deixar o
                // ultimo passo ligado faria a proxima captura manual sair com o
                // estado do fim do lote, e ninguem lembraria disso.
                SobreposicaoDeVfx.forcarModo(null);
                SobreposicaoDeVfx.forcarOutput(-1.0F);
                SobreposicaoDeVfx.forcarDensidade(-1.0F);
                avisar(mc, Component.translatable("nenfoundation.vfx.lote_concluido",
                        this.roteiro.total(), PASTA));
            }
            case NADA -> {
                // Esperando o estado assentar.
            }
        }
    }

    /**
     * Neutraliza o pedido local de movimento para a pose assentar em idle.
     *
     * <p>O evento acontece depois de {@link Input#tick(boolean, float)}, entao
     * limpar aqui ganha inclusive de uma tecla que continua fisicamente
     * pressionada. Fora do modo, nem lemos os campos.
     */
    public void aoAtualizarMovimento(MovementInputUpdateEvent evento) {
        if (neutralizarSeLigado(this.ligado, evento.getInput())) {
            evento.getEntity().setSprinting(false);
        }
    }

    /**
     * Fixa o recuo solicitado; o raycast vanilla ainda pode reduzi-lo.
     */
    public void aoCalcularDistanciaDaCamera(CalculateDetachedCameraDistanceEvent evento) {
        evento.setDistance(distanciaSolicitada(this.ligado, evento.getDistance()));
    }

    static boolean neutralizarSeLigado(boolean ligado, Input input) {
        if (!ligado) {
            return false;
        }
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
        return true;
    }

    static float distanciaSolicitada(boolean ligado, float distanciaAtual) {
        return ligado ? DISTANCIA_DA_CAMERA : distanciaAtual;
    }

    private static void aplicar(RoteiroDeCaptura.Passo passo) {
        SobreposicaoDeVfx.forcarModo(passo.modo());
        // O OUTPUT VAI A 1.0 EM TODO PASSO DO LOTE. Sem isto, uma sessao
        // anterior que tenha baixado o output entregaria um lote inteiro com a
        // aura fraca -- e a comparacao com a referencia acusaria a shell, que
        // esta certa. O aviso esta em av-evidencias.md secao 6.
        SobreposicaoDeVfx.forcarOutput(1.0F);
        SobreposicaoDeVfx.forcarDensidade(passo.densidade());
    }

    /**
     * Fotografa agora, com o nome do protocolo.
     *
     * @param nome o que a captura mostra; o resto do nome e montado aqui
     */
    public void capturar(Minecraft mc, String nome) {
        File raiz = mc.gameDirectory;
        File destino = new File(new File(raiz, "screenshots"), PASTA);
        if (!destino.isDirectory() && !destino.mkdirs()) {
            // FALHA COM MOTIVO. Uma captura que nao gravou e some em silencio e
            // pior que nenhuma: a pessoa segue a sessao inteira achando que tem
            // catorze imagens e descobre no fim que tem zero.
            avisar(mc, Component.translatable("nenfoundation.vfx.captura_sem_pasta",
                    destino.getAbsolutePath()));
            return;
        }
        String arquivo = PASTA + "/" + NomeDeCaptura.de(nome, LocalDate.now(),
                InfoDeBuild.commit(), NomeDeCaptura.BLOOM_AUSENTE);
        Consumer<Component> relato = mensagem -> avisar(mc, mensagem);
        try {
            Screenshot.grab(raiz, arquivo, mc.getMainRenderTarget(), relato);
        } catch (ReportedException erro) {
            // O Screenshot do jogo embrulha falha de GPU em ReportedException,
            // que derruba o cliente. Uma ferramenta de captura nao pode ser o
            // motivo de um crash durante uma sessao de arte.
            CrashReport relatorio = erro.getReport();
            LOG.error("Falha ao capturar '{}': {}", arquivo,
                    relatorio == null ? erro.getMessage() : relatorio.getTitle());
            avisar(mc, Component.translatable("nenfoundation.vfx.captura_falhou"));
        }
    }

    private static void avisar(Minecraft mc, Component mensagem) {
        if (mc.player != null) {
            mc.player.displayClientMessage(mensagem, false);
        }
    }
}
