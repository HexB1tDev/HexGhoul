package com.hex.kagune.control.client.animation;

/**
 * 4.3.0 (the owner, quoted exactly: "Ukaku Mastered animation should be a evil laugh sequence: Put hand on face (head tilted down), start
 * laughing, hand back down a little on the side (right hand) the hand should be a little to the right off the body, crack finger with sound,
 * ukaku emerges."): a mastered Ukaku pulling the kagune out (pure, previewed). Laid over whatever the body was doing, never a jump to it:
 * the right hand comes up over the face as the head bows ({@link #FACE}), held still while the laugh rises over the head, "HA" by "HA"
 * ({@link #text}, from {@link #LAUGH}),
 * the hand comes down a little and out to the right of the body ({@link #SIDE}), the fingers crack ({@link #CRACK}), and the wings tear out
 * ({@link #OUT}); then the body eases back to itself ({@link #END}).
 */
public final class LaughPose {
    /** The hand over the face by here; laughing from here to {@link #SIDE}; the hand down at the side by {@link #CRACK} (ticks). */
    public static final float FACE = 10.0F;
    public static final float LAUGH = 10.0F;
    public static final float SIDE = 38.0F;
    /** The fingers crack (its sound the same tick), the wings come out, all of it eased away by the end (ticks). */
    public static final float CRACK = 48.0F;
    public static final float OUT = 52.0F;
    public static final float END = 66.0F;

    /** How many "HA"s the laugh comes to, one more every {@link #HA_EVERY} ticks from {@link #LAUGH}. */
    public static final int HAS = 5;
    public static final float HA_EVERY = 4.0F;

    /** The hand over the face: the arm raised and turned in, the elbow folded so the palm covers it (found on the posed body: the palm on the face, the elbow clear of the head); the head bowed into it. */
    static final float FACE_ARM_X = -1.3F;
    static final float FACE_ARM_Y = -0.75F;
    static final float FACE_ARM_Z = 0.06F;
    static final float FACE_ELBOW = 1.0F;
    static final float FACE_HEAD_X = 0.5F;
    /** The hand down a little, a little out to the right of the body, the elbow eased, ready to crack; the head still a little down. */
    static final float SIDE_ARM_X = -0.32F;
    static final float SIDE_ARM_Y = 0.12F;
    static final float SIDE_ARM_Z = 0.42F;
    static final float SIDE_ELBOW = 0.55F;
    static final float SIDE_HEAD_X = 0.28F;

    private LaughPose() {
    }

    /** {@code p} posed {@code t} ticks into the laugh, laid over what it was. */
    public static void over(RigPose p, float t) {
        float w = weight(t);
        if (w <= 0.0F) {
            return;
        }
        // From the face to the side: the hand comes down between the laugh's end and the crack.
        float down = ease((t - SIDE) / (CRACK - 4.0F - SIDE));
        float armX = lerp(FACE_ARM_X, SIDE_ARM_X, down);
        float armY = lerp(FACE_ARM_Y, SIDE_ARM_Y, down);
        float armZ = lerp(FACE_ARM_Z, SIDE_ARM_Z, down);
        float elbow = lerp(FACE_ELBOW, SIDE_ELBOW, down);
        float headX = lerp(FACE_HEAD_X, SIDE_HEAD_X, down);
        // The laugh: shoulders and head shaking, building, through the hand on the face and dying off as it comes down.
        // 4.3.0 (the owner: "its a bit too much jitter, that laugh is horrible man", then "can you revert it but add like "HAHAHAHAHA" text
        // over the head"): the body holds still through the laugh, hand on the face, head bowed; the laugh is the text over the head ({@link
        // #text}) and its sound.
        float headZ = 0.0F;
        // The crack: a small flick of the hand and the head tipping with it.
        float snap = snap(t);
        armX += 0.14F * snap;
        elbow += 0.12F * snap;
        headZ += 0.16F * snap;
        p.rightArmX += (armX - p.rightArmX) * w;
        p.rightArmY += (armY - p.rightArmY) * w;
        p.rightArmZ += (armZ - p.rightArmZ) * w;
        p.rightElbow += (elbow - p.rightElbow) * w;
        p.headX += (headX - p.headX) * w;
        p.headZ += (headZ - p.headZ) * w;
        p.headY += (0.0F - p.headY) * w;
        // The body hunched a little over the laugh; the free arm hanging.
        p.lean += 0.12F * w * (1.0F - down * 0.5F);
        p.leftArmX += (0.08F - p.leftArmX) * w;
        p.leftArmZ += (-0.1F - p.leftArmZ) * w;
    }

