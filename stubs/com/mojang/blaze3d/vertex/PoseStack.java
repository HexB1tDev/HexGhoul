// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package com.mojang.blaze3d.vertex;

public class PoseStack {
    public PoseStack() {
    }

    /** pushPose */ public void m_85836_() { throw new UnsupportedOperationException(); }
    /** popPose */ public void m_85849_() { throw new UnsupportedOperationException(); }
    /** translate */ public void m_85837_(double x, double y, double z) { throw new UnsupportedOperationException(); }
    /** scale */ public void m_85841_(float x, float y, float z) { throw new UnsupportedOperationException(); }
    /** translate (floats) */ public void m_252880_(float x, float y, float z) { throw new UnsupportedOperationException(); }
    /** mulPose */ public void m_252781_(org.joml.Quaternionf rotation) { throw new UnsupportedOperationException(); }
    /** last */ public Pose m_85850_() { throw new UnsupportedOperationException(); }

    public static final class Pose {
        /** pose */ public org.joml.Matrix4f m_252922_() { throw new UnsupportedOperationException(); }
        /** normal */ public org.joml.Matrix3f m_252943_() { throw new UnsupportedOperationException(); }
    }
}
