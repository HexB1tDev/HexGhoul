package com.hex.kagune.control.client.animation;

/**
 * Eto's body at rest in her base form (6.1.0; pure, tested): absurdly strong and barely taking any of it seriously. Standing,
 * she settles hip-shot on one leg, the other knee loose, arms hanging easy, the head tilted; she breathes, her weight drifts.
 * Every few seconds she does something small: shrugs, stretches, tilts her head at something, glances round, swings an arm,
 * shifts her weight to the other leg, taps a foot, rocks on her heels with her hands behind her back, rolls her shoulders, or
 * giggles behind a hand. Never the same twice running, never the last two; only once she has stood still a second (looking
 * round is fine), never on the move. Everything eases in and out, and the moment she moves, flips or strikes the gesture going
 * fades out within a few ticks and is dropped, so nothing plays under her moves.
 *
 * <p>A deterministic generator per player (seeded from the entity id) picks the gestures, so the previews can show them.
 */
public final class EtoIdle {
    /** Something small she does now and then: how long it takes (ticks), and whether she does it on the move too. */
    public enum Gesture {
        SHRUG(26, false), STRETCH(46, false), HEAD_TILT(30, true), GLANCE(42, true), ARM_SWING(34, false), WEIGHT_SHIFT(24, false),
        FOOT_TAP(38, false), ROCK(48, false), SHOULDER_ROLL(32, true), AMUSED(28, true);

        public final int ticks;
        public final boolean light;

        Gesture(int ticks, boolean light) {
            this.ticks = ticks;
            this.light = light;
        }
    }

    /** Quiet ticks between two gestures (at least, and at most). */
    public static final int REST_MIN = 50;
    public static final int REST_MAX = 130;

    private long seed;
    private int age;
    private Gesture current;
    /** How much of the gesture going shows (eased; it fades out the moment she is no longer standing still). */
    private float presence;
    private float lastPresence;
    private int startedAt = Integer.MIN_VALUE / 2;
    private int nextAt;
    private Gesture last;
    private Gesture beforeLast;
    /** Which leg carries her weight (+1 her right, -1 her left), and the one a weight shift is moving it to. */
    private int side = 1;
    private int toward = 1;
    /** Which hand or way a gesture goes (+1 her right, -1 her left). */
    private int hand = 1;

    public EtoIdle(long seed) {
        this.seed = seed * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        this.nextAt = 40 + (int)(this.random() * 60.0F);
    }

    private float random() {
        this.seed = this.seed * 6364136223846793005L + 1442695040888963407L;
        return (this.seed >>> 40) / (float)(1L << 24);
    }

    /**
     * One tick: {@code still} while she has stood still (not moving from where she is; looking round is fine) long enough for her
     * stance to show. Only then does a gesture start; the moment she is not (she moves, flips, strikes, rides, lies down) the one
     * going fades out within a few ticks and is dropped. ({@code moving} is no longer used: nothing plays on the move.)
     */
    public void tick(boolean still, boolean moving) {
        this.age++;
        this.lastPresence = this.presence;
        this.presence += ((still && this.current != null ? 1.0F : 0.0F) - this.presence) * (still ? 0.3F : 0.45F);
        if (!still && this.current != null && this.presence < 0.02F) {
            this.presence = 0.0F;
            this.current = null;
            this.nextAt = this.age + REST_MIN / 2;
        }
        if (this.current != null && this.age - this.startedAt >= this.current.ticks) {
            if (this.current == Gesture.WEIGHT_SHIFT) {
                this.side = this.toward;
            }
            this.current = null;
            this.nextAt = this.age + REST_MIN + (int)(this.random() * (REST_MAX - REST_MIN));
        }
        if (this.current == null && this.age >= this.nextAt && still) {
            Gesture next = this.pick(still);
            if (next != null) {
                this.current = next;
                this.startedAt = this.age;
                this.hand = this.random() < 0.5F ? 1 : -1;
                if (next == Gesture.WEIGHT_SHIFT) {
                    this.toward = -this.side;
                }
                this.beforeLast = this.last;
                this.last = next;
            } else {
                this.nextAt = this.age + 20;
            }
        }
    }

    /** A gesture she can do now: not the last two. */
    private Gesture pick(boolean still) {
        Gesture[] all = Gesture.values();
        int options = 0;
        for (Gesture g : all) {
            if (g != this.last && g != this.beforeLast && (still || g.light)) {
                options++;
            }
        }
        if (options == 0) {
            return null;
        }
        int choice = Math.min(options - 1, (int)(this.random() * options));
        for (Gesture g : all) {
            if (g != this.last && g != this.beforeLast && (still || g.light)) {
                if (choice-- == 0) {
                    return g;
                }
            }
        }
        return null;
    }

