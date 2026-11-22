package com.grinandrij80blip.manik;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * A light rescue helicopter. helicopter.png is split into four colour bands of 32 px
 * (texOffs v = 0 body green, 32 dark grey, 64 glass blue, 96 red), so every cube uses one band.
 * The nose points to -Z; the main and tail rotors spin.
 */
public final class HelicopterModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.parse("manik:helicopter"), "main");

    private final ModelPart rotor;
    private final ModelPart tailRotor;

    public HelicopterModel(ModelPart root) {
        super(root);
        this.rotor = root.getChild("rotor");
        this.tailRotor = root.getChild("tail_rotor");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartPose ground = PartPose.offset(0.0F, 24.0F, 0.0F);

        root.addOrReplaceChild("cabin",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -13.0F, -8.0F, 10.0F, 9.0F, 15.0F), ground);
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(0, 64).addBox(-4.0F, -12.0F, -12.0F, 8.0F, 7.0F, 4.0F), ground);
        root.addOrReplaceChild("tail_boom",
                CubeListBuilder.create().texOffs(0, 32).addBox(-1.5F, -11.0F, 7.0F, 3.0F, 3.0F, 19.0F), ground);
        root.addOrReplaceChild("tail_fin",
                CubeListBuilder.create().texOffs(0, 96).addBox(-0.5F, -17.0F, 23.0F, 1.0F, 8.0F, 4.0F), ground);
        root.addOrReplaceChild("mast",
                CubeListBuilder.create().texOffs(0, 32).addBox(-1.0F, -17.0F, -2.0F, 2.0F, 4.0F, 2.0F), ground);
        root.addOrReplaceChild("skid_l",
                CubeListBuilder.create().texOffs(0, 32).addBox(-8.0F, -1.0F, -9.0F, 1.0F, 1.0F, 18.0F)
                        .texOffs(0, 32).addBox(-6.5F, -5.0F, -5.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(0, 32).addBox(-6.5F, -5.0F, 3.0F, 1.0F, 4.0F, 1.0F), ground);
        root.addOrReplaceChild("skid_r",
                CubeListBuilder.create().texOffs(0, 32).addBox(7.0F, -1.0F, -9.0F, 1.0F, 1.0F, 18.0F)
                        .texOffs(0, 32).addBox(5.5F, -5.0F, -5.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(0, 32).addBox(5.5F, -5.0F, 3.0F, 1.0F, 4.0F, 1.0F), ground);
        root.addOrReplaceChild("rotor",
                CubeListBuilder.create().texOffs(0, 32).addBox(-22.0F, -0.5F, -1.5F, 44.0F, 1.0F, 3.0F)
                        .texOffs(0, 32).addBox(-1.5F, -0.5F, -22.0F, 3.0F, 1.0F, 44.0F),
                PartPose.offset(0.0F, 24.0F - 18.0F, -1.0F));
        root.addOrReplaceChild("tail_rotor",
                CubeListBuilder.create().texOffs(0, 32).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offset(1.8F, 24.0F - 14.0F, 25.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        this.rotor.yRot = state.ageInTicks * 1.6F;
        this.tailRotor.xRot = state.ageInTicks * 1.9F;
    }
}
