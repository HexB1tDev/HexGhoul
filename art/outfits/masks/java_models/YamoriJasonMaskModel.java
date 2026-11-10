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
public final class YamoriJasonMaskModel {
    public final ModelPart head;
    public YamoriJasonMaskModel(ModelPart root) { this.head = root.getChild("head"); }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("shell", CubeListBuilder.create()
            .texOffs(34, 2).addBox(-1.5F, -0.5F, -5.5F, 1.25F, 0.75F, 0.9F)
            .texOffs(34, 2).addBox(0.25F, -0.5F, -5.5F, 1.25F, 0.75F, 0.9F)
            .texOffs(34, 2).addBox(-2.0F, -0.5F, -5.5F, 0.5F, 0.5F, 0.9F)
            .texOffs(34, 2).addBox(1.5F, -0.5F, -5.5F, 0.5F, 0.5F, 0.9F)
            .texOffs(34, 2).addBox(-2.5F, -0.5F, -5.5F, 0.5F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-0.25F, -0.5F, -5.5F, 0.5F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(2.0F, -0.5F, -5.5F, 0.5F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-2.75F, -0.75F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-2.5F, -1.25F, -5.625F, 1.25F, 0.75F, 1.025F)
            .texOffs(34, 2).addBox(-0.75F, -6.25F, -5.625F, 1.5F, 5.75F, 1.025F)
            .texOffs(34, 2).addBox(1.25F, -1.25F, -5.625F, 1.25F, 0.75F, 1.025F)
            .texOffs(34, 2).addBox(2.5F, -0.75F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-3.0F, -1.5F, -5.5F, 0.25F, 0.75F, 0.9F)
            .texOffs(34, 2).addBox(-2.75F, -1.25F, -5.625F, 0.25F, 0.5F, 1.025F)
            .texOffs(34, 2).addBox(2.5F, -1.25F, -5.625F, 0.25F, 0.5F, 1.025F)
            .texOffs(34, 2).addBox(2.75F, -1.5F, -5.5F, 0.25F, 0.75F, 0.9F)
            .texOffs(34, 2).addBox(-1.25F, -1.25F, -5.625F, 0.5F, 0.25F, 1.025F)
            .texOffs(34, 2).addBox(0.75F, -1.25F, -5.625F, 0.5F, 0.25F, 1.025F)
            .texOffs(34, 2).addBox(-3.5F, -6.75F, -5.375F, 0.25F, 5.5F, 0.775F)
            .texOffs(34, 2).addBox(-3.25F, -2.0F, -5.5F, 0.25F, 0.75F, 0.9F)
            .texOffs(34, 2).addBox(-1.5F, -1.5F, -5.625F, 0.25F, 0.25F, 1.025F)
            .texOffs(34, 2).addBox(1.25F, -1.5F, -5.625F, 0.25F, 0.25F, 1.025F)
            .texOffs(34, 2).addBox(3.0F, -2.0F, -5.5F, 0.25F, 0.75F, 0.9F)
            .texOffs(34, 2).addBox(3.25F, -6.75F, -5.375F, 0.25F, 5.5F, 0.775F)
            .texOffs(34, 2).addBox(-3.75F, -6.5F, -5.375F, 0.25F, 5.0F, 0.775F)
            .texOffs(34, 2).addBox(3.5F, -6.5F, -5.375F, 0.25F, 5.0F, 0.775F)
            .texOffs(34, 2).addBox(-1.25F, -2.0F, -5.625F, 0.5F, 0.25F, 1.025F)
            .texOffs(34, 2).addBox(0.75F, -2.0F, -5.625F, 0.5F, 0.25F, 1.025F)
            .texOffs(34, 2).addBox(-4.0F, -6.0F, -5.25F, 0.25F, 4.0F, 0.65F)
            .texOffs(34, 2).addBox(-1.0F, -7.5F, -5.625F, 0.25F, 5.5F, 1.025F)
            .texOffs(34, 2).addBox(0.75F, -7.5F, -5.625F, 0.25F, 5.5F, 1.025F)
            .texOffs(34, 2).addBox(3.75F, -6.0F, -5.25F, 0.25F, 4.0F, 0.65F)
            .texOffs(34, 2).addBox(-4.25F, -5.5F, -5.25F, 0.25F, 3.0F, 0.65F)
            .texOffs(34, 2).addBox(-3.25F, -7.0F, -5.5F, 0.25F, 4.5F, 0.9F)
            .texOffs(34, 2).addBox(-1.25F, -7.5F, -5.625F, 0.25F, 5.0F, 1.025F)
            .texOffs(34, 2).addBox(1.0F, -7.5F, -5.625F, 0.25F, 5.0F, 1.025F)
            .texOffs(34, 2).addBox(3.0F, -7.0F, -5.5F, 0.25F, 4.5F, 0.9F)
            .texOffs(34, 2).addBox(4.0F, -5.5F, -5.25F, 0.25F, 3.0F, 0.65F)
            .texOffs(34, 2).addBox(-3.0F, -7.25F, -5.5F, 0.25F, 4.25F, 0.9F)
            .texOffs(34, 2).addBox(-2.75F, -6.0F, -5.625F, 0.25F, 3.0F, 1.025F)
            .texOffs(34, 2).addBox(-1.75F, -7.5F, -5.625F, 0.5F, 4.5F, 1.025F)
            .texOffs(34, 2).addBox(1.25F, -7.5F, -5.625F, 0.5F, 4.5F, 1.025F)
            .texOffs(34, 2).addBox(2.5F, -6.0F, -5.625F, 0.25F, 3.0F, 1.025F)
            .texOffs(34, 2).addBox(2.75F, -7.25F, -5.5F, 0.25F, 4.25F, 0.9F)
            .texOffs(34, 2).addBox(-2.5F, -6.0F, -5.625F, 0.75F, 2.75F, 1.025F)
            .texOffs(34, 2).addBox(1.75F, -6.0F, -5.625F, 0.75F, 2.75F, 1.025F)
            .texOffs(34, 2).addBox(-4.5F, -4.5F, -5.125F, 0.25F, 1.0F, 0.525F)
            .texOffs(34, 2).addBox(4.25F, -4.5F, -5.125F, 0.25F, 1.0F, 0.525F)
            .texOffs(34, 2).addBox(-2.25F, -7.5F, -5.625F, 0.5F, 1.5F, 1.025F)
            .texOffs(34, 2).addBox(1.75F, -7.5F, -5.625F, 0.5F, 1.5F, 1.025F)
            .texOffs(34, 2).addBox(-0.75F, -7.5F, -5.625F, 0.5F, 1.25F, 1.025F)
            .texOffs(34, 2).addBox(0.25F, -7.5F, -5.625F, 0.5F, 1.25F, 1.025F)
            .texOffs(34, 2).addBox(-2.75F, -7.25F, -5.625F, 0.5F, 0.75F, 1.025F)
            .texOffs(34, 2).addBox(2.25F, -7.25F, -5.625F, 0.5F, 0.75F, 1.025F)
            .texOffs(34, 2).addBox(-0.25F, -7.5F, -5.625F, 0.5F, 0.75F, 1.025F)
            .texOffs(34, 2).addBox(-2.75F, -7.5F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-2.5F, -7.5F, -5.625F, 0.25F, 0.25F, 1.025F)
            .texOffs(34, 2).addBox(2.25F, -7.5F, -5.625F, 0.25F, 0.25F, 1.025F)
            .texOffs(34, 2).addBox(2.5F, -7.5F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-2.5F, -7.75F, -5.5F, 5.0F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-2.0F, -8.0F, -5.5F, 4.0F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-1.5F, -8.25F, -5.5F, 3.0F, 0.25F, 0.9F)
            , PartPose.ZERO);
        head.addOrReplaceChild("straps", CubeListBuilder.create()
            .texOffs(162, 2).addBox(-4.95F, -1.85F, -4.6F, 0.45F, 0.65F, 9.35F)
            .texOffs(162, 2).addBox(-4.95F, -1.85F, -5.1F, 1.75F, 0.65F, 0.5F)
            .texOffs(162, 2).addBox(4.5F, -1.85F, -4.6F, 0.45F, 0.65F, 9.35F)
            .texOffs(162, 2).addBox(3.2F, -1.85F, -5.1F, 1.75F, 0.65F, 0.5F)
            .texOffs(162, 2).addBox(-4.95F, -1.85F, 4.5F, 9.9F, 0.65F, 0.4F)
            .texOffs(162, 2).addBox(-4.95F, -6.35F, -4.6F, 0.45F, 0.65F, 9.35F)
            .texOffs(162, 2).addBox(-4.95F, -6.35F, -5.1F, 1.75F, 0.65F, 0.5F)
            .texOffs(162, 2).addBox(4.5F, -6.35F, -4.6F, 0.45F, 0.65F, 9.35F)
            .texOffs(162, 2).addBox(3.2F, -6.35F, -5.1F, 1.75F, 0.65F, 0.5F)
            .texOffs(162, 2).addBox(-4.95F, -6.35F, 4.5F, 9.9F, 0.65F, 0.4F)
            , PartPose.ZERO);
        head.addOrReplaceChild("buckles", CubeListBuilder.create()
            .texOffs(194, 34).addBox(3.65F, -1.97F, 4.85F, 0.85F, 0.89F, 0.22F)
            .texOffs(130, 34).addBox(3.85F, -1.8F, 5.05F, 0.4F, 0.55F, 0.1F)
            .texOffs(194, 34).addBox(3.65F, -6.47F, 4.85F, 0.85F, 0.89F, 0.22F)
            .texOffs(130, 34).addBox(3.85F, -6.3F, 5.05F, 0.4F, 0.55F, 0.1F)
            , PartPose.ZERO);
        head.addOrReplaceChild("nose", CubeListBuilder.create()
            .texOffs(2, 2).addBox(-0.425F, -2.07F, -6.55F, 0.85F, 0.52F, 1.95F)
            .texOffs(2, 2).addBox(-0.34F, -2.57F, -6.374F, 0.68F, 0.52F, 1.774F)
            .texOffs(2, 2).addBox(-0.255F, -3.07F, -6.198F, 0.51F, 0.52F, 1.598F)
            , PartPose.ZERO);
        head.addOrReplaceChild("hardware", CubeListBuilder.create()
            .texOffs(194, 34).addBox(-3.935F, -4.935F, -5.57F, 0.27F, 0.27F, 0.25F)
            .texOffs(162, 34).addBox(-3.8864F, -4.8864F, -5.63F, 0.1728F, 0.1728F, 0.13F)
            .texOffs(194, 34).addBox(-3.225F, -1.025F, -5.82F, 0.25F, 0.25F, 0.25F)
            .texOffs(162, 34).addBox(-3.18F, -0.98F, -5.88F, 0.16F, 0.16F, 0.13F)
            .texOffs(194, 34).addBox(3.665F, -4.935F, -5.57F, 0.27F, 0.27F, 0.25F)
            .texOffs(162, 34).addBox(3.7136F, -4.8864F, -5.63F, 0.1728F, 0.1728F, 0.13F)
            .texOffs(194, 34).addBox(2.975F, -1.025F, -5.82F, 0.25F, 0.25F, 0.25F)
            .texOffs(162, 34).addBox(3.02F, -0.98F, -5.88F, 0.16F, 0.16F, 0.13F)
            , PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }
    public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        head.render(pose, vertices, light, overlay, red, green, blue, alpha);
    }
}
