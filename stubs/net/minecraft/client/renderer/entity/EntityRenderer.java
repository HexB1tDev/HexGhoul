// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.client.renderer.entity;

public abstract class EntityRenderer<T extends net.minecraft.world.entity.Entity> {
    protected EntityRenderer() {
    }

    protected EntityRenderer(EntityRendererProvider.Context context) {
    }

    /** render(entity, yaw, partialTick, pose, buffers, light) (the original's spike renderer overrides and calls it) */ public void m_7392_(T entity,
        float yaw, float partialTicks, com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        throw new UnsupportedOperationException();
    }

    /** getTextureLocation */ public net.minecraft.resources.ResourceLocation m_5478_(T entity) { throw new UnsupportedOperationException(); }
}
