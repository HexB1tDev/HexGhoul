package com.hex.kagune.control.npc;

/**
 * 1.3.19: Touka's wings in a fight (pure): her own little thing, nowhere near Ken or Eto. 1.3.29: the owner: "only one wing, not two, and
 * it should be smaller": one small wing ({@link #SIDE}, Ukaku not long grown, {@link #MASTERY}) out of her back the moment a fight finds
 * her, turned to crystal, throwing crystal spikes from it: a volley of three, a fan of five, now and then the barrage. Up close she
 * fights with what her eyes give her (strikes, the kick, flips).
 */
public final class WingRules {
    private WingRules() {
    }

    /** How grown her wing is drawn (Ukaku mastery): just past the first wings, small. */
    public static final int MASTERY = 170;
    /** Her one wing: on her right (+1); every spike leaves from it. */
    public static final int SIDE = 1;
    /** Out over, crystal after (ticks). */
    public static final int DEPLOY_TICKS = 10;
    public static final int CRYSTAL_AFTER = 12;
    /** Closer than this (blocks), her fists; farther than this, she closes in before she throws. */
    public static final double NEAR = 3.2;
    public static final double FAR = 20.0;
    /** The distance she keeps while she throws (she backs off inside it). */
    public static final double KEEP = 6.0;
    /** A spike: how fast (blocks a tick), how far, how wide it catches. */
    public static final double SPEED = 2.0;
    public static final double RANGE = 26.0;
    public static final double RADIUS = 0.35;
    /** What each kind hurts: the volley's, the fan's, the barrage's. */
    public static final float SHOT = 4.0F;
    public static final float FAN = 3.0F;
    public static final float BARRAGE = 2.0F;
    /** The volley: every how many ticks a spike, how many, the rest after. */
    public static final int SHOT_EVERY = 7;
    public static final int VOLLEY = 3;
    public static final int VOLLEY_REST = 26;
    /** The fan: how many spikes, how wide (degrees each side), how often. */
    public static final int FAN_SPIKES = 5;
    public static final double FAN_SPREAD = 14.0;
    public static final int FAN_EVERY = 140;
    /** The barrage: how many spikes, one every how many ticks, how often. */
    public static final int BARRAGE_SPIKES = 14;
    public static final int BARRAGE_EVERY_TICK = 2;
    public static final int BARRAGE_EVERY = 240;
    /** How long out of a fight before her wings go back in. */
    public static final long PUT_AWAY = 100L;

    /** What her wings do now. */
    public enum Act {
        /** Too close, or nothing to throw at: her fists and her feet (or a chase). */
        NONE,
        /** The next spike of a volley. */
        VOLLEY,
        /** A fan of spikes at once. */
        FAN,
        /** The barrage. */
        BARRAGE,
        /** Waiting on the next spike, keeping her distance. */
        HOLD
    }

    /** What she does with her wings at {@code gap} blocks off it, seeing it or not, out of what is ready; {@code roll} in [0, 1). */
    public static Act choose(double gap, boolean sees, long now, long nextShot, long fanReady, long barrageReady, double roll) {
        if (gap < NEAR || !sees || gap > FAR) {
            return Act.NONE;
        }
        if (barrageReady <= now && roll < 0.35) {
            return Act.BARRAGE;
        }
        if (fanReady <= now && roll < 0.55) {
            return Act.FAN;
        }
        return nextShot <= now ? Act.VOLLEY : Act.HOLD;
    }

    /** How far a fan's {@code i}th of {@code count} spikes is turned off its aim (degrees). */
    public static double fan(int i, int count) {
        return count <= 1 ? 0.0 : FAN_SPREAD * (2.0 * i / (count - 1) - 1.0);
    }

    /**
     * 1.3.26: a companion's wings (Touka's) as the rig draws them at {@link #MASTERY}, crystal as she throws, aiming as she shoots. 1.3.29:
     * one wing alone, on {@link #SIDE} (the other shoulder blade's grown to nothing: the rig draws nothing there).
     */
    public static com.hex.kagune.control.ukaku.WingRig.Input companion(float deploy, double time, float crystal, boolean dissolving, float aim,
                                                                       double[] at, float kickRight, float kickLeft, float detail) {
        return new com.hex.kagune.control.ukaku.WingRig.Input(com.hex.kagune.control.ukaku.WingForm.TWIN, MASTERY, deploy, time, crystal, crystal,
            com.hex.kagune.control.ukaku.WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false, detail, 1.0F, dissolving, aim, aim, at, kickRight, kickLeft,
            0.0F, 0.0F, SIDE > 0 ? 1.0F : 0.0F, SIDE > 0 ? 0.0F : 1.0F);
    }

