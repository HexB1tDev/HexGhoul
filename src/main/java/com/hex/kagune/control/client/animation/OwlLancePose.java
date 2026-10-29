package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.pale.OwlLances;

/**
 * 6.2.0, the Owl's Watch This lances on her body as it is drawn (pure, tested): each out of its place on her back ({@link
 * OwlLances#px}), straight out of it and rising, arching over to where its spear is gripped ({@link OwlLances.Tip#knot}), coming at
 * it along the way the spear points, so the spear carries straight on out of the muscle. Rig space throughout.
 */
public final class OwlLancePose {
    private OwlLancePose() {
    }

    /** The muscle's path (root in her back first, the knot last) of lance {@code i} out of {@code body} to {@code tip}. */
    public static double[][] path(RigPose body, int i, OwlLances.Tip tip) {
        double[] px = OwlLances.px(i);
        double[] root = BodyFrame.world(body, BodyFrame.model(body, "torso", px));
        double[] behind = BodyFrame.world(body, BodyFrame.model(body, "torso", new double[] {px[0], px[1], px[2] + 1.0}));
        return path(root, normalize(sub(behind, root)), i, tip);
    }

    /**
     * 6.2.4: the same out of the Owl's creature ({@code rig}): out of the same place in its back.
     */
    public static double[][] path(com.hex.kagune.control.pale.OwlCreature.Rig rig, int i, OwlLances.Tip tip) {
        double[] px = OwlLances.px(i);
        return path(com.hex.kagune.control.pale.OwlCreature.backAt(rig, px), com.hex.kagune.control.pale.OwlCreature.backWay(rig, px), i, tip);
    }

    /** A lance's path from {@code root} on her, out {@code back} (unit), to its tip. */
    static double[][] path(double[] root, double[] back, int i, OwlLances.Tip tip) {
        double[] up = {0.0, 1.0, 0.0};
        double[] knot = tip.knot();
        double[] dir = normalize(tip.dir());
        // Straight out of her back first, then rising out toward its own way round her (the six fan out of her, never bunched), and
        // arching over to the knot.
        double b = OwlLances.bearing(i);
        double[] toward = {Math.sin(b), 0.0, Math.cos(b)};
        double[] stem = add(root, mul(back, 0.3));
        double[] out = add(root, add(add(mul(back, 0.55), mul(toward, 0.35)), mul(up, 0.35)));
        double reach = length(sub(knot, out));
        double[] before = sub(knot, mul(dir, Math.min(0.8, 0.3 + reach * 0.15)));
        double[] arch = add(lerp(out, before, 0.5), mul(up, Math.min(0.8, 0.15 + reach * 0.1)));
        return new double[][] {root, stem, out, arch, before, knot};
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] mul(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    static double length(double[] a) {
        return Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
    }

    static double[] normalize(double[] a) {
        double l = length(a);
        return l < 1.0E-9 ? new double[] {0.0, -1.0, 0.0} : mul(a, 1.0 / l);
    }
}