    /** The gesture going now, or null. */
    public Gesture current() {
        return this.current;
    }

    /**
     * Her stance (weighted {@code still}, 0..1) and the gesture going (weighted by {@code still}, or half {@code moving} for a light
     * one) this frame; {@code partial} the tick's fraction, {@code bends} false where elbows and knees can't be drawn bent.
     */
    public RigPose pose(float partial, float still, float moving, boolean bends) {
        RigPose p = new RigPose();
        double time = (this.age + partial) / 20.0;
        if (still > 0.0F) {
            RigPose stance = new RigPose();
            // A weight shift carries the stance across to the other leg over the gesture.
            float to = this.current == Gesture.WEIGHT_SHIFT ? this.toward : this.side;
            float s = this.side + (to - this.side) * smooth(this.shiftAt(partial));
            stance(stance, s, time);
            if (this.current != null) {
                square(stance, squared(this.current, (this.age - this.startedAt + partial) / this.current.ticks));
            }
            if (!bends) {
                IdlePose.straighten(stance);
            }
            p.add(stance, still);
        }
        if (this.current != null) {
            float weight = still * (this.lastPresence + (this.presence - this.lastPresence) * partial);
            if (weight > 0.0F) {
                float t = (this.age - this.startedAt + partial) / this.current.ticks;
                RigPose g = new RigPose();
                gesture(g, this.current, Math.max(0.0F, Math.min(1.0F, t)), this.hand, this.side);
                if (!bends) {
                    g.rightElbow = 0.0F;
                    g.leftElbow = 0.0F;
                    g.rightKnee = 0.0F;
                    g.leftKnee = 0.0F;
                }
                p.add(g, weight);
            }
        }
        return p;
    }

