package com.hex.kagune.control.bikaku;

import com.hex.kagune.control.graft.GraftMesh;

/**
 * 3.4.9: where the Bikaku worm is, each frame (pure: the renderer, the previews and the tests pose it the same way). All in the body's own
 * frame: blocks from the feet, x to its right, y up, z ahead.
 *
 * <p>At rest it rises out of the lower back, arches up behind him and over his right shoulder, its head hanging there looking about: it
 * sways, its body undulating, its head turning in short predatory snaps from one place to the next, and now and then its mouth opens,
 * showing its teeth, and shuts. A bite pulls it back, throws it out at what it bites with its mouth gaping, snaps the mouth shut on it,
 * shakes it, and draws it back.
 */
public final class MawMotion {
    private MawMotion() {
    }

    /** Where the worm leaves the body: the lower back, a little in. */
    public static final double[] ROOT = {0.0, 0.84, -0.13};
    /**
     * 3.4.9 (the owner: "this [kagune] should be longer than all the others. It should like have zigzags and all ... make sure it's
     * long"): where its head rests, towering up over the right shoulder, and the zigzag of coils it climbs there by behind him (side to
     * side, back and up). About eight blocks of it at rest.
     */
    public static final double[] HEAD = {1.15, 2.95, 0.25};
    static final double[][] ZIGZAG = {{0.95, 1.05, -1.05}, {-0.75, 1.5, -1.75}, {1.05, 2.15, -2.25}, {-0.55, 2.85, -2.2}, {0.75, 3.45, -1.45}};
    /** Over the right shoulder: where a worm thrown out ahead passes him (never through him). */
    static final double[] SHOULDER = {0.62, 2.15, -0.25};

    /** 3.4.9: {@link #SHOULDER}, for the server (where the worm's reach is measured from) and the renderer. */
    public static double[] shoulder() {
        return SHOULDER.clone();
    }

    /** The worm this frame: its control points (root first, the mouth last), the way its back faces at the root, how wide its mouth is. */
    public record Shape(double[][] path, double[] dorsal, double aperture) {
    }

    // ---------------------------------------------------------------- at rest

    /** At rest, {@code time} ticks into the world; {@code seed} gives each worm its own rhythm. */
    public static Shape idle(double time, int seed) {
        double[] head = idleHead(time, seed);
        double[] look = idleLook(time, seed);
        return new Shape(path(head, look, time, seed, 0.0), dorsal(), idleMouth(time, seed));
    }

    /** Its head's place at rest: towering over the right shoulder, swaying. */
    static double[] idleHead(double time, int seed) {
        double t = time + seed * 97.0;
        return new double[] {HEAD[0] + 0.24 * Math.sin(t * 0.031) + 0.07 * Math.sin(t * 0.083 + 1.0),
            HEAD[1] + 0.16 * Math.sin(t * 0.047 + 0.5) + 0.05 * Math.sin(t * 0.13),
            HEAD[2] + 0.2 * Math.sin(t * 0.027 + 2.0)};
    }

