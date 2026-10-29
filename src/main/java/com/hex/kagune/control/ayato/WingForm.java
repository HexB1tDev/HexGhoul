package com.hex.kagune.control.ayato;

import java.util.Locale;

/**
 * How Ayato's kagune is out (docs/AYATO_PATH.md, "Abilities"): the twin wings, the one-sided "Rabbit Guise" he uses to
 * pass as Touka (her Ukaku is on the left shoulder, so the left wing stays out), and the :re One-Winged Kagune (one
 * huge crystallized blade, also from the left shoulder blade). A new Ayato has only the one wing (the left); the twin
 * wings come at mastery 100. His first kakuja (mastery 750, tap K) is not wings at all: six armored tentacles grow from his
 * shoulder blades and armor grows over his body. His final kakuja (mastery 1000, hold K) keeps three of them as needle
 * tentacles and burns the other three into fire tendrils, under a half mask. The wings follow his mastery and K transforms
 * him into a kakuja and back ({@code AyatoForms}); Rabbit Guise and the One-Winged Kagune are kept for when they get their
 * own way in. New forms go at the end: the state packet sends the form by its place in this list.
 */
public enum WingForm {
    TWIN("twin", "Twin Wings", 100),
    RABBIT("rabbit", "Rabbit Guise", 0),
    KAKUJA("kakuja", "First Kakuja", 750),
    ONE_WING("one_wing", "One-Winged Kagune", 750),
    FINAL_KAKUJA("final_kakuja", "Final Kakuja", 1000);

    private final String id;
    private final String displayName;
    private final int requiredMastery;

    WingForm(String id, String displayName, int requiredMastery) {
        this.id = id;
        this.displayName = displayName;
        this.requiredMastery = requiredMastery;
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public int requiredMastery() {
        return this.requiredMastery;
    }

    public boolean unlocked(int mastery) {
        return mastery >= this.requiredMastery;
    }

    /** True when the right wing is out (only the twin wings have it). */
    public boolean rightWing() {
        return this == TWIN;
    }

    /**
     * True for the Ukaku wings themselves (the twin wings and Rabbit Guise): what the crystal mode, the Wing Vortex and the
     * Gatling need. The One-Winged Kagune is one blade and the kakuja is six tentacles.
     */
    public boolean wings() {
        return this == TWIN || this == RABBIT;
    }

    /** True for either kakuja: the six tentacles instead of wings. */
    public boolean kakuja() {
        return this == KAKUJA || this == FINAL_KAKUJA;
    }

    /** True for the final kakuja: three needle tentacles, three fire tendrils and the half mask. */
    public boolean finalKakuja() {
        return this == FINAL_KAKUJA;
    }

    /** The form actually usable at {@code mastery}: a locked form falls back to the twin wings, or the one wing before them. */
    public WingForm usable(int mastery) {
        if (this == FINAL_KAKUJA && !this.unlocked(mastery)) {
            return KAKUJA.usable(mastery);
        }
        return this.unlocked(mastery) ? this : TWIN.unlocked(mastery) ? TWIN : RABBIT;
    }

    /**
     * The form a client shows for this one (the form the server last sent) at the mastery the client knows. The server sends
     * {@link #RABBIT} for a new Ayato's one wing, the wings his mastery has grown so far, so from the twin wings' mastery on
     * that is the twin wings, whether or not the server's next state has come yet (5.0.16: the second wing waited for the next
     * state, which only something like the crystal mode sent). Every other form is {@link #usable}. If Rabbit Guise ever
     * becomes a form he picks, it needs its own id here.
     */
    public WingForm shown(int mastery) {
        return this == RABBIT ? TWIN.usable(mastery) : this.usable(mastery);
    }

    /** What to call this form at {@code mastery}: before the twin wings, the one wing is just that. */
    public String displayName(int mastery) {
        return this == RABBIT && !TWIN.unlocked(mastery) ? "One Wing" : this.displayName;
    }

    /** The next unlocked form after this one, wrapping around (the cycle key). */
    public WingForm next(int mastery) {
        WingForm[] all = values();
        for (int step = 1; step <= all.length; step++) {
            WingForm candidate = all[(this.ordinal() + step) % all.length];
            if (candidate.unlocked(mastery)) {
                return candidate;
            }
        }
        return this.usable(mastery);
    }

    public static WingForm fromId(String id) {
        if (id != null) {
            String key = id.trim().toLowerCase(Locale.ROOT);
            for (WingForm form : values()) {
                if (form.id.equals(key)) {
                    return form;
                }
            }
        }
        return TWIN;
    }

    public static WingForm byOrdinal(int ordinal) {
        WingForm[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : TWIN;
    }
}