    // ---------------------------------------------------------------- 4.1.54: Banjou's one small wing

    /**
     * 4.1.54 (the owner: "Kagune - super small blue Ukaku wing (singular) cannot shoot projectiles, only uses basic m1s with it"): Banjou's
     * wing is the twin wings as they first come in ({@link #SLAP_MASTERY}: the smallest the rig grows them), only the one on {@link #SIDE},
     * drawn smaller still about its root ({@link #SLAP_SCALE}: about half Touka's at rest); it throws nothing. His M1 is a string of the base
     * form's one-wing blows on his right, the player's own (ComboStyle.UKAKU_LEFT_WING's, mirrored): a slap, a wide swing and a rising wing,
     * each as long and landing as late as the player's, then a rest. (The overhead slam is left out: a wing this small brings its hand down
     * beside his own shoulder, short of anything in front of him; rendered and judged.)
     */
    public static final int SLAP_MASTERY = 100;
    public static final float SLAP_SCALE = 0.7F;
    public static final com.hex.kagune.control.ukaku.WingRig.Motion[] SLAPS = {com.hex.kagune.control.ukaku.WingRig.Motion.SLAP_RIGHT,
        com.hex.kagune.control.ukaku.WingRig.Motion.SWING_RIGHT, com.hex.kagune.control.ukaku.WingRig.Motion.RISE_RIGHT};
    /** The body's part in each (UkakuBody's poses of the same blows). */
    public static final com.hex.kagune.control.StrikePattern[] SLAP_PATTERNS = {com.hex.kagune.control.StrikePattern.WING_SLAP,
        com.hex.kagune.control.StrikePattern.WING_SWING, com.hex.kagune.control.StrikePattern.WING_RISE};
    /** Each blow's length (ticks) and where in it the wing lands (the player's: ComboStyle.UKAKU_LEFT_WING's). */
    public static final int[] SLAP_TICKS = {16, 20, 20};
    public static final float[] SLAP_IMPACT = {0.5F, 0.5F, 0.5F};
    /** What each hurts before his {@code Persona.wingPower} (his is 0.6: a weak man's wing). */
    public static final float[] SLAP_DAMAGE = {5.0F, 6.0F, 6.5F};
    /**
     * How far (blocks, his middle to the edge of what he hits) each blow reaches (his wing's hand lands about 0.75 in front of him, his body
     * turned into it: rendered with a body at that distance), and how close he walks in before he starts one.
     */
    public static final double SLAP_REACH = 1.3;
    public static final double SLAP_START = 0.9;
    /** The rest after the string's last blow (ticks), and the longest a string waits between blows before it starts again from the slap. */
    public static final int SLAP_REST = 24;
    public static final int SLAP_CHAIN = 30;

    /** The tick of blow {@code slap} its wing lands on. */
    public static int slapImpact(int slap) {
        return Math.max(2, Math.min(SLAP_TICKS[slap] - 2, Math.round(SLAP_TICKS[slap] * SLAP_IMPACT[slap])));
    }

    /**
     * His wing as the rig draws it ({@code slap}: the blow under way, or -1; {@code progress} 0..1 through it): only the right wing, never
     * aiming, crystal as a fight's.
     */
    public static com.hex.kagune.control.ukaku.WingRig.Input banjou(float deploy, double time, float crystal, boolean dissolving, float detail, int slap,
                                                                    float progress) {
        boolean striking = slap >= 0 && slap < SLAPS.length;
        return new com.hex.kagune.control.ukaku.WingRig.Input(com.hex.kagune.control.ukaku.WingForm.TWIN, SLAP_MASTERY, deploy, time, crystal, crystal,
            striking ? SLAPS[slap] : com.hex.kagune.control.ukaku.WingRig.Motion.NONE, striking ? progress : 0.0F, striking ? SLAP_IMPACT[slap] : 0.4F,
            0.0F, 0.0F, false, detail, 1.0F, dissolving, 0.0F, 0.0F, com.hex.kagune.control.ukaku.WingRig.FORWARD, 0.0F, 0.0F, 0.0F, 0.0F,
            SIDE > 0 ? 1.0F : 0.0F, SIDE > 0 ? 0.0F : 1.0F);
    }

