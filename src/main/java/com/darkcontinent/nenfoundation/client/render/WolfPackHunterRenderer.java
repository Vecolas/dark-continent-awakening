package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.WolfPackHunterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Wolf Pack Hunter -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.
 * Nao ha timer nem copia de regra de combate deste lado.</p>
 */
public final class WolfPackHunterRenderer extends GeoEntityRenderer<WolfPackHunterEntity> {
    /** Metade da largura da hitbox (0.9F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.45F;

    public WolfPackHunterRenderer(EntityRendererProvider.Context context) {
        super(context, new WolfPackHunterGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
