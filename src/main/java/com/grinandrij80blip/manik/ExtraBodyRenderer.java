package com.grinandrij80blip.manik;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ExtraBodyRenderer
        extends LivingEntityRenderer<ManiacBodyEntity, ManiacBodyRenderState, EntityModel<ManiacBodyRenderState>> {
    private final Identifier texture;

    public ExtraBodyRenderer(EntityRendererProvider.Context context, EntityModel<ManiacBodyRenderState> model,
                             String textureName, float shadow) {
        super(context, model, shadow);
        this.texture = Identifier.parse("manik:textures/entity/" + textureName + ".png");
    }

    @Override
    public ManiacBodyRenderState createRenderState() {
        return new ManiacBodyRenderState();
    }

    @Override
    public Identifier getTextureLocation(ManiacBodyRenderState state) {
        return texture;
    }

    @Override
    protected boolean shouldShowName(ManiacBodyEntity entity, double distanceToCameraSq) {
        // The body has no name of its own; the player's name tag is hidden with them.
        return false;
    }

    @Override
    public void extractRenderState(ManiacBodyEntity entity, ManiacBodyRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        // Fallback gait when the owner is not known on this client yet.
        state.walkPos = Mth.lerp(partialTick, entity.animPosO, entity.animPos);
        state.walkSpeed = Mth.lerp(partialTick, entity.animSpeedO, entity.animSpeed);
        state.attack = entity.getAttackAnim(partialTick);
        state.hiddenFromOwner = false;
        state.wings = entity.hasWings();
        state.crouch = Mth.lerp(partialTick, entity.crouchO, entity.crouch);

        Entity owner = entity.getOwner();
        if (owner instanceof LivingEntity player) {
            // Copy exactly what the game shows for the player this frame, so the
            // model moves, turns, walks and swings together with the maniac.
            state.x = Mth.lerp(partialTick, owner.xOld, owner.getX());
            state.y = Mth.lerp(partialTick, owner.yOld, owner.getY());
            state.z = Mth.lerp(partialTick, owner.zOld, owner.getZ());

            float bodyRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
            float headRot = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
            state.bodyRot = bodyRot;
            state.yRot = Mth.wrapDegrees(headRot - bodyRot);
            state.xRot = Mth.lerp(partialTick, player.xRotO, player.getXRot());

            state.walkPos = player.walkAnimation.position(partialTick);
            state.walkSpeed = player.walkAnimation.speed(partialTick);
            state.attack = player.getAttackAnim(partialTick);

            // From the maniac's own eyes the camera sits inside the model, so hide it.
            Minecraft minecraft = Minecraft.getInstance();
            state.hiddenFromOwner = owner == minecraft.player
                    && minecraft.options.getCameraType().isFirstPerson();
        }
    }

    @Override
    public void submit(ManiacBodyRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.hiddenFromOwner) return;
        super.submit(state, poseStack, collector, cameraState);
    }
}
