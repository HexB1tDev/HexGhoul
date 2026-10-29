package com.hex.kagune.control.ayato;

import static com.hex.kagune.control.ayato.UkakuModel.*;

/**
 * The :re One-Winged Kagune: one giant blade of overlapping crimson crystal plates on a crescent spine, a needle tip,
 * thorns along its back edge, dark inclusions, a bright cutting edge, and plates over the shoulder at its root.
 */
public final class UkakuOneWing {
    // One-Winged Kagune palette (crimson crystal).
    static final int BLADE_RED = 0xD5121E;
    static final int BLADE_LIGHT = 0xFF3B30;
    static final int BLADE_DEEP = 0x7E0A16;
    static final int BLADE_EDGE = 0xFFB0A0;
    static final int BLADE_INCLUSION = 0x2C0A3A;

    private UkakuOneWing() {
    }

    // ------------------------------------------------------------------ the One-Winged Kagune

    /**
     * The :re One-Winged Kagune: one giant blade of overlapping crimson crystal plates on a crescent spine, a needle tip,
     * thorns along its back edge, dark inclusions, a bright cutting edge, and plates over the shoulder at its root.
     * The frame's {@code out} runs along the blade, {@code up} toward its back edge, {@code back} out of its face.
     */
    public static void wing(Sink sink, Frame frame, Params p) {
        float deploy = clamp01(p.deploy());
        if (deploy <= 0.001F || p.alpha() <= 0.003F) {
            return;
        }
        double grow = p.dissolving() ? smooth(deploy / 0.85) : smooth(deploy / 0.8);
        double alpha = clamp01(p.alpha());
        boolean fine = p.detail() >= 0.6F;
        int plates = 6;
        int segments = fine ? 5 : 2;
        for (int plate = 0; plate < plates; plate++) {
            double from = plate / (double)plates - (plate == 0 ? 0.0 : 0.035);
            double to = (plate + 1) / (double)plates + 0.02;
            if (from >= grow) {
                break;
            }
            to = Math.min(to, grow);
            plate(sink, frame, plate, plates, from, to, segments, alpha, fine);
        }
        // Thorns jut back from the joints between plates (the barbs along the manga blade).
        for (int thorn = 0; thorn < 3; thorn++) {
            double t = (2 + thorn) / (double)plates;
            if (t > grow) {
                continue;
            }
            double[] s = crescent(t);
            double[] d = crescentTangent(t);
            double nx = -d[1];
            double ny = d[0];
            double width = bladeWidth(t);
            double depth = 0.034 * (2 + thorn);
            double length = 0.17 - 0.025 * thorn;
            double[] base = {s[0] + nx * width, s[1] + ny * width, depth};
            double[] tip = {base[0] + nx * length * 0.75 - d[0] * length * 0.7, base[1] + ny * length * 0.75 - d[1] * length * 0.7, depth};
            double[] upper = {base[0] + d[0] * 0.05, base[1] + d[1] * 0.05, depth + 0.02};
            double[] lower = {base[0] - d[0] * 0.05, base[1] - d[1] * 0.05, depth - 0.016};
            double[] centre = {s[0], s[1], depth};
            int dark = argb(BLADE_DEEP, 0.96 * alpha);
            int point = argb(BLADE_LIGHT, 0.96 * alpha);
            facet(sink, frame, upper, tip, tip, lower, centre, dark, point, point, dark);
            facet(sink, frame, lower, tip, tip, new double[] {base[0], base[1], depth + 0.02}, centre, dark, point, point, dark);
        }
        // Shoulder plates at the root.
        for (int plate = 0; plate < 2; plate++) {
            double angle = Math.toRadians(120 + 25 * plate);
            double len = (0.11 - 0.02 * plate) * Math.min(1.0, grow * 1.5);
            double[] r0 = {0.0, 0.0, -0.02 - 0.02 * plate};
            double[] tip = {Math.cos(angle) * len, Math.sin(angle) * len, r0[2]};
            double side = 0.035;
            double[] l = {Math.cos(angle) * len * 0.4 - Math.sin(angle) * side, Math.sin(angle) * len * 0.4 + Math.cos(angle) * side, r0[2] + 0.015};
            double[] rr = {Math.cos(angle) * len * 0.4 + Math.sin(angle) * side, Math.sin(angle) * len * 0.4 - Math.cos(angle) * side, r0[2] - 0.01};
            int body = argb(BLADE_RED, 0.95 * alpha);
            int point = argb(BLADE_LIGHT, 0.95 * alpha);
            facet(sink, frame, r0, l, tip, tip, new double[] {0, 0, r0[2] - 0.02}, body, body, point, point);
            facet(sink, frame, r0, rr, tip, tip, new double[] {0, 0, r0[2] + 0.02}, body, body, point, point);
        }
        if (p.dissolving() && deploy < 0.999F) {
            for (int chip = 0; chip < 4; chip++) {
                UkakuFeathers.Feather pseudo = new UkakuFeathers.Feather(40 + 15 * chip, 0.9, 0.1, 0, 0, 0.02, 0, false, 20 + chip);
                UkakuFeathers.chips(sink, frame, pseudo, p);
            }
        }
    }

