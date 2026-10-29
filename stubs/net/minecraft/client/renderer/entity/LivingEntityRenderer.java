// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.client.renderer.entity;

public abstract class LivingEntityRenderer<T extends net.minecraft.world.entity.LivingEntity, M extends net.minecraft.client.model.EntityModel<T>>
    extends EntityRenderer<T> implements RenderLayerParent<T, M> {
    /** model (1.0.41: a ghoul NPC's, wide or slim, chosen as it is drawn) */ protected M f_115290_;
    /** getModel (declared by RenderLayerParent) */ public M m_7200_() { throw new UnsupportedOperationException(); }
    /** render (1.0.13: a body cut in half, drawn twice) */ public void m_7392_(T entity, float yaw, float partialTicks, com.mojang.blaze3d.vertex.PoseStack pose,
        net.minecraft.client.renderer.MultiBufferSource buffers, int light) { throw new UnsupportedOperationException(); }
    /** addLayer */ public final boolean m_115326_(net.minecraft.client.renderer.entity.layers.RenderLayer<T, M> layer) { throw new UnsupportedOperationException(); }
}
