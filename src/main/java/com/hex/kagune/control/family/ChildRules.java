package com.hex.kagune.control.family;

/**
 * 1.3.10: what a child does (pure): its fighting, taught a lesson a day (child and teen only), how hard it hits by stage and skill; how
 * close it keeps when it follows; where a carried child is held (in front of the chest, a player's size).
 */
public final class ChildRules {
    /** The most it can be taught; a lesson a day at most. */
    public static final int SKILL_MOST = 5;
    public static final long LESSON_EVERY = ChildStage.DAY;
    /** It follows when this far (blocks), is brought along when this far, and stops this close. */
    public static final double FOLLOW_FROM = 4.0;
    public static final double FOLLOW_TELEPORT = 24.0;
    public static final double FOLLOW_STOP = 2.0;
    /**
     * Where a carried child is held: ahead of the chest, and how high its hips sit (blocks up from the carrier's feet, a player's size).
     * 1.3.15: up at the chest on their forearms (it was held at the hip, drawn by their feet in first person), closer in.
     */
    public static final double CARRY_AHEAD = 0.34;
    public static final double CARRY_UP = 1.02;
    /**
     * 1.3.20: how it is held: in their arms ({@link #RIDE_ARMS}), up on their shoulders ({@link #RIDE_SHOULDERS}: a toddler or a child, its
     * hips at the top of their back, its legs down their front), or held out at arm's length to be spun ({@link #RIDE_SPIN}).
     */
    public static final int RIDE_ARMS = 0;
    public static final int RIDE_SHOULDERS = 1;
    public static final int RIDE_SPIN = 2;
    public static final double SHOULDERS_UP = 1.42;
    public static final double SHOULDERS_BACK = 0.24;
    public static final double SPIN_AHEAD = 0.82;
    public static final double SPIN_UP = 1.02;

    /** True if a child at {@code stage} can ride on a parent's shoulders. */
    public static boolean shoulders(ChildStage stage) {
        return stage == ChildStage.TODDLER || stage == ChildStage.CHILD;
    }

    /** How far it goes for something hostile to its family (blocks), and how close it must be to hit. */
    public static final double GUARD = 10.0;
    public static final double REACH = 1.6;

    private ChildRules() {
    }

    /** Whether it may be taught now: old enough, not yet taught all there is, a day since the last lesson. */
    public static boolean teachable(ChildStage stage, int skill, long lastLesson, long now) {
        return stage.learns() && skill < SKILL_MOST && (lastLesson == Long.MIN_VALUE || now - lastLesson >= LESSON_EVERY);
    }

    /** Whether it fights (old enough and taught at least once); otherwise it runs to its parent. */
    public static boolean fights(ChildStage stage, int skill) {
        return stage.learns() && skill > 0;
    }

    /** What its blow deals. */
    public static float damage(ChildStage stage, int skill, boolean ghoul) {
        if (!fights(stage, skill)) {
            return 0.0F;
        }
        float base = stage == ChildStage.TEEN ? 3.0F + 1.4F * skill : 1.5F + 0.8F * skill;
        return ghoul ? base * 1.5F : base;
    }

    /** Ticks between its blows. */
    public static int cooldown(int skill) {
        return Math.max(8, 22 - 2 * Math.max(0, skill));
    }
}
