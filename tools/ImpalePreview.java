import com.hex.kagune.control.RinkakuIdle;
import com.hex.kagune.control.RinkakuImpale;
import com.hex.kagune.control.RinkakuRibbon;
import com.hex.kagune.control.RinkakuRoot;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.ReaperBuild;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 3.1.5: Rinkaku's Impale off-game, as the game lays it out and draws it: a Four-Tail Rinkaku, three tails at rest ({@code RinkakuIdle}) and
 * one ({@link RinkakuImpale#spear}) up over its shoulder and straight out along his look, grown out of his back ({@code RinkakuRoot.attach},
 * as {@code StrikePaths} does after it), its mesh {@code RinkakuRibbon.build(..., sharp 1)}: its end drawn to a point. Bodies it carries are
 * placed where the server puts them ({@link RinkakuImpale#slot}). Drawn with {@link KaguneShowcase}'s skin, scales, veins and mark.
 *
 * <pre>
 * javac -nowarn -cp build/quick/classes:build/quick/base.jar -d build/impale-preview tools/PoseRender.java tools/ArmSolve.java \
 *     tools/ReaperPreview.java tools/KaguneShowcase.java tools/ImpalePreview.java
 * java -Djava.awt.headless=true -cp build/impale-preview:build/quick/classes:build/quick/base.jar ImpalePreview \
 *     &lt;skin.png&gt; src/main/resources/assets/hexkagune/textures/misc docs/handoff/rinkaku-impale-3.1.5.png
 * </pre>
 */
public final class ImpalePreview {
    record Shot(String label, double[] eye, double[] at, double fov, double length, double pitch, int carried, int w, int h, boolean own) {
        Shot(String label, double[] eye, double[] at, double fov, double length, double pitch, int carried, int w, int h) {
            this(label, eye, at, fov, length, pitch, carried, w, h, false);
        }
    }

    static final int COLOUR = 0xA50F37;
    static final int LIMB = 0;

    public static void main(String[] args) throws Exception {
        PoseRender.skin = ImageIO.read(new File(args[0]));
        PoseRender.slim = false;
        File misc = new File(args[1]);
        KaguneShowcase.scales = ImageIO.read(new File(misc, "rinkaku_skin.png"));
        KaguneShowcase.mark = ImageIO.read(new File(misc, "rinkaku_mark.png"));
        KaguneShowcase.markGlow = ImageIO.read(new File(misc, "rinkaku_mark_glow.png"));
        KaguneShowcase.time = 12.0;
        List<Shot> shots = List.of(
            new Shot("a tap (6 blocks), from the side", new double[] {-9.5, 2.6, 3.4}, new double[] {0.0, 1.5, 3.4}, 44, RinkakuImpale.TAP, 0.0, 0, 760, 420),
            new Shot("held: 16 blocks (its reach at mastery 0), from the side", new double[] {-20.0, 3.4, 8.2}, new double[] {0.0, 1.5, 8.2}, 46, 16.0, 0.0, 0, 760, 420),
            new Shot("three-quarter behind, two run through and carried", new double[] {-4.2, 3.4, -4.6}, new double[] {0.0, 1.6, 4.0}, 50, 9.0, 0.0, 2, 760, 420),
            new Shot("from in front: up over his shoulder, then straight", new double[] {-3.4, 2.4, 11.5}, new double[] {0.0, 1.5, 0.0}, 40, 9.0, 0.0, 0, 760, 420),
            new Shot("aimed down a little, two carried", new double[] {-8.8, 3.0, 4.8}, new double[] {0.0, 1.0, 4.6}, 48, 9.0, -0.14, 2, 760, 420),
            new Shot("from behind: out of his back, up over his shoulder", new double[] {-1.6, 2.9, -5.2}, new double[] {0.0, 1.5, 2.0}, 46, 9.0, 0.0, 1, 760, 420),
            new Shot("its sharpened end, close", new double[] {-1.6, 2.1, 6.9}, new double[] {0.0, 1.55, 8.4}, 32, 9.0, 0.0, 0, 760, 420),
            new Shot("his own first person (it leaves lower, out of his eyes), one carried", new double[] {0.0, 1.62, 0.05},
                new double[] {0.0, 1.5, 10.0}, 70, 12.0, -0.02, 1, 760, 420, true));
        int cols = 2;
        int rows = (shots.size() + cols - 1) / cols;
        int w = 760;
        int h = 420;
        BufferedImage sheet = new BufferedImage(w * cols, h * rows, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        for (int i = 0; i < shots.size(); i++) {
            Shot s = shots.get(i);
            g.drawImage(render(s), (i % cols) * w, (i / cols) * h, w, h, null);
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

    static BufferedImage render(Shot s) {
        RigPose pose = new RigPose();
        pose.rightArmZ = 0.12F;
        pose.leftArmZ = -0.12F;
        pose.rightLegX = -0.1F;
        pose.leftLegX = 0.1F;
        PoseRender.armOut = ReaperBuild.PLAYER_ARM_OUT;
        KaguneShowcase.closeUp = false;
        PoseRender.Camera cam = new PoseRender.Camera(s.eye, s.at, s.fov);
        PoseRender.Canvas canvas = new PoseRender.Canvas(s.w, s.h, 2, cam, 0x3A4450);
        canvas.floor(30, 0x58626C, 0x4E5862);
        List<PoseRender.Tri> body = PoseRender.model(pose);
        ReaperPreview.chest(body, pose, false);
        ReaperPreview.chest(body, pose, true);
        if (!s.own) {
            for (PoseRender.Tri t : body) {
                canvas.tri(t, true);
            }
            KaguneShowcase.drawMark(canvas, pose, COLOUR, true, 1.0);
        }
        for (int limb = 0; limb < 4; limb++) {
            if (limb != LIMB) {
                KaguneShowcase.drawTail(canvas, pose, limb, COLOUR, cam, 1.0);
            }
        }
        // The impaling tail, in his own frame (x right, y up, z forward, from his feet), as ImpaleClient lays it out.
        double[][] rest = RinkakuIdle.pose(LIMB, 4, 0.0, 46, 1.8, KaguneShowcase.LENGTH, KaguneShowcase.time, 77 + LIMB * 131, 0.0, 0.0, 0.0);
        double side = RinkakuRoot.side(LIMB, 0.0);
        double[] launch = RinkakuImpale.launch(new double[] {0.0, 0.0, 0.0}, 1.8, side, 1.0, 0.0, 0.0, 1.0, s.own);
        double[] way = {0.0, Math.sin(s.pitch), Math.cos(s.pitch)};
        double[][] spear = RinkakuImpale.spear(rest[0], launch, way, s.length, rest.length, side, 1.0, 0.0, 0.0, 1.0);
        double[][] own = RinkakuRoot.attach(spear, LIMB, 0.0, 1.8);
        double[][] world = new double[own.length][];
        for (int i = 0; i < own.length; i++) {
            world[i] = new double[] {-own[i][0], own[i][1], own[i][2]};
        }
        // Bodies it carries: where the server holds them (their middles on its shaft).
        for (int k = 0; k < s.carried; k++) {
            double along = RinkakuImpale.slot(k, s.carried, s.length);
            double[] at = {launch[0] + way[0] * along, launch[1] + way[1] * along, launch[2] + way[2] * along};
            dummy(canvas, -at[0], at[1] - 0.9, at[2]);
        }
        double thickness = (0.18 + 3.5E-5 * 500) * 0.6 * 0.94;
        drawRibbon(canvas, world, thickness, COLOUR, LIMB * 131 + 7, cam);
        return canvas.image();
    }

    /** A body standing at {@code x, y, z} (its feet), facing him (a darker, greener figure: a mob on the spear). */
    static void dummy(PoseRender.Canvas canvas, double x, double y, double z) {
        RigPose pose = new RigPose();
        pose.rightArmX = -1.2F;
        pose.leftArmX = -1.1F;
        pose.rightLegX = 0.35F;
        pose.leftLegX = -0.2F;
        for (PoseRender.Tri t : PoseRender.model(pose)) {
            double[][] v = new double[t.v().length][];
            for (int i = 0; i < v.length; i++) {
                // turned to face him (back toward -z) and moved there
                v[i] = new double[] {-t.v()[i][0] + x, t.v()[i][1] + y, -t.v()[i][2] + z};
            }
            int rgb = t.rgb();
            int r = (rgb >> 16 & 255) * 4 / 10, gr = Math.min(255, (rgb >> 8 & 255) * 7 / 10 + 40), b = (rgb & 255) * 4 / 10;
            canvas.tri(new PoseRender.Tri(v, r << 16 | gr << 8 | b, new double[] {-t.n()[0], t.n()[1], -t.n()[2]}), true);
        }
    }

    /** The tail's mesh as RinkakuTube draws an impaling one (sharp 1): skin with its scales, then its veins. */
    static void drawRibbon(PoseRender.Canvas canvas, double[][] world, double thickness, int colour, int seed, PoseRender.Camera cam) {
        RinkakuRibbon.Mesh m = RinkakuRibbon.build(world, thickness, colour, seed, cam.eye(), KaguneShowcase.time, 1.0);
        if (m == null) {
            return;
        }
        int rings = m.at().length;
        int sides = RinkakuRibbon.SIDES;
        int cuts = 4;
        BufferedImage scales = KaguneShowcase.scales;
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
                        int tint = KaguneShowcase.mix4(tints, sm, tm);
                        int rgb = KaguneShowcase.lit(0xFF000000 | KaguneShowcase.multiply(texel, RinkakuRibbon.lifted(tint)), n, 1.0) & 0xFFFFFF;
                        double alpha = (tint >>> 24) / 255.0;
                        double[][] cell = {KaguneShowcase.bilerp(q, s0, t0), KaguneShowcase.bilerp(q, s1, t0), KaguneShowcase.bilerp(q, s1, t1),
                            KaguneShowcase.bilerp(q, s0, t1)};
                        int c = (int)(alpha * 255) << 24 | rgb;
                        KaguneShowcase.tri(canvas, cell[0], cell[1], cell[2], c, c, c, true, 1.0, 0, false);
                        KaguneShowcase.tri(canvas, cell[0], cell[2], cell[3], c, c, c, true, 1.0, 0, false);
                    }
                }
            }
        }
        for (RinkakuRibbon.Glow glow : RinkakuRibbon.veins(m, seed, KaguneShowcase.time)) {
            int[] a = glow.argb();
            KaguneShowcase.blendQuad(canvas, glow.corners(), a[0] & 0xFFFFFF, (a[0] >>> 24) / 255.0, 0);
        }
    }
}
