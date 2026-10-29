package com.hex.kagune.control.wound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A player's wounds (pure, tested; 3.0.0), following their health: every loss of health tears new wounds open, more and
 * bigger the harder the hit; as health comes back they close again, the newest first, and are gone once healed. So a player
 * looks as hurt as they are, and a ghoul's regeneration visibly knits them shut. Ayato's spikes add punctures where they sat
 * ({@link #puncture}), which close the same way.
 */
public final class WoundBook {
    /** At most this many wounds on one body; the oldest go first. */
    public static final int MOST = 24;
    /** The share of a hit's lost health over which its wounds close again, as a multiple of that loss (they close a little early). */
    static final double CLOSING = 1.4;
    /** 6.0.11: the least a hit's force counts for in an arrow's hole and a bite, so even a light one is plain to see. */
    static final double VISIBLE = 0.7;

    /**
     * How a blow that is about to land is dealt, and so what it leaves: Kaneki's tails (3.0.3: a slash's long gash where it struck,
     * along the swing; a stab's deep hole; a crush's tears round the torso and arms, where the tail squeezed; a slam's down the back
     * and legs, where the ground met him), and since 5.0.17 every other hit on a player, as the server saw it land
     * ({@code WoundHits}). New blows go at the end: the network sends them by their place in this list.
     */
    public enum Blow {
        SLASH, STAB, CRUSH, SLAM,
        /** A blunt blow (a fist, a club): a bruise, the skin split when it is hard, a bone broken when it is very hard. */
        BLUNT,
        /** Fire, lava, a fireball, Ayato's fire: burns. */
        BURN,
        /** An explosion: flesh torn out on the side facing it, shrapnel holes, more tears round it. */
        BLAST,
        /** A fall: the legs; a long one breaks a shin. */
        FALL,
        /** Thorns, a cactus, a berry bush: scratches. */
        SCRATCH,
        /** One of Ayato's spikes bursting out of the body: a crater where it sat. */
        BURST,
        /** Poison, hunger, drowning, magic, bleeding: nothing to see. */
        INTERNAL,
        /** A beast's claws and teeth (6.0.11: a bite): the flesh torn open, the teeth's punctures round it, a claw's rakes beside. */
        MAUL;

        public static Blow byOrdinal(int ordinal) {
            Blow[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : SLASH;
        }

        /** How many wounds a blow that cost {@code lost} of the health opens. */
        public int wounds(double lost) {
            return switch (this) {
                case SLASH -> lost >= 0.3 ? 2 : 1;
                case STAB -> lost >= 0.2 ? 2 : 1;
                case BLUNT -> lost >= 0.3 ? 2 : 1;
                case CRUSH -> lost >= 0.15 ? 4 : 3;
                case SLAM -> lost >= 0.15 ? 5 : 4;
                case BURN -> lost >= 0.2 ? 2 : 1;
                case BLAST -> (int)Math.max(3, Math.min(6, 2 + Math.round(lost * 8.0)));
                case FALL -> lost >= 0.25 ? 2 : 1;
                case SCRATCH -> lost >= 0.1 ? 2 : 1;
                case BURST -> 1;
                case INTERNAL -> 0;
                case MAUL -> lost >= 0.25 ? 6 : 5;
            };
        }

        /** True if it draws blood where it lands. */
        public boolean bleeds() {
            return this != BURN && this != INTERNAL && this != FALL && this != BLUNT && this != SCRATCH;
        }
    }

    /** Ticks a blow waits for the health it costs (the hit and the health arrive a moment apart). */
    public static final long BLOW_TICKS = 10L;

    private final List<Wound> wounds = new ArrayList<>();
    private double deficit;
    private boolean slim;
    private Blow blow;
    private Spot blowSpot;
    private double blowAngle;
    private long blowUntil = Long.MIN_VALUE;

    /** The health missing now (0 healthy .. 1 dead). */
    public double deficit() {
        return this.deficit;
    }

    public List<Wound> wounds() {
        return Collections.unmodifiableList(this.wounds);
    }

    public void slim(boolean slim) {
        this.slim = slim;
    }

    /** How open a wound is now: fully while at least its threshold is missing, closing over its size as health returns. */
    public double openness(Wound w) {
        double t = (this.deficit - (w.threshold() - w.size())) / Math.max(1.0E-6, w.size());
        double c = Math.max(0.0, Math.min(1.0, t));
        return c * c * (3.0 - 2.0 * c);
    }

    /**
     * Health is now {@code health} of {@code max}: a loss opens wounds (placed from {@code seed}), a gain closes them; full health
     * or death clears the body.
     */
    public void health(double health, double max, int seed) {
        health(health, max, seed, Long.MIN_VALUE);
    }

    /**
     * {@link #health(double, double, int)} at game time {@code time}: a loss within {@link #BLOW_TICKS} of a {@link #blow} takes
     * that blow's shape.
     */
    public void health(double health, double max, int seed, long time) {
        double now = max <= 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, 1.0 - health / max));
        if (health <= 0.0 || now <= 1.0E-4) {
            this.wounds.clear();
            this.deficit = now;
            return;
        }
        double lost = now - this.deficit;
        if (lost > 0.004) {
            Blow shaped = this.blow != null && time != Long.MIN_VALUE && time <= this.blowUntil ? this.blow : null;
            // 5.0.17: fewer wounds, much bigger ones: a hit leaves its wound, not a scatter of scratches.
            int count = shaped != null ? shaped.wounds(lost) : (int)Math.max(1, Math.min(3, Math.round(lost * 8.0)));
            for (int i = 0; i < count; i++) {
                double share = lost / count;
                double threshold = this.deficit + lost * (i + 1) / count;
                double size = Math.min(threshold, share * CLOSING);
                Wound w = shaped != null ? shaped(shaped, i, seed * 31 + i * 7, lost, threshold, size) : place(seed * 31 + i * 7, lost, threshold, size);
                if (w != null) {
                    add(w);
                }
            }
            this.blow = null;
        }
        this.deficit = now;
        this.wounds.removeIf(w -> this.openness(w) <= 0.0);
    }

    /**
     * A blow is landing at game time {@code time}: the health it costs (if it arrives within {@link #BLOW_TICKS}) opens wounds of
     * its shape, the first at {@code spot} (null: anywhere fitting), a slash's gash running at {@code angle} (radians: 0 across the
     * side, pi/2 down it).
     */
    public void blow(Blow blow, Spot spot, double angle, long time) {
        this.blow = blow;
        this.blowSpot = spot;
        this.blowAngle = angle;
        this.blowUntil = time + BLOW_TICKS;
    }

    /**
     * The {@code index}th wound of a shaped blow that cost {@code lost} of the health, the first where it landed (null: somewhere
     * fitting). Much bigger than before 5.0.17: a sword opens a gash across the chest down to the ribs, an arrow a hole, a fist a
     * bruise or a broken bone, an explosion tears the flesh out of the side facing it.
     */
    Wound shaped(Blow blow, int index, int seed, double lost, double threshold, double size) {
        double k = Math.max(0.3, Math.min(1.5, lost / 0.3));
        Spot spot = this.blowSpot;
        Spot at = spot != null ? spot : random(seed);
        switch (blow) {
            case SLASH -> {
                if (index == 0) {
                    boolean deep = lost >= 0.1;
                    return make(at, deep ? Wound.Kind.GASH : Wound.Kind.CUT, along(at.part(), this.blowAngle, seed), 9.0 * (0.55 + 0.3 * k),
                        deep ? 1.0 + 0.9 * k : 0.5 + 0.3 * k, seed, threshold, size);
                }
                return make(beside(at, this.blowAngle, seed), Wound.Kind.CUT, along(at.part(), this.blowAngle, seed), 6.0 * (0.5 + 0.3 * k), 0.6,
                    seed, threshold, size);
            }
            case STAB -> {
                // 6.0.11: an arrow's hole is plain to see even when it cost little (it was a pixel wide, lost on the skin).
                double kv = Math.max(VISIBLE, k);
                if (index == 0) {
                    if (lost >= 0.35) {
                        return make(at, Wound.Kind.CAVITY, hash(seed) * Math.PI, 2.8 + 1.2 * kv, 2.6 + 1.0 * kv, seed, threshold, size);
                    }
                    double width = 2.2 + 0.9 * kv;
                    return make(at, Wound.Kind.HOLE, 0.0, width, width, seed, threshold, size);
                }
                // Through and out the other side: the exit is bigger and torn.
                return make(through(at), Wound.Kind.TEAR, hash(seed + 3) * Math.PI, 2.4 + 1.0 * kv, 1.8 + 0.6 * kv, seed, threshold, size);
            }
            case BLUNT -> {
                if (index > 0) {
                    return make(beside(at, 0.0, seed), Wound.Kind.BRUISE, hash(seed) * Math.PI, 2.4 + 1.5 * k, 2.0 + 1.0 * k, seed, threshold, size);
                }
                if (lost < 0.12) {
                    return make(at, Wound.Kind.BRUISE, hash(seed) * Math.PI, 2.4 + 2.5 * k, 2.0 + 2.0 * k, seed, threshold, size);
                }
                if (lost < 0.3 || at.part() == Wound.Part.HEAD) {
                    return make(at, at.part() == Wound.Part.HEAD && lost >= 0.3 ? Wound.Kind.GASH : Wound.Kind.TEAR, along(at.part(), 0.0, seed),
                        2.4 + 1.6 * k, 1.0 + 0.6 * k, seed, threshold, size);
                }
                return make(at, Wound.Kind.FRACTURE, along(at.part(), 0.0, seed), 2.6 + 1.2 * k, 1.1 + 0.5 * k, seed, threshold, size);
            }
            case CRUSH -> {
                // Where the tail wrapped round him: the chest and back, then the arms pinned to it.
                boolean hard = lost >= 0.15;
                return switch (index % 4) {
                    case 0 -> make(new Spot(Wound.Part.BODY, Wound.Face.FRONT, 3.0 + 2.0 * hash(seed), 3.5 + 3.0 * hash(seed + 1)),
                        hard ? Wound.Kind.CAVITY : Wound.Kind.TEAR, (hash(seed + 2) - 0.5) * 1.2, 3.8 + 1.4 * k, 3.0 + 1.2 * k, seed, threshold, size);
                    case 1 -> make(new Spot(Wound.Part.BODY, Wound.Face.BACK, 2.5 + 3.0 * hash(seed), 3.0 + 5.0 * hash(seed + 1)), Wound.Kind.TEAR,
                        (hash(seed + 2) - 0.5) * 1.2, 3.6 + 1.2 * k, 1.8 + 0.8 * k, seed, threshold, size);
                    case 2 -> make(new Spot(Wound.Part.RIGHT_ARM, Wound.Face.RIGHT, 2.0, 2.5 + 4.0 * hash(seed)), hard ? Wound.Kind.FRACTURE
                        : Wound.Kind.BRUISE, along(Wound.Part.RIGHT_ARM, 0.0, seed), 3.0 + 1.0 * k, 1.4 + 0.6 * k, seed, threshold, size);
                    default -> make(new Spot(Wound.Part.LEFT_ARM, Wound.Face.LEFT, 2.0, 2.5 + 4.0 * hash(seed)), hard ? Wound.Kind.FRACTURE
                        : Wound.Kind.BRUISE, along(Wound.Part.LEFT_ARM, 0.0, seed), 3.0 + 1.0 * k, 1.4 + 0.6 * k, seed, threshold, size);
                };
            }
            case SLAM -> {
                // Slammed into the ground on his back: the back torn open to the spine, the backs of the legs, the back of the head.
                boolean hard = lost >= 0.15;
                return switch (index % 5) {
                    case 0 -> make(new Spot(Wound.Part.BODY, Wound.Face.BACK, 4.0, 3.5 + 4.0 * hash(seed)), hard ? Wound.Kind.CAVITY : Wound.Kind.TEAR,
                        (hash(seed + 2) - 0.5) * 0.8, 4.2 + 1.2 * k, 3.2 + 1.0 * k, seed, threshold, size);
                    case 1 -> make(new Spot(Wound.Part.RIGHT_LEG, Wound.Face.BACK, 2.0, 3.0 + 5.0 * hash(seed)), Wound.Kind.TEAR,
                        along(Wound.Part.RIGHT_LEG, 0.0, seed), 3.6 + 1.2 * k, 1.6 + 0.6 * k, seed, threshold, size);
                    case 2 -> make(new Spot(Wound.Part.LEFT_LEG, Wound.Face.BACK, 2.0, 3.0 + 5.0 * hash(seed)), hard ? Wound.Kind.TEAR : Wound.Kind.BRUISE,
                        along(Wound.Part.LEFT_LEG, 0.0, seed), 3.6 + 1.2 * k, 1.6 + 0.6 * k, seed, threshold, size);
                    case 3 -> make(new Spot(Wound.Part.HEAD, Wound.Face.BACK, 4.0, 4.5), Wound.Kind.GASH, (hash(seed + 2) - 0.5) * 1.0, 3.6 + 1.0 * k,
                        1.0 + 0.4 * k, seed, threshold, size);
                    default -> make(new Spot(Wound.Part.LEFT_ARM, Wound.Face.BACK, 2.0, 4.0), Wound.Kind.BRUISE, 0.0, 3.0, 2.4, seed, threshold, size);
                };
            }
            case BURN -> {
                Spot where = index == 0 ? at : random(seed);
                double length = 2.6 + 2.4 * k;
                return make(where, Wound.Kind.BURN, hash(seed) * Math.PI, length, length * 0.8, seed, threshold, size);
            }
            case BLAST -> {
                if (index == 0) {
                    return make(at, Wound.Kind.CAVITY, hash(seed) * Math.PI, 3.0 + 2.0 * k, 2.6 + 1.6 * k, seed, threshold, size);
                }
                // Shrapnel and torn flesh all over the side that faced it, and a scorch.
                Spot where = facing(at.face(), seed);
                Wound.Kind kind = index == 1 ? Wound.Kind.BURN : index % 2 == 0 ? Wound.Kind.HOLE : Wound.Kind.TEAR;
                double length = kind == Wound.Kind.HOLE ? 1.0 + 0.4 * k : kind == Wound.Kind.BURN ? 2.4 + 1.2 * k : 2.6 + 1.2 * k;
                double width = kind == Wound.Kind.HOLE ? length : kind == Wound.Kind.BURN ? length * 0.8 : 1.2 + 0.5 * k;
                return make(where, kind, hash(seed + 5) * Math.PI, length, width, seed, threshold, size);
            }
            case FALL -> {
                // Landed on his feet: the shins and knees; a long fall snaps a shin and the bone comes through.
                Wound.Part leg = (hash(seed) < 0.5) == (index == 0) ? Wound.Part.RIGHT_LEG : Wound.Part.LEFT_LEG;
                Spot shin = new Spot(leg, Wound.Face.FRONT, 2.0, 6.5 + 3.0 * hash(seed + 1));
                if (index == 0 && lost >= 0.25) {
                    return make(shin, Wound.Kind.FRACTURE, along(leg, 0.0, seed), 3.0 + 1.2 * k, 1.3 + 0.5 * k, seed, threshold, size);
                }
                return make(shin, lost >= 0.25 ? Wound.Kind.TEAR : Wound.Kind.BRUISE, along(leg, 0.0, seed), 3.0 + 1.5 * k, 1.8 + 0.8 * k, seed,
                    threshold, size);
            }
            case SCRATCH -> {
                Spot where = index == 0 ? at : beside(at, 0.4, seed);
                return make(where, Wound.Kind.CUT, along(where.part(), (hash(seed) - 0.5) * 2.0, seed), 1.8 + 1.4 * k, 0.35, seed, threshold, size);
            }
            case BURST -> {
                return make(at, Wound.Kind.CAVITY, hash(seed) * Math.PI, 2.4 + 1.8 * k, 2.2 + 1.6 * k, seed, threshold, size);
            }
            case MAUL -> {
                // 6.0.11: a bite. The flesh torn open where the jaws closed (a hard one down to the bone), the teeth's punctures above
                // and below it, then a claw's rakes down the side.
                double kv = Math.max(VISIBLE, k);
                double angle = along(at.part(), this.blowAngle, seed);
                double length = 4.2 + 1.6 * kv;
                double width = 2.2 + 0.8 * kv;
                if (index == 0) {
                    return make(at, lost >= 0.25 ? Wound.Kind.GASH : Wound.Kind.TEAR, angle, length, width, seed, threshold, size);
                }
                if (index <= 3) {
                    double reach = width * 0.5 + 1.1;
                    double run = index == 3 ? 0.0 : (index == 1 ? 0.3 : -0.3) * length;
                    double tooth = 1.4 + 0.3 * kv;
                    return make(shifted(at, angle, run, index == 3 ? -reach : reach), Wound.Kind.HOLE, 0.0, tooth, tooth, seed, threshold, size);
                }
                double rake = (index == 4 ? 1.0 : -1.0) * (width * 0.5 + 2.4);
                return make(shifted(at, angle, 0.0, rake), Wound.Kind.CUT, angle, 4.0 + 1.2 * kv, 0.7, seed, threshold, size);
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * A wound of {@code kind} at {@code spot}, its outline {@code length} by {@code width} pixels turned by {@code angle}: shrunk to
     * fit its side if it must, and moved in so all of it is on the side. Never over the face.
     */
    Wound make(Spot spot, Wound.Kind kind, double angle, double length, double width, int seed, double threshold, double size) {
        Wound.Part part = spot.part();
        Wound.Face face = spot.face();
        double u = spot.u();
        double v = spot.v();
        if (part == Wound.Part.HEAD && face == Wound.Face.FRONT) {
            face = WoundShape.hash(seed + 9) < 0.5 ? Wound.Face.LEFT : Wound.Face.RIGHT;
            u = 4.0;
        }
        double[] extent = face.size(part.box(this.slim));
        double c = Math.abs(Math.cos(angle));
        double s = Math.abs(Math.sin(angle));
        double room = 0.3;
        double hu = c * length * 0.5 + s * width * 0.5;
        double hv = s * length * 0.5 + c * width * 0.5;
        double fit = Math.min(1.0, Math.min((extent[0] - 2.0 * room) / Math.max(1.0E-6, 2.0 * hu), (extent[1] - 2.0 * room) / Math.max(1.0E-6, 2.0 * hv)));
        length *= fit;
        width *= fit;
        hu *= fit;
        hv *= fit;
        double vMin = part == Wound.Part.HEAD ? Math.max(2.2, room + hv) : room + hv;
        return new Wound(part, face, kind, clampTo(u, room + hu, extent[0] - room - hu, extent[0]), clampTo(v, vMin, extent[1] - room - hv, extent[1]),
            angle, length, width, seed, threshold, size);
    }

    /** {@code value} kept between {@code min} and {@code max}, or the middle of the side ({@code extent}) when they cross. */
    private static double clampTo(double value, double min, double max, double extent) {
        return min > max ? extent * 0.5 : Math.max(min, Math.min(max, value));
    }

    /** On the long limbs a wound runs mostly along the limb, down to the bone along it; on the torso and head as it was struck. */
    static double along(Wound.Part part, double angle, int seed) {
        if (part == Wound.Part.BODY || part == Wound.Part.HEAD) {
            return angle;
        }
        return Math.PI * 0.5 + (WoundShape.hash(seed + 21) - 0.5) * 0.9;
    }

    /** {@code spot} moved {@code run} pixels along a stroke at {@code angle} and {@code across} it, kept on its side. */
    Spot shifted(Spot spot, double angle, double run, double across) {
        double[] extent = spot.face().size(spot.part().box(this.slim));
        double u = spot.u() + Math.cos(angle) * run - Math.sin(angle) * across;
        double v = spot.v() + Math.sin(angle) * run + Math.cos(angle) * across;
        return new Spot(spot.part(), spot.face(), Math.max(0.5, Math.min(extent[0] - 0.5, u)), Math.max(0.5, Math.min(extent[1] - 0.5, v)));
    }

    /** A spot a little to the side of {@code spot}, across a stroke at {@code angle} (a second claw, a second cut). */
    Spot beside(Spot spot, double angle, int seed) {
        double offset = (WoundShape.hash(seed + 13) < 0.5 ? -1.0 : 1.0) * (1.4 + 0.5 * WoundShape.hash(seed + 14));
        double[] extent = spot.face().size(spot.part().box(this.slim));
        return new Spot(spot.part(), spot.face(), Math.max(0.5, Math.min(extent[0] - 0.5, spot.u() - Math.sin(angle) * offset)),
            Math.max(0.5, Math.min(extent[1] - 0.5, spot.v() + Math.cos(angle) * offset)));
    }

    /** The same place on the far side of the part: where a stab that went through comes out. */
    Spot through(Spot spot) {
        Wound.Face far = switch (spot.face()) {
            case FRONT -> Wound.Face.BACK;
            case BACK -> Wound.Face.FRONT;
            case LEFT -> Wound.Face.RIGHT;
            case RIGHT -> Wound.Face.LEFT;
        };
        if (spot.part() == Wound.Part.HEAD && far == Wound.Face.FRONT) {
            far = Wound.Face.BACK;
        }
        double[] extent = far.size(spot.part().box(this.slim));
        return new Spot(spot.part(), far, Math.max(0.5, extent[0] - spot.u()), spot.v());
    }

    /** Somewhere on the body for a hit that came from nowhere in particular: mostly the torso, the limbs often, the head now and then. */
    Spot random(int seed) {
        double roll = WoundShape.hash(seed);
        Wound.Part part = roll < 0.46 ? Wound.Part.BODY : roll < 0.61 ? Wound.Part.RIGHT_ARM : roll < 0.76 ? Wound.Part.LEFT_ARM
            : roll < 0.86 ? Wound.Part.RIGHT_LEG : roll < 0.96 ? Wound.Part.LEFT_LEG : Wound.Part.HEAD;
        double side = WoundShape.hash(seed + 1);
        Wound.Face face = part == Wound.Part.HEAD ? (side < 0.4 ? Wound.Face.LEFT : side < 0.8 ? Wound.Face.RIGHT : Wound.Face.BACK)
            : side < 0.5 ? Wound.Face.FRONT : side < 0.75 ? Wound.Face.BACK : side < 0.875 ? Wound.Face.RIGHT : Wound.Face.LEFT;
        double[] extent = face.size(part.box(this.slim));
        return new Spot(part, face, extent[0] * (0.2 + 0.6 * WoundShape.hash(seed + 6)), extent[1] * (0.2 + 0.6 * WoundShape.hash(seed + 7)));
    }

    /** Somewhere on a part's {@code face} (a blast's shrapnel all over the side that faced it). */
    Spot facing(Wound.Face face, int seed) {
        double roll = WoundShape.hash(seed + 31);
        Wound.Part part = roll < 0.4 ? Wound.Part.BODY : roll < 0.55 ? Wound.Part.RIGHT_ARM : roll < 0.7 ? Wound.Part.LEFT_ARM
            : roll < 0.83 ? Wound.Part.RIGHT_LEG : roll < 0.96 ? Wound.Part.LEFT_LEG : Wound.Part.HEAD;
        double[] extent = face.size(part.box(this.slim));
        return new Spot(part, face, extent[0] * (0.2 + 0.6 * WoundShape.hash(seed + 32)), extent[1] * (0.15 + 0.7 * WoundShape.hash(seed + 33)));
    }

    /** A puncture where a spike sat, on {@code part}'s {@code face} at ({@code u}, {@code v}); it closes with the health it cost and a little more. */
    public void puncture(Wound.Part part, Wound.Face face, double u, double v, int seed) {
        double size = Math.max(0.06, Math.min(this.deficit, 0.15));
        if (this.deficit <= 1.0E-3) {
            return;
        }
        double width = 1.3 + 0.4 * WoundShape.hash(seed);
        add(new Wound(part, face, Wound.Kind.HOLE, u, v, 0.0, width, width, seed, this.deficit, size));
    }

    /** Where on the body a point lies: a part, its side, and the spot on that side ({@code u} across, {@code v} down, pixels). */
    public record Spot(Wound.Part part, Wound.Face face, double u, double v) {
    }

    /** Model pixels per block (the player model is drawn at 15/16). */
    static final double PIXELS = 16.0 / 0.9375;

    /**
     * The spot a spike stuck in a body went in at: its point {@code right}, {@code up} (from the feet) and {@code forward} of the
     * body (blocks, in the body's own frame), pointing ({@code dirRight}, {@code dirUp}, {@code dirForward}) into it. The part is
     * the one its point is in (or nearest), the side the one it came through, standing still.
     */
    public static Spot spot(double right, double up, double forward, double dirRight, double dirUp, double dirForward, boolean slim) {
        // Model space: x to the model's left, y down from the neck (24 pixels up from the feet), z backward.
        double x = -right * PIXELS;
        double y = 24.0 - up * PIXELS;
        double z = -forward * PIXELS;
        double[][] pivots = {{0, 0, 0}, {0, 0, 0}, {-5, 2, 0}, {5, 2, 0}, {-1.9, 12, 0}, {1.9, 12, 0}};
        Wound.Part best = Wound.Part.BODY;
        double nearest = Double.MAX_VALUE;
        double[] local = null;
        for (Wound.Part part : Wound.Part.values()) {
            double[] pivot = pivots[part.ordinal()];
            double[] box = part.box(slim);
            double lx = x - pivot[0];
            double ly = y - pivot[1];
            double lz = z - pivot[2];
            double dx = Math.max(0.0, Math.max(box[0] - lx, lx - box[3]));
            double dy = Math.max(0.0, Math.max(box[1] - ly, ly - box[4]));
            double dz = Math.max(0.0, Math.max(box[2] - lz, lz - box[5]));
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < nearest) {
                nearest = distance;
                best = part;
                local = new double[] {lx, ly, lz};
            }
        }
        // The side it came in through: the one facing back along the way it points.
        double mx = -dirRight;
        double mz = -dirForward;
        Wound.Face face = Math.abs(mz) >= Math.abs(mx) ? (mz > 0.0 ? Wound.Face.FRONT : Wound.Face.BACK) : (mx > 0.0 ? Wound.Face.RIGHT : Wound.Face.LEFT);
        if (best == Wound.Part.HEAD && face == Wound.Face.FRONT) {
            face = mx > 0.0 ? Wound.Face.RIGHT : Wound.Face.LEFT;
        }
        double[] box = best.box(slim);
        double[] size = face.size(box);
        double u = switch (face) {
            case FRONT -> local[0] - box[0];
            case BACK -> box[3] - local[0];
            case RIGHT -> box[5] - local[2];
            case LEFT -> local[2] - box[2];
        };
        double v = local[1] - box[1];
        double margin = 0.8;
        return new Spot(best, face, Math.max(margin, Math.min(size[0] - margin, u)), Math.max(margin, Math.min(size[1] - margin, v)));
    }

    private static double hash(int n) {
        return WoundShape.hash(n);
    }

    private void add(Wound w) {
        this.wounds.add(w);
        while (this.wounds.size() > MOST) {
            this.wounds.remove(0);
        }
    }

    /**
     * A new wound for a hit nobody said anything about (the server didn't, or it came from another mod) that cost {@code lost} of
     * the player's health: somewhere on the body (mostly the torso, the arms and legs often, the head now and then, never over the
     * face), a cut, a tear, or for a hard hit a gash down to the bone.
     */
    Wound place(int seed, double lost, double threshold, double size) {
        Spot at = random(seed);
        double k = Math.max(0.3, Math.min(1.5, lost / 0.3));
        double roll = WoundShape.hash(seed + 2);
        Wound.Kind kind = lost >= 0.25 && roll < 0.6 ? Wound.Kind.GASH : roll < 0.55 ? Wound.Kind.TEAR : Wound.Kind.CUT;
        double angle = along(at.part(), (WoundShape.hash(seed + 4) - 0.5) * 1.6, seed);
        double length = (kind == Wound.Kind.TEAR ? 3.4 : 4.6) * (0.7 + 0.5 * k) * (0.85 + 0.3 * WoundShape.hash(seed + 3));
        double width = kind == Wound.Kind.GASH ? 1.0 + 0.8 * k : kind == Wound.Kind.TEAR ? 1.4 + 0.7 * k : 0.6 + 0.2 * k;
        return make(at, kind, angle, length, width, seed, threshold, size);
    }
}
