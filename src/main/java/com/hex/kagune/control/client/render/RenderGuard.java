package com.hex.kagune.control.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 4.1.103 (the owner: "making sure absolutely nothing in the mod fails"): an NPC's renderer that throws (the game wraps it in a crash: "Rendering
 * entity in world") is skipped for that frame instead: the pose stack put back where the renderer found it (what it pushed and never
 * popped, popped), the failure told once in the log (PlayerLayers.failed), and the world drawn on. Each of the mod's NPC renderers wraps its
 * render in this ({@code mark} taken as it begins: {@code pose.last()}).
 */
public final class RenderGuard {
    /** Deeper than this is never pushed by one renderer: the stack put back no further (a broken one can't loop for ever). */
    static final int MOST_POPS = 64;

    private RenderGuard() {
    }

    /** {@code what}'s renderer threw {@code failure}: the pose stack back to {@code mark}, the failure told. */
    public static void failed(PoseStack pose, PoseStack.Pose mark, String what, RuntimeException failure) {
        try {
            for (int i = 0; i < MOST_POPS && pose.m_85850_() != mark; i++) {
                pose.m_85849_();
            }
        } catch (RuntimeException emptied) {
            // popped past its bottom: nothing more to put back
        }
        PlayerLayers.failed(what + " (its renderer)", failure);
    }
}
