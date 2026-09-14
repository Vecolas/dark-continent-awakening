package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.KingWhiteStagBeetleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do King White Stag Beetle -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.
 * Nao ha timer nem copia de regra de combate deste lado.</p>
 */
public final class KingWhiteStagBeetleRenderer extends GeoEntityRenderer<KingWhiteStagBeetleEntity> {
    /** Metade da largura da hitbox (1.6F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.8F;

    public KingWhiteStagBeetleRenderer(EntityRendererProvider.Context context) {
        super(context, new KingWhiteStagBeetleGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
