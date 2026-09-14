package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.AvianCommanderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Avian Commander -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.</p>
 */
public final class AvianCommanderRenderer extends GeoEntityRenderer<AvianCommanderEntity> {
    /** Metade da largura da hitbox (1.4F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.7F;

    public AvianCommanderRenderer(EntityRendererProvider.Context context) {
        super(context, new AvianCommanderGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
