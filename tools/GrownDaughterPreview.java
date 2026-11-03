import com.hex.kagune.control.FemaleBody;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.npc.Persona;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 2.0.3: a grown daughter (ChildStage.ADULT) as ChildRenderer draws her: Touka's legs (wider, deeper, their pivots out), slim arms
 * (a wide skin's drawn {@link FemaleBody#ARMS} as wide), the bust (FemaleFigure.bust's wedge, Touka's curves, painted from the skin's
 * torso front by the game's own u = 24 + x, v = 20 + y, its second layer over it), drawn at Touka's size; beside her the same daughter
 * as a teen, to scale. Stiff limbs, standing (PoseRender: no bounce). Run the build (or tools/quick_check.py) first, then:
 * <pre>
 * javac -nowarn -cp build/quick/classes:build/quick/base.jar -d build/grown-preview tools/PoseRender.java tools/VillagerPreview.java tools/GrownDaughterPreview.java
 * java -Djava.awt.headless=true -cp build/grown-preview:build/quick/classes:build/quick/base.jar GrownDaughterPreview &lt;out.png&gt; daughter_01 ...
 * </pre>
 */
public final class GrownDaughterPreview {
    static final String SKINS = "src/main/resources/assets/hexkagune/textures/entity/child/skins/";

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        String[] keys = java.util.Arrays.copyOfRange(args, 1, args.length);
        int w = 220;
        int h = 300;
        String[] heads = {"teen (before)", "grown: front", "grown: three-quarter", "grown: side"};
        BufferedImage sheet = new BufferedImage(heads.length * w, keys.length * (h + 22) + 26, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x20262C));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        g.setColor(Color.WHITE);
        for (int c = 0; c < heads.length; c++) {
            g.drawString(heads[c], c * w + 8, 18);
        }
        for (int r = 0; r < keys.length; r++) {
            BufferedImage skin = VillagerPreview.small(ImageIO.read(new File(SKINS + keys[r] + ".png")));
            boolean slim = slim(keys[r]);
            PoseRender.skin = skin;
            PoseRender.slim = slim;
            float woman = ChildStage.ADULT.draw * Persona.TOUKA.size();
            Object[][] shots = {{false, 0.0, ChildStage.TEEN.draw}, {true, 0.0, woman}, {true, 35.0, woman}, {true, 90.0, woman}};
            for (int c = 0; c < shots.length; c++) {
                boolean grown = (Boolean)shots[c][0];
                double yaw = (Double)shots[c][1];
                double size = (Float)shots[c][2];
                List<PoseRender.Tri> tris = grown ? grown(new RigPose(), skin, slim) : PoseRender.model(new RigPose());
                List<PoseRender.Tri> scaled = new ArrayList<>();
                for (PoseRender.Tri t : tris) {
                    double[][] v = new double[3][];
                    for (int i = 0; i < 3; i++) {
                        v[i] = PoseRender.scale(t.v()[i], size);
                    }
                    scaled.add(new PoseRender.Tri(v, t.rgb(), t.n()));
                }
                PoseRender.Canvas canvas = new PoseRender.Canvas(w, h, 2, PoseRender.orbit(yaw, 8.0, 3.6, 0.85), 0x3A4450);
                canvas.floor(6, 0x58626C, 0x4E5862);
                for (PoseRender.Tri t : scaled) {
                    canvas.tri(t, true);
                }
                g.drawImage(canvas.image(), c * w, 26 + r * (h + 22), null);
            }
            g.setColor(Color.WHITE);
            g.drawString(keys[r] + (slim ? " (slim skin)" : " (wide skin: arms drawn slim)"), 8, 26 + r * (h + 22) + h + 16);
        }
        ImageIO.write(sheet, "png", out);
    }

    static boolean slim(String key) throws Exception {
        String src = new String(java.nio.file.Files.readAllBytes(new File("src/main/java/com/hex/kagune/control/family/ChildSkins.java").toPath()));
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + key + "\", (true|false), (true|false)").matcher(src);
        return m.find() && Boolean.parseBoolean(m.group(2));
    }

    /** PoseRender.model's skinned branch with the figure: legs shaped and spread (Touka's), wide arms slim, the bust on the torso. */
    static List<PoseRender.Tri> grown(RigPose pose, BufferedImage skin, boolean slim) {
        return grown(pose, skin, slim, Persona.TOUKA.legs(), Persona.TOUKA.curves());
    }

    /** 2.0.34: the same with any figure's {@code legs} and {@code curves} (a village woman's, AgentRules.FEMALE_*). */
    static List<PoseRender.Tri> grown(RigPose pose, BufferedImage skin, boolean slim, float legs, float curves) {
        List<PoseRender.Tri> tris = new ArrayList<>();
        double hips = 1.0 + (FemaleBody.HIPS - 1.0) * legs;
        double thighs = 1.0 + (FemaleBody.THIGHS - 1.0) * legs;
        double legX = 2.0 * hips - 0.1;
        PoseRender.Part head = PoseRender.part(pose, "head", 0, 0, 0, 0, 0, 0);
        PoseRender.Part torso = PoseRender.part(pose, "torso", 0, 0, 0, 0, 0, 0);
        PoseRender.Part rightArm = PoseRender.part(pose, "rightArm", -5, 2, 0, 0, 0, PoseRender.BOB);
        PoseRender.Part leftArm = PoseRender.part(pose, "leftArm", 5, 2, 0, 0, 0, -PoseRender.BOB);
        PoseRender.Part rightLeg = PoseRender.part(pose, "rightLeg", -legX, 12, 0.1, 0, 0, 0);
        PoseRender.Part leftLeg = PoseRender.part(pose, "leftLeg", legX, 12, 0.1, 0, 0, 0);
        java.util.function.UnaryOperator<double[]> same = q -> q;
        java.util.function.UnaryOperator<double[]> world = q -> PoseRender.world(pose, q);
        double arms = slim ? 1.0 : FemaleBody.ARMS;
        Object[][] parts = {
            {new double[] {-4, -8, -4}, new double[] {4, 0, 4}, 0, head, 0, 0, 32, 0, 0.5, 1.0, 1.0},
            {new double[] {-4, 0, -2}, new double[] {4, 12, 2}, -1, torso, 16, 16, 16, 32, 0.25, 1.0, 1.0},
            {new double[] {slim ? -2 : -3, slim ? -1.5 : -2, -2}, new double[] {1, slim ? 10.5 : 10, 2}, 1, rightArm, 40, 16, 40, 32, 0.25, arms, 1.0},
            {new double[] {-1, slim ? -1.5 : -2, -2}, new double[] {slim ? 2 : 3, slim ? 10.5 : 10, 2}, 1, leftArm, 32, 48, 48, 48, 0.25, arms, 1.0},
            {new double[] {-2, 0, -2}, new double[] {2, 12, 2}, 1, rightLeg, 0, 16, 0, 32, 0.25, hips, thighs},
            {new double[] {-2, 0, -2}, new double[] {2, 12, 2}, 1, leftLeg, 16, 48, 0, 48, 0.25, hips, thighs}};
        for (Object[] p : parts) {
            double[] mn = (double[])p[0];
            double[] mx = (double[])p[1];
            int w = (int)Math.round(mx[0] - mn[0]);
            int h = (int)Math.round(mx[1] - mn[1]);
            int d = (int)Math.round(mx[2] - mn[2]);
            double sx = (Double)p[9];
            double sz = (Double)p[10];
            PoseRender.cube(tris, new PoseRender.Cube(mn, mx, (Integer)p[2], PoseRender.textured((Integer)p[4], (Integer)p[5], w, h, d, false)),
                (PoseRender.Part)p[3], same, world, sx, sz);
            PoseRender.cube(tris, new PoseRender.Cube(mn, mx, (Integer)p[2], PoseRender.textured((Integer)p[6], (Integer)p[7], w, h, d, true)),
                (PoseRender.Part)p[3], same, world, sx, sz, (Double)p[8]);
        }
        bust(tris, pose, skin, false, curves);
        bust(tris, pose, skin, true, curves);
        return tris;
    }

    /** FemaleFigure.bust's wedge in cells a texel wide, each the skin's torso front texel the game samples there. */
    static void bust(List<PoseRender.Tri> tris, RigPose pose, BufferedImage skin, boolean jacket, float curves) {
        double grow = jacket ? 0.25 : 0.0;
        double top = FemaleBody.BUST_TOP;
        double tip = FemaleBody.BUST_TIP;
        double bottom = FemaleBody.BUST_BOTTOM;
        double front = FemaleBody.BUST_FRONT - grow;
        double out = FemaleBody.BUST_FRONT + (FemaleBody.BUST_OUT - FemaleBody.BUST_FRONT) * curves - grow;
        int v0 = jacket ? 36 : 20;
        int steps = 6;
        for (int i = 0; i < 8; i++) {
            double xa = -4.0 - (i == 0 ? grow : 0.0) + i;
            double xb = -4.0 + i + 1.0 + (i == 7 ? grow : 0.0);
            for (int s = 0; s < steps; s++) {
                // The upper slope (top -> tip) and the underside (tip -> bottom), a strip each.
                for (int half = 0; half < 2; half++) {
                    double ya = half == 0 ? top + (tip - top) * s / steps : tip + (bottom - tip) * s / steps;
                    double yb = half == 0 ? top + (tip - top) * (s + 1) / steps : tip + (bottom - tip) * (s + 1) / steps;
                    double za = half == 0 ? front + (out - front) * s / steps : out + (front - out) * s / steps;
                    double zb = half == 0 ? front + (out - front) * (s + 1) / steps : out + (front - out) * (s + 1) / steps;
                    int rgb = texel(skin, 24.0 + (xa + xb) / 2.0, v0 + (ya + yb) / 2.0, jacket);
                    if (rgb < 0) {
                        continue;
                    }
                    // The game's own normals (model space, y down, -z front): the upper slope up and out, the underside down and out.
                    double[] n = half == 0 ? new double[] {0.0, -0.8, -1.0} : new double[] {0.0, 0.6, -1.0};
                    quad(tris, pose, rgb, new double[][] {{xa, ya, za}, {xb, ya, za}, {xb, yb, zb}, {xa, yb, zb}}, n);
                }
            }
        }
        // The two sides: a triangle each (top, tip, bottom), the edge column's texels.
        for (double x : new double[] {-4.0 - grow, 4.0 + grow}) {
            int rgb = texel(skin, 24.0 + Math.signum(x) * 3.75, v0 + (top + bottom) / 2.0, jacket);
            if (rgb < 0) {
                continue;
            }
            double[] a = PoseRender.point(pose, "torso", new double[] {x, top, front});
            double[] b = PoseRender.point(pose, "torso", new double[] {x, tip, out});
            double[] c = PoseRender.point(pose, "torso", new double[] {x, bottom, front});
            tris.add(new PoseRender.Tri(new double[][] {a, b, c}, rgb, normal(pose, new double[] {Math.signum(x), 0.0, 0.0})));
        }
    }

    static int texel(BufferedImage skin, double u, double v, boolean jacket) {
        int argb = skin.getRGB(Math.max(0, Math.min(63, (int)Math.floor(u))), Math.max(0, Math.min(63, (int)Math.floor(v))));
        if (jacket && (argb >>> 24) < 128) {
            return -1;
        }
        return argb & 0xFFFFFF;
    }

    static void quad(List<PoseRender.Tri> tris, RigPose pose, int rgb, double[][] q, double[] modelNormal) {
        double[][] p = new double[4][];
        for (int i = 0; i < 4; i++) {
            p[i] = PoseRender.point(pose, "torso", q[i]);
        }
        double[] n = normal(pose, modelNormal);
        tris.add(new PoseRender.Tri(new double[][] {p[0], p[1], p[2]}, rgb, n));
        tris.add(new PoseRender.Tri(new double[][] {p[0], p[2], p[3]}, rgb, n));
    }

    /** A model-space direction (pixels: x her left, y down, z back) in the world, as the torso is posed. */
    static double[] normal(RigPose pose, double[] d) {
        double[] a = PoseRender.point(pose, "torso", new double[] {0.0, 4.0, 0.0});
        double[] b = PoseRender.point(pose, "torso", new double[] {d[0], 4.0 + d[1], d[2]});
        return PoseRender.norm(PoseRender.sub(b, a));
    }
}
