package com.hex.kagune.control.awakening.geo;

/** Row-major 4x4 matrices as {@code double[16]}; just what bone kinematics needs. */
public final class Mat4 {
    private Mat4() {
    }

    public static double[] identity() {
        return new double[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }

    public static double[] multiply(double[] a, double[] b) {
        double[] out = new double[16];
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                out[r * 4 + c] = a[r * 4] * b[c] + a[r * 4 + 1] * b[4 + c] + a[r * 4 + 2] * b[8 + c] + a[r * 4 + 3] * b[12 + c];
            }
        }
        return out;
    }

    public static double[] translation(double x, double y, double z) {
        return new double[] {1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1};
    }

    public static double[] scaling(double x, double y, double z) {
        return new double[] {x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0, 0, 0, 0, 1};
    }

    public static double[] rotationX(double radians) {
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        return new double[] {1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0, 0, 0, 0, 1};
    }

    public static double[] rotationY(double radians) {
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        return new double[] {c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0, 0, 0, 0, 1};
    }

    public static double[] rotationZ(double radians) {
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        return new double[] {c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }

    /** The point {@code (x, y, z)} transformed by {@code m}. */
    public static double[] point(double[] m, double x, double y, double z) {
        return new double[] {
                m[0] * x + m[1] * y + m[2] * z + m[3],
                m[4] * x + m[5] * y + m[6] * z + m[7],
                m[8] * x + m[9] * y + m[10] * z + m[11]};
    }

    /** The direction {@code (x, y, z)} transformed by the linear part of {@code m}. */
    public static double[] direction(double[] m, double x, double y, double z) {
        return new double[] {
                m[0] * x + m[1] * y + m[2] * z,
                m[4] * x + m[5] * y + m[6] * z,
                m[8] * x + m[9] * y + m[10] * z};
    }

    /** Rotation about a pivot in GeckoLib's order: pivot, then Z, Y, X, then back. */
    static double[] pivoted(double px, double py, double pz, double rx, double ry, double rz, double sx, double sy, double sz) {
        double[] m = translation(px, py, pz);
        if (rz != 0.0) m = multiply(m, rotationZ(rz));
        if (ry != 0.0) m = multiply(m, rotationY(ry));
        if (rx != 0.0) m = multiply(m, rotationX(rx));
        if (sx != 1.0 || sy != 1.0 || sz != 1.0) m = multiply(m, scaling(sx, sy, sz));
        return multiply(m, translation(-px, -py, -pz));
    }
}
