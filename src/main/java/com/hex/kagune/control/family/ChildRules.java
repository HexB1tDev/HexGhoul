package com.hex.kagune.control.family;

/**
 * 1.3.10: what a child does (pure): its fighting, taught a lesson a day (child and teen only), how hard it hits by stage and skill; how
 * close it keeps when it follows; where a carried child is held (in front of the chest, a player's size).
 */
public final class ChildRules {
    /**
     * 2.3.0 (the owner: "The daughter should be variating. They should be the size of their mother, basically"): how far a grown daughter's
     * height may be from her mother's, either way (her own: sisters aren't all the same height).
     */
    public static final double DAUGHTER_SPREAD = 0.04;

    /** 2.3.0: a grown daughter's size (a player's 1): her mother's ({@code mother}), give or take {@link #DAUGHTER_SPREAD}, by {@code seed}. */
    public static float daughter(float mother, long seed) {
        double v = (Math.floorMod(seed, 81L) - 40L) / 40.0 * DAUGHTER_SPREAD;
        return (float)Math.max(0.7, Math.min(1.05, mother * (1.0 + v)));
    }

    /** 2.3.0: a girl's size drawn at {@code stage}: grown, her own ({@code grown}); a teen, never taller than she will be grown. */
    public static float girl(ChildStage stage, float grown) {
        return stage == ChildStage.ADULT ? grown : stage == ChildStage.TEEN ? Math.min(stage.draw, grown) : stage.draw;
    }

    /** The most it can be taught; a lesson a day at most. */
    public static final int SKILL_MOST = 5;
    public static final long LESSON_EVERY = ChildStage.DAY;
    /** It follows when this far (blocks), is brought along when this far, and stops this close. */
    public static final double FOLLOW_FROM = 4.0;
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
    /** 1.3.21: hand in hand, walking at their left (on its own feet; its hand in theirs). */
    public static final int RIDE_HAND = 3;
    /**
     * 1.3.21: hand in hand: the parent's left arm by its child's stage and its own size (z out, x forward; a player's model,
     * ChildMomentPose.handParent): a little out to its hand; further out (its hand higher) if its child's arm could not reach down to it
     * (a teen at a smaller parent's side).
     */
    public static float handParentZ(ChildStage stage, double parent) {
        float z = switch (stage) {
            case BABY, TODDLER -> -0.2F;
            case CHILD -> -0.117F;
            case TEEN, ADULT -> -0.183F;
        };
        for (int i = 0; i < 16 && !reaches(stage, parent, z); i++) {
            z -= 0.03F;
        }
        return z;
    }

    /** True if its arm (wide: the higher shoulder of the two) reaches down to their hand with theirs out {@code z}. */
    static boolean reaches(ChildStage stage, double parent, float z) {
        double[] h = turn(new double[] {1.0, 10.0, 0.0}, handParentX(stage), z);
        double handH = (24.0 - 2.0 - h[1]) * PLAYER_PIXEL * parent;
        double part = stage.young ? 0.5 : 1.0;
        double kpx = stage.draw / 16.0;
        double shoulderH = (24.0 - (stage.young ? 13.0 : 2.0)) * kpx;
        // Down to the hand with room to spare (its arm a little out from its side, not hanging straight past them).
        return shoulderH - handH <= 0.97 * Math.hypot(1.0, 10.0) * part * kpx;
    }

    public static float handParentX(ChildStage stage) {
        return stage.older() ? -0.15F : -0.06F;
    }

    /** 1.3.21: hand in hand: how far to its parent's left it walks (blocks), and its right arm (z out to them, x forward). */
    public record Hand(double side, float armZ, float armX) {
    }

    /** How far apart a player's model's pixels are drawn (PlayerRenderer: 0.9375 of a sixteenth of a block). */
    static final double PLAYER_PIXEL = 0.9375 / 16.0;

