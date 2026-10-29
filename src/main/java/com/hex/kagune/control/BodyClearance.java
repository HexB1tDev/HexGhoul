package com.hex.kagune.control;

/**
 * Keeps a drawn kagune limb out of its owner's body (3.0.0; pure, tested). The limb grows from his lower back, and a strike
 * at something in front of him would otherwise draw it straight through his torso: the original only routes held and
 * grappling limbs around him. Here every drawn path is checked against his body (the torso with the arms beside it, and the
 * head, as boxes) widened by the limb's own radius; when it cuts in, the stretch from the root to where it is clear again is
 * bent round his side (the limb's own side), blended in by how deep it cut so it never snaps, and whatever still grazes him
 * is pushed out. Near the root the clearance grows from nothing (the limb has to leave his back somewhere).
 */
public final class BodyClearance {
    /** Extra air between the limb's surface and his body (blocks). */
    public static final double MARGIN = 0.04;
    /** Arc length from the root over which the clearance grows to its full size (blocks). */
    public static final double ROOT_EASE = 0.35;
    /** How deep a cut gets the full reroute (blocks); shallower ones blend it in. */
    public static final double BLEND_DEPTH = 0.12;

    private BodyClearance() {
    }

    /**
     * Signed distance (blocks; negative inside) from a point to his body, in his body frame: {@code across} toward his right,
     * {@code up} from his feet, {@code ahead} along his body's facing. {@code width} and {@code height} are his hitbox's.
     */
    public static double distance(double across, double up, double ahead, double width, double height) {
        double neck = height * 0.75;
        double torso = box(across, up, ahead, width * 0.83, 0.0, neck, width * 0.21);
        double head = box(across, up, ahead, width * 0.42, neck, height + 0.03, width * 0.42);
        return Math.min(torso, head);
    }

    /** Signed distance to an axis-aligned box centred on the body axis, {@code y0..y1} high. */
    static double box(double x, double y, double z, double halfX, double y0, double y1, double halfZ) {
        double dx = Math.abs(x) - halfX;
        double dy = Math.abs(y - (y0 + y1) * 0.5) - (y1 - y0) * 0.5;
        double dz = Math.abs(z) - halfZ;
        double outside = Math.sqrt(square(Math.max(dx, 0.0)) + square(Math.max(dy, 0.0)) + square(Math.max(dz, 0.0)));
        return outside + Math.min(Math.max(dx, Math.max(dy, dz)), 0.0);
    }

    /**
     * Clears the path {@code points} (world {x, y, z}; {@code points[0]} the root, kept) of the body of an owner standing at
     * {@code feet} facing ({@code forwardX}, {@code forwardZ}) (horizontal, unit). {@code radius} is the limb's radius,
     * {@code side} +1 if the limb belongs to his right side, -1 his left. Returns true if anything moved.
     */
    public static boolean clear(double[][] points, double[] feet, double forwardX, double forwardZ, double width, double height, double radius,
                                double side) {
        int n = points.length;
        if (n < 3) {
            return false;
        }
        double rightX = forwardZ;
        double rightZ = -forwardX;
        double[] arc = new double[n];
        for (int i = 1; i < n; i++) {
            arc[i] = arc[i - 1] + Math.sqrt(square(points[i][0] - points[i - 1][0]) + square(points[i][1] - points[i - 1][1])
                + square(points[i][2] - points[i - 1][2]));
        }
        double worst = 0.0;
        int last = -1;
        for (int i = 1; i < n; i++) {
            double cut = needed(arc[i], radius) - distanceOf(points[i], feet, forwardX, forwardZ, rightX, rightZ, width, height);
            if (cut > 0.0) {
                worst = Math.max(worst, cut);
                last = i;
            }
        }
        if (last < 0) {
            return false;
        }
        int rejoin = Math.min(n - 1, last + 1);
        // Round the side the path rejoins on when it is clearly to one side, else the limb's own side.
        double rejoinAcross = (points[rejoin][0] - feet[0]) * rightX + (points[rejoin][2] - feet[2]) * rightZ;
        double out = Math.abs(rejoinAcross) > width * 0.6 ? Math.signum(rejoinAcross) : side >= 0.0 ? 1.0 : -1.0;
        if (rejoin == n - 1 && last == n - 1) {
            push(points[rejoin], arc[rejoin], radius, feet, forwardX, forwardZ, rightX, rightZ, width, height, out);
        }
        double weight = smooth(worst / BLEND_DEPTH);
        double reach = width * 0.83 + radius + MARGIN + 0.1;
        double[] root = points[0];
        double[] end = points[rejoin].clone();
        double[] a = {root[0] - forwardX * 0.3 + rightX * out * reach, root[1] + 0.05, root[2] - forwardZ * 0.3 + rightZ * out * reach};
        double[] b = {end[0] + rightX * out * reach * 0.6 - forwardX * 0.1, end[1], end[2] + rightZ * out * reach * 0.6 - forwardZ * 0.1};
        for (int i = 1; i < rejoin; i++) {
            double t = i / (double)rejoin;
            double[] around = cubic(root, a, b, end, t);
            for (int k = 0; k < 3; k++) {
                points[i][k] += (around[k] - points[i][k]) * weight;
            }
        }
        settle(points, radius, feet, forwardX, forwardZ, rightX, rightZ, width, height, out);
        // The detour crowds a few points round his side: spread them evenly along the new path again, then settle once more.
        respace(points);
        settle(points, radius, feet, forwardX, forwardZ, rightX, rightZ, width, height, out);
        return true;
    }