    private float shiftAt(float partial) {
        if (this.current != Gesture.WEIGHT_SHIFT) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (this.age - this.startedAt + partial) / (float)Gesture.WEIGHT_SHIFT.ticks));
    }

    /**
     * Hip-shot on one leg ({@code side} +1 her right carries her, -1 her left; between them while she shifts): the chest leaning
     * a touch over the standing leg and turned a little, the free knee loose and its foot turned out, arms easy, head tilted,
     * breathing and drifting slowly.
     */
    public static void stance(RigPose p, float side, double time) {
        float breath = (float)Math.sin(time * 1.5);
        float drift = (float)Math.sin(time * 0.43);
        float wander = (float)Math.sin(time * 0.29 + 1.3);
        float right = Math.max(0.0F, side);
        float left = Math.max(0.0F, -side);
        p.lean = 0.02F + 0.012F * breath;
        p.side = -0.09F * side + 0.012F * drift;
        p.twist = 0.12F * side + 0.03F * wander;
        p.headX = -0.03F - 0.008F * breath;
        p.headY = -0.08F * side + 0.12F * wander;
        p.headZ = 0.11F * side + 0.03F * drift;
        p.rightShoulderY = -0.08F * breath;
        p.leftShoulderY = -0.08F * breath;
        // Arms hanging loose, a little out, elbows soft.
        p.rightArmX = 0.04F + 0.025F * breath;
        p.rightArmZ = 0.1F + 0.02F * drift;
        p.rightElbow = 0.22F;
        p.leftArmX = 0.02F + 0.025F * breath;
        p.leftArmZ = -0.1F + 0.02F * drift;
        p.leftElbow = 0.25F;
        // The standing leg straight and a touch out; the free one forward and out, knee loose, toes turned out.
        p.rightLegZ = 0.06F * right + 0.15F * left;
        p.rightLegX = -0.22F * left;
        p.rightLegY = 0.32F * left;
        p.rightKnee = 0.46F * left;
        p.leftLegZ = -0.06F * left - 0.15F * right;
        p.leftLegX = -0.22F * right;
        p.leftLegY = -0.32F * right;
        p.leftKnee = 0.46F * right;
        p.drop = Math.max(RigPose.legDrop(p.rightLegX, p.rightLegZ, p.rightKnee), RigPose.legDrop(p.leftLegX, p.leftLegZ, p.leftKnee));
    }

    /**
     * One gesture at {@code t} (0..1 of it): {@code hand} which side it goes to, {@code side} the leg she stands on. Every frame of
     * every gesture keeps her arms out of her torso and head ({@link BodyFrame#clipping}, checked by the tests): the model's arms
     * are boxes, so a hand never crosses her face or chest, and never meets the other behind her back.
     */
    public static void gesture(RigPose p, Gesture gesture, float t, int hand, int side) {
        float e = window(t);
        switch (gesture) {
            case SHRUG -> {
                // Shoulders up, forearms opening out to the sides palms up, a tilt: "whatever". The upper arms turn about their
                // length (+Y on the right, -Y on the left) so the bent forearms swing out, away from the body.
                // The arms swing out first and come back in last, so the twist never brings an elbow into her sides.
                float out = window(t, 0.08F, 0.82F);
                float lift = window(t, 0.18F, 0.72F);
                p.rightShoulderY -= 1.2F * lift;
                p.leftShoulderY -= 1.2F * lift;
                p.rightArmZ += 0.55F * out;
                p.leftArmZ -= 0.55F * out;
                p.rightArmX -= 0.15F * lift;
                p.leftArmX -= 0.15F * lift;
                p.rightArmY += 0.8F * lift;
                p.leftArmY -= 0.8F * lift;
                p.rightElbow += 1.35F * lift;
                p.leftElbow += 1.35F * lift;
                p.headZ += 0.2F * hand * lift;
                p.headX -= 0.06F * lift;
            }
            case STRETCH -> {
                // Both arms up over her head, spread wide (raised overhead, an arm swings out by -Z on the right, +Z on the left),
                // a lean back, then a lean to one side, and let go.
                // "Out" flips as an arm passes level (below, +Z on the right swings it out; above, -Z does), so the spread grows
                // only as the arms rise past level: they clear her sides on the way up and her head at the top.
                float up = stretchRaise(t);
                float reach = window(t, 0.45F, 0.78F);
                float raised = 2.85F * up;
                float spread = stretchSpread(raised);
                p.rightArmX -= raised;
                p.leftArmX -= raised;
                p.rightArmZ += spread;
                p.leftArmZ -= spread;
                p.rightElbow += 0.12F * up;
                p.leftElbow += 0.12F * up;
                p.lean -= 0.14F * up;
                p.side += 0.18F * hand * reach;
                p.headX -= 0.2F * up;
            }
            case HEAD_TILT -> {
                // Something caught her interest.
                p.headZ += 0.42F * hand * e;
                p.headY += 0.18F * hand * e;
                p.headX += 0.06F * e;
                p.side -= 0.05F * hand * e;
            }
            case GLANCE -> {
                // A look over one shoulder, then the other, the chest following a little.
                float look = (float)(Math.sin(t * Math.PI * 2.0) * hand) * e;
                p.headY += 0.8F * look;
                p.twist += 0.12F * look;
                p.headX -= 0.05F * e;
            }
            case ARM_SWING -> {
                // One arm swung loose like a pendulum, twice, the body twisting against it.
                float swing = (float)Math.sin(t * Math.PI * 4.0) * e;
                if (hand > 0) {
                    p.rightArmX += 0.65F * swing;
                    p.rightElbow += 0.25F * e;
                } else {
                    p.leftArmX += 0.65F * swing;
                    p.leftElbow += 0.25F * e;
                }
                p.twist -= 0.07F * swing * hand;
            }
            case WEIGHT_SHIFT -> {
                // The stance itself carries the shift; a little dip as the weight crosses over.
                p.drop += 0.35F * (float)Math.sin(Math.PI * t);
                p.headZ -= 0.08F * side * (float)Math.sin(Math.PI * t);
            }
            case FOOT_TAP -> {
                // The free foot taps three times; the head bobs along.
                float tap = (float)Math.max(0.0, Math.sin(t * Math.PI * 6.0)) * e;
                if (side > 0) {
                    p.leftLegX -= 0.14F * tap;
                    p.leftKnee += 0.28F * tap;
                } else {
                    p.rightLegX -= 0.14F * tap;
                    p.rightKnee += 0.28F * tap;
                }
                p.headX += 0.05F * tap;
            }
            case ROCK -> {
                // Rocking heel to toe, the arms hanging loose a little behind her.
                float hold = window(t, 0.12F, 0.86F);
                float rock = (float)Math.sin(t * Math.PI * 4.0) * hold;
                p.rightArmX += 0.3F * hold - 0.08F * rock;
                p.leftArmX += 0.3F * hold - 0.08F * rock;
                p.rightArmZ += 0.03F * hold;
                p.leftArmZ -= 0.03F * hold;
                p.lean += 0.07F * rock;
                p.headX -= 0.04F * rock;
            }
            case SHOULDER_ROLL -> {
                // One shoulder rolls round, then the other.
                double phase = t * Math.PI * 4.0;
                p.rightShoulderY -= 0.7F * (float)Math.max(0.0, Math.sin(phase)) * e;
                p.rightShoulderZ += 0.5F * (float)Math.cos(phase) * e;
                p.leftShoulderY -= 0.7F * (float)Math.max(0.0, Math.sin(phase - Math.PI / 2.0)) * e;
                p.leftShoulderZ += 0.5F * (float)Math.cos(phase - Math.PI / 2.0) * e;
                p.headZ += 0.06F * (float)Math.sin(phase) * e;
            }
            case AMUSED -> {
                // A giggle and a little "hey" wave: a hand up out beside her shoulder, palm out, waggling, her shoulders bouncing,
                // the head tilted toward it.
                // The arm swings out before it turns and bends, and is back down by the side before it comes in.
                float hold = window(t, 0.2F, 0.7F);
                float out = window(t, 0.12F, 0.8F);
                float giggle = (float)Math.abs(Math.sin(t * Math.PI * 10.0)) * hold;
                float wave = 0.12F * (float)Math.sin(t * Math.PI * 8.0) * hold;
                float twist = hold * hold;
                if (hand > 0) {
                    p.rightArmX -= 0.3F * hold;
                    p.rightArmY += 1.57F * twist;
                    p.rightArmZ += 0.65F * out + wave;
                    p.rightElbow += 1.6F * hold;
                } else {
                    p.leftArmX -= 0.3F * hold;
                    p.leftArmY -= 1.57F * twist;
                    p.leftArmZ -= 0.65F * out + wave;
                    p.leftElbow += 1.6F * hold;
                }
                p.rightShoulderY -= 0.45F * giggle;
                p.leftShoulderY -= 0.45F * giggle;
                p.headX += 0.1F * hold;
                p.headZ += 0.12F * hand * hold;
            }
        }
    }

    /** How far the stretch has the arms up (0..1) at {@code t} of it. */
    static float stretchRaise(float t) {
        return window(t, 0.12F, 0.82F);
    }

    /**
     * The stretch's spread (right arm's Z offset; the left takes its negative) for arms raised {@code raised} radians: none while
     * they are low, then out along the narrow band that keeps them off her head as they pass its side (solved against
     * {@link BodyFrame#clipping}: with the chest and head squared up, between about 100 and 115 degrees only -0.2 to -0.3 clears).
     */
    static float stretchSpread(float raised) {
        float[] at = {1.0F, 1.57F, 1.71F, 1.85F, 2.0F, 2.2F};
        float[] dz = {0.0F, -0.15F, -0.2F, -0.25F, -0.31F, -0.4F};
        if (raised <= at[0]) {
            return 0.0F;
        }
        for (int i = 1; i < at.length; i++) {
            if (raised <= at[i]) {
                float u = (raised - at[i - 1]) / (at[i] - at[i - 1]);
                return dz[i - 1] + (dz[i] - dz[i - 1]) * u;
            }
        }
        return dz[dz.length - 1];
    }

    /**
     * How much of her stance's lean and turn a gesture leaves (1 all of it): the stretch squares her chest and head up as the arms
     * go over (a turned head's corners would otherwise stand out into the arms' way).
     */
    static float squared(Gesture gesture, float t) {
        return gesture == Gesture.STRETCH ? 1.0F - stretchRaise(t) : 1.0F;
    }

    /**
     * One frame exactly as {@link #pose} builds it at full weight: her stance on {@code side} at {@code time} (seconds) with
     * {@code gesture} (or null) at {@code t}, going {@code hand}'s way. The tests and previews check and draw this.
     */
    public static RigPose compose(float side, double time, Gesture gesture, float t, int hand) {
        RigPose stance = new RigPose();
        stance(stance, side, time);
        RigPose p = new RigPose();
        if (gesture != null) {
            square(stance, squared(gesture, t));
        }
        p.add(stance, 1.0F);
        if (gesture != null) {
            RigPose g = new RigPose();
            gesture(g, gesture, Math.max(0.0F, Math.min(1.0F, t)), hand, side >= 0.0F ? 1 : -1);
            p.add(g, 1.0F);
        }
        return p;
    }

    /** Scales the stance's chest turn and lean sideways and the head's turn and tilt by {@code keep}. */
    static void square(RigPose stance, float keep) {
        stance.twist *= keep;
        stance.side *= keep;
        stance.headY *= keep;
        stance.headZ *= keep;
    }

    /** In over the first fifth, out over the last fifth, smoothly. */
    static float window(float t) {
        return window(t, 0.2F, 0.8F);
    }

    /** Eased in until {@code in}, held, eased out from {@code out} to 1. */
    static float window(float t, float in, float out) {
        if (t <= 0.0F || t >= 1.0F) {
            return 0.0F;
        }
        if (t < in) {
            return smooth(t / in);
        }
        if (t > out) {
            return smooth((1.0F - t) / (1.0F - out));
        }
        return 1.0F;
    }

    static float smooth(float t) {
        float u = Math.max(0.0F, Math.min(1.0F, t));
        return u * u * (3.0F - 2.0F * u);
    }
}
