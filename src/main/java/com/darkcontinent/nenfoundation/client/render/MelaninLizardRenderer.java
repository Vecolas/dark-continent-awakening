package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.MelaninLizardEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Melanin Lizard -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica.
 * Nao ha timer nem copia de regra de combate deste lado.</p>
 */
public final class MelaninLizardRenderer extends GeoEntityRenderer<MelaninLizardEntity> {
    /** Metade da largura da hitbox (1.6F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.8F;

    public MelaninLizardRenderer(EntityRendererProvider.Context context) {
        super(context, new MelaninLizardGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
