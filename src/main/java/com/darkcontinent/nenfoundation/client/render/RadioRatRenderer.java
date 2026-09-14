package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.RadioRatEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Radio Rat -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.
 * Nao ha timer nem copia de regra de combate deste lado.</p>
 */
public final class RadioRatRenderer extends GeoEntityRenderer<RadioRatEntity> {
    /** Metade da largura da hitbox (0.6F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.3F;

    public RadioRatRenderer(EntityRendererProvider.Context context) {
        super(context, new RadioRatGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
