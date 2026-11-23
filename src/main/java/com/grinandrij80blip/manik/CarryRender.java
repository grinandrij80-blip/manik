package com.grinandrij80blip.manik;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderAvatarEvent;
import net.minecraftforge.client.event.RenderHandEvent;

/**
 * Draws the carried flare crate in 3D.
 * First person: the vanilla hands are replaced by both of the player's own arms (with their skin)
 * gripping the crate, which sways heavily and bobs while walking.
 * Third person: whoever carries the crate has both arms stretched forward and the crate in front of them.
 */
public final class CarryRender {
    private static ItemStack crateStack;

    public static void register() {
        RenderHandEvent.BUS.addListener((java.util.function.Predicate<RenderHandEvent>) CarryRender::onRenderHand);
        RenderAvatarEvent.Pre.BUS.addListener((java.util.function.Predicate<RenderAvatarEvent.Pre>) CarryRender::onAvatarPre);
        RenderAvatarEvent.Post.BUS.addListener(CarryRender::onAvatarPost);
    }

    private static ItemStack stack() {
        if (crateStack == null) crateStack = new ItemStack(ManikBlocks.FLARE_CRATE_ITEM.get());
        return crateStack;
    }

    private static void submitCrate(PoseStack poseStack, SubmitNodeCollector collector, int light) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemStackRenderState renderState = new ItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(
                renderState, stack(), ItemDisplayContext.FIXED, minecraft.level, null, 0);
        renderState.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
    }

    // ---- first person --------------------------------------------------------------

    private static boolean onRenderHand(RenderHandEvent event) {
        if (!CarryState.isCarrying()) return false;
        if (event.getHand() == InteractionHand.OFF_HAND) return true; // the main hand draws everything

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return false;

        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getNodeCollector();
        int light = event.getPackedLight();

        double time = System.currentTimeMillis() / 50.0;
        float moving = Math.min(1.0F, player.walkAnimation.speed());
        // Heavy load: a slow sway all the time, harder bobbing when walking.
        float swayX = (float) Math.sin(time * 0.07) * 0.012F + (float) Math.sin(time * 0.3) * 0.02F * moving;
        float bob = 0.04F + (float) Math.sin(time * 0.1) * 0.01F
                + Math.abs((float) Math.sin(time * 0.3)) * 0.05F * moving;

        // The crate, held in front of the chest.
        poseStack.pushPose();
        poseStack.translate(swayX, -0.62F - bob, -1.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F));
        poseStack.scale(0.85F, 0.85F, 0.85F);
        submitCrate(poseStack, collector, light);
        poseStack.popPose();

        // Both arms with the player's own skin.
        var renderer = minecraft.getEntityRenderDispatcher().getPlayerRenderer(player);
        var skin = player.getSkin().body().texturePath();
        for (int side = 1; side >= -1; side -= 2) {
            float f = side;
            poseStack.pushPose();
            poseStack.translate(f * 0.64000005F + swayX, -0.6F - bob, -0.71999997F);
            poseStack.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
            poseStack.translate(f * -1.0F, 3.6F, 3.5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
            poseStack.translate(f * 5.6F, 0.0F, 0.0F);
            if (side > 0) {
                renderer.renderRightHand(poseStack, collector, light, skin, true);
            } else {
                renderer.renderLeftHand(poseStack, collector, light, skin, true);
            }
            poseStack.popPose();
        }
        return true;
    }

    // ---- third person ----------------------------------------------------------------

    private static boolean isCarrierState(net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || CarryState.carrierId() < 0) return false;
        Entity carrier = minecraft.level.getEntity(CarryState.carrierId());
        if (carrier == null) return false;
        double dx = state.x - carrier.getX();
        double dy = state.y - carrier.getY();
        double dz = state.z - carrier.getZ();
        return dx * dx + dy * dy + dz * dz < 0.6 * 0.6;
    }

    private static boolean onAvatarPre(RenderAvatarEvent.Pre event) {
        if (isCarrierState(event.getState())) {
            event.getState().rightArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
            event.getState().leftArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        return false;
    }

    private static void onAvatarPost(RenderAvatarEvent.Post event) {
        var state = event.getState();
        if (!isCarrierState(state)) return;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.bodyRot));
        poseStack.translate(0.0F, 1.0F, 0.62F);
        poseStack.scale(0.8F, 0.8F, 0.8F);
        submitCrate(poseStack, event.getNodeCollector(), state.lightCoords);
        poseStack.popPose();
    }

    private CarryRender() {}
}
