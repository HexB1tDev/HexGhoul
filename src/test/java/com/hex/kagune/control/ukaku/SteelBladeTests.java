package com.hex.kagune.control.ukaku;

import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free tests for the Steel Blade (1.3.1: the ghouls' arm blade in the old blade's steel, slimmer and thinner, a bright edge),
 * its forming in order, how smoothly it forms and lives, and the ground guard.
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
        check(SteelBlade.LENGTH > 1.3 && SteelBlade.LENGTH < 1.7, "the ghouls' great blade, about a block and a half past the fist (" + SteelBlade.LENGTH + ")");
        check(Math.abs(SteelBlade.REACH - (SteelBlade.ARM + SteelBlade.LENGTH + 0.9)) < 1.0E-9 && SteelBlade.REACH >= 3.0,
            "its reach is the arm, all of its length and its sweep (" + SteelBlade.REACH + ")");
        check(Math.abs(SteelBlade.TIP_Y - (SteelBlade.HAND_Y + SteelBlade.LENGTH * SteelBlade.PIXELS)) < 1.0E-9, "the point is its length past the hand");
        check(SteelBlade.WIDTH < 1.0 && SteelBlade.THICKNESS < 1.0, "slimmer and thinner than the ghouls' (the owner: \"a bit slimmer\", \"thinner\")");
        // Its edge, root to point: on the back of the arm (z +, facing down with the arm held out), out past the spine, ending in a point.
        double far = 0.0;
        for (double s = 0.3; s <= 0.95; s += 0.05) {
            double[] e = SteelBlade.edgePoint(s, 1.0, false);
            far = Math.max(far, e[2]);
            check(Double.isFinite(e[0]) && Double.isFinite(e[1]) && Double.isFinite(e[2]), "its edge is somewhere (at " + s + ")");
        }
        check(far > 3.0, "its edge stands well out of the arm (" + far + " px)");
        double[] point = SteelBlade.edgePoint(1.0, 1.0, false);
        check(Math.abs(point[1] - SteelBlade.TIP_Y) < 0.6, "its edge runs to the point (" + point[1] + ")");
    }

    private static void model() {
        List<SteelBlade.Quad> quads = SteelBlade.build(1.0, 3.7, false);
        check(quads.size() > 200 && quads.size() < 2000, "a few hundred quads (" + quads.size() + "; about 4400 before 1.3.1)");
        boolean sane = true;
        boolean unit = true;
        double far = 0.0;
        int gleam = 0;
        for (SteelBlade.Quad q : quads) {
            for (int k = 0; k < 4; k++) {
                double[] c = q.corners()[k];
                double[] n = q.normals()[k];
                sane &= Double.isFinite(c[0]) && Double.isFinite(c[1]) && Double.isFinite(c[2]) && Math.abs(c[0]) < 12.0 && Math.abs(c[2]) < 12.0
                    && c[1] > -4.0 && c[1] <= SteelBlade.TIP_Y + 0.5;
                unit &= Math.abs(Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]) - 1.0) < 1.0E-3;
                far = Math.max(far, c[1]);
            }
            if (SteelBlade.glows(q.normals()[0][0], q.normals()[0][1], q.normals()[0][2])) {
                gleam++;
                // The edge's bright line: near white.
                int c = q.argb()[0];
                check(((c >> 16) & 0xFF) > 220 && ((c >> 8) & 0xFF) > 220 && (c & 0xFF) > 220, "its edge is a bright line");
            }
        }
        check(sane, "every corner is finite and on the arm or the blade");
        check(unit, "every normal is a unit");
        check(gleam > 20, "a bright line glows all down its edge (" + gleam + " quads)");
        check(Math.abs(far - SteelBlade.TIP_Y) < 0.6, "the point is where its length puts it (" + far + " vs " + SteelBlade.TIP_Y + ")");
        check(SteelBlade.build(1.0, 0.0, true).size() > 0, "on slim arms too");
        check(SteelBlade.build(0.004, 0.0, false).isEmpty(), "open, nothing");
        // Steel, not the ghouls' kagune colour: its plain faces grey, clouded, never red (their veins are inked grooves on it).
        boolean grey = true;
        for (SteelBlade.Quad q : quads) {
            int c = q.argb()[0];
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            grey &= r <= g + 20 || r < 80;
        }
        check(grey, "it is the old Steel Blade's steel, nowhere red");
        // Blood after it cuts, on the blade only.
        boolean bloody = false;
        List<SteelBlade.Quad> cut = new ArrayList<>();
        SteelBlade.build(1.0, 0.0, false, true, 1.0, 0.0, SteelBlade.collect(cut));
        for (SteelBlade.Quad q : cut) {
            int c = q.argb()[0];
            bloody |= ((c >> 16) & 0xFF) > ((c >> 8) & 0xFF) + 60;
        }
        check(bloody, "after it cuts, blood on it");
    }

    private static void forming() {
        check(SteelBlade.formed(true, 0.0) == 0.0 && SteelBlade.formed(true, SteelBlade.FORM_TICKS) == 1.0
            && SteelBlade.formed(false, SteelBlade.FORM_TICKS) == 0.0 && SteelBlade.formed(false, 0.0) == 1.0, "it closes and opens");
        check(SteelBlade.fold(0.0) == 0.0 && SteelBlade.fold(0.42) == 1.0, "the steel tentacles go in as the ribbons come");
        // In order: the ribbons wind down the arm first; the blade grows once they reach the wrist.
        check(SteelBlade.extend(0.5) == 0.0 && SteelBlade.extend(1.0) == 1.0, "the blade grows last");
        check(!SteelBlade.build(0.1, 0.0, false).isEmpty(), "the ribbons are on their way from the first ticks");
    }

    /** Smooth: at 60 frames a second (3 a tick) the blade's growth never jumps far in a frame, and its life barely moves it. */
    private static void smooth() {
        double most = 0.0;
        int frames = SteelBlade.FORM_TICKS * 3;
        for (int frame = 0; frame < frames; frame++) {
            double a = SteelBlade.formed(true, frame / 3.0);
            double b = SteelBlade.formed(true, (frame + 1) / 3.0);
            most = Math.max(most, Math.abs(SteelBlade.extend(b) - SteelBlade.extend(a)));
            most = Math.max(most, Math.abs(SteelBlade.fold(b) - SteelBlade.fold(a)));
        }
        check(most < 0.17, "no phase moves more than a sixth of its way in one frame (" + most + ")");
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
        // 1.3.1: the arm hanging straight down (the ghouls' moves hold it up; this is the worst it can be): drawn in, clear of the ground.
        float[] hold = {0.0F, 0.0F, 0.0F};
        double rest = BladeCarry.fit(BladeCarry.zyx(hold[0], hold[1], hold[2]), standing, BladeCarry.GROUND, 1.0, false);
        check(rest < 1.0 && BladeCarry.lowest(BladeCarry.zyx(hold[0], hold[1], hold[2]), standing, rest, false) <= limit + 1.0E-3,
            "hanging straight down, it draws in to clear the ground (" + rest + ")");
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
