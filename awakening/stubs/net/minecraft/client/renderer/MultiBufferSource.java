package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;

public interface MultiBufferSource {
    VertexConsumer getBuffer(RenderType type);

    class BufferSource implements MultiBufferSource {
        @Override public VertexConsumer getBuffer(RenderType type) { throw null; }
        public void endBatch(RenderType type) { throw null; }
    }
}
