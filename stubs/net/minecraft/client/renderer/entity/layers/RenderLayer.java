// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.client.renderer.entity.layers;

public abstract class RenderLayer<T extends net.minecraft.world.entity.Entity, M extends net.minecraft.client.model.EntityModel<T>> {
    public RenderLayer(net.minecraft.client.renderer.entity.RenderLayerParent<T, M> parent) {
    }

    /** render(pose, buffers, packedLight, entity, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch) */
    public abstract void m_6494_(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int packedLight, T entity,
                                 float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch);

    /** getParentModel */ public M m_117386_() { throw new UnsupportedOperationException(); }
}
