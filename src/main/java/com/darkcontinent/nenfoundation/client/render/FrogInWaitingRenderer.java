package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.FrogInWaitingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer AUTORAL do frog-in-waiting -- o andaime saiu (ADR-017).
 *
 * <p>Aqui nao ha mais {@code ModelLayers}, nem textura vanilla, nem
 * {@code scale()} de correcao. Os tres existiam pelo mesmo motivo: o corpo era
 * emprestado do sapo vanilla e nao cabia na hitbox. O modelo proprio ja nasce no
 * tamanho de {@code sized(1.4F, 1.0F)}, com o piso das patas em y=0, entao a
 * unica correcao que sobrava deixa de ser necessaria -- e escala emprestada que
 * sobrevive a troca de modelo nao da erro, da um bicho com o tamanho errado.</p>
 *
 * <p>SUMIU TAMBEM O TRUQUE DE ESCONDER PARTES ENQUANTO ENTERRADO. Afundar o sapo
 * na terra agora e o clipe {@code burrowed}, e so ele. Duas maneiras de esconder
 * o mesmo sapo -- uma no renderer, outra na animacao -- divergiriam na primeira
 * correcao, e a divergencia apareceria como meio sapo dentro do chao, sem erro
 * nenhum no log.</p>
 *
 * <p>Quem escolhe o clipe e a entidade, lendo o enterrado e a fase que o servidor
 * publica; este renderer so desenha. Ver
 * {@code FrogInWaitingEntity#registerControllers}.</p>
 */
public final class FrogInWaitingRenderer extends GeoEntityRenderer<FrogInWaitingEntity> {
    /** Metade da largura da hitbox (1.4F): a sombra acompanha o corpo, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.7F;

    public FrogInWaitingRenderer(EntityRendererProvider.Context context) {
        super(context, new FrogInWaitingGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
