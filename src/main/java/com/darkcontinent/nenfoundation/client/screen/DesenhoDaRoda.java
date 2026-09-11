package com.darkcontinent.nenfoundation.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * Desenha setores de anel. A roda e REDONDA.
 *
 * <p>POR QUE ISTO EXISTE: {@code GuiGraphics} so sabe desenhar retangulo. A
 * primeira versao da roda aproximou cada fatia por um retangulo posicionado no
 * angulo dela -- a selecao era radial de verdade, mas o desenho era um
 * quadrado, e quem abriu em jogo viu um quadrado. Aproximacao declarada
 * continua sendo aproximacao: quem joga ve o desenho, e nao o comentario.
 *
 * <p>Um setor de anel precisa de malha propria, e e isso que este arquivo faz:
 * uma tira de triangulos entre o raio interno e o externo, varrendo o angulo.
 *
 * <p>SEPARADO DA TELA de proposito. Ele nao conhece tecnica, nem cache, nem
 * jogador -- so geometria e cor. Assim a tela cuida de o QUE desenhar, e este
 * arquivo de COMO.
 */
public final class DesenhoDaRoda {

    /**
     * Quantos passos por fatia. Doze e o ponto em que a borda deixa de
     * aparecer serrilhada num monitor comum; mais que isso gasta vertice sem
     * ninguem ver diferenca.
     */
    private static final int PASSOS_POR_FATIA = 12;

    private DesenhoDaRoda() {
    }

    /**
     * Um setor de anel, do angulo {@code de} ao {@code ate}.
     *
     * <p>Os angulos sao em radianos, com ZERO EM CIMA e sentido horario --
     * iguais aos de {@link GeometriaDaRoda}. Usar outra convencao aqui faria o
     * desenho discordar da selecao, e o jogador clicaria numa fatia e ativaria
     * outra.
     */
    public static void setorDeAnel(GuiGraphics g, float cx, float cy,
            float raioInterno, float raioExterno, double de, double ate, int argb) {

        Matrix4f matriz = g.pose().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance()
                .begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int passo = 0; passo <= PASSOS_POR_FATIA; passo++) {
            double angulo = de + (ate - de) * passo / PASSOS_POR_FATIA;
            float sin = (float) Math.sin(angulo);
            float cos = (float) Math.cos(angulo);

            // Y CRESCE PARA BAIXO na tela, por isso o cosseno entra negativo.
            // Com o sinal trocado, a roda sai espelhada na vertical -- e o topo
            // vira o fundo sem nenhum erro em lugar nenhum.
            buffer.addVertex(matriz, cx + sin * raioInterno, cy - cos * raioInterno, 0.0F)
                    .setColor(argb);
            buffer.addVertex(matriz, cx + sin * raioExterno, cy - cos * raioExterno, 0.0F)
                    .setColor(argb);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    /** Um anel inteiro. Para o fundo da roda. */
    public static void anel(GuiGraphics g, float cx, float cy,
            float raioInterno, float raioExterno, int argb) {
        setorDeAnel(g, cx, cy, raioInterno, raioExterno, 0.0D, 2.0D * Math.PI, argb);
    }

    /** Um disco cheio. Para o miolo. */
    public static void disco(GuiGraphics g, float cx, float cy, float raio, int argb) {
        setorDeAnel(g, cx, cy, 0.0F, raio, 0.0D, 2.0D * Math.PI, argb);
    }
}
