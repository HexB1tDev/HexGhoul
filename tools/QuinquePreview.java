import com.hex.kagune.control.client.animation.QuinquePose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.quinque.Quinque;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * 1.3.30: the quinques in the hand, off-game: each one's real model (its "full" frame's elements and its third person transform, read
 * from the resources) carried through vanilla's chain (PlayerModel.translateToHand, ItemInHandLayer's turn and offset, ItemTransform,
 * the model's -0.5 shift) on the right arm of a body posed by {@link QuinquePose} ({@link PoseRender}, stiff limbs: an investigator's
 * vanilla model). The left fist is marked, and the left grip, so a hand off its grip shows. Run {@code python3 build.py} first, then:
 * <pre>
 * javac -nowarn -cp build/classes:build/base.jar -d build/quinque-preview tools/PoseRender.java tools/QuinquePreview.java
 * java -Djava.awt.headless=true -cp build/quinque-preview:build/classes:build/base.jar QuinquePreview &lt;out-dir&gt;
 * </pre>
 */
public final class QuinquePreview {
    static final File MODELS = new File("src/main/resources/assets/hexkagune/models/item/quinque");
    static final File TEXTURES = new File("src/main/resources/assets/hexkagune/textures/item");

    record Element(double[] from, double[] to, int rgb) {
    }

    record Model(List<Element> elements, double[] rotation, double[] translation, double scale) {
    }

