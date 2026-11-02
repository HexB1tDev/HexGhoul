package com.hex.kagune.control.family;

/**
 * 1.3.57: what a child does with its body while it says a line in a talk (pure; chosen by {@link TalkCues}, posed by the client's
 * {@code ChildTalkPose}), the owner: "the beauty of interactions". Sent by ordinal ({@code S2CChild}): append only.
 */
public enum ChildCue {
    NONE(0),
    /** Arms folded tight, chin up and turned a little away. */
    CROSS(70),
    /** Fists down at its sides and a stamp of the foot. */
    STAMP(36),
    /** An arm out, pointing at the other, jabbing. */
    POINT(50),
    /** Hands on its hips, chest out, chin up: proud of itself. */
    HIPS(70),
    /** Arms out, palms up, shoulders up and down. */
    SHRUG(40),
    /** Head down, hands behind its back, a foot scuffing the ground. */
    SORRY(80),
    /** Both arms up, a hop. */
    CHEER(44),
    /** A hand over its mouth, shoulders shaking. */
    GIGGLE(50),
    /** Leaning in, a hand by its mouth: a secret. */
    WHISPER(70),
    /** Head waggling side to side, leaning back: "bleh". */
    BLEH(40),
    /** A hand out to the other's shoulder, patting. */
    PAT(70),
    /** Arms folded low, head down and turned away. */
    SULK(90),
    /** A hand to its chin, head tipped: thinking. */
    THINK(70),
    /** A wave. */
    WAVE(36),
    /** Hands together under its chin, bouncing on its toes: "pleeease". */
    PLEAD(60),
    /** 1.3.58 (hide and seek): crouched down small, arms round its knees, peeking. Held while it hides (a new cue ends it). */
    HIDE(1200),
    /** 1.3.58 (hide and seek): both hands over its eyes, counting. */
    COUNT(110),
    /**
     * 1.3.65 (a parent in War): hunched behind the other parent, its arms crossed up in front of its chest and chin (a little one's hands
     * clutched under its chin), its head tucked down, peeking out at them, trembling. Held while it hides (cleared when it stops).
     */
    COWER(1200);

    /** How long it lasts (ticks). */
    public final int ticks;

    ChildCue(int ticks) {
        this.ticks = ticks;
    }

    public static ChildCue byOrdinal(int ordinal) {
        ChildCue[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
    }

    /** True while it still plays {@code age} ticks in. */
    public boolean running(double age) {
        return this != NONE && age >= 0.0 && age < this.ticks;
    }

    /** How much of it shows {@code age} ticks in: easing in over 4 ticks, out over its last 8. */
    public float weight(double age) {
        if (!this.running(age)) {
            return 0.0F;
        }
        double in = Math.min(1.0, age / 4.0);
        double out = Math.min(1.0, (this.ticks - age) / 8.0);
        double w = Math.max(0.0, Math.min(in, out));
        return (float)(w * w * (3.0 - 2.0 * w));
    }
}
