package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.SpiderWebberEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Spider Webber -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.</p>
 */
public final class SpiderWebberRenderer extends GeoEntityRenderer<SpiderWebberEntity> {
    /** Metade da largura da hitbox (1.2F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.6F;

    public SpiderWebberRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderWebberGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
