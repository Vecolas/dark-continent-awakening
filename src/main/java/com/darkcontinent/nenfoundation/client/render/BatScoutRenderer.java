package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.BatScoutEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Bat Scout -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.</p>
 */
public final class BatScoutRenderer extends GeoEntityRenderer<BatScoutEntity> {
    /** Metade da largura da hitbox (0.8F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.4F;

    public BatScoutRenderer(EntityRendererProvider.Context context) {
        super(context, new BatScoutGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
