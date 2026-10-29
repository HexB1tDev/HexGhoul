package com.hex.kagune.control.king;

/**
 * 1.0.14: War's M1 (pure): six blows chained click by click, fists, feet and the bladed arm, each thrown with the whole body, as
 * Pale's blitz is: a click during a blow is kept and thrown the moment that blow reaches its chain tick; a click after one, within
 * {@link #CHAIN_WINDOW}, carries on the chain; later, it starts over with the jab. After the last he needs {@link #RECOVERY} before
 * the next. He locks onto what he clicked (or the nearest body in front within {@link #STEP_IN_RANGE}) and closes the gap in the
 * wind-up, so the blow lands on the impact tick.
 *
 * <p>Each blow: its length, impact tick and the tick the next may cut its recovery short from, damage (of {@link #BASE}), how hard
 * and which way it throws the target (back, to his right (negative: left), up), its reach (blocks from his eyes to the near side of
 * a body), what it catches (in front, a wide arc, all round), and what does the hitting.
 */
public enum WarStrike {
    /** The free (left) hand's jab, snapped straight out of the stance. */
    JAB(8, 3, 5, 0.8F, 0.25, 0.0, 0.05, 3.4, Arc.FRONT, Kind.FIST),
    /** The bladed arm swept across in front of him, right to left: it throws them to his left. */
    REAP(10, 4, 6, 1.1F, 0.2, -0.45, 0.08, 4.4, Arc.WIDE, Kind.BLADE),
    /** Turned side-on, the right foot driven out flat into them: it throws them well back. */
    SIDE_KICK(10, 4, 6, 1.0F, 0.95, 0.0, 0.12, 3.8, Arc.FRONT, Kind.KICK),
    /** The blade brought up from low on his left to high on his right: it lifts them off their feet. */
    RISING(11, 4, 7, 1.2F, 0.15, 0.0, 0.62, 4.2, Arc.FRONT, Kind.BLADE),
    /** A full turn on the spot into the left heel: it catches everything round him and throws it to his right. */
    HEEL(12, 6, 8, 1.25F, 0.35, 0.55, 0.15, 3.8, Arc.ALL, Kind.KICK),
    /**
     * King's Verdict: the blade raised and brought down on them with all his weight while four rinkaku drive up out of the ground
     * through them; everything near it is caught.
     */
    VERDICT(18, 8, 18, 2.0F, 1.1, 0.0, 0.38, 4.6, Arc.FRONT, Kind.VERDICT),
    // 1.0.20: the fighting grammar's blows (KingCombo): the launcher, the spike, and the heavy's three.
    /** Ascension, the launcher: crouched, the blade hooked in under them from low and torn up high, throwing them into the air. */
    ASCENSION(12, 5, 12, 1.3F, 0.1, 0.0, KingCombo.LAUNCH_UP, 4.2, Arc.FRONT, Kind.BLADE),
    /** Downfall, the spike: in the air, the blade raised over his head and brought down on them, driving them into the ground. */
    DOWNFALL(12, 4, 12, 1.6F, 0.15, 0.0, -KingCombo.SPIKE_DOWN, 4.4, Arc.WIDE, Kind.BLADE),
    /** Cleaver, the heavy: the bladed arm raised high and brought down with his whole weight in it; it breaks a guard. */
    CLEAVER(15, 8, 15, 1.7F, 0.95, 0.0, 0.25, 4.4, Arc.FRONT, Kind.BLADE),
    /** Black Calamity, the charged heavy: the blade and four rinkaku slammed into the ground before him, all there thrown up. */
    CALAMITY(20, 6, 20, 2.6F, 0.55, 0.0, 0.95, 5.6, Arc.WIDE, Kind.VERDICT),
    /** Rend, the ender: his rinkaku driven through them and flung out, throwing them far, harder for every blow before it. */
    REND(14, 6, 14, 1.4F, 2.1, 0.0, 0.45, 4.8, Arc.FRONT, Kind.BLADE);

    /** 1.0.20: the blows thrown one after another in the air (KingCombo.AIR_HITS of them, then {@link #DOWNFALL}). */
    static final WarStrike[] AIR = {JAB, REAP, SIDE_KICK};

    /** What a blow catches: what is in front of him ({@link #FRONT_COS}), a wider arc ({@link #WIDE_COS}), or all round. */
    public enum Arc {
        FRONT, WIDE, ALL
    }

    /** What does the hitting: a fist, a foot, the bladed arm (which bleeds what it cuts), the finisher. */
    public enum Kind {
        FIST, KICK, BLADE, VERDICT
    }

