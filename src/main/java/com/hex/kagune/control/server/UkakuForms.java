package com.hex.kagune.control.server;

import com.hex.kagune.control.KakujaBurst;
import com.hex.kagune.control.RinkakuForms;
import com.hex.kagune.control.ukaku.WingForm;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.TendrilMode;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * K, on every path: the kakuja transformation. K does nothing until a kakuja is available ({@link #KAKUJA_MASTERY}); from then
 * on, with the kagune out, a tap transforms into the first kakuja and back, and from mastery 1000 holding it brings out the final
 * one. Both clients time the key ({@code ukaku/KakujaKey}) and send which ({@code C2SUkakuAction.KAKUJA} or {@code FINAL_KAKUJA}).
 * <ul>
 *   <li>Ukaku: a tap of K and the wings give way to his first kakuja (six armored tentacles and the armor over his body), and
 *       back to the wings; from mastery 1000 holding K brings out his final kakuja (three of them burn into fire tendrils, a
 *       half mask grows over his face), straight from the wings or from the first kakuja. The transformation lasts while the
 *       kagune is out. His client times the key and sends which ({@code C2SUkakuAction.KAKUJA} or {@code FINAL_KAKUJA}).</li>
 *   <li>Rinkaku: his last two forms are his kakuja ({@link RinkakuForms}): a tap gives way to the first (the Centipede Form, two
 *       centipede legs), a hold from mastery 1000 to the final one (the Full Kakuja, legs and tentacles); a tap or a hold
 *       goes back to the form it came from (remembered in the profile as {@code KakujaBaseForm}). A kakuja K brought out ends
 *       when the kagune goes back in.</li>
 * </ul>
 * {@code KaguneServerController.cycleTendrils} (the original's tendril-count cycle) is a bytecode trampoline to here.
 */
public final class UkakuForms {
    /** Mastery at which the kakuja becomes available on either path (Rinkaku's Centipede Form, Ukaku's first kakuja). */
    public static final int KAKUJA_MASTERY = 750;
    /** Profile key: the Rinkaku form a K transformation came from. */
    static final String BASE_FORM_KEY = "KakujaBaseForm";

    private UkakuForms() {
    }

    /** Replaces {@code KaguneServerController.cycleTendrils}: K transforms into the kakuja and back. */
    public static void cycleTendrils(ServerPlayer player) {
        if (UkakuServer.isUkaku(player)) {
            kakujaKey(player, false);
        } else {
            rinkakuKey(player, false);
        }
    }

    /**
     * Rinkaku's K ({@link RinkakuForms#afterKey}): a tap brings out the first kakuja (the Centipede Form) or goes back out of
     * either; a hold brings out the final kakuja (the Full Kakuja, from a base form or the first kakuja) or goes back out of it.
     * Going back lands on the form the kakuja came from. His client times the key as Ukaku's does.
     * <p>1.0.10, after the Dragon: the Centipede Form and the Full Kakuja are his half kakujas, and a tap brings out the strongest
     * he has (the Full Kakuja once its quest is done); a hold puts on his final kakuja, War or Peace, once chosen ({@link
     * KingServer#toggle}), and K takes it off again.
     */
    public static void rinkakuKey(ServerPlayer player, boolean hold) {
        if (KingServer.wearing(player) != com.hex.kagune.control.king.KingForm.NONE) {
            KingServer.off(player, true);
            return;
        }
        if (hold && KingServer.chosen(player).built()) {
            leaveRinkakuKakuja(player); // out of a half kakuja first: the final one has the body to itself
            KingServer.toggle(player);
            return;
        }
        int mastery = KaguneProfile.mastery(player);
        if (mastery < KAKUJA_MASTERY) {
            player.m_5661_(Component.m_237110_("message.hexkagune.kakuja_locked", KAKUJA_MASTERY), true);
            return;
        }
        if (!KaguneProfile.isActive(player)) {
            player.m_5661_(Component.m_237115_("message.hexkagune.kakuja_kagune_out"), true);
            return;
        }
        KaguneForm current = KaguneProfile.form(player);
        CompoundTag root = KaguneProfile.root(player);
        // 1.0.10: once the Dragon has been through him, a tap is the strongest half kakuja he has.
        boolean strongest = !hold && KingServer.awake(player) && !rinkakuKakuja(current)
            && QuestServer.open(player, com.hex.kagune.control.quest.Gates.rinkaku(RinkakuForms.finalKakuja()));
        KaguneForm next = RinkakuForms.afterKey(current, baseForm(root, mastery), mastery, hold || strongest);
        if (next == null) {
            return;
        }
        if (rinkakuKakuja(next) && next != current && !QuestServer.pass(player, com.hex.kagune.control.quest.Gates.rinkaku(next))) {
            return; // 7.0.1: a kakuja also needs its quest
        }
        if (!rinkakuKakuja(next)) {
            root.m_128473_(BASE_FORM_KEY);
        } else if (!rinkakuKakuja(current)) {
            root.m_128359_(BASE_FORM_KEY, current.id());
        }
        if (!KaguneProfile.setForm(player, next)) {
            return;
        }
        KaguneServerController.onFormChanged(player);
        if (rinkakuKakuja(next)) {
            // 5.0.7: the kakuja is pulled out of his body by the kagune's own manifestation, synced to everyone.
            pullOut(player);
            // 5.0.0: the legs tear out of his back first (KakujaBurst); they hunt once they are out.
            KaguneRuntime.State state = KaguneRuntime.peek(player.m_20148_());
            if (state != null) {
                state.nextCentipedeAttackAt = Math.max(state.nextCentipedeAttackAt, player.m_9236_().m_46467_() + (long)KakujaBurst.burstEnd(2));
            }
        }
        KaguneNetwork.broadcast(player);
        announce(player, rinkakuKakuja(next), next.displayName());
    }

    /** 1.0.10: back out of a Rinkaku kakuja to the form it came from (nothing when he is in none). */
    static void leaveRinkakuKakuja(ServerPlayer player) {
        KaguneForm current = KaguneProfile.form(player);
        if (!KaguneProfile.isActive(player) || !rinkakuKakuja(current)) {
            return;
        }
        CompoundTag root = KaguneProfile.root(player);
        KaguneForm back = baseForm(root, KaguneProfile.mastery(player));
        root.m_128473_(BASE_FORM_KEY);
        if (KaguneProfile.setForm(player, back)) {
            KaguneServerController.onFormChanged(player);
            KaguneNetwork.broadcast(player);
        }
    }

    /**
     * Ukaku's K: a tap ({@code hold} false) transforms into the first kakuja, or out of either kakuja; holding it transforms into
     * the final kakuja (from the wings or the first kakuja), or out of it.
     */
    public static void kakujaKey(ServerPlayer player, boolean hold) {
        int mastery = KaguneProfile.mastery(player);
        if (mastery < KAKUJA_MASTERY) {
            player.m_5661_(Component.m_237110_("message.hexkagune.kakuja_locked", KAKUJA_MASTERY), true);
            return;
        }
        if (!KaguneProfile.isActive(player)) {
            player.m_5661_(Component.m_237115_("message.hexkagune.kakuja_kagune_out"), true);
            return;
        }
        WingForm now = UkakuServer.form(player);
        if (hold && !WingForm.FINAL_KAKUJA.unlocked(mastery)) {
            UkakuServer.locked(player, WingForm.FINAL_KAKUJA.displayName(), WingForm.FINAL_KAKUJA.requiredMastery());
            return;
        }
        boolean on = hold ? !now.finalKakuja() : !now.kakuja();
        if (on && !QuestServer.pass(player, hold ? com.hex.kagune.control.quest.Gate.UKAKU_FINAL_KAKUJA : com.hex.kagune.control.quest.Gate.UKAKU_KAKUJA)) {
            return; // 7.0.1: a kakuja also needs its quest
        }
        UkakuServer.setKakuja(player, on, hold);
        if (on) {
            // 5.0.7: his kakuja grows out of his back on the kagune's own manifestation, synced to everyone.
            pullOut(player);
            KaguneNetwork.broadcast(player);
        }
        WingForm after = UkakuServer.form(player);
        announce(player, on, after.displayName(mastery));
    }

    /**
     * A kakuja coming out (5.0.7): the kagune goes through the original's own manifestation (its MANIFEST mode), as when it is
     * unleashed: every limb grows out of his back in turn, with the body's pain pose, the blood and the tear (Rinkaku) or the
     * ignition (Ukaku) on every client. Not while the kagune is busy (a strike, a hold): the kakuja then just appears.
     */
    static void pullOut(ServerPlayer player) {
        KaguneRuntime.State state = KaguneRuntime.peek(player.m_20148_());
        if (state == null || state.mode != TendrilMode.IDLE && state.mode != TendrilMode.RESHAPE && state.mode != TendrilMode.MANIFEST) {
            return;
        }
        int count = KaguneProfile.manifestedLimbs(player);
        state.transitionFromLimbCount = 0;
        state.transitionToLimbCount = count;
        state.begin(TendrilMode.MANIFEST, player.m_9236_().m_46467_(), 18 + Math.max(1, count) * 7, 0, Vec3.f_82478_);
    }

    /** True for a Rinkaku kakuja: the last two forms (the Centipede Form and the Full Kakuja). */
    static boolean rinkakuKakuja(KaguneForm form) {
        return RinkakuForms.kakuja(form);
    }

    /** The form a Rinkaku kakuja goes back to: the one it came from, or the strongest form before the kakuja. */
    static KaguneForm baseForm(CompoundTag root, int mastery) {
        if (root.m_128441_(BASE_FORM_KEY)) {
            KaguneForm stored = KaguneForm.fromId(root.m_128461_(BASE_FORM_KEY));
            if (stored != null && !rinkakuKakuja(stored) && stored.isUnlocked(mastery)) {
                return stored;
            }
        }
        return RinkakuForms.strongestBase(mastery);
    }

    /**
     * Called when a Rinkaku kagune goes back in: a kakuja K brought out ends with it (the next time it comes out as the form
     * it came from). A kakuja picked some other way (the menu) is left alone.
     */
    static void kagunePutAway(ServerPlayer player) {
        CompoundTag root = KaguneProfile.root(player);
        if (!root.m_128441_(BASE_FORM_KEY) || !rinkakuKakuja(KaguneProfile.form(player))) {
            return;
        }
        KaguneForm base = baseForm(root, KaguneProfile.mastery(player));
        root.m_128473_(BASE_FORM_KEY);
        if (KaguneProfile.setForm(player, base)) {
            KaguneServerController.onFormChanged(player);
            KaguneNetwork.broadcast(player);
        }
    }

    private static void announce(ServerPlayer player, boolean on, String name) {
        player.m_5661_(Component.m_237110_(on ? "message.hexkagune.kakuja_on" : "message.hexkagune.kakuja_off", name), true);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, on ? 1.0F : 0.7F,
            KaguneServerController.vary(player, on ? 0.75F : 1.1F, 0.05F));
    }
}
