package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.CheetahLeaderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Cheetah Leader -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.</p>
 */
public final class CheetahLeaderRenderer extends GeoEntityRenderer<CheetahLeaderEntity> {
    /** Metade da largura da hitbox (1.2F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.6F;

    public CheetahLeaderRenderer(EntityRendererProvider.Context context) {
        super(context, new CheetahLeaderGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
