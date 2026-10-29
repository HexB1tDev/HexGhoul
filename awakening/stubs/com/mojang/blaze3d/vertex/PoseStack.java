package com.mojang.blaze3d.vertex;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class PoseStack {
    public void pushPose() { throw null; }
    public void popPose() { throw null; }
    public void translate(double x, double y, double z) { throw null; }
    public void scale(float x, float y, float z) { throw null; }
    public Pose last() { throw null; }

    public static final class Pose {
        public Matrix4f pose() { throw null; }
        public Matrix3f normal() { throw null; }
    }
}