    /** Half-width of the One-Wing at {@code t} along its spine: broad over the middle, a needle at the tip. */
    static double bladeWidth(double t) {
        return (0.05 + 0.15 * Math.pow(Math.sin(Math.PI * Math.min(1.0, t * 1.05)), 0.8)) * Math.pow(Math.max(0.0, 1.0 - t), 0.3);
    }

    /**
     * One crystal plate of the One-Wing: a faceted slab from {@code from} to {@code to} along the spine, broad at its
     * leading end and narrowing back like a shingle, its back corner jutting out, stacked a little above the previous one.
     */
    private static void plate(Sink sink, Frame frame, int index, int plates, double from, double to, int segments, double alpha, boolean fine) {
        double depth = 0.034 * index;
        double[][] cut = new double[segments + 1][];
        double[][] face = new double[segments + 1][];
        double[][] back = new double[segments + 1][];
        double[][] belly = new double[segments + 1][];
        double[][] spine = new double[segments + 1][];
        for (int k = 0; k <= segments; k++) {
            double local = k / (double)segments;
            double t = from + (to - from) * local;
            double[] s = crescent(t);
            double[] d = crescentTangent(t);
            double nx = -d[1];
            double ny = d[0];
            double shingle = local < 0.2 ? 0.55 + 0.45 * (local / 0.2) : 1.0 - 0.38 * (local - 0.2) / 0.8;
            double width = bladeWidth(t) * shingle;
            boolean last = index == plates - 1;
            double jut = !last && k == segments ? 0.35 : 0.0;
            double th = Math.max(0.006, (0.05 - 0.004 * index) * shingle * (last ? Math.pow(1.0 - local, 0.6) : 1.0));
            cut[k] = new double[] {s[0] - nx * width * 0.6, s[1] - ny * width * 0.6, depth};
            face[k] = new double[] {s[0] + nx * width * 0.05, s[1] + ny * width * 0.05, depth + th};
            back[k] = new double[] {s[0] + nx * width * (1.0 + jut) - d[0] * jut * 0.08, s[1] + ny * width * (1.0 + jut) - d[1] * jut * 0.08, depth};
            belly[k] = new double[] {s[0] + nx * width * 0.05, s[1] + ny * width * 0.05, depth - th * 0.8};
            spine[k] = new double[] {s[0], s[1], depth};
        }
        int light = index % 2 == 0 ? BLADE_LIGHT : mixInt(BLADE_RED, BLADE_DEEP, 0.2);
        for (int k = 0; k < segments; k++) {
            double local0 = k / (double)segments;
            double local1 = (k + 1) / (double)segments;
            // Each plate is brightest at its leading end and darkens toward the next plate's overlap.
            int top0 = argb(mixInt(light, BLADE_RED, local0 * 0.8), 0.95 * alpha);
            int top1 = argb(mixInt(light, BLADE_RED, local1 * 0.8), 0.95 * alpha);
            int dark0 = argb(mixInt(BLADE_RED, BLADE_DEEP, 0.5 + 0.3 * local0), 0.95 * alpha);
            int dark1 = argb(mixInt(BLADE_RED, BLADE_DEEP, 0.5 + 0.3 * local1), 0.95 * alpha);
            int edge = argb(mixInt(BLADE_LIGHT, BLADE_EDGE, 0.6), 0.97 * alpha);
            double[] centre = spine[k];
            facet(sink, frame, cut[k], cut[k + 1], face[k + 1], face[k], centre, edge, edge, top1, top0);
            facet(sink, frame, face[k], face[k + 1], back[k + 1], back[k], centre, top0, top1, dark1, dark0);
            facet(sink, frame, back[k], back[k + 1], belly[k + 1], belly[k], centre, dark0, dark1, dark1, dark0);
            facet(sink, frame, belly[k], belly[k + 1], cut[k + 1], cut[k], centre, dark0, dark1, edge, edge);
            // The cutting edge burns white-hot; the back edge smoulders red; a soft glow around both.
            double[] o0 = outward(cut[k], spine[k], 0.012);
            double[] o1 = outward(cut[k + 1], spine[k + 1], 0.012);
            int hot = argb(BLADE_EDGE, 0.95 * alpha);
            glowQuad(sink, frame, cut[k], cut[k + 1], o1, o0, hot, hot, hot, hot);
            double[] g0 = outward(cut[k], spine[k], 0.045);
            double[] g1 = outward(cut[k + 1], spine[k + 1], 0.045);
            glowQuad(sink, frame, o0, o1, g1, g0, argb(UkakuFeathers.GLOW, 0.3 * alpha), argb(UkakuFeathers.GLOW, 0.3 * alpha), argb(UkakuFeathers.GLOW, 0.0), argb(UkakuFeathers.GLOW, 0.0));
            double[] b0 = outward(back[k], spine[k], 0.03);
            double[] b1 = outward(back[k + 1], spine[k + 1], 0.03);
            glowQuad(sink, frame, back[k], back[k + 1], b1, b0, argb(UkakuFeathers.RIM, 0.6 * alpha), argb(UkakuFeathers.RIM, 0.6 * alpha), argb(UkakuFeathers.RIM, 0.0), argb(UkakuFeathers.RIM, 0.0));
        }
        // Dark inclusions marbling the crystal.
        if (fine) {
            for (int streak = 0; streak < 2; streak++) {
                double local = 0.3 + 0.35 * streak;
                double t = from + (to - from) * local;
                double[] s = crescent(t);
                double[] d = crescentTangent(t);
                double nx = -d[1];
                double ny = d[0];
                double lateral = (hash(index * 11 + streak * 5 + 3) - 0.4) * bladeWidth(t);
                double len = 0.05 + 0.04 * hash(index * 7 + streak);
                double z = depth + 0.05 * 0.9 + 0.003;
                double cx = s[0] + nx * lateral;
                double cy = s[1] + ny * lateral;
                int ink = argb(BLADE_INCLUSION, 0.7 * alpha);
                glowQuad(sink, frame, new double[] {cx - d[0] * len, cy - d[1] * len, z}, new double[] {cx + nx * 0.018, cy + ny * 0.018, z},
                    new double[] {cx + d[0] * len, cy + d[1] * len, z}, new double[] {cx - nx * 0.012, cy - ny * 0.012, z}, ink, ink, ink, ink);
            }
        }
    }

