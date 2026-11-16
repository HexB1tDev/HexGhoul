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
public final class BinBrothersMaskModel {
    public final ModelPart head;
    public BinBrothersMaskModel(ModelPart root) { this.head = root.getChild("head"); }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("shell", CubeListBuilder.create()
            .texOffs(34, 2).addBox(-1.5F, -0.5F, -5.5F, 3.0F, 0.75F, 0.9F)
            .texOffs(34, 2).addBox(-2.0F, -0.5F, -5.5F, 0.5F, 0.5F, 0.9F)
            .texOffs(34, 2).addBox(1.5F, -0.5F, -5.5F, 0.5F, 0.5F, 0.9F)
            .texOffs(34, 2).addBox(-2.5F, -0.5F, -5.5F, 0.5F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(2.0F, -0.5F, -5.5F, 0.5F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-2.75F, -0.75F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-2.5F, -1.5F, -5.625F, 5.0F, 1.0F, 1.025F)
            .texOffs(34, 2).addBox(2.5F, -0.75F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-3.0F, -1.5F, -5.5F, 0.25F, 0.75F, 0.9F)
            .texOffs(34, 2).addBox(-2.75F, -1.75F, -5.625F, 0.25F, 1.0F, 1.025F)
            .texOffs(34, 2).addBox(2.5F, -1.75F, -5.625F, 0.25F, 1.0F, 1.025F)
            .texOffs(34, 2).addBox(2.75F, -1.5F, -5.5F, 0.25F, 0.75F, 0.9F)
            .texOffs(34, 2).addBox(-3.25F, -7.0F, -5.5F, 0.25F, 6.0F, 0.9F)
            .texOffs(34, 2).addBox(3.0F, -7.0F, -5.5F, 0.25F, 6.0F, 0.9F)
            .texOffs(34, 2).addBox(-3.5F, -6.75F, -5.375F, 0.25F, 5.5F, 0.775F)
            .texOffs(34, 2).addBox(3.25F, -6.75F, -5.375F, 0.25F, 5.5F, 0.775F)
            .texOffs(34, 2).addBox(-3.75F, -6.5F, -5.375F, 0.25F, 5.0F, 0.775F)
            .texOffs(34, 2).addBox(-2.5F, -1.75F, -5.625F, 1.25F, 0.25F, 1.025F)
            .texOffs(34, 2).addBox(-1.0F, -7.5F, -5.625F, 2.0F, 6.0F, 1.025F)
            .texOffs(34, 2).addBox(1.25F, -1.75F, -5.625F, 1.25F, 0.25F, 1.025F)
            .texOffs(34, 2).addBox(3.5F, -6.5F, -5.375F, 0.25F, 5.0F, 0.775F)
            .texOffs(34, 2).addBox(-2.25F, -2.0F, -5.625F, 0.5F, 0.25F, 1.025F)
            .texOffs(34, 2).addBox(1.75F, -2.0F, -5.625F, 0.5F, 0.25F, 1.025F)
            .texOffs(34, 2).addBox(-4.0F, -6.0F, -5.25F, 0.25F, 4.0F, 0.65F)
            .texOffs(34, 2).addBox(-1.25F, -7.5F, -5.625F, 0.25F, 5.5F, 1.025F)
            .texOffs(34, 2).addBox(1.0F, -7.5F, -5.625F, 0.25F, 5.5F, 1.025F)
            .texOffs(34, 2).addBox(3.75F, -6.0F, -5.25F, 0.25F, 4.0F, 0.65F)
            .texOffs(34, 2).addBox(-3.0F, -7.25F, -5.5F, 0.25F, 5.0F, 0.9F)
            .texOffs(34, 2).addBox(-2.75F, -7.25F, -5.625F, 0.25F, 5.0F, 1.025F)
            .texOffs(34, 2).addBox(-1.5F, -7.5F, -5.625F, 0.25F, 5.25F, 1.025F)
            .texOffs(34, 2).addBox(1.25F, -7.5F, -5.625F, 0.25F, 5.25F, 1.025F)
            .texOffs(34, 2).addBox(2.5F, -7.25F, -5.625F, 0.25F, 5.0F, 1.025F)
            .texOffs(34, 2).addBox(2.75F, -7.25F, -5.5F, 0.25F, 5.0F, 0.9F)
            .texOffs(34, 2).addBox(-4.25F, -5.5F, -5.25F, 0.25F, 3.0F, 0.65F)
            .texOffs(34, 2).addBox(-2.5F, -7.5F, -5.625F, 1.0F, 5.0F, 1.025F)
            .texOffs(34, 2).addBox(1.5F, -7.5F, -5.625F, 1.0F, 5.0F, 1.025F)
            .texOffs(34, 2).addBox(4.0F, -5.5F, -5.25F, 0.25F, 3.0F, 0.65F)
            .texOffs(34, 2).addBox(-4.5F, -4.5F, -5.125F, 0.25F, 1.0F, 0.525F)
            .texOffs(34, 2).addBox(4.25F, -4.5F, -5.125F, 0.25F, 1.0F, 0.525F)
            .texOffs(34, 2).addBox(-2.75F, -7.5F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(2.5F, -7.5F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-2.5F, -7.75F, -5.5F, 5.0F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-2.0F, -8.0F, -5.5F, 4.0F, 0.25F, 0.9F)
            .texOffs(34, 2).addBox(-1.5F, -8.25F, -5.5F, 3.0F, 0.25F, 0.9F)
            , PartPose.ZERO);
        head.addOrReplaceChild("straps", CubeListBuilder.create()
            .texOffs(162, 2).addBox(-4.95F, -2.05F, -4.6F, 0.45F, 0.65F, 9.35F)
            .texOffs(162, 2).addBox(-4.95F, -2.05F, -5.1F, 1.75F, 0.65F, 0.5F)
            .texOffs(162, 2).addBox(4.5F, -2.05F, -4.6F, 0.45F, 0.65F, 9.35F)
            .texOffs(162, 2).addBox(3.2F, -2.05F, -5.1F, 1.75F, 0.65F, 0.5F)
            .texOffs(162, 2).addBox(-4.95F, -2.05F, 4.5F, 9.9F, 0.65F, 0.4F)
            , PartPose.ZERO);
        head.addOrReplaceChild("buckles", CubeListBuilder.create()
            .texOffs(194, 34).addBox(3.65F, -2.17F, 4.85F, 0.85F, 0.89F, 0.22F)
            .texOffs(130, 34).addBox(3.85F, -2.0F, 5.05F, 0.4F, 0.55F, 0.1F)
            , PartPose.ZERO);
        head.addOrReplaceChild("smile", CubeListBuilder.create()
            .texOffs(226, 2).addBox(-0.875F, -0.375F, -5.77F, 1.75F, 0.25F, 0.28F)
            .texOffs(226, 2).addBox(-1.5F, -0.375F, -5.77F, 0.625F, 0.125F, 0.28F)
            .texOffs(226, 2).addBox(0.875F, -0.375F, -5.77F, 0.625F, 0.125F, 0.28F)
            .texOffs(226, 2).addBox(-1.875F, -0.75F, -5.895F, 3.75F, 0.375F, 0.28F)
            .texOffs(226, 2).addBox(-2.25F, -1.125F, -5.895F, 0.375F, 0.625F, 0.28F)
            .texOffs(226, 2).addBox(1.875F, -1.125F, -5.895F, 0.375F, 0.625F, 0.28F)
            .texOffs(226, 2).addBox(-2.5F, -1.25F, -5.895F, 0.25F, 0.625F, 0.28F)
            .texOffs(226, 2).addBox(2.25F, -1.25F, -5.895F, 0.25F, 0.625F, 0.28F)
            .texOffs(226, 2).addBox(-2.75F, -1.0F, -5.77F, 0.125F, 0.25F, 0.28F)
            .texOffs(226, 2).addBox(-2.625F, -1.375F, -5.895F, 0.125F, 0.625F, 0.28F)
            .texOffs(226, 2).addBox(-1.875F, -0.875F, -5.895F, 1.25F, 0.125F, 0.28F)
            .texOffs(226, 2).addBox(0.625F, -0.875F, -5.895F, 1.25F, 0.125F, 0.28F)
            .texOffs(226, 2).addBox(2.5F, -1.375F, -5.895F, 0.125F, 0.625F, 0.28F)
            .texOffs(226, 2).addBox(2.625F, -1.0F, -5.77F, 0.125F, 0.25F, 0.28F)
            .texOffs(226, 2).addBox(-1.875F, -1.0F, -5.895F, 0.5F, 0.125F, 0.28F)
            .texOffs(226, 2).addBox(1.375F, -1.0F, -5.895F, 0.5F, 0.125F, 0.28F)
            .texOffs(226, 2).addBox(-2.75F, -1.375F, -5.895F, 0.125F, 0.375F, 0.28F)
            .texOffs(226, 2).addBox(-1.875F, -1.125F, -5.895F, 0.125F, 0.125F, 0.28F)
            .texOffs(226, 2).addBox(1.75F, -1.125F, -5.895F, 0.125F, 0.125F, 0.28F)
            .texOffs(226, 2).addBox(2.625F, -1.375F, -5.895F, 0.125F, 0.375F, 0.28F)
            .texOffs(226, 2).addBox(-2.25F, -1.25F, -5.895F, 0.125F, 0.125F, 0.28F)
            .texOffs(226, 2).addBox(2.125F, -1.25F, -5.895F, 0.125F, 0.125F, 0.28F)
            , PartPose.ZERO);
        head.addOrReplaceChild("cheeks", CubeListBuilder.create()
            .texOffs(2, 2).addBox(-3.25F, -2.0F, -5.86F, 0.25F, 1.25F, 0.38F)
            .texOffs(2, 2).addBox(-3.0F, -2.0F, -5.86F, 0.25F, 1.0F, 0.38F)
            .texOffs(2, 2).addBox(-2.75F, -2.0F, -5.985F, 0.25F, 1.0F, 0.38F)
            .texOffs(2, 2).addBox(-3.5F, -1.75F, -5.735F, 0.25F, 0.5F, 0.38F)
            .texOffs(2, 2).addBox(-2.5F, -2.0F, -5.985F, 0.25F, 0.75F, 0.38F)
            .texOffs(2, 2).addBox(-2.25F, -2.0F, -5.985F, 0.25F, 0.5F, 0.38F)
            .texOffs(2, 2).addBox(-2.0F, -2.0F, -5.985F, 0.25F, 0.25F, 0.38F)
            .texOffs(2, 2).addBox(3.0F, -2.0F, -5.86F, 0.25F, 1.25F, 0.38F)
            .texOffs(2, 2).addBox(2.5F, -2.0F, -5.985F, 0.25F, 1.0F, 0.38F)
            .texOffs(2, 2).addBox(2.75F, -2.0F, -5.86F, 0.25F, 1.0F, 0.38F)
            .texOffs(2, 2).addBox(2.25F, -2.0F, -5.985F, 0.25F, 0.75F, 0.38F)
            .texOffs(2, 2).addBox(3.25F, -1.75F, -5.735F, 0.25F, 0.5F, 0.38F)
            .texOffs(2, 2).addBox(2.0F, -2.0F, -5.985F, 0.25F, 0.5F, 0.38F)
            .texOffs(2, 2).addBox(1.75F, -2.0F, -5.985F, 0.25F, 0.25F, 0.38F)
            , PartPose.ZERO);
        head.addOrReplaceChild("nose", CubeListBuilder.create()
            .texOffs(2, 2).addBox(-0.4F, -1.42F, -6.43F, 0.8F, 0.37F, 1.83F)
            .texOffs(2, 2).addBox(-0.32F, -1.77F, -6.2804F, 0.64F, 0.37F, 1.6804F)
            .texOffs(2, 2).addBox(-0.24F, -2.12F, -6.1308F, 0.48F, 0.37F, 1.5308F)
            , PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }
    public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        head.render(pose, vertices, light, overlay, red, green, blue, alpha);
    }
}
