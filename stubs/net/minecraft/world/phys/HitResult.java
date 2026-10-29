// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.phys;

public abstract class HitResult {
    /** getLocation */ public Vec3 m_82450_() { throw new UnsupportedOperationException(); }
    /** getType */ public abstract Type m_6662_();

    public enum Type {
        MISS, BLOCK, ENTITY
    }
}
