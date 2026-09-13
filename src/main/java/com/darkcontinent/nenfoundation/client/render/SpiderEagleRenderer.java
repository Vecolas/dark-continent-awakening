package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.SpiderEagleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer AUTORAL da spider eagle -- o andaime saiu (ADR-017).
 *
 * <p>Aqui nao ha mais {@code ModelLayers}, nem textura vanilla, nem a colagem
 * vertical que a versao anterior precisava. Os tres existiam pelo mesmo motivo:
 * o corpo era emprestado do phantom, cuja raiz bakeada fica na altura do tronco,
 * e sem compensar o {@code -1.501} do {@code LivingEntityRenderer} a ave ficava
 * pendurada um bloco e meio acima da propria hitbox. O modelo proprio nasce com
 * o piso em y=0, entao a correcao deixa de ser necessaria -- e translate
 * emprestado que sobrevive a troca de modelo nao da erro, da um bicho flutuando
 * longe da caixa em que ele de fato leva flecha.</p>
 *
 * <p>SEM {@code withScale()}, e isso e a decisao central deste arquivo. A
 * dimensao dominante da ave e a ENVERGADURA, e ela foi o ponto que o emprestimo
 * do phantom errava: ~2,7 blocos de asa sobre uma hitbox {@code sized(1.2F,
 * 0.9F)}. Num mob cuja unica resposta ensinada e RECUAR, a silhueta E a regua que
 * o jogador usa para julgar distancia -- uma asa que passa do dobro da caixa faz
 * ele recuar da coisa errada. O geo proprio ja nasce com a asa DOBRADA dentro de
 * ~1,6x a largura da hitbox, e quem a abre e o clipe de aviso. Multiplicar
 * qualquer escala aqui devolveria a mentira pela porta dos fundos, sem erro
 * nenhum no log.</p>
 *
 * <p>Quem escolhe o clipe e a entidade, lendo o aviso e a fase que o servidor
 * publica; este renderer so desenha. Ver
 * {@code SpiderEagleEntity#registerControllers}.</p>
 */
public final class SpiderEagleRenderer extends GeoEntityRenderer<SpiderEagleEntity> {
    /**
     * Metade da largura da hitbox (1.2F): a sombra acompanha o CORPO, nao a
     * envergadura. Uma sombra do tamanho da asa aberta diria ao jogador que o
     * alcance da ave e o dobro do que e -- o mesmo engano da silhueta, desenhado
     * no chao.
     */
    private static final float RAIO_DA_SOMBRA = 0.6F;

    public SpiderEagleRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderEagleGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
