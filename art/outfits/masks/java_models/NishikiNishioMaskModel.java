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
public final class NishikiNishioMaskModel {
    public final ModelPart head;
    public NishikiNishioMaskModel(ModelPart root) { this.head = root.getChild("head"); }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("shell", CubeListBuilder.create()
            .texOffs(130, 2).addBox(-3.0F, 0.65F, -5.375F, 0.25F, 0.75F, 0.775F)
            .texOffs(130, 2).addBox(-2.75F, 0.15F, -5.5F, 5.5F, 1.25F, 0.9F)
            .texOffs(130, 2).addBox(2.75F, 0.65F, -5.375F, 0.25F, 0.75F, 0.775F)
            .texOffs(130, 2).addBox(-3.25F, 0.15F, -5.375F, 0.25F, 1.0F, 0.775F)
            .texOffs(130, 2).addBox(3.0F, 0.15F, -5.375F, 0.25F, 1.0F, 0.775F)
            .texOffs(130, 2).addBox(-3.5F, 0.65F, -5.25F, 0.25F, 0.25F, 0.65F)
            .texOffs(130, 2).addBox(3.25F, 0.65F, -5.25F, 0.25F, 0.25F, 0.65F)
            .texOffs(130, 2).addBox(-3.75F, 0.15F, -5.25F, 0.25F, 0.5F, 0.65F)
            .texOffs(130, 2).addBox(-3.5F, -0.85F, -5.375F, 0.25F, 1.5F, 0.775F)
            .texOffs(130, 2).addBox(-3.0F, -0.85F, -5.5F, 0.25F, 1.5F, 0.9F)
            .texOffs(130, 2).addBox(2.75F, -0.85F, -5.5F, 0.25F, 1.5F, 0.9F)
            .texOffs(130, 2).addBox(3.25F, -0.85F, -5.375F, 0.25F, 1.5F, 0.775F)
            .texOffs(130, 2).addBox(3.5F, 0.15F, -5.25F, 0.25F, 0.5F, 0.65F)
            .texOffs(130, 2).addBox(-4.0F, -0.85F, -5.25F, 0.25F, 1.25F, 0.65F)
            .texOffs(130, 2).addBox(3.75F, -0.85F, -5.25F, 0.25F, 1.25F, 0.65F)
            .texOffs(130, 2).addBox(-4.25F, -0.35F, -5.125F, 0.25F, 0.5F, 0.525F)
            .texOffs(130, 2).addBox(-3.75F, -0.85F, -5.375F, 0.25F, 1.0F, 0.775F)
            .texOffs(130, 2).addBox(-3.25F, -0.85F, -5.5F, 0.25F, 1.0F, 0.9F)
            .texOffs(130, 2).addBox(-2.75F, -0.1F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(130, 2).addBox(-2.5F, -0.85F, -5.625F, 5.0F, 1.0F, 1.025F)
            .texOffs(130, 2).addBox(2.5F, -0.1F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(130, 2).addBox(3.0F, -0.85F, -5.5F, 0.25F, 1.0F, 0.9F)
            .texOffs(130, 2).addBox(3.5F, -0.85F, -5.375F, 0.25F, 1.0F, 0.775F)
            .texOffs(130, 2).addBox(4.0F, -0.35F, -5.125F, 0.25F, 0.5F, 0.525F)
            .texOffs(130, 2).addBox(-2.75F, -0.85F, -5.625F, 0.25F, 0.75F, 1.025F)
            .texOffs(130, 2).addBox(2.5F, -0.85F, -5.625F, 0.25F, 0.75F, 1.025F)
            .texOffs(130, 2).addBox(-4.25F, -0.85F, -5.25F, 0.25F, 0.5F, 0.65F)
            .texOffs(130, 2).addBox(4.0F, -0.85F, -5.25F, 0.25F, 0.5F, 0.65F)
            .texOffs(130, 2).addBox(-0.75F, -1.1F, -5.625F, 1.5F, 0.25F, 1.025F)
            .texOffs(130, 2).addBox(-0.5F, -1.35F, -5.625F, 1.0F, 0.25F, 1.025F)
            .texOffs(130, 2).addBox(-0.25F, -1.6F, -5.625F, 0.5F, 0.25F, 1.025F)
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
        head.addOrReplaceChild("mouth", CubeListBuilder.create()
            .texOffs(130, 2).addBox(-1.5F, 0.3F, -5.75F, 3.0F, 1.0F, 0.25F)
            .texOffs(130, 2).addBox(-2.5F, 0.3F, -5.75F, 1.0F, 0.75F, 0.25F)
            .texOffs(130, 2).addBox(1.5F, 0.3F, -5.75F, 1.0F, 0.75F, 0.25F)
            .texOffs(130, 2).addBox(-2.75F, -0.2F, -5.75F, 0.25F, 1.0F, 0.25F)
            .texOffs(130, 2).addBox(2.5F, -0.2F, -5.75F, 0.25F, 1.0F, 0.25F)
            .texOffs(130, 2).addBox(-3.0F, 0.05F, -5.75F, 0.25F, 0.5F, 0.25F)
            .texOffs(130, 2).addBox(2.75F, 0.05F, -5.75F, 0.25F, 0.5F, 0.25F)
            .texOffs(130, 2).addBox(-2.5F, -0.45F, -5.875F, 5.0F, 0.75F, 0.25F)
            .texOffs(130, 2).addBox(-1.5F, -0.7F, -5.875F, 3.0F, 0.25F, 0.25F)
            .texOffs(226, 2).addBox(-1.5F, 0.3F, -5.93F, 3.0F, 0.75F, 0.15F)
            .texOffs(226, 2).addBox(-2.5F, 0.3F, -5.93F, 1.0F, 0.5F, 0.15F)
            .texOffs(226, 2).addBox(1.5F, 0.3F, -5.93F, 1.0F, 0.5F, 0.15F)
            .texOffs(226, 2).addBox(-2.75F, 0.05F, -5.93F, 0.25F, 0.5F, 0.15F)
            .texOffs(226, 2).addBox(2.5F, 0.05F, -5.93F, 0.25F, 0.5F, 0.15F)
            .texOffs(226, 2).addBox(-2.5F, -0.2F, -6.055F, 5.0F, 0.5F, 0.15F)
            .texOffs(226, 2).addBox(-1.5F, -0.45F, -6.055F, 3.0F, 0.25F, 0.15F)
            , PartPose.ZERO);
        head.addOrReplaceChild("upper_teeth", CubeListBuilder.create()
            .texOffs(98, 34).addBox(-2.785F, -0.71829F, -6.22F, 0.485F, 0.567F, 0.43F)
            .texOffs(98, 34).addBox(-2.22F, -0.60276F, -6.22F, 0.485F, 0.591F, 0.43F)
            .texOffs(98, 34).addBox(-1.655F, -0.51274F, -6.22F, 0.485F, 0.615F, 0.43F)
            .texOffs(98, 34).addBox(-1.09F, -0.45099F, -6.22F, 0.485F, 0.639F, 0.43F)
            .texOffs(98, 34).addBox(-0.525F, -0.42258F, -6.22F, 0.485F, 0.663F, 0.43F)
            .texOffs(98, 34).addBox(0.04F, -0.42258F, -6.22F, 0.485F, 0.663F, 0.43F)
            .texOffs(98, 34).addBox(0.605F, -0.45099F, -6.22F, 0.485F, 0.639F, 0.43F)
            .texOffs(98, 34).addBox(1.17F, -0.51274F, -6.22F, 0.485F, 0.615F, 0.43F)
            .texOffs(98, 34).addBox(1.735F, -0.60276F, -6.22F, 0.485F, 0.591F, 0.43F)
            .texOffs(98, 34).addBox(2.3F, -0.71829F, -6.22F, 0.485F, 0.567F, 0.43F)
            , PartPose.ZERO);
        head.addOrReplaceChild("lower_teeth", CubeListBuilder.create()
            .texOffs(98, 34).addBox(-2.785F, -0.05129F, -6.19F, 0.485F, 0.567F, 0.4F)
            .texOffs(98, 34).addBox(-2.22F, 0.08824F, -6.19F, 0.485F, 0.591F, 0.4F)
            .texOffs(98, 34).addBox(-1.655F, 0.20226F, -6.19F, 0.485F, 0.615F, 0.4F)
            .texOffs(98, 34).addBox(-1.09F, 0.28801F, -6.19F, 0.485F, 0.639F, 0.4F)
            .texOffs(98, 34).addBox(-0.525F, 0.34042F, -6.19F, 0.485F, 0.663F, 0.4F)
            .texOffs(98, 34).addBox(0.04F, 0.34042F, -6.19F, 0.485F, 0.663F, 0.4F)
            .texOffs(98, 34).addBox(0.605F, 0.28801F, -6.19F, 0.485F, 0.639F, 0.4F)
            .texOffs(98, 34).addBox(1.17F, 0.20226F, -6.19F, 0.485F, 0.615F, 0.4F)
            .texOffs(98, 34).addBox(1.735F, 0.08824F, -6.19F, 0.485F, 0.591F, 0.4F)
            .texOffs(98, 34).addBox(2.3F, -0.05129F, -6.19F, 0.485F, 0.567F, 0.4F)
            , PartPose.ZERO);
        head.addOrReplaceChild("nose", CubeListBuilder.create()
            .texOffs(162, 2).addBox(-0.6F, -1.02F, -6.6F, 1.2F, 0.37F, 2.0F)
            .texOffs(162, 2).addBox(-0.48F, -1.37F, -6.413F, 0.96F, 0.37F, 1.813F)
            .texOffs(162, 2).addBox(-0.36F, -1.72F, -6.226F, 0.72F, 0.37F, 1.626F)
            , PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }
    public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        head.render(pose, vertices, light, overlay, red, green, blue, alpha);
    }
}
