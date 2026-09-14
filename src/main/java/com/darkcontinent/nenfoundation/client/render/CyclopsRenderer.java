package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.CyclopsEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Cyclops -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.
 * Nao ha timer nem copia de regra de combate deste lado.</p>
 */
public final class CyclopsRenderer extends GeoEntityRenderer<CyclopsEntity> {
    /** Metade da largura da hitbox (1.8F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.9F;

    public CyclopsRenderer(EntityRendererProvider.Context context) {
        super(context, new CyclopsGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
