package com.hex.kagune.control.family;

/**
 * 1.3.43: how a ghoul child fights with the kagune it inherited (pure, tested; the world's side is {@code ChildKagune}). 1.3.44 (the
 * owner): its Rinkaku is one tentacle, a Rinkaku player's (the original's tube, the first tentacle's curl over the head at rest), lashing
 * on the beats of the lash string; its wing or wings shoot as the wild ghouls' do (one wing: every shot from that side); all of it at its
 * body's size; with a tentacle and a wing it uses both, the tentacle up close, the wing from further off. It only ever fights what
 * threatens its family (as before).
 */
public final class ChildKaguneRules {
    /** How close it keeps when it only shoots, how far it lets its prey get, how far a shot flies, ticks between shots. */
    public static final double SHOOT_NEAREST = 4.0;
    public static final double SHOOT_FARTHEST = 10.0;
    public static final double SHOT_RANGE = 14.0;
    public static final int SHOT_EVERY = 10;
    public static final double SHOT_SPEED = 1.3;
    /** How long it waits between strings and volleys (ticks), at least and at most. */
    public static final int PAUSE_LEAST = 20;
    public static final int PAUSE_MOST = 42;
    /** A lash or a shot's damage against its punch's (a child's kagune is lighter than a grown ghoul's). */
    public static final float KAGUNE_SHARE = 0.45F;

    /** What it does this tick: lash with its tails, shoot with its wing(s), or close in (it has nothing that reaches yet). */
    public enum Use {
        TAILS, WINGS, CLOSE_IN, NONE
    }

    private ChildKaguneRules() {
    }

    /** How big its kagune is against a grown ghoul's: as big as it is ({@link ChildStage#draw}). */
    public static double scale(ChildStage stage) {
        return stage == null ? 1.0 : stage.draw;
    }

    /**
     * The mastery its tentacle strikes at (the lash string's beats, {@code GraftMove.beats}): 1.3.44 (the owner: "it's meant to be one
     * tentacle"): one, a teen's a little longer, always under the second tail's mastery. By its stage alone: its clients draw it from what
     * they know of it, and the server strikes from the same.
     */
    public static int tailMastery(ChildStage stage) {
        return stage.older() ? 60 : 0;
    }

    /** 1.3.44: how many Rinkaku tentacles it has: one (with or without its wing). */
    public static final int TENTACLES = 1;
    /** 1.3.44: the tentacle's thickness, as a Rinkaku player's first tentacle is (the original's 0.18 at mastery 0), before its size. */
    public static final float TENTACLE_THICKNESS = 0.18F;
    /**
     * 1.3.44: the original seed tentacle's layout (KaguneForm.RINKAKU_SEED: fan to the side, height, back, its sway). 1.3.45 (the owner:
     * the back was crowded, the tentacle up the middle and the wing on it): the fan far out to its right, the one tail of a first kagune
     * up over its right shoulder ({@link #LEAN}: how much of the way it leans over from the root on).
     */
    public static final double SEED_FAN = 0.95;
    public static final double LEAN = 0.45;
    public static final double SEED_HEIGHT = 0.44;
    public static final double SEED_BACK = 0.62;
    public static final double SEED_WIGGLE = 0.16;
    /** 1.3.44: where it leaves the body (rig space: its pelvis, as a Rinkaku player's, TendrilChain.pelvisAnchor's height and back). */
    public static final double[] TENTACLE_ROOT = {0.13, 1.8 * 0.43, -0.18}; // 1.3.45: off the spine, on its right

    /** 1.3.44: how much of its length its tentacle shows at rest (the rest curled over; a lash straightens it out to the whole). */
    public static final double REST_SHARE = 0.85;

    /**
     * 1.3.46 (the owner: "add other variations, like two wings and the tentacle in the middle... it just stays in the back like a tail"):
     * how a child of a tentacle and a wing carries them, born with it (by its seed): its tentacle over its right shoulder and one wing on
     * its left; the mirror of that; or its tentacle low out of the middle of its back like a tail, both wings above it. A child with only
     * the tentacle carries it over a shoulder or as a tail the same way.
     */
    public enum Layout {
        SHOULDER_RIGHT(1), SHOULDER_LEFT(-1), TAIL(0);

        /** Which side the tentacle leaves from and leans to: 1 its right, -1 its left, 0 the middle. */
        public final int side;

        Layout(int side) {
            this.side = side;
        }
    }

    /** 1.3.46: the share of children born to each layout: over the right shoulder, the tail, over the left (the rest). */
    public static final double RIGHT_SHARE = 0.4;
    public static final double TAIL_SHARE = 0.35;

