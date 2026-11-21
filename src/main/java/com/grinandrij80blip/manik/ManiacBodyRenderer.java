package com.grinandrij80blip.manik;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class ManiacBodyRenderer
        extends LivingEntityRenderer<ManiacBodyEntity, ManiacBodyRenderState, ManiacBodyModel> {
    private static final Identifier TEXTURE = Identifier.parse("manik:textures/entity/maniac_body.png");

    public ManiacBodyRenderer(EntityRendererProvider.Context context) {
        super(context, new ManiacBodyModel(context.bakeLayer(ManiacBodyModel.LAYER)), 0.9F);
    }

    @Override
    public ManiacBodyRenderState createRenderState() {
        return new ManiacBodyRenderState();
    }

    @Override
    public Identifier getTextureLocation(ManiacBodyRenderState state) {
        return TEXTURE;
    }

    @Override
    public void extractRenderState(ManiacBodyEntity entity, ManiacBodyRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.walkPos = Mth.lerp(partialTick, entity.animPosO, entity.animPos);
        state.walkSpeed = Mth.lerp(partialTick, entity.animSpeedO, entity.animSpeed);
        state.attack = entity.getAttackAnim(partialTick);
    }
}
