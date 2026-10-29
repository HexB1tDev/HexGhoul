package com.hex.kagune.control.server;

import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;
import net.minecraft.server.level.ServerPlayer;

/**
 * 6.2.0: any move of hers can be thrown at any moment, mid-strike, mid-flip, mid-kick, in the air: the move it cuts off is ended
 * cleanly first ({@link #cut}). A move asks {@link #allowed} instead of waiting for her to be free, checks everything of its own
 * (its cooldown, her mastery, her RC), and only then cuts what was going on, so a key that can't start anything never stops what
 * she is doing. What was cut never comes back: its blow lands no more (nothing is hit twice), what it held is let go, the arm draws
 * back in, a cooldown it had charged stays charged (and one it would only have charged at its end is charged now), and a fall from
 * a cut flip or kick still never hurts her. Every client draws the new move out of whatever she was drawn in.
 */
final class PaleCancel {
    private PaleCancel() {
    }

    /** Whether a new move of her base form may start now (never in the Owl, nor while it grows or goes: its moves are its own). */
    static boolean allowed(PaleServer.PaleState state, long now) {
        // (6.2.0: Witness This is never cut off by a stray key: it plays to its end.)
        return !PaleOwl.busy(state, now) && state.move != PaleMove.WITNESS;
    }

    /** 6.2.0: whether one of the Owl's own moves may start now: in it, all grown out (never while it grows or goes). */
    static boolean allowedOwl(PaleServer.PaleState state, long now) {
        return PaleOwl.ready(state, now);
    }

    /** Whatever she is doing is cut off cleanly, for a new move about to begin (its own begin tells every client). */
    static void cut(ServerPlayer player, PaleServer.PaleState state, long now) {
        if (state.armOut) {
            PaleArm.cut(player, state, now);
        }
        switch (state.move) {
            case NONE -> {
                return;
            }
            case STRIKE -> {
                // Its blow has landed or never will; a click kept for it is dropped.
                state.queuedAt = Long.MIN_VALUE;
            }
            case FLIP -> {
                state.push = null;
                state.pushTicks = 0;
                state.soarTick = PaleRules.SOAR_RAMP;
            }
            case GRAB_REACH, GRAB_HOLD -> {
                // Never thrown: let go where it is; the grab's cooldown is charged now (a cut is no way round it).
                PaleFlesh.release(player, state);
                state.grabReadyAt = Math.max(state.grabReadyAt, now + Cooldowns.of(player, PaleRules.GRAB_COOLDOWN));
            }
            case GRAB_THROW -> PaleFlesh.release(player, state); // let go mid-haul: it flies on as it was going
            case HAMMER_CHARGE -> state.hammerLetGo = false; // never let go: no slam, the club drawn back in
            case OWL_LEAP -> {
                // 6.2.0: cut mid-bound: the launch's push no more (no slam: it never lands as a pounce).
                state.push = null;
                state.pushTicks = 0;
            }
            case OWL_VOLLEY -> {
                // 6.2.0: cut mid-volley: no more spikes (the ones out fly on); its cooldown (6.2.4: J's) is charged now.
                state.grabReadyAt = Math.max(state.grabReadyAt, now + Cooldowns.of(player, com.hex.kagune.control.pale.ArmRules.COOLDOWN));
            }
            case OWL_GRAB_REACH, OWL_GRAB_HOLD, OWL_GRAB_SLAM -> {
                // 6.2.4: the great hand cut off: let go where it is, its cooldown charged now.
                PaleFlesh.release(player, state);
                state.armReadyAt = Math.max(state.armReadyAt, now + Cooldowns.of(player, com.hex.kagune.control.pale.OwlRules.GRAB_COOLDOWN));
            }
            default -> {
                // The punch, the kick, the hammer, the swipe, a missed grab, Watch This: their cooldowns were charged as they began,
                // their blows are done or never come, a pinned body is free again in a moment.
            }
        }
        if (!player.m_20096_()) {
            state.fallSafe = true; // cut off in the air: the fall still never hurts her
        }
        state.lastStrike = null;
        state.hit.clear();
        state.move = PaleMove.NONE;
        state.detail = 0;
        state.targetId = 0;
        state.charge = 0.0F;
    }
}
