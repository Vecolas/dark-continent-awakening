package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.FoxbearEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer do Foxbear com geometria e UVs de urso vanilla.
 *
 * <p>A TEXTURA E AUTORAL E CASA COM A UV EMPRESTADA. Antes ela apontava para a
 * imagem do URSO POLAR: os UVs batiam, nada dava erro, e o Foxbear aparecia
 * BRANCO em jogo -- que para quem joga le como "esse bicho nao tem textura".
 *
 * <p>A arte autoral de verdade ({@code foxbear.png}, atlas 1254x1254) NAO serve
 * aqui: ela foi feita para um modelo GeckoLib que ainda nao existe no
 * repositorio, e a camada {@link ModelLayers#POLAR_BEAR} tem UV 128x64. Textura
 * quadrada em UV 2:1 nao da erro -- da um bicho manchado. Por isso existe a
 * variante {@code emprestado.png}, pintada na UV desta camada, com a fonte em
 * {@code art-source/enemies/foxbear/emprestado.py}. Ela MORRE no dia em que o
 * modelo GeckoLib chegar; quem sobrevive e {@code foxbear.png}.
 *
 * <p>TODO: PLACEHOLDER -- a camada do URSO-POLAR vanilla; a textura ja e autoral. Sai quando houver renderer GeoModel proprio, com dimensoes e sons do foxbear.
 * Mob vanilla e andaime: ver ADR-017 e docs/inimigos/mobs-customizados.md.
 */
public final class FoxbearRenderer extends MobRenderer<FoxbearEntity, FoxbearModel> {
    private static final ResourceLocation TEXTURE =
            NenFoundation.id("textures/entity/foxbear/emprestado.png");

    public FoxbearRenderer(EntityRendererProvider.Context context) {
        super(context, new FoxbearModel(context.bakeLayer(ModelLayers.POLAR_BEAR)), 0.7F);
    }

    @Override public ResourceLocation getTextureLocation(FoxbearEntity entity) { return TEXTURE; }
}
