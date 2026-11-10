package com.hex.kagune.control.pale;

/**
 * Pale's Speed Blitz (M1; 6.1.0, pure): eight strikes chained click by click, each thrown with the whole body. The first four
 * are hers from the start, the uppercut and the blinking palm from {@link PaleRules#BLITZ_BLINK_MASTERY}, the spinning backfist
 * and the twin palm finisher from {@link PaleRules#BLITZ_FINISH_MASTERY}; past the last one she has, the blitz starts over.
 *
 * <p>Each strike: its length, impact tick and the tick the next strike may cut its recovery short from (the fighter's chain: the
 * last strike's extension is the next one's coil), damage multiplier, how hard and which way it throws the target (back, to her right
 * or left, up), its reach, whether it is thrown from the right hand, and whether she blinks (vanishes for an instant and strikes
 * from another angle) first.
 */
public enum PaleStrike {
    /** A snapping lead jab, the left fist straight from the guard. */
    JAB(7, 2, 4, 0.8F, 0.25, 0.0, 0.05, 3.2, false, Blink.NONE, 0),
    /** The right cross, hips and shoulders turning through. */
    CROSS(8, 3, 5, 0.9F, 0.3, 0.0, 0.05, 3.3, true, Blink.NONE, 0),
    /** A left hook round the side of the head: the target is knocked to her right. */
    HOOK(8, 3, 5, 1.0F, 0.2, 0.42, 0.08, 3.1, false, Blink.NONE, 0),
    /** Dipping under, a right hook into the body (the liver): it staggers. */
    LIVER(8, 3, 5, 1.0F, 0.32, 0.0, 0.02, 3.0, true, Blink.NONE, 0),
    /** Rising from the dip, a left uppercut that lifts the target off its feet. */
    UPPERCUT(9, 4, 6, 1.2F, 0.15, 0.0, 0.55, 3.1, false, Blink.NONE, PaleRules.BLITZ_BLINK_MASTERY),
    /** She vanishes and strikes from the target's side with a right palm. */
    BLINK_PALM(10, 4, 6, 1.25F, 0.25, -0.55, 0.12, 3.0, true, Blink.SIDE, PaleRules.BLITZ_BLINK_MASTERY),
    /** A full turn on the spot into a left backfist; it catches everything round her. */
    BACKFIST(10, 5, 7, 1.35F, 0.35, 0.55, 0.1, 3.4, false, Blink.NONE, PaleRules.BLITZ_FINISH_MASTERY),
    /** She vanishes, reappears square in front of the target and drives both palms into it. */
    TWIN_PALM(14, 6, 14, 1.8F, 1.55, 0.0, 0.3, 3.2, true, Blink.FRONT, PaleRules.BLITZ_FINISH_MASTERY),

    // ---------------------------------------------------------------- 4.1.7: Kick Master (FightStyle.KICK_MASTER), its own string
    // The owner: "a new combat style mixing kicks and punches (mostly side kicks, spinning kicks etc) ... Do a very deep research into
    // combat moves like I described make sure the corehography is perfect, this combat style upon learned fully replaces the default M1
    // combos." Written as a kicker's own combination, each blow leaving the body where the next one starts from: the jab finds the
    // distance and the cross turns the hips through, which loads the rear leg for the round kick; the round kick lands that leg in
    // front, side-on, which is the lead side kick's stance; the side kick's turned hip is half the turn of the back kick; the hook
    // punch squares her up and winds her the other way for the spinning hook kick; and the hook kick's turn carries on into the jump
    // of the tornado kick.
    /** The lead jab: it finds the distance. */
    KICK_JAB(7, 2, 4, 0.8F, 0.25, 0.0, 0.05, 3.2, false, Blink.NONE, 0, Style.KICKS),
    /** The cross: the hips turned all the way through, the rear heel up (the rear leg is loaded). */
    KICK_CROSS(8, 3, 5, 0.9F, 0.3, 0.0, 0.05, 3.3, true, Blink.NONE, 0, Style.KICKS),
    /** A rear round kick into the ribs: the knee up and out, the standing foot pivoted, the shin whipped round level. It knocks left. */
    ROUND_KICK(10, 4, 6, 1.15F, 0.3, -0.5, 0.08, 3.6, true, Blink.NONE, 0, Style.KICKS),
    /** The lead side kick: the knee drawn up across the body, the hips turned over, the heel driven straight out. It drives back. */
    SIDE_KICK(10, 4, 7, 1.25F, 0.75, 0.0, 0.1, 3.8, false, Blink.NONE, 0, Style.KICKS),
    /** A spinning back kick: the back turned, the eyes over the shoulder, the heel thrust straight back, the trunk laid out level. */
    BACK_KICK(11, 5, 7, 1.4F, 0.9, 0.0, 0.15, 3.7, true, Blink.NONE, PaleRules.BLITZ_BLINK_MASTERY, Style.KICKS),
    /** A lead hook into the body on the way back round: it squares her up, and winds her for what comes. */
    KICK_HOOK(8, 3, 5, 1.0F, 0.2, 0.42, 0.05, 3.1, false, Blink.NONE, PaleRules.BLITZ_BLINK_MASTERY, Style.KICKS),
    /** A spinning hook kick: a full turn, the leg thrown out past it and the heel hooked back across its head. It knocks right. */
    HOOK_KICK(12, 6, 8, 1.5F, 0.3, 0.7, 0.15, 3.7, true, Blink.NONE, PaleRules.BLITZ_FINISH_MASTERY, Style.KICKS),
    /** The finisher, a tornado kick: a full turn into a jump, the round kick thrown from the air with the whole turn behind it. */
    TORNADO_KICK(16, 7, 16, 1.9F, 1.5, -0.3, 0.35, 3.8, true, Blink.NONE, PaleRules.BLITZ_FINISH_MASTERY, Style.KICKS);