    /**
     * 1.3.21: hand in hand, solved on the real models so the two hands meet: the parent's left hand where its arm puts it (a player's
     * model at {@code parent} size, {@link #handParentZ}/{@link #handParentX}); the child at its stage's size and layout (a baby's and a
     * toddler's arms half size at its sides, ChildRenderer.little; {@code slim}: its skin's slim arms), its right arm pointed at that hand
     * (as far forward as it is), and it walking just far enough to their left for its arm to reach it. A toddler's short arm goes up to
     * the hand; a child's and a teen's down and out.
     */
    public static Hand hand(ChildStage stage, double parent, boolean slim) {
        // Their hand: the end of their left arm (model pixels: pivot (5, 2), the hand's middle (1, 10) from it), turned z after x.
        double[] h = turn(new double[] {1.0, 10.0, 0.0}, handParentX(stage), handParentZ(stage, parent));
        double px = PLAYER_PIXEL * parent;
        double handU = (5.0 + h[0]) * px;
        double handH = (24.0 - 2.0 - h[1]) * px;
        double handF = -h[2] * px;
        // Its right arm: its pivot and its hand's middle from it (a little one's at half size), in its pixels.
        boolean little = stage.young;
        double part = little ? 0.5 : 1.0;
        double kpx = stage.draw / 16.0;
        double pivotX = little ? -2.5 : -5.0;
        double pivotY = little ? 13.0 : slim ? 2.5 : 2.0;
        double c = (slim ? 0.5 : 1.0) * part;
        double reach = 10.0 * part;
        // As far forward as their hand (x first), the rest of its arm across to it.
        double sinX = Math.max(-0.9, Math.min(0.9, handF / (reach * kpx)));
        float armX = (float)-Math.asin(sinX);
        double down = reach * Math.cos(armX);
        double frontal = Math.hypot(c, down) * kpx;
        double shoulderH = (24.0 - pivotY) * kpx;
        double dh = handH - shoulderH;
        double across = Math.sqrt(Math.max(0.0, frontal * frontal - dh * dh));
        double side = handU + across - pivotX * kpx;
        // The turn (z) taking its arm's own line (-c, down) onto the line from its shoulder to their hand, both in its pixels (y down).
        double toX = -across / kpx;
        double toY = -dh / kpx;
        double armZ = Math.atan2(toY, toX) - Math.atan2(down, -c);
        armZ = Math.atan2(Math.sin(armZ), Math.cos(armZ)); // the short way round (-pi..pi): blended, it swings straight there
        return new Hand(side, (float)armZ, armX);
    }

    /** A model point turned as a model part turns it (x, then z: ModelPart's ZYX with no y turn), in pixels, y down. */
    static double[] turn(double[] q, double x, double z) {
        double y1 = q[1] * Math.cos(x) - q[2] * Math.sin(x);
        double z1 = q[1] * Math.sin(x) + q[2] * Math.cos(x);
        return new double[] {q[0] * Math.cos(z) - y1 * Math.sin(z), q[0] * Math.sin(z) + y1 * Math.cos(z), z1};
    }

    /** Where it walks hand in hand at a player's side (blocks to their left). */
    public static double handSide(ChildStage stage) {
        return hand(stage, 1.0, false).side();
    }
    public static final double SHOULDERS_UP = 1.42;
    public static final double SHOULDERS_BACK = 0.24;
    public static final double SPIN_AHEAD = 0.82;
    public static final double SPIN_UP = 1.02;

    /** 1.3.21: true if a child at {@code stage} can walk hand in hand (a baby can't walk). */
    public static boolean hand(ChildStage stage) {
        return stage != ChildStage.BABY;
    }

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

    /**
     * Whether it fights; otherwise it runs to its parent. 2.5.4 (the owner: "Sons/Daughters should also attack mobs if they get close and on
     * the line of sight"): any child old enough to be taught fights, taught or not (a lesson makes its blows harder, {@link #damage}); a
     * baby or a toddler still runs.
     */
    public static boolean fights(ChildStage stage, int skill) {
        return stage.learns();
    }

    /** 2.5.4: how close a monster has to come for it to go for it, if it can see it (blocks). */
    public static final double SPOTS = 8.0;

    /**
     * 2.5.4: whether it goes for a monster {@code distance} blocks off that isn't after its family: close ({@link #SPOTS}) and in its line of
     * sight ({@code seen}); never one behind a wall.
     */
    public static boolean spots(double distance, boolean seen) {
        return seen && distance <= SPOTS;
    }

