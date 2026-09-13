package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.MasterOfTheSwampEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer AUTORAL do master of the swamp -- nasce sem andaime (ADR-017).
 *
 * <p>Nao ha {@code ModelLayers} aqui, nem textura vanilla, nem translate
 * emprestado. O mob nunca vestiu corpo de outra especie: o modelo proprio nasce
 * com a raiz no piso da hitbox, entao nao existe o {@code -1.501} do
 * {@code LivingEntityRenderer} para compensar. Compensacao copiada de um mob que
 * emprestava corpo nao da erro nenhum -- da um bicho flutuando longe da caixa em
 * que ele de fato leva o anzol.</p>
 *
 * <p>SEM {@code withScale()}, e isso e a decisao central deste arquivo. Este mob
 * e uma PROMESSA DE TAMANHO: a ficha pede "peixe enorme", a hitbox e
 * {@code sized(2.4F, 1.6F)} e a silhueta e a unica regua que o jogador tem para
 * decidir se aquele vulto na agua vale a vara. Multiplicar escala aqui faria o
 * peixe parecer maior do que a caixa que o anzol precisa acertar, e o relato
 * seria "a linha nao fisga, mesmo mirando nele" -- sem uma linha no log.</p>
 *
 * <p>Quem escolhe o clipe e a entidade, lendo fisgado, cansado e a fase que o
 * servidor publica; este renderer so desenha. Ver
 * {@code MasterOfTheSwampEntity#registerControllers}.</p>
 */
public final class MasterOfTheSwampRenderer extends GeoEntityRenderer<MasterOfTheSwampEntity> {
    /**
     * Metade da largura da hitbox (2.4F). A sombra acompanha o CORPO, e num mob
     * aquatico ela e o que denuncia o vulto submerso antes de a silhueta ficar
     * legivel: encolhe-la faria o peixe enorme se anunciar como um peixe comum,
     * e o jogador escolheria a isca errada.
     */
    private static final float RAIO_DA_SOMBRA = 1.2F;

    public MasterOfTheSwampRenderer(EntityRendererProvider.Context context) {
        super(context, new MasterOfTheSwampGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
