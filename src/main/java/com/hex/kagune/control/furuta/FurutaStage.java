package com.hex.kagune.control.furuta;

/**
 * Furuta DNA's kagune as it grows (pure, tested; 7.0.0). Everything comes out of one organ, Rize's kakuhou transplanted into a
 * half-human body: first one muscular rinkaku tail from the lower back (a second at {@link #TWIN_MASTERY}), then three armored
 * centipedes (one with a mouth), four thorned giants, the many-eyed black mass, and the kakuja: living armor over the body, a
 * monstrous head and two colossal centipedes. The kakuja is never a stage the kagune rests in: K turns it on and off.
 *
 * <p>Sizes are in blocks; a limb's length and half-width at its thickest, growing a little with mastery inside its stage.
 */
public enum FurutaStage {
    /** Rize's transplanted kakuhou: one tail, two at {@link #TWIN_MASTERY}. Fast and precise (his first reveal). */
    TRANSPLANT("transplant", "Rize's Kakuhou", 0, 3.9, 4.7, 0.37),
    /** Three armored centipedes; the first has a toothed mouth. */
    CENTIPEDE("centipede", "Centipede Rinkaku", 250, 4.8, 5.4, 0.33),
    /** Four enormous thorn-covered tails, far bigger than he is. */
    THORNS("thorns", "Four Thorns", 450, 6.4, 7.2, 0.38),
    /** Vast thick crescents full of eyes, stacked one on another into one mass out to his side. */
    DRAGON("dragon", "Many-Eyed Kagune", 700, 7.6, 8.6, 0.46),
    /** The kakuja (K): living armor over his body, the monstrous head and two colossal centipedes. */
    KAKUJA("kakuja", "Kakuja", 900, 9.4, 10.6, 0.62);

    /** The second tail of the transplant grows in at this mastery. */
    public static final int TWIN_MASTERY = 100;

    private final String id;
    private final String displayName;
    private final int mastery;
    private final double shortest;
    private final double longest;
    private final double width;

    FurutaStage(String id, String displayName, int mastery, double shortest, double longest, double width) {
        this.id = id;
        this.displayName = displayName;
        this.mastery = mastery;
        this.shortest = shortest;
        this.longest = longest;
        this.width = width;
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    /** The mastery this stage unlocks at. */
    public int mastery() {
        return this.mastery;
    }

    public boolean kakuja() {
        return this == KAKUJA;
    }

    public boolean unlocked(int mastery) {
        return mastery >= this.mastery;
    }

    /** How many limbs grow out at {@code mastery}. */
    public int limbs(int mastery) {
        return switch (this) {
            case TRANSPLANT -> mastery >= TWIN_MASTERY ? 2 : 1;
            case CENTIPEDE -> 3;
            case THORNS -> 4;
            case DRAGON -> 5;
            case KAKUJA -> 2;
        };
    }

    /** A limb's length at rest (blocks), growing with mastery from the stage's unlock to the next. */
    public double length(int mastery) {
        return this.shortest + (this.longest - this.shortest) * grownIn(mastery);
    }

    /** A limb's half-width where it is thickest (blocks). */
    public double width(int mastery) {
        return this.width * (0.92 + 0.08 * grownIn(mastery));
    }

    /** 0 at this stage's unlock, 1 by the next stage's (or mastery 1000 for the last two). */
    double grownIn(int mastery) {
        int next = this.ordinal() + 1 < values().length && this != DRAGON ? values()[this.ordinal() + 1].mastery : 1000;
        double span = Math.max(1.0, next - this.mastery);
        return Math.max(0.0, Math.min(1.0, (mastery - this.mastery) / span));
    }

    /** Damage multiplier of every blow in this stage (the kagune's mass behind it). */
    public float damage() {
        return switch (this) {
            case TRANSPLANT -> 1.0F;
            case CENTIPEDE -> 1.18F;
            case THORNS -> 1.36F;
            case DRAGON -> 1.52F;
            case KAKUJA -> 1.75F;
        };
    }

    /** The highest base stage (never the kakuja) unlocked at {@code mastery}. */
    public static FurutaStage highest(int mastery) {
        FurutaStage best = TRANSPLANT;
        for (FurutaStage stage : values()) {
            if (!stage.kakuja() && stage.unlocked(mastery)) {
                best = stage;
            }
        }
        return best;
    }

    /** The stage a player asked for, held to what their mastery allows (never the kakuja: K does that). */
    public static FurutaStage usable(FurutaStage wanted, int mastery) {
        if (wanted == null || wanted.kakuja() || !wanted.unlocked(mastery)) {
            return highest(mastery);
        }
        return wanted;
    }

    /** The next unlocked base stage after {@code from}, round to the first. */
    public static FurutaStage next(FurutaStage from, int mastery) {
        FurutaStage[] all = values();
        for (int step = 1; step <= all.length; step++) {
            FurutaStage stage = all[Math.floorMod(from.ordinal() + step, all.length)];
            if (!stage.kakuja() && stage.unlocked(mastery)) {
                return stage;
            }
        }
        return TRANSPLANT;
    }

    public static FurutaStage byOrdinal(int ordinal) {
        FurutaStage[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : TRANSPLANT;
    }

    public static FurutaStage fromId(String id) {
        for (FurutaStage stage : values()) {
            if (stage.id.equals(id)) {
                return stage;
            }
        }
        return null;
    }
}
