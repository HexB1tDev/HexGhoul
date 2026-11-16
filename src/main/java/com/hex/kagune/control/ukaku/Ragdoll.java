package com.hex.kagune.control.ukaku;

import java.util.ArrayList;
import java.util.List;

/**
 * 4.2.9 (the owner: "the entire thing is meant to be an adaptive ragdoll that adapts to any terrain, can roll, etc."): the pieces the Steel
 * Blade's finisher leaves (pure, tested; {@link Finisher}). Each piece is a rigid body made of boxes, falling under gravity, meeting whatever
 * it falls on (the blocks, through {@link Terrain}: any shape the game's collision gives, slabs, stairs, slopes of blocks, walls) and every
 * other piece, by its boxes' corners: pushed out, bounced a little, rubbed by friction, so it topples off an edge, rolls down a slope and comes
 * to rest lying on whatever is there, then sleeps (no more work for it). Blocks and ticks throughout; y is up.
 */
public final class Ragdoll {
    /** Gravity (blocks a tick, each tick), air drag on its speed and on its turning (kept each tick). */
    public static final double GRAVITY = 0.07;
    static final double DRAG = 0.985;
    static final double SPIN_DRAG = 0.97;
    /** How many steps a tick (contacts are found and answered in each). */
    public static final int STEPS = 3;
    /** How much of its speed into a surface it bounces back with, and how hard it rubs along one. */
    static final double BOUNCE = 0.12;
    static final double FRICTION = 0.7;
    /** How deep it may sink before it is pushed out (blocks), and how much of the rest each step pushes out. */
    static final double SLOP = 0.002;
    static final double PUSH = 0.8;
    /** Still for this many ticks at below these speeds (blocks a tick, radians a tick), it sleeps. */
    static final int SLEEP_TICKS = 15;
    static final double SLEEP_SPEED = 0.0015;
    static final double SLEEP_SPIN = 0.002;
    /** Fastest it ever goes (blocks a tick) and turns (radians a tick): nothing tunnels through a block. */
    static final double FASTEST = 1.2;
    static final double FASTEST_SPIN = 0.9;

    /** What the pieces fall on: where the segment from {@code a} to {@code b} first meets something solid, or null (the game: a block clip). */
    public interface Terrain {
        double[] hit(double[] a, double[] b);
    }

    /** One piece: its boxes about its middle (local blocks: {min x, y, z, max x, y, z}), where it is, how it is turned, and how it moves. */
    public static final class Body {
        public final double[][] boxes;
        final double[][] points;
        final double invMass;
        final double[] invInertia;
        /** Its middle (world blocks), its turn (a unit quaternion w, x, y, z), its speed (blocks a tick) and its spin (radians a tick, world). */
        public final double[] pos;
        public final double[] q;
        public final double[] v = new double[3];
        public final double[] w = new double[3];
        int still;
        public boolean asleep;
        boolean touched;

