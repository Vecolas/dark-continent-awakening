package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.CrabHeavyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Crab Heavy -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.</p>
 */
public final class CrabHeavyRenderer extends GeoEntityRenderer<CrabHeavyEntity> {
    /** Metade da largura da hitbox (1.4F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.7F;

    public CrabHeavyRenderer(EntityRendererProvider.Context context) {
        super(context, new CrabHeavyGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
