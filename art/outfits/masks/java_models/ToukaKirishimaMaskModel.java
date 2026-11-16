package com.hex.masks.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Model geometry only. Head-local coordinates; 1 unit = 1 player-skin pixel. */
public final class ToukaKirishimaMaskModel {
    public final ModelPart head;
    public ToukaKirishimaMaskModel(ModelPart root) { this.head = root.getChild("head"); }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("shell", CubeListBuilder.create()
            .texOffs(34, 2).addBox(-0.5F, -0.5F, -5.5F, 1.0F, 1.0F, 0.9F)
            .texOffs(34, 2).addBox(-1.5F, -0.5F, -5.5F, 1.0F, 0.75F, 0.9F)
            .texOffs(34, 2).addBox(0.5F, -0.5F, -5.5F, 1.0F, 0.75F, 0.9F)
            .texOffs(34, 2).addBox(-2.0F, -0.5F, -5.5F, 0.5F, 0.5F, 0.9F)
            .texOffs(34, 2).addBox(1.5F, -0.5F, -5.5F, 0.5F, 0.5F, 0.9F)
            .texOffs(34, 2).addBox(-2.5F, -0.5F, -5.5F, 0.5F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(2.0F, -0.5F, -5.5F, 0.5F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-2.75F, -0.75F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-2.5F, -1.5F, -5.625F, 5.0F, 1.0F, 1.025F)
            .texOffs(34, 2).addBox(2.5F, -0.75F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-3.0F, -7.0F, -5.5F, 0.25F, 6.25F, 0.9F)
            .texOffs(34, 2).addBox(-2.75F, -7.25F, -5.625F, 0.25F, 6.5F, 1.025F)
            .texOffs(34, 2).addBox(2.5F, -7.25F, -5.625F, 0.25F, 6.5F, 1.025F)
            .texOffs(34, 2).addBox(2.75F, -7.0F, -5.5F, 0.25F, 6.25F, 0.9F)
            .texOffs(34, 2).addBox(-3.25F, -6.75F, -5.5F, 0.25F, 5.75F, 0.9F)
            .texOffs(34, 2).addBox(3.0F, -6.75F, -5.5F, 0.25F, 5.75F, 0.9F)
            .texOffs(34, 2).addBox(-3.5F, -6.5F, -5.375F, 0.25F, 5.25F, 0.775F)
            .texOffs(34, 2).addBox(3.25F, -6.5F, -5.375F, 0.25F, 5.25F, 0.775F)
            .texOffs(34, 2).addBox(-2.5F, -1.75F, -5.625F, 0.25F, 0.25F, 1.025F)
            .texOffs(34, 2).addBox(-2.0F, -1.75F, -5.625F, 4.0F, 0.25F, 1.025F)
            .texOffs(34, 2).addBox(2.25F, -1.75F, -5.625F, 0.25F, 0.25F, 1.025F)
            .texOffs(34, 2).addBox(-3.75F, -6.25F, -5.375F, 0.25F, 4.5F, 0.775F)
            .texOffs(34, 2).addBox(-1.75F, -7.5F, -5.625F, 3.5F, 5.75F, 1.025F)
            .texOffs(34, 2).addBox(3.5F, -6.25F, -5.375F, 0.25F, 4.5F, 0.775F)
            .texOffs(34, 2).addBox(-4.0F, -5.75F, -5.25F, 0.25F, 3.5F, 0.65F)
            .texOffs(34, 2).addBox(3.75F, -5.75F, -5.25F, 0.25F, 3.5F, 0.65F)
            .texOffs(34, 2).addBox(-4.25F, -5.0F, -5.25F, 0.25F, 2.25F, 0.65F)
            .texOffs(34, 2).addBox(-2.5F, -7.5F, -5.625F, 0.75F, 4.75F, 1.025F)
            .texOffs(34, 2).addBox(1.75F, -7.5F, -5.625F, 0.75F, 4.75F, 1.025F)
            .texOffs(34, 2).addBox(4.0F, -5.0F, -5.25F, 0.25F, 2.25F, 0.65F)
            .texOffs(34, 2).addBox(-2.0F, -7.75F, -5.5F, 4.0F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-1.5F, -8.0F, -5.5F, 3.0F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-0.75F, -8.25F, -5.5F, 1.5F, 0.25F, 0.9F)
            , PartPose.ZERO);
        head.addOrReplaceChild("straps", CubeListBuilder.create()
            .texOffs(162, 2).addBox(-4.95F, -2.6F, -4.6F, 0.45F, 0.6F, 9.35F)
            .texOffs(162, 2).addBox(-4.95F, -2.6F, -5.1F, 1.75F, 0.6F, 0.5F)
            .texOffs(162, 2).addBox(4.5F, -2.6F, -4.6F, 0.45F, 0.6F, 9.35F)
            .texOffs(162, 2).addBox(3.2F, -2.6F, -5.1F, 1.75F, 0.6F, 0.5F)
            .texOffs(162, 2).addBox(-4.95F, -2.6F, 4.5F, 9.9F, 0.6F, 0.4F)
            , PartPose.ZERO);
        head.addOrReplaceChild("buckles", CubeListBuilder.create()
            .texOffs(194, 34).addBox(3.65F, -2.72F, 4.85F, 0.85F, 0.84F, 0.22F)
            .texOffs(130, 34).addBox(3.85F, -2.55F, 5.05F, 0.4F, 0.5F, 0.1F)
            , PartPose.ZERO);
        head.addOrReplaceChild("ears", CubeListBuilder.create()
            .texOffs(34, 2).addBox(-2.5F, -8.0F, -5.7F, 1.5F, 0.5F, 1.1F)
            .texOffs(34, 2).addBox(-2.75F, -13.0F, -5.7F, 0.25F, 5.25F, 1.1F)
            .texOffs(34, 2).addBox(-2.5F, -11.0F, -5.7F, 1.25F, 3.0F, 1.1F)
            .texOffs(34, 2).addBox(-3.0F, -13.0F, -5.575F, 0.25F, 3.5F, 1.1F)
            .texOffs(34, 2).addBox(-3.25F, -13.0F, -5.575F, 0.25F, 2.0F, 1.1F)
            .texOffs(34, 2).addBox(-2.5F, -12.75F, -5.7F, 1.0F, 1.75F, 1.1F)
            .texOffs(34, 2).addBox(-3.5F, -12.5F, -5.45F, 0.25F, 0.75F, 1.1F)
            .texOffs(34, 2).addBox(-2.5F, -13.0F, -5.7F, 0.25F, 0.25F, 1.1F)
            .texOffs(34, 2).addBox(1.0F, -8.0F, -5.7F, 1.5F, 0.5F, 1.1F)
            .texOffs(34, 2).addBox(2.5F, -13.0F, -5.7F, 0.25F, 5.25F, 1.1F)
            .texOffs(34, 2).addBox(1.25F, -11.0F, -5.7F, 1.25F, 3.0F, 1.1F)
            .texOffs(34, 2).addBox(2.75F, -13.0F, -5.575F, 0.25F, 3.5F, 1.1F)
            .texOffs(34, 2).addBox(1.5F, -12.75F, -5.7F, 1.0F, 1.75F, 1.1F)
            .texOffs(34, 2).addBox(3.0F, -13.0F, -5.575F, 0.25F, 2.0F, 1.1F)
            .texOffs(34, 2).addBox(3.25F, -12.5F, -5.45F, 0.25F, 0.75F, 1.1F)
            .texOffs(34, 2).addBox(2.25F, -13.0F, -5.7F, 0.25F, 0.25F, 1.1F)
            , PartPose.ZERO);
        head.addOrReplaceChild("ear_insets", CubeListBuilder.create()
            .texOffs(226, 34).addBox(-2.25F, -8.5F, -5.86F, 0.75F, 0.25F, 0.2F)
            .texOffs(226, 34).addBox(-2.25F, -11.0F, -5.86F, 0.5F, 2.5F, 0.2F)
            .texOffs(226, 34).addBox(-2.5F, -12.25F, -5.86F, 0.25F, 2.75F, 0.2F)
            .texOffs(226, 34).addBox(-2.75F, -12.25F, -5.86F, 0.25F, 1.25F, 0.2F)
            .texOffs(226, 34).addBox(-2.25F, -12.25F, -5.86F, 0.25F, 1.25F, 0.2F)
            .texOffs(226, 34).addBox(-3.0F, -12.25F, -5.735F, 0.25F, 0.5F, 0.2F)
            .texOffs(226, 34).addBox(1.5F, -8.5F, -5.86F, 0.75F, 0.25F, 0.2F)
            .texOffs(226, 34).addBox(1.75F, -11.0F, -5.86F, 0.5F, 2.5F, 0.2F)
            .texOffs(226, 34).addBox(2.25F, -12.25F, -5.86F, 0.25F, 2.75F, 0.2F)
            .texOffs(226, 34).addBox(2.0F, -12.25F, -5.86F, 0.25F, 1.25F, 0.2F)
            .texOffs(226, 34).addBox(2.5F, -12.25F, -5.86F, 0.25F, 1.25F, 0.2F)
            .texOffs(226, 34).addBox(2.75F, -12.25F, -5.735F, 0.25F, 0.5F, 0.2F)
            , PartPose.ZERO);
        head.addOrReplaceChild("bunny_muzzle", CubeListBuilder.create()
            .texOffs(226, 2).addBox(-0.125F, -1.75F, -5.905F, 0.25F, 0.375F, 0.2F)
            .texOffs(226, 2).addBox(-0.25F, -1.75F, -5.905F, 0.125F, 0.25F, 0.2F)
            .texOffs(226, 2).addBox(0.125F, -1.75F, -5.905F, 0.125F, 0.25F, 0.2F)
            .texOffs(226, 2).addBox(-0.075F, -1.45F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(-0.075F, -1.325F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(-0.075F, -1.2F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(-0.075F, -1.075F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(-0.075F, -0.95F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(-0.2F, -0.95F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(-0.325F, -0.825F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(-0.45F, -0.7F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(-0.575F, -0.825F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(-0.7F, -0.95F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(-0.825F, -1.075F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(-0.075F, -0.95F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(0.05F, -0.95F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(0.175F, -0.825F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(0.3F, -0.7F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(0.425F, -0.825F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(0.55F, -0.95F, -5.865F, 0.15F, 0.15F, 0.16F)
            .texOffs(226, 2).addBox(0.675F, -1.075F, -5.865F, 0.15F, 0.15F, 0.16F)
            , PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }
    public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        head.render(pose, vertices, light, overlay, red, green, blue, alpha);
    }
}