    /**
     * The way its head looks at rest: ahead and about, held on one spot and then snapping to the next (a predator scanning), a quiver on
     * top. Unit, in the body's frame.
     */
    static double[] idleLook(double time, int seed) {
        double t = time + seed * 61.0;
        double hold = 46.0 + 14.0 * GraftMesh.hash(seed * 7);
        int beat = (int)Math.floor(t / hold);
        double into = (t - beat * hold) / 7.0;
        double w = GraftMesh.smooth(GraftMesh.clamp01(into));
        double yaw = lerp(scanYaw(seed, beat - 1), scanYaw(seed, beat), w) + 0.04 * Math.sin(t * 0.9);
        double pitch = lerp(scanPitch(seed, beat - 1), scanPitch(seed, beat), w) + 0.03 * Math.sin(t * 1.1 + 2.0);
        return new double[] {-Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)};
    }

    /** Where it looks on the {@code beat}th spot: anywhere from well to the left of ahead to well round to the right. */
    private static double scanYaw(int seed, int beat) {
        return -0.35 + 1.25 * (GraftMesh.hash(seed * 131 + beat * 17) - 0.3);
    }

    private static double scanPitch(int seed, int beat) {
        return -0.35 + 0.45 * GraftMesh.hash(seed * 53 + beat * 29);
    }

    /**
     * How wide its mouth is at rest: nearly shut, breathing; now and then it gapes, showing its teeth (open over half a second, held,
     * shut), and in between it tastes the air (a little open).
     */
    public static double idleMouth(double time, int seed) {
        double t = time + seed * 211.0;
        double period = 150.0 + 60.0 * GraftMesh.hash(seed * 19);
        double phase = t - Math.floor(t / period) * period;
        double rest = 0.05 + 0.03 * Math.sin(t * 0.2);
        double gape = phase < 12.0 ? GraftMesh.smooth(phase / 12.0) : phase < 26.0 ? 1.0 : phase < 38.0 ? 1.0 - GraftMesh.smooth((phase - 26.0) / 12.0) : 0.0;
        double taste = phase > 80.0 && phase < 96.0 ? Math.sin(Math.PI * (phase - 80.0) / 16.0) : 0.0;
        return Math.max(rest, Math.max(0.78 * gape + 0.06 * gape * Math.sin(t * 1.3), 0.3 * taste));
    }

    // ---------------------------------------------------------------- 3.4.9: this frame, from what the server says

    /**
     * The worm this frame for a player whose worm is making {@code bite} ({@code age} ticks into it, at {@code target} in the body's frame,
     * held to its reach), or at rest when it makes none or the bite is over. The renderer and the previews both pose it here.
     */
    public static Shape of(double time, int seed, BikakuRules.Bite bite, double age, double[] target) {
        if (bite == null || bite == BikakuRules.Bite.NONE || target == null || !(age >= 0.0) || age >= bite.ticks()) {
            return idle(time, seed);
        }
        return bite(time, seed, BikakuRules.reach(target), age, bite.ticks(), bite.land(), bite.style());
    }

    /** First person: how far the near part of the worm is kept to the right and up (blocks, at the eyes), and how far out that fades. */
    static final double[] FIRST_PERSON_ASIDE = {0.8, 0.35, -0.1};
    static final double FIRST_PERSON_FADE = 3.4;
    /** First person: the worm's line kept this far from the eyes. */
    public static final double FIRST_PERSON_CLEAR = 1.25;

    /**
     * 3.4.9, his own first person (the body's frame, his eyes at {@code eye}): the worm's points near him moved aside to the right and up,
     * fading out over a few blocks, then kept {@link #FIRST_PERSON_CLEAR} from the eyes, so a bite comes in past the top right of his view
     * and never covers what it bites. What it bites, further out, is where it is. Only he sees it so; everyone else sees it as it is.
     */
    public static double[][] firstPerson(double[][] points, double[] eye) {
        double[][] out = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            double d = GraftMesh.length3(GraftMesh.sub(points[i], eye));
            double fade = GraftMesh.smooth(GraftMesh.clamp01(1.0 - d / FIRST_PERSON_FADE));
            out[i] = GraftMesh.add(points[i], GraftMesh.mul(FIRST_PERSON_ASIDE, fade));
        }
        return clearOfEyes(out, eye, FIRST_PERSON_CLEAR);
    }

    /**
     * 3.4.9, first person: the worm's points (camera-relative, the eyes at {@code eye}) kept at least {@code radius} from the eyes (pushed
     * straight out), so nothing of it comes into the camera; what passes him still shows, round him.
     */
    public static double[][] clearOfEyes(double[][] points, double[] eye, double radius) {
        double[][] out = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            double[] from = GraftMesh.sub(points[i], eye);
            double d = GraftMesh.length3(from);
            out[i] = d >= radius ? points[i] : GraftMesh.add(eye, GraftMesh.mul(d < 1.0E-6 ? new double[] {0.0, -1.0, 0.0} : GraftMesh.mul(from, 1.0 / d), radius));
        }
        return out;
    }

    /**
     * 3.4.9: the worm while it grows out of his back ({@code grown} 0 .. 1): its points cut where that share of its length (along the line
     * through them) ends, so its first part shows, mouth first, and at 1 it is exactly the whole worm.
     */
    public static double[][] grown(double[][] points, double grown) {
        if (grown >= 1.0 || points.length < 2) {
            return points;
        }
        double total = 0.0;
        for (int i = 1; i < points.length; i++) {
            total += GraftMesh.length3(GraftMesh.sub(points[i], points[i - 1]));
        }
        double want = Math.max(0.15, total * Math.max(0.0, grown));
        java.util.List<double[]> kept = new java.util.ArrayList<>();
        kept.add(points[0]);
        double along = 0.0;
        for (int i = 1; i < points.length; i++) {
            double step = GraftMesh.length3(GraftMesh.sub(points[i], points[i - 1]));
            if (along + step >= want) {
                double f = step < 1.0E-9 ? 1.0 : (want - along) / step;
                kept.add(GraftMesh.lerp(points[i - 1], points[i], GraftMesh.clamp01(f)));
                break;
            }
            kept.add(points[i]);
            along += step;
        }
        return kept.toArray(new double[0][]);
    }

    // ---------------------------------------------------------------- a bite

    /** A bite of style 0 (the M1 string's first, low over the shoulder): {@link #bite(double, int, double[], double, double, double, int)}. */
    public static Shape bite(double time, int seed, double[] target, double age, double total, double land) {
        return bite(time, seed, target, age, total, land, 0);
    }

    /**
     * A bite at {@code target} (body frame), {@code age} ticks into one {@code total} long that lands at {@code land}: drawn back with its
     * mouth opening, thrown out at the target gaping, snapped shut on it, shaken, drawn back to rest. 3.4.9: {@code style} is the way the
     * M1 string's bites come: 0 drawn back a little and thrown low over the shoulder, 1 reared up high and brought down on it, 2 drawn
     * far back and thrown in a wide hook from the side, held and shaken harder.
     */
    public static Shape bite(double time, int seed, double[] target, double age, double total, double land, int style) {
        double[] restHead = idleHead(time, seed);
        double[] restLook = idleLook(time, seed);
        double[] toward = GraftMesh.normalize(GraftMesh.sub(target, SHOULDER));
        // His right, square to the way it goes (the heavy bite hooks out that way, round the outside, never across his face).
        double[] side = GraftMesh.cross(new double[] {0.0, 1.0, 0.0}, toward);
        side = GraftMesh.dot(side, side) < 1.0E-9 ? new double[] {1.0, 0.0, 0.0} : GraftMesh.normalize(side);
        double[] back = style == 1 ? new double[] {-0.1, 0.8, -0.5} : style == 2 ? new double[] {0.3, 0.45, -0.85} : new double[] {0.08, 0.22, -0.32};
        double pull = style == 2 ? 0.4 : style == 1 ? 0.2 : 0.1;
        double[] coiled = GraftMesh.add(GraftMesh.add(restHead, back), GraftMesh.mul(toward, -pull));
        double[] at = GraftMesh.sub(target, GraftMesh.mul(toward, 0.08));
        double windup = Math.max(1.0, land * 0.42);
        double hold = style == 2 ? 6.0 : 3.0;
        double shakeBy = style == 2 ? 0.24 : 0.14;
        double[] head;
        double[] look;
        double mouth;
        if (age < windup) {
            double w = GraftMesh.smooth(age / windup);
            head = GraftMesh.lerp(restHead, coiled, w);
            look = GraftMesh.normalize(GraftMesh.lerp(restLook, toward, w));
            mouth = lerp(idleMouth(time, seed), 0.45, w);
        } else if (age < land) {
            double w = (age - windup) / Math.max(1.0E-6, land - windup);
            double fast = w * w * (3.0 - 2.0 * w);
            head = GraftMesh.lerp(coiled, at, fast);
            // The way it comes: over the top (1), in a hook from the side (2); straight (0).
            double arc = Math.sin(Math.PI * fast);
            if (style == 1) {
                head = GraftMesh.add(head, new double[] {0.0, 0.7 * arc, 0.0});
            } else if (style == 3) {
                // 3.5.1: a fast snaking lunge (Ravenous Pursuit): weaving side to side as it comes, the weave dying out as it bites.
                double weave = 0.55 * Math.sin(Math.PI * fast * 2.5) * Math.sqrt(1.0 - fast);
                head = GraftMesh.add(head, GraftMesh.add(GraftMesh.mul(side, weave), new double[] {0.0, 0.25 * arc, 0.0}));
            } else if (style == 2) {
                head = GraftMesh.add(head, GraftMesh.add(GraftMesh.mul(side, 0.8 * arc), new double[] {0.0, 0.25 * arc, 0.0}));
            }
            look = style == 0 || style == 3 ? toward : GraftMesh.normalize(GraftMesh.sub(at, GraftMesh.lerp(coiled, at, Math.max(0.0, fast - 0.25))));
            if (GraftMesh.dot(look, look) < 1.0E-9 || !Double.isFinite(look[0])) {
                look = toward;
            }
            look = GraftMesh.normalize(GraftMesh.lerp(look, toward, w));
            mouth = age < land - 1.0 ? lerp(0.45, 1.0, GraftMesh.smooth(w * 1.4)) : lerp(1.0, 0.04, age - (land - 1.0));
        } else {
            double after = Math.max(1.0, total - land - hold);
            if (age < land + hold) {
                double shake = Math.sin((age - land) * 2.6) * shakeBy * (1.0 - (age - land) / hold);
                head = GraftMesh.add(at, GraftMesh.mul(side, shake));
                look = GraftMesh.normalize(GraftMesh.add(toward, GraftMesh.mul(side, shake * 1.5)));
                mouth = 0.04;
            } else {
                double w = GraftMesh.smooth(GraftMesh.clamp01((age - land - hold) / after));
                head = GraftMesh.lerp(at, restHead, w);
                look = GraftMesh.normalize(GraftMesh.lerp(toward, restLook, w));
                mouth = lerp(0.04, idleMouth(time, seed), w);
            }
        }
        if (age < land - 1.0 || age > land + hold) {
            head = clear(head); // in flight, never through his own head or body (on its target, it is where its target is)
        }
        double reach = GraftMesh.smooth(GraftMesh.clamp01(GraftMesh.length3(GraftMesh.sub(head, restHead)) / 2.0));
        return new Shape(path(head, look, time, seed, reach), dorsal(), GraftMesh.clamp01(mouth));
    }

    // ---------------------------------------------------------------- 3.5.0: Devouring Maw and Predatory Drag

    /**
     * 3.5.0: the worm making {@code move}, {@code age} ticks in, at {@code target} (body frame), holding what is at {@code held} (null: none
     * seen), having caught something or not; past its end (or no move), at rest.
     */
    public static Shape move(double time, int seed, BikakuMove move, double age, double[] target, double[] held, boolean caught) {
        if (move == null || target == null || !(age >= 0.0) || age >= (caught ? move.ticks() : move.missedTicks())) {
            return idle(time, seed);
        }
        return switch (move) {
            case MAW -> maw(time, seed, target, held, age, caught);
            case DRAG -> drag(time, seed, target, held, age, caught);
            case ERUPTION -> eruption(time, seed, target, age);
            default -> idle(time, seed);
        };
    }

    /** Devouring Maw: where its catch is held up before him to be shaken (the body's frame, its middle). */
    public static final double[] MAW_HOLD = {0.85, 2.45, 2.5};
    /** Predatory Drag: beside his right heel, where the worm runs down to the ground on its way out along it. */
    static final double[] GROUND_ANCHOR = {0.5, 0.3, -0.35};
    /** Predatory Drag: lifted high before him, then slammed down here. */
    public static final double[] DRAG_LIFT = {0.7, 3.4, 2.1};
    public static final double[] DRAG_SLAMMED = {0.55, 0.55, 2.2};
    /** How far into what it bites a grabbing mouth sits (blocks short of its middle). */
    static final double GRIP = 0.42;

    /**
     * Devouring Maw's catch, {@code age} ticks in (the body's frame, its middle), bitten where it stood ({@code at}): pulled up before him,
     * shaken violently side to side, flung out to his right (let go at {@link BikakuMove#MAW_RELEASE}). The server holds it here.
     */
    public static double[] mawHeld(double[] at, double age) {
        int land = BikakuMove.MAW.land();
        double w = GraftMesh.smooth(GraftMesh.clamp01((age - land) / (double)(BikakuMove.MAW_PULLED - land)));
        double[] p = GraftMesh.lerp(at, MAW_HOLD, w);
        if (age >= land && age < BikakuMove.MAW_SHAKE_END) {
            double envelope = Math.min(1.0, (age - land) / 3.0) * Math.min(1.0, (BikakuMove.MAW_SHAKE_END - age) / 2.0);
            p = GraftMesh.add(p, new double[] {0.42 * Math.sin((age - land) * 2.5) * envelope, 0.1 * Math.sin((age - land) * 3.7) * envelope, 0.0});
        }
        if (age >= BikakuMove.MAW_SHAKE_END) {
            double t = GraftMesh.smooth(GraftMesh.clamp01((age - BikakuMove.MAW_SHAKE_END) / (double)(BikakuMove.MAW_RELEASE - BikakuMove.MAW_SHAKE_END)));
            p = GraftMesh.add(p, new double[] {1.6 * t, 0.35 * t, -0.2 * t});
        }
        return p;
    }

    /**
     * Devouring Maw, {@code age} ticks in, at {@code target} (body frame); {@code held} where its catch is now (the client's own sight of it,
     * or null: {@link #mawHeld}); {@code caught} whether it caught anything. Drawn far back, its mouth opening extremely wide; thrown out
     * and snapped shut round it; holding it up, shaking; the fling; back to rest. Missed, it snaps on nothing and draws back.
     */
    public static Shape maw(double time, int seed, double[] target, double[] held, double age, boolean caught) {
        BikakuMove move = BikakuMove.MAW;
        int land = move.land();
        double[] restHead = idleHead(time, seed);
        double[] restLook = idleLook(time, seed);
        double[] aim = BikakuRules.reach(target);
        double[] toward = way(SHOULDER, aim);
        double[] coiled = GraftMesh.add(GraftMesh.add(restHead, new double[] {0.25, 0.55, -0.75}), GraftMesh.mul(toward, -0.45));
        double[] bitten = GraftMesh.sub(aim, GraftMesh.mul(toward, GRIP));
        double[] head;
        double[] look;
        double mouth;
        boolean free = true;
        if (age < BikakuMove.MAW_WINDUP) {
            double w = GraftMesh.smooth(age / BikakuMove.MAW_WINDUP);
            head = GraftMesh.lerp(restHead, coiled, w);
            look = GraftMesh.normalize(GraftMesh.lerp(restLook, toward, w));
            mouth = lerp(idleMouth(time, seed), 1.0, w);
        } else if (age < land) {
            double w = (age - BikakuMove.MAW_WINDUP) / (double)(land - BikakuMove.MAW_WINDUP);
            head = GraftMesh.lerp(coiled, bitten, w * w * (3.0 - 2.0 * w));
            look = toward;
            mouth = age < land - 1.0 ? 1.0 : lerp(1.0, 0.04, age - (land - 1.0));
        } else if (!caught) {
            double back = Math.max(1.0, move.missedTicks() - land - 3.0);
            if (age < land + 3.0) {
                head = GraftMesh.add(bitten, new double[] {0.12 * Math.sin((age - land) * 2.6), 0.0, 0.0});
                look = toward;
                mouth = 0.04;
            } else {
                double w = GraftMesh.smooth(GraftMesh.clamp01((age - land - 3.0) / back));
                head = GraftMesh.lerp(bitten, restHead, w);
                look = GraftMesh.normalize(GraftMesh.lerp(toward, restLook, w));
                mouth = lerp(0.04, idleMouth(time, seed), w);
            }
        } else if (age < BikakuMove.MAW_RELEASE) {
            // Clamped on it: wherever it is, the mouth is on it, its own head thrashing on top.
            double[] victim = held != null ? held : mawHeld(aim, age);
            look = way(SHOULDER, victim);
            head = GraftMesh.sub(victim, GraftMesh.mul(look, GRIP));
            double thrash = age < BikakuMove.MAW_SHAKE_END ? 0.1 * Math.sin(age * 3.3) : 0.0;
            head = GraftMesh.add(head, new double[] {thrash, 0.5 * thrash, 0.0});
            mouth = 0.04;
            free = false;
        } else {
            double[] last = mawHeld(aim, BikakuMove.MAW_RELEASE);
            double[] lastLook = way(SHOULDER, last);
            double[] from = GraftMesh.sub(last, GraftMesh.mul(lastLook, GRIP));
            double w = GraftMesh.smooth(GraftMesh.clamp01((age - BikakuMove.MAW_RELEASE) / (double)(move.ticks() - BikakuMove.MAW_RELEASE)));
            head = GraftMesh.lerp(from, restHead, w);
            look = GraftMesh.normalize(GraftMesh.lerp(lastLook, restLook, w));
            // Let go: the jaws spring open, then settle.
            mouth = lerp(0.04 + 0.6 * Math.sin(Math.PI * Math.min(1.0, w * 3.0)), idleMouth(time, seed), w);
        }
        if (free && (age < land - 1.0 || age > land + 3.0)) {
            head = clear(head);
        }
        double reach = GraftMesh.smooth(GraftMesh.clamp01(GraftMesh.length3(GraftMesh.sub(head, restHead)) / 2.0));
        return new Shape(path(head, look, time, seed, reach), dorsal(), GraftMesh.clamp01(mouth));
    }

    /**
     * Predatory Drag's catch, {@code age} ticks in (the body's frame, its middle), bitten where it stood ({@code at}): dragged back across
     * the ground to before him (keeping to its own ground), lifted high, slammed down at {@link BikakuMove#DRAG_SLAM}. The server moves it
     * here.
     */
    public static double[] dragHeld(double[] at, double age) {
        int land = BikakuMove.DRAG.land();
        double[] home = {0.55, at[1], 1.9};
        if (age < BikakuMove.DRAG_HOME) {
            double w = GraftMesh.smooth(GraftMesh.clamp01((age - land) / (double)(BikakuMove.DRAG_HOME - land)));
            double jerk = 0.14 * Math.sin((age - land) * 1.9) * Math.sin(Math.PI * w);
            return GraftMesh.add(GraftMesh.lerp(at, home, w), new double[] {jerk, 0.0, 0.0});
        }
        if (age < BikakuMove.DRAG_LIFTED) {
            double w = GraftMesh.smooth((age - BikakuMove.DRAG_HOME) / (double)(BikakuMove.DRAG_LIFTED - BikakuMove.DRAG_HOME));
            return GraftMesh.lerp(home, DRAG_LIFT, w);
        }
        if (age < BikakuMove.DRAG_SLAM) {
            double w = (age - BikakuMove.DRAG_LIFTED) / (double)(BikakuMove.DRAG_SLAM - BikakuMove.DRAG_LIFTED);
            return GraftMesh.lerp(DRAG_LIFT, DRAG_SLAMMED, w * w);
        }
        return DRAG_SLAMMED.clone();
    }

    /**
     * Predatory Drag, {@code age} ticks in, at {@code target} (body frame); {@code held} where its catch is now (or null: {@link #dragHeld});
     * {@code caught} whether it caught anything. Down to the ground before him, then along the ground at it, mouth open and teeth bared,
     * weaving like a snake; it bites on low, drags it back across the ground to him, lifts it high and slams it down, lets go, back to rest.
     */
    public static Shape drag(double time, int seed, double[] target, double[] held, double age, boolean caught) {
        BikakuMove move = BikakuMove.DRAG;
        int land = move.land();
        double[] restHead = idleHead(time, seed);
        double[] restLook = idleLook(time, seed);
        double[] aim = dragReach(target);
        double[] flat = way(new double[] {0.0, 0.0, 0.0}, new double[] {aim[0], 0.0, aim[2]});
        double[] side = GraftMesh.normalize(GraftMesh.cross(new double[] {0.0, 1.0, 0.0}, flat));
        double[] low = {0.6, 0.35, 0.9};
        double[] bitten = GraftMesh.add(GraftMesh.sub(aim, GraftMesh.mul(flat, GRIP)), new double[] {0.0, -0.45, 0.0});
        bitten[1] = Math.max(0.3, bitten[1]);
        double[] head;
        double[] look;
        double mouth;
        boolean free = true;
        if (age < BikakuMove.DRAG_DOWN) {
            double w = GraftMesh.smooth(age / BikakuMove.DRAG_DOWN);
            head = GraftMesh.lerp(restHead, low, w);
            look = GraftMesh.normalize(GraftMesh.lerp(restLook, GraftMesh.normalize(GraftMesh.add(flat, new double[] {0.0, -0.3, 0.0})), w));
            mouth = lerp(idleMouth(time, seed), 0.85, w);
        } else if (age < land) {
            double w = (age - BikakuMove.DRAG_DOWN) / (double)(land - BikakuMove.DRAG_DOWN);
            double weave = 0.5 * Math.sin(Math.PI * w * 3.0) * (1.0 - w);
            head = GraftMesh.add(GraftMesh.lerp(low, bitten, w), GraftMesh.mul(side, weave));
            look = GraftMesh.normalize(GraftMesh.add(flat, GraftMesh.mul(side, 0.6 * Math.cos(Math.PI * w * 3.0) * (1.0 - w))));
            mouth = age < land - 1.0 ? 1.0 : lerp(1.0, 0.04, age - (land - 1.0));
        } else if (!caught) {
            double w = GraftMesh.smooth(GraftMesh.clamp01((age - land - 2.0) / (double)(move.missedTicks() - land - 2)));
            head = GraftMesh.lerp(bitten, restHead, w);
            look = GraftMesh.normalize(GraftMesh.lerp(flat, restLook, w));
            mouth = lerp(0.04, idleMouth(time, seed), w);
        } else if (age <= BikakuMove.DRAG_SLAM) {
            double[] victim = held != null ? held : dragHeld(aim, age);
            if (age < BikakuMove.DRAG_HOME) {
                // Dragging: its mouth on their legs, pulling back along the ground.
                look = way(new double[] {0.0, 0.0, 0.0}, new double[] {victim[0], 0.0, victim[2]});
                head = GraftMesh.add(GraftMesh.sub(victim, GraftMesh.mul(look, GRIP)), new double[] {0.0, -0.45, 0.0});
                head[1] = Math.max(0.3, head[1]);
            } else {
                // Lifted and slammed: gripping them from below.
                look = GraftMesh.normalize(GraftMesh.add(way(SHOULDER, victim), new double[] {0.0, 0.35, 0.0}));
                head = GraftMesh.sub(victim, GraftMesh.mul(look, GRIP));
            }
            mouth = 0.04;
            free = false;
        } else {
            double[] lastLook = GraftMesh.normalize(GraftMesh.add(way(SHOULDER, DRAG_SLAMMED), new double[] {0.0, 0.35, 0.0}));
            double[] from = GraftMesh.sub(DRAG_SLAMMED, GraftMesh.mul(lastLook, GRIP));
            double w = GraftMesh.smooth(GraftMesh.clamp01((age - BikakuMove.DRAG_SLAM) / (double)(move.ticks() - BikakuMove.DRAG_SLAM)));
            head = GraftMesh.lerp(from, restHead, w);
            look = GraftMesh.normalize(GraftMesh.lerp(lastLook, restLook, w));
            mouth = lerp(0.04 + 0.5 * Math.sin(Math.PI * Math.min(1.0, w * 3.0)), idleMouth(time, seed), w);
        }
        if (free) {
            head = clear(head);
        }
        double reach = GraftMesh.smooth(GraftMesh.clamp01(GraftMesh.length3(GraftMesh.sub(head, restHead)) / 2.0));
        // Out along the ground, it runs down beside his heel; lifted, back over his shoulder.
        double[] anchor = GraftMesh.lerp(GROUND_ANCHOR, SHOULDER, GraftMesh.smooth(GraftMesh.clamp01((head[1] - 0.8) / 1.8)));
        return new Shape(path(head, look, time, seed, reach, anchor), dorsal(), GraftMesh.clamp01(mouth));
    }

    /** Predatory Drag's reach: along the ground, as far as {@link BikakuRules#DRAG_REACH} from his feet. */
    public static double[] dragReach(double[] target) {
        double flat = Math.hypot(target[0], target[2]);
        if (flat <= BikakuRules.DRAG_REACH || flat < 1.0E-9) {
            return target;
        }
        double k = BikakuRules.DRAG_REACH / flat;
        return new double[] {target[0] * k, target[1], target[2] * k};
    }

    /** The unit way from {@code from} to {@code to} (ahead if they are the same). */
    static double[] way(double[] from, double[] to) {
        double[] d = GraftMesh.sub(to, from);
        return GraftMesh.dot(d, d) < 1.0E-12 ? new double[] {0.0, 0.0, 1.0} : GraftMesh.normalize(d);
    }

    // ---------------------------------------------------------------- 3.5.1: Ravenous Pursuit

    /** One of several worms drawn at once: its shape, how thick (1 the kagune's own), how far grown out (0 .. 1), its own seed. */
    public record Worm(Shape shape, double girth, double grown, int seed) {
    }

    /**
     * 3.5.1 (the owner: "they're too close up to each other ... forming a kind of mass in the back ... separate them more, make them
     * further away from the body if need be, just so it doesn't create that mass in the back ever"): each of Ravenous Pursuit's worms
     * keeps to its own lane, well apart. The side worms' lanes are {@link #LANE} out to his right and left, their zigzag drawn in to
     * {@link #LANE_SQUEEZE} of its width so it stays in its lane; the middle worm's is narrow and {@link #MIDDLE_LIFT} higher and
     * {@link #MIDDLE_BACK} further back.
     */
    static final double LANE = 1.5;
    static final double LANE_SQUEEZE = 0.45;
    static final double MIDDLE_SQUEEZE = 0.3;
    static final double MIDDLE_LIFT = 1.1;
    static final double MIDDLE_BACK = 0.5;

    /**
     * Ravenous Pursuit's worm {@code i}'s own frame into his ({@code s}: how far into its lane, 0 .. 1): 0 the worm out in the lane past
     * his right shoulder, 1 the same mirrored past his left, 2 high and further back behind his head. Its own frame is the main worm's, so
     * each poses and bites as the worm does.
     */
    static double[] pursuitFrame(int i, double[] p, double s) {
        return switch (i) {
            case 1 -> new double[] {-lane(p[0], s), p[1], p[2]};
            case 2 -> new double[] {p[0] * (1.0 - s * (1.0 - MIDDLE_SQUEEZE)), p[1] + MIDDLE_LIFT * s, p[2] - MIDDLE_BACK * s};
            default -> new double[] {lane(p[0], s), p[1], p[2]};
        };
    }

    /** The inverse of {@link #pursuitFrame}: a point of the body's frame in worm {@code i}'s own. */
    static double[] pursuitUnframe(int i, double[] p, double s) {
        return switch (i) {
            case 1 -> new double[] {unlane(-p[0], s), p[1], p[2]};
            case 2 -> new double[] {p[0] / (1.0 - s * (1.0 - MIDDLE_SQUEEZE)), p[1] - MIDDLE_LIFT * s, p[2] + MIDDLE_BACK * s};
            default -> new double[] {unlane(p[0], s), p[1], p[2]};
        };
    }

    private static double lane(double x, double s) {
        return x * (1.0 - s * (1.0 - LANE_SQUEEZE)) + s * LANE;
    }

    private static double unlane(double x, double s) {
        return (x - s * LANE) / (1.0 - s * (1.0 - LANE_SQUEEZE));
    }

    /**
     * Ravenous Pursuit, {@code age} ticks in: three worms (worm {@code i} after {@code targets[i]}, body frame; null: the air ahead). The
     * worm thins as two more grow out of the same root; each, on its turn ({@link BikakuMove#pursuitLands}), is drawn back and thrown at
     * its own body in a fast snaking lunge, bites, and goes back; then the two draw back into the root and the worm is whole again.
     */
    public static Worm[] pursuit(double time, int seed, double[][] targets, double age) {
        double split = GraftMesh.smooth(GraftMesh.clamp01(age / BikakuMove.PURSUIT_SPLIT))
            * (1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - BikakuMove.PURSUIT_MERGE) / (double)(BikakuMove.PURSUIT.ticks() - BikakuMove.PURSUIT_MERGE))));
        double thin = lerp(1.0, BikakuRules.PURSUIT_GIRTH, split);
        // The main worm goes out into its lane first, before the others grow (so it never crosses where they come up); at the end they
        // draw back into the root before it comes back to its place.
        int end = BikakuMove.PURSUIT.ticks();
        double out = GraftMesh.smooth(GraftMesh.clamp01(age / 3.0)) * (1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - (end - 4)) / 4.0)));
        double grow = GraftMesh.smooth(GraftMesh.clamp01((age - 1.0) / (BikakuMove.PURSUIT_SPLIT - 1.0)))
            * (1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - BikakuMove.PURSUIT_MERGE) / 5.0)));
        Worm[] worms = new Worm[BikakuMove.PURSUIT_WORMS];
        for (int i = 0; i < worms.length; i++) {
            int own = seed + 101 * i;
            double[] target = targets != null && i < targets.length && targets[i] != null ? targets[i] : new double[] {0.0, 1.4, 5.0};
            int lands = BikakuMove.pursuitLands(i);
            double biteAge = age - (lands - BikakuMove.PURSUIT_LEAD);
            int total = BikakuMove.PURSUIT_LEAD + BikakuMove.PURSUIT_AFTER;
            Shape mine;
            // The main worm moves out into its lane as it splits (and back as they merge); the others grow out already in theirs.
            double into = i == 0 ? out : 1.0;
            if (biteAge >= 0.0 && biteAge < total) {
                double[] there = pursuitUnframe(i, BikakuRules.reach(pursuitReach(target)), into);
                mine = bite(time, own, there, biteAge, total, BikakuMove.PURSUIT_LEAD, 3);
            } else {
                mine = idle(time, own);
            }
            double[][] path = new double[mine.path().length][];
            for (int k = 0; k < path.length; k++) {
                // Out of the one root, fanning out at once to its own lane.
                double fan = k == 0 ? 0.0 : k == 1 ? 0.5 : 1.0;
                path[k] = pursuitFrame(i, mine.path()[k], into * fan);
                if (k >= 2 && k < path.length - 1) {
                    path[k] = clear(path[k]); // its own frame kept it clear of the main worm's body, not his: kept clear here
                }
            }
            worms[i] = new Worm(new Shape(path, mine.dorsal(), mine.aperture()), i == 0 ? thin : BikakuRules.PURSUIT_GIRTH, i == 0 ? 1.0 : grow, own);
        }
        return worms;
    }

    /** Ravenous Pursuit's reach: as far as {@link BikakuRules#PURSUIT_REACH} from his eyes. */
    public static double[] pursuitReach(double[] target) {
        double[] eye = {0.0, 1.62, 0.0};
        double[] to = GraftMesh.sub(target, eye);
        double d = GraftMesh.length3(to);
        return d <= BikakuRules.PURSUIT_REACH || d < 1.0E-9 ? target : GraftMesh.add(eye, GraftMesh.mul(to, BikakuRules.PURSUIT_REACH / d));
    }

    // ---------------------------------------------------------------- 3.5.1: Carrion Eruption

    /** Over his right shoulder and down in front of him to the ground, where it goes in; and where it is under the ground in front of him. */
    static final double[] ERUPT_FRONT = {0.7, 1.25, 0.8};
    static final double[] ERUPT_ENTRY = {0.45, 0.05, 1.2};
    static final double[] ERUPT_DIVE = {0.45, -1.35, 2.0};
    /** How deep it tunnels under its mark, and how high over its mark it bursts up. */
    static final double ERUPT_DEPTH = 1.5;
    static final double ERUPT_HEIGHT = 2.6;

    /** Where Carrion Eruption bursts up (the body's frame, on the ground: {@code target} its mark's middle), held to its reach. */
    public static double[] eruptSpot(double[] target) {
        double ground = target[1] - 0.99;
        double flat = Math.hypot(target[0], target[2]);
        double k = flat > BikakuRules.ERUPTION_REACH && flat > 1.0E-9 ? BikakuRules.ERUPTION_REACH / flat : 1.0;
        return new double[] {target[0] * k, ground, target[2] * k};
    }

    /** Its way there and back: from its head at rest over the shoulder, down into the ground before him, under its mark, up over it. */
    static double[][] eruptTrack(double[] restHead, double[] spot) {
        double[] under = {spot[0], spot[1] - ERUPT_DEPTH, spot[2]};
        double[] above = {spot[0], spot[1] + ERUPT_HEIGHT, spot[2]};
        return new double[][] {restHead, SHOULDER, ERUPT_FRONT, ERUPT_ENTRY, ERUPT_DIVE, under, above};
    }

    /** How far along its track ({@link #eruptTrack}, by its knots: 0 at rest .. 6 burst up over its mark) its head is, {@code age} in. */
    public static double eruptKnot(double age) {
        if (age < BikakuMove.ERUPT_DIVE) {
            return 4.0 * GraftMesh.smooth(age / BikakuMove.ERUPT_DIVE);
        }
        if (age < BikakuMove.ERUPT_RISE) {
            return 4.0 + (age - BikakuMove.ERUPT_DIVE) / (double)(BikakuMove.ERUPT_RISE - BikakuMove.ERUPT_DIVE);
        }
        int land = BikakuMove.ERUPTION.land();
        if (age < land) {
            double w = (age - BikakuMove.ERUPT_RISE) / (double)(land - BikakuMove.ERUPT_RISE);
            return 5.0 + w * w * (3.0 - 2.0 * w);
        }
        if (age < BikakuMove.ERUPT_HOLD) {
            return 6.0;
        }
        if (age < BikakuMove.ERUPT_SUNK) {
            return 6.0 - GraftMesh.smooth((age - BikakuMove.ERUPT_HOLD) / (double)(BikakuMove.ERUPT_SUNK - BikakuMove.ERUPT_HOLD));
        }
        if (age < BikakuMove.ERUPT_BACK) {
            return 5.0 - GraftMesh.smooth((age - BikakuMove.ERUPT_SUNK) / (double)(BikakuMove.ERUPT_BACK - BikakuMove.ERUPT_SUNK));
        }
        return 4.0 * (1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - BikakuMove.ERUPT_BACK) / (double)(BikakuMove.ERUPTION.ticks() - BikakuMove.ERUPT_BACK))));
    }

    /** The point {@code knot} of the way along {@code track} (by its knots). */
    static double[] onTrack(double[][] track, double knot) {
        int k = (int)Math.max(0, Math.min(track.length - 2, Math.floor(knot)));
        return GraftMesh.lerp(track[k], track[k + 1], GraftMesh.clamp01(knot - k));
    }

    /** Where the ground heaves over its head as it tunnels (the body's frame), or null while its head is not under the ground. */
    public static double[] eruptBulge(double[] target, double age) {
        double knot = eruptKnot(age);
        if (knot < 4.0 || knot > 5.0) {
            return null;
        }
        double[] spot = eruptSpot(target);
        double[] p = onTrack(eruptTrack(new double[] {0.0, 0.0, 0.0}, spot), knot);
        double w = knot - 4.0;
        return new double[] {p[0], lerp(0.0, spot[1], w), p[2]};
    }

    /**
     * Carrion Eruption, {@code age} ticks in, at {@code target} (body frame, its mark's middle): its head goes down over his shoulder into
     * the ground before him and along under the surface (its body following the same way, as a worm burrows), bursts up under its mark with
     * its jaws thrown wide and shuts at {@link BikakuMove#land()}, held up there shaking, sinks back the way it came and comes back out of
     * the ground to rest. What is under the ground is hidden by the ground itself.
     */
    public static Shape eruption(double time, int seed, double[] target, double age) {
        double[] restHead = idleHead(time, seed);
        double[] restLook = idleLook(time, seed);
        double[] spot = eruptSpot(target);
        double[][] track = eruptTrack(restHead, spot);
        double knot = eruptKnot(age);
        double[] head = onTrack(track, knot);
        int land = BikakuMove.ERUPTION.land();
        if (age >= land && age < BikakuMove.ERUPT_HOLD) {
            double shake = Math.sin((age - land) * 2.4) * 0.22 * (1.0 - (age - land) / (double)(BikakuMove.ERUPT_HOLD - land));
            head = GraftMesh.add(head, new double[] {shake, 0.0, 0.5 * shake});
        }
        // It looks the way its track goes on (up out of the ground over its mark), turning back to its rest at the end.
        int k = (int)Math.max(0, Math.min(track.length - 2, Math.floor(Math.max(knot, 0.01))));
        double[] along = way(track[k], track[k + 1]);
        double home = GraftMesh.smooth(GraftMesh.clamp01(1.0 - knot / 1.5));
        double[] look = GraftMesh.normalize(GraftMesh.lerp(along, restLook, home));
        double mouth;
        if (age < BikakuMove.ERUPT_RISE) {
            mouth = lerp(idleMouth(time, seed), 0.15, GraftMesh.smooth(GraftMesh.clamp01(age / 4.0)));
        } else if (age < land) {
            mouth = age < land - 1.0 ? lerp(0.15, 1.0, GraftMesh.smooth(GraftMesh.clamp01((age - BikakuMove.ERUPT_RISE) / 1.5))) : lerp(1.0, 0.04, age - (land - 1.0));
        } else if (age < BikakuMove.ERUPT_HOLD) {
            mouth = 0.04;
        } else {
            mouth = lerp(0.3, idleMouth(time, seed), home);
        }
        // Its body: its coils behind him, then the track it has gone along, behind its head.
        Shape rest = idle(time, seed);
        int n = ZIGZAG.length;
        java.util.List<double[]> points = new java.util.ArrayList<>();
        for (int i = 0; i < n + 3; i++) {
            points.add(rest.path()[i]);
        }
        double pulled = GraftMesh.smooth(GraftMesh.clamp01(knot / 1.2));
        if (pulled > 0.0) {
            // As it goes, its curl straightens toward over the shoulder.
            points.set(n + 2, GraftMesh.lerp(rest.path()[n + 2], GraftMesh.lerp(ZIGZAG[n - 1], SHOULDER, 0.5), pulled));
        }
        for (int i = 1; i < track.length && i < knot - 0.05; i++) {
            points.add(track[i]);
        }
        double[] neck = GraftMesh.sub(head, GraftMesh.mul(look, 0.6));
        if (knot < 1.0) {
            neck = GraftMesh.lerp(rest.path()[n + 3], neck, GraftMesh.clamp01(knot));
        } else {
            neck = clear(neck);
        }
        points.add(neck);
        points.add(head);
        return new Shape(points.toArray(new double[0][]), dorsal(), GraftMesh.clamp01(mouth));
    }

    // ---------------------------------------------------------------- its body between root and head

    /** Its back faces up out of his back (its spurs up, its belly down). */
    static double[] dorsal() {
        return new double[] {0.0, 1.0, 0.0};
    }

    /**
     * 3.4.9: how far along the worm each coil is from its head at rest (blocks, along the line through its points): a bite uncoils the
     * worm from the head back, only as far as it needs to reach.
     */
    static final double[] ARC = arcs();

    private static double[] arcs() {
        double[] neck = {HEAD[0], HEAD[1], HEAD[2] - 0.6};
        double[] curl = GraftMesh.lerp(ZIGZAG[ZIGZAG.length - 1], neck, 0.5);
        double[] out = new double[ZIGZAG.length];
        double along = 0.6 + GraftMesh.length3(GraftMesh.sub(curl, neck));
        double[] before = curl;
        for (int k = ZIGZAG.length - 1; k >= 0; k--) {
            along += GraftMesh.length3(GraftMesh.sub(ZIGZAG[k], before));
            out[k] = along;
            before = ZIGZAG[k];
        }
        return out;
    }

    /**
     * Its control points from the root to {@code head}, the mouth coming at it along {@code look}: straight out of the back, up behind him
     * in a zigzag of coils (a wave running up them, so it slithers), round over the right shoulder to the head. Thrown out ({@code reach}
     * 1), 3.4.9 (the owner: "make sure it's long"): it uncoils from the head back only as far as the bite needs; the coils it needs go out
     * over his shoulder along the way to the head, spread out and still swinging from side to side, and the rest stay coiled up behind him,
     * drawn in a little (they gave the front its length). Never into his own head or body.
     */
    static double[][] path(double[] head, double[] look, double time, int seed, double reach) {
        return path(head, look, time, seed, reach, SHOULDER);
    }

    /**
     * 3.5.0: as above, the worm thrown out from {@code anchor} on its way to the head instead of over his shoulder (Predatory Drag: down
     * beside his right heel and along the ground).
     */
    static double[][] path(double[] head, double[] look, double time, int seed, double reach, double[] anchor) {
        double t = time + seed * 43.0;
        double wave = 1.0 - 0.6 * reach;
        double[] out = add(ROOT, new double[] {0.02, 0.05, -0.34});
        double neckLength = 0.6 + 0.3 * reach;
        double[] neck = clear(GraftMesh.sub(head, GraftMesh.mul(GraftMesh.normalize(look), neckLength)));
        int n = ZIGZAG.length;
        double straighten = GraftMesh.smooth(reach);
        double need = GraftMesh.length3(GraftMesh.sub(neck, anchor)) + 0.4;
        double[] way = GraftMesh.sub(neck, anchor);
        way = GraftMesh.dot(way, way) < 1.0E-9 ? new double[] {0.0, 0.0, 1.0} : GraftMesh.normalize(way);
        double[] sideways = GraftMesh.cross(way, new double[] {0.0, 1.0, 0.0});
        sideways = GraftMesh.dot(sideways, sideways) < 0.04 ? new double[] {1.0, 0.0, 0.0} : GraftMesh.normalize(sideways);
        double[][] coils = new double[n][];
        for (int k = 0; k < n; k++) {
            double phase = t * 0.07 - k * 1.1;
            double sign = k % 2 == 0 ? 1.0 : -1.0;
            double[] wobble = {0.22 * Math.sin(phase) * sign * wave, 0.12 * Math.sin(phase * 0.8 + 1.0) * wave, 0.16 * Math.sin(phase * 0.9 + 2.0) * wave};
            double[] rest = add(ZIGZAG[k], wobble);
            if (straighten <= 1.0E-6) {
                coils[k] = rest;
                continue;
            }
            // Uncoiled if the bite needs it (from the head back), placed along the way over the shoulder by how far it is from the head.
            double w = GraftMesh.smooth(GraftMesh.clamp01((need + 0.9 - ARC[k]) / 1.8)) * straighten;
            double f = Math.max(0.06, Math.min(0.94, 1.0 - ARC[k] / need));
            double taper = Math.sqrt(Math.sin(Math.PI * f));
            double[] on = GraftMesh.add(GraftMesh.lerp(anchor, neck, f), GraftMesh.add(GraftMesh.mul(sideways, 0.32 * taper * sign),
                new double[] {0.0, -0.1 * taper * sign, 0.0}));
            double[] stay = GraftMesh.lerp(rest, add(rest, new double[] {0.0, -0.3, 0.45}), 0.5 * straighten);
            coils[k] = clear(GraftMesh.lerp(stay, on, w));
        }
        double[] curl = GraftMesh.lerp(coils[n - 1], neck, 0.5);
        curl = clear(add(curl, new double[] {0.1 * Math.sin(t * 0.09 + 1.3) * wave, 0.08 * Math.sin(t * 0.08) * wave, 0.0}));
        double[][] path = new double[n + 5][];
        path[0] = ROOT;
        path[1] = out;
        System.arraycopy(coils, 0, path, 2, n);
        path[n + 2] = curl;
        path[n + 3] = neck;
        path[n + 4] = head;
        return path;
    }

    /** Where his head is (its middle) and how far the worm's middle keeps from it; his torso's half-width with the worm's thickness. */
    static final double[] HEAD_CENTRE = {0.0, 1.72, 0.0};
    static final double HEAD_CLEAR = 0.62;
    static final double TORSO_HALF = 0.72;
    static final double TORSO_BACK = -0.4;
    static final double TORSO_FRONT = 0.4;
    static final double LEGS_HALF = 0.5;
    static final double LEGS_DEPTH = 0.37;

    /** 3.4.9: {@code p} kept out of his head (pushed out from its middle) and his torso (pushed out behind him, or in front). */
    static double[] clear(double[] p) {
        double[] out = p;
        double[] from = GraftMesh.sub(out, HEAD_CENTRE);
        double d = GraftMesh.length3(from);
        if (d < HEAD_CLEAR) {
            double[] way = d < 1.0E-6 ? new double[] {0.0, 0.0, -1.0} : GraftMesh.mul(from, 1.0 / d);
            out = GraftMesh.add(HEAD_CENTRE, GraftMesh.mul(way, HEAD_CLEAR));
        }
        if (out[1] > 0.6 && out[1] < 1.55 && Math.abs(out[0]) < TORSO_HALF && out[2] > TORSO_BACK && out[2] < TORSO_FRONT) {
            out = new double[] {out[0], out[1], out[2] < 0.0 ? TORSO_BACK : TORSO_FRONT};
        }
        // 3.5.0: and his legs (the worm runs along the ground beside them): pushed out to the side.
        if (out[1] >= -0.5 && out[1] <= 0.6 && Math.abs(out[0]) < LEGS_HALF && Math.abs(out[2]) < LEGS_DEPTH) {
            out = new double[] {out[0] < 0.0 ? -LEGS_HALF : LEGS_HALF, out[1], out[2]};
        }
        return out;
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
