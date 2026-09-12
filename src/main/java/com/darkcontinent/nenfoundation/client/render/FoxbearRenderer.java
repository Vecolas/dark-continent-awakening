package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.FoxbearEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renderer do Foxbear com geometria e UVs de urso vanilla. */
public final class FoxbearRenderer extends MobRenderer<FoxbearEntity, FoxbearModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/bear/polarbear.png");

    public FoxbearRenderer(EntityRendererProvider.Context context) {
        super(context, new FoxbearModel(context.bakeLayer(ModelLayers.POLAR_BEAR)), 0.7F);
    }

    @Override public ResourceLocation getTextureLocation(FoxbearEntity entity) { return TEXTURE; }
}
