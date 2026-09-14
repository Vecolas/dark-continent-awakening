package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.MosquitoOfficerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Mosquito Officer -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.</p>
 */
public final class MosquitoOfficerRenderer extends GeoEntityRenderer<MosquitoOfficerEntity> {
    /** Metade da largura da hitbox (0.9F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.45F;

    public MosquitoOfficerRenderer(EntityRendererProvider.Context context) {
        super(context, new MosquitoOfficerGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