    /** Pushes every point out; pushing one lengthens the path behind it (and the clearance grows with it), so a few passes. */
    private static void settle(double[][] points, double radius, double[] feet, double forwardX, double forwardZ, double rightX, double rightZ,
                               double width, double height, double out) {
        int n = points.length;
        for (int pass = 0; pass < 3; pass++) {
            double along = 0.0;
            for (int i = 1; i < n; i++) {
                along += Math.sqrt(square(points[i][0] - points[i - 1][0]) + square(points[i][1] - points[i - 1][1])
                    + square(points[i][2] - points[i - 1][2]));
                push(points[i], along, radius, feet, forwardX, forwardZ, rightX, rightZ, width, height, out);
            }
        }
    }

    /** Re-spaces the points evenly along the polyline they make (the root and the tip stay). */
    static void respace(double[][] points) {
        int n = points.length;
        double[] arc = new double[n];
        for (int i = 1; i < n; i++) {
            arc[i] = arc[i - 1] + Math.sqrt(square(points[i][0] - points[i - 1][0]) + square(points[i][1] - points[i - 1][1])
                + square(points[i][2] - points[i - 1][2]));
        }
        double total = arc[n - 1];
        if (total < 1.0E-9) {
            return;
        }
        double[][] source = new double[n][];
        for (int i = 0; i < n; i++) {
            source[i] = points[i].clone();
        }
        int segment = 0;
        for (int i = 1; i < n - 1; i++) {
            double at = total * i / (n - 1);
            while (segment < n - 2 && arc[segment + 1] < at) {
                segment++;
            }
            double span = arc[segment + 1] - arc[segment];
            double t = span < 1.0E-12 ? 0.0 : (at - arc[segment]) / span;
            for (int k = 0; k < 3; k++) {
                points[i][k] = source[segment][k] + (source[segment + 1][k] - source[segment][k]) * t;
            }
        }
    }

    /** The clearance a point {@code arc} along the limb needs: none at the root, the limb's radius plus air past {@link #ROOT_EASE}. */
    static double needed(double arc, double radius) {
        return (radius + MARGIN) * smooth(arc / ROOT_EASE);
    }

    private static double distanceOf(double[] p, double[] feet, double forwardX, double forwardZ, double rightX, double rightZ, double width,
                                     double height) {
        double x = p[0] - feet[0];
        double z = p[2] - feet[2];
        return distance(x * rightX + z * rightZ, p[1] - feet[1], x * forwardX + z * forwardZ, width, height);
    }

    /** Pushes one point straight out from his body axis (scaled to the body's shape) until it clears it. */
    private static void push(double[] p, double arc, double radius, double[] feet, double forwardX, double forwardZ, double rightX, double rightZ,
                             double width, double height, double out) {
        double need = needed(arc, radius);
        for (int pass = 0; pass < 6; pass++) {
            double gap = distanceOf(p, feet, forwardX, forwardZ, rightX, rightZ, width, height) - need;
            if (gap >= -1.0E-9) {
                return;
            }
            double x = p[0] - feet[0];
            double z = p[2] - feet[2];
            double across = (x * rightX + z * rightZ) / (width * 0.83);
            double ahead = (x * forwardX + z * forwardZ) / (width * 0.21);
            double length = Math.sqrt(across * across + ahead * ahead);
            if (length < 1.0E-3) {
                across = out;
                ahead = -0.3;
                length = Math.sqrt(across * across + ahead * ahead);
            }
            across /= length;
            ahead /= length;
            double dx = across * rightX + ahead * forwardX;
            double dz = across * rightZ + ahead * forwardZ;
            double norm = Math.sqrt(dx * dx + dz * dz);
            double step = -gap + 0.005;
            p[0] += dx / norm * step;
            p[2] += dz / norm * step;
        }
    }

    static double[] cubic(double[] a, double[] b, double[] c, double[] d, double t) {
        double u = 1.0 - t;
        double w0 = u * u * u;
        double w1 = 3.0 * u * u * t;
        double w2 = 3.0 * u * t * t;
        double w3 = t * t * t;
        return new double[] {w0 * a[0] + w1 * b[0] + w2 * c[0] + w3 * d[0], w0 * a[1] + w1 * b[1] + w2 * c[1] + w3 * d[1],
            w0 * a[2] + w1 * b[2] + w2 * c[2] + w3 * d[2]};
    }

    static double smooth(double s) {
        double c = Math.max(0.0, Math.min(1.0, s));
        return c * c * (3.0 - 2.0 * c);
    }

    private static double square(double v) {
        return v * v;
    }
}