    static double[] nums(String text) {
        String[] parts = text.split(",");
        double[] out = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Double.parseDouble(parts[i].trim());
        }
        return out;
    }

    static Model load(String kind, String texture) throws Exception {
        String json = Files.readString(new File(MODELS, kind + "_full.json").toPath());
        BufferedImage tex = ImageIO.read(new File(TEXTURES, "quinque_" + texture + ".png"));
        List<Element> elements = new ArrayList<>();
        Matcher m = Pattern.compile("\"from\":\\s*\\[([^\\]]*)\\],\\s*\"to\":\\s*\\[([^\\]]*)\\],\\s*\"faces\":\\s*\\{\\s*\"north\":\\s*\\{\\s*\"uv\":\\s*\\[([^\\]]*)\\]")
            .matcher(json);
        while (m.find()) {
            double[] uv = nums(m.group(3));
            int u = (int)((uv[0] + 1.0) / 16.0 * tex.getWidth());
            int v = (int)((uv[1] + 1.0) / 16.0 * tex.getHeight());
            elements.add(new Element(nums(m.group(1)), nums(m.group(2)), tex.getRGB(Math.min(tex.getWidth() - 1, u), Math.min(tex.getHeight() - 1, v)) & 0xFFFFFF));
        }
        Matcher t = Pattern.compile("\"thirdperson_righthand\":\\s*\\{\\s*\"rotation\":\\s*\\[([^\\]]*)\\],\\s*\"translation\":\\s*\\[([^\\]]*)\\],\\s*\"scale\":\\s*\\[([^\\]]*)\\]")
            .matcher(json);
        if (!t.find()) {
            throw new IllegalStateException("no third person transform in " + kind);
        }
        return new Model(elements, nums(t.group(1)), nums(t.group(2)), nums(t.group(3))[0]);
    }

    /** An item model point (pixels, 0..16) to the right arm's own frame (pixels), as the game carries it. */
    static double[] toArm(Model model, double[] v, boolean slim) {
        // The element's own turn: -45 degrees about z through (8, 8, 8).
        double a = Math.toRadians(-45.0);
        double x = v[0] - 8.0, y = v[1] - 8.0;
        double[] p = {8.0 + x * Math.cos(a) - y * Math.sin(a), 8.0 + x * Math.sin(a) + y * Math.cos(a), v[2]};
        // The model's -0.5 shift, in blocks.
        p = new double[] {p[0] / 16.0 - 0.5, p[1] / 16.0 - 0.5, p[2] / 16.0 - 0.5};
        // ItemTransform: translate, rotationXYZ (Rx Ry Rz: Rz first), scale.
        p = scale(p, model.scale);
        p = rz(p, Math.toRadians(model.rotation[2]));
        p = ry(p, Math.toRadians(model.rotation[1]));
        p = rx(p, Math.toRadians(model.rotation[0]));
        p = add(p, new double[] {model.translation[0] / 16.0, model.translation[1] / 16.0, model.translation[2] / 16.0});
        // ItemInHandLayer: Rx(-90) Ry(180), then (1/16, 0.125, -0.625).
        p = add(p, new double[] {1.0 / 16.0, 0.125, -0.625});
        p = ry(p, Math.toRadians(180.0));
        p = rx(p, Math.toRadians(-90.0));
        double[] px = scale(p, 16.0);
        if (slim) {
            px[0] += 0.5; // translateToHand moves a slim arm's pivot half a pixel (the part's own frame, near enough at the hand)
        }
        return px;
    }

    static double[] rx(double[] p, double a) {
        return new double[] {p[0], p[1] * Math.cos(a) - p[2] * Math.sin(a), p[1] * Math.sin(a) + p[2] * Math.cos(a)};
    }

    static double[] ry(double[] p, double a) {
        return new double[] {p[0] * Math.cos(a) + p[2] * Math.sin(a), p[1], -p[0] * Math.sin(a) + p[2] * Math.cos(a)};
    }

    static double[] rz(double[] p, double a) {
        return new double[] {p[0] * Math.cos(a) - p[1] * Math.sin(a), p[0] * Math.sin(a) + p[1] * Math.cos(a), p[2]};
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] scale(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    /** The held item's triangles in the world (blocks), and a marker cube at each of {@code marks} (arm-frame pixels of the given part). */
    static List<PoseRender.Tri> item(RigPose pose, Model model) {
        List<PoseRender.Tri> tris = new ArrayList<>();
        for (Element e : model.elements()) {
            double[][] c = new double[8][];
            for (int i = 0; i < 8; i++) {
                double[] v = {(i & 1) == 0 ? e.from()[0] : e.to()[0], (i & 2) == 0 ? e.from()[1] : e.to()[1], (i & 4) == 0 ? e.from()[2] : e.to()[2]};
                c[i] = PoseRender.point(pose, "rightArm", toArm(model, v, false));
            }
            box(tris, c, e.rgb());
        }
        return tris;
    }

    static void box(List<PoseRender.Tri> tris, double[][] c, int rgb) {
        int[][] faces = {{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
        double[] mid = {0, 0, 0};
        for (double[] p : c) {
            mid = add(mid, scale(p, 1.0 / 8.0));
        }
        for (int[] f : faces) {
            double[] a = c[f[0]], b = c[f[1]], cc = c[f[2]], d = c[f[3]];
            double[] n = PoseRender.norm(PoseRender.cross(PoseRender.sub(b, a), PoseRender.sub(d, a)));
            double[] centre = scale(add(add(a, b), add(cc, d)), 0.25);
            if (PoseRender.dot(n, PoseRender.sub(centre, mid)) < 0) {
                n = scale(n, -1.0);
            }
            tris.add(new PoseRender.Tri(new double[][] {a, b, cc}, rgb, n));
            tris.add(new PoseRender.Tri(new double[][] {a, cc, d}, rgb, n));
        }
    }

    /** A small cube of {@code rgb} at the world point {@code p} (a marker). */
    static void mark(List<PoseRender.Tri> tris, double[] p, int rgb) {
        double s = 0.025;
        double[][] c = new double[8][];
        for (int i = 0; i < 8; i++) {
            c[i] = new double[] {p[0] + ((i & 1) == 0 ? -s : s), p[1] + ((i & 2) == 0 ? -s : s), p[2] + ((i & 4) == 0 ? -s : s)};
        }
        box(tris, c, rgb);
    }

    static final String[][] KINDS = {{"koukaku", "koukaku"}, {"katana", "katana"}, {"mace", "mace"}, {"launcher", "launcher"}, {"bikaku", "bikaku"}};
    static final Quinque[] QUINQUES = {Quinque.SPEAR, Quinque.KATANA, Quinque.MACE, Quinque.LAUNCHER, Quinque.BLADE};

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : ".");
        dir.mkdirs();
        PoseRender.stiff = true;
        double[][] views = {{-40, 12}, {-90, 5}, {200, 15}};
        int cell = 300;
        for (int k = 0; k < KINDS.length; k++) {
            Model model = load(KINDS[k][0], KINDS[k][1]);
            Quinque q = QUINQUES[k];
            // Columns: the stance, then each of its three swings at its windup, its cut and its follow through.
            List<RigPose> poses = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            RigPose hold = QuinquePose.hold(q);
            poses.add(hold == null ? QuinquePose.swing(q, 0, 0.0) : hold);
            labels.add("stance");
            for (int step = 0; step < 3; step++) {
                for (double t : new double[] {0.35, 0.55, 0.8}) {
                    poses.add(QuinquePose.swing(q, step, t));
                    labels.add("M1 " + (step + 1) + " t" + t);
                }
            }
            BufferedImage sheet = new BufferedImage(cell * poses.size(), cell * views.length, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = sheet.createGraphics();
            for (int i = 0; i < poses.size(); i++) {
                RigPose pose = poses.get(i);
                List<PoseRender.Tri> extra = item(pose, model);
                for (int v = 0; v < views.length; v++) {
                    PoseRender.Camera cam = PoseRender.orbit(views[v][0], views[v][1], 4.4, 1.0);
                    g.drawImage(PoseRender.render(pose, cam, cell, cell, extra), i * cell, v * cell, null);
                }
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                g.drawString(KINDS[k][0] + " " + labels.get(i), i * cell + 6, 16);
            }
            g.dispose();
            File out = new File(dir, "quinque-" + KINDS[k][0] + ".png");
            ImageIO.write(sheet, "png", out);
            System.out.println("wrote " + out);
        }
    }
}
