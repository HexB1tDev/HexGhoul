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

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/weapon-preview");
        out.mkdirs();
        for (Arm arm : Arm.values()) {
            List<String> labels = new ArrayList<>();
            List<RigPose> poses = new ArrayList<>();
            List<double[][]> whips = new ArrayList<>();
            List<Float> grown = new ArrayList<>();
            add(labels, poses, whips, grown, "stance", finish(arm, WeaponPose.stance(arm), false), arm, rest(arm), 1.0F);
            for (int step = 0; step < arm.length(); step++) {
                Arm.Strike s = arm.strike(step);
                for (double at : new double[] {s.land() * 0.55, s.land()}) {
                    RigPose p = finish(arm, WeaponPose.strike(arm, step, at), false);
                    double[][] w = arm == Arm.WHIP ? WhipPath.lash(step, at / s.total(), s.land() / (double)s.total(), WhipPath.length(false), at) : null;
                    add(labels, poses, whips, grown, (at == s.land() ? "lands: " : "") + s.name(), p, arm, w, 1.0F);
                }
            }
            for (Arm.Ability a : arm.abilities()) {
                double[] ages = switch (a) {
                    case BIND -> new double[] {4, 8, 16, 24};
                    case FLAY -> new double[] {6, 11, 16};
                    case JAVELIN -> new double[] {12, 14, 22};
                    case PIERCE -> new double[] {6, 11};
                    case QUAKE -> new double[] {6, 14};
                    case WHIRL -> new double[] {10, 13};
                    case LEAP -> new double[] {5, 11, 17};
                    case CLEAVE -> new double[] {8, 12, 15};
                };
                for (double age : ages) {
                    RigPose p = finish(arm, WeaponPose.ability(arm, a, age), WeaponPose.oneHanded(a, age));
                    double[][] w = a == Arm.Ability.BIND ? WhipPath.bind(age, new double[] {4.0, 0.0, -0.3}, 0.35, WhipPath.length(false), age)
                        : a == Arm.Ability.FLAY ? WhipPath.flay(age, WhipPath.length(false), age) : null;
                    add(labels, poses, whips, grown, a.title + " " + (int)age + "t", p, arm, w, WeaponPose.grown(a, age));
                }
            }
            int cols = 5;
            int rows = (poses.size() + cols - 1) / cols;
            BufferedImage sheet = new BufferedImage(W * cols, H * rows + 34, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = sheet.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(26, 26, 32));
            g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            g.drawString("3.0.0: " + arm.title + " (held as the " + WeaponPose.base(arm).name().toLowerCase() + ")", 10, 24);
            for (int i = 0; i < poses.size(); i++) {
                RigPose p = poses.get(i);
                List<PoseRender.Tri> extra = new ArrayList<>();
                weapon(extra, p, arm, grown.get(i));
                if (arm == Arm.WHIP) {
                    tendril(extra, p, whips.get(i));
                }
                BufferedImage img = PoseRender.render(p, PoseRender.orbit(i % 2 == 0 ? 35 : 55, 14, 7.2, 1.0), W, H, extra);
                int x = (i % cols) * W;
                int y = 34 + (i / cols) * H;
                g.drawImage(img, x, y, null);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                g.setColor(Color.WHITE);
                g.drawString(labels.get(i), x + 6, y + 16);
            }
            g.dispose();
            File file = new File(out, arm.name().toLowerCase() + "-poses-3.0.0.png");
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
