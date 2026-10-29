// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.client.gui.components.events;

public interface GuiEventListener {
    /** mouseClicked */ default boolean m_6375_(double mouseX, double mouseY, int button) { throw new UnsupportedOperationException(); }
    /** mouseReleased */ default boolean m_6348_(double mouseX, double mouseY, int button) { throw new UnsupportedOperationException(); }
    /** keyPressed */ default boolean m_7933_(int keyCode, int scanCode, int modifiers) { throw new UnsupportedOperationException(); }
    /** keyReleased */ default boolean m_7920_(int keyCode, int scanCode, int modifiers) { throw new UnsupportedOperationException(); }
}
