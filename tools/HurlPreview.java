import com.hex.kagune.control.RinkakuHurl;
import com.hex.kagune.control.RinkakuIdle;
import com.hex.kagune.control.RinkakuRoot;
import com.hex.kagune.control.ThyDeath;
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
 * 3.1.6: Rinkaku's Rubble Hurl off-game, as the game lays it out: the chunk where the server holds it ({@link RinkakuHurl#rising},
 * {@link RinkakuHurl#hover}) at its size, drawn as {@code HurlClient} draws it (a core cube with lumps, {@link RinkakuHurl#pieces}; flat
 * stone colours here, the block's own model in game), the two gripping tails bent to it as {@code HurlClient.apply} bends them
 * ({@code ThyDeath.strike}), then grown out of his back ({@code RinkakuRoot.attach}); the throw's flight ({@link RinkakuHurl#aim},
 * {@link RinkakuHurl#step}) as a ghosted chunk every other tick to its mark.
 *
 * <pre>
 * javac -nowarn -cp build/quick/classes:build/quick/base.jar -d build/impale-preview tools/PoseRender.java tools/ArmSolve.java \
 *     tools/ReaperPreview.java tools/KaguneShowcase.java tools/ImpalePreview.java tools/HurlPreview.java
 * java -Djava.awt.headless=true -cp build/impale-preview:build/quick/classes:build/quick/base.jar HurlPreview \
 *     &lt;skin.png&gt; src/main/resources/assets/hexkagune/textures/misc docs/handoff/rinkaku-hurl-3.1.6.png
 * </pre>
 */
public final class HurlPreview {
    /** age: ticks since it was torn out (held), or negative: -(ticks since it was thrown) - 1. */
    record Shot(String label, double[] eye, double[] at, double fov, double age, double charge, boolean own, boolean flight) {
    }

    static final int COLOUR = 0xA50F37;
    static final double[] EYE = {0.0, 1.62, 0.0};
    static final double[] SOURCE = {0.0, -0.5, RinkakuHurl.GROUND_AHEAD};
    static final double[] MARK = {0.0, 0.9, 18.0};

    public static void main(String[] args) throws Exception {
        PoseRender.skin = ImageIO.read(new File(args[0]));
        PoseRender.slim = false;
        File misc = new File(args[1]);
        KaguneShowcase.scales = ImageIO.read(new File(misc, "rinkaku_skin.png"));
        KaguneShowcase.mark = ImageIO.read(new File(misc, "rinkaku_mark.png"));
        KaguneShowcase.markGlow = ImageIO.read(new File(misc, "rinkaku_mark_glow.png"));
        KaguneShowcase.time = 12.0;
        List<Shot> shots = List.of(
            new Shot("torn out of the ground, rising (3 ticks in)", new double[] {-6.5, 2.4, 2.4}, new double[] {0.0, 1.0, 1.4}, 44, 3, 0.1, false, false),
            new Shot("a tap: a small chunk in the tails' grip", new double[] {-4.2, 2.6, 6.0}, new double[] {0.0, 1.9, 1.0}, 44, 10, 0.0, false, false),
            new Shot("held to full charge: two across", new double[] {-4.6, 2.8, 6.4}, new double[] {0.0, 2.3, 1.0}, 44, 40, 1.0, false, false),
            new Shot("from behind: both tails gripping it", new double[] {-1.8, 3.2, -4.8}, new double[] {0.0, 2.2, 1.4}, 46, 40, 1.0, false, false),
            new Shot("straight from behind (3.1.7: round his shoulders, never through him)", new double[] {0.0, 2.6, -4.2}, new double[] {0.0, 2.0, 1.0},
                44, 40, 1.0, false, false),
            new Shot("from his side, held: out of his back, up behind his shoulder", new double[] {-6.0, 2.2, 0.6}, new double[] {0.0, 1.9, 0.9}, 44, 40,
                1.0, false, false),
            new Shot("thrown at a mark 18 blocks off: its lob, every other tick", new double[] {-15.0, 4.0, 9.0}, new double[] {0.0, 2.0, 9.0}, 48, -3, 1.0,
                false, true),
            new Shot("his own first person, full charge", new double[] {0.0, 1.62, 0.05}, new double[] {0.0, 2.3, 6.0}, 70, 40, 1.0, true, false));
        int cols = 2;
        int w = 760;
        int h = 420;
        int rows = (shots.size() + cols - 1) / cols;
        BufferedImage sheet = new BufferedImage(w * cols, h * rows, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        for (int i = 0; i < shots.size(); i++) {
            Shot s = shots.get(i);
            g.drawImage(render(s, w, h), (i % cols) * w, (i / cols) * h, w, h, null);
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
        RigPose pose = new RigPose();
        pose.rightArmZ = 0.12F;
        pose.leftArmZ = -0.12F;
        pose.rightLegX = -0.1F;
        pose.leftLegX = 0.1F;
        PoseRender.armOut = ReaperBuild.PLAYER_ARM_OUT;
        KaguneShowcase.closeUp = false;
        PoseRender.Camera cam = new PoseRender.Camera(s.eye, s.at, s.fov);
        PoseRender.Canvas canvas = new PoseRender.Canvas(w, h, 2, cam, 0x3A4450);
        canvas.floor(30, 0x58626C, 0x4E5862);
        if (!s.own) {
            List<PoseRender.Tri> body = PoseRender.model(pose);
            ReaperPreview.chest(body, pose, false);
            ReaperPreview.chest(body, pose, true);
            for (PoseRender.Tri t : body) {
                canvas.tri(t, true);
            }
            KaguneShowcase.drawMark(canvas, pose, COLOUR, true, 1.0);
        }
        double size = RinkakuHurl.size(s.charge, 1);
        double[] hover = RinkakuHurl.hover(EYE, 0.0, 1.0, size);
        double[] chunk;
        double tailAge;
        double[] whip = null;
        if (s.age >= 0) {
            chunk = s.age < RinkakuHurl.RISE ? RinkakuHurl.rising(SOURCE, hover, s.age) : hover;
            tailAge = Math.min(ThyDeath.HIT_AT, s.age * 2.0);
            // The hole it was torn from (the block out of the world, put back later).
            if (s.age < RinkakuHurl.RISE) {
                cube(canvas, new double[] {SOURCE[0], -0.499, SOURCE[2]}, 1.0, 0.0, 0.0, 0x262A30, 0.02);
            }
        } else {
            double flown = -s.age - 1;
            double[] pos = hover.clone();
            double[] vel = RinkakuHurl.aim(pos, MARK, RinkakuHurl.speed(s.charge));
            for (int t = 0; t < RinkakuHurl.FLIGHT; t++) {
                if (t % 2 == 0) {
                    rubble(canvas, pos, size, 77, t * 21.0, 0.55);
                }
                RinkakuHurl.step(pos, vel);
                if (Math.abs(pos[2] - MARK[2]) < 1e-6) {
                    rubble(canvas, pos, size, 77, (t + 1) * 21.0, 0.8); // where it bursts: on the mark
                    break;
                }
            }
            cube(canvas, new double[] {MARK[0], 0.02, MARK[2]}, 1.6, 0.0, 0.0, 0xD0B040, 0.02);
            chunk = null;
            tailAge = ThyDeath.HIT_AT + flown;
            double[] p = hover.clone();
            double[] v = RinkakuHurl.aim(p, MARK, RinkakuHurl.speed(s.charge));
            for (int t = 0; t < flown; t++) {
                RinkakuHurl.step(p, v);
            }
            whip = p;
        }
        if (chunk != null) {
            rubble(canvas, chunk, size, 77, Math.sin(s.age * 0.13) * 9.0, 1.0);
        }
        for (int limb = 0; limb < 4; limb++) {
            boolean grips = limb < 2;
            if (!grips) {
                if (!s.own) {
                    KaguneShowcase.drawTail(canvas, pose, limb, COLOUR, cam, 1.0);
                }
                continue;
            }
            double[][] rest = RinkakuIdle.pose(limb, 4, 0.0, 46, 1.8, KaguneShowcase.LENGTH, KaguneShowcase.time, 77 + limb * 131, 0.0, 0.0, 0.0);
            double[][] world = new double[rest.length][];
            for (int i = 0; i < rest.length; i++) {
                world[i] = new double[] {-rest[i][0], rest[i][1], rest[i][2]};
            }
            double side = RinkakuRoot.side(limb, 0.0);
            double sx = side >= 0.0 ? 1.0 : -1.0;
            double[] target = chunk != null ? chunk : whip;
            if (chunk == null) {
                double[] root = world[0];
                double dx = whip[0] - root[0], dy = whip[1] - root[1], dz = whip[2] - root[2];
                double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double k = d > 4.5 ? 4.5 / d : 1.0;
                target = new double[] {root[0] + dx * k, root[1] + dy * k, root[2] + dz * k};
            }
            // His right at yaw 0 is -x in this world, as in game. 3.1.7: round him to its grip (RinkakuHurl.grip), blended in by a lunge's
            // reach (HurlClient.apply), then kept clear of him (BodyClearance, as StrikePaths does next).
            double[] grip = {target[0] - sx * size * 0.3, target[1], target[2]};
            double[][] bent = world;
            if (tailAge < ThyDeath.STRIKE) {
                double[][] round = RinkakuHurl.grip(world[0], new double[] {0.0, 0.0, 0.0}, 1.8, side, -1.0, 0.0, 0.0, 1.0, grip, world.length, s.own);
                bent = com.hex.kagune.control.RinkakuImpale.blend(world, round, ThyDeath.reach(tailAge));
            }
            double thickness = (0.18 + 3.5E-5 * 500) * 0.6 * 0.94;
            com.hex.kagune.control.BodyClearance.clear(bent, new double[] {0.0, 0.0, 0.0}, 0.0, 1.0, 0.6, 1.8, thickness,
                RinkakuRoot.clearance(limb, 0.0));
            double[][] own = new double[bent.length][];
            for (int i = 0; i < bent.length; i++) {
                own[i] = new double[] {-bent[i][0], bent[i][1], bent[i][2]};
            }
            double[][] grown = RinkakuRoot.attach(own, limb, 0.0, 1.8);
            double[][] drawn = new double[grown.length][];
            for (int i = 0; i < grown.length; i++) {
                drawn[i] = new double[] {-grown[i][0], grown[i][1], grown[i][2]};
            }
            ImpalePreview.drawRibbon(canvas, drawn, thickness, COLOUR, limb * 131 + 7, cam, 0.0);
        }
        return canvas.image();
    }

    /** The chunk as HurlClient draws it: a core cube and lumps round it, turned {@code turn} degrees; {@code shade} dims a ghost of it. */
    static void rubble(PoseRender.Canvas canvas, double[] at, double size, int seed, double turn, double shade) {
        for (double[] lump : RinkakuHurl.pieces(seed, RinkakuHurl.lumps(size))) {
            double[] c = {at[0] + lump[0] * size, at[1] + lump[1] * size, at[2] + lump[2] * size};
            cube(canvas, c, lump[3] * size, lump[4] + turn * 0.3, turn, PoseRender.shade(0x8A8F94, shade), 0.5);
        }
    }

    /** A cube {@code edge} across at {@code at}, turned {@code yaw} about the vertical and {@code pitch} about x, lit flat-ish. */
    static void cube(PoseRender.Canvas canvas, double[] at, double edge, double yaw, double pitch, int rgb, double height) {
        double e = edge * 0.5;
        double ya = Math.toRadians(yaw), pa = Math.toRadians(pitch);
        double[][] corners = new double[8][];
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? -e : e;
            double y = height < 0.1 ? ((i & 2) == 0 ? 0.0 : 0.002) : ((i & 2) == 0 ? -e : e);
            double z = (i & 4) == 0 ? -e : e;
            double y2 = y * Math.cos(pa) - z * Math.sin(pa);
            double z2 = y * Math.sin(pa) + z * Math.cos(pa);
            double x3 = x * Math.cos(ya) + z2 * Math.sin(ya);
            double z3 = -x * Math.sin(ya) + z2 * Math.cos(ya);
            corners[i] = new double[] {at[0] + x3, at[1] + y2, at[2] + z3};
        }
        int[][] faces = {{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
        for (int[] f : faces) {
            double[] a = corners[f[0]], b = corners[f[1]], c = corners[f[2]], d = corners[f[3]];
            double[] n = PoseRender.norm(PoseRender.cross(PoseRender.sub(b, a), PoseRender.sub(d, a)));
            canvas.tri(new PoseRender.Tri(new double[][] {a, b, c}, rgb, n), true);
            canvas.tri(new PoseRender.Tri(new double[][] {a, c, d}, rgb, n), true);
        }
    }
}
