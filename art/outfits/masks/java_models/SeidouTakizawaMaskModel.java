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
public final class SeidouTakizawaMaskModel {
    public final ModelPart head;
    public SeidouTakizawaMaskModel(ModelPart root) { this.head = root.getChild("head"); }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("shell", CubeListBuilder.create()
            .texOffs(2, 66).addBox(-0.5F, -0.5F, -5.5F, 1.0F, 1.0F, 0.9F)
            .texOffs(2, 66).addBox(-1.25F, -0.5F, -5.5F, 0.75F, 0.75F, 0.9F)
            .texOffs(2, 66).addBox(0.5F, -0.5F, -5.5F, 0.75F, 0.75F, 0.9F)
            .texOffs(2, 66).addBox(-1.75F, -0.5F, -5.5F, 0.5F, 0.5F, 0.9F)
            .texOffs(2, 66).addBox(1.25F, -0.5F, -5.5F, 0.5F, 0.5F, 0.9F)
            .texOffs(2, 66).addBox(-2.5F, -0.5F, -5.5F, 0.75F, 0.25F, 0.9F)
            .texOffs(2, 66).addBox(1.75F, -0.5F, -5.5F, 0.75F, 0.25F, 0.9F)
            .texOffs(2, 66).addBox(-3.0F, -0.75F, -5.5F, 0.5F, 0.25F, 0.9F)
            .texOffs(2, 66).addBox(-2.5F, -1.75F, -5.625F, 5.0F, 1.25F, 1.025F)
            .texOffs(2, 66).addBox(2.5F, -0.75F, -5.5F, 0.5F, 0.25F, 0.9F)
            .texOffs(2, 66).addBox(-3.25F, -1.75F, -5.5F, 0.5F, 1.0F, 0.9F)
            .texOffs(2, 66).addBox(-2.75F, -1.75F, -5.625F, 0.25F, 1.0F, 1.025F)
            .texOffs(2, 66).addBox(2.5F, -1.75F, -5.625F, 0.25F, 1.0F, 1.025F)
            .texOffs(2, 66).addBox(2.75F, -1.75F, -5.5F, 0.5F, 1.0F, 0.9F)
            .texOffs(2, 66).addBox(-3.5F, -8.0F, -5.375F, 0.25F, 7.0F, 0.775F)
            .texOffs(2, 66).addBox(3.25F, -8.0F, -5.375F, 0.25F, 7.0F, 0.775F)
            .texOffs(2, 66).addBox(-3.75F, -7.5F, -5.375F, 0.25F, 6.25F, 0.775F)
            .texOffs(2, 66).addBox(3.5F, -7.5F, -5.375F, 0.25F, 6.25F, 0.775F)
            .texOffs(2, 66).addBox(-4.0F, -7.75F, -5.25F, 0.25F, 6.25F, 0.65F)
            .texOffs(2, 66).addBox(3.75F, -7.75F, -5.25F, 0.25F, 6.25F, 0.65F)
            .texOffs(2, 66).addBox(-4.25F, -7.0F, -5.25F, 0.25F, 5.25F, 0.65F)
            .texOffs(2, 66).addBox(-1.25F, -7.5F, -5.625F, 2.5F, 5.75F, 1.025F)
            .texOffs(2, 66).addBox(4.0F, -7.0F, -5.25F, 0.25F, 5.25F, 0.65F)
            .texOffs(2, 66).addBox(-3.25F, -7.5F, -5.5F, 0.5F, 5.0F, 0.9F)
            .texOffs(2, 66).addBox(-2.75F, -7.25F, -5.625F, 1.5F, 4.75F, 1.025F)
            .texOffs(2, 66).addBox(1.25F, -7.25F, -5.625F, 1.5F, 4.75F, 1.025F)
            .texOffs(2, 66).addBox(2.75F, -7.5F, -5.5F, 0.5F, 5.0F, 0.9F)
            .texOffs(2, 66).addBox(-4.25F, -7.75F, -5.125F, 0.25F, 0.75F, 0.525F)
            .texOffs(2, 66).addBox(4.0F, -7.75F, -5.125F, 0.25F, 0.75F, 0.525F)
            .texOffs(2, 66).addBox(-2.75F, -8.25F, -5.5F, 0.25F, 1.0F, 0.9F)
            .texOffs(2, 66).addBox(-2.5F, -7.5F, -5.625F, 1.25F, 0.25F, 1.025F)
            .texOffs(2, 66).addBox(1.25F, -7.5F, -5.625F, 1.25F, 0.25F, 1.025F)
            .texOffs(2, 66).addBox(2.5F, -8.25F, -5.5F, 0.25F, 1.0F, 0.9F)
            .texOffs(2, 66).addBox(-3.75F, -8.25F, -5.25F, 0.25F, 0.75F, 0.65F)
            .texOffs(2, 66).addBox(-3.25F, -8.25F, -5.375F, 0.25F, 0.75F, 0.775F)
            .texOffs(2, 66).addBox(-3.0F, -8.0F, -5.5F, 0.25F, 0.5F, 0.9F)
            .texOffs(2, 66).addBox(-2.5F, -8.25F, -5.5F, 5.0F, 0.75F, 0.9F)
            .texOffs(2, 66).addBox(2.75F, -8.0F, -5.5F, 0.25F, 0.5F, 0.9F)
            .texOffs(2, 66).addBox(3.0F, -8.25F, -5.375F, 0.25F, 0.75F, 0.775F)
            .texOffs(2, 66).addBox(3.5F, -8.25F, -5.25F, 0.25F, 0.75F, 0.65F)
            .texOffs(2, 66).addBox(-4.0F, -8.0F, -5.125F, 0.25F, 0.25F, 0.525F)
            .texOffs(2, 66).addBox(3.75F, -8.0F, -5.125F, 0.25F, 0.25F, 0.525F)
            .texOffs(2, 66).addBox(-3.5F, -8.25F, -5.25F, 0.25F, 0.25F, 0.65F)
            .texOffs(2, 66).addBox(-3.0F, -8.25F, -5.375F, 0.25F, 0.25F, 0.775F)
            .texOffs(2, 66).addBox(2.75F, -8.25F, -5.375F, 0.25F, 0.25F, 0.775F)
            .texOffs(2, 66).addBox(3.25F, -8.25F, -5.25F, 0.25F, 0.25F, 0.65F)
            , PartPose.ZERO);
        head.addOrReplaceChild("straps", CubeListBuilder.create()
            .texOffs(2, 66).addBox(-4.95F, -1.85F, -4.6F, 0.45F, 0.65F, 9.35F)
            .texOffs(2, 66).addBox(-4.95F, -1.85F, -5.1F, 1.75F, 0.65F, 0.5F)
            .texOffs(2, 66).addBox(4.5F, -1.85F, -4.6F, 0.45F, 0.65F, 9.35F)
            .texOffs(2, 66).addBox(3.2F, -1.85F, -5.1F, 1.75F, 0.65F, 0.5F)
            .texOffs(2, 66).addBox(-4.95F, -1.85F, 4.5F, 9.9F, 0.65F, 0.4F)
            .texOffs(2, 66).addBox(-4.95F, -6.35F, -4.6F, 0.45F, 0.65F, 9.35F)
            .texOffs(2, 66).addBox(-4.95F, -6.35F, -5.1F, 1.75F, 0.65F, 0.5F)
            .texOffs(2, 66).addBox(4.5F, -6.35F, -4.6F, 0.45F, 0.65F, 9.35F)
            .texOffs(2, 66).addBox(3.2F, -6.35F, -5.1F, 1.75F, 0.65F, 0.5F)
            .texOffs(2, 66).addBox(-4.95F, -6.35F, 4.5F, 9.9F, 0.65F, 0.4F)
            , PartPose.ZERO);
        head.addOrReplaceChild("buckles", CubeListBuilder.create()
            .texOffs(194, 34).addBox(3.65F, -1.97F, 4.85F, 0.85F, 0.89F, 0.22F)
            .texOffs(130, 34).addBox(3.85F, -1.8F, 5.05F, 0.4F, 0.55F, 0.1F)
            .texOffs(194, 34).addBox(3.65F, -6.47F, 4.85F, 0.85F, 0.89F, 0.22F)
            .texOffs(130, 34).addBox(3.85F, -6.3F, 5.05F, 0.4F, 0.55F, 0.1F)
            , PartPose.ZERO);
        head.addOrReplaceChild("mouth", CubeListBuilder.create()
            .texOffs(66, 34).addBox(-1.5F, -0.35F, -5.75F, 3.0F, 0.75F, 0.25F)
            .texOffs(66, 34).addBox(-2.5F, -0.35F, -5.75F, 1.0F, 0.5F, 0.25F)
            .texOffs(66, 34).addBox(1.5F, -0.35F, -5.75F, 1.0F, 0.5F, 0.25F)
            .texOffs(66, 34).addBox(-3.0F, -0.85F, -5.75F, 0.5F, 0.75F, 0.25F)
            .texOffs(66, 34).addBox(2.5F, -0.85F, -5.75F, 0.5F, 0.75F, 0.25F)
            .texOffs(66, 34).addBox(-3.25F, -0.6F, -5.625F, 0.25F, 0.25F, 0.25F)
            .texOffs(66, 34).addBox(-2.5F, -1.35F, -5.875F, 5.0F, 1.0F, 0.25F)
            .texOffs(66, 34).addBox(3.0F, -0.6F, -5.625F, 0.25F, 0.25F, 0.25F)
            .texOffs(66, 34).addBox(-3.25F, -0.85F, -5.75F, 0.25F, 0.25F, 0.25F)
            .texOffs(66, 34).addBox(3.0F, -0.85F, -5.75F, 0.25F, 0.25F, 0.25F)
            .texOffs(66, 34).addBox(-3.0F, -1.1F, -5.75F, 0.25F, 0.25F, 0.25F)
            .texOffs(66, 34).addBox(-2.75F, -1.35F, -5.875F, 0.25F, 0.5F, 0.25F)
            .texOffs(66, 34).addBox(2.5F, -1.35F, -5.875F, 0.25F, 0.5F, 0.25F)
            .texOffs(66, 34).addBox(2.75F, -1.1F, -5.75F, 0.25F, 0.25F, 0.25F)
            .texOffs(66, 34).addBox(-2.0F, -1.6F, -5.875F, 4.0F, 0.25F, 0.25F)
            .texOffs(226, 2).addBox(-1.5F, -0.35F, -5.93F, 3.0F, 0.5F, 0.15F)
            .texOffs(226, 2).addBox(-2.5F, -0.35F, -5.93F, 1.0F, 0.25F, 0.15F)
            .texOffs(226, 2).addBox(1.5F, -0.35F, -5.93F, 1.0F, 0.25F, 0.15F)
            .texOffs(226, 2).addBox(-3.0F, -0.85F, -5.93F, 0.5F, 0.5F, 0.15F)
            .texOffs(226, 2).addBox(-2.5F, -1.1F, -6.055F, 5.0F, 0.75F, 0.15F)
            .texOffs(226, 2).addBox(2.5F, -0.85F, -5.93F, 0.5F, 0.5F, 0.15F)
            .texOffs(226, 2).addBox(-2.75F, -1.1F, -6.055F, 0.25F, 0.25F, 0.15F)
            .texOffs(226, 2).addBox(2.5F, -1.1F, -6.055F, 0.25F, 0.25F, 0.15F)
            .texOffs(226, 2).addBox(-2.0F, -1.35F, -6.055F, 4.0F, 0.25F, 0.15F)
            , PartPose.ZERO);
        head.addOrReplaceChild("upper_teeth", CubeListBuilder.create()
            .texOffs(98, 34).addBox(-2.96F, -1.70609F, -6.22F, 0.52F, 0.6048F, 0.43F)
            .texOffs(98, 34).addBox(-2.36F, -1.59216F, -6.345F, 0.52F, 0.6304F, 0.43F)
            .texOffs(98, 34).addBox(-1.76F, -1.50374F, -6.345F, 0.52F, 0.656F, 0.43F)
            .texOffs(98, 34).addBox(-1.16F, -1.44359F, -6.345F, 0.52F, 0.6816F, 0.43F)
            .texOffs(98, 34).addBox(-0.56F, -1.41678F, -6.345F, 0.52F, 0.7072F, 0.43F)
            .texOffs(98, 34).addBox(0.04F, -1.41678F, -6.345F, 0.52F, 0.7072F, 0.43F)
            .texOffs(98, 34).addBox(0.64F, -1.44359F, -6.345F, 0.52F, 0.6816F, 0.43F)
            .texOffs(98, 34).addBox(1.24F, -1.50374F, -6.345F, 0.52F, 0.656F, 0.43F)
            .texOffs(98, 34).addBox(1.84F, -1.59216F, -6.345F, 0.52F, 0.6304F, 0.43F)
            .texOffs(98, 34).addBox(2.44F, -1.70609F, -6.22F, 0.52F, 0.6048F, 0.43F)
            , PartPose.ZERO);
        head.addOrReplaceChild("lower_teeth", CubeListBuilder.create()
            .texOffs(98, 34).addBox(-2.96F, -1.00129F, -6.19F, 0.52F, 0.6048F, 0.4F)
            .texOffs(98, 34).addBox(-2.36F, -0.86176F, -6.315F, 0.52F, 0.6304F, 0.4F)
            .texOffs(98, 34).addBox(-1.76F, -0.74774F, -6.315F, 0.52F, 0.656F, 0.4F)
            .texOffs(98, 34).addBox(-1.16F, -0.66199F, -6.315F, 0.52F, 0.6816F, 0.4F)
            .texOffs(98, 34).addBox(-0.56F, -0.60958F, -6.315F, 0.52F, 0.7072F, 0.4F)
            .texOffs(98, 34).addBox(0.04F, -0.60958F, -6.315F, 0.52F, 0.7072F, 0.4F)
            .texOffs(98, 34).addBox(0.64F, -0.66199F, -6.315F, 0.52F, 0.6816F, 0.4F)
            .texOffs(98, 34).addBox(1.24F, -0.74774F, -6.315F, 0.52F, 0.656F, 0.4F)
            .texOffs(98, 34).addBox(1.84F, -0.86176F, -6.315F, 0.52F, 0.6304F, 0.4F)
            .texOffs(98, 34).addBox(2.44F, -1.00129F, -6.19F, 0.52F, 0.6048F, 0.4F)
            , PartPose.ZERO);
        head.addOrReplaceChild("nose", CubeListBuilder.create()
            .texOffs(34, 66).addBox(-0.625F, -2.67F, -6.7F, 1.25F, 0.62F, 2.1F)
            .texOffs(34, 66).addBox(-0.5F, -3.27F, -6.491F, 1.0F, 0.62F, 1.891F)
            .texOffs(34, 66).addBox(-0.375F, -3.87F, -6.282F, 0.75F, 0.62F, 1.682F)
            , PartPose.ZERO);
        head.addOrReplaceChild("eye_brows", CubeListBuilder.create()
            .texOffs(34, 66).addBox(-3.0F, -3.5F, -5.7F, 0.25F, 0.75F, 0.25F)
            .texOffs(34, 66).addBox(-2.75F, -3.5F, -5.825F, 2.0F, 0.75F, 0.25F)
            .texOffs(34, 66).addBox(-3.75F, -3.5F, -5.575F, 0.5F, 0.5F, 0.25F)
            .texOffs(34, 66).addBox(-3.25F, -3.5F, -5.7F, 0.25F, 0.5F, 0.25F)
            .texOffs(34, 66).addBox(-3.75F, -3.75F, -5.575F, 0.25F, 0.25F, 0.25F)
            .texOffs(34, 66).addBox(0.75F, -3.5F, -5.825F, 2.0F, 0.75F, 0.25F)
            .texOffs(34, 66).addBox(2.75F, -3.5F, -5.7F, 0.25F, 0.75F, 0.25F)
            .texOffs(34, 66).addBox(3.0F, -3.5F, -5.7F, 0.25F, 0.5F, 0.25F)
            .texOffs(34, 66).addBox(3.25F, -3.5F, -5.575F, 0.5F, 0.5F, 0.25F)
            .texOffs(34, 66).addBox(3.5F, -3.75F, -5.575F, 0.25F, 0.25F, 0.25F)
            , PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }
    public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        head.render(pose, vertices, light, overlay, red, green, blue, alpha);
    }
}
