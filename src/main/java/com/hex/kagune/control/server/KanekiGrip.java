package com.hex.kagune.control.server;

import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.gameplay.TendrilMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Kaneki's crush lets go (3.0.3). The original crush squeezes every body his tails hold, then went back to holding them, so the
 * same body could be crushed again and again. Now each crush is once: when it ends, everything it crushed is released (the
 * original's own release: AI, gravity and motion restored, the slot freed) and can be grabbed again at once. A slam already
 * lets go when it ends. The tails held by the autonomous Centipede legs keep what they hold.
 */
public final class KanekiGrip {
    private KanekiGrip() {
    }

    /**
     * Replaces the end of the original crush ({@code state.begin(HOLD, now, 0, targetId, target)} in
     * {@code KaguneServerController.tickCrush}): release what was crushed, then hold whatever is left or retract.
     */
    public static void crushDone(KaguneRuntime.State state, TendrilMode mode, long now, int duration, int targetId, Vec3 target, ServerPlayer player) {
        ServerLevel level = player.m_284548_();
        for (int slot = 0; slot < state.heldEntityIds.length; slot++) {
            if (state.heldEntityIds[slot] != 0 && !state.autonomousCentipedeHeld[slot]) {
                KaguneServerController.releaseHeldSlot(level, state, slot);
            }
        }
        KaguneServerController.returnToHoldingOrRetract(player, state, now);
    }
}
