package com.hex.kagune.control.ayato;

import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free tests for the Steel Blade (5.0.19): a cone with an edge after the manga (it swallows the arm, tapers to a
 * needle, its back a jagged row of barbs), its forming in order, and how smoothly it forms and lives.
 */
public final class SteelBladeTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) {
        shape();
        model();
        forming();
        smooth();
        carry();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Steel blade tests: " + passed + " checks passed");
    }

    private static void shape() {
        check(SteelBlade.LENGTH < 2.9 && SteelBlade.LENGTH >= 2.0, "a little shorter than 5.0.18's (2.9 blocks past the hand)");
        check(Math.abs(SteelBlade.REACH - (SteelBlade.ARM + SteelBlade.LENGTH + 0.4)) < 1.0E-9, "its reach is the arm and all of its length");
        check(Math.abs(SteelBlade.TIP_Y - (SteelBlade.HAND_Y + SteelBlade.LENGTH * SteelBlade.PIXELS)) < 1.0E-9, "the point is its length past the hand");
        // A cone: round and massive at the base, tapering all the way, never flat.
        check(SteelBlade.radius(0.0) >= 4.0, "massive round the arm (" + SteelBlade.radius(0.0) + " px)");
        for (double s = 0.0; s < 1.0; s += 0.01) {
            double r = SteelBlade.radius(s);
            double e = SteelBlade.reach(s);
            check(SteelBlade.radius(s + 0.01) <= r + 1.0E-9, "it tapers all the way (at " + s + ")");
            check(e >= r - 1.0E-9, "its edge stands out of the round (at " + s + ")");
            check(Math.abs(SteelBlade.reach(s + 0.005) - e) < 0.16, "its edge is one smooth curve (at " + s + ")");
            if (s <= 0.6) {
                check(2.0 * r >= 0.35 * (e + r), "a cone, not a flat blade: as deep as a third of its width (at " + s + ")");
            }
        }
        check(SteelBlade.reach(0.95) < 0.7 && SteelBlade.radius(0.95) < 0.25 && SteelBlade.reach(1.0) == 0.0 && SteelBlade.radius(1.0) == 0.0,
            "a needle point (" + SteelBlade.reach(0.95) + " px from the axis at 0.95)");
        check(SteelBlade.reach(SteelBlade.EDGE_PEAK) > 5.0 && SteelBlade.reach(SteelBlade.EDGE_PEAK) < 6.0, "its edge stands out past the hand, slim (6.0.0)");
        // The section: the edge out front, the round behind, the flanks between, sharp at the edge.
        double r = 2.5;
        double e = 6.0;
        check(Math.abs(SteelBlade.section(r, e, 0.0) - e) < 1.0E-9 && Math.abs(SteelBlade.section(r, e, Math.PI) - r) < 1.0E-9, "edge out front, round behind");
        double alpha = Math.acos(r / e);
        check(Math.abs(SteelBlade.section(r, e, alpha - 1.0E-6) - r) < 1.0E-3, "the flanks meet the round smoothly");
        double last = e;
        for (double t = 0.0; t <= Math.PI; t += 0.02) {
            double now = SteelBlade.section(r, e, t);
            check(now <= last + 1.0E-9 && now >= r - 1.0E-9 && Math.abs(SteelBlade.section(r, e, -t) - now) < 1.0E-9,
                "the section falls away from the edge evenly, never inside the round (" + t + ")");
            last = now;
        }
        check(SteelBlade.section(r, e, alpha * 0.5) < r + (r / Math.cos(alpha * 0.5) - r) - 0.1, "its flanks are hollow-ground");
        // It swallows the arm: every corner of the arm, shoulder to hand, inside the cone.
        for (boolean slim : new boolean[] {false, true}) {
            double cx = slim ? -0.5 : -1.0;
            double half = slim ? 1.5 : 2.0;
            double ax = SteelBlade.axis(slim);
            boolean inside = true;
            for (double y = SteelBlade.BASE_Y; y <= SteelBlade.HAND_Y; y += 0.5) {
                double s = (y - SteelBlade.BASE_Y) / SteelBlade.fullLength();
                for (double x : new double[] {cx - half, cx + half}) {
                    for (double z : new double[] {-2.0, 2.0}) {
                        double theta = Math.atan2(z, x - ax);
                        inside &= SteelBlade.section(SteelBlade.radius(s), SteelBlade.reach(s), theta) > Math.hypot(x - ax, z) + 0.1;
                    }
                }
            }
            check(inside, "the arm is swallowed whole" + (slim ? " (slim)" : ""));
        }
    }

    private static void model() {
        List<SteelBlade.Quad> quads = SteelBlade.build(1.0, 3.7, false);
        check(quads.size() > 2000, "detailed (" + quads.size() + " quads)");
        boolean sane = true;
        boolean unit = true;
        double far = 0.0;
        for (SteelBlade.Quad q : quads) {
            for (int k = 0; k < 4; k++) {
                double[] c = q.corners()[k];
                double[] n = q.normals()[k];
                sane &= Double.isFinite(c[0]) && Double.isFinite(c[1]) && Double.isFinite(c[2]) && Math.abs(c[0]) < 16.0 && Math.abs(c[2]) < 14.0
                    && c[1] > -12.0 && c[1] <= SteelBlade.TIP_Y + 0.5;
                unit &= Math.abs(Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]) - 1.0) < 1.0E-6;
                far = Math.max(far, c[1]);
            }
        }
        check(sane, "every corner is finite and on the arm, the shoulder or the blade");
        check(unit, "every normal is a unit");
        check(Math.abs(far - SteelBlade.TIP_Y) < 0.6, "the point is where its length puts it (" + far + " vs " + SteelBlade.TIP_Y + ")");
        check(SteelBlade.build(1.0, 0.0, true).size() == quads.size(), "on slim arms too");
        // The normals face out: the cone's away from its axis (the edge's flanks too), so it is lit from the right side.
        double ax = SteelBlade.axis(false);
        int[] core = {0, 0};
        SteelBlade.build(1.0, 3.7, false, SteelBlade.CORE, (x, y, z, argb, nx, ny, nz) -> {
            if (y > -1.0 && y < SteelBlade.TIP_Y - 4.0 && Math.hypot(x - ax, z) > 0.3) {
                core[0]++;
                core[1] += (x - ax) * nx + z * nz > 0.0 ? 1 : 0;
            }
        });
        check(core[0] > 1000 && core[1] == core[0], "the cone's faces all look outward (" + core[1] + " of " + core[0] + ")");
        // The jagged row: right past each plate's barb its back drops sharply to the plate beneath.
        for (int k = 0; k < SteelBlade.PLATES.length; k++) {
            double end = SteelBlade.station(SteelBlade.PLATES[k][0]);
            double at = Math.max(back(quads, ax, end - 0.6), back(quads, ax, end - 1.2));
            double after = back(quads, ax, end + 1.7);
            check(at - after > 1.2, "plate " + k + "'s barb stands " + (at - after) + " px over what follows it");
        }
        check(SteelBlade.build(0.004, 0.0, false).isEmpty(), "open, nothing");
        // In first person it leaves the pauldron off.
        int[] firstPerson = {0};
        SteelBlade.build(1.0, 0.0, false, false, (x, y, z, argb, nx, ny, nz) -> firstPerson[0]++);
        check(firstPerson[0] / 4 < quads.size() && firstPerson[0] % 4 == 0, "first person leaves the pauldron off");
    }

    /** How far the blade's back stands out (toward -x, past the axis) about {@code y}. */
    private static double back(List<SteelBlade.Quad> quads, double ax, double y) {
        double most = 0.0;
        for (SteelBlade.Quad q : quads) {
            for (double[] c : q.corners()) {
                if (Math.abs(c[1] - y) < 0.9) {
                    most = Math.max(most, ax - c[0]);
                }
            }
        }
        return most;
    }

    private static void forming() {
        check(SteelBlade.formed(true, 0.0) == 0.0 && SteelBlade.formed(true, SteelBlade.FORM_TICKS) == 1.0
            && SteelBlade.formed(false, SteelBlade.FORM_TICKS) == 0.0 && SteelBlade.formed(false, 0.0) == 1.0, "it closes and opens");
        check(SteelBlade.fold(0.0) == 0.0 && SteelBlade.fold(0.42) == 1.0, "the steel tentacles go in as the plates come");
        // In order: the plates sweep round the arm and the pauldron fans in; the blade drives out; the edge unfurls; the barbs last.
        check(SteelBlade.wrap(0.28, 0) > 0.9 && SteelBlade.pauldron(0.3) > 0.9 && SteelBlade.extend(0.28) == 0.0,
            "the plates close on the arm before the blade drives out");
        check(SteelBlade.wrap(0.15, 0) > SteelBlade.wrap(0.15, SteelBlade.PLATES.length - 1), "the innermost plates close first");
        check(SteelBlade.unfurl(0.42) < 0.25 && SteelBlade.extend(0.42) > 0.2 && SteelBlade.unfurl(0.8) == 1.0, "the edge unfurls once the blade is out");
        boolean last = true;
        for (int k = 0; k < SteelBlade.PLATES.length; k++) {
            last &= SteelBlade.barb(0.575, k) == 0.0 && SteelBlade.barb(1.0, k) == 1.0;
        }
        check(last, "the barbs spring last, and are all out when it is formed");
        check(SteelBlade.barb(0.72, SteelBlade.PLATES.length - 1) > SteelBlade.barb(0.72, 0), "the barbs spring from the shoulder's toward the point's");
        // The forming shows from the start: something of it is there a tenth of the way in.
        check(!SteelBlade.build(0.1, 0.0, false).isEmpty(), "the plates are on their way from the first ticks");
        double peak = 0.0;
        for (double t = 0.0; t <= 1.0; t += 0.001) {
            peak = Math.max(peak, SteelBlade.backOut(t));
        }
        check(peak > 1.02 && peak < 1.1 && SteelBlade.backOut(1.0) == 1.0 && SteelBlade.backOut(0.0) == 0.0, "driven home a touch past, then settled");
    }

    /**
     * Smooth: at 60 frames a second (3 a tick) no phase moves more than a sixth of its way in a frame, no point of it jumps more
     * than a few pixels in a frame as it forms, and its ambient life barely moves it frame to frame.
     */
    private static void smooth() {
        double most = 0.0;
        int frames = SteelBlade.FORM_TICKS * 3;
        for (int frame = 0; frame < frames; frame++) {
            double a = SteelBlade.formed(true, frame / 3.0);
            double b = SteelBlade.formed(true, (frame + 1) / 3.0);
            most = Math.max(most, Math.abs(SteelBlade.extend(b) - SteelBlade.extend(a)));
            most = Math.max(most, Math.abs(SteelBlade.unfurl(b) - SteelBlade.unfurl(a)));
            most = Math.max(most, Math.abs(SteelBlade.fold(b) - SteelBlade.fold(a)));
            most = Math.max(most, Math.abs(SteelBlade.swell(b) - SteelBlade.swell(a)));
            most = Math.max(most, Math.abs(SteelBlade.pauldron(b) - SteelBlade.pauldron(a)));
            for (int k = 0; k < SteelBlade.PLATES.length; k++) {
                most = Math.max(most, Math.abs(SteelBlade.wrap(b, k) - SteelBlade.wrap(a, k)));
                most = Math.max(most, Math.abs(SteelBlade.slide(b, k) - SteelBlade.slide(a, k)));
                most = Math.max(most, Math.abs(SteelBlade.barb(b, k) - SteelBlade.barb(a, k)));
            }
        }
        check(most < 0.17, "no phase moves more than a sixth of its way in one frame (" + most + ")");
        double jump = 0.0;
        int compared = 0;
        for (int frame = 0; frame < frames; frame++) {
            List<SteelBlade.Quad> a = SteelBlade.build(SteelBlade.formed(true, frame / 3.0), 0.0, false);
            List<SteelBlade.Quad> b = SteelBlade.build(SteelBlade.formed(true, (frame + 1) / 3.0), 0.0, false);
            if (a.size() != b.size()) {
                continue; // a plate or the pauldron came in: it starts small, tucked in at the arm
            }
            compared++;
            jump = Math.max(jump, moved(a, b));
        }
        check(compared > frames * 0.7, "most frames compare (" + compared + " of " + frames + ")");
        check(jump < 4.5, "no point of it jumps more than a few pixels in a frame as it forms (" + jump + ")");
        double drift = 0.0;
        for (double t = 0.0; t < 6.0; t += 0.5) {
            drift = Math.max(drift, moved(SteelBlade.build(1.0, t, false), SteelBlade.build(1.0, t + 1.0 / 60.0, false)));
        }
        check(drift < 0.2, "its ambient life moves it gently (" + drift + " px a frame at most)");
    }

    /** The carry keeps the blade off the ground by itself; the guard keeps it off in any pose, just enough, and smoothly. */
    private static void carry() {
        float[] standing = {-BladeCarry.SHOULDER * 0.0F - 5.0F, BladeCarry.SHOULDER, 0.0F};
        float[] crouching = {-5.0F, BladeCarry.CROUCH_SHOULDER, 0.0F};
        double limit = BladeCarry.GROUND - BladeCarry.CLEARANCE;
        double crouchLimit = BladeCarry.CROUCH_GROUND - BladeCarry.CLEARANCE;
        // 6.0.4: the hold hangs steeply at his side, so resting it draws in (BladeCarry.fit) to clear the ground, standing and
        // crouched, with its breathing, weight shifts, stride sway, running and turning the blade over: never under the shortest,
        // and well short of its full length at rest.
        double worst = -1.0E9;
        double highest = 1.0E9;
        double shortest = 1.0;
        double restReach = 0.0;
        for (double t = 0.0; t < 20.0; t += 0.05) {
            for (double walk : new double[] {0.0, 1.0}) {
                for (double run : new double[] {0.0, 1.0}) {
                    for (boolean crouch : new boolean[] {false, true}) {
                        float[] e = SteelSwing.euler(SteelSwing.hold(t, walk, t * 3.1, run, crouch ? 1.0 : 0.0));
                        double[][] m = BladeCarry.zyx(e[0], e[1], e[2]);
                        float[] pivot = crouch ? crouching : standing;
                        double ground = crouch ? BladeCarry.CROUCH_GROUND : BladeCarry.GROUND;
                        double reach = BladeCarry.fit(m, pivot, ground, 1.0, false);
                        double low = BladeCarry.lowest(m, pivot, reach, false);
                        worst = Math.max(worst, low - (ground - BladeCarry.CLEARANCE));
                        shortest = Math.min(shortest, reach);
                        if (walk == 0.0 && run == 0.0 && !crouch) {
                            highest = Math.min(highest, low);
                            restReach = Math.max(restReach, reach);
                        }
                    }
                }
            }
        }
        check(worst <= 0.0, "drawn in, the hold keeps the blade off the ground by itself (" + worst + " px past the clearance at worst)");
        check(shortest > BladeCarry.MIN_REACH, "it never has to draw in to the shortest (" + shortest + ")");
        check(restReach < 0.75 && restReach > 0.35, "at rest it is drawn in to about 1.6 blocks out of the shoulder (" + restReach + " of its length)");
        check(highest > limit - 1.0, "standing, its point rests just off the ground (" + highest + ")");
        float[] hold = SteelSwing.euler(SteelSwing.hold(0.0, 0.0, 0.0, 0.0));
        double rest = BladeCarry.fit(BladeCarry.zyx(hold[0], hold[1], hold[2]), standing, BladeCarry.GROUND, 1.0, false);
        float[] same = BladeCarry.guard(hold[0], hold[1], hold[2], standing, BladeCarry.GROUND, rest, false);
        check(same[0] == hold[0] && same[1] == hold[1] && same[2] == hold[2], "drawn in, the resting hold is left exactly as it is");
        // The fit: as far out as clears the ground, all of it where it is clear anyway, and smoothly with the arm.
        float[] level = {-0.2F, 0.0F, 1.4F};
        double[][] up = BladeCarry.zyx(level[0], level[1], level[2]);
        check(BladeCarry.fit(up, standing, BladeCarry.GROUND, 1.0, false) == 1.0, "where it is clear anyway, all of it is out");
        double[][] down = BladeCarry.zyx(hold[0], hold[1], hold[2]);
        check(BladeCarry.lowest(down, standing, rest + 0.01, false) > limit - 0.02, "as far out as clears the ground, no shorter");
        double fitJump = 0.0;
        double previousFit = Double.NaN;
        for (double a = 0.0; a <= 1.6; a += 0.002) {
            double f = BladeCarry.fit(BladeCarry.zyx(hold[0], hold[1], hold[2] - a), standing, BladeCarry.GROUND, 1.0, false);
            if (!Double.isNaN(previousFit)) {
                fitJump = Math.max(fitJump, Math.abs(f - previousFit));
            }
            previousFit = f;
        }
        check(fitJump < 0.01, "it draws in and out smoothly as the arm rises and falls (" + fitJump + ")");
        // Drawn in, the model is the same blade, shorter: its point comes in as far as the fit says, nothing reaching past it.
        double[] tips = new double[2];
        double[] reaches = {1.0, 0.5};
        for (int k = 0; k < 2; k++) {
            int index = k;
            tips[k] = -1.0E9;
            SteelBlade.build(1.0, 0.0, false, true, 0.0, 0.0, reaches[k], (x, y, z, argb, nx, ny, nz) -> tips[index] = Math.max(tips[index], y));
        }
        double expected = SteelBlade.GRIP_Y + (SteelBlade.TIP_Y - SteelBlade.GRIP_Y) * 0.5;
        check(tips[0] > SteelBlade.TIP_Y - 1.5 && Math.abs(tips[1] - expected) < 1.5, "drawn in to half, its point comes in to half (" + tips[1] + " for "
            + expected + ")");
        // Crouched, the guard lifts it clear.
        float[] crouched = BladeCarry.guard(hold[0], hold[1], hold[2], crouching, BladeCarry.CROUCH_GROUND, 1.0, true);
        check(BladeCarry.lowest(BladeCarry.zyx(crouched[0], crouched[1], crouched[2]), crouching, 1.0, true) <= crouchLimit + 1.0E-3,
            "crouched, the hold is lifted clear");
        // Any pose at all, standing, crouched or dropped into a landing: guarded, no part of it is below the ground.
        boolean clear = true;
        boolean untouched = true;
        double most = 0.0;
        int lifted = 0;
        String failure = "";
        float[][] pivots = {standing, crouching, {-5.0F, 2.0F, 0.0F}};
        double[] grounds = {BladeCarry.GROUND, BladeCarry.CROUCH_GROUND, BladeCarry.GROUND - 4.0};
        for (int p = 0; p < pivots.length; p++) {
            for (double x = -3.3; x <= 1.61; x += 0.3) {
                for (double y = -1.6; y <= 1.61; y += 0.4) {
                    for (double z = -1.6; z <= 1.61; z += 0.4) {
                        for (double ext : new double[] {0.3, 1.0, 1.05}) {
                            float[] in = {(float)x, (float)y, (float)z};
                            float[] out = BladeCarry.guard(in[0], in[1], in[2], pivots[p], grounds[p], ext, p == 1);
                            double low = BladeCarry.lowest(BladeCarry.zyx(out[0], out[1], out[2]), pivots[p], ext, p == 1);
                            double before = BladeCarry.lowest(BladeCarry.zyx(in[0], in[1], in[2]), pivots[p], ext, p == 1);
                            if (low > grounds[p] - BladeCarry.CLEARANCE + 1.0E-3) {
                                clear = false;
                                failure = x + "," + y + "," + z + " -> " + low;
                            }
                            if (before <= grounds[p] - BladeCarry.CLEARANCE) {
                                untouched &= out[0] == in[0] && out[1] == in[1] && out[2] == in[2];
                            } else {
                                lifted++;
                                // Just enough: it ends right at the clearance, not flung up.
                                most = Math.max(most, grounds[p] - BladeCarry.CLEARANCE - low);
                            }
                        }
                    }
                }
            }
        }
        check(clear, "in any pose no part of the blade is below the ground (" + failure + ")");
        check(untouched, "the guard leaves every clear pose alone");
        check(lifted > 1000 && most < 0.06, "it lifts just enough (" + lifted + " lifted, at most " + most + " px over the clearance)");
        // Smoothly: a pose moving a little moves the guarded blade a little, never a jump; except right round the lift's still
        // point, which is down behind his left hip, through his own body, where the arm never points.
        double[] still = BladeCarry.STILL;
        check(still[1] > 0.7 && still[2] > 0.25 && still[0] > 0.15, "the lift's still point is down behind his left hip, through his body");
        double jump = 0.0;
        for (double x = -1.0; x <= 1.6; x += 0.05) {
            for (double z = -1.2; z <= 1.6; z += 0.2) {
                double[][] in = BladeCarry.zyx(x, 0.3, z);
                if (in[0][1] * still[0] + in[1][1] * still[1] + in[2][1] * still[2] > Math.cos(Math.toRadians(40.0))) {
                    continue;
                }
                float[] a = BladeCarry.guard((float)x, 0.3F, (float)z, standing, BladeCarry.GROUND, 1.0, false);
                float[] b = BladeCarry.guard((float)(x + 0.01), 0.3F, (float)z, standing, BladeCarry.GROUND, 1.0, false);
                double[][] ma = BladeCarry.zyx(a[0], a[1], a[2]);
                double[][] mb = BladeCarry.zyx(b[0], b[1], b[2]);
                double dot = ma[0][1] * mb[0][1] + ma[1][1] * mb[1][1] + ma[2][1] * mb[2][1];
                jump = Math.max(jump, Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
            }
        }
        check(jump < 0.03, "a little move of the arm moves the guarded blade a little (" + jump + " rad at most for 0.01)");
        // The extents cover the blade: no vertex of it reaches further out than the guard reckons.
        double ax = SteelBlade.axis(false);
        boolean covered = true;
        for (SteelBlade.Quad q : SteelBlade.build(1.0, 1.3, false)) {
            for (double[] c : q.corners()) {
                double s = Math.max(0.0, Math.min(1.0, (c[1] - SteelBlade.BASE_Y) / SteelBlade.fullLength()));
                covered &= Math.hypot(c[0] - ax, c[2]) <= SteelBlade.extent(s) + 1.0E-9;
            }
        }
        check(covered, "the guard's reckoning of the blade covers all of it");
    }

    private static double moved(List<SteelBlade.Quad> a, List<SteelBlade.Quad> b) {
        double most = 0.0;
        for (int i = 0; i < a.size(); i++) {
            for (int k = 0; k < 4; k++) {
                double[] p = a.get(i).corners()[k];
                double[] q = b.get(i).corners()[k];
                most = Math.max(most, Math.sqrt((p[0] - q[0]) * (p[0] - q[0]) + (p[1] - q[1]) * (p[1] - q[1]) + (p[2] - q[2]) * (p[2] - q[2])));
            }
        }
        return most;
    }
}
