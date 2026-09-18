package com.darkcontinent.nenfoundation.client.vfx.render;

import com.darkcontinent.nenfoundation.client.vfx.AuraRenderLod;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualState;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualSystem;
import com.darkcontinent.nenfoundation.client.vfx.CorDaAura;
import com.darkcontinent.nenfoundation.client.vfx.MedidorDeVfx;
import com.darkcontinent.nenfoundation.client.vfx.SondagemDeChao;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilDePressao;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfis;
import com.darkcontinent.nenfoundation.config.NenClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * O anel irregular de pressao sob quem esta em Ren.
 *
 * <p>ELE E O QUARTO ITEM DA HIERARQUIA DE LEITURA -- personagem, borda,
 * filamentos, chao, faiscas --, e responde por cerca de 15% do esforco visual de
 * Ren. O suficiente para dizer "o chao esta sentindo isso"; nem de longe o
 * bastante para competir com o personagem.
 *
 * <p><b>NUNCA UM CIRCULO PERFEITO.</b> Circulo perfeito le como marca de
 * invocacao, e este projeto nao tem magia: tem pressao de energia. O raio e
 * modulado segmento a segmento por harmonicos de frequencia INTEIRA em volta do
 * circulo -- o que garante duas coisas ao mesmo tempo: a forma nao fecha numa
 * costura visivel, e ela nao e um circulo. Ruido branco por segmento daria a
 * irregularidade e abriria a costura.
 *
 * <p><b>SEM SIMBOLO E SEM RUNA.</b> A textura e um arco com falhas; ela nao
 * desenha glifo, pentagrama nem relogio. Isso esta escrito aqui porque e o tipo
 * de coisa que alguem "melhora" seis meses depois sem saber que foi decidido.
 *
 * <p><b>POR QUE UM EVENTO DE MUNDO, E NAO UMA LAYER.</b> A pilha de uma
 * {@code RenderLayer} chega com a rotacao de corpo do jogador e com
 * {@code scale(-1,-1,1)} ja aplicados; desenhar um anel alinhado ao MUNDO dali
 * exigiria desfazer os dois na ordem certa -- e a ordem certa nao e obvia,
 * porque a inversao de eixos e a rotacao em Y nao comutam. Errar isso nao lanca:
 * desenha um anel que gira junto com o jogador. Aqui o espaco e o do mundo por
 * construcao.
 *
 * <p><b>O ANEL NAO TOCA O MUNDO.</b> Nao pinta bloco, nao quebra nada, nao gera
 * {@code ItemEntity}, nao emite luz de bloco e nao decide fisica. O empurrao de
 * Ren, se um dia existir, e sistema de gameplay separado e server-side
 * (ADR-001).
 *
 * <p>QUEM LIGA, DESLIGA: o cache de altura do chao e limpo no MESMO ponto de
 * saida que ja limpa a sessao de vfx -- o logout.
 */
public final class AuraGroundRenderer {

    /** Quanto o anel flutua acima da superficie, em blocos. Menos que isso: z-fighting. */
    private static final float FOLGA_DO_CHAO = 0.02F;

    /**
     * A largura da faixa do anel, em fracao do raio.
     *
     * <p>CONSTANTE DE DESENHO. Uma faixa larga vira um DISCO sob o jogador, e
     * disco e tapete -- nao pressao. O anel e uma borda.
     */
    private static final float FAIXA = 0.38F;

    /** Ate onde o anel e desenhado: o corte do LOD 0-1. */
    private static final double DISTANCIA_MAXIMA = 24.0D;

    /**
     * A sondagem, COMPARTILHADA com os detritos.
     *
     * <p>O raio do anel limita de onde os fragmentos nascem, e os dois precisam
     * do mesmo chao. Duas sondagens com dois ritmos de cache produziriam um anel
     * num degrau e detritos noutro -- sem erro nenhum.
     */
    private final SondagemDeChao sondagem;

