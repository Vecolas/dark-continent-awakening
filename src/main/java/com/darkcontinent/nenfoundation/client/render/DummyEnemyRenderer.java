package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.DummyEnemyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer do Boneco de Treino -- ele so desenha.
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase e o cambaleio que o
 * servidor publica. Nao ha timer, heuristica nem copia de regra de combate deste
 * lado: se a animacao e a hitbox discordarem, quem esta errado e o arquivo de
 * animacao, nunca o servidor.</p>
 */
public final class DummyEnemyRenderer extends GeoEntityRenderer<DummyEnemyEntity> {
    /** Metade da largura da hitbox (0.8F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.4F;

    public DummyEnemyRenderer(EntityRendererProvider.Context context) {
        super(context, new DummyEnemyGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