    /** 1.3.46: how a child with kagune {@code bits} and {@code seed} carries its tentacle (and its wing): the same all its life. */
    public static Layout layout(int bits, long seed) {
        if (!KaguneGenes.tails(bits)) {
            return Layout.SHOULDER_RIGHT; // no tentacle: nothing to lay out (its wing keeps its seed's side)
        }
        long mixed = seed * 0x2545F4914F6CDD1DL;
        mixed ^= mixed >>> 29;
        double roll = (mixed >>> 11) * 0x1.0p-53;
        return roll < RIGHT_SHARE ? Layout.SHOULDER_RIGHT : roll < RIGHT_SHARE + TAIL_SHARE ? Layout.TAIL : Layout.SHOULDER_LEFT;
    }

    /**
     * 1.3.46: how many wings it has as it carries them: a child of a tentacle and a wing has the pair when its tentacle is a tail (room
     * for both), one beside a tentacle over its shoulder; anyone else as born ({@link KaguneGenes#wings}).
     */
    public static int wingCount(int bits, long seed) {
        int wings = KaguneGenes.wings(bits);
        if (wings > 0 && KaguneGenes.tails(bits)) {
            return layout(bits, seed) == Layout.TAIL ? 2 : 1;
        }
        if (wings > 0 && KaguneGenes.worms(bits) > 0) {
            return 2; // 4.0.0: its worm low out of its back, as a tail: room for both wings above it
        }
        return wings;
    }

    /** 1.3.46: where its tentacle leaves its body (rig space): its pelvis, off the spine on the side it leans to, or in the middle as a tail. */
    public static double[] tentacleRoot(Layout layout) {
        return new double[] {TENTACLE_ROOT[0] * layout.side, TENTACLE_ROOT[1], TENTACLE_ROOT[2]};
    }

    /** As {@link #tentacleRest(double, double, int, int, Layout)} over its right shoulder (1.3.45's). */
    public static double[][] tentacleRest(double length, double time, int seed, int points) {
        return tentacleRest(length, time, seed, points, Layout.SHOULDER_RIGHT);
    }

    /**
     * 1.3.44: its tentacle at rest, in the shape the original lays a Rinkaku player's first tentacle out in (the cubic from the pelvis
     * through its two controls to its rest above and a little ahead of the head: up behind the back and curling over), drawn out about
     * its root to {@link #REST_SHARE} of its {@code length} (rig space, a grown body's: as long at rest as it is in a lash, so it never
     * stretches), swaying a little on {@code time} (ticks) by its {@code seed}; {@code points} evenly along the curve, root first. 1.3.46:
     * over the shoulder its {@code layout} gives, or as a tail: out behind it, a little low, the end curling up, swaying side to side.
     */
    public static double[][] tentacleRest(double length, double time, int seed, int points, Layout layout) {
        double[] root = tentacleRoot(layout);
        double[] a;
        double[] b;
        double[] rest;
        if (layout == Layout.TAIL) {
            a = new double[] {0.0, root[1] - length * 0.04, root[2] - length * 0.32};
            b = new double[] {0.0, root[1] - length * 0.02, root[2] - length * 0.62};
            rest = new double[] {0.0, root[1] + length * 0.2, root[2] - length * 0.78};
        } else {
            double fan = SEED_FAN * layout.side;
            double arch = length * 0.09;
            rest = new double[] {fan, root[1] + SEED_HEIGHT + arch, root[2] + length * (0.03 + SEED_BACK * 0.04)};
            a = new double[] {root[0] + fan * LEAN, root[1] + SEED_HEIGHT * 0.42 + length * 0.15, root[2] - length * 0.22};
            b = new double[] {rest[0] + fan * 0.05, rest[1] + length * 0.12, rest[2] - length * 0.16};
        }
        int n = Math.max(2, points);
        double[][] curve = new double[n][];
        double run = 0.0;
        for (int i = 0; i < n; i++) {
            double t = i / (double)(n - 1);
            double u = 1.0 - t;
            double[] p = new double[3];
            for (int k = 0; k < 3; k++) {
                p[k] = u * u * u * root[k] + 3.0 * u * u * t * a[k] + 3.0 * u * t * t * b[k] + t * t * t * rest[k];
            }
            curve[i] = p;
            if (i > 0) {
                run += Math.sqrt(Math.pow(p[0] - curve[i - 1][0], 2) + Math.pow(p[1] - curve[i - 1][1], 2) + Math.pow(p[2] - curve[i - 1][2], 2));
            }
        }
        double grow = run < 1.0E-6 ? 1.0 : REST_SHARE * length / run;
        double phase = time / 20.0 + (seed & 255) * 0.0245;
        boolean tail = layout == Layout.TAIL;
        double[][] out = new double[n][];
        for (int i = 0; i < n; i++) {
            double t = i / (double)(n - 1);
            double[] p = new double[3];
            for (int k = 0; k < 3; k++) {
                p[k] = root[k] + (curve[i][k] - root[k]) * grow;
            }
            // The sway: nothing at the root, the most at the point, slow and a little out of step along it (a tail's from side to side).
            double sway = SEED_WIGGLE * t * t * (tail ? 2.2 : 1.0);
            p[0] += sway * Math.sin(phase * (tail ? 1.2 : 1.7) + t * 2.1);
            p[1] += sway * (tail ? 0.25 : 0.6) * Math.sin(phase * 1.3 + t * 1.4 + 1.1);
            p[2] += sway * (tail ? 0.2 : 0.5) * Math.cos(phase * 1.1 + t * 1.8);
            out[i] = p;
        }
        return out;
    }

