package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.BubbleHorseEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Bubble Horse -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.
 * Nao ha timer nem copia de regra de combate deste lado.</p>
 */
public final class BubbleHorseRenderer extends GeoEntityRenderer<BubbleHorseEntity> {
    /** Metade da largura da hitbox (1.2F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.6F;

    public BubbleHorseRenderer(EntityRendererProvider.Context context) {
        super(context, new BubbleHorseGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
