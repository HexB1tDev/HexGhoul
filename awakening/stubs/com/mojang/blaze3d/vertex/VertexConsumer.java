package com.mojang.blaze3d.vertex;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

public interface VertexConsumer {
    VertexConsumer vertex(Matrix4f pose, float x, float y, float z);
    VertexConsumer color(int r, int g, int b, int a);
    VertexConsumer uv(float u, float v);
    VertexConsumer overlayCoords(int overlay);
    VertexConsumer uv2(int light);
    VertexConsumer normal(Matrix3f normal, float x, float y, float z);
    void endVertex();
}
