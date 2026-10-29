// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.nbt;

public class CompoundTag implements Tag {
    /** hasUUID (7.0.2; the original's) */ public boolean m_128403_(String key) { throw new UnsupportedOperationException(); }
    /** getUUID (7.0.2; the original's) */ public java.util.UUID m_128342_(String key) { throw new UnsupportedOperationException(); }
    /** putUUID (7.0.2; the original's) */ public void m_128362_(String key, java.util.UUID value) { throw new UnsupportedOperationException(); }
    public CompoundTag() {
    }

    /** contains(key) */ public boolean m_128441_(String key) { throw new UnsupportedOperationException(); }
    /** remove(key) */ public void m_128473_(String key) { throw new UnsupportedOperationException(); }
    /** getString */ public String m_128461_(String key) { throw new UnsupportedOperationException(); }
    /** putString */ public void m_128359_(String key, String value) { throw new UnsupportedOperationException(); }
    /** getInt */ public int m_128451_(String key) { throw new UnsupportedOperationException(); }
    /** putInt */ public void m_128405_(String key, int value) { throw new UnsupportedOperationException(); }
    /** getLong */ public long m_128454_(String key) { throw new UnsupportedOperationException(); }
    /** putLong */ public void m_128356_(String key, long value) { throw new UnsupportedOperationException(); }
    /** getBoolean */ public boolean m_128471_(String key) { throw new UnsupportedOperationException(); }
    /** putBoolean */ public void m_128379_(String key, boolean value) { throw new UnsupportedOperationException(); }
    /** getCompound (a new empty tag, not attached, when missing) */ public CompoundTag m_128469_(String key) { throw new UnsupportedOperationException(); }
    /** getList(key, type) */ public ListTag m_128437_(String key, int type) { throw new UnsupportedOperationException(); }
    /** put */ public Tag m_128365_(String key, Tag value) { throw new UnsupportedOperationException(); }
}
