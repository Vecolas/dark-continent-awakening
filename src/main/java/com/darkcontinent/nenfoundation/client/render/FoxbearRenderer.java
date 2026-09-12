package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.FoxbearEntity;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renderer vanilla inicial; o modelo autoral entra sem mudar o contrato da entidade. */
public final class FoxbearRenderer extends MobRenderer<FoxbearEntity, CowModel<FoxbearEntity>> {
    private static final ResourceLocation TEXTURE = NenFoundation.id("textures/entity/foxbear/foxbear.png");
    public FoxbearRenderer(EntityRendererProvider.Context context) {
        super(context, new CowModel<>(context.bakeLayer(ModelLayers.COW)), 0.7F);
    }
    @Override public ResourceLocation getTextureLocation(FoxbearEntity entity) { return TEXTURE; }
}
