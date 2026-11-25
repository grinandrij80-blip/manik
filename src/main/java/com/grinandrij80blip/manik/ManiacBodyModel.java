package com.grinandrij80blip.manik;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Custom MANIK maniac model: a tall hunched figure with a hood, two horns, a
 * pale mask and long clawed arms. The geometry and texture layout are generated
 * together, so every cube's texOffs matches maniac_body.png.
 *
 * Height is about 2.9 blocks, matching the maniac's 1.6x scaled hitbox.
 */
public final class ManiacBodyModel extends EntityModel<ManiacBodyRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.parse("manik:maniac_body"), "main");

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

    public ManiacBodyModel(ModelPart root) {
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
        PartDefinition legR = root.addOrReplaceChild("leg_r",
                CubeListBuilder.create()
                        .texOffs(40, 18).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F)
                        .texOffs(0, 46).addBox(-3.0F, 10.0F, -4.0F, 6.0F, 3.0F, 7.0F),
                PartPose.offset(-3.0F, 11.0F, 0.0F));
        PartDefinition legL = root.addOrReplaceChild("leg_l",
                CubeListBuilder.create()
                        .texOffs(56, 18).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F)
                        .texOffs(26, 46).addBox(-3.0F, 10.0F, -4.0F, 6.0F, 3.0F, 7.0F),
                PartPose.offset(3.0F, 11.0F, 0.0F));
        PartDefinition torso = root.addOrReplaceChild("torso",
                CubeListBuilder.create()
                        .texOffs(36, 33).addBox(-4.0F, -6.0F, -3.0F, 8.0F, 6.0F, 6.0F)
                        .texOffs(52, 46).addBox(-5.0F, -3.0F, -4.0F, 10.0F, 2.0F, 8.0F)
                        .texOffs(0, 0).addBox(-4.0F, -6.0F, 3.0F, 8.0F, 17.0F, 1.0F)
                        .texOffs(72, 18).addBox(-7.0F, -6.0F, 3.0F, 3.0F, 13.0F, 1.0F)
                        .texOffs(80, 18).addBox(4.0F, -6.0F, 3.0F, 3.0F, 13.0F, 1.0F),
                PartPose.offset(0.0F, 11.0F, 0.0F));
        PartDefinition chest = torso.addOrReplaceChild("chest",
                CubeListBuilder.create()
                        .texOffs(0, 18).addBox(-6.0F, -7.0F, -4.0F, 12.0F, 7.0F, 8.0F)
                        .texOffs(12, 56).addBox(-4.0F, -6.0F, 4.0F, 8.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.3F, 0.0F, 0.0F));
        PartDefinition head = chest.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(18, 0).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 9.0F, 8.0F)
                        .texOffs(64, 33).addBox(-5.0F, -11.0F, -5.0F, 10.0F, 2.0F, 10.0F)
                        .texOffs(50, 0).addBox(-5.0F, -9.0F, -5.0F, 1.0F, 8.0F, 9.0F)
                        .texOffs(70, 0).addBox(4.0F, -9.0F, -5.0F, 1.0F, 8.0F, 9.0F)
                        .texOffs(88, 46).addBox(-4.0F, -9.0F, 4.0F, 8.0F, 8.0F, 1.0F)
                        .texOffs(106, 46).addBox(1.0F, -17.0F, -2.0F, 3.0F, 6.0F, 3.0F)
                        .texOffs(50, 56).addBox(2.0F, -21.0F, -1.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(0, 56).addBox(-4.0F, -17.0F, -2.0F, 3.0F, 6.0F, 3.0F)
                        .texOffs(54, 56).addBox(-3.0F, -21.0F, -1.0F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -7.0F, -1.0F, -0.2F, 0.0F, 0.0F));
        PartDefinition armR = chest.addOrReplaceChild("arm_r",
                CubeListBuilder.create()
                        .texOffs(88, 18).addBox(-3.0F, -2.0F, -2.0F, 5.0F, 10.0F, 4.0F)
                        .texOffs(34, 56).addBox(-2.0F, -6.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(-8.0F, -5.0F, 0.0F, -0.3F, 0.0F, 0.05F));
        PartDefinition foreR = armR.addOrReplaceChild("fore_r",
                CubeListBuilder.create()
                        .texOffs(0, 33).addBox(-3.0F, 0.0F, -2.0F, 5.0F, 9.0F, 4.0F)
                        .texOffs(58, 56).addBox(-3.0F, 9.0F, -2.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(62, 56).addBox(-1.0F, 9.0F, -2.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(66, 56).addBox(1.0F, 9.0F, -2.0F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, -0.25F, 0.0F, 0.0F));
        PartDefinition armL = chest.addOrReplaceChild("arm_l",
                CubeListBuilder.create()
                        .texOffs(106, 18).addBox(-2.0F, -2.0F, -2.0F, 5.0F, 10.0F, 4.0F)
                        .texOffs(42, 56).addBox(0.0F, -6.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(8.0F, -5.0F, 0.0F, -0.3F, 0.0F, -0.05F));
        PartDefinition foreL = armL.addOrReplaceChild("fore_l",
                CubeListBuilder.create()
                        .texOffs(18, 33).addBox(-2.0F, 0.0F, -2.0F, 5.0F, 9.0F, 4.0F)
                        .texOffs(70, 56).addBox(-2.0F, 9.0F, -2.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(74, 56).addBox(0.0F, 9.0F, -2.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(78, 56).addBox(2.0F, 9.0F, -2.0F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, -0.25F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(ManiacBodyRenderState state) {
        super.setupAnim(state);

        float crouch = state.crouch;
        float speed = Math.min(1.0F, state.walkSpeed);
        float cycle = state.walkPos * 0.6662F;
        float age = state.ageInTicks;
        float t = state.attack;
        float idle = 1.0F - Math.min(1.0F, speed * 4.0F);
        float breathe = Mth.sin(age * 0.08F);
        float stride = 1.0F - 0.45F * crouch; // shorter steps while sneaking

        // Hips: dip on every step, sink when crouching.
        float bob = Math.abs(Mth.cos(cycle)) * speed * 0.9F;
        this.torso.y = 11.0F + bob + crouch * 1.8F;
        this.torso.zRot = Mth.cos(cycle) * 0.06F * speed;
        this.legR.y = 11.0F + crouch * 1.8F;
        this.legL.y = 11.0F + crouch * 1.8F;

        // Hunched chest: breathes, leans into a run, drops forward in a crouch, wild on attacks.
        float swingLean = ManiacAnim.lean(t, -0.25F, 0.65F);
        this.chest.xRot = 0.3F + breathe * 0.025F + speed * 0.12F + crouch * 0.55F + swingLean;
        this.chest.yRot = t > 0.0F ? Mth.sin(Mth.sqrt(t) * (float) Math.PI * 2.0F) * 0.4F
                : Mth.cos(cycle) * 0.08F * speed;
        this.torso.yRot = this.chest.yRot * -0.3F;

        // Head: looks where the player looks; idles with slow predatory glances; counter-bobs when moving.
        float glance = Mth.sin(age * 0.035F) * 0.18F * idle;
        this.head.xRot = -0.2F - crouch * 0.25F + state.xRot * DEG * 0.6F - Math.max(0.0F, swingLean) * 0.3F
                + Mth.sin(cycle * 2.0F) * 0.04F * speed;
        this.head.yRot = Mth.clamp(state.yRot, -70.0F, 70.0F) * DEG - this.chest.yRot * 0.6F + glance;
        this.head.zRot = Mth.sin(age * 0.05F) * 0.04F * idle;

        // Arms: big opposite swing, claws raised overhead on an attack, dangling and twitching when idle.
        float armSwingR = Mth.cos(cycle + (float) Math.PI) * 0.9F * speed * stride;
        float armSwingL = Mth.cos(cycle) * 0.9F * speed * stride;
        float sway = Mth.sin(age * 0.09F) * 0.05F * idle;
        float slamR = ManiacAnim.armAngle(t, -2.9F, -0.7F);
        float slamL = ManiacAnim.armAngle(t, -2.6F, -0.5F);
        this.armR.xRot = -0.3F - crouch * 0.4F + armSwingR + slamR;
        this.armL.xRot = -0.3F - crouch * 0.4F + armSwingL + slamL;
        this.armR.zRot = 0.05F + sway + Mth.cos(cycle) * 0.05F * speed - Math.abs(slamR) * 0.05F;
        this.armL.zRot = -0.05F - sway - Mth.cos(cycle) * 0.05F * speed + Math.abs(slamL) * 0.05F;
        this.foreR.xRot = -0.25F - Math.abs(slamR) * 0.25F - Math.abs(armSwingR) * 0.4F - breathe * 0.03F;
        this.foreL.xRot = -0.25F - Math.abs(slamL) * 0.25F - Math.abs(armSwingL) * 0.4F + breathe * 0.03F;

        // Legs: walk cycle; a staggered, bent stance when crouching (hips are lowered above).
        float legSwing = 1.3F * speed * stride;
        this.legR.xRot = Mth.cos(cycle) * legSwing - crouch * 0.5F;
        this.legL.xRot = Mth.cos(cycle + (float) Math.PI) * legSwing + crouch * 0.5F;
    }
}
