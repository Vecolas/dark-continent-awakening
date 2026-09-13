package com.darkcontinent.nenfoundation.client.vfx.ribbon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Transforma uma curva numa tira de quads e a escreve no buffer.
 *
 * <p>UMA INSTANCIA POR LAYER, reaproveitada entre quadros. Todos os vetores de
 * trabalho vivem aqui como campo: vinte e oito ribbons vezes nove nos vezes
 * sessenta quadros por segundo e lixo demais para um efeito cosmetico.
 *
 * <p><b>DUAS ARMADILHAS QUE COMPILAM E DESENHAM ERRADO</b>, e as duas foram
 * conferidas na fonte, nao lembradas:
 *
 * <ol>
 *   <li>Na sobrecarga barata de {@code addVertex}, <b>{@code packedOverlay} vem
 *       ANTES de {@code packedLight}</b>. Os dois sao {@code int}. Troca-los
 *       compila, nao avisa, e produz um filamento que pisca em vermelho de dano
 *       e ignora a luz do mundo.</li>
 *   <li><b>{@code +Y} aponta para BAIXO</b> nesta pilha, por causa do
 *       {@code scale(-1,-1,1)} de {@code LivingEntityRenderer}. Quem sobe,
 *       subtrai. Com o sinal trocado a ribbon entra no chao, e o chao nao
 *       reclama.</li>
 * </ol>
 *
 * <p>A TIRA OLHA PARA A CAMERA, e isso sai de graca: a origem deste espaco E a
 * camera, entao a direcao de visao de um ponto e o proprio ponto normalizado.
 * Nao e preciso consultar {@code Camera} nem inverter matriz.
 */
public final class AuraRibbonBatch {

    /** O maior numero de nos que uma curva pode ter. */
    private static final int NOS_MAXIMOS = 9;

    private final float[] curva = new float[NOS_MAXIMOS * 3];
    private final Vector3f[] mundo = criarVetores(NOS_MAXIMOS);
    private final Vector3f direcao = new Vector3f();
    private final Vector3f visao = new Vector3f();
    private final Vector3f lado = new Vector3f();
    private final Vector3f normal = new Vector3f();
    private final Vector3f esquerda = new Vector3f();
    private final Vector3f direita = new Vector3f();

    /**
     * Desenha um filamento.
     *
     * <p>Devolve {@code false} sem escrever nada quando a curva degenera -- dois
     * nos no mesmo lugar, ou um segmento alinhado com a visao. Degenerescencia
     * produz normal zero, e normal zero vira {@code NaN} na normalizacao: um
     * quad com {@code NaN} nao lanca, ele some, e as vezes leva o lote junto.
     *
     * @param pose      transformacao da PARTE em que a ancora nasce
     * @param largura   largura da tira, em BLOCOS
     * @param argb      cor ja com o alpha aplicado
     */
    public boolean desenhar(VertexConsumer buffer, PoseStack.Pose pose, AuraAnchor ancora,
            boolean slim, long semente, float folgaBase, float comprimento, float largura,
            int argb, int luz) {

        int n = AuraCurve.pontos(this.curva, ancora, slim, semente, folgaBase, comprimento);
        if (n < 2 || largura <= 0.0F) {
            return false;
        }

        Matrix4f matriz = pose.pose();
        for (int i = 0; i < n; i++) {
            // A CURVA ESTA EM UNIDADE DE MODELO e a pilha esta em BLOCO: a
            // divisao por 16 acontece aqui, uma vez so. O comprimento do perfil
            // ja vem em blocos e por isso NAO passa por ela.
            this.mundo[i].set(
                    this.curva[i * 3] / AuraCurve.UNIDADES_POR_BLOCO,
                    this.curva[i * 3 + 1] / AuraCurve.UNIDADES_POR_BLOCO,
                    this.curva[i * 3 + 2] / AuraCurve.UNIDADES_POR_BLOCO);
            matriz.transformPosition(this.mundo[i]);
        }

        float meia = largura * 0.5F;
        boolean escreveu = false;

        for (int i = 0; i < n - 1; i++) {
            Vector3f a = this.mundo[i];
            Vector3f b = this.mundo[i + 1];

            this.direcao.set(b).sub(a);
            if (this.direcao.lengthSquared() < 1.0e-9F) {
                continue;
            }
            this.direcao.normalize();

            // A CAMERA ESTA NA ORIGEM deste espaco, entao a direcao de visao de
            // um ponto e o proprio ponto.
            this.visao.set(a);
            if (this.visao.lengthSquared() < 1.0e-9F) {
                continue;
            }
            this.visao.normalize();

            this.lado.set(this.direcao).cross(this.visao);
            if (this.lado.lengthSquared() < 1.0e-9F) {
                // Segmento alinhado com a visao: a tira teria largura zero na
                // tela. Pular e melhor que desenhar uma linha degenerada.
                continue;
            }
            this.lado.normalize().mul(meia * afinamento(i, n));

            this.normal.set(this.lado).cross(this.direcao);
            if (this.normal.lengthSquared() < 1.0e-9F) {
                continue;
            }
            this.normal.normalize();

            float v0 = i / (float) (n - 1);
            float v1 = (i + 1) / (float) (n - 1);

            this.esquerda.set(a).sub(this.lado);
            this.direita.set(a).add(this.lado);
            vertice(buffer, this.esquerda, argb, 0.0F, v0, luz, this.normal);
            vertice(buffer, this.direita, argb, 1.0F, v0, luz, this.normal);

            this.lado.normalize().mul(meia * afinamento(i + 1, n));
            this.esquerda.set(b).sub(this.lado);
            this.direita.set(b).add(this.lado);
            vertice(buffer, this.direita, argb, 1.0F, v1, luz, this.normal);
            vertice(buffer, this.esquerda, argb, 0.0F, v1, luz, this.normal);

            escreveu = true;
        }
        return escreveu;
    }

    /**
     * A tira afina nas duas pontas.
     *
     * <p>Sem isto o filamento termina num corte reto, que o olho le como fita
     * cortada com tesoura -- e nao como energia que se dissipa.
     */
    private static float afinamento(int no, int total) {
        float s = no / (float) (total - 1);
        return (float) Math.sin(Math.PI * Math.min(1.0F, Math.max(0.0F, s)));
    }

    private static void vertice(VertexConsumer buffer, Vector3f p, int argb, float u, float v,
            int luz, Vector3f normal) {
        // A POSICAO JA VEM TRANSFORMADA, entao usamos a sobrecarga de onze
        // argumentos: ela e o caminho rapido do BufferBuilder para NEW_ENTITY e
        // e a unica que nao aloca um Vector3f por vertice.
        //
        // A ORDEM E: cor, u, v, OVERLAY, LUZ, normal. Nessa ordem.
        buffer.addVertex(p.x(), p.y(), p.z(), argb, u, v,
                OverlayTexture.NO_OVERLAY, luz, normal.x(), normal.y(), normal.z());
    }

    private static Vector3f[] criarVetores(int quantos) {
        Vector3f[] vetores = new Vector3f[quantos];
        for (int i = 0; i < quantos; i++) {
            vetores[i] = new Vector3f();
        }
        return vetores;
    }
}