    /** The mastery its wing or wings are drawn at (and shoot from the tip of): a child's small, a teen's grown; by its stage alone. */
    public static int wingMastery(ChildStage stage) {
        return stage.older() ? 220 : 110;
    }

    /** One unit of a lash's power, or one shot: a share of its punch ({@link ChildRules#damage}), as strong as it was born. */
    public static float damage(ChildStage stage, int skill, float might) {
        return ChildRules.damage(stage, skill, true, might) * KAGUNE_SHARE;
    }

    /** How many lashes a string, or shots a volley, before it pauses: two, up to four the better it was taught. */
    public static int string(int skill) {
        return 2 + Math.max(0, Math.min(2, skill / 2));
    }

    /**
     * What it does with kagune {@code bits} against prey {@code distance} away that it {@code sees}: its tails within their reach
     * ({@code tailReach}, already at its size); its wing(s) beyond it (or always, with no tails); closer if it can't reach at all.
     */
    public static Use use(int bits, double distance, boolean sees, double tailReach) {
        boolean tails = KaguneGenes.tails(bits);
        boolean wings = KaguneGenes.wings(bits) > 0;
        if (!tails && !wings) {
            return Use.NONE;
        }
        if (tails && distance <= tailReach - 0.3 && sees) {
            return Use.TAILS;
        }
        if (wings && sees && distance <= SHOT_RANGE) {
            return tails && distance <= tailReach + 1.5 ? Use.CLOSE_IN : Use.WINGS;
        }
        return Use.CLOSE_IN;
    }

    /** The wing its next shot leaves from (1 its right, -1 its left): its one wing's side, or turn about with the pair ({@code last}). */
    public static int side(int bits, long seed, int last) {
        if (wingCount(bits, seed) == 1) {
            return wingSide(bits, seed);
        }
        return last >= 0 ? -1 : 1;
    }

    /**
     * 1.3.45: the side its one wing grows on (1 its right, -1 its left): with a tentacle over a shoulder, the other side (the owner, a
     * crowded back; 1.3.46: either shoulder); else the side its seed gives ({@link KaguneGenes#side}).
     */
    public static int wingSide(int bits, long seed) {
        Layout layout = layout(bits, seed);
        return KaguneGenes.tails(bits) && layout.side != 0 ? -layout.side : KaguneGenes.side(seed);
    }

    /** 1.3.46: what it has as it carries it, in words ("a Rinkaku tentacle as a tail and Ukaku wings"), for {@code /kagune children}. */
    public static String describe(int bits, long seed) {
        if (!KaguneGenes.tails(bits)) {
            int worms = KaguneGenes.worms(bits);
            if (worms > 0 && KaguneGenes.wings(bits) > 0) {
                // 4.0.0: wings over a worm carried low as a tail.
                return "Ukaku wings and " + (worms == 2 ? "two Bikaku worms" : "a Bikaku worm") + (KaguneGenes.owl(bits) ? " and Eto's flesh" : "");
            }
            return KaguneGenes.describe(bits);
        }
        Layout layout = layout(bits, seed);
        String tentacle = "a Rinkaku tentacle " + switch (layout) {
            case SHOULDER_RIGHT -> "over its right shoulder";
            case SHOULDER_LEFT -> "over its left shoulder";
            case TAIL -> "as a tail";
        };
        int wings = wingCount(bits, seed);
        String rest = wings == 2 ? " and Ukaku wings" : wings == 1 ? " and an Ukaku wing on its " + (wingSide(bits, seed) > 0 ? "right" : "left") : "";
        int worms = KaguneGenes.worms(bits);
        return tentacle + rest + (KaguneGenes.owl(bits) ? " and Eto's flesh" : "") + (worms == 2 ? " and two Bikaku worms" : worms == 1 ? " and a Bikaku worm" : "");
    }

