package com.grinandrij80blip.manik;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.PartPose;
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

        float speed = Math.min(1.0F, state.walkSpeed);
        float cycle = state.walkPos * 0.6662F;
        float age = state.ageInTicks;
        float attack = state.attack;
        // 0 -> 1 -> 0 over the course of one swing of the claws.
        float swing = attack > 0.0F ? Mth.sin(attack * (float) Math.PI) : 0.0F;
        float breathe = Mth.sin(age * 0.08F);

        // Hunched torso: breathes when idle, lunges forward and twists on an attack.
        this.chest.xRot = 0.3F + breathe * 0.02F + swing * 0.45F;
        this.chest.yRot = attack > 0.0F ? Mth.sin(Mth.sqrt(attack) * (float) Math.PI * 2.0F) * 0.35F : 0.0F;

        // The head keeps looking where the player looks, on top of the lean.
        this.head.xRot = -0.2F + state.xRot * DEG * 0.6F - swing * 0.25F;
        this.head.yRot = Mth.clamp(state.yRot, -70.0F, 70.0F) * DEG - this.chest.yRot * 0.6F;

        // Walking: opposite arm and leg swing together; claws rake forward when attacking.
        float armSwingR = Mth.cos(cycle + (float) Math.PI) * 0.9F * speed;
        float armSwingL = Mth.cos(cycle) * 0.9F * speed;
        float sway = Mth.sin(age * 0.09F) * 0.03F;
        this.armR.xRot = -0.3F + armSwingR - swing * 2.3F;
        this.armL.xRot = -0.3F + armSwingL - swing * 2.3F;
        this.armR.zRot = 0.05F + sway - swing * 0.12F;
        this.armL.zRot = -0.05F - sway + swing * 0.12F;
        this.foreR.xRot = -0.25F - swing * 0.7F - Math.abs(armSwingR) * 0.4F;
        this.foreL.xRot = -0.25F - swing * 0.7F - Math.abs(armSwingL) * 0.4F;

        this.legR.xRot = Mth.cos(cycle) * 1.3F * speed;
        this.legL.xRot = Mth.cos(cycle + (float) Math.PI) * 1.3F * speed;
        this.torso.yRot = this.chest.yRot * -0.3F;
    }
}