    /** 4.1.7: which string a strike belongs to: the Speed Blitz (hers, and Speedster's), or Kick Master's. */
    public enum Style {
        BLITZ, KICKS
    }

    /** Where a blink puts her: nowhere (no blink), at the target's side, square in front of it. */
    public enum Blink {
        NONE, SIDE, FRONT
    }

    public final int duration;
    public final int impact;
    /** From this tick a queued strike starts at once, out of this one's hold (its recovery is never seen). */
    public final int chain;
    public final float damage;
    /** Knockback away from her, to her right (negative: left) and up (blocks a tick). */
    public final double back;
    public final double sideways;
    public final double up;
    public final double reach;
    public final boolean right;
    public final Blink blink;
    public final int mastery;
    public final Style style;

    PaleStrike(int duration, int impact, int chain, float damage, double back, double sideways, double up, double reach, boolean right, Blink blink,
              int mastery) {
        this(duration, impact, chain, damage, back, sideways, up, reach, right, blink, mastery, Style.BLITZ);
    }

    PaleStrike(int duration, int impact, int chain, float damage, double back, double sideways, double up, double reach, boolean right, Blink blink,
              int mastery, Style style) {
        this.style = style;
        this.duration = duration;
        this.impact = impact;
        this.chain = chain;
        this.damage = damage;
        this.back = back;
        this.sideways = sideways;
        this.up = up;
        this.reach = reach;
        this.right = right;
        this.blink = blink;
        this.mastery = mastery;
    }

    /** The tick she blinks on (the strike's first, so the fist comes from the new angle). */
    public static final int BLINK_TICK = 1;

    /** The finisher: a recovery follows it. */
    public boolean finisher() {
        return this == TWIN_PALM || this == TORNADO_KICK;
    }

    /** 4.1.7: true for a blow of a leg (its mark and its sound are a kick's). */
    public boolean kick() {
        return this == ROUND_KICK || this == SIDE_KICK || this == BACK_KICK || this == HOOK_KICK || this == TORNADO_KICK;
    }

    /** 4.1.7: the tornado kick leaves the ground: from this tick to this one, this high at most (sixteenths of a block). */
    public static final double JUMP_FROM = 2.5;
    public static final double JUMP_TO = 11.0;
    public static final double JUMP_HEIGHT = 9.0;

    /** 4.1.7: how far off the ground (sixteenths of a block) this strike has her {@code age} ticks in: an arc for the tornado kick, 0 else. */
    public double lift(double age) {
        if (this != TORNADO_KICK || age <= JUMP_FROM || age >= JUMP_TO) {
            return 0.0;
        }
        double u = (age - JUMP_FROM) / (JUMP_TO - JUMP_FROM);
        return JUMP_HEIGHT * 4.0 * u * (1.0 - u);
    }

    /** 4.1.7: a string's strikes, in order. */
    public static PaleStrike[] of(Style style) {
        return java.util.Arrays.stream(values()).filter(strike -> strike.style == style).toArray(PaleStrike[]::new);
    }

    /** RC a strike costs (before the original's own scaling by mastery). */
    public float cost() {
        return this.finisher() ? 1.2F : 0.3F + 0.08F * this.place();
    }

    /** 4.1.7: its place in its own string (0: the first). */
    public int place() {
        return this.ordinal() - of(this.style)[0].ordinal();
    }

    /** The strike after {@code previous} (null: the first) that {@code mastery} allows; after the last one, the first again. */
    public static PaleStrike next(PaleStrike previous, int mastery) {
        return next(previous, mastery, Style.BLITZ);
    }

    /** 4.1.7: the same in {@code style}'s string (a strike of another string before it: the first). */
    public static PaleStrike next(PaleStrike previous, int mastery, Style style) {
        PaleStrike[] all = of(style);
        int from = previous == null || previous.style != style ? 0 : previous.place() + 1;
        if (from >= all.length || all[from].mastery > mastery) {
            return all[0];
        }
        return all[from];
    }

    public static PaleStrike byOrdinal(int ordinal) {
        PaleStrike[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : JAB;
    }

    /** How many strikes the blitz has at {@code mastery}. */
    public static int count(int mastery) {
        return count(mastery, Style.BLITZ);
    }

    /** 4.1.7: how many {@code style}'s string has at {@code mastery}. */
    public static int count(int mastery, Style style) {
        int count = 0;
        for (PaleStrike strike : of(style)) {
            if (strike.mastery <= mastery) {
                count++;
            }
        }
        return count;
    }
}
