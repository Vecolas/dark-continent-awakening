package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.KirikoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer AUTORAL do kiriko -- nasce sem andaime (ADR-017).
 *
 * <p>Sem {@code ModelLayers}, sem textura vanilla e sem translate emprestado de
 * outra especie. As duas geometrias sao proprias e nascem com a raiz no piso da
 * hitbox, entao nao existe o {@code -1.501} do {@code LivingEntityRenderer} para
 * compensar. Compensacao copiada de um mob que emprestava corpo nao da erro
 * nenhum -- da um bicho flutuando longe da caixa em que ele de fato e acertado.</p>
 *
 * <p>SEM {@code withScale()}, e neste mob isso e mais que economia. As duas
 * formas nascem dentro da MESMA {@code sized(1.0F, 2.1F)} DE PROPOSITO: o
 * disfarce so vale enquanto o corpo nao o entrega antes da cena. Uma escala
 * aplicada aqui nao daria erro nenhum -- daria uma forma maior que a outra na
 * tela, e o jogador aprenderia a reconhecer o kiriko pelo tamanho em vez de
 * pelo comportamento, que e justamente o que o encontro existe para testar.</p>
 *
 * <p>Quem troca de corpo e o {@link KirikoGeoModel}, trocando modelo, textura e
 * animacao JUNTOS; quem escolhe o clipe e a entidade, lendo o disfarce, a
 * transformacao, o julgamento e a fase que o servidor publica. Este renderer so
 * desenha. Ver {@code KirikoEntity#registerControllers}.</p>
 */
public final class KirikoRenderer extends GeoEntityRenderer<KirikoEntity> {
    /**
     * Metade da largura da hitbox (1.0F): a sombra acompanha o CORPO, nao o
     * modelo.
     *
     * <p>Ela e a mesma nas duas formas porque a caixa e a mesma. Uma sombra que
     * crescesse na revelacao entregaria a troca antes do clipe de transformacao
     * -- e, pior, entregaria o kiriko disfarcado a quem estivesse olhando para o
     * chao, sem que nada no codigo parecesse errado.</p>
     */
    private static final float RAIO_DA_SOMBRA = 0.5F;

    public KirikoRenderer(EntityRendererProvider.Context context) {
        super(context, new KirikoGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
