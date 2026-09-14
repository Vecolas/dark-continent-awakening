package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.WolfRunnerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Wolf Runner -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.</p>
 */
public final class WolfRunnerRenderer extends GeoEntityRenderer<WolfRunnerEntity> {
    /** Metade da largura da hitbox (1.0F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.5F;

    public WolfRunnerRenderer(EntityRendererProvider.Context context) {
        super(context, new WolfRunnerGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
