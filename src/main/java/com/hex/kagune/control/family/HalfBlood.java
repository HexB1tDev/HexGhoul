package com.hex.kagune.control.family;

/**
 * 2.2.1 (pure, tested; the owner: "if a kid is conceived with a human (70% chance of it dying and pregnancy failing) a one eyed ghoul child
 * will be born, gaining a significant power/health and speed boost, the child can use Arima's dashes and speed, along with the Parent's
 * Kagune (since one parent is a ghoul it will inherit it fully from that person)"): a child of a ghoul and a human. Most are lost before
 * they are born ({@link #LOSS}, somewhere in the first half of the pregnancy: {@link #lostAt}); the one that lives is a one-eyed ghoul
 * ({@link Genome#half}), the ghoul parent's kagune whole, far stronger, harder to kill and quicker than any other child, and it dashes as
 * the White Reaper does ({@code ReaperDash}).
 */
public final class HalfBlood {
    private HalfBlood() {
    }

    /** The chance a half-human pregnancy is lost. */
    public static final double LOSS = 0.7;
    /** Lost somewhere between these shares of the term. */
    public static final double LOST_FROM = 0.15;
    public static final double LOST_TO = 0.55;
    /** What it has over any other child: its health, its blows, its pace (times). */
    public static final float HEALTH = 1.6F;
    public static final float DAMAGE = 1.5F;
    public static final double SPEED = 1.3;
    /** Its dash in a fight: closing a gap of at least {@link #DASH_FROM} blocks (up to {@link #DASH_TO}), or out of the way of a blow. */
    public static final double DASH_FROM = 3.5;
    public static final double DASH_TO = 14.0;

    /**
     * 2.2.2 (the owner: "the kid when in adult form, can use that kagune's Kakuja (Ukaku: first kakuja, Rinkaku: Centipede legs + normal
     * kagune form)"): true while a half-human child ({@code genome}) is grown ({@code stage}) and fighting with its kagune out ({@code out}):
     * its kagune's kakuja on it.
     */
    public static boolean kakuja(Genome genome, ChildStage stage, boolean out) {
        return genome != null && genome.half() && stage == ChildStage.ADULT && out;
    }

    /** Its kakuja's centipede legs (a Rinkaku child's): how many, how far they strike (blocks), how often (ticks), how hard (its blows times). */
    public static final int LEGS = 2;
    public static final double LEG_REACH = 5.5;
    public static final int LEG_EVERY = 28;
    public static final float LEG_DAMAGE = 1.4F;
    /** Its Ukaku kakuja's blows, times. */
    public static final float WING_KAKUJA = 1.3F;

    /** True if a child of {@code mother} and {@code father} is half-human: one a ghoul, the other not. */
    public static boolean half(Genome.Parent mother, Genome.Parent father) {
        return mother.ghoul() != father.ghoul();
    }

    /** Where (a share of the term) a half-human pregnancy is lost, from two rolls in [0, 1): -1 if it is carried to term. */
    public static double lostAt(double roll, double when) {
        return roll < LOSS ? LOST_FROM + Math.max(0.0, Math.min(1.0, when)) * (LOST_TO - LOST_FROM) : -1.0;
    }
}