        /**
         * A piece of {@code boxes} (local blocks about any origin) whose origin is at {@code at} (world), turned {@code q}. Its middle is found
         * from its boxes (each as heavy as it is big) and the boxes are kept about it; {@link #offset} is where that middle was from the origin.
         */
        public Body(double[][] boxes, double[] at, double[] q) {
            double mass = 0.0;
            double[] centre = new double[3];
            for (double[] b : boxes) {
                double m = volume(b);
                mass += m;
                for (int k = 0; k < 3; k++) {
                    centre[k] += m * (b[k] + b[k + 3]) * 0.5;
                }
            }
            mass = Math.max(1.0E-6, mass);
            for (int k = 0; k < 3; k++) {
                centre[k] /= mass;
            }
            this.offset = centre;
            this.boxes = new double[boxes.length][];
            double[] inertia = new double[3];
            List<double[]> corners = new ArrayList<>();
            for (int i = 0; i < boxes.length; i++) {
                double[] b = boxes[i];
                double[] c = new double[6];
                for (int k = 0; k < 3; k++) {
                    c[k] = b[k] - centre[k];
                    c[k + 3] = b[k + 3] - centre[k];
                }
                this.boxes[i] = c;
                double m = volume(c);
                double sx = c[3] - c[0];
                double sy = c[4] - c[1];
                double sz = c[5] - c[2];
                double mx = (c[0] + c[3]) * 0.5;
                double my = (c[1] + c[4]) * 0.5;
                double mz = (c[2] + c[5]) * 0.5;
                inertia[0] += m / 12.0 * (sy * sy + sz * sz) + m * (my * my + mz * mz);
                inertia[1] += m / 12.0 * (sx * sx + sz * sz) + m * (mx * mx + mz * mz);
                inertia[2] += m / 12.0 * (sx * sx + sy * sy) + m * (mx * mx + my * my);
                for (int k = 0; k < 8; k++) {
                    corners.add(new double[] {(k & 1) == 0 ? c[0] : c[3], (k & 2) == 0 ? c[1] : c[4], (k & 4) == 0 ? c[2] : c[5]});
                }
            }
            this.points = corners.toArray(new double[0][]);
            this.invMass = 1.0 / mass;
            this.invInertia = new double[] {1.0 / Math.max(1.0E-9, inertia[0]), 1.0 / Math.max(1.0E-9, inertia[1]), 1.0 / Math.max(1.0E-9, inertia[2])};
            this.q = normalised(q.clone());
            double[] turned = rotate(this.q, centre);
            this.pos = new double[] {at[0] + turned[0], at[1] + turned[1], at[2] + turned[2]};
        }

        /** Where its middle lies from the origin its boxes were given about (local blocks). */
        public final double[] offset;

        /** Corner {@code i} where it is now (world). */
        double[] world(int i) {
            return toWorld(this.points[i]);
        }

        /** A point of the piece (local blocks about its middle) in the world. */
        public double[] toWorld(double[] local) {
            double[] r = rotate(this.q, local);
            return new double[] {this.pos[0] + r[0], this.pos[1] + r[1], this.pos[2] + r[2]};
        }

        /** A world point in the piece's own frame (local blocks about its middle). */
        double[] toLocal(double[] world) {
            return rotate(conjugate(this.q), new double[] {world[0] - this.pos[0], world[1] - this.pos[1], world[2] - this.pos[2]});
        }

        /** Its inverse inertia applied to a world vector. */
        double[] invInertia(double[] world) {
            double[] l = rotate(conjugate(this.q), world);
            return rotate(this.q, new double[] {l[0] * this.invInertia[0], l[1] * this.invInertia[1], l[2] * this.invInertia[2]});
        }

        void push(double[] impulse, double[] r) {
            for (int k = 0; k < 3; k++) {
                this.v[k] += impulse[k] * this.invMass;
            }
            double[] dw = invInertia(cross(r, impulse));
            for (int k = 0; k < 3; k++) {
                this.w[k] += dw[k];
            }
        }

        /** Its speed at the world point {@code r} from its middle. */
        double[] velocityAt(double[] r) {
            double[] s = cross(this.w, r);
            return new double[] {this.v[0] + s[0], this.v[1] + s[1], this.v[2] + s[2]};
        }

        void wake() {
            this.asleep = false;
            this.still = 0;
        }
    }

    private Ragdoll() {
    }

    /** One tick for every piece: gravity, motion, then the ground and each other answered {@link #STEPS} times. */
    public static void tick(List<Body> bodies, Terrain terrain) {
        double h = 1.0 / STEPS;
        for (int s = 0; s < STEPS; s++) {
            for (Body b : bodies) {
                if (b.asleep) {
                    continue;
                }
                b.v[1] -= GRAVITY * h;
                clampSpeed(b);
                for (int k = 0; k < 3; k++) {
                    b.pos[k] += b.v[k] * h;
                }
                double[] dq = {0.0, b.w[0] * h * 0.5, b.w[1] * h * 0.5, b.w[2] * h * 0.5};
                double[] next = multiply(dq, b.q);
                for (int k = 0; k < 4; k++) {
                    b.q[k] += next[k];
                }
                normalised(b.q);
                b.touched = false;
            }
            for (Body b : bodies) {
                if (!b.asleep) {
                    ground(b, terrain);
                }
            }
            for (int i = 0; i < bodies.size(); i++) {
                for (int j = 0; j < bodies.size(); j++) {
                    if (i != j && !(bodies.get(i).asleep && bodies.get(j).asleep)) {
                        between(bodies.get(i), bodies.get(j));
                    }
                }
            }
        }
        for (Body b : bodies) {
            if (b.asleep) {
                continue;
            }
            for (int k = 0; k < 3; k++) {
                b.v[k] *= DRAG;
                b.w[k] *= SPIN_DRAG;
            }
            if (b.touched && length(b.v) < SLEEP_SPEED && length(b.w) < SLEEP_SPIN) {
                if (++b.still >= SLEEP_TICKS) {
                    b.asleep = true;
                    java.util.Arrays.fill(b.v, 0.0);
                    java.util.Arrays.fill(b.w, 0.0);
                }
            } else {
                b.still = 0;
            }
        }
    }