    /**
     * {@code carry} (a body frame: 3 x 3 then its offset, rig space) with what it carries drawn {@code scale} times its size about the wing's
     * root on {@link #SIDE} (its shoulder blade stays where it is; the wing shrinks into it).
     */
    public static double[] shrunk(double[] carry, double scale) {
        double[] a = carry == null ? new double[] {1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0} : carry;
        double[] root = {com.hex.kagune.control.ukaku.WingRig.ROOT_X * SIDE, com.hex.kagune.control.ukaku.WingRig.ROOT_Y,
            com.hex.kagune.control.ukaku.WingRig.ROOT_Z};
        double[] out = new double[12];
        for (int r = 0; r < 3; r++) {
            double moved = 0.0;
            for (int c = 0; c < 3; c++) {
                out[r * 3 + c] = a[r * 3 + c] * scale;
                moved += a[r * 3 + c] * root[c] * (1.0 - scale);
            }
            out[9 + r] = a[9 + r] + moved;
        }
        return out;
    }

    /** Where his wing's hand is (rig space, at his size 1) {@code progress} through blow {@code slap}: the rig's own, shrunk as it is drawn. */
    public static double[] slapTip(int slap, float progress) {
        double[] tip = com.hex.kagune.control.ukaku.WingRig.slamTip(SLAP_MASTERY, com.hex.kagune.control.ukaku.WingForm.TWIN, SIDE, SLAPS[slap], progress,
            SLAP_IMPACT[slap]);
        double[] s = shrunk(null, SLAP_SCALE);
        return new double[] {s[0] * tip[0] + s[9], s[4] * tip[1] + s[10], s[8] * tip[2] + s[11]};
    }

    /**
     * 4.0.0: Ayato's: {@code in} as a full pair (both wings, a stronger Ukaku's size), or, {@code kakuja}, the first kakuja (its armoured
     * tentacles, drawn by the kakuja's own rig).
     */
    public static com.hex.kagune.control.ukaku.WingRig.Input full(com.hex.kagune.control.ukaku.WingRig.Input in, boolean kakuja) {
        return new com.hex.kagune.control.ukaku.WingRig.Input(kakuja ? com.hex.kagune.control.ukaku.WingForm.KAKUJA : com.hex.kagune.control.ukaku.WingForm.TWIN,
            kakuja ? 800 : 420, in.deploy(), in.time(), in.crystalRight(), in.crystalLeft(), in.motion(), in.progress(), in.impact(), in.volley(),
            in.recoil(), in.crouching(), in.detail(), in.alpha(), in.dissolving(), in.aimRight(), in.aimLeft(), in.aim(), in.kickRight(), in.kickLeft(),
            in.coilRight(), in.coilLeft(), 1.0F, 1.0F);
    }

    /**
     * 1.3.43: a ghoul child's wing or wings (Ukaku inherited) as the rig draws them at {@code mastery} (its stage's): the pair, or one
     * alone on {@code side} (1 its right, -1 its left; the other grown to nothing), aiming as it shoots.
     */
    public static com.hex.kagune.control.ukaku.WingRig.Input child(int mastery, int wings, int side, float deploy, double time, boolean dissolving,
                                                                   float aim, double[] at, float kickRight, float kickLeft, float detail) {
        return child(mastery, wings, side, deploy, time, dissolving, aim, at, kickRight, kickLeft, detail, 0.0F);
    }

    /** 1.3.48: the same, turned to crystal as far as {@code crystal} (0..1). */
    public static com.hex.kagune.control.ukaku.WingRig.Input child(int mastery, int wings, int side, float deploy, double time, boolean dissolving,
                                                                   float aim, double[] at, float kickRight, float kickLeft, float detail, float crystal) {
        return child(mastery, wings, side, deploy, time, dissolving, aim, at, kickRight, kickLeft, detail, crystal, false);
    }

    /** 2.2.2: the same, as the first kakuja's wings ({@code kakuja}: a grown half-human child's, in a fight: HalfBlood). */
    public static com.hex.kagune.control.ukaku.WingRig.Input child(int mastery, int wings, int side, float deploy, double time, boolean dissolving,
                                                                   float aim, double[] at, float kickRight, float kickLeft, float detail, float crystal,
                                                                   boolean kakuja) {
        float right = wings >= 2 || side > 0 ? 1.0F : 0.0F;
        float left = wings >= 2 || side < 0 ? 1.0F : 0.0F;
        com.hex.kagune.control.ukaku.WingForm form = kakuja ? com.hex.kagune.control.ukaku.WingForm.KAKUJA : com.hex.kagune.control.ukaku.WingForm.TWIN;
        mastery = kakuja ? Math.max(mastery, com.hex.kagune.control.ukaku.WingForm.KAKUJA.requiredMastery()) : mastery;
        return new com.hex.kagune.control.ukaku.WingRig.Input(form, mastery, deploy, time, crystal, crystal,
            com.hex.kagune.control.ukaku.WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false, detail, 1.0F, dissolving, aim, aim, at, kickRight, kickLeft,
            0.0F, 0.0F, right, left);
    }
}
