package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.ScorpionLeaderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Scorpion Leader -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.</p>
 */
public final class ScorpionLeaderRenderer extends GeoEntityRenderer<ScorpionLeaderEntity> {
    /** Metade da largura da hitbox (1.5F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.75F;

    public ScorpionLeaderRenderer(EntityRendererProvider.Context context) {
        super(context, new ScorpionLeaderGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