    /** The One-Wing's crescent spine (cubic, unit scale): up from the shoulder, over, and out to a long point. */
    static double[] crescent(double t) {
        double[][] c = {{0.0, 0.0}, {-0.25, 0.70}, {0.55, 1.15}, {1.30, 0.30}};
        double u = 1.0 - t;
        double b0 = u * u * u;
        double b1 = 3 * u * u * t;
        double b2 = 3 * u * t * t;
        double b3 = t * t * t;
        return new double[] {b0 * c[0][0] + b1 * c[1][0] + b2 * c[2][0] + b3 * c[3][0], b0 * c[0][1] + b1 * c[1][1] + b2 * c[2][1] + b3 * c[3][1]};
    }

    static double[] crescentTangent(double t) {
        double[][] c = {{0.0, 0.0}, {-0.25, 0.70}, {0.55, 1.15}, {1.30, 0.30}};
        double u = 1.0 - t;
        double x = 3 * u * u * (c[1][0] - c[0][0]) + 6 * u * t * (c[2][0] - c[1][0]) + 3 * t * t * (c[3][0] - c[2][0]);
        double y = 3 * u * u * (c[1][1] - c[0][1]) + 6 * u * t * (c[2][1] - c[1][1]) + 3 * t * t * (c[3][1] - c[2][1]);
        double l = Math.max(1.0E-9, Math.sqrt(x * x + y * y));
        return new double[] {x / l, y / l};
    }


    static int oneWingColor(double t, int station, boolean top) {
        int base = mixInt(BLADE_RED, BLADE_DEEP, 0.25 + 0.35 * t);
        if (top) {
            base = mixInt(base, BLADE_LIGHT, station % 5 == 0 ? 0.45 : 0.2);
        } else {
            base = mixInt(base, BLADE_DEEP, 0.5);
        }
        return base;
    }
}
