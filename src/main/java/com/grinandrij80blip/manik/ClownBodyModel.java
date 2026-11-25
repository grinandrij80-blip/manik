package com.grinandrij80blip.manik;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Clown maniac model: very tall, thin and hunched, with bloody stripes, a
 * wide grin, red nose, long arms and a circus mallet (with an eye) in the
 * right hand. Cubes and texture (clown_body.png) are generated together.
 */
public final class ClownBodyModel extends EntityModel<ManiacBodyRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.parse("manik:clown_body"), "main");

    private static final float DEG = 0.017453292F;

    private final ModelPart torso;
    private final ModelPart chest;
    private final ModelPart head;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart foreR;
    private final ModelPart foreL;
    private final ModelPart legR;
    private final ModelPart legL;

    public ClownBodyModel(ModelPart root) {
        super(root);
        this.torso = root.getChild("torso");
        this.chest = this.torso.getChild("chest");
        this.head = this.chest.getChild("head");
        this.armR = this.chest.getChild("arm_r");
        this.armL = this.chest.getChild("arm_l");
        this.foreR = this.armR.getChild("fore_r");
        this.foreL = this.armL.getChild("fore_l");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("leg_r",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 24.0F, 3.0F)
                        .texOffs(0, 27).addBox(-2.5F, 24.0F, -6.0F, 5.0F, 3.0F, 9.0F),
                PartPose.offset(-2.5F, -3.0F, 0.0F));
        root.addOrReplaceChild("leg_l",
                CubeListBuilder.create()
                        .texOffs(12, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 24.0F, 3.0F)
                        .texOffs(28, 27).addBox(-2.5F, 24.0F, -6.0F, 5.0F, 3.0F, 9.0F),
                PartPose.offset(2.5F, -3.0F, 0.0F));
        PartDefinition torso = root.addOrReplaceChild("torso",
                CubeListBuilder.create()
                        .texOffs(88, 27).addBox(-3.0F, -5.0F, -2.0F, 6.0F, 5.0F, 4.0F)
,
                PartPose.offset(0.0F, -3.0F, 0.0F));
        PartDefinition chest = torso.addOrReplaceChild("chest",
                CubeListBuilder.create()
                        .texOffs(24, 0).addBox(-3.5F, -13.0F, -2.0F, 7.0F, 13.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.55F, 0.0F, 0.0F));
        chest.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(46, 0).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 9.0F, 8.0F)
                        .texOffs(86, 39).addBox(-1.0F, -4.0F, -6.0F, 2.0F, 2.0F, 2.0F)
                        .texOffs(108, 27).addBox(-7.0F, -7.0F, -2.0F, 3.0F, 4.0F, 4.0F)
                        .texOffs(0, 39).addBox(4.0F, -7.0F, -2.0F, 3.0F, 4.0F, 4.0F)
                        .texOffs(14, 39).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F)
                        .texOffs(30, 39).addBox(-5.0F, -1.0F, -3.0F, 10.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, -0.3F, 0.0F, 0.0F));
        PartDefinition armR = chest.addOrReplaceChild("arm_r",
                CubeListBuilder.create()
                        .texOffs(86, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F),
                PartPose.offsetAndRotation(-5.0F, -11.0F, 0.0F, -0.15F, 0.0F, 0.08F));
        PartDefinition foreR = armR.addOrReplaceChild("fore_r",
                CubeListBuilder.create()
                        .texOffs(94, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F)
                        .texOffs(62, 39).addBox(-1.5F, 11.0F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, 0.0F, -0.2F, 0.0F, 0.0F));
        foreR.addOrReplaceChild("hammer",
                CubeListBuilder.create()
                        .texOffs(78, 0).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 15.0F, 2.0F)
                        .texOffs(56, 27).addBox(-5.0F, 13.0F, -3.0F, 10.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 12.5F, 0.0F, -1.2F, 0.0F, 0.0F));
        PartDefinition armL = chest.addOrReplaceChild("arm_l",
                CubeListBuilder.create()
                        .texOffs(102, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F),
                PartPose.offsetAndRotation(5.0F, -11.0F, 0.0F, -0.15F, 0.0F, -0.08F));
        armL.addOrReplaceChild("fore_l",
                CubeListBuilder.create()
                        .texOffs(110, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F)
                        .texOffs(74, 39).addBox(-1.5F, 11.0F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, 0.0F, -0.2F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(ManiacBodyRenderState state) {
        super.setupAnim(state);

        float crouch = state.crouch;
        float speed = Math.min(1.0F, state.walkSpeed);
        float cycle = state.walkPos * 0.6F;
        float age = state.ageInTicks;
        float t = state.attack;
        float idle = 1.0F - Math.min(1.0F, speed * 4.0F);
        float breathe = Mth.sin(age * 0.06F);
        float stride = 1.0F - 0.45F * crouch;

        // Long loose legs: hips dip each step and sink when crouching.
        float bob = Math.abs(Mth.cos(cycle)) * speed * 1.1F;
        float hipY = -3.0F + bob + crouch * 3.3F;
        this.torso.y = hipY;
        this.legR.y = -3.0F + crouch * 3.3F;
        this.legL.y = -3.0F + crouch * 3.3F;
        // Lanky sway side to side while walking, like a puppet on strings.
        this.torso.zRot = Mth.cos(cycle) * 0.08F * speed;

        float swingLean = ManiacAnim.lean(t, -0.2F, 0.6F);
        this.chest.xRot = 0.55F + breathe * 0.03F + speed * 0.1F + crouch * 0.5F + swingLean;
        this.chest.yRot = t > 0.0F ? Mth.sin(Mth.sqrt(t) * (float) Math.PI * 2.0F) * 0.35F
                : Mth.cos(cycle) * 0.1F * speed;
        this.torso.yRot = this.chest.yRot * -0.3F;

        // Creepy head: tilts, follows the player's gaze, glances around when idle, nods when walking.
        float glance = Mth.sin(age * 0.03F) * 0.2F * idle;
        this.head.xRot = -0.3F - crouch * 0.2F + state.xRot * DEG * 0.6F - Math.max(0.0F, swingLean) * 0.25F
                + Mth.sin(cycle * 2.0F) * 0.06F * speed;
        this.head.yRot = Mth.clamp(state.yRot, -70.0F, 70.0F) * DEG - this.chest.yRot * 0.6F + glance;
        this.head.zRot = Mth.sin(age * 0.05F) * 0.07F + Mth.cos(cycle) * 0.08F * speed;

        float armSwingR = Mth.cos(cycle + (float) Math.PI) * 0.5F * speed * stride;
        float armSwingL = Mth.cos(cycle) * 0.5F * speed * stride;
        float sway = Mth.sin(age * 0.07F) * 0.05F * idle;
        // The mallet arm winds up overhead and slams down; the free arm flails for balance.
        float slamR = ManiacAnim.armAngle(t, -3.0F, -0.8F);
        float slamL = ManiacAnim.armAngle(t, -1.2F, -0.3F);
        this.armR.xRot = -0.15F - crouch * 0.3F + armSwingR + slamR;
        this.armL.xRot = -0.15F - crouch * 0.3F + armSwingL + slamL;
        this.armR.zRot = 0.08F + sway + Math.abs(armSwingR) * 0.08F;
        this.armL.zRot = -0.08F - sway - Math.abs(armSwingL) * 0.08F - Math.abs(slamL) * 0.2F;
        this.foreR.xRot = -0.2F - Math.abs(slamR) * 0.2F - Math.abs(armSwingR) * 0.3F - breathe * 0.04F;
        this.foreL.xRot = -0.2F - Math.abs(slamL) * 0.3F - Math.abs(armSwingL) * 0.3F + breathe * 0.04F;

        float legSwing = 1.0F * speed * stride;
        this.legR.xRot = Mth.cos(cycle) * legSwing - crouch * 0.4F;
        this.legL.xRot = Mth.cos(cycle + (float) Math.PI) * legSwing + crouch * 0.4F;
    }
}
