package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.FoxbearEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer AUTORAL do foxbear -- o ultimo andaime saiu (ADR-017).
 *
 * <p>Aqui nao ha mais {@code ModelLayers}, nem a textura emprestada. Os dois
 * existiam pelo mesmo motivo: o corpo era a geometria do URSO-POLAR vanilla, cuja
 * camada tem UV 128x64, e a arte autoral quadrada nao cabia nela -- por isso
 * existiu {@code emprestado.png}, uma imagem pintada na UV de outra especie. Ela
 * morre com este arquivo. O modelo proprio traz a propria folha e a propria UV, e
 * textura emprestada que sobrevive a troca de modelo nao da erro: da um bicho
 * manchado que le como erro de arte.</p>
 *
 * <p>SEM {@code withScale()} e sem {@code translate()} de correcao. O geo nasce
 * medindo a hitbox de {@code sized(1.4F, 1.35F)} com as patas em y=0, entao nao ha
 * o que compensar -- e escala ou deslocamento emprestados que sobrevivem a troca de
 * modelo nao aparecem no log, aparecem como um urso flutuando longe da caixa em que
 * ele de fato leva flecha. Num predador TERRITORIAL isso custa caro: a leitura que o
 * jogador faz de "ja estou perto demais" e a silhueta, e o raio de territorio do
 * mob (12 blocos) so ensina alguma coisa se o corpo desenhado estiver onde o
 * servidor acha que ele esta.</p>
 *
 * <p>Quem escolhe o clipe e a entidade, lendo o estado que o servidor publica; este
 * renderer so desenha. Ver {@code FoxbearEntity#registerControllers}.</p>
 */
public final class FoxbearRenderer extends GeoEntityRenderer<FoxbearEntity> {
    /** Metade da largura da hitbox (1.4F): a sombra acompanha o CORPO, nao o modelo. */
    private static final float RAIO_DA_SOMBRA = 0.7F;

    public FoxbearRenderer(EntityRendererProvider.Context context) {
        super(context, new FoxbearGeoModel());
        this.shadowRadius = RAIO_DA_SOMBRA;
    }
}