    /**
     * Every corner swept out from the piece's own middle (never inside the ground, so the surface met is the true one between them, at any
     * depth): what it meets pushes it out and answers its motion.
     */
    static void ground(Body b, Terrain terrain) {
        double[] correction = new double[3];
        double[] from = b.pos.clone();
        List<double[][]> contacts = new ArrayList<>();
        for (int i = 0; i < b.points.length; i++) {
            double[] now = b.world(i);
            double[] at = terrain.hit(from, now);
            if (at == null) {
                continue;
            }
            double[] n = face(from, now, at);
            double depth = (at[0] - now[0]) * n[0] + (at[1] - now[1]) * n[1] + (at[2] - now[2]) * n[2];
            if (depth < -1.0E-6) {
                continue;
            }
            b.touched = true;
            contacts.add(new double[][] {{now[0] - b.pos[0], now[1] - b.pos[1], now[2] - b.pos[2]}, n});
            double d = Math.max(0.0, depth - SLOP) * PUSH;
            for (int k = 0; k < 3; k++) {
                double want = n[k] * d;
                if (Math.abs(want) > Math.abs(correction[k])) {
                    correction[k] = want;
                }
            }
        }
        if (contacts.isEmpty()) {
            return;
        }
        solve(b, contacts);
        for (int k = 0; k < 3; k++) {
            b.pos[k] += correction[k];
        }
    }

    /** How many passes the contacts of one step are answered in (each from the motion the last left). */
    static final int PASSES = 8;

    /**
     * The ground contacts of one step answered together, in passes (a running total each, never pulling, its friction capped by its push), the
     * order turned each pass: a piece lying flat is held evenly by its corners, with no twist and no creep, and stops.
     */
    static void solve(Body b, List<double[][]> contacts) {
        int count = contacts.size();
        double[] target = new double[count];
        double[] normal = new double[count];
        double[][] rub = new double[count][3];
        for (int c = 0; c < count; c++) {
            double vn = dot(b.velocityAt(contacts.get(c)[0]), contacts.get(c)[1]);
            target[c] = vn < -0.08 ? -BOUNCE * vn : 0.0;
        }
        for (int pass = 0; pass < PASSES; pass++) {
            for (int step = 0; step < count; step++) {
                int c = pass % 2 == 0 ? step : count - 1 - step;
                double[] r = contacts.get(c)[0];
                double[] n = contacts.get(c)[1];
                double vn = dot(b.velocityAt(r), n);
                double k = b.invMass + dot(n, cross(b.invInertia(cross(r, n)), r));
                double total = Math.max(0.0, normal[c] + (target[c] - vn) / k);
                double dj = total - normal[c];
                normal[c] = total;
                b.push(new double[] {n[0] * dj, n[1] * dj, n[2] * dj}, r);
                // Friction, along the way it slides now.
                double[] vp = b.velocityAt(r);
                double along = dot(vp, n);
                double[] slide = {vp[0] - n[0] * along, vp[1] - n[1] * along, vp[2] - n[2] * along};
                double speed = length(slide);
                if (speed < 1.0E-9) {
                    continue;
                }
                double[] t = {slide[0] / speed, slide[1] / speed, slide[2] / speed};
                double kt = b.invMass + dot(t, cross(b.invInertia(cross(r, t)), r));
                double[] want = {rub[c][0] - t[0] * speed / kt, rub[c][1] - t[1] * speed / kt, rub[c][2] - t[2] * speed / kt};
                double most = FRICTION * normal[c];
                double size = length(want);
                if (size > most) {
                    for (int q = 0; q < 3; q++) {
                        want[q] *= most / size;
                    }
                }
                b.push(new double[] {want[0] - rub[c][0], want[1] - rub[c][1], want[2] - rub[c][2]}, r);
                rub[c] = want;
            }
        }
    }

