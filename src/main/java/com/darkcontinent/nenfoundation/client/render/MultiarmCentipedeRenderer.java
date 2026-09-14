package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.MultiarmCentipedeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Multiarm Centipede -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.</p>
 */
public final class MultiarmCentipedeRenderer extends GeoEntityRenderer<MultiarmCentipedeEntity> {
    /** Metade da largura da hitbox (1.6F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.8F;

    public MultiarmCentipedeRenderer(EntityRendererProvider.Context context) {
        super(context, new MultiarmCentipedeGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
