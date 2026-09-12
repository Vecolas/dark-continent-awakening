package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.GreatStampEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer AUTORAL do great stamp -- o andaime saiu (ADR-017).
 *
 * <p>Aqui nao ha mais {@code ModelLayers}, nem textura vanilla, nem
 * {@code scale()} de correcao. Os tres existiam pelo mesmo motivo: o corpo era
 * emprestado e nao cabia na hitbox. O modelo proprio ja nasce no tamanho de
 * {@code sized(1.9F, 1.55F)}, com o piso dos cascos em y=0, entao a unica
 * correcao que sobrava deixa de ser necessaria -- e escala emprestada que
 * sobrevive a troca de modelo nao da erro, da um bicho com o tamanho errado.</p>
 *
 * <p>Quem escolhe o clipe e a entidade, lendo a fase que o servidor publica; este
 * renderer so desenha. Ver {@code GreatStampEntity#registerControllers}.</p>
 */
public final class GreatStampRenderer extends GeoEntityRenderer<GreatStampEntity> {
    /** Metade da largura da hitbox (1.9F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.95F;

    public GreatStampRenderer(EntityRendererProvider.Context context) {
        super(context, new GreatStampGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
