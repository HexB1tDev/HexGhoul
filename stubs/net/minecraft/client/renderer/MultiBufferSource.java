// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.client.renderer;

public interface MultiBufferSource {
    /** getBuffer */ com.mojang.blaze3d.vertex.VertexConsumer m_6299_(RenderType type);

    class BufferSource implements MultiBufferSource {
        /** getBuffer */ public com.mojang.blaze3d.vertex.VertexConsumer m_6299_(RenderType type) { throw new UnsupportedOperationException(); }
        /** endBatch(type) */ public void m_109912_(RenderType type) { throw new UnsupportedOperationException(); }
    }
}
