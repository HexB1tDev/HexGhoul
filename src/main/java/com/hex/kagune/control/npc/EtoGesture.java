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
    LAUGH(44, false, false),
    /** 1.3.3: hand in hand: her hand out to the one at her side, walking with them. */
    HAND(0, false, true),
    // 1.3.14: Touka's and Ken's
    /** Arms folded tight, weight on one leg, chin turned a little away: unimpressed. */
    CROSS(120, false, false),
    /** Hands on her hips, one foot tapping: waiting, and not patiently. */
    TAP(80, false, false),
    /** A sharp breath out through the nose, shoulders up and dropped, head turned away. */
    HUFF(36, false, false),
    /** A cup held in both hands, a sip, a sigh. */
    SIP(100, false, false, "minecraft:flower_pot", null),
    /** Drying a cup with a cloth, round and round. */
    WIPE(120, false, false, "minecraft:paper", "minecraft:flower_pot"),
    /** A practice kick, high and fast, then the leg down and a shake of the shoulders. */
    KICKS(40, false, false),
    /** Hands on hips, leaning in, a finger wagged: a telling-off. */
    SCOLD(64, false, false),
    /** Turned half away, arms crossed, cheeks pink, a glance back: it's not like that. */
    SHY(90, false, true),
    /** A hand to the chin, head tipped: thinking. Or hiding something. */
    CHIN(96, false, false),
    /** A thumb pressed to a finger until it cracks; the head tilts with it. */
    CRACK(56, false, false),
    /** A small, polite bow. */
    BOW(36, false, false),
    /** A quick little kiss, given unasked: up on her toes, in, out, eyes shut and pink. */
    PECK(30, true, true),
    /**
     * 1.3.29 (Touka): a kiss stolen: she turns their face to hers (their view drawn to her while it lasts), kisses them before they know
     * it, then pulls back, arms crossed, looking anywhere else.
     */
    STEAL(60, true, true),
    /** 1.3.29: watering the flowers round the house (a bucket in her hand, tipped over them). */
    WATER(120, false, false, "minecraft:water_bucket", null),
    /** 1.3.29: tidying a chest (bent over it, putting things where they go). */
    TIDY(140, false, false),
    /** 1.3.31: cooking at the stove (a bowl in her off hand, stirring with the other, her head down to the fire). */
    COOK(160, false, false, "minecraft:wooden_shovel", "minecraft:bowl"),
    /**
     * 1.3.65 (a final kakuja, War above all): a step back from it, one arm drawn in across her body, the other hand thrown up in front of
     * her, palm out, to keep it off; her head pulled back and turned a little aside; a tremble that settles as she holds her ground.
     */
    FLINCH(70, false, false),
    /** 1.3.65 (Peace): her hands together low on her chest, her head tipped back to look up at it, a slow breath in and out. */
    AWE(96, false, false),
    /** 2.4.9: bent to the stove's mouth, feeding the fire (a lump of coal pushed in, a poke, the hand drawn back). */
    STOKE(60, false, false, "minecraft:coal", null),
    /** 2.4.9: a pinch of something shaken over the pot from high up, the bowl held under it. */
    SEASON(56, false, false, "minecraft:sugar", "minecraft:bowl"),
    /** 2.4.9: a taste off the spoon: brought up, blown on, tasted, a little nod. */
    TASTE(70, false, false, "minecraft:wooden_shovel", "minecraft:bowl"),
    /** 2.4.9: what it made held out to its spouse in both hands (the food itself put in its hand by the server), a little bob. */
    SERVE(50, false, false),
    /** 3.3.6 (the owner: a jealous animation): her back turned, a glare over her shoulder, a huff, the snub and a foot tapping. Appended. */
    JEALOUS(140, false, false),
    // 4.0.2 (the owner: Ayato "just looks edgy. He likes to pose. Just a massive poser"; "Noro simply like tilts his head and looks at the
    // player weird ... He's just a weird guy"). Appended.
    /** Ayato: weight back on one leg, hands in his pockets, chin up, looking down his nose at whoever is there; a slow roll of the head. */
    POSE(110, false, false),
    /** Ayato: a hand dragged back through his hair, then the arm folded behind his head, the other hand on his hip; a smirk of a tilt. */
    SLICK(80, false, false),
    /** Ayato: a big, lazy shrug, palms up, head rolled to one side: whatever. */
    SHRUG(40, false, false),
    /** Noro: his head tilts, slowly, far over to one side, and stays there, staring; then back, just as slowly. Nothing else moves. */
    TILT(90, false, false),
    /**
     * 4.0.9 (the owner: "make sure he sits on the top"): Ken, sitting on the top of his tower: down on the deck, one leg out, the other knee
     * up with a forearm on it, leaning back on the other hand, looking out. Held until something calls him up. Appended.
     */
    SIT(0, false, false);

    /** 2.5.1: when, in a finger crack ({@link #CRACK}), the finger cracks (ticks): the pose's snap and its sound, together. */
    public static final int CRACK_SNAP = 17; // 3.0.2: 22 before (the kagune came out a long second after the key)

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

    /** 3.2.3: how long a held gesture lasts with nothing holding it (ticks). */
    public static final long ALONE = 80L;

    /**
     * 3.2.3 (the owner: "whenever you finish kissing, sometimes the NPC gets stuck in that position after the kiss"): true if this held
     * gesture (a kiss, a blush, the pull, a hand) has nothing holding it any more ({@code kissing}: a kiss under way or on its way;
     * {@code holding}: a hand held) and has been up {@link #ALONE} ticks. A blush given as a reaction (an answer's, a partner's eyes) was
     * such a one: held, it never ran out, and she stood frozen leaning in for a kiss nobody was giving.
     */
    public boolean stranded(long age, boolean kissing, boolean holding) {
        return this.held() && this != SIT && age >= ALONE && (this == HAND ? !holding : !kissing);
    }

    /** True if it is over {@code age} ticks in. */
    public boolean over(long age) {
        return this == NONE || this.ticks > 0 && age >= this.ticks;
    }

    /** True if she stands still for it (a prop in her hands, a lean, a sulk). */
    public boolean still() {
        return this.mainHand != null || this == LEAN || this == STRETCH || this == POUT || this == YAWN || this == PET || this == LOOK || this == CROSS
            || this == TAP || this == SCOLD || this == SHY || this == CHIN || this == CRACK || this == BOW || this == KICKS || this == STEAL || this == TIDY
            || this == COOK || this == FLINCH || this == AWE || this == SERVE || this == JEALOUS || this == SIT;
    }
}