    // ---------------------------------------------------------------- 4.0.0: a Bikaku parent's child

    /**
     * 4.0.0 (the owner: "Make sure The child of a Bikaku user with all the other combinations matches perfectly"): its worm (or its two,
     * one from each Bikaku parent) out of the small of its back as a Bikaku player's, at its size, in its colour; in a fight it bites in
     * turn (a snap, a lunge, a maul: Bikaku's own bites) whatever comes within {@link #WORM_REACH} (a grown body's, before its size), each
     * bite feeding it a little ({@link #WORM_HEAL}); further off its wings shoot, or it closes in. With a tentacle too, the worm bites
     * whenever it is ready and the tentacle lashes between. Never a kakuja, never a worm move.
     */
    public static final double WORM_REACH = 5.0;
    /** Ticks between one bite's end and the next; a bite's blow against its lash's unit; the health each bite gives it back. */
    public static final int WORM_PAUSE = 10;
    public static final float WORM_SHARE = 1.25F;
    public static final float WORM_HEAL = 1.0F;

    /** True if its worm bites now: it has one, sees its prey within the worm's reach ({@code reach}, at its size), and the worm is ready. */
    public static boolean bites(int bits, double distance, boolean sees, double reach, long now, long ready) {
        return KaguneGenes.worms(bits) > 0 && sees && distance <= reach && now >= ready;
    }

    // ---------------------------------------------------------------- 1.3.48: the rest of its parents' kit (never a kakuja)

    /**
     * 1.3.48 (the owner: "crystallize... grab, throw, crush, backflip, front flip, everything the mother and father can do... the kid will
     * never have access to the [kakujas]"): what it does next, beyond a lash or a shot. Its wings turn to crystal a moment after they are
     * out ({@link #CRYSTAL_AFTER}), and then throw crystal spikes (and now and then a fan of them); its tentacle grabs what it fights (if
     * that is no bigger than a person), squeezes it, and throws it or crushes it into the ground; it flips back out of reach and in over
     * a gap, as a ghoul does. Never a kakuja: no move here grows one.
     */
    public enum Act {
        NONE, LASH, SHOOT, FAN, GRAB, BACK_FLIP, FRONT_FLIP, CLOSE_IN,
        /** 1.3.50, Eto's child: her flesh's rake, its grab, its club; her Mutated Arm, its transformation at low health. */
        SWIPE, FLESH_GRAB, HAMMER, ARM
    }

    /** Ticks after its wings are out that they turn to crystal; a fan's spikes, their spread (degrees), how often (ticks). */
    public static final int CRYSTAL_AFTER = 20;
    public static final int FAN_SPIKES = 5;
    public static final double FAN_SPREAD = 32.0;
    public static final int FAN_EVERY = 160;
    /** A crystal spike's damage against a fire shot's. */
    public static final float SPIKE_SHARE = 1.2F;
    /** A grab: how often; the tick it catches; how long it holds and squeezes (every {@link #SQUEEZE_EVERY}); what it is then. */
    public static final int GRAB_EVERY = 220;
    public static final int GRAB_CATCH = 8;
    public static final int GRAB_HOLD = 26;
    public static final int SQUEEZE_EVERY = 8;
    /** What it may grab: no wider and no taller than this (blocks): a person-sized monster, not a spider or a golem. */
    public static final double GRAB_WIDEST = 1.0;
    public static final double GRAB_TALLEST = 2.2;
    /** Flips: how often; a back flip when its prey is this close; a front flip in when it is this far (and it has a tentacle to reach with). */
    public static final int FLIP_EVERY = 90;
    public static final double BACK_FLIP_INSIDE = 1.7;
    public static final double FRONT_FLIP_FROM = 6.0;
    public static final double FRONT_FLIP_TO = 11.0;

    /** True if it can grab a body {@code width} wide and {@code height} tall. */
    public static boolean grabbable(double width, double height) {
        return width <= GRAB_WIDEST && height <= GRAB_TALLEST;
    }

