// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.client.gui;

public class GuiGraphics {
    /** pose */ public com.mojang.blaze3d.vertex.PoseStack m_280168_() { throw new UnsupportedOperationException(); }
    /** drawString(font, text, x, y, color, shadow) */ public int m_280056_(Font font, String text, int x, int y, int color, boolean shadow) { throw new UnsupportedOperationException(); }
    /** drawCenteredString(font, text, centerX, y, color) */ public void m_280137_(Font font, String text, int centerX, int y, int color) { throw new UnsupportedOperationException(); }
    /** drawManaged: batches every draw inside into one flush */ public void m_286007_(Runnable draws) { throw new UnsupportedOperationException(); }
    /** fill */ public void m_280509_(int x1, int y1, int x2, int y2, int color) { throw new UnsupportedOperationException(); }
    /** fillGradient (vertical) */ public void m_280024_(int x1, int y1, int x2, int y2, int colorFrom, int colorTo) { throw new UnsupportedOperationException(); }
    /** blit(texture, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight) */
    public void m_280411_(net.minecraft.resources.ResourceLocation texture, int x, int y, int width, int height, float u, float v,
        int uWidth, int vHeight, int textureWidth, int textureHeight) { throw new UnsupportedOperationException(); }
}
