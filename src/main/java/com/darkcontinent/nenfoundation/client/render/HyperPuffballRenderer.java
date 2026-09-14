package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.HyperPuffballEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Hyper Puffball -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.
 * Nao ha timer nem copia de regra de combate deste lado.</p>
 */
public final class HyperPuffballRenderer extends GeoEntityRenderer<HyperPuffballEntity> {
    /** Metade da largura da hitbox (1.2F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.6F;

    public HyperPuffballRenderer(EntityRendererProvider.Context context) {
        super(context, new HyperPuffballGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
