package com.hex.kagune.control.server;

import com.hex.kagune.control.KanekiUnlocks;
import com.hex.kagune.data.KaguneProfile;

import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Trampoline targets (bytecode hooks on {@code KaguneServerController}) that keep the Kaneki path's tendril-only moves
 * off the Ayato path, as the canon-only rule requires: Ukaku wings never grapple, grab, throw, crush, lift or slam, they
 * never strike on their own (Threat Auto Attack), and no tendril catches Ayato's falls (ghoul durability still cancels
 * fall damage for every manifested kagune). V (the Lift key) is Ayato's crystal mode instead, and J (the Slingshot key) his
 * Gatling, and C (the Guard key) does nothing for him. Every other path goes straight to the original.
 *
 * <p>5.0.10: in Kaneki's first kakuja none of these keys do anything ({@link KanekiAutonomy}): its legs fight on their own.
 *
 * <p>6.0.4: on Kaneki's path each move unlocks with mastery ({@link KanekiUnlocks}), as Ayato's do: until then the key says when.
 */
public final class AyatoGates {
    /** Ticks between two "wings can't do that" reminders. */
    private static final long NOTICE_TICKS = 60L;

    private AyatoGates() {
    }

    /** J: Kaneki's grapple; on the Ayato path the key is the Gatling, held (its trigger comes as {@code C2SAyatoAction.GATLING}). */
    public static void slingshot(ServerPlayer player) {
        // 6.1.0: on Eto's path J is her flesh (C2SEtoAction, timed by her client): the original's grapple never runs for her.
        if (!AyatoServer.isAyato(player) && !EtoServer.isEto(player) && !FurutaServer.isFuruta(player) && !KanekiAutonomy.refuse(player)
            && unlocked(player, "The grapple", KanekiUnlocks.GRAPPLE)) {
            KaguneServerController.slingshotLegacy(player);
        }
    }

    public static void grabOrThrow(ServerPlayer player, Entity hint) {
        if (!refused(player, "message.hexkagune.ayato.no_grapple") && !KanekiAutonomy.refuse(player)
            && unlocked(player, "The grab", KanekiUnlocks.GRAB)) {
            KaguneServerController.grabOrThrowLegacy(player, hint);
            // 1.0.42: a grab it set out reaches twice as slowly, and can be broken on its way (KanekiGrab).
            KanekiGrab.slow(player);
        }
    }

    public static void throwHeld(ServerPlayer player) {
        if (!refused(player, "message.hexkagune.ayato.no_grapple") && !KanekiAutonomy.refuse(player)
            && unlocked(player, "The throw", KanekiUnlocks.GRAB)) {
            KaguneServerController.throwHeldLegacy(player);
        }
    }

    public static void crush(ServerPlayer player) {
        if (!refused(player, "message.hexkagune.ayato.no_grapple") && !KanekiAutonomy.refuse(player)
            && unlocked(player, "The crush", KanekiUnlocks.CRUSH)) {
            KaguneServerController.crushLegacy(player);
        }
    }

    /** V: Kaneki's tendrils lift what they hold; Ayato's wings crystallize (the crystal mode, {@link AyatoServer#toggleCrystal}). */
    public static void lift(ServerPlayer player) {
        if (AyatoServer.isAyato(player)) {
            AyatoServer.toggleCrystal(player);
        } else if (EtoServer.isEto(player) || FurutaServer.isFuruta(player)) {
            // 6.1.0: V is her kicks (C2SEtoAction, timed by her client); 7.0.0: Furuta's V is his own (C2SFurutaAction).
            return;
        } else if (!KanekiAutonomy.refuse(player) && unlocked(player, "The lift", KanekiUnlocks.LIFT)) {
            KaguneServerController.liftLegacy(player);
        }
    }

    public static void slam(ServerPlayer player) {
        if (EtoServer.isEto(player) || FurutaServer.isFuruta(player)) {
            return; // 6.2.0: B is her Watch This (C2SEtoAction, from her client); 7.0.0: Furuta's B is his own
        }
        if (!refused(player, "message.hexkagune.ayato.no_grapple") && !KanekiAutonomy.refuse(player)
            && unlocked(player, "The slam", KanekiUnlocks.SLAM)) {
            KaguneServerController.slamLegacy(player);
        }
    }

    /** C: Kaneki's tendril guard; Ayato has no guard (5.0.13: the Wing Block is gone), so the key does nothing for him. */
    public static void toggleGuard(ServerPlayer player) {
        if (EtoServer.isEto(player) || FurutaServer.isFuruta(player)) {
            return; // 6.1.0: Eto has no tendril guard; C does nothing on her path (7.0.0: Furuta's C is his own guard)
        }
        if (AyatoServer.isAyato(player)) {
            // 5.0.18: C is the final kakuja's Steel Blade.
            if (KaguneProfile.isActive(player) && player.m_6084_()) {
                AyatoServer.toggleBlade(player);
            }
        } else if (!KanekiAutonomy.refuse(player)) {
            KaguneServerController.toggleGuardLegacy(player);
        }
    }

    public static void toggleThreatAutoAttack(ServerPlayer player) {
        if (!refused(player, "message.hexkagune.ayato.no_threat_auto")) {
            KaguneServerController.toggleThreatAutoAttackLegacy(player);
        }
    }

    /**
     * Tendrils catching a long fall: never on the Ayato path. 6.0.5: nor while the kakuja's vanish hides him (the catch plays the
     * original's landing burst where he lands; the manifested kagune takes no fall damage anyway).
     */
    public static boolean tryStartFallArrest(ServerPlayer player, KaguneRuntime.State state, long now) {
        // 6.1.0: nor on Eto's path: her flesh never comes out to catch her (she lands her flips on her feet).
        return !AyatoServer.isAyato(player) && !EtoServer.isEto(player) && !FurutaServer.isFuruta(player) && !KakujaPassive.vanished(player)
            && KaguneServerController.tryStartFallArrestLegacy(player, state, now);
    }

    /**
     * 6.0.4: true once Kaneki's mastery has reached {@code needs} for this move; until then the action bar says when it unlocks
     * ({@code name} unlocks at mastery {@code needs}).
     */
    static boolean unlocked(ServerPlayer player, String name, int needs) {
        if (KanekiUnlocks.unlocked(KaguneProfile.mastery(player), needs)) {
            return true;
        }
        AyatoServer.locked(player, name, needs);
        return false;
    }

    /** True for an Ayato player, with a short action-bar reminder at most every few seconds (6.1.0: and for Eto). */
    private static boolean refused(ServerPlayer player, String message) {
        if (FurutaServer.isFuruta(player)) {
            return true; // 7.0.0: his keys are his own moves (C2SFurutaAction); the original's grab, throw, crush never run for him
        }
        if (EtoServer.isEto(player)) {
            EtoServer.notice(player, EtoServer.state(player), Component.m_237115_("message.hexkagune.eto.no_tendrils"));
            return true;
        }
        if (!AyatoServer.isAyato(player)) {
            return false;
        }
        AyatoServer.AyatoState state = AyatoServer.state(player);
        long now = player.m_9236_().m_46467_();
        if (now >= state.nextNoticeAt || now < state.nextNoticeAt - NOTICE_TICKS) {
            state.nextNoticeAt = now + NOTICE_TICKS;
            player.m_5661_(Component.m_237115_(message), true);
        }
        return true;
    }
}
