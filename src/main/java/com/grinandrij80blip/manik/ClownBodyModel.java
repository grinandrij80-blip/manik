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

        float speed = Math.min(1.0F, state.walkSpeed);
        float cycle = state.walkPos * 0.6F;
        float age = state.ageInTicks;
        float attack = state.attack;
        float swing = attack > 0.0F ? Mth.sin(attack * (float) Math.PI) : 0.0F;
        float breathe = Mth.sin(age * 0.06F);

        this.chest.xRot = 0.55F + breathe * 0.03F + swing * 0.5F;
        this.chest.yRot = attack > 0.0F ? Mth.sin(Mth.sqrt(attack) * (float) Math.PI * 2.0F) * 0.3F : 0.0F;

        // Creepy tilt of the head on top of looking where the player looks.
        this.head.xRot = -0.3F + state.xRot * DEG * 0.6F - swing * 0.2F;
        this.head.yRot = Mth.clamp(state.yRot, -70.0F, 70.0F) * DEG - this.chest.yRot * 0.6F;
        this.head.zRot = Mth.sin(age * 0.05F) * 0.07F;

        float armSwingR = Mth.cos(cycle + (float) Math.PI) * 0.5F * speed;
        float armSwingL = Mth.cos(cycle) * 0.5F * speed;
        float sway = Mth.sin(age * 0.07F) * 0.04F;
        // The mallet arm lifts overhead and slams down on an attack.
        this.armR.xRot = -0.15F + armSwingR - swing * 3.0F;
        this.armL.xRot = -0.15F + armSwingL - swing * 1.2F;
        this.armR.zRot = 0.08F + sway;
        this.armL.zRot = -0.08F - sway;
        this.foreR.xRot = -0.2F - swing * 0.6F - Math.abs(armSwingR) * 0.3F;
        this.foreL.xRot = -0.2F - swing * 0.4F - Math.abs(armSwingL) * 0.3F;

        this.legR.xRot = Mth.cos(cycle) * 1.0F * speed;
        this.legL.xRot = Mth.cos(cycle + (float) Math.PI) * 1.0F * speed;
        this.torso.yRot = this.chest.yRot * -0.3F;
    }
}
