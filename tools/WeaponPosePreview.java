import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.client.animation.WeaponPose;
import com.hex.kagune.control.client.animation.QuinqueGrip;
import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.quinque.ArmMesh;
import com.hex.kagune.control.quinque.WhipPath;
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
 * 3.0.0: an investigator with its kagune-made weapon, posed off-game exactly as the game poses it ({@link WeaponPose}, the left hand put
 * on its grip by QuinqueGrip.reach as AgentClient does), the weapon's real mesh where WeaponLayer draws it, the whip's tendril along its
 * real path ({@link WhipPath}) as a plain tube. One sheet per weapon: its stance, every blow of its string as it lands, its two moves.
 * <pre>
 * javac -nowarn -cp build/quick/classes -d build/weapon-preview tools/PoseRender.java tools/WeaponPosePreview.java
 * java -Djava.awt.headless=true -cp build/weapon-preview:build/quick/classes WeaponPosePreview &lt;out-dir&gt;
 * </pre>
 */
public class WeaponPosePreview {
    static final int W = 300;
    static final int H = 360;

    /** Where in a blow its frames are drawn (its keys' times: 0.5 its landing). */
    static final double[] KEYS = {0.0, 0.12, 0.3, 0.42, 0.5, 0.6, 0.75, 0.9};

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/weapon-preview");
        double yaw = args.length > 1 ? Double.parseDouble(args[1]) : 40.0;
        String tag = args.length > 2 ? args[2] : "";
        out.mkdirs();
        int fw = 200;
        int fh = 250;
        for (Arm arm : Arm.values()) {
            List<String> rowNames = new ArrayList<>();
            List<RigPose[]> rows = new ArrayList<>();
            List<double[][][]> rowWhips = new ArrayList<>();
            List<float[]> rowGrown = new ArrayList<>();
            List<double[]> rowAges = new ArrayList<>();
            for (int step = 0; step < arm.length(); step++) {
                Arm.Strike s = arm.strike(step);
                double l = s.land() / (double)s.total();
                RigPose[] frames = new RigPose[KEYS.length];
                double[][][] whips = new double[KEYS.length][][];
                float[] g = new float[KEYS.length];
                double[] ages = new double[KEYS.length];
                for (int i = 0; i < KEYS.length; i++) {
                    double u = KEYS[i];
                    double c = u < 0.5 ? u / 0.5 * l : l + (u - 0.5) / 0.5 * (1.0 - l);
                    double age = c * s.total();
                    ages[i] = age;
                    frames[i] = finish(arm, WeaponPose.strike(arm, step, age), false);
                    whips[i] = arm == Arm.WHIP ? WhipPath.lash(step, c, l, WhipPath.length(false), age) : null;
                    g[i] = 1.0F;
                }
                rowNames.add((step + 1) + ". " + s.name());
                rows.add(frames);
                rowWhips.add(whips);
                rowGrown.add(g);
                rowAges.add(ages);
            }
            for (Arm.Ability a : arm.abilities()) {
                RigPose[] frames = new RigPose[KEYS.length];
                double[][][] whips = new double[KEYS.length][][];
                float[] g = new float[KEYS.length];
                double[] ages = new double[KEYS.length];
                for (int i = 0; i < KEYS.length; i++) {
                    double age = (i + 0.5) / KEYS.length * a.total;
                    ages[i] = age;
                    frames[i] = finish(arm, WeaponPose.ability(arm, a, age), WeaponPose.oneHanded(a, age));
                    whips[i] = a == Arm.Ability.BIND ? WhipPath.bind(age, new double[] {4.0, 0.0, -0.3}, 0.35, WhipPath.length(false), age)
                        : a == Arm.Ability.FLAY ? WhipPath.flay(age, WhipPath.length(false), age) : null;
                    g[i] = WeaponPose.grown(a, age);
                }
                rowNames.add(a.title);
                rows.add(frames);
                rowWhips.add(whips);
                rowGrown.add(g);
                rowAges.add(ages);
            }
            BufferedImage sheet = new BufferedImage(fw * KEYS.length + 150, fh * rows.size() + 34, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = sheet.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(26, 26, 32));
            g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            g.drawString("3.0.1: " + arm.title + " - every blow of its string and both moves, frame by frame (the 5th frame of a blow is where it lands)", 10, 24);
            for (int r = 0; r < rows.size(); r++) {
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                g.setColor(Color.WHITE);
                g.drawString(rowNames.get(r), 8, 34 + r * fh + fh / 2);
                for (int i = 0; i < KEYS.length; i++) {
                    RigPose p = rows.get(r)[i];
                    List<PoseRender.Tri> extra = new ArrayList<>();
                    weapon(extra, p, arm, rowGrown.get(r)[i]);
                    if (arm == Arm.WHIP) {
                        double[][] w = rowWhips.get(r)[i];
                        tendril(extra, p, w == null ? rest(arm) : w);
                    }
                    BufferedImage img = PoseRender.render(p, PoseRender.orbit(yaw, 12, 7.8, 0.9), fw, fh, extra);
                    g.drawImage(img, 150 + i * fw, 34 + r * fh, null);
                    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
                    g.setColor(new Color(220, 220, 220));
                    g.drawString(String.format("%.1ft", rowAges.get(r)[i]), 150 + i * fw + 4, 34 + r * fh + 13);
                }
            }
            g.dispose();
            File file = new File(out, arm.name().toLowerCase() + "-moves-3.0.1" + tag + ".png");
            ImageIO.write(sheet, "png", file);
            System.out.println(file);
        }
    }

    /** As AgentClient finishes it: the left hand put on a two-handed one's grip (not while it throws). */
    static RigPose finish(Arm arm, RigPose p, boolean oneHanded) {
        if (arm.twoHanded && !oneHanded) {
            QuinqueGrip.reach(p, WeaponPose.base(arm), false);
        }
        return p;
    }

    static double[][] rest(Arm arm) {
        return arm == Arm.WHIP ? WhipPath.rest(WhipPath.length(false), 0.85, 0.0) : null;
    }

    static void add(List<String> labels, List<RigPose> poses, List<double[][]> whips, List<Float> grown, String label, RigPose p, Arm arm,
                    double[][] whip, float g) {
        labels.add(label);
        poses.add(p);
        whips.add(whip == null && arm == Arm.WHIP ? rest(arm) : whip);
        grown.add(g);
    }

    /** The weapon's mesh where WeaponLayer draws it on the right arm. */
    static void weapon(List<PoseRender.Tri> tris, RigPose pose, Arm arm, float k) {
        if (k <= 0.01F) {
            return;
        }
        ArmMesh.Mesh mesh = ArmMesh.of(arm, false);
        double[][] placed = WeaponPose.meshToArm(arm);
        double[] fist = placed[3];
        for (int q = 0; q < mesh.quads; q++) {
            double[][] v = new double[4][];
            for (int c = 0; c < 4; c++) {
                int i = (q * 4 + c) * 3;
                double[] a = WeaponPose.onArm(placed, new double[] {mesh.pos[i], mesh.pos[i + 1], mesh.pos[i + 2]});
                for (int j = 0; j < 3; j++) {
                    a[j] = fist[j] + (a[j] - fist[j]) * k;
                }
                v[c] = PoseRender.point(pose, "rightArm", new double[] {a[0] * 16.0, a[1] * 16.0, a[2] * 16.0});
            }
            int argb = mesh.argb[q * 4];
            int rgb = mesh.glow[q] ? brighten(argb) : argb & 0xFFFFFF;
            double[] n = PoseRender.norm(PoseRender.cross(PoseRender.sub(v[1], v[0]), PoseRender.sub(v[2], v[0])));
            tris.add(new PoseRender.Tri(new double[][] {v[0], v[1], v[2]}, rgb, n));
            tris.add(new PoseRender.Tri(new double[][] {v[0], v[2], v[3]}, rgb, n));
        }
    }

    static int brighten(int argb) {
        int r = Math.min(255, ((argb >> 16) & 255) + 60);
        int g = Math.min(255, ((argb >> 8) & 255) + 30);
        int b = Math.min(255, (argb & 255) + 30);
        return r << 16 | g << 8 | b;
    }

    /** The whip's tendril along {@code path} ({forward, left, up} from its knot) as a tapering tube. */
    static void tendril(List<PoseRender.Tri> tris, RigPose pose, double[][] path) {
        if (path == null) {
            return;
        }
        double[] knotArm = WeaponPose.onArm(WeaponPose.meshToArm(Arm.WHIP), new double[] {0.0, 0.0, -ArmMesh.WHIP_ROOT});
        double[] knot = PoseRender.point(pose, "rightArm", new double[] {knotArm[0] * 16.0, knotArm[1] * 16.0, knotArm[2] * 16.0});
        float[] rot = pose.transform("body", RigPose.ROTATION, 0, 0, 0);
        double[][] world = new double[path.length][];
        for (int i = 0; i < path.length; i++) {
            double[] off = {path[i][1], path[i][2], path[i][0]};
            off = PoseRender.rotate(off, new double[] {0, 1, 0}, rot[1]);
            world[i] = PoseRender.add(knot, off);
            world[i][1] = Math.max(world[i][1], 0.03); // as WeaponLayer: along the ground, never into it
        }
        int sides = 6;
        for (int i = 0; i + 1 < world.length; i++) {
            double[] a = world[i];
            double[] b = world[i + 1];
            double[] t = PoseRender.norm(PoseRender.sub(b, a));
            double[] u = PoseRender.norm(PoseRender.cross(t, Math.abs(t[1]) < 0.9 ? new double[] {0, 1, 0} : new double[] {1, 0, 0}));
            double[] w = PoseRender.cross(t, u);
            double ra = 0.07 * (1.0 - 0.85 * i / (world.length - 1.0));
            double rb = 0.07 * (1.0 - 0.85 * (i + 1) / (world.length - 1.0));
            for (int s = 0; s < sides; s++) {
                double a0 = 2 * Math.PI * s / sides;
                double a1 = 2 * Math.PI * (s + 1) / sides;
                double[] d0 = PoseRender.add(PoseRender.scale(u, Math.cos(a0)), PoseRender.scale(w, Math.sin(a0)));
                double[] d1 = PoseRender.add(PoseRender.scale(u, Math.cos(a1)), PoseRender.scale(w, Math.sin(a1)));
                double[] p0 = PoseRender.add(a, PoseRender.scale(d0, ra));
                double[] p1 = PoseRender.add(a, PoseRender.scale(d1, ra));
                double[] p2 = PoseRender.add(b, PoseRender.scale(d1, rb));
                double[] p3 = PoseRender.add(b, PoseRender.scale(d0, rb));
                double[] n = PoseRender.norm(PoseRender.add(d0, d1));
                int c = i % 3 == 0 ? 0x8A0E1A : 0xA8141F;
                tris.add(new PoseRender.Tri(new double[][] {p0, p1, p2}, c, n));
                tris.add(new PoseRender.Tri(new double[][] {p0, p2, p3}, c, n));
            }
        }
    }
}