    /** What its blow deals. */
    public static float damage(ChildStage stage, int skill, boolean ghoul) {
        if (!fights(stage, skill)) {
            return 0.0F;
        }
        float base = stage.older() ? 3.0F + 1.4F * skill : 1.5F + 0.8F * skill;
        return ghoul ? base * 1.5F : base;
    }

    /**
     * 1.3.21: the owner: "the stronger the father or the mother, the stronger the child". How strong it is born (0 a weak couple's .. 1 the
     * strongest's), from its parents' own strength (each 0..1): the stronger parent counts most, the other still counts.
     */
    public static float might(float mother, float father) {
        float strong = Math.max(mother, father);
        float weak = Math.min(mother, father);
        return Math.max(0.0F, Math.min(1.0F, 0.65F * strong + 0.35F * weak));
    }

    /** 1.3.21: its health at {@code stage}, born {@code might} strong (up to two and a half times a weak couple's child's). */
    public static float health(ChildStage stage, float might) {
        return stage.health * (1.0F + 1.5F * Math.max(0.0F, Math.min(1.0F, might)));
    }

    /**
     * 1.3.47 (the owner: "the stronger the parents, the stronger the child... father max mastery, mother 360 HP: the kid 400"): how much
     * health it grows into: the stronger parent's health ({@code motherHealth}, {@code fatherHealth}: a companion's own, a player's most),
     * and up to {@link #PARENT_BONUS} more for the other parent's strength ({@code ...Might}, 0..1: a player's mastery, ChildRules.playerMight).
     */
    public static float grownHealth(float motherHealth, float motherMight, float fatherHealth, float fatherMight) {
        boolean mother = motherHealth >= fatherHealth;
        float strong = Math.max(1.0F, Math.max(motherHealth, fatherHealth));
        float other = Math.max(0.0F, Math.min(1.0F, mother ? fatherMight : motherMight));
        return strong * (1.0F + PARENT_BONUS * other);
    }

    /** 1.3.47: the most the other parent's strength adds to a child's grown health (a share of the stronger's). */
    public static final float PARENT_BONUS = 0.14F;

    /** 1.3.47: how much of its grown health it has at {@code stage}: a quarter as a baby, all of it as a teen. */
    public static float share(ChildStage stage) {
        return switch (stage) {
            case BABY -> 0.25F;
            case TODDLER -> 0.4F;
            case CHILD -> 0.65F;
            case TEEN, ADULT -> 1.0F;
        };
    }

    /**
     * 1.3.47: its health at {@code stage}: its share of what it grows into ({@code grown}, {@link #grownHealth}; not known yet: 0), never
     * less than 1.3.21's ({@link #health(ChildStage, float)}).
     */
    public static float health(ChildStage stage, float might, float grown) {
        float before = health(stage, might);
        return grown <= 0.0F ? before : Math.max(before, grown * share(stage));
    }

    /** 1.3.47: how long after it was last hurt it starts to heal (ticks), and how much a second (a ghoul's in two minutes, a human's in ten). */
    public static final int HEAL_AFTER = 100;

    public static float healing(float most, boolean ghoul) {
        return most / (ghoul ? 120.0F : 600.0F);
    }

    /** 1.3.21: what its blow deals, born {@code might} strong (up to more than twice as hard). */
    public static float damage(ChildStage stage, int skill, boolean ghoul, float might) {
        return damage(stage, skill, ghoul) * (1.0F + 1.25F * Math.max(0.0F, Math.min(1.0F, might)));
    }

    /**
     * 1.3.21: how strong a player is as a parent (0..1): their kagune's mastery (out of 1000), more for a final kakuja chosen; a human with
     * no kagune is weak.
     */
    public static float playerMight(boolean ghoul, int mastery, boolean finalKakuja) {
        if (!ghoul) {
            return 0.15F;
        }
        float m = 0.2F + 0.6F * Math.max(0.0F, Math.min(1.0F, mastery / 1000.0F));
        return Math.min(1.0F, m + (finalKakuja ? 0.2F : 0.0F));
    }

    /** Ticks between its blows. */
    public static int cooldown(int skill) {
        return Math.max(8, 22 - 2 * Math.max(0, skill));
    }
}