    /** Vetores de trabalho: o anel nao aloca nada por quadro. */
    private final Vector3f interno = new Vector3f();
    private final Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);

    public AuraGroundRenderer(SondagemDeChao sondagem) {
        if (sondagem == null) {
            throw new NullPointerException("sondagem de chao obrigatoria");
        }
        this.sondagem = sondagem;
    }

    /** Desenha os aneis de todos os jogadores visiveis em Ren. */
    public void aoRenderizarMundo(RenderLevelStageEvent evento) {
        // DEPOIS DAS PARTICULAS: o anel e translucido e precisa ordenar contra o
        // que ja esta na cena. Antes dos blocos translucidos ele sumiria dentro
        // da agua; depois de tudo, ele passaria por cima dela.
        if (evento.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (!NenClientConfig.qualidade().teto().visivel()) {
            return;
        }

        Camera camera = evento.getCamera();
        Vec3 olho = camera.getPosition();
        float parcial = evento.getPartialTick().getGameTimeDeltaPartialTick(false);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vertices = null;

        for (Player jogador : mc.level.players()) {
            if (jogador.isInvisible() || jogador.isSpectator()) {
                continue;
            }
            double distancia = mc.player.distanceTo(jogador);
            // O ANEL SAI NO LOD 2, junto com os detritos. Ele e o componente
            // mais barato de cortar e o que menos falta faz a vinte e cinco
            // blocos, onde a leitura ja e so a borda.
            if (distancia > DISTANCIA_MAXIMA) {
                continue;
            }
            AuraRenderLod lod = NenClientConfig.qualidade().limitar(
                    com.darkcontinent.nenfoundation.client.vfx.SobreposicaoDeVfx.aplicarNoLod(
                            AuraRenderLod.porDistancia(distancia)));
            if (lod != AuraRenderLod.FULL && lod != AuraRenderLod.NEAR) {
                continue;
            }

            AuraVisualState estado = AuraVisualSystem.estadoDe(jogador);
            if (!estado.enabled() || estado.fases().pressao() <= 0.0F) {
                continue;
            }
            AuraPerfilVisual perfil = AuraPerfis.de(estado);
            AuraPerfilDePressao pressao = perfil.pressao();
            float forca = pressao.anel() * estado.fases().pressao() * estado.intensity()
                    * lod.intensidade();
            if (forca <= 0.004F) {
                continue;
            }

            SondagemDeChao.Amostra chao = this.sondagem.sob(mc.level, jogador, parcial);
            if (!chao.achou()) {
                // SEM CHAO, SEM ANEL. Pular e melhor que desenhar a seis blocos
                // de altura: um anel flutuando no ar nao le como pressao, le
                // como bug -- e e um bug que a pessoa relata sem conseguir dizer
                // o que aconteceu.
                continue;
            }

            if (vertices == null) {
                vertices = buffers.getBuffer(AuraRenderTypes.ground());
            }
            desenharAnel(evento.getPoseStack(), vertices, jogador, estado, pressao, forca,
                    chao, olho, parcial, mc);
        }

        if (vertices != null) {
            buffers.endBatch(AuraRenderTypes.ground());
            MedidorDeVfx.chamadaDeDesenho();
        }
    }

    /**
     * Quem liga, desliga.
     *
     * <p>A SONDAGEM NAO E LIMPA AQUI, e isso e deliberado: ela e compartilhada,
     * e quem a criou -- o ponto de entrada do cliente -- e quem a limpa, no
     * mesmo lugar em que ja limpa o cache e a sessao. Limpar de dois lugares e
     * como um deles acaba esquecido.
     */
    public void limpar() {
        // Nada de estado proprio: os vetores de trabalho sao reaproveitados e
        // nao guardam nada entre quadros.
    }

    /**
     * Um anel, em espaco de MUNDO.
     *
     * <p>A MALHA E UMA TIRA FECHADA: cada segmento e um quad entre o raio
     * interno e o externo. Ela e horizontal por construcao -- nao ha rotacao de
     * corpo nesta pilha --, entao o anel nao gira com o jogador.
     */
    private void desenharAnel(PoseStack pilha, VertexConsumer vertices, Player jogador,
            AuraVisualState estado, AuraPerfilDePressao pressao, float forca, SondagemDeChao.Amostra chao,
            Vec3 olho, float parcial, Minecraft mc) {

        int segmentos = pressao.anelSegmentos();
        if (segmentos < 3) {
            return;
        }
        float raio = pressao.raioPara(estado.intensity());
        if (raio <= 0.0F) {
            return;
        }

        double x = Mth.lerp(parcial, jogador.xo, jogador.getX());
        double z = Mth.lerp(parcial, jogador.zo, jogador.getZ());

        pilha.pushPose();
        pilha.translate(x - olho.x, chao.y() + FOLGA_DO_CHAO - olho.y, z - olho.z);
        Matrix4f matriz = pilha.last().pose();

        // A FASE VEM DO UUID, como a pulsacao da shell: sem ela, dois jogadores
        // lado a lado teriam aneis com exatamente as mesmas reentrancias, e
        // simetria acidental e a coisa mais artificial que um efeito organico
        // pode fazer.
        float fase = (jogador.getUUID().getLeastSignificantBits() & 0xFFFF) / 65535.0F
                * 6.2831855F;
        float tempo = (jogador.tickCount + parcial) / 20.0F;
        int luz = luzDoChao(mc, jogador, chao);
        int argb = CorDaAura.comAlpha(estado.primaryColor(), forca);

        float anterior = raioDe(0, segmentos, raio, fase, tempo);
        for (int i = 0; i < segmentos; i++) {
            float atual = anterior;
            float proximo = raioDe(i + 1, segmentos, raio, fase, tempo);

            float a0 = (float) (2.0 * Math.PI * i / segmentos);
            float a1 = (float) (2.0 * Math.PI * (i + 1) / segmentos);
            float u0 = i / (float) segmentos;
            float u1 = (i + 1) / (float) segmentos;

            // A UV DA U VOLTA INTEIRA UMA VEZ. A textura e tileavel em U, entao
            // repetir a volta N vezes tambem funcionaria -- e daria N copias do
            // mesmo arco, que o olho reconhece como padrao. Uma volta, uma
            // forma.
            emitir(vertices, matriz, a0, atual * (1.0F - FAIXA), u0, 0.0F, argb, luz);
            emitir(vertices, matriz, a0, atual, u0, 1.0F, argb, luz);
            emitir(vertices, matriz, a1, proximo, u1, 1.0F, argb, luz);
            emitir(vertices, matriz, a1, proximo * (1.0F - FAIXA), u1, 0.0F, argb, luz);

            anterior = proximo;
        }
        pilha.popPose();
        MedidorDeVfx.anelDePressao();
    }

    /**
     * O raio de um segmento, ja irregular.
     *
     * <p>HARMONICOS DE FREQUENCIA INTEIRA, e nao ruido por segmento. O anel e
     * fechado: um valor sorteado por segmento deixaria um degrau entre o ultimo
     * e o primeiro -- uma COSTURA, que o olho encontra imediatamente porque e a
     * unica descontinuidade da forma. Com harmonicos inteiros a volta fecha
     * sozinha.
     *
     * <p>E O SEGMENTO {@code segmentos} DEVOLVE O MESMO QUE O SEGMENTO ZERO,
     * pela mesma razao -- e por isso o laco pede o raio do proximo em vez de
     * reaproveitar o primeiro.
     */
    private static float raioDe(int segmento, int segmentos, float raio, float fase, float tempo) {
        float a = (float) (2.0 * Math.PI * segmento / segmentos);
        float lento = (float) Math.sin(3.0F * a + fase + tempo * 0.7F);
        float fino = (float) Math.sin(7.0F * a - fase * 1.7F + tempo * 1.3F);
        float muitoFino = (float) Math.sin(11.0F * a + fase * 0.3F - tempo * 0.4F);
        return raio * (1.0F + 0.14F * lento + 0.07F * fino + 0.035F * muitoFino);
    }

    private void emitir(VertexConsumer vertices, Matrix4f matriz, float angulo, float raio,
            float u, float v, int argb, int luz) {
        this.interno.set((float) (Math.cos(angulo) * raio), 0.0F,
                (float) (Math.sin(angulo) * raio));
        matriz.transformPosition(this.interno);
        // A ORDEM E: cor, u, v, OVERLAY, LUZ, normal -- nessa ordem. Os dois
        // inteiros do meio sao intercambiaveis para o compilador e nao para o
        // buffer; trocados, o anel pisca em vermelho de dano e ignora a luz.
        vertices.addVertex(this.interno.x(), this.interno.y(), this.interno.z(), argb, u, v,
                OverlayTexture.NO_OVERLAY, luz, this.normal.x(), this.normal.y(),
                this.normal.z());
    }

    /**
     * A luz do bloco sob o anel.
     *
     * <p>O ANEL NAO EMITE LUZ, e essa distincao e um criterio de aceite: ele LE
     * a luz do lugar onde esta. Desenhar em fullbright faria o anel brilhar
     * dentro de uma caverna como se iluminasse o chao -- e iluminar o chao e
     * alterar o mundo.
     */
    private static int luzDoChao(Minecraft mc, Player jogador, SondagemDeChao.Amostra chao) {
        BlockPos pos = BlockPos.containing(jogador.getX(), chao.y() + 0.1D, jogador.getZ());
        int bloco = mc.level.getBrightness(LightLayer.BLOCK, pos);
        int ceu = mc.level.getBrightness(LightLayer.SKY, pos);
        return (ceu << 20) | (bloco << 4);
    }
}
