package com.hex.kagune.control.bikaku;

/**
 * 3.6.0 (the owner: "we need more forms. Currently there's only one one of these things, but another form should have like two worms. The
 * third should have three, and ... the fourth before the Kakuja should be like the player no longer even like walks with their own legs";
 * and of the moves: "to basically become overpowered, like, to do enhanced versions of themselves whenever they're, like, in different
 * transformations"): the Bikaku path's forms before its kakuja (pure). Each opens at its mastery (as the Rinkaku's forms do, every 250)
 * and is picked in the menu; each has its worms, out of the one root, each in its own lane.
 */
public enum BikakuForm {
    ONE("One Worm", 1, 0, true),
    TWIN("Twin Worms", 2, 250, true),
    THREE("Three Worms", 3, 500, true),
    /**
     * 3.6.2: The Walker: three worms as Three Worms, and two walking tentacles out of the same root that carry him (his own legs hang
     * still), finding the ground round him and climbing walls ({@link WalkerLegs}).
     */
    WALKER("The Walker", 3, 750, true);

    private final String displayName;
    private final int worms;
    private final int mastery;
    private final boolean built;

    BikakuForm(String displayName, int worms, int mastery, boolean built) {
        this.displayName = displayName;
        this.worms = worms;
        this.mastery = mastery;
        this.built = built;
    }

    /** False for a form still to come (shown, not picked); every form is built since 3.6.2. */
    public boolean built() {
        return this.built;
    }

    public String displayName() {
        return this.displayName;
    }

    /** How many worms it has (The Walker's walking tentacles are not counted: they carry him and make no move). */
    public int worms() {
        return this.worms;
    }

    /** The mastery it opens at. */
    public int mastery() {
        return this.mastery;
    }

    /** 3.6.2: true for The Walker: walking tentacles carry him. */
    public boolean walks() {
        return this == WALKER;
    }

    public boolean unlocked(int mastery) {
        return this.built && mastery >= this.mastery;
    }

    public static BikakuForm byOrdinal(int ordinal) {
        BikakuForm[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : ONE;
    }

    /** {@code wanted} if {@code mastery} has opened it, else the strongest form it has. */
    public static BikakuForm allowed(BikakuForm wanted, int mastery) {
        if (wanted != null && wanted.unlocked(mastery)) {
            return wanted;
        }
        BikakuForm best = ONE;
        for (BikakuForm form : values()) {
            if (form.unlocked(mastery) && (wanted == null || form.ordinal() < wanted.ordinal())) {
                best = form;
            }
        }
        return best;
    }

    /** Which of {@code worms} worms makes a bite of the M1 string ({@code bite}): each in turn. */
    public static int biter(BikakuRules.Bite bite, int worms) {
        if (worms <= 1 || bite == null || bite == BikakuRules.Bite.NONE) {
            return 0;
        }
        return Math.floorMod(bite.ordinal() - 1, worms);
    }
}
