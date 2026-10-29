// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.phys;

public class AABB {
    // Blank finals, never initialised to constants: a constant would be inlined into the expansion's bytecode (it was, in 1.3.0,
    // and AyatoCrystal's hit point clamped to 0.0 instead of the hitbox).
    /** minX */ public final double f_82288_;
    /** minY */ public final double f_82289_;
    /** minZ */ public final double f_82290_;
    /** maxX */ public final double f_82291_;
    /** maxY */ public final double f_82292_;
    /** maxZ */ public final double f_82293_;
    public AABB(double x1, double y1, double z1, double x2, double y2, double z2) { throw new UnsupportedOperationException(); }
    /** move */ public AABB m_82386_(double x, double y, double z) { throw new UnsupportedOperationException(); }
    /** contains */ public boolean m_82390_(Vec3 point) { throw new UnsupportedOperationException(); }
    /** getCenter */ public Vec3 m_82399_() { throw new UnsupportedOperationException(); }
    /** inflate */ public AABB m_82400_(double amount) { throw new UnsupportedOperationException(); }
    /** expandTowards(v) */ public AABB m_82369_(Vec3 v) { throw new UnsupportedOperationException(); }
    /** move(pos) */ public AABB m_82338_(net.minecraft.core.BlockPos pos) { throw new UnsupportedOperationException(); }
    /** intersects(other) */ public boolean m_82381_(AABB other) { throw new UnsupportedOperationException(); }
}