    /** Damage of a blow at 1.0 (hearts x2). */
    public static final float BASE = 6.0F;
    /** A click this long after a blow ends still carries the chain on (ticks). */
    public static final int CHAIN_WINDOW = 12;
    /** After the Verdict, the next blow waits this long (ticks). */
    public static final int RECOVERY = 10;
    /** A target farther than the blow's reach but within this is stepped in to (blocks), at no more than this a tick. */
    public static final double STEP_IN_RANGE = 6.0;
    public static final double STEP_IN_SPEED = 1.0;
    /** The cones in front of him blows catch in. */
    public static final double FRONT_COS = Math.cos(Math.toRadians(58.0));
    public static final double WIDE_COS = Math.cos(Math.toRadians(80.0));
    /** How long a blow stuns (ticks): a blow, a heavy one (the rising slash, the heel, the Verdict). */
    public static final int STUN = 8;
    public static final int HEAVY_STUN = 22;
    /** What the blade's cuts leave bleeding (ticks), and the Verdict's. */
    public static final int BLEED = 60;
    public static final int VERDICT_BLEED = 120;
    /** The Verdict's rinkaku also catch everything within this of where they come up (blocks). */
    public static final double VERDICT_RADIUS = 2.6;

    public final int duration;
    public final int impact;
    /** From this tick a queued blow starts at once, out of this one's hold (its recovery is never seen). */
    public final int chain;
    public final float damage;
    public final double back;
    public final double sideways;
    public final double up;
    public final double reach;
    public final Arc arc;
    public final Kind kind;

    WarStrike(int duration, int impact, int chain, float damage, double back, double sideways, double up, double reach, Arc arc, Kind kind) {
        this.duration = duration;
        this.impact = impact;
        this.chain = chain;
        this.damage = damage;
        this.back = back;
        this.sideways = sideways;
        this.up = up;
        this.reach = reach;
        this.arc = arc;
        this.kind = kind;
    }

    /** The last blow of the chain: the recovery follows it (1.0.20: and after the spike and the heavy's, the chain begins again). */
    public boolean finisher() {
        return this == VERDICT || this == DOWNFALL || this == CLEAVER || this == CALAMITY || this == REND;
    }

    /** 1.0.20: one of the fighting grammar's blows, not one of the chain's. */
    public boolean special() {
        return this.ordinal() > VERDICT.ordinal();
    }

    /** It stuns for {@link #HEAVY_STUN}. */
    public boolean heavy() {
        return this == RISING || this == HEEL || this == VERDICT || this.special();
    }

    /** 1.0.20: it breaks a guard (a raised shield knocked aside). */
    public boolean breaksGuard() {
        return this == CLEAVER || this == CALAMITY || this == REND;
    }

    /** 1.0.20: the {@code n}th air blow (from 0), or the spike once there have been {@link KingCombo#AIR_HITS}. */
    public static WarStrike air(int n) {
        return n >= KingCombo.AIR_HITS ? DOWNFALL : AIR[Math.max(0, n) % AIR.length];
    }

    /** What it does to a body hit: damage. */
    public float hit() {
        return BASE * this.damage;
    }

    /** True if a body {@code cos} (the cosine of the angle between where he faces and the way to it) is in what this blow catches. */
    public boolean catches(double cos) {
        return switch (this.arc) {
            case FRONT -> cos >= FRONT_COS;
            case WIDE -> cos >= WIDE_COS;
            case ALL -> true;
        };
    }

    /** The blow after {@code previous} (null: the first); after the Verdict, the jab again. */
    public static WarStrike next(WarStrike previous) {
        if (previous == null || previous.finisher() || previous.special()) {
            return JAB;
        }
        return values()[previous.ordinal() + 1];
    }

    /** What the chain goes on with: the next blow if {@code previous} ended no more than {@link #CHAIN_WINDOW} ago, else the jab. */
    public static WarStrike after(WarStrike previous, long endedAgo) {
        return previous != null && endedAgo <= CHAIN_WINDOW ? next(previous) : JAB;
    }

    public static WarStrike byOrdinal(int ordinal) {
        WarStrike[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : JAB;
    }

    /** How far from a body's centre (blocks, level) he stops stepping in: a fist and a foot land a block out, the blade further. */
    public double stand(double targetWidth) {
        return (this.kind == Kind.BLADE || this.kind == Kind.VERDICT ? 1.7 : 1.1) + targetWidth * 0.5;
    }
}
