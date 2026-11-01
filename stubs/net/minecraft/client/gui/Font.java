// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.client.gui;

public class Font {
    /** width(String) */ public int m_92895_(String text) { throw new UnsupportedOperationException(); }
    /** 1.3.38: drawInBatch(String, x, y, colour, shadow, matrix, buffers, mode, background, light) */
    public int m_271703_(String text, float x, float y, int colour, boolean shadow, org.joml.Matrix4f matrix,
                         net.minecraft.client.renderer.MultiBufferSource buffers, DisplayMode mode, int background, int light) {
        throw new UnsupportedOperationException();
    }

    /** 1.3.38: Font.DisplayMode (its constants by their SRG field names). */
    public static final class DisplayMode {
        /** NORMAL */ public static final DisplayMode f_181350_ = null;
    }
}
