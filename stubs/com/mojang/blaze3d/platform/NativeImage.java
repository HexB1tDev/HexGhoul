// Compile-only API stub (Minecraft 1.20.1 / Forge 47). Never packaged; see verify/api-allowlist.txt.
package com.mojang.blaze3d.platform;

public final class NativeImage implements AutoCloseable {
    /** NativeImage(width, height, useCalloc), RGBA */ public NativeImage(int width, int height, boolean useCalloc) { throw new UnsupportedOperationException(); }
    /** downloadTexture(level, opaque): reads the bound texture back */ public void m_85045_(int level, boolean opaque) { throw new UnsupportedOperationException(); }
    /** getPixelRGBA (packed ABGR) */ public int m_84985_(int x, int y) { throw new UnsupportedOperationException(); }
    /** setPixelRGBA (packed ABGR) */ public void m_84988_(int x, int y, int abgr) { throw new UnsupportedOperationException(); }
    @Override public void close() { throw new UnsupportedOperationException(); }
}
