import com.hex.kagune.control.KaguneTint;
import com.hex.kagune.control.RinkakuRibbon;
import com.hex.kagune.control.RinkakuRoot;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.ReaperBuild;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 2.0.46: the Rinkaku kagune as it is now, off-game, on a man (his build, 2.0.46): the body through {@link PoseRender}'s exact transforms in
 * a real skin, the emergence mark on his back ({@code rinkaku_mark.png} and its glow, tinted as {@code RinkakuMarkLayer} tints them) and
 * four tails grown out of it ({@code RinkakuRoot.attach}) as {@code RinkakuTube} draws them: {@code RinkakuRibbon}'s mesh shaded from the
 * camera, its skin with the scales baked in ({@code rinkaku_skin.png}, 2.0.51) and its glowing veins, in the kagune's colour. 2.0.51: the
 * tails at rest as the game draws them now ({@code RinkakuIdle}), alive ({@code RinkakuRibbon.stretch}, {@code swell}) at {@link #time}. Was: a pose made up
 * for the picture (the game's come from the original's motion).
 *
 * <p>Usage: {@code java -cp <classes> KaguneShowcase <skin.png> <textures/misc dir> <out.png> [sheet|build]}.
 */
public final class KaguneShowcase {
    static BufferedImage scales;
    static BufferedImage mark;
    static BufferedImage markGlow;

    record Shot(String label, int colour, double yaw, double pitch, double distance, double target, boolean tails, boolean build) {
    }

    public static void main(String[] args) throws Exception {
        PoseRender.skin = ImageIO.read(new File(args[0]));
        PoseRender.slim = false;
        File misc = new File(args[1]);
        scales = ImageIO.read(new File(misc, "rinkaku_skin.png"));
        mark = ImageIO.read(new File(misc, "rinkaku_mark.png"));
        markGlow = ImageIO.read(new File(misc, "rinkaku_mark_glow.png"));
        String mode = args.length > 3 ? args[3] : "sheet";
        if (mode.equals("frames")) {
            // 2.0.51: frames of it alive (every other tick through a whole sway), for a GIF: <out> is a directory.
            File dir = new File(args[2]);
            dir.mkdirs();
            Shot shot = new Shot("", 0xA50F37, 145, 10, 8.2, 1.8, true, true);
            for (int f = 0; f < 42; f++) {
                time = f * 2.0;
                ImageIO.write(render(shot, 420, 460), "png", new File(dir, String.format("f%03d.png", f)));
            }
            System.out.println("wrote 42 frames to " + dir);
            return;
        }
        List<Shot> shots = mode.equals("build") ? List.of(
            new Shot("before (as every player was), front", 0, 0, 4, 4.2, 1.0, false, false),
            new Shot("a man now (the White Reaper's build), front", 0, 0, 4, 4.2, 1.0, false, true),
            new Shot("before, three-quarter", 0, 35, 8, 4.2, 1.0, false, false),
            new Shot("a man now, three-quarter", 0, 35, 8, 4.2, 1.0, false, true),
            new Shot("before, side", 0, -90, 2, 4.2, 1.0, false, false),
            new Shot("a man now, side", 0, -90, 2, 4.2, 1.0, false, true),
            new Shot("a man now, behind", 0, 160, 8, 4.2, 1.0, false, true),
            new Shot("a man now, close", 0, 20, 6, 2.6, 1.25, false, true))
            : List.of(
            new Shot("crimson (default), from behind", 0xA50F37, 180, 14, 8.4, 1.8, true, true),
            new Shot("crimson, three-quarter behind", 0xA50F37, 140, 10, 8.4, 1.8, true, true),
            new Shot("crimson, from the side", 0xA50F37, 90, 6, 8.4, 1.8, true, true),
            new Shot("crimson, from the front", 0xA50F37, 20, 6, 8.4, 1.8, true, true),
            new Shot("a tail up close: its scales and veins", 0xA50F37, 0, 0, 0, 0, true, true),
            new Shot("the mark as they tear out (tails hidden)", 0xA50F37, 180, 4, 2.1, 1.0, false, true),
            new Shot("blue", 0x1E50D8, 150, 12, 8.4, 1.8, true, true),
            new Shot("purple", 0x7A1FB8, 150, 12, 8.4, 1.8, true, true),
            new Shot("black (its veins glow blood red)", 0x141414, 150, 12, 8.4, 1.8, true, true),
            new Shot("white", 0xE8E8E8, 150, 12, 8.4, 1.8, true, true),
            new Shot("green", 0x1F9E4A, 150, 12, 8.4, 1.8, true, true),
            new Shot("crimson, at night (the veins and the mark glow)", -1, 150, 12, 8.4, 1.8, true, true));
        int w = 480;
        int h = 520;
        int cols = 4;
        int rows = (shots.size() + cols - 1) / cols;
        BufferedImage sheet = new BufferedImage(w * cols, h * rows, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        for (int i = 0; i < shots.size(); i++) {
            Shot s = shots.get(i);
            BufferedImage img = render(s, w, h);
            g.drawImage(img, (i % cols) * w, (i / cols) * h, w, h, null);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString(s.label, (i % cols) * w + 10, (i / cols) * h + 24);
        }
        g.dispose();
        File out = new File(args[2]);
        out.getAbsoluteFile().getParentFile().mkdirs();
        ImageIO.write(sheet, "png", out);
        System.out.println("wrote " + out);
    }

    static BufferedImage render(Shot s, int w, int h) {
        boolean night = s.colour == -1;
        int colour = night ? 0xA50F37 : s.colour;
        double dark = night ? 0.28 : 1.0;
        RigPose pose = new RigPose();
        // A ready stance: knees a little bent, arms a little out.
        pose.rightArmZ = 0.12F;
        pose.leftArmZ = -0.12F;
        pose.rightElbow = 0.25F;
        pose.leftElbow = 0.25F;
        pose.rightLegX = -0.1F;
        pose.leftLegX = 0.1F;
        PoseRender.armOut = s.build ? ReaperBuild.ARM_OUT : 0.0;
        closeUp = s.label.startsWith("a tail up close");
        // The close-up: square on the broad far end of the upper left tail, where its scales are biggest.
        PoseRender.Camera cam = closeUp
            ? new PoseRender.Camera(new double[] {1.7, 3.3, -2.3}, new double[] {1.0, 2.75, -0.7}, 30)
            : PoseRender.orbit(s.yaw, s.pitch, s.distance, s.target);
        PoseRender.Canvas canvas = new PoseRender.Canvas(w, h, 2, cam, night ? 0x10141C : 0x3A4450);
        canvas.floor(6, night ? 0x1E232B : 0x58626C, night ? 0x1A1F27 : 0x4E5862);
        List<PoseRender.Tri> body = PoseRender.model(pose);
        if (s.build) {
            ReaperPreview.chest(body, pose, false);
            ReaperPreview.chest(body, pose, true);
        }
        for (PoseRender.Tri t : body) {
            canvas.tri(night ? new PoseRender.Tri(t.v(), PoseRender.shade(t.rgb(), dark), t.n()) : t, true);
        }
        if (s.tails || s.label.startsWith("the mark")) {
            drawMark(canvas, pose, colour, s.build, dark);
        }
        if (s.tails) {
            for (int limb = 0; limb < 4; limb++) {
                drawTail(canvas, pose, limb, colour, cam, dark);
            }
        }
        return canvas.image();
    }

    // ------------------------------------------------------------------ the mark

    /** The mark on his back, as RinkakuMarkLayer lays it (over his build's back on a man), lit then glowing. */
    static void drawMark(PoseRender.Canvas canvas, RigPose pose, int colour, boolean build, double dark) {
        int red = KaguneTint.recolour(0xFFD0203A, colour);
        int glowing = KaguneTint.recolour(0xFFFF3A48, colour);
        int cuts = 64;
        for (int a = 0; a < cuts; a++) {
            for (int b = 0; b < cuts * 5 / 4; b++) {
                double u0 = a / (double)cuts, u1 = (a + 1) / (double)cuts;
                double v0 = b / (cuts * 5.0 / 4.0), v1 = (b + 1) / (cuts * 5.0 / 4.0);
                double um = (u0 + u1) / 2, vm = (v0 + v1) / 2;
                int lit = mark.getRGB((int)Math.min(mark.getWidth() - 1, um * mark.getWidth()), (int)Math.min(mark.getHeight() - 1, vm * mark.getHeight()));
                int glow = markGlow.getRGB((int)Math.min(markGlow.getWidth() - 1, um * markGlow.getWidth()),
                    (int)Math.min(markGlow.getHeight() - 1, vm * markGlow.getHeight()));
                double alpha = (lit >>> 24) / 255.0;
                int rgb = multiply(lit, red);
                rgb = PoseRender.shade(rgb, 0.75 * dark);
                int add = multiply(glow, glowing);
                if (alpha < 0.02 && (add & 0xFFFFFF) == 0) {
                    continue;
                }
                double[][] q = new double[4][];
                double[][] corners = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
                for (int k = 0; k < 4; k++) {
                    double x = 4.0 - 8.0 * corners[k][0];
                    double y = 2.0 + 10.0 * corners[k][1];
                    double z = build ? Math.max(2.32, ReaperBuild.back(y, true) + 0.07) : 2.32;
                    q[k] = PoseRender.point(pose, "torso", new double[] {x, y, z});
                }
                blendQuad(canvas, q, rgb, alpha, add);
            }
        }
    }

    // ------------------------------------------------------------------ the tails

    /** The time (ticks) the tails are drawn at: their sway, stretch, swell and veins' pulse. */
    static double time = 0.0;
    /** A tail's length as the game has it by default (the original's 8 + 0.0016 a mastery point, at 500, times the default appearance's
     *  length and the form's own: about 4.6 blocks). */
    static final double LENGTH = 4.6;

    /** Tail {@code limb}'s path (owner's frame: x right, y up, z forward, from his feet): at rest as the game draws it (RinkakuIdle), grown
     *  out of his back (RinkakuRoot.attach), at {@link #time}. */
    static double[][] path(int limb) {
        double[][] rest = com.hex.kagune.control.RinkakuIdle.pose(limb, 4, 0.0, 46, 1.8, LENGTH, time, 77 + limb * 131, 0.0, 0.0, 0.0);
        return RinkakuRoot.attach(rest, limb, 0.0, 1.8);
    }

    static boolean closeUp;

    static void drawTail(PoseRender.Canvas canvas, RigPose pose, int limb, int colour, PoseRender.Camera cam, double dark) {
        double[][] own = path(limb);
        // Into PoseRender's world (x to his left), riding his torso where his pose has carried it.
        double side = RinkakuRoot.side(limb, 0.0);
        double[] root = RinkakuRoot.root(limb, side, 1.8);
        double[] local = {-root[0] * 16.0, (1.5 - root[1]) * 16.0, -root[2] * 16.0};
        double[] posed = PoseRender.point(pose, "torso", local);
        double[] still = PoseRender.point(new RigPose(), "torso", local);
        double[][] world = new double[own.length][];
        for (int i = 0; i < own.length; i++) {
            world[i] = new double[] {-own[i][0] + posed[0] - still[0], own[i][1] + posed[1] - still[1], own[i][2] + posed[2] - still[2]};
        }
        int seed = limb * 131 + 7;
        // The game's own thickness: the original's 0.18 + 3.5e-5 a mastery point (here 500), times the default appearance scale (0.6), times
        // the Four-Tail form's own (0.94 the upper pair, 0.88 the lower).
        double thickness = (0.18 + 3.5E-5 * 500) * 0.6 * (limb < 2 ? 0.94 : 0.88);
        RinkakuRibbon.Mesh m = RinkakuRibbon.build(world, thickness, colour, seed, cam.eye(), time);
        if (m == null) {
            return;
        }
        int rings = m.at().length;
        int sides = RinkakuRibbon.SIDES;
        // Its one skin (2.0.51): the baked scales' texel times its colour (brightened back, RinkakuRibbon.lifted), lit as an entity is.
        int cuts = closeUp ? 16 : 6;
        for (int r = 0; r < rings - 1; r++) {
            for (int k = 0; k < sides; k++) {
                double ua = RinkakuRibbon.u(k), ub = RinkakuRibbon.u(k + 1);
                double va = RinkakuRibbon.v(m.along()[r]), vb = RinkakuRibbon.v(m.along()[r + 1]);
                double[][] q = {m.at()[r][k], m.at()[r + 1][k], m.at()[r + 1][k + 1], m.at()[r][k + 1]};
                int[] tints = {m.tint()[r][k], m.tint()[r + 1][k], m.tint()[r + 1][k + 1], m.tint()[r][k + 1]};
                double[] n = m.way()[r][k];
                for (int a = 0; a < cuts; a++) {
                    for (int b = 0; b < cuts; b++) {
                        double s0 = a / (double)cuts, s1 = (a + 1) / (double)cuts, t0 = b / (double)cuts, t1 = (b + 1) / (double)cuts;
                        double sm = (s0 + s1) / 2, tm = (t0 + t1) / 2;
                        double tu = ua + (ub - ua) * tm;
                        double tv = va + (vb - va) * sm;
                        int texel = scales.getRGB(Math.floorMod((int)Math.floor(tu * scales.getWidth()), scales.getWidth()),
                            Math.floorMod((int)Math.floor(tv * scales.getHeight()), scales.getHeight()));
                        int tint = mix4(tints, sm, tm);
                        int rgb = lit(0xFF000000 | multiply(texel, RinkakuRibbon.lifted(tint)), n, dark) & 0xFFFFFF;
                        double alpha = (tint >>> 24) / 255.0;
                        double[][] cell = {bilerp(q, s0, t0), bilerp(q, s1, t0), bilerp(q, s1, t1), bilerp(q, s0, t1)};
                        int c = (int)(alpha * 255) << 24 | rgb;
                        tri(canvas, cell[0], cell[1], cell[2], c, c, c, true, 1.0, 0, false);
                        tri(canvas, cell[0], cell[2], cell[3], c, c, c, true, 1.0, 0, false);
                    }
                }
            }
        }
        // Its veins, at full brightness (the world's light never dims them).
        for (RinkakuRibbon.Glow glow : RinkakuRibbon.veins(m, seed, time)) {
            double[][] p = glow.corners();
            int[] a = glow.argb();
            blendQuad(canvas, p, a[0] & 0xFFFFFF, (a[0] >>> 24) / 255.0, 0);
        }
    }

    /** The colour (ARGB) at ({@code s} along, {@code t} across) of a quad whose corners are {@code c} (in its corners' order). */
    static int mix4(int[] c, double s, double t) {
        int out = 0;
        for (int shift = 24; shift >= 0; shift -= 8) {
            double a = ((c[0] >>> shift) & 255) * (1 - s) + ((c[1] >>> shift) & 255) * s;
            double b = ((c[3] >>> shift) & 255) * (1 - s) + ((c[2] >>> shift) & 255) * s;
            out |= (int)Math.round(a + (b - a) * t) << shift;
        }
        return out;
    }

    // ------------------------------------------------------------------ drawing

    static int lit(int argb, double[] n, double dark) {
        double[] l0 = PoseRender.norm(new double[] {0.2, 1.0, -0.7});
        double[] l1 = PoseRender.norm(new double[] {-0.2, 1.0, 0.7});
        double light = Math.min(1.0, 0.45 + 0.45 * Math.max(0, PoseRender.dot(n, l0)) + 0.45 * Math.max(0, PoseRender.dot(n, l1)));
        return (argb & 0xFF000000) | PoseRender.shade(argb & 0xFFFFFF, light * dark);
    }

    static int multiply(int a, int b) {
        int r = ((a >> 16) & 255) * ((b >> 16) & 255) / 255;
        int g = ((a >> 8) & 255) * ((b >> 8) & 255) / 255;
        int bl = (a & 255) * (b & 255) / 255;
        return (r << 16) | (g << 8) | bl;
    }

    static double[] bilerp(double[][] q, double s, double t) {
        double[] a = lerp(q[0], q[1], s);
        double[] b = lerp(q[3], q[2], s);
        return lerp(a, b, t);
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    /** A quad with a colour per corner (alpha: how much of it over what is behind), depth tested and written. */
    static void smoothQuad(PoseRender.Canvas canvas, double[][] p, int[] c, boolean write) {
        tri(canvas, p[0], p[1], p[2], c[0], c[1], c[2], write, 1.0, 0, false);
        tri(canvas, p[0], p[2], p[3], c[0], c[2], c[3], write, 1.0, 0, false);
    }

    /** A flat quad laid over what is drawn ({@code alpha} of {@code rgb}, plus {@code add}), depth tested a hair loosely, not written. */
    static void blendQuad(PoseRender.Canvas canvas, double[][] p, int rgb, double alpha, int add) {
        int c = (int)(Math.max(0, Math.min(1, alpha)) * 255) << 24 | rgb;
        tri(canvas, p[0], p[1], p[2], c, c, c, false, 1.0, add, true);
        tri(canvas, p[0], p[2], p[3], c, c, c, false, 1.0, add, true);
    }

    static void tri(PoseRender.Canvas canvas, double[] pa, double[] pb, double[] pc, int ca, int cb, int cc, boolean write, double k, int add, boolean loose) {
        double[] a = canvas.project(pa), b = canvas.project(pb), c = canvas.project(pc);
        if (a == null || b == null || c == null) {
            return;
        }
        int minX = (int)Math.max(0, Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
        int maxX = (int)Math.min(canvas.w - 1, Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
        int minY = (int)Math.max(0, Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
        int maxY = (int)Math.min(canvas.h - 1, Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
        double area = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
        if (Math.abs(area) < 1.0E-9) {
            return;
        }
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double px = x + 0.5, py = y + 0.5;
                double w0 = ((b[0] - px) * (c[1] - py) - (b[1] - py) * (c[0] - px)) / area;
                double w1 = ((c[0] - px) * (a[1] - py) - (c[1] - py) * (a[0] - px)) / area;
                double w2 = 1 - w0 - w1;
                if (w0 < -1e-6 || w1 < -1e-6 || w2 < -1e-6) {
                    continue;
                }
                double z = 1.0 / (w0 / a[2] + w1 / b[2] + w2 / c[2]);
                int i = y * canvas.w + x;
                if (z > canvas.depth[i] + (loose ? 0.03 : 0.0)) {
                    continue;
                }
                double alpha = (((ca >>> 24) * w0 + (cb >>> 24) * w1 + (cc >>> 24) * w2) / 255.0) * k;
                int under = canvas.rgb[i];
                int out = 0;
                for (int sh = 16; sh >= 0; sh -= 8) {
                    double col = ((ca >> sh) & 255) * w0 + ((cb >> sh) & 255) * w1 + ((cc >> sh) & 255) * w2;
                    double v = ((under >> sh) & 255) * (1 - alpha) + col * alpha + ((add >> sh) & 255);
                    out |= (int)Math.max(0, Math.min(255, v)) << sh;
                }
                canvas.rgb[i] = out;
                if (write) {
                    canvas.depth[i] = z;
                }
            }
        }
    }

    static double[][] spline(double[][] keys, int per) {
        int k = keys.length;
        double[][] out = new double[(k - 1) * per + 1][];
        int o = 0;
        for (int s = 0; s < k - 1; s++) {
            double[] p0 = keys[Math.max(0, s - 1)], p1 = keys[s], p2 = keys[s + 1], p3 = keys[Math.min(k - 1, s + 2)];
            for (int j = 0; j < per; j++) {
                double t = j / (double)per, t2 = t * t, t3 = t2 * t;
                double[] p = new double[3];
                for (int c = 0; c < 3; c++) {
                    p[c] = 0.5 * (2 * p1[c] + (-p0[c] + p2[c]) * t + (2 * p0[c] - 5 * p1[c] + 4 * p2[c] - p3[c]) * t2 + (-p0[c] + 3 * p1[c] - 3 * p2[c] + p3[c]) * t3);
                }
                out[o++] = p;
            }
        }
        out[o] = keys[k - 1].clone();
        return out;
    }

    static double[][] resample(double[][] line, int n) {
        int m = line.length;
        double[] arc = new double[m];
        for (int i = 1; i < m; i++) {
            arc[i] = arc[i - 1] + Math.sqrt(Math.pow(line[i][0] - line[i - 1][0], 2) + Math.pow(line[i][1] - line[i - 1][1], 2) + Math.pow(line[i][2] - line[i - 1][2], 2));
        }
        double[][] out = new double[n][];
        int j = 1;
        for (int i = 0; i < n; i++) {
            double at = arc[m - 1] * i / (n - 1);
            while (j < m - 1 && arc[j] < at) {
                j++;
            }
            double span = arc[j] - arc[j - 1];
            double t = span < 1e-9 ? 0 : Math.max(0, Math.min(1, (at - arc[j - 1]) / span));
            out[i] = lerp(line[j - 1], line[j], t);
        }
        return out;
    }
}
