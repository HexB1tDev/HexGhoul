package com.hex.kagune.control;

import com.hex.kagune.control.client.KakuganScreenLayout;
import com.hex.kagune.control.client.PathChoiceLayout.Box;
import com.hex.kagune.control.kakugan.KakuganEyes;
import com.hex.kagune.control.kakugan.KakuganModel;
import com.hex.kagune.control.kakugan.KakuganSkin;
import java.util.ArrayList;
import java.util.List;

/** Dependency-free tests for the kakugan: eye placement, the geometry on the skin, the wake rule and the placement screen. */
public final class KakuganTests {
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
        eyes();
        geometry();
        glasses();
        waking();
        screen();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Kakugan tests: " + passed + " checks passed");
    }

    /** Every placement a player could make (and many nonsense ones). */
    private static List<KakuganEyes> placements() {
        List<KakuganEyes> all = new ArrayList<>();
        for (int rx = -2; rx <= 9; rx++) {
            for (int lx = -2; lx <= 9; lx += 2) {
                for (int y = -1; y <= 8; y += 3) {
                    for (int h = 0; h <= 3; h++) {
                        all.add(new KakuganEyes(rx, y, lx, 7 - y, h, (rx + lx) % 2 == 0, (rx + y) % 3 == 0));
                    }
                }
            }
        }
        return all;
    }

    private static void eyes() {
        for (KakuganEyes raw : placements()) {
            KakuganEyes e = raw.validated();
            String at = " for " + raw;
            check(e.height() == 1 || e.height() == 2, "eyes are one or two pixels tall" + at);
            for (boolean right : new boolean[] {true, false}) {
                check(e.column(right) >= 0 && e.column(right) + KakuganEyes.WIDTH <= 8, "an eye never leaves the face sideways" + at);
                check(e.row(right) >= 0 && e.row(right) + e.height() <= 8, "an eye never leaves the face up or down" + at);
                check(e.irisColumn(right) != e.scleraColumn(right) && Math.abs(e.irisColumn(right) - e.scleraColumn(right)) == 1,
                    "each eye is one white and one iris pixel" + at);
            }
            check(e.rightX() + KakuganEyes.WIDTH <= e.leftX(), "the eyes never overlap" + at);
            check(KakuganEyes.unpack(e.pack()).equals(e), "a placement survives the save and the network" + at);
            check(e.validated().equals(e), "validating twice changes nothing" + at);
            KakuganEyes mirrored = e.withRight(e.rightX(), e.rightY(), true);
            check(mirrored.mirrored(), "mirroring makes the eyes symmetrical" + at);
            check(e.withLeft(e.leftX(), e.leftY(), true).mirrored(), "mirroring from the left eye too" + at);
            check(KakuganEyes.unpack((e.pack() & 0x0FFFFFFF) | 1 << 28).equals(e), "a placement saved before 3.1.0 still reads (both eyes)" + at);
            for (int ghoul = KakuganEyes.BOTH; ghoul <= KakuganEyes.LEFT; ghoul++) {
                KakuganEyes one = e.withGhoul(ghoul);
                String which = at + " ghoul " + ghoul;
                check(one.ghoul() == ghoul && KakuganEyes.unpack(one.pack()).equals(one), "the ghoul eye survives the save and the network" + which);
                check(one.withRight(one.rightX(), one.rightY(), true).ghoul() == ghoul && one.withLeft(one.leftX(), one.leftY(), false).ghoul() == ghoul
                    && one.withHeight(3 - one.height()).ghoul() == ghoul && one.withIrisInside(!one.irisInside()).ghoul() == ghoul,
                    "moving the eyes keeps the ghoul eye" + which);
                check(one.oneEyed() == (ghoul != KakuganEyes.BOTH) && (one.ghoulEye(true) || one.ghoulEye(false))
                    && one.ghoulEye(true) == (ghoul != KakuganEyes.LEFT) && one.ghoulEye(false) == (ghoul != KakuganEyes.RIGHT),
                    "one eyed turns exactly the eye picked" + which);
            }
        }
        check(KakuganEyes.DEFAULT.ghoul() == KakuganEyes.BOTH && KakuganEyes.DEFAULT.withGhoul(9).ghoul() == KakuganEyes.LEFT
            && KakuganEyes.DEFAULT.withGhoul(-3).ghoul() == KakuganEyes.BOTH, "both eyes by default, and nonsense is clamped");
        KakuganEyes d = KakuganEyes.DEFAULT;
        check(d.rightX() == 1 && d.leftX() == 5 && d.rightY() == 4 && d.height() == 2 && d.irisInside() && d.mirrored(),
            "the default is Steve's eyes (two by two on rows 4 and 5, irises toward the nose)");
        check(d.irisColumn(true) == 2 && d.scleraColumn(true) == 1 && d.irisColumn(false) == 5 && d.scleraColumn(false) == 6,
            "irises sit toward the nose by default");
        check(KakuganEyes.unpack(0).equals(d) && KakuganEyes.unpack(-1).equals(d) && KakuganEyes.unpack(0x7FFFFFFF).equals(d),
            "a corrupt saved value falls back to the default");
        check(d.withRight(0, 4, true).leftX() == 6 && d.withRight(6, 4, true).rightX() == 2, "mirrored eyes stop at the middle");
        check(!d.withRight(0, 3, false).mirrored() && d.withRight(0, 3, false).leftX() == 5, "unmirrored, the other eye stays put");
    }

    private static boolean inside(float[] xyz, int corner, double u0, double v0, double u1, double v1) {
        double u = xyz[corner * 3] + 4.0;
        double v = xyz[corner * 3 + 1] + 8.0;
        return u > u0 + 1.0E-4 && u < u1 - 1.0E-4 && v > v0 + 1.0E-4 && v < v1 - 1.0E-4;
    }

    /** True if face point (u, v) is inside a (convex) quad. */
    private static boolean covers(KakuganModel.Quad quad, double u, double v) {
        float[] p = quad.xyz();
        int sign = 0;
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            double ax = p[i * 3] + 4.0;
            double ay = p[i * 3 + 1] + 8.0;
            double bx = p[j * 3] + 4.0;
            double by = p[j * 3 + 1] + 8.0;
            double cross = (bx - ax) * (v - ay) - (by - ay) * (u - ax);
            if (Math.abs(cross) < 1.0E-12) {
                continue;
            }
            int s = cross > 0 ? 1 : -1;
            if (sign == 0) {
                sign = s;
            } else if (s != sign) {
                return false;
            }
        }
        return sign != 0;
    }

    private static void geometry() {
        int[] seeds = {0, 1, -7, 1234567, Integer.MIN_VALUE};
        List<KakuganEyes> samples = new ArrayList<>();
        for (KakuganEyes e : placements()) {
            if (samples.stream().noneMatch(e.validated()::equals)) {
                samples.add(e.validated());
            }
        }
        double layer = KakuganModel.layerFor(16.0);
        for (KakuganEyes e : samples) {
            for (int seed : seeds) {
                String at = " for " + e + " seed " + seed;
                List<KakuganModel.Quad> quads = KakuganModel.build(e, seed, 3.7, 1.0F, true, layer);
                check(!quads.isEmpty(), "a turned kakugan draws something" + at);
                boolean onFace = true;
                boolean onSkin = true;
                boolean finite = true;
                for (KakuganModel.Quad q : quads) {
                    float[] p = q.xyz();
                    for (int k = 0; k < 4; k++) {
                        finite &= Float.isFinite(p[k * 3]) && Float.isFinite(p[k * 3 + 1]) && Float.isFinite(p[k * 3 + 2]);
                        onFace &= p[k * 3] >= -4.0001F && p[k * 3] <= 4.0001F && p[k * 3 + 1] >= -8.0001F && p[k * 3 + 1] <= 0.0001F;
                        // Lies on the inner layer: never further off the face than a few depth steps (the hat layer is 0.5 px out).
                        onSkin &= p[k * 3 + 2] <= -4.0F && p[k * 3 + 2] >= -4.0F - 6.0F * layer - 1.0E-5F;
                    }
                    check(q.nz() == -1.0F && q.nx() == 0.0F && q.ny() == 0.0F, "everything lies flat on the face" + at);
                }
                check(finite, "all corners are finite" + at);
                if (seed == seeds[0] || seed == seeds[3]) {
                    oneEyed(e, seed, layer, at);
                }
                check(onFace, "the kakugan stays on the face (only the eyes and the skin around them change)" + at);
                check(onSkin, "the kakugan lies on the skin, not floating over it" + at);
                for (boolean right : new boolean[] {true, false}) {
                    int sclera = e.scleraColumn(right);
                    int top = e.row(right);
                    // The white turns pure black: a fully opaque black quad covers it...
                    boolean black = quads.stream().anyMatch(q -> !q.glow() && q.argb()[0] == 0xFF000000 && covers(q, sclera + 0.5, top + e.height() / 2.0));
                    check(black, "the white of the eye turns pure black" + at);
                    // ...and no glow spills over it, except the thin cracks.
                    int samplesIn = 0;
                    int glowing = 0;
                    for (double u = sclera + 0.05; u < sclera + 1.0; u += 0.1) {
                        for (double v = top + 0.05; v < top + e.height(); v += 0.1) {
                            samplesIn++;
                            double su = u;
                            double sv = v;
                            if (quads.stream().anyMatch(q -> q.glow() && covers(q, su, sv))) {
                                glowing++;
                            }
                        }
                    }
                    check(glowing < samplesIn * 0.2, "the black stays black: only thin cracks glow over it (" + glowing + "/" + samplesIn + ")" + at);
                    // The iris turns red.
                    int iris = e.irisColumn(right);
                    check(quads.stream().anyMatch(q -> !q.glow() && (q.argb()[0] & 0xFFFFFF) == 0xC8101C && covers(q, iris + 0.5, top + e.height() / 2.0)),
                        "the iris turns red" + at);
                    // Veins and cracks never cross into the other eye.
                    int ox = e.column(!right);
                    int oy = e.row(!right);
                    boolean clear = true;
                    for (KakuganModel.Quad q : quads) {
                        boolean solidVein = !q.glow() && (q.argb()[0] & 0xFFFFFF) != 0x000000 && (q.argb()[0] & 0xFFFFFF) != 0xC8101C
                            && (q.argb()[0] & 0xFFFFFF) != 0x4C0008 && (q.argb()[0] & 0xFFFFFF) != 0x2C0612 // the socket lies under the eyes
                            && (q.argb()[0] & 0xFFFFFF) != 0xFF2A24; // cracks belong inside their own eye's black (5.0.12: brighter red)
                        if (solidVein) {
                            for (int k = 0; k < 4; k++) {
                                if (inside(q.xyz(), k, ox + 0.1, oy + 0.1, ox + 1.9, oy + e.height() - 0.1)) {
                                    clear = false;
                                }
                            }
                        }
                    }
                    check(clear, "veins never cross the other eye" + at);
                }
            }
            veinsGoOutward(KakuganModel.build(e, 5, 2.0, 1.0F, true, layer), e, " for " + e);
            List<KakuganModel.Quad> a = KakuganModel.build(e, 5, 2.0, 1.0F, true, layer);
            List<KakuganModel.Quad> b = KakuganModel.build(e, 5, 2.0, 1.0F, true, layer);
            check(a.size() == b.size(), "the same frame builds the same kakugan");
        }
        KakuganEyes d = KakuganEyes.DEFAULT;
        check(KakuganModel.build(d, 1, 1.0, 0.0F, true, 0.003).isEmpty(), "human eyes draw nothing");
        check(KakuganModel.build(d, 1, 1.0, 1.0F, true, 0.003).size() != KakuganModel.build(d, 2, 1.0, 1.0F, true, 0.003).size()
            || !sameCorners(KakuganModel.build(d, 1, 1.0, 1.0F, true, 0.003), KakuganModel.build(d, 2, 1.0, 1.0F, true, 0.003)),
            "every player's veins have their own pattern");
        check(KakuganModel.build(d, 1, 1.0, 1.0F, false, 0.003).size() < KakuganModel.build(d, 1, 1.0, 1.0F, true, 0.003).size(),
            "far away, the finest detail is skipped");
        check(KakuganModel.build(d, 1, 1.0, 0.3F, true, 0.003).size() < KakuganModel.build(d, 1, 1.0, 1.0F, true, 0.003).size(),
            "the veins grow out as the eyes wake");
        check(KakuganModel.build(d, 1, 1.0, 1.0F, true, 0.003).size() < 1200, "a kakugan stays cheap to draw");
        check(KakuganModel.layerFor(0.0) > 0.0 && KakuganModel.layerFor(16.0) < 0.01 && KakuganModel.layerFor(1.0E6) <= 0.06,
            "the depth step is a hair up close and never more than a fraction of a pixel");
    }

    private static boolean sameCorners(List<KakuganModel.Quad> a, List<KakuganModel.Quad> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!java.util.Arrays.equals(a.get(i).xyz(), b.get(i).xyz())) {
                return false;
            }
        }
        return true;
    }

    /** The user's rule: veins leave each eye from its outer side only; none gather under the irises or between the eyes. */
    /**
     * One eyed: each eye alone draws everything it draws with both (on the skin and on the outer layer) plus more cracks and
     * veins (6.0.7: the one kakugan is the stronger one), and nothing on the human eye.
     */
    private static void oneEyed(KakuganEyes e, int seed, double layer, String at) {
        for (long covered : new long[] {0L, -1L}) {
            List<String> both = keys(KakuganModel.build(e, seed, 3.7, 1.0F, true, layer, covered));
            List<String> right = keys(KakuganModel.build(e.withGhoul(KakuganEyes.RIGHT), seed, 3.7, 1.0F, true, layer, covered));
            List<String> left = keys(KakuganModel.build(e.withGhoul(KakuganEyes.LEFT), seed, 3.7, 1.0F, true, layer, covered));
            List<String> bothRight = new ArrayList<>(both);
            bothRight.retainAll(right);
            List<String> bothLeft = new ArrayList<>(both);
            bothLeft.retainAll(left);
            String where = at + (covered != 0L ? " (outer layer)" : "");
            check(!bothRight.isEmpty() && !bothLeft.isEmpty() && bothRight.size() + bothLeft.size() == both.size(),
                "one eyed still draws all of that eye as it would with both, and nothing of the other" + where);
            check(right.size() > bothRight.size() && left.size() > bothLeft.size(), "one eyed draws more cracks and veins than with both" + where);
        }
    }

    private static List<String> keys(List<KakuganModel.Quad> quads) {
        List<String> out = new ArrayList<>();
        for (KakuganModel.Quad q : quads) {
            out.add(java.util.Arrays.toString(q.xyz()) + java.util.Arrays.toString(q.argb()) + q.glow() + q.outer());
        }
        return out;
    }

    private static void veinsGoOutward(List<KakuganModel.Quad> quads, KakuganEyes e, String at) {
        for (boolean right : new boolean[] {true, false}) {
            int iris = e.irisColumn(right);
            int top = e.row(right);
            double between = right ? iris + 1.0 : iris; // the iris's edge toward the nose
            int start = 0;
            int under = 0;
            for (KakuganModel.Quad q : quads) {
                int rgb = q.argb()[0] & 0xFFFFFF;
                if (q.glow() || rgb != 0x8A0A18) {
                    continue; // vein cores only
                }
                float[] p = q.xyz();
                double u = (p[0] + p[3] + p[6] + p[9]) / 4.0 + 4.0;
                double v = (p[1] + p[4] + p[7] + p[10]) / 4.0 + 8.0;
                boolean near = Math.abs(v - (top + e.height())) < 1.2 && Math.abs(u - (e.column(right) + 1.0)) < 1.6;
                if (near) {
                    start++;
                    boolean inward = right ? u > between - 0.15 : u < between + 0.15;
                    if (inward && v > top + e.height()) {
                        under++;
                    }
                }
            }
            check(under <= start * 0.12, "veins don't gather under the iris toward the nose (" + under + "/" + start + ")" + at);
        }
    }

    /**
     * 3.0.0: a second layer over the eyes is never cut away. Opaque pixels over an eye (eyes painted on that layer) get the
     * kakugan drawn on them, pixel for pixel on the layer's own grid, and nothing else does; glass is drawn over the kakugan.
     */
    private static void glasses() {
        check(KakuganModel.build(KakuganEyes.DEFAULT, 3, 1.0, 1.0F, true, 0.003).stream().noneMatch(KakuganModel.Quad::outer),
            "with nothing covering the eyes, nothing is drawn on the second layer");
        for (KakuganEyes raw : placements()) {
            KakuganEyes e = raw.validated();
            String at = " for " + e;
            long all = -1L;
            List<KakuganModel.Quad> quads = KakuganModel.build(e, 3, 1.0, 1.0F, true, 0.003, all);
            List<KakuganModel.Quad> outer = quads.stream().filter(KakuganModel.Quad::outer).toList();
            check(!outer.isEmpty(), "a covered eye is drawn on the second layer" + at);
            for (KakuganModel.Quad q : outer) {
                float[] v = q.xyz();
                for (int k = 0; k < 4; k++) {
                    // Back to face cells through the second layer's 9/8.
                    double u = v[k * 3] / KakuganModel.OUTER_SCALE + 4.0;
                    double w = (v[k * 3 + 1] + 4.0) / KakuganModel.OUTER_SCALE + 4.0;
                    boolean onEye = false;
                    for (boolean right : new boolean[] {true, false}) {
                        onEye |= u >= e.column(right) - 1.0E-4 && u <= e.column(right) + 2 + 1.0E-4 && w >= e.row(right) - 1.0E-4
                            && w <= e.row(right) + e.height() + 1.0E-4;
                    }
                    check(onEye, "the second layer only gets the eyes, never the veins" + at);
                    check(v[k * 3 + 2] < -4.5F && v[k * 3 + 2] > -4.6F, "copies lie just over the second layer (z " + v[k * 3 + 2] + ")" + at);
                }
            }
            // Only the covered pixels are copied: cover just the right eye's iris.
            for (int row = e.row(true); row < e.row(true) + e.height(); row++) {
                long iris = KakuganSkin.bit(e.irisColumn(true), row);
                List<KakuganModel.Quad> one = KakuganModel.build(e, 3, 1.0, 1.0F, true, 0.003, iris).stream().filter(KakuganModel.Quad::outer).toList();
                boolean inside = !one.isEmpty();
                for (KakuganModel.Quad q : one) {
                    float[] v = q.xyz();
                    for (int k = 0; k < 4; k++) {
                        double u = v[k * 3] / KakuganModel.OUTER_SCALE + 4.0;
                        double w = (v[k * 3 + 1] + 4.0) / KakuganModel.OUTER_SCALE + 4.0;
                        inside &= u >= e.irisColumn(true) - 1.0E-4 && u <= e.irisColumn(true) + 1 + 1.0E-4 && w >= row - 1.0E-4 && w <= row + 1 + 1.0E-4;
                    }
                }
                check(inside, "a single covered pixel gets exactly that pixel copied" + at);
            }
        }
        skins();
    }

    /** Reading a skin: the second layer's cover, and the eyes found on stand-in skins wherever they are. */
    private static void skins() {
        int[] skin = new int[64 * 64];
        int[][] face = {
            {0x4A3A30, 0x6E5444, 0x4A3A30, 0x6E5444, 0x6E5444, 0x4A3A30, 0x6E5444, 0x4A3A30},
            {0x56463A, 0x87695A, 0x56463A, 0x87695A, 0x87695A, 0x56463A, 0x87695A, 0x56463A},
            {0x56463A, 0x9A7A64, 0x56463A, 0x93745F, 0x93745F, 0x56463A, 0x9A7A64, 0x56463A},
            {0x56463A, 0x28221A, 0x2E2820, 0xDCBC8C, 0xDCBC8C, 0x2E2820, 0x28221A, 0x56463A},
            {0x2A241C, 0xFAD8FA, 0xA050BE, 0xE6C49A, 0xEEC898, 0xA050BE, 0xFAD8FA, 0x2A241C},
            {0x2E2820, 0xFFFFFF, 0xE070FA, 0xE8C69C, 0xF0CC9C, 0xE070FA, 0xFFFFFF, 0x2E2820},
            {0x4E3E32, 0xE8BC90, 0xEAC496, 0xE6C49A, 0xE6C49A, 0xEAC496, 0xE8BC90, 0x4E3E32},
            {0x4E3E32, 0xE2B48A, 0xE4BC90, 0xE2BC92, 0xE2BC92, 0xE4BC90, 0xE2B48A, 0x4E3E32}};
        check(!KakuganSkin.readable(skin, 64), "a blank read (a driver that cannot read textures back) is not trusted");
        int[] scrambled = new int[64 * 64];
        java.util.Random noise = new java.util.Random(7);
        for (int i = 0; i < scrambled.length; i++) {
            scrambled[i] = noise.nextInt();
        }
        check(!KakuganSkin.readable(scrambled, 64), "nor is a scrambled one");
        paint(skin, 8, face, 0xFF);
        check(KakuganSkin.readable(skin, 64), "a real skin (opaque face) is read");
        check(KakuganSkin.covered(skin, 64) == 0L, "an empty second layer covers nothing");
        KakuganEyes found = KakuganSkin.detect(skin, 64);
        check(found != null && found.rightX() == 1 && found.rightY() == 4 && found.leftX() == 5 && found.leftY() == 4 && found.height() == 2
            && found.irisInside(), "finds the stand-in skin's eyes (rows 4-5, white outside, violet iris inside): " + found);

        // The same face moved: eyes a row higher, wider apart, one pixel tall, irises outside.
        int skinTone = 0xD8B08A;
        int[][] moved = new int[8][8];
        for (int[] row : moved) {
            java.util.Arrays.fill(row, skinTone);
        }
        java.util.Arrays.fill(moved[0], 0x2A1E16);
        java.util.Arrays.fill(moved[1], 0x2A1E16);
        moved[3][0] = 0x3A6AD0;
        moved[3][1] = 0xF4F4F4;
        moved[3][6] = 0xF4F4F4;
        moved[3][7] = 0x3A6AD0;
        int[] other = new int[64 * 64];
        paint(other, 8, moved, 0xFF);
        KakuganEyes wide = KakuganSkin.detect(other, 64);
        check(wide != null && wide.rightX() == 0 && wide.rightY() == 3 && wide.leftX() == 6 && wide.height() == 1 && !wide.irisInside(),
            "finds eyes set high and wide, one pixel tall, irises outside: " + wide);

        // Eyes painted on the second layer over a blank face: found there, and those cells read as covered.
        int[] layered = new int[64 * 64];
        int[][] blank = new int[8][8];
        for (int[] row : blank) {
            java.util.Arrays.fill(row, skinTone);
        }
        paint(layered, 8, blank, 0xFF);
        int[][] hat = new int[8][8];
        hat[4][1] = 0xFFFFFF;
        hat[4][2] = 0x5A2A10;
        hat[4][5] = 0x5A2A10;
        hat[4][6] = 0xFFFFFF;
        for (int u : new int[] {1, 2, 5, 6}) {
            layered[(8 + 4) * 64 + 40 + u] = 0xFF000000 | hat[4][u];
        }
        long cover = KakuganSkin.covered(layered, 64);
        check(cover == (KakuganSkin.bit(1, 4) | KakuganSkin.bit(2, 4) | KakuganSkin.bit(5, 4) | KakuganSkin.bit(6, 4)),
            "the second layer's opaque pixels read as covered, its empty ones not");
        KakuganEyes onHat = KakuganSkin.detect(layered, 64);
        check(onHat != null && onHat.rightX() == 1 && onHat.rightY() == 4 && onHat.height() == 1, "finds eyes painted on the second layer: " + onHat);
        // A faint tint (alpha 30) does not cover.
        layered[(8 + 6) * 64 + 40 + 3] = 30 << 24 | 0x202020;
        check(KakuganSkin.covered(layered, 64) == cover, "a faint second-layer tint covers nothing");

        // The user's own skin (shared for testing): eyes on rows 5-6 under glasses on the second layer, see-through grey lenses.
        int[][] userFace = {
            {0x191511, 0x1F1A15, 0x241E18, 0x191511, 0x191511, 0x1F1A14, 0x1C1813, 0x1C1813},
            {0x191511, 0x1F1A15, 0x251E18, 0x241E18, 0x1F1A15, 0x1F1A15, 0x241E18, 0x1C1813},
            {0x241E18, 0x241E18, 0x241E18, 0x1F1A15, 0x1F1A15, 0x1F1A15, 0x241E18, 0x241E18},
            {0x806A57, 0x241E18, 0x241E19, 0x1F1A15, 0x1F1A15, 0x1F1A15, 0x241E19, 0x806A56},
            {0x241E18, 0x1F1A15, 0x1F1A15, 0xF9C192, 0xF9C192, 0x1F1A15, 0x1F1A15, 0x241E18},
            {0x806A57, 0xBAC1C1, 0x191919, 0xFFD0AA, 0xFFD0AA, 0x161616, 0xBAC1C1, 0x806A57},
            {0x806A57, 0xE6E6E6, 0x1A1A1A, 0xE5A885, 0xE5A885, 0x050505, 0xE6E6E6, 0x806A57},
            {0xE5A885, 0xF8C093, 0xFFD0AA, 0xFFD0AA, 0xFFD0AA, 0xFFD0AA, 0xF9C193, 0xE5A885}};
        int[][] userHat = {
            {0, 0, 0xFF2E261F, 0xFF2E261F, 0xFF2E261F, 0xFF2E261F, 0xFF26201A, 0},
            {0, 0xFF29221C, 0xFF29231C, 0xFF2E261E, 0xFF26201A, 0xFF26201A, 0xFF27201A, 0},
            {0xFF302921, 0xFF29221C, 0xFF29221C, 0xFF2E261F, 0xFF26201A, 0xFF26201A, 0xFF26201A, 0},
            {0xFF312921, 0xFF302921, 0xFF29221C, 0xFF26201A, 0xFF26201A, 0, 0xFF26201A, 0},
            {0xFF3D342A, 0xFF000000, 0xFF000100, 0xFF26201A, 0, 0xFF000000, 0xFF000000, 0},
            {0xFF000000, 0xDFC1C1C1, 0xDFC1C1C1, 0xFF000000, 0xFF000000, 0xDFC1C1C1, 0xDFC1C1C1, 0xFF000000},
            {0, 0xDFC1C1C1, 0xDFC1C1C1, 0, 0, 0xDFC1C1C1, 0xDFC1C1C1, 0},
            {0, 0, 0, 0, 0, 0, 0, 0}};
        int[] user = new int[64 * 64];
        paint(user, 8, userFace, 0xFF);
        for (int v = 0; v < 8; v++) {
            for (int u = 0; u < 8; u++) {
                user[(8 + v) * 64 + 40 + u] = userHat[v][u];
            }
        }
        KakuganEyes userEyes = KakuganSkin.detect(user, 64);
        check(userEyes != null && userEyes.rightX() == 1 && userEyes.rightY() == 5 && userEyes.leftX() == 5 && userEyes.leftY() == 5
            && userEyes.height() == 2 && userEyes.irisInside(), "finds the user's eyes under their glasses: " + userEyes);
        long userCover = KakuganSkin.covered(user, 64);
        long userGlass = KakuganSkin.glass(user, 64);
        for (int row = 5; row <= 6; row++) {
            for (int column : new int[] {1, 2, 5, 6}) {
                check((userCover & KakuganSkin.bit(column, row)) != 0L && (userGlass & KakuganSkin.bit(column, row)) != 0L,
                    "the user's lenses cover their eyes and are glass (" + column + ", " + row + ")");
            }
        }
        check((userGlass & KakuganSkin.bit(0, 5)) == 0L && (userCover & KakuganSkin.bit(0, 5)) != 0L, "the frame is solid, not glass");
        // Glass is never painted on: the kakugan stays on the face and the lenses are drawn over it (no second pair of eyes).
        check(KakuganModel.build(userEyes, 7, 1.0, 1.0F, true, 0.003, userCover & ~userGlass).stream().noneMatch(KakuganModel.Quad::outer),
            "nothing is drawn on the user's see-through lenses");
        check((userGlass & KakuganSkin.eyeCells(userEyes)) != 0L, "the user's lenses lie over their eyes (drawn after the kakugan)");
        check(KakuganSkin.eyeCells(KakuganEyes.DEFAULT) == (KakuganSkin.bit(1, 4) | KakuganSkin.bit(2, 4) | KakuganSkin.bit(1, 5) | KakuganSkin.bit(2, 5)
            | KakuganSkin.bit(5, 4) | KakuganSkin.bit(6, 4) | KakuganSkin.bit(5, 5) | KakuganSkin.bit(6, 5)), "Steve's eye cells");

        // A blank face has no eyes to find; an HD skin (128 wide) reads the same as its 64-pixel layout.
        int[] empty = new int[64 * 64];
        paint(empty, 8, blank, 0xFF);
        check(KakuganSkin.detect(empty, 64) == null, "a face with no eyes finds none");
        int[] hd = new int[128 * 128];
        for (int v = 0; v < 64; v++) {
            for (int u = 0; u < 64; u++) {
                for (int k = 0; k < 4; k++) {
                    hd[(v * 2 + k / 2) * 128 + u * 2 + k % 2] = skin[v * 64 + u];
                }
            }
        }
        check(java.util.Objects.equals(KakuganSkin.detect(hd, 128), found), "an HD skin reads like its 64-pixel layout");
    }

    /** Paints an 8x8 face into a skin at the inner face (texture square 8,8). */
    private static void paint(int[] skin, int origin, int[][] face, int alpha) {
        for (int v = 0; v < 8; v++) {
            for (int u = 0; u < 8; u++) {
                skin[(origin + v) * 64 + origin + u] = alpha << 24 | face[v][u];
            }
        }
    }

    private static void waking() {
        check(KakuganModel.awake(true, 0.0F) == 1.0F, "the kagune out turns the eyes fully");
        check(KakuganModel.awake(false, 0.0F) == 0.0F && KakuganModel.awake(false, KakuganModel.FLARE_FROM) == 0.0F, "a calm ghoul has human eyes");
        check(KakuganModel.awake(false, 1.0F) == 1.0F && KakuganModel.awake(false, KakuganModel.FLARE_FULL) == 1.0F,
            "a starving or frenzied ghoul's eyes turn on their own");
        float previous = 0.0F;
        for (float b = 0.0F; b <= 1.0F; b += 0.01F) {
            float a = KakuganModel.awake(false, b);
            check(a >= previous - 1.0E-6F && a >= 0.0F && a <= 1.0F, "bloodlust turns the eyes gradually");
            previous = a;
        }
    }

    private static void screen() {
        for (int width = 320; width <= 3840; width += width < 700 ? 7 : 61) {
            for (int height = 240; height <= 2160; height += height < 500 ? 5 : 53) {
                String at = " at " + width + "x" + height;
                KakuganScreenLayout layout = KakuganScreenLayout.of(width, height);
                Box screen = new Box(0, 0, width, height);
                Box header = new Box(0, layout.titleY, width, layout.subtitleY + layout.subtitleLines * KakuganScreenLayout.LINE - layout.titleY);
                Box[] boxes = {layout.face, layout.mirror, layout.eyeHeight, layout.iris, layout.oneEye, layout.outer, layout.reset, layout.done,
                    layout.later};
                for (int i = 0; i < boxes.length; i++) {
                    check(boxes[i].inside(screen), "everything is on screen" + at + ": " + boxes[i]);
                    check(!boxes[i].overlaps(header), "nothing covers the title" + at);
                    for (int j = i + 1; j < boxes.length; j++) {
                        check(!boxes[i].overlaps(boxes[j]), "nothing overlaps" + at + ": " + boxes[i] + " " + boxes[j]);
                    }
                }
                check(layout.choice.inside(screen), "the which-eye box is on screen" + at);
                check(!layout.choice.overlaps(layout.face) && layout.choice.width() >= 130, "the which-eye box leaves the face in sight" + at
                    + ": " + layout.choice + " " + layout.face);
                Box[] picks = {layout.choiceRight, layout.choiceLeft, layout.choiceBoth};
                for (int i = 0; i < picks.length; i++) {
                    check(picks[i].inside(layout.choice) && picks[i].width() >= 40, "its buttons are in it and wide enough to read" + at);
                    for (int j = i + 1; j < picks.length; j++) {
                        check(!picks[i].overlaps(picks[j]), "its buttons don't overlap" + at);
                    }
                }
                check(layout.choiceRight.x() < layout.choiceLeft.x(), "the right eye's button is on the side the right eye is drawn" + at);
                check(layout.cell >= KakuganScreenLayout.MIN_CELL && layout.face.width() == 8 * layout.cell && layout.face.height() == 8 * layout.cell,
                    "the face is eight skin pixels square" + at);
                check(layout.hintY < 0 || layout.hintY >= layout.done.bottom(), "the hint sits under the buttons" + at);
                int[] corner = layout.cellAt(layout.face.x(), layout.face.y());
                int[] far = layout.cellAt(layout.face.right() - 1, layout.face.bottom() - 1);
                check(corner != null && corner[0] == 0 && corner[1] == 0 && far != null && far[0] == 7 && far[1] == 7, "pixels under the mouse" + at);
                check(layout.cellAt(layout.face.x() - 1, layout.face.y()) == null, "outside the face is no pixel" + at);
                Box right = layout.eye(KakuganEyes.DEFAULT, true);
                check(right.x() == layout.face.x() + layout.cell && right.width() == 2 * layout.cell, "eyes are drawn where they are" + at);
            }
        }
        check(KakuganScreenLayout.of(480, 270).cell >= 12, "1080p at GUI scale 4 shows the face big enough to place pixels");
    }
}
