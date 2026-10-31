package com.hex.kagune.control.npc;

/**
 * 1.3.0: what Eto is doing with her body when she is not fighting (pure; sent by ordinal, new ones at the end). Her idle ones, goofy and
 * relaxed, last {@link #ticks}; the kiss and the blush are held until the server ends them (0). {@link #eyesClosed}: her eyes shut,
 * "- -"; {@link #blush}: her cheeks pink. 1.3.2: what she holds while she does it ({@link #mainHand}, {@link #offHand}: an item id, or
 * null), the server putting it in her hands and taking it back after.
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
    ANGRY(80, false, false),
    // 1.3.2
    /** A book open in both hands, head bowed, a page turned now and then. */
    READ(180, false, false, "minecraft:book", null),
    /** Her pocket book in one hand, a quill scratching in the other. */
    WRITE(150, false, false, "minecraft:feather", "minecraft:writable_book"),
    /** Brushing the dust off the floor, stroke by stroke. */
    SWEEP(130, false, false, "minecraft:brush", null),
    /** A lock of hair wound round a finger, head tipped. */
    HAIR(56, false, false),
    /** Swaying, hands behind her back, humming something. */
    HUM(96, true, false),
    /** A hand over her eyes, looking out one way, then the other. */
    LOOK(64, false, false),
    /** A silly little dance: arms up in turn, a hop, a half turn. */
    DANCE(64, false, false),
    /** Sulking: arms crossed, chin down, turned half away. */
    POUT(96, false, false),
    /** A yawn behind her hand, a shudder of a stretch. */
    YAWN(48, true, false),
    /** A finger at her lips, head tipped, eye bright: hungry. */
    LICK(44, false, false),
    /** Wrapped round her partner's arm, cheek to their shoulder. */
    CLING(140, true, true),
    /** The kiss held long enough: her arms round their head, pulling them in. */
    PULL(0, true, true),
    /** The ring: hands to her mouth, then a jump. */
    RING(64, false, true),
    /** A puppy (or kitten, or whatever was brought) cuddled to her chest. */
    PET(96, true, true),
    /** Her laugh in a fight gone bad for someone: head back, arms wide. */
    LAUGH(44, false, false);

    public final int ticks;
    public final boolean eyesClosed;
    public final boolean blush;
    public final String mainHand;
    public final String offHand;

    EtoGesture(int ticks, boolean eyesClosed, boolean blush) {
        this(ticks, eyesClosed, blush, null, null);
    }

    EtoGesture(int ticks, boolean eyesClosed, boolean blush, String mainHand, String offHand) {
        this.ticks = ticks;
        this.eyesClosed = eyesClosed;
        this.blush = blush;
        this.mainHand = mainHand;
        this.offHand = offHand;
    }

    public static EtoGesture byOrdinal(int ordinal) {
        EtoGesture[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
    }

    /** The goofy ones she does by herself now and then (1.3.2: her mood's, {@link EtoMood#idles}, first). */
    public static final EtoGesture[] IDLE = {WAVE, STRETCH, TWIRL, HOP, PEACE, LEAN, GIGGLE, READ, WRITE, SWEEP, HAIR, HUM, LOOK, DANCE};

    /** True if it is held until the server ends it. */
    public boolean held() {
        return this.ticks == 0 && this != NONE;
    }

    /** True if it is over {@code age} ticks in. */
    public boolean over(long age) {
        return this == NONE || this.ticks > 0 && age >= this.ticks;
    }

    /** True if she stands still for it (a prop in her hands, a lean, a sulk). */
    public boolean still() {
        return this.mainHand != null || this == LEAN || this == STRETCH || this == POUT || this == YAWN || this == PET || this == LOOK;
    }
}
