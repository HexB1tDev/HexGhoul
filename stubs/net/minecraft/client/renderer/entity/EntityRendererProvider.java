// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.client.renderer.entity;

public interface EntityRendererProvider<T extends net.minecraft.world.entity.Entity> {
    /** create(context) */ EntityRenderer<T> m_174009_(Context context);

    public static class Context {
        /** bakeLayer */ public net.minecraft.client.model.geom.ModelPart m_174023_(net.minecraft.client.model.geom.ModelLayerLocation layer) { throw new UnsupportedOperationException(); }
    }
}
