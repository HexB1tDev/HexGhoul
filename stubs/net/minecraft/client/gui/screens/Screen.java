// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.client.gui.screens;

public abstract class Screen extends net.minecraft.client.gui.components.events.AbstractContainerEventHandler {
    /** width */ public int f_96543_;
    /** height */ public int f_96544_;

    protected Screen(net.minecraft.network.chat.Component title) { throw new UnsupportedOperationException(); }

    /** render */ public void m_88315_(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { throw new UnsupportedOperationException(); }
    /** renderBackground */ public void m_280273_(net.minecraft.client.gui.GuiGraphics graphics) { throw new UnsupportedOperationException(); }
    /** onClose */ public void m_7379_() { throw new UnsupportedOperationException(); }
    /** hasAltDown */ public static boolean m_96639_() { throw new UnsupportedOperationException(); }
    /** isPauseScreen */ public boolean m_7043_() { throw new UnsupportedOperationException(); }
    /** keyPressed (Esc closes) */ @Override public boolean m_7933_(int keyCode, int scanCode, int modifiers) { throw new UnsupportedOperationException(); }
}
