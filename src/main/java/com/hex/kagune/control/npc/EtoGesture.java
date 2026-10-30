package com.hex.kagune.control.npc;

/**
 * 1.3.0: what Eto is doing with her body when she is not fighting (pure; sent by ordinal, new ones at the end). Her idle ones, goofy and
 * relaxed, last {@link #ticks}; the kiss and the blush are held until the server ends them (0). {@link #eyesClosed}: her eyes shut,
 * "- -"; {@link #blush}: her cheeks pink.
 */
public enum EtoGesture {
    NONE(0, false, false),
    WAVE(40, false, false),
    STRETCH(56, true, false),
    TWIRL(30, false, false),
    HOP(24, false, false),
    PEACE(40, false, false),
    LEAN(70, false, false),
    GIGGLE(36, true, false),
    /** Waiting for a kiss: hands behind her back, leaning in, eyes shut. */
    KISS(0, true, false),
    /** Kissed and holding it: the same, blushing. */
    BLUSH(0, true, true),
    /** Kissed: a happy little bounce, hearts. */
    KISSED(30, true, true),
    HUG(50, true, true),
    /** Left hanging: arms crossed, the anger mark over her head. */
    ANGRY(80, false, false);

    public final int ticks;
    public final boolean eyesClosed;
    public final boolean blush;

    EtoGesture(int ticks, boolean eyesClosed, boolean blush) {
        this.ticks = ticks;
        this.eyesClosed = eyesClosed;
        this.blush = blush;
    }

    public static EtoGesture byOrdinal(int ordinal) {
        EtoGesture[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
    }

    /** The goofy ones she does by herself now and then. */
    public static final EtoGesture[] IDLE = {WAVE, STRETCH, TWIRL, HOP, PEACE, LEAN, GIGGLE};

    /** True if it is held until the server ends it. */
    public boolean held() {
        return this.ticks == 0 && this != NONE;
    }

    /** True if it is over {@code age} ticks in. */
    public boolean over(long age) {
        return this == NONE || this.ticks > 0 && age >= this.ticks;
    }
}