    /** Body {@code a}'s corners inside any of {@code b}'s boxes: pushed apart the shortest way out, both answered. */
    static void between(Body a, Body b) {
        for (int i = 0; i < a.points.length; i++) {
            double[] p = a.world(i);
            double[] local = b.toLocal(p);
            for (double[] box : b.boxes) {
                if (local[0] <= box[0] || local[0] >= box[3] || local[1] <= box[1] || local[1] >= box[4] || local[2] <= box[2] || local[2] >= box[5]) {
                    continue;
                }
                double[] out = {local[0] - box[0], box[3] - local[0], local[1] - box[1], box[4] - local[1], local[2] - box[2], box[5] - local[2]};
                int best = 0;
                for (int k = 1; k < 6; k++) {
                    if (out[k] < out[best]) {
                        best = k;
                    }
                }
                double[] ln = new double[3];
                ln[best / 2] = best % 2 == 0 ? -1.0 : 1.0;
                double[] n = rotate(b.q, ln);
                double depth = out[best];
                double[] ra = {p[0] - a.pos[0], p[1] - a.pos[1], p[2] - a.pos[2]};
                double[] rb = {p[0] - b.pos[0], p[1] - b.pos[1], p[2] - b.pos[2]};
                if (a.asleep || b.asleep) {
                    a.wake();
                    b.wake();
                }
                answer(a, b, ra, rb, n);
                double d = Math.max(0.0, depth - SLOP) * PUSH * 0.5;
                double share = a.invMass / (a.invMass + b.invMass);
                for (int k = 0; k < 3; k++) {
                    a.pos[k] += n[k] * d * 2.0 * share;
                    b.pos[k] -= n[k] * d * 2.0 * (1.0 - share);
                }
                a.touched = true;
                b.touched = true;
                break;
            }
        }
    }

    /**
     * A contact at {@code ra} from {@code a}'s middle ({@code rb} from {@code b}'s, or none: the ground) whose surface faces {@code n} (out of
     * {@code b}, toward {@code a}): its speed into it taken away, a little given back, its sliding rubbed by friction.
     */
    static void answer(Body a, Body b, double[] ra, double[] rb, double[] n) {
        double[] va = a.velocityAt(ra);
        double[] vb = b == null ? new double[3] : b.velocityAt(rb);
        double[] rel = {va[0] - vb[0], va[1] - vb[1], va[2] - vb[2]};
        double vn = dot(rel, n);
        if (vn >= 0.0) {
            return;
        }
        double k = a.invMass + dot(n, cross(a.invInertia(cross(ra, n)), ra));
        if (b != null) {
            k += b.invMass + dot(n, cross(b.invInertia(cross(rb, n)), rb));
        }
        double j = -(1.0 + (vn < -0.08 ? BOUNCE : 0.0)) * vn / k;
        double[] impulse = {n[0] * j, n[1] * j, n[2] * j};
        a.push(impulse, ra);
        if (b != null) {
            b.push(new double[] {-impulse[0], -impulse[1], -impulse[2]}, rb);
        }
        // Friction, along the way it slides.
        va = a.velocityAt(ra);
        vb = b == null ? new double[3] : b.velocityAt(rb);
        rel = new double[] {va[0] - vb[0], va[1] - vb[1], va[2] - vb[2]};
        double along = dot(rel, n);
        double[] slide = {rel[0] - n[0] * along, rel[1] - n[1] * along, rel[2] - n[2] * along};
        double speed = length(slide);
        if (speed < 1.0E-9) {
            return;
        }
        double[] t = {slide[0] / speed, slide[1] / speed, slide[2] / speed};
        double kt = a.invMass + dot(t, cross(a.invInertia(cross(ra, t)), ra));
        if (b != null) {
            kt += b.invMass + dot(t, cross(b.invInertia(cross(rb, t)), rb));
        }
        double jt = Math.min(speed / kt, FRICTION * j);
        double[] rub = {-t[0] * jt, -t[1] * jt, -t[2] * jt};
        a.push(rub, ra);
        if (b != null) {
            b.push(new double[] {-rub[0], -rub[1], -rub[2]}, rb);
        }
    }