    /**
     * 1.3.48: what it does now, against prey {@code distance} away that it {@code sees}, with kagune {@code bits}: a back flip out when it
     * is right on top of it, a front flip in when it is far and its tentacle can't reach it yet; a grab when it can, its tentacle reaching
     * ({@code tailReach}) and the prey small enough ({@code small}); a fan of crystal when its wings are crystal ({@code crystal}) and the
     * fan is ready; else a lash or a shot as before ({@link #use}). The {@code ...Ready} are the ticks each comes back on; {@code roll} in
     * 0..1; {@code onGround}: flips only from the ground.
     */
    public static Act choose(int bits, double distance, boolean sees, double tailReach, boolean crystal, boolean small, boolean onGround, long now,
                             long flipReady, long grabReady, long fanReady, double roll) {
        boolean tails = KaguneGenes.tails(bits);
        boolean wings = KaguneGenes.wings(bits) > 0;
        if (!tails && !wings) {
            return Act.NONE;
        }
        if (onGround && now >= flipReady) {
            if (distance < BACK_FLIP_INSIDE && roll < 0.6) {
                return Act.BACK_FLIP;
            }
            if (tails && sees && distance >= FRONT_FLIP_FROM && distance <= FRONT_FLIP_TO && roll < 0.35) {
                return Act.FRONT_FLIP;
            }
        }
        if (tails && sees && small && now >= grabReady && distance <= tailReach - 0.3 && roll < 0.45) {
            return Act.GRAB;
        }
        if (wings && crystal && sees && now >= fanReady && distance >= 3.0 && distance <= SHOT_RANGE && roll < 0.5) {
            return Act.FAN;
        }
        return switch (use(bits, distance, sees, tailReach)) {
            case TAILS -> Act.LASH;
            case WINGS -> Act.SHOOT;
            case CLOSE_IN -> Act.CLOSE_IN;
            case NONE -> Act.NONE;
        };
    }

    // ---------------------------------------------------------------- 1.3.50: Eto's child

    /** How often its rake, its grab and its club come back; its arm (ticks). */
    public static final int SWIPE_EVERY = 50;
    public static final int HAMMER_EVERY = 170;
    public static final int ARM_EVERY = 320;
    /** Under this share of its health, its arm turns (its transformation, the owner's "at low health"); held to a sphere this often. */
    public static final float ARM_BELOW = 0.4F;
    public static final double ARM_HELD = 0.35;
    /** Each move's blow against its lash's unit. */
    public static final float SWIPE_SHARE = 1.5F;
    public static final float FLESH_THROW_SHARE = 1.4F;
    public static final float HAMMER_SHARE = 1.3F;
    public static final float ARM_SHARE = 1.1F;
    public static final float ARM_CRUSH_SHARE = 0.7F;
    /** How far its rake reaches and its grab goes, against a grown body's (PaleRules: 4 and 9 blocks), before its size. */
    public static final double SWIPE_REACH = 4.0;
    public static final double GRAB_RANGE = 8.0;

    /**
     * 1.3.50: what Eto's child does with her flesh, against prey {@code distance} away (blocks, the world) at its size ({@code scale}):
     * badly hurt ({@code health} under {@link #ARM_BELOW} of its most), its arm turns and shoots out at it (its transformation); close,
     * the rake (and now and then the club instead); further, the grab if it is no bigger than a person ({@code small}); else nothing of the
     * flesh ({@link Act#CLOSE_IN}: it closes in, or uses what else it has, its father's tentacle or wing).
     */
    public static Act chooseFlesh(double distance, boolean sees, double scale, boolean small, float health, long now, long swipeReady, long grabReady,
                                  long hammerReady, long armReady, double roll) {
        if (!sees) {
            return Act.CLOSE_IN;
        }
        if (health < ARM_BELOW && now >= armReady && distance <= 14.0 * scale) {
            return Act.ARM;
        }
        double reach = SWIPE_REACH * scale;
        if (distance <= reach) {
            if (now >= hammerReady && roll < 0.35) {
                return Act.HAMMER;
            }
            if (now >= swipeReady) {
                return Act.SWIPE;
            }
        }
        if (small && now >= grabReady && distance > reach * 0.6 && distance <= GRAB_RANGE * scale && roll < 0.6) {
            return Act.FLESH_GRAB;
        }
        return Act.CLOSE_IN;
    }

    /** 1.3.48: a fan's spike {@code i} of {@link #FAN_SPIKES}: degrees off its aim, the middle one straight. */
    public static double fan(int i) {
        return FAN_SPREAD * (2.0 * i / (FAN_SPIKES - 1) - 1.0);
    }

    /** 1.3.48: how a grab ends ({@code roll} in 0..1): thrown away (three in five), or crushed into the ground. */
    public static boolean throwsIt(double roll) {
        return roll < 0.6;
    }

    /** Its pause after a string or volley ({@code roll} in 0..1). */
    public static int pause(double roll) {
        return PAUSE_LEAST + (int)Math.round(Math.max(0.0, Math.min(1.0, roll)) * (PAUSE_MOST - PAUSE_LEAST));
    }
}
