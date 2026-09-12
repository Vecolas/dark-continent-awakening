package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.ManFacedApeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer AUTORAL do man-faced ape -- o andaime saiu (ADR-017).
 *
 * <p>Aqui nao ha mais {@code ModelLayers}, nem textura vanilla, nem duas raizes
 * bakeadas de camadas diferentes. Os tres existiam pelo mesmo motivo: as duas
 * silhuetas eram emprestadas (aldeao e piglin) e precisavam ser costuradas a
 * mao. Agora quem troca de corpo e o {@link ManFacedApeGeoModel}, trocando
 * modelo, textura e animacao JUNTOS.</p>
 *
 * <p>SEM {@code withScale()}, de proposito. As duas geometrias proprias nascem
 * dentro de {@code sized(0.9F, 1.95F)} -- e nascem na MESMA caixa de proposito:
 * o jogador nao pode perceber a troca pela hitbox, so pelo corpo. Escala de
 * correcao aqui nao daria erro nenhum; daria um bicho cujo alcance de golpe
 * mente sobre a silhueta.</p>
 *
 * <p>Quem escolhe o clipe e a entidade, lendo o disfarce e a fase que o servidor
 * publica; este renderer so desenha. Ver
 * {@code ManFacedApeEntity#registerControllers}.</p>
 */
public final class ManFacedApeRenderer extends GeoEntityRenderer<ManFacedApeEntity> {
    /**
     * Metade da largura da hitbox (0.9F): a sombra acompanha o CORPO, nao o
     * modelo. Ela e a mesma nas duas formas porque a caixa e a mesma -- uma
     * sombra que mudasse de tamanho na revelacao entregaria a troca antes do
     * telegrafo.
     */
    private static final float RAIO_DA_SOMBRA = 0.45F;

    public ManFacedApeRenderer(EntityRendererProvider.Context context) {
        super(context, new ManFacedApeGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
