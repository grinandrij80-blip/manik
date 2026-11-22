package com.grinandrij80blip.manik;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class HelicopterRenderer
        extends LivingEntityRenderer<HelicopterEntity, LivingEntityRenderState, HelicopterModel> {
    private static final Identifier TEXTURE = Identifier.parse("manik:textures/entity/helicopter.png");

    public HelicopterRenderer(EntityRendererProvider.Context context) {
        super(context, new HelicopterModel(context.bakeLayer(HelicopterModel.LAYER)), 1.5F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(HelicopterEntity entity, double distanceToCameraSq) {
        return false;
    }
}
