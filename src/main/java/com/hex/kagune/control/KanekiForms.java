package com.hex.kagune.control;

import com.hex.kagune.gameplay.KaguneForm;

/**
 * Kaneki's forms as the expansion orders them (pure, tested). The build swaps the original's Centipede Form and Partial Kakuja
 * ({@code KagunePatch.patchFormOrder}, 3.0.1) and then drops the Partial Kakuja (3.0.2): it stays an enum constant, because
 * switch maps, saves and packets index forms by ordinal, but it never unlocks and every list a player sees skips it
 * ({@link #forms()}: the menu, {@code /kagune form}, stored ids). A save that was in it loads as the Rinkaku Seed. The forms
 * come every 250 mastery: Rinkaku Seed 0, Twin Rinkaku 250, Four-Tail Rinkaku 500, Centipede Form 750, Full Kakuja 1000. The
 * last two are always his kakuja: the first (the two centipede legs, a tap of K) and the final one (centipede legs and
 * tentacles together, K held).
 */
public final class KanekiForms {
    /** Menu button fills for a kakuja form (the original's crimson ones turned purple, as Ayato's kakuja buttons). */
    public static final int KAKUJA_SELECTED = 0xFF8A2EC0;
    public static final int KAKUJA_UNLOCKED = 0xFF44205E;
    public static final int KAKUJA_LOCKED = 0xFF261E30;
    /** Menu button labels for a kakuja form. */
    public static final int KAKUJA_LABEL = 0xFFEADCF6;
    public static final int KAKUJA_LABEL_LOCKED = 0xFF665A74;

    /** The form taken out in 3.0.2 (two centipede legs and two tentacles between the Four-Tail and the first kakuja). */
    public static final KaguneForm REMOVED = KaguneForm.PARTIAL_KAKUJA;

    private KanekiForms() {
    }

    /** The forms a player can have, weakest first: every form but {@link #REMOVED} (the patched {@code KaguneForm.find/ids} and menu use this). */
    public static KaguneForm[] forms() {
        KaguneForm[] all = KaguneForm.values();
        KaguneForm[] forms = new KaguneForm[all.length - 1];
        int count = 0;
        for (KaguneForm form : all) {
            if (form != REMOVED) {
                forms[count++] = form;
            }
        }
        return forms;
    }

    /** True for one of Kaneki's two kakuja: the last two forms. */
    public static boolean kakuja(KaguneForm form) {
        return form != null && (form == firstKakuja() || form == finalKakuja());
    }

    /** The first kakuja: the second to last form (the Centipede Form, two centipede legs). */
    public static KaguneForm firstKakuja() {
        KaguneForm[] forms = forms();
        return forms[forms.length - 2];
    }

    /** The final kakuja: the last form (the Full Kakuja, centipede legs and tentacles). */
    public static KaguneForm finalKakuja() {
        KaguneForm[] forms = forms();
        return forms[forms.length - 1];
    }

    /** Speed amplifier the centipede forms run with (the original's Speed III), and the first kakuja's own (5.0.9: Speed V). */
    public static final int CENTIPEDE_SPEED = 2;
    public static final int FIRST_KAKUJA_SPEED = 4;

    /**
     * True for a form whose kagune the player doesn't control: the first kakuja. Its centipede legs hunt and fight on their own,
     * so the player can only walk in it (5.0.9 took the M1, 5.0.10 every move key: {@code KanekiAutonomy}); the final kakuja's
     * tentacles are still his.
     */
    public static boolean autonomous(KaguneForm form) {
        return form != null && form == firstKakuja();
    }

    /** The Speed amplifier {@code form} runs with while the kagune is out, or -1 for none (only the centipede forms have it). */
    public static int speed(KaguneForm form) {
        if (form == null || !form.hasCentipedeLegs()) {
            return -1;
        }
        return autonomous(form) ? FIRST_KAKUJA_SPEED : CENTIPEDE_SPEED;
    }

    /** The strongest form below the kakuja unlocked at {@code mastery} (what a kakuja falls back to). */
    public static KaguneForm strongestBase(int mastery) {
        KaguneForm best = forms()[0];
        for (KaguneForm form : forms()) {
            if (!kakuja(form) && form.isUnlocked(mastery) && form.requiredMastery() >= best.requiredMastery()) {
                best = form;
            }
        }
        return best;
    }

    /**
     * What K turns {@code current} into, or {@code null} when it changes nothing. A tap ({@code hold} false) brings out the first
     * kakuja, or goes back out of either; a hold brings out the final kakuja (from a base form or from the first kakuja), or goes
     * back out of it. Going back lands on {@code base} (the form the kakuja came from, never a kakuja). Below the final kakuja's
     * mastery a hold is a tap; below the first kakuja's, K does nothing.
     */
    public static KaguneForm afterKey(KaguneForm current, KaguneForm base, int mastery, boolean hold) {
        if (!firstKakuja().isUnlocked(mastery)) {
            return null;
        }
        boolean finalHeld = hold && finalKakuja().isUnlocked(mastery);
        KaguneForm back = base != null && base != REMOVED && !kakuja(base) && base.isUnlocked(mastery) ? base : strongestBase(mastery);
        KaguneForm next;
        if (finalHeld) {
            next = current == finalKakuja() ? back : finalKakuja();
        } else {
            next = kakuja(current) ? back : firstKakuja();
        }
        return next == current ? null : next;
    }

    /** The menu button fill for {@code form}: the original's {@code colour}, purple for a kakuja. */
    public static int buttonColor(int colour, KaguneForm form, boolean selected, boolean unlocked) {
        if (!kakuja(form)) {
            return colour;
        }
        return selected ? KAKUJA_SELECTED : unlocked ? KAKUJA_UNLOCKED : KAKUJA_LOCKED;
    }

    /** The menu button label colour for {@code form}: the original's {@code colour}, lavender for a kakuja. */
    public static int labelColor(int colour, KaguneForm form, boolean unlocked) {
        if (!kakuja(form)) {
            return colour;
        }
        return unlocked ? KAKUJA_LABEL : KAKUJA_LABEL_LOCKED;
    }
}
