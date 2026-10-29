// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package com.mojang.blaze3d.vertex;

public interface VertexConsumer {
    /** vertex(pose, x, y, z) */ VertexConsumer m_252986_(org.joml.Matrix4f pose, float x, float y, float z);
    /** color(r, g, b, a) */ VertexConsumer m_6122_(int red, int green, int blue, int alpha);
    /** uv */ VertexConsumer m_7421_(float u, float v);
    /** overlayCoords */ VertexConsumer m_86008_(int overlay);
    /** uv2 (light) */ VertexConsumer m_85969_(int light);
    /** normal(matrix, x, y, z) */ VertexConsumer m_252939_(org.joml.Matrix3f normal, float x, float y, float z);
    /** vertex(x, y, z) */ VertexConsumer m_5483_(double x, double y, double z);
    /** endVertex */ void m_5752_();
}
