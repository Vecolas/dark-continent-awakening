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
import net.minecraft.network.chat.Component;
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
 * <p>O QUE ELE NAO CONSEGUE TRAVAR, e esta declarado em vez de fingido: a
 * <b>pose</b> (parar de andar e trabalho de quem esta no teclado) e a
 * <b>distancia real de camera</b>, que em terceira pessoa e fixa pelo jogo,
 * exceto quando ha parede atras -- e ai ela encurta sem avisar. Capturar de
 * costas para um muro produz um enquadramento diferente com o mesmo nome.
 *
 * <p>CLIENT-ONLY.
 */
public final class AuraCaptureMode {

    private static final Logger LOG = LoggerFactory.getLogger(AuraCaptureMode.class);

    /** Onde as capturas caem, dentro de {@code screenshots/}. */
    public static final String PASTA = "nenfoundation-av";

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
