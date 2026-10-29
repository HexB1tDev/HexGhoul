package com.hex.kagune.control.ayato;

/**
 * The Steel Blade's ground guard (pure, tested; 5.0.19). Whatever moves the blade arm (the hold, the swings, a lean, a crouch, a
 * drop into a landing), {@link #guard} keeps every part of the blade off the ground: when the pose would put any of it below his
 * feet, the arm is lifted (up, drawn round toward {@link #LIFT_X ahead of him to his right}) just far enough to clear it, keeping
 * the blade's own roll (its edge stays where it was turned), and smoothly: poses near each other are lifted near each other.
 * (5.0.20: the hold itself is {@link SteelSwing#hold}; until then the arm was carried en garde along the lift's way.)
 *
 * <p>Model space throughout: pixels, x to the player's left, y down, z backward; an arm rotation is ModelPart's (Rz Ry Rx).
 */
public final class BladeCarry {
    /** The way the guard lifts the blade toward, as the right arm's rotation: ahead of him to his right, a little raised. */
    public static final float LIFT_X = -1.036F;
    public static final float LIFT_Y = 0.343F;
    public static final float LIFT_Z = 1.371F;
    /** Where the ground is below a standing model (its feet), and how far a crouch lowers the model (the renderer's offset). */
    public static final float GROUND = 24.0F;
    public static final float CROUCH_GROUND = 24.0F - 0.125F * 16.0F / 0.9375F;
    /** The right shoulder's pivot height, standing and crouching (vanilla's). */
    public static final float SHOULDER = 2.0F;
    public static final float CROUCH_SHOULDER = 5.2F;
    /** How far above the ground the blade's lowest point is kept (pixels). */
    public static final double CLEARANCE = 1.0;
    /** Stations checked along the blade. */
    static final int STATIONS = 48;

    private BladeCarry() {
    }

    /**
     * The arm rotation {@code (x, y, z)} with the shoulder's pivot at {@code pivot}, lifted just enough that no part of the blade
     * (driven {@code ext} of the way out) is below {@code ground} (model y) less {@link #CLEARANCE}; as it was when it is clear.
     */
    public static float[] guard(float x, float y, float z, float[] pivot, double ground, double ext, boolean slim) {
        double[][] m = zyx(x, y, z);
        if (ext <= 0.0 || lowest(m, pivot, ext, slim) <= ground - CLEARANCE) {
            return new float[] {x, y, z};
        }
        // Aimed a hair inside the clearance, for the angles' rounding to floats.
        double limit = ground - CLEARANCE - 0.02;
        // Lift it along a smooth flow: up off the ground, nudged round toward the carry's direction (ahead of him, to his right),
        // the whole arm turned with it so the blade keeps its roll (its edge stays down). Poses near each other flow near each
        // other, so the lifted blade never jumps; the flow's one still point is far down behind his left hip (through his body),
        // where the arm never goes.
        double[][] at = m;
        for (int i = 0; i < 90; i++) {
            double[] d = column(at, 1);
            double[] v = flow(d);
            double[] axis = {d[1] * v[2] - d[2] * v[1], d[2] * v[0] - d[0] * v[2], d[0] * v[1] - d[1] * v[0]};
            double length = Math.sqrt(axis[0] * axis[0] + axis[1] * axis[1] + axis[2] * axis[2]);
            if (length < 1.0E-9) {
                break;
            }
            axis = new double[] {axis[0] / length, axis[1] / length, axis[2] / length};
            double[][] next = multiply(turn(axis, STEP), at);
            if (lowest(next, pivot, ext, slim) <= limit) {
                // Clear within this step: halved down to a hair.
                double lo = 0.0;
                double hi = STEP;
                for (int k = 0; k < 14; k++) {
                    double mid = 0.5 * (lo + hi);
                    if (lowest(multiply(turn(axis, mid), at), pivot, ext, slim) > limit) {
                        lo = mid;
                    } else {
                        hi = mid;
                    }
                }
                return euler(multiply(turn(axis, hi), at));
            }
            at = next;
        }
        return euler(at);
    }

    /** 6.0.4: the shortest the blade draws in to (of its length past the fist) when it rests near the ground. */
    public static final double MIN_REACH = 0.3;

