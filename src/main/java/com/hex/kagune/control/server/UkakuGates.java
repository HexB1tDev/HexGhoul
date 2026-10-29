package com.hex.kagune.control.server;

import com.hex.kagune.control.RinkakuUnlocks;
import com.hex.kagune.data.KaguneProfile;

import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Trampoline targets (bytecode hooks on {@code KaguneServerController}) that keep the Rinkaku path's tendril-only moves
 * off the Ukaku path, as the canon-only rule requires: Ukaku wings never grapple, grab, throw, crush, lift or slam, they
 * never strike on their own (Threat Auto Attack), and no tendril catches Ukaku's falls (ghoul durability still cancels
 * fall damage for every manifested kagune). V (the Lift key) is Ukaku's crystal mode instead, and J (the Slingshot key) his
 * Gatling, and C (the Guard key) does nothing for him. Every other path goes straight to the original.
 *
 * <p>5.0.10: in Rinkaku's first kakuja none of these keys do anything ({@link RinkakuAutonomy}): its legs fight on their own.
 *
 * <p>6.0.4: on Rinkaku's path each move unlocks with mastery ({@link RinkakuUnlocks}), as Ukaku's do: until then the key says when.
 */
public final class UkakuGates {
    /** Ticks between two "wings can't do that" reminders. */
    private static final long NOTICE_TICKS = 60L;

    private UkakuGates() {
    }

    /** J: Rinkaku's grapple; on the Ukaku path the key is the Gatling, held (its trigger comes as {@code C2SUkakuAction.GATLING}). */
    public static void slingshot(ServerPlayer player) {
        // 6.1.0: on Pale's path J is her flesh (C2SPaleAction, timed by her client): the original's grapple never runs for her.
        if (!UkakuServer.isUkaku(player) && !PaleServer.isPale(player) && !GraftServer.isGraft(player) && !RinkakuAutonomy.refuse(player)
            && unlocked(player, "The grapple", RinkakuUnlocks.GRAPPLE)) {
            KaguneServerController.slingshotLegacy(player);
        }
    }

    public static void grabOrThrow(ServerPlayer player, Entity hint) {
        if (!refused(player, "message.hexkagune.ukaku.no_grapple") && !RinkakuAutonomy.refuse(player)
            && unlocked(player, "The grab", RinkakuUnlocks.GRAB)) {
            KaguneServerController.grabOrThrowLegacy(player, hint);
            // 1.0.42: a grab it set out reaches twice as slowly, and can be broken on its way (RinkakuGrab).
            RinkakuGrab.slow(player);
        }
    }

    public static void throwHeld(ServerPlayer player) {
        if (!refused(player, "message.hexkagune.ukaku.no_grapple") && !RinkakuAutonomy.refuse(player)
            && unlocked(player, "The throw", RinkakuUnlocks.GRAB)) {
            KaguneServerController.throwHeldLegacy(player);
        }
    }

    public static void crush(ServerPlayer player) {
        if (!refused(player, "message.hexkagune.ukaku.no_grapple") && !RinkakuAutonomy.refuse(player)
            && unlocked(player, "The crush", RinkakuUnlocks.CRUSH)) {
            KaguneServerController.crushLegacy(player);
        }
    }

    /** V: Rinkaku's tendrils lift what they hold; Ukaku's wings crystallize (the crystal mode, {@link UkakuServer#toggleCrystal}). */
    public static void lift(ServerPlayer player) {
        if (UkakuServer.isUkaku(player)) {
            UkakuServer.toggleCrystal(player);
        } else if (PaleServer.isPale(player) || GraftServer.isGraft(player)) {
            // 6.1.0: V is her kicks (C2SPaleAction, timed by her client); 7.0.0: Graft's V is his own (C2SGraftAction).
            return;
        } else if (!RinkakuAutonomy.refuse(player) && unlocked(player, "The lift", RinkakuUnlocks.LIFT)) {
            KaguneServerController.liftLegacy(player);
        }
    }

    public static void slam(ServerPlayer player) {
        if (PaleServer.isPale(player) || GraftServer.isGraft(player)) {
            return; // 6.2.0: B is her Watch This (C2SPaleAction, from her client); 7.0.0: Graft's B is his own
        }
        if (!refused(player, "message.hexkagune.ukaku.no_grapple") && !RinkakuAutonomy.refuse(player)
            && unlocked(player, "The slam", RinkakuUnlocks.SLAM)) {
            KaguneServerController.slamLegacy(player);
        }
    }

    /** C: Rinkaku's tendril guard; Ukaku has no guard (5.0.13: the Wing Block is gone), so the key does nothing for him. */
    public static void toggleGuard(ServerPlayer player) {
        if (PaleServer.isPale(player) || GraftServer.isGraft(player)) {
            return; // 6.1.0: Pale has no tendril guard; C does nothing on her path (7.0.0: Graft's C is his own guard)
        }
        if (UkakuServer.isUkaku(player)) {
            // 5.0.18: C is the final kakuja's Steel Blade.
            if (KaguneProfile.isActive(player) && player.m_6084_()) {
                UkakuServer.toggleBlade(player);
            }
        } else if (!RinkakuAutonomy.refuse(player)) {
            KaguneServerController.toggleGuardLegacy(player);
        }
    }

    public static void toggleThreatAutoAttack(ServerPlayer player) {
        if (!refused(player, "message.hexkagune.ukaku.no_threat_auto")) {
            KaguneServerController.toggleThreatAutoAttackLegacy(player);
        }
    }

    /**
     * Tendrils catching a long fall: never on the Ukaku path. 6.0.5: nor while the kakuja's vanish hides him (the catch plays the
     * original's landing burst where he lands; the manifested kagune takes no fall damage anyway).
     */
    public static boolean tryStartFallArrest(ServerPlayer player, KaguneRuntime.State state, long now) {
        // 6.1.0: nor on Pale's path: her flesh never comes out to catch her (she lands her flips on her feet).
        return !UkakuServer.isUkaku(player) && !PaleServer.isPale(player) && !GraftServer.isGraft(player) && !KakujaPassive.vanished(player)
            && KaguneServerController.tryStartFallArrestLegacy(player, state, now);
    }

    /**
     * 6.0.4: true once Rinkaku's mastery has reached {@code needs} for this move; until then the action bar says when it unlocks
     * ({@code name} unlocks at mastery {@code needs}).
     */
    static boolean unlocked(ServerPlayer player, String name, int needs) {
        if (RinkakuUnlocks.unlocked(KaguneProfile.mastery(player), needs)) {
            return true;
        }
        UkakuServer.locked(player, name, needs);
        return false;
    }

    /** True for an Ukaku player, with a short action-bar reminder at most every few seconds (6.1.0: and for Pale). */
    private static boolean refused(ServerPlayer player, String message) {
        if (GraftServer.isGraft(player)) {
            return true; // 7.0.0: his keys are his own moves (C2SGraftAction); the original's grab, throw, crush never run for him
        }
        if (PaleServer.isPale(player)) {
            PaleServer.notice(player, PaleServer.state(player), Component.m_237115_("message.hexkagune.pale.no_tendrils"));
            return true;
        }
        if (!UkakuServer.isUkaku(player)) {
            return false;
        }
        UkakuServer.UkakuState state = UkakuServer.state(player);
        long now = player.m_9236_().m_46467_();
        if (now >= state.nextNoticeAt || now < state.nextNoticeAt - NOTICE_TICKS) {
            state.nextNoticeAt = now + NOTICE_TICKS;
            player.m_5661_(Component.m_237115_(message), true);
        }
        return true;
    }
}