    /** How much of the laugh is on the body (0..1): eased up to the face at once, held, eased away after the wings are out. */
    public static float weight(float t) {
        if (t <= 0.0F || t >= END) {
            return 0.0F;
        }
        float in = (float)Math.sin(Math.min(1.0F, t / FACE) * Math.PI * 0.5);
        return in * (1.0F - ease((t - OUT) / (END - OUT)));
    }

    /**
     * The laugh over the head {@code t} ticks in: "HA", "HAHA" ... "HAHAHAHAHA" ({@link #HAS}), one more "HA" every {@link #HA_EVERY} ticks
     * from {@link #LAUGH}; empty outside it. Held until the hand comes down, then it fades ({@link #textFade}).
     */
    public static String text(float t) {
        if (t < LAUGH || t >= CRACK) {
            return "";
        }
        int has = Math.min(HAS, 1 + (int)((t - LAUGH) / HA_EVERY));
        return "HA".repeat(has);
    }

    /** How big the text is (1 at rest): each new "HA" pops it out a little and it settles back. */
    public static float textPop(float t) {
        if (t < LAUGH || t >= CRACK) {
            return 0.0F;
        }
        float since = (t - LAUGH) % HA_EVERY;
        boolean growing = (t - LAUGH) / HA_EVERY < HAS;
        return 1.0F + (growing ? 0.22F * (1.0F - ease(since / 3.0F)) : 0.0F);
    }

    /** How solid the text is (0..1): there at once, held through the laugh, fading as the hand comes down to the crack. */
    public static float textFade(float t) {
        if (t < LAUGH || t >= CRACK) {
            return 0.0F;
        }
        return 1.0F - ease((t - SIDE) / (CRACK - SIDE));
    }

    /** Each "HA"'s tilt (radians) and how far above or below the line it sits (in its own heights), as hand-lettered sound effects lie. */
    static final double[] HA_TILT = {-0.12, 0.08, -0.05, 0.11, -0.09};
    static final double[] HA_RISE = {0.05, -0.08, 0.1, -0.04, 0.07};
    /** How far apart the "HA"s are (in their own widths: a little overlapped). */
    public static final double HA_SPACING = 0.8;

    /**
     * The "HA"s over the head {@code t} ticks in, left to right: for each {x, y (in its own widths and heights from the middle of the row),
     * tilt (radians), size (1 at rest; the newest pops in), alpha (0..1)}; none outside the laugh.
     */
    public static double[][] has(float t) {
        int count = text(t).length() / 2;
        double[][] out = new double[count][];
        double fade = textFade(t);
        for (int i = 0; i < count; i++) {
            float since = t - LAUGH - i * HA_EVERY;
            double pop = 1.0 + 0.35 * (1.0 - ease(since / 3.0F));
            double appear = ease(since / 1.5F);
            out[i] = new double[] {(i - (count - 1) * 0.5) * HA_SPACING, HA_RISE[i % HA_RISE.length], HA_TILT[i % HA_TILT.length], pop * (0.6 + 0.4 * appear),
                fade * appear};
        }
        return out;
    }

    /** The crack: a small flick and back over five ticks (0 outside it). */
    public static float snap(float t) {
        return t >= CRACK && t < CRACK + 5.0F ? (float)Math.sin((t - CRACK) / 5.0F * Math.PI) : 0.0F;
    }

    static float ease(float x) {
        float c = x < 0.0F ? 0.0F : x > 1.0F ? 1.0F : x;
        return c * c * (3.0F - 2.0F * c);
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
