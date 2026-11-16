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
public final class AyatoKirishimaMaskModel {
    public final ModelPart head;
    public AyatoKirishimaMaskModel(ModelPart root) { this.head = root.getChild("head"); }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("shell", CubeListBuilder.create()
            .texOffs(162, 2).addBox(-3.0F, 0.65F, -5.375F, 0.25F, 0.75F, 0.775F)
            .texOffs(162, 2).addBox(-2.75F, 0.15F, -5.5F, 5.5F, 1.25F, 0.9F)
            .texOffs(162, 2).addBox(2.75F, 0.65F, -5.375F, 0.25F, 0.75F, 0.775F)
            .texOffs(162, 2).addBox(-3.25F, 0.15F, -5.375F, 0.25F, 1.0F, 0.775F)
            .texOffs(162, 2).addBox(3.0F, 0.15F, -5.375F, 0.25F, 1.0F, 0.775F)
            .texOffs(162, 2).addBox(-3.5F, 0.65F, -5.25F, 0.25F, 0.25F, 0.65F)
            .texOffs(162, 2).addBox(3.25F, 0.65F, -5.25F, 0.25F, 0.25F, 0.65F)
            .texOffs(162, 2).addBox(-3.75F, 0.15F, -5.25F, 0.25F, 0.5F, 0.65F)
            .texOffs(162, 2).addBox(-3.5F, -0.85F, -5.375F, 0.25F, 1.5F, 0.775F)
            .texOffs(162, 2).addBox(-3.0F, -0.85F, -5.5F, 0.25F, 1.5F, 0.9F)
            .texOffs(162, 2).addBox(2.75F, -0.85F, -5.5F, 0.25F, 1.5F, 0.9F)
            .texOffs(162, 2).addBox(3.25F, -0.85F, -5.375F, 0.25F, 1.5F, 0.775F)
            .texOffs(162, 2).addBox(3.5F, 0.15F, -5.25F, 0.25F, 0.5F, 0.65F)
            .texOffs(162, 2).addBox(-4.0F, -0.85F, -5.25F, 0.25F, 1.25F, 0.65F)
            .texOffs(162, 2).addBox(3.75F, -0.85F, -5.25F, 0.25F, 1.25F, 0.65F)
            .texOffs(162, 2).addBox(-4.25F, -0.35F, -5.125F, 0.25F, 0.5F, 0.525F)
            .texOffs(162, 2).addBox(-3.75F, -0.85F, -5.375F, 0.25F, 1.0F, 0.775F)
            .texOffs(162, 2).addBox(-3.25F, -0.85F, -5.5F, 0.25F, 1.0F, 0.9F)
            .texOffs(162, 2).addBox(-2.75F, -0.1F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(162, 2).addBox(-2.5F, -0.85F, -5.625F, 5.0F, 1.0F, 1.025F)
            .texOffs(162, 2).addBox(2.5F, -0.1F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(162, 2).addBox(3.0F, -0.85F, -5.5F, 0.25F, 1.0F, 0.9F)
            .texOffs(162, 2).addBox(3.5F, -0.85F, -5.375F, 0.25F, 1.0F, 0.775F)
            .texOffs(162, 2).addBox(4.0F, -0.35F, -5.125F, 0.25F, 0.5F, 0.525F)
            .texOffs(162, 2).addBox(-2.75F, -0.85F, -5.625F, 0.25F, 0.75F, 1.025F)
            .texOffs(162, 2).addBox(2.5F, -0.85F, -5.625F, 0.25F, 0.75F, 1.025F)
            .texOffs(162, 2).addBox(-4.25F, -0.85F, -5.25F, 0.25F, 0.5F, 0.65F)
            .texOffs(162, 2).addBox(4.0F, -0.85F, -5.25F, 0.25F, 0.5F, 0.65F)
            .texOffs(162, 2).addBox(-0.75F, -1.1F, -5.625F, 1.5F, 0.25F, 1.025F)
            .texOffs(162, 2).addBox(-0.5F, -1.35F, -5.625F, 1.0F, 0.25F, 1.025F)
            .texOffs(162, 2).addBox(-0.25F, -1.6F, -5.625F, 0.5F, 0.25F, 1.025F)
            , PartPose.ZERO);
        head.addOrReplaceChild("straps", CubeListBuilder.create()
            .texOffs(162, 2).addBox(-4.95F, -1.15F, -4.6F, 0.45F, 0.65F, 9.35F)
            .texOffs(162, 2).addBox(-4.95F, -1.15F, -5.1F, 1.75F, 0.65F, 0.5F)
            .texOffs(162, 2).addBox(4.5F, -1.15F, -4.6F, 0.45F, 0.65F, 9.35F)
            .texOffs(162, 2).addBox(3.2F, -1.15F, -5.1F, 1.75F, 0.65F, 0.5F)
            .texOffs(162, 2).addBox(-4.95F, -1.15F, 4.5F, 9.9F, 0.65F, 0.4F)
            , PartPose.ZERO);
        head.addOrReplaceChild("buckles", CubeListBuilder.create()
            .texOffs(194, 34).addBox(3.65F, -1.27F, 4.85F, 0.85F, 0.89F, 0.22F)
            .texOffs(130, 34).addBox(3.85F, -1.1F, 5.05F, 0.4F, 0.55F, 0.1F)
            , PartPose.ZERO);
        head.addOrReplaceChild("nose", CubeListBuilder.create()
            .texOffs(130, 2).addBox(-0.575F, -0.97F, -6.75F, 1.15F, 0.42F, 2.15F)
            .texOffs(130, 2).addBox(-0.46F, -1.37F, -6.53F, 0.92F, 0.42F, 1.93F)
            .texOffs(130, 2).addBox(-0.345F, -1.77F, -6.31F, 0.69F, 0.42F, 1.71F)
            , PartPose.ZERO);
        head.addOrReplaceChild("center_filter", CubeListBuilder.create()
            .texOffs(130, 2).addBox(-0.8F, -0.85F, -6.65F, 1.6F, 2.1F, 1.0F)
            , PartPose.ZERO);
        head.addOrReplaceChild("filter_ribs", CubeListBuilder.create()
            .texOffs(194, 34).addBox(-0.66F, -0.65F, -6.88F, 0.28F, 1.7F, 0.25F)
            .texOffs(194, 34).addBox(-0.14F, -0.65F, -6.88F, 0.28F, 1.7F, 0.25F)
            .texOffs(194, 34).addBox(0.38F, -0.65F, -6.88F, 0.28F, 1.7F, 0.25F)
            , PartPose.ZERO);
        head.addOrReplaceChild("hardware", CubeListBuilder.create()
            .texOffs(194, 34).addBox(-0.615F, 0.685F, -6.85F, 0.19F, 0.19F, 0.25F)
            .texOffs(162, 34).addBox(-0.5808F, 0.7192F, -6.91F, 0.1216F, 0.1216F, 0.13F)
            .texOffs(194, 34).addBox(-0.615F, 0.085F, -6.975F, 0.19F, 0.19F, 0.25F)
            .texOffs(162, 34).addBox(-0.5808F, 0.1192F, -7.035F, 0.1216F, 0.1216F, 0.13F)
            .texOffs(194, 34).addBox(-0.615F, -0.495F, -6.975F, 0.19F, 0.19F, 0.25F)
            .texOffs(162, 34).addBox(-0.5808F, -0.4608F, -7.035F, 0.1216F, 0.1216F, 0.13F)
            .texOffs(194, 34).addBox(-0.095F, 0.685F, -6.85F, 0.19F, 0.19F, 0.25F)
            .texOffs(162, 34).addBox(-0.0608F, 0.7192F, -6.91F, 0.1216F, 0.1216F, 0.13F)
            .texOffs(194, 34).addBox(-0.095F, 0.085F, -6.975F, 0.19F, 0.19F, 0.25F)
            .texOffs(162, 34).addBox(-0.0608F, 0.1192F, -7.035F, 0.1216F, 0.1216F, 0.13F)
            .texOffs(194, 34).addBox(-0.095F, -0.495F, -6.975F, 0.19F, 0.19F, 0.25F)
            .texOffs(162, 34).addBox(-0.0608F, -0.4608F, -7.035F, 0.1216F, 0.1216F, 0.13F)
            .texOffs(194, 34).addBox(0.425F, 0.685F, -6.85F, 0.19F, 0.19F, 0.25F)
            .texOffs(162, 34).addBox(0.4592F, 0.7192F, -6.91F, 0.1216F, 0.1216F, 0.13F)
            .texOffs(194, 34).addBox(0.425F, 0.085F, -6.975F, 0.19F, 0.19F, 0.25F)
            .texOffs(162, 34).addBox(0.4592F, 0.1192F, -7.035F, 0.1216F, 0.1216F, 0.13F)
            .texOffs(194, 34).addBox(0.425F, -0.495F, -6.975F, 0.19F, 0.19F, 0.25F)
            .texOffs(162, 34).addBox(0.4592F, -0.4608F, -7.035F, 0.1216F, 0.1216F, 0.13F)
            .texOffs(194, 34).addBox(-3.58F, -0.58F, -5.695F, 0.36F, 0.36F, 0.25F)
            .texOffs(162, 34).addBox(-3.5152F, -0.5152F, -5.755F, 0.2304F, 0.2304F, 0.13F)
            .texOffs(194, 34).addBox(3.22F, -0.58F, -5.695F, 0.36F, 0.36F, 0.25F)
            .texOffs(162, 34).addBox(3.2848F, -0.5152F, -5.755F, 0.2304F, 0.2304F, 0.13F)
            , PartPose.ZERO);
        head.addOrReplaceChild("respirator_ribs", CubeListBuilder.create()
            .texOffs(130, 2).addBox(-1.38F, -0.6F, -5.85F, 0.36F, 1.55F, 0.35F)
            .texOffs(130, 2).addBox(-1.98F, -0.6F, -5.85F, 0.36F, 1.55F, 0.35F)
            .texOffs(130, 2).addBox(-2.63F, -0.6F, -5.85F, 0.36F, 1.55F, 0.35F)
            .texOffs(130, 2).addBox(-3.38F, -0.6F, -5.725F, 0.36F, 1.55F, 0.35F)
            .texOffs(130, 2).addBox(1.02F, -0.6F, -5.85F, 0.36F, 1.55F, 0.35F)
            .texOffs(130, 2).addBox(1.62F, -0.6F, -5.85F, 0.36F, 1.55F, 0.35F)
            .texOffs(130, 2).addBox(2.27F, -0.6F, -5.85F, 0.36F, 1.55F, 0.35F)
            .texOffs(130, 2).addBox(3.02F, -0.6F, -5.725F, 0.36F, 1.55F, 0.35F)
            , PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }
    public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        head.render(pose, vertices, light, overlay, red, green, blue, alpha);
    }
}