    /**
     * 6.0.4: how far the blade can be driven out ({@code ext} at most, never under {@link #MIN_REACH} of it) with the arm turned by
     * {@code m} about {@code pivot} and every part of it still {@link #CLEARANCE} off {@code ground}: the length it draws in to
     * when it rests hanging low (the hold), instead of the arm being lifted. Below the shortest it lets the guard lift the arm.
     */
    public static double fit(double[][] m, float[] pivot, double ground, double ext, boolean slim) {
        double limit = ground - CLEARANCE - 0.02;
        if (ext <= 0.0 || lowest(m, pivot, ext, slim) <= limit) {
            return ext;
        }
        double lo = ext * MIN_REACH;
        if (lowest(m, pivot, lo, slim) > limit) {
            return lo;
        }
        double hi = ext;
        for (int k = 0; k < 18; k++) {
            double mid = 0.5 * (lo + hi);
            if (lowest(m, pivot, mid, slim) <= limit) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    /** How far the lift turns the arm a step (radians). */
    static final double STEP = 0.04;
    /** How strongly the lift is drawn round toward the carry's direction as it rises. */
    static final double TOWARD_CARRY = 0.6;

    /**
     * Which way the lift turns the blade pointing {@code d}: up, and round toward the carry (a tangent at {@code d}). It is the
     * turn toward one fixed way ({@link #STILL}'s opposite), so its one still point is {@link #STILL}.
     */
    static double[] flow(double[] d) {
        double[] c = CARRY_DIRECTION;
        double up = -d[1];
        double toward = d[0] * c[0] + d[1] * c[1] + d[2] * c[2];
        return new double[] {-d[0] * up + TOWARD_CARRY * (c[0] - d[0] * toward), -1.0 - d[1] * up + TOWARD_CARRY * (c[1] - d[1] * toward),
            -d[2] * up + TOWARD_CARRY * (c[2] - d[2] * toward)};
    }

    /** Which way the guard lifts the blade toward (model space, unit). */
    static final double[] CARRY_DIRECTION = column(zyx(LIFT_X, LIFT_Y, LIFT_Z), 1);
    /** The lift's one still point: the blade pointing down and back behind his left hip (through his body). */
    static final double[] STILL = still();

    static double[] still() {
        double[] w = {TOWARD_CARRY * CARRY_DIRECTION[0], -1.0 + TOWARD_CARRY * CARRY_DIRECTION[1], TOWARD_CARRY * CARRY_DIRECTION[2]};
        double l = Math.sqrt(w[0] * w[0] + w[1] * w[1] + w[2] * w[2]);
        return new double[] {-w[0] / l, -w[1] / l, -w[2] / l};
    }

    static double[] column(double[][] m, int j) {
        return new double[] {m[0][j], m[1][j], m[2][j]};
    }

    /**
     * The lowest any part of the blade reaches (the largest model y) with the arm turned by {@code m} about {@code pivot}: down
     * its length, each station's axis point and as far as the blade reaches out round it there.
     */
    public static double lowest(double[][] m, float[] pivot, double ext, boolean slim) {
        double ax = SteelBlade.axis(slim);
        double dy = m[1][1];
        double across = Math.sqrt(Math.max(0.0, 1.0 - dy * dy));
        double most = -1.0E9;
        for (int i = 0; i <= STATIONS; i++) {
            double s = i / (double)STATIONS;
            double ly = SteelBlade.station(s);
            if (ly > SteelBlade.GRIP_Y) {
                ly = SteelBlade.GRIP_Y + (ly - SteelBlade.GRIP_Y) * ext;
            }
            double y = pivot[1] + m[1][0] * ax + m[1][1] * ly;
            most = Math.max(most, y + SteelBlade.extent(s) * across);
        }
        return most;
    }

    /** ModelPart's rotation as a matrix: Rz(z) Ry(y) Rx(x). */
    public static double[][] zyx(double x, double y, double z) {
        double cx = Math.cos(x);
        double sx = Math.sin(x);
        double cy = Math.cos(y);
        double sy = Math.sin(y);
        double cz = Math.cos(z);
        double sz = Math.sin(z);
        double[][] rx = {{1, 0, 0}, {0, cx, -sx}, {0, sx, cx}};
        double[][] ry = {{cy, 0, sy}, {0, 1, 0}, {-sy, 0, cy}};
        double[][] rz = {{cz, -sz, 0}, {sz, cz, 0}, {0, 0, 1}};
        return multiply(rz, multiply(ry, rx));
    }

    /** The three angles (x, y, z) of a rotation matrix, as ModelPart applies them (Rz Ry Rx). */
    static float[] euler(double[][] m) {
        double sinY = Math.max(-1.0, Math.min(1.0, -m[2][0]));
        double ry = Math.asin(sinY);
        double rx;
        double rz;
        if (Math.abs(sinY) < 0.999999) {
            rx = Math.atan2(m[2][1], m[2][2]);
            rz = Math.atan2(m[1][0], m[0][0]);
        } else {
            rz = 0.0;
            rx = Math.atan2(-m[1][2], m[1][1]);
        }
        return new float[] {(float)rx, (float)ry, (float)rz};
    }

    /** A turn by {@code angle} about the unit {@code axis} (Rodrigues). */
    static double[][] turn(double[] axis, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double t = 1.0 - c;
        double x = axis[0];
        double y = axis[1];
        double z = axis[2];
        return new double[][] {{c + x * x * t, x * y * t - z * s, x * z * t + y * s}, {y * x * t + z * s, c + y * y * t, y * z * t - x * s},
            {z * x * t - y * s, z * y * t + x * s, c + z * z * t}};
    }

    static double[][] multiply(double[][] a, double[][] b) {
        double[][] out = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                out[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
            }
        }
        return out;
    }
}
