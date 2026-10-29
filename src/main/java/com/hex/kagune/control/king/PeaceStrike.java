package com.hex.kagune.control.king;

/**
 * 1.0.15: Peace's M1 (pure): the cross blades strike out in front of him, switching every blow (1.0.16: four blows, each its own:
 * the lance, the sweep, the scissor, the crucifix; 1.0.18: of the six blades), thrusting and slashing, out to several blocks; chained click by click as War's blows are ({@link WarStrike#CHAIN_WINDOW}). Each blow: its length,
 * the tick it lands, the tick the next may start from, the blades that strike ({@link PeaceMotion} blade numbers) and how, its damage
 * (of {@link #BASE}) and whether it bleeds. The hits follow the blades' own paths ({@link PeaceMotion#blade}).
 */
public enum PeaceStrike {
    /** Lance: MR drawn back beside his right shoulder, coiled, then driven straight out at the target. */
    LANCE(10, 5, 7, new int[] {PeaceMotion.MR}, Way.THRUST, 1.0F, false),
    /** Sweep: ML out to his left, then flat across in front of him to his right, a scythe. */
    SWEEP(11, 5, 8, new int[] {PeaceMotion.ML}, Way.SLASH, 1.1F, true),
    /** Scissor: the upper and the lower pairs swung out wide, then closed through the front of him, crossing high and low. */
    SCISSOR(12, 6, 8, new int[] {PeaceMotion.UR, PeaceMotion.UL, PeaceMotion.LR, PeaceMotion.LL}, Way.SLASH, 1.25F, true),
    /** Crucifix: all six out round his front like a star's points, then driven in together on one point. */
    CRUCIFIX(18, 9, 18, new int[] {PeaceMotion.UR, PeaceMotion.UL, PeaceMotion.MR, PeaceMotion.ML, PeaceMotion.LR, PeaceMotion.LL}, Way.THRUST, 2.0F,
        true),
    // 1.0.20: the fighting grammar's blows (KingCombo): the launcher, the spike, and the heavy's three.
    /** Ascend, the launcher: the great pair low before him, then scooped up through them and high, throwing them into the air. */
    ASCEND(13, 5, 13, new int[] {PeaceMotion.MR, PeaceMotion.ML}, Way.SLASH, 1.3F, true),
    /** Judgment, the spike: in the air, the upper and great pairs raised over him, then driven down through them into the ground. */
    JUDGMENT(13, 5, 13, new int[] {PeaceMotion.UR, PeaceMotion.UL, PeaceMotion.MR, PeaceMotion.ML}, Way.THRUST, 1.6F, true),
    /** Cross Cleave, the heavy: the great pair raised wide, then crossed down through them in an X; it breaks a guard. */
    CROSS_CLEAVE(15, 7, 15, new int[] {PeaceMotion.MR, PeaceMotion.ML}, Way.SLASH, 1.7F, true),
    /** Seraph Thrust, the charged heavy: all six drawn back behind him, then driven out together along the aim, far, in a column. */
    SERAPH(20, 8, 20, new int[] {PeaceMotion.UR, PeaceMotion.UL, PeaceMotion.MR, PeaceMotion.ML, PeaceMotion.LR, PeaceMotion.LL}, Way.THRUST, 2.6F, true),
    /** Halo Rend, the ender: each side's three swept round its half of him in a ring, flinging what they cut away from him. */
    HALO(16, 6, 16, new int[] {PeaceMotion.UR, PeaceMotion.UL, PeaceMotion.MR, PeaceMotion.ML, PeaceMotion.LR, PeaceMotion.LL}, Way.SLASH, 1.4F, true);

    /** 1.0.20: the blows thrown one after another in the air (KingCombo.AIR_HITS of them, then {@link #JUDGMENT}). */
    static final PeaceStrike[] AIR = {LANCE, SWEEP, SCISSOR};

    /** How a blade strikes: straight out and back, or swept through an arc. */
    public enum Way {
        THRUST, SLASH
    }

    /** Damage of a blow at 1.0. */
    public static final float BASE = 5.0F;
    /** How long a blow stuns, the finisher's; how long a blade's cut bleeds. */
    public static final int STUN = 6;
    public static final int FINISH_STUN = 20;
    public static final int BLEED = 60;
    /** How far round a blade's heart a body is cut (blocks): the blades are a block and more across. */
    public static final double CUT_RADIUS = 1.0;

    public final int duration;
    public final int impact;
    public final int chain;
    private final int[] blades;
    public final Way way;
    public final float damage;
    public final boolean bleeds;

    PeaceStrike(int duration, int impact, int chain, int[] blades, Way way, float damage, boolean bleeds) {
        this.duration = duration;
        this.impact = impact;
        this.chain = chain;
        this.blades = blades;
        this.way = way;
        this.damage = damage;
        this.bleeds = bleeds;
    }

    /** The blades that strike in this blow. */
    public int[] blades() {
        return this.blades.clone();
    }

    /** True if blade {@code blade} strikes in this blow. */
    public boolean uses(int blade) {
        for (int b : this.blades) {
            if (b == blade) {
                return true;
            }
        }
        return false;
    }

    public boolean finisher() {
        return this == CRUCIFIX || this == JUDGMENT || this == CROSS_CLEAVE || this == SERAPH || this == HALO;
    }

    /** 1.0.20: one of the fighting grammar's blows, not one of the chain's. */
    public boolean special() {
        return this.ordinal() > CRUCIFIX.ordinal();
    }

    /** 1.0.20: it breaks a guard (a raised shield knocked aside). */
    public boolean breaksGuard() {
        return this == CROSS_CLEAVE || this == SERAPH || this == HALO;
    }

    /** 1.0.20: the {@code n}th air blow (from 0), or the spike once there have been {@link KingCombo#AIR_HITS}. */
    public static PeaceStrike air(int n) {
        return n >= KingCombo.AIR_HITS ? JUDGMENT : AIR[Math.max(0, n) % AIR.length];
    }

    public float hit() {
        return BASE * this.damage;
    }

    /** The ticks its blades cut in: about the impact tick (the sweep all through its arc); a body in their way then is hit. */
    public boolean cutting(double age) {
        return switch (this) {
            case SWEEP -> age >= this.impact - 2 && age <= this.impact + 2;
            case ASCEND, LANCE -> age >= this.impact - 2.5 && age <= this.impact + 1;
            case HALO -> age >= this.impact - 3 && age <= this.impact + 3;
            case SERAPH -> age >= this.impact - 2 && age <= this.impact + 2;
            case CROSS_CLEAVE -> age >= this.impact - 1.5 && age <= this.impact + 1.5;
            default -> age >= this.impact - 1.5 && age <= this.impact + 1;
        };
    }

    /** The blow after {@code previous} (null: the first); after the finisher, the first again. */
    public static PeaceStrike next(PeaceStrike previous) {
        return previous == null || previous.finisher() || previous.special() ? LANCE : values()[previous.ordinal() + 1];
    }

    /** What the chain goes on with: the next blow if {@code previous} ended no more than the window ago, else the first. */
    public static PeaceStrike after(PeaceStrike previous, long endedAgo) {
        return previous != null && endedAgo <= WarStrike.CHAIN_WINDOW ? next(previous) : LANCE;
    }

    public static PeaceStrike byOrdinal(int ordinal) {
        PeaceStrike[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : LANCE;
    }
}