    /**
     * Which way the surface a corner met faces: the game's collision is boxes on a sixteenth of a block, so the face it crossed lies on that grid
     * across the way it went; the way back up if it started inside (or nothing fits).
     */
    public static double[] face(double[] from, double[] to, double[] at) {
        double[] d = {to[0] - from[0], to[1] - from[1], to[2] - from[2]};
        double span = length(d);
        double[] n = new double[3];
        if (span < 1.0E-9) {
            n[1] = 1.0;
            return n;
        }
        // How far along the sweep it met the surface; the face is the grid plane the sweep crosses right there.
        double hit = dot(new double[] {at[0] - from[0], at[1] - from[1], at[2] - from[2]}, d) / (span * span);
        int best = -1;
        double bestOff = 0.02;
        for (int k = 0; k < 3; k++) {
            if (Math.abs(d[k]) < 1.0E-9) {
                continue;
            }
            double grid = at[k] * 16.0;
            if (Math.abs(grid - Math.rint(grid)) > 0.02) {
                continue;
            }
            double t = (Math.rint(grid) / 16.0 - from[k]) / d[k];
            double off = Math.abs(t - hit);
            if (t > 1.0E-6 && t <= 1.0 + 1.0E-6 && off < bestOff) {
                best = k;
                bestOff = off;
            }
        }
        if (best < 0) {
            // It started inside, or the shape is not on the grid: back the way it came, or up when it came mostly sideways or up.
            double[] back = {-d[0] / span, -d[1] / span, -d[2] / span};
            if (back[1] > -0.3) {
                n[1] = 1.0;
                return n;
            }
            return back;
        }
        n[best] = d[best] < 0.0 ? 1.0 : -1.0;
        return n;
    }

    static void clampSpeed(Body b) {
        double s = length(b.v);
        if (s > FASTEST) {
            for (int k = 0; k < 3; k++) {
                b.v[k] *= FASTEST / s;
            }
        }
        double w = length(b.w);
        if (w > FASTEST_SPIN) {
            for (int k = 0; k < 3; k++) {
                b.w[k] *= FASTEST_SPIN / w;
            }
        }
    }

    // ------------------------------------------------------------------ small maths

    static double volume(double[] b) {
        return Math.max(1.0E-9, (b[3] - b[0]) * (b[4] - b[1]) * (b[5] - b[2]));
    }

    public static double[] rotate(double[] q, double[] v) {
        double[] p = {0.0, v[0], v[1], v[2]};
        double[] r = multiply(multiply(q, p), conjugate(q));
        return new double[] {r[1], r[2], r[3]};
    }

    static double[] multiply(double[] a, double[] b) {
        return new double[] {a[0] * b[0] - a[1] * b[1] - a[2] * b[2] - a[3] * b[3], a[0] * b[1] + a[1] * b[0] + a[2] * b[3] - a[3] * b[2],
            a[0] * b[2] - a[1] * b[3] + a[2] * b[0] + a[3] * b[1], a[0] * b[3] + a[1] * b[2] - a[2] * b[1] + a[3] * b[0]};
    }

    static double[] conjugate(double[] q) {
        return new double[] {q[0], -q[1], -q[2], -q[3]};
    }

    static double[] normalised(double[] q) {
        double l = Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        if (l < 1.0E-12) {
            q[0] = 1.0;
            q[1] = q[2] = q[3] = 0.0;
            return q;
        }
        for (int k = 0; k < 4; k++) {
            q[k] /= l;
        }
        return q;
    }

    /** A turn of {@code angle} radians about the unit {@code axis}, as a quaternion. */
    public static double[] about(double[] axis, double angle) {
        double s = Math.sin(angle * 0.5);
        return new double[] {Math.cos(angle * 0.5), axis[0] * s, axis[1] * s, axis[2] * s};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double length(double[] a) {
        return Math.sqrt(dot(a, a));
    }
}
