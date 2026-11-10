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
public final class ShachiMaskModel {
    public final ModelPart head;
    public ShachiMaskModel(ModelPart root) { this.head = root.getChild("head"); }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("shell", CubeListBuilder.create()
            .texOffs(130, 2).addBox(-3.25F, 0.65F, -5.375F, 0.5F, 0.75F, 0.775F)
            .texOffs(130, 2).addBox(-2.75F, 0.15F, -5.5F, 5.5F, 1.25F, 0.9F)
            .texOffs(130, 2).addBox(2.75F, 0.65F, -5.375F, 0.5F, 0.75F, 0.775F)
            .texOffs(130, 2).addBox(-3.5F, 0.65F, -5.25F, 0.25F, 0.5F, 0.65F)
            .texOffs(130, 2).addBox(3.25F, 0.65F, -5.25F, 0.25F, 0.5F, 0.65F)
            .texOffs(130, 2).addBox(-3.75F, 0.15F, -5.25F, 0.25F, 0.75F, 0.65F)
            .texOffs(130, 2).addBox(3.5F, 0.15F, -5.25F, 0.25F, 0.75F, 0.65F)
            .texOffs(130, 2).addBox(-3.5F, 0.15F, -5.375F, 0.5F, 0.5F, 0.775F)
            .texOffs(130, 2).addBox(-3.0F, -0.85F, -5.5F, 0.25F, 1.5F, 0.9F)
            .texOffs(130, 2).addBox(2.75F, -0.85F, -5.5F, 0.25F, 1.5F, 0.9F)
            .texOffs(130, 2).addBox(3.0F, 0.15F, -5.375F, 0.5F, 0.5F, 0.775F)
            .texOffs(130, 2).addBox(-4.0F, -0.85F, -5.25F, 0.25F, 1.25F, 0.65F)
            .texOffs(130, 2).addBox(3.75F, -0.85F, -5.25F, 0.25F, 1.25F, 0.65F)
            .texOffs(130, 2).addBox(-4.25F, -0.35F, -5.125F, 0.25F, 0.5F, 0.525F)
            .texOffs(130, 2).addBox(-3.75F, -0.85F, -5.375F, 0.5F, 1.0F, 0.775F)
            .texOffs(130, 2).addBox(-3.25F, -0.85F, -5.5F, 0.25F, 1.0F, 0.9F)
            .texOffs(130, 2).addBox(-2.75F, -0.1F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(130, 2).addBox(-2.5F, -0.85F, -5.625F, 5.0F, 1.0F, 1.025F)
            .texOffs(130, 2).addBox(2.5F, -0.1F, -5.5F, 0.25F, 0.25F, 0.9F)
            .texOffs(130, 2).addBox(3.0F, -0.85F, -5.5F, 0.25F, 1.0F, 0.9F)
            .texOffs(130, 2).addBox(3.25F, -0.85F, -5.375F, 0.5F, 1.0F, 0.775F)
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
            .texOffs(226, 2).addBox(-3.5F, 0.65F, -5.4F, 0.25F, 0.5F, 0.2F)
            .texOffs(226, 2).addBox(-3.25F, 0.65F, -5.525F, 0.5F, 0.5F, 0.2F)
            .texOffs(226, 2).addBox(-2.75F, 0.15F, -5.65F, 5.5F, 1.0F, 0.2F)
            .texOffs(226, 2).addBox(2.75F, 0.65F, -5.525F, 0.5F, 0.5F, 0.2F)
            .texOffs(226, 2).addBox(3.25F, 0.65F, -5.4F, 0.25F, 0.5F, 0.2F)
            .texOffs(226, 2).addBox(-3.5F, 0.15F, -5.525F, 0.5F, 0.5F, 0.2F)
            .texOffs(226, 2).addBox(-3.0F, -0.85F, -5.65F, 0.25F, 1.5F, 0.2F)
            .texOffs(226, 2).addBox(2.75F, -0.85F, -5.65F, 0.25F, 1.5F, 0.2F)
            .texOffs(226, 2).addBox(3.0F, 0.15F, -5.525F, 0.5F, 0.5F, 0.2F)
            .texOffs(226, 2).addBox(-3.5F, -0.85F, -5.525F, 0.25F, 1.0F, 0.2F)
            .texOffs(226, 2).addBox(-3.25F, -0.85F, -5.65F, 0.25F, 1.0F, 0.2F)
            .texOffs(226, 2).addBox(-2.75F, -0.1F, -5.65F, 0.25F, 0.25F, 0.2F)
            .texOffs(226, 2).addBox(-2.5F, -0.85F, -5.775F, 5.0F, 1.0F, 0.2F)
            .texOffs(226, 2).addBox(2.5F, -0.1F, -5.65F, 0.25F, 0.25F, 0.2F)
            .texOffs(226, 2).addBox(3.0F, -0.85F, -5.65F, 0.25F, 1.0F, 0.2F)
            .texOffs(226, 2).addBox(3.25F, -0.85F, -5.525F, 0.25F, 1.0F, 0.2F)
            .texOffs(226, 2).addBox(-2.75F, -0.85F, -5.775F, 0.25F, 0.75F, 0.2F)
            .texOffs(226, 2).addBox(2.5F, -0.85F, -5.775F, 0.25F, 0.75F, 0.2F)
            , PartPose.ZERO);
        head.addOrReplaceChild("upper_fangs", CubeListBuilder.create()
            .texOffs(98, 34).addBox(-2.875F, 0.65F, -5.925F, 0.125F, 0.125F, 0.48F)
            .texOffs(98, 34).addBox(-2.875F, -0.85F, -6.05F, 0.125F, 1.5F, 0.48F)
            .texOffs(98, 34).addBox(-3.0F, -0.85F, -6.05F, 0.125F, 1.25F, 0.48F)
            .texOffs(98, 34).addBox(-2.75F, -0.35F, -6.05F, 0.125F, 0.75F, 0.48F)
            .texOffs(98, 34).addBox(-2.625F, -0.1F, -6.05F, 0.125F, 0.125F, 0.48F)
            .texOffs(98, 34).addBox(-2.625F, -0.85F, -6.175F, 0.125F, 0.75F, 0.48F)
            .texOffs(98, 34).addBox(-2.75F, -0.85F, -6.175F, 0.125F, 0.5F, 0.48F)
            .texOffs(98, 34).addBox(-3.125F, -0.85F, -6.05F, 0.125F, 0.375F, 0.48F)
            .texOffs(98, 34).addBox(-1.25F, 0.275F, -6.05F, 0.125F, 0.375F, 0.48F)
            .texOffs(98, 34).addBox(-1.125F, 0.275F, -6.05F, 0.125F, 0.25F, 0.48F)
            .texOffs(98, 34).addBox(-1.375F, -0.85F, -6.175F, 0.375F, 1.125F, 0.48F)
            .texOffs(98, 34).addBox(-1.0F, -0.85F, -6.175F, 0.125F, 1.0F, 0.48F)
            .texOffs(98, 34).addBox(1.125F, 0.275F, -6.05F, 0.125F, 0.375F, 0.48F)
            .texOffs(98, 34).addBox(1.0F, 0.275F, -6.05F, 0.125F, 0.25F, 0.48F)
            .texOffs(98, 34).addBox(1.0F, -0.85F, -6.175F, 0.375F, 1.125F, 0.48F)
            .texOffs(98, 34).addBox(0.875F, -0.85F, -6.175F, 0.125F, 1.0F, 0.48F)
            .texOffs(98, 34).addBox(2.75F, 0.65F, -5.925F, 0.125F, 0.125F, 0.48F)
            .texOffs(98, 34).addBox(2.75F, -0.85F, -6.05F, 0.125F, 1.5F, 0.48F)
            .texOffs(98, 34).addBox(2.625F, -0.35F, -6.05F, 0.125F, 0.75F, 0.48F)
            .texOffs(98, 34).addBox(2.875F, -0.85F, -6.05F, 0.125F, 1.25F, 0.48F)
            .texOffs(98, 34).addBox(2.5F, -0.1F, -6.05F, 0.125F, 0.125F, 0.48F)
            .texOffs(98, 34).addBox(2.5F, -0.85F, -6.175F, 0.125F, 0.75F, 0.48F)
            .texOffs(98, 34).addBox(2.625F, -0.85F, -6.175F, 0.125F, 0.5F, 0.48F)
            .texOffs(98, 34).addBox(3.0F, -0.85F, -6.05F, 0.125F, 0.375F, 0.48F)
            , PartPose.ZERO);
        head.addOrReplaceChild("lower_fangs", CubeListBuilder.create()
            .texOffs(98, 34).addBox(-3.0F, 0.65F, -5.855F, 0.25F, 0.5F, 0.45F)
            .texOffs(98, 34).addBox(-2.75F, 0.4F, -5.98F, 0.125F, 0.75F, 0.45F)
            .texOffs(98, 34).addBox(-3.0F, 0.525F, -5.855F, 0.125F, 0.125F, 0.45F)
            .texOffs(98, 34).addBox(-2.875F, 0.025F, -5.98F, 0.125F, 0.625F, 0.45F)
            .texOffs(98, 34).addBox(-1.375F, 0.65F, -5.98F, 0.375F, 0.5F, 0.45F)
            .texOffs(98, 34).addBox(-1.25F, 0.275F, -5.98F, 0.25F, 0.375F, 0.45F)
            .texOffs(98, 34).addBox(-1.25F, 0.15F, -6.105F, 0.125F, 0.125F, 0.45F)
            .texOffs(98, 34).addBox(1.0F, 0.65F, -5.98F, 0.375F, 0.5F, 0.45F)
            .texOffs(98, 34).addBox(1.0F, 0.275F, -5.98F, 0.25F, 0.375F, 0.45F)
            .texOffs(98, 34).addBox(1.125F, 0.15F, -6.105F, 0.125F, 0.125F, 0.45F)
            .texOffs(98, 34).addBox(2.625F, 0.4F, -5.98F, 0.125F, 0.75F, 0.45F)
            .texOffs(98, 34).addBox(2.75F, 0.65F, -5.855F, 0.25F, 0.5F, 0.45F)
            .texOffs(98, 34).addBox(2.75F, 0.025F, -5.98F, 0.125F, 0.625F, 0.45F)
            .texOffs(98, 34).addBox(2.875F, 0.525F, -5.855F, 0.125F, 0.125F, 0.45F)
            , PartPose.ZERO);
        head.addOrReplaceChild("nose", CubeListBuilder.create()
            .texOffs(162, 2).addBox(-0.55F, -1.10333F, -6.35F, 1.1F, 0.35333F, 1.75F)
            .texOffs(162, 2).addBox(-0.44F, -1.43666F, -6.218F, 0.88F, 0.35333F, 1.618F)
            .texOffs(162, 2).addBox(-0.33F, -1.77F, -6.086F, 0.66F, 0.35333F, 1.486F)
            , PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }
    public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        head.render(pose, vertices, light, overlay, red, green, blue, alpha);
    }
}
