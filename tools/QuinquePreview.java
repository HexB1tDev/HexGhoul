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

    /** An element: its box, its colour (its north face's patch) and its own turn (angle degrees about axis x/y/z through origin). */
    record Element(double[] from, double[] to, int rgb, double angle, char axis, double[] origin) {
    }

    record Model(List<Element> elements, double[] rotation, double[] translation, double scale) {
    }

    /** A small JSON reader (objects as maps, arrays as lists, numbers as doubles): enough for a model file. */
    static final class Json {
        final String s;
        int i;

        Json(String s) {
            this.s = s;
        }

        Object value() {
            skip();
            char c = this.s.charAt(this.i);
            if (c == '{') {
                java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                this.i++;
                skip();
                if (this.s.charAt(this.i) == '}') {
                    this.i++;
                    return map;
                }
                while (true) {
                    skip();
                    String key = (String)value();
                    skip();
                    this.i++; // :
                    map.put(key, value());
                    skip();
                    if (this.s.charAt(this.i++) == '}') {
                        return map;
                    }
                }
            }
            if (c == '[') {
                List<Object> list = new ArrayList<>();
                this.i++;
                skip();
                if (this.s.charAt(this.i) == ']') {
                    this.i++;
                    return list;
                }
                while (true) {
                    list.add(value());
                    skip();
                    if (this.s.charAt(this.i++) == ']') {
                        return list;
                    }
                }
            }
            if (c == '"') {
                int end = this.s.indexOf('"', this.i + 1);
                String text = this.s.substring(this.i + 1, end);
                this.i = end + 1;
                return text;
            }
            int start = this.i;
            while (this.i < this.s.length() && "-+.eE0123456789truefalsn".indexOf(this.s.charAt(this.i)) >= 0) {
                this.i++;
            }
            String word = this.s.substring(start, this.i);
            return word.equals("true") || word.equals("false") ? (Object)Boolean.valueOf(word) : (Object)Double.valueOf(word);
        }

        void skip() {
            while (this.i < this.s.length() && Character.isWhitespace(this.s.charAt(this.i))) {
                this.i++;
            }
        }
    }

    @SuppressWarnings("unchecked")
    static double[] nums(Object list) {
        List<Object> l = (List<Object>)list;
        double[] out = new double[l.size()];
        for (int k = 0; k < out.length; k++) {
            out[k] = (Double)l.get(k);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static Model load(String kind, String texture) throws Exception {
        java.util.Map<String, Object> json = (java.util.Map<String, Object>)new Json(Files.readString(new File(MODELS, kind + "_full.json").toPath())).value();
        BufferedImage tex = ImageIO.read(new File(TEXTURES, "quinque_" + texture + ".png"));
        List<Element> elements = new ArrayList<>();
        for (Object o : (List<Object>)json.get("elements")) {
            java.util.Map<String, Object> e = (java.util.Map<String, Object>)o;
            double[] uv = nums(((java.util.Map<String, Object>)((java.util.Map<String, Object>)e.get("faces")).get("north")).get("uv"));
            int u = (int)((uv[0] + 1.0) / 16.0 * tex.getWidth());
            int v = (int)((uv[1] + 1.0) / 16.0 * tex.getHeight());
            int rgb = tex.getRGB(Math.min(tex.getWidth() - 1, u), Math.min(tex.getHeight() - 1, v)) & 0xFFFFFF;
            java.util.Map<String, Object> r = (java.util.Map<String, Object>)e.get("rotation");
            double angle = r == null ? 0.0 : (Double)r.get("angle");
            char axis = r == null ? 'z' : ((String)r.get("axis")).charAt(0);
            double[] origin = r == null ? new double[] {8, 8, 8} : nums(r.get("origin"));
            elements.add(new Element(nums(e.get("from")), nums(e.get("to")), rgb, angle, axis, origin));
        }
        java.util.Map<String, Object> third = (java.util.Map<String, Object>)((java.util.Map<String, Object>)json.get("display")).get("thirdperson_righthand");
        return new Model(elements, nums(third.get("rotation")), nums(third.get("translation")), nums(third.get("scale"))[0]);
    }

    /** An item model point (pixels, 0..16) to the right arm's own frame (pixels), as the game carries it. */
    static double[] toArm(Model model, Element e, double[] v, boolean slim) {
        // The element's own turn.
        double a = Math.toRadians(e.angle());
        double[] o = e.origin();
        double[] q = {v[0] - o[0], v[1] - o[1], v[2] - o[2]};
        q = e.axis() == 'x' ? rx(q, a) : e.axis() == 'y' ? ry(q, a) : rz(q, a);
        double[] p = {q[0] + o[0], q[1] + o[1], q[2] + o[2]};
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
        return scale(p, 16.0);
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
                c[i] = PoseRender.point(pose, "rightArm", toArm(model, e, v, false));
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

    static final double[] TIMES = {0.3, 0.42, 0.5, 0.6, 0.75};
    static final String[][] KINDS = {{"koukaku", "koukaku"}, {"katana", "katana"}, {"mace", "mace"}, {"launcher", "launcher"}, {"bikaku", "bikaku"}, {"dawn", "dawn"}};
    static final Quinque[] QUINQUES = {Quinque.SPEAR, Quinque.KATANA, Quinque.MACE, Quinque.LAUNCHER, Quinque.BLADE, Quinque.DAWN};

    /**
     * Writes, per quinque, quinque-KIND.png (its stance and every swing in a row, three views), and for a closer look
     * quinque-KIND-stance.png and quinque-KIND-m1..m3.png (larger, two views). An optional second argument picks one kind.
     */
    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : ".");
        String only = args.length > 1 ? args[1] : null;
        dir.mkdirs();
        PoseRender.stiff = true;
        for (int k = 0; k < KINDS.length; k++) {
            if (only != null && !only.equals(KINDS[k][0])) {
                continue;
            }
            Model model = load(KINDS[k][0], KINDS[k][1]);
            Quinque q = QUINQUES[k];
            List<List<RigPose>> rows = new ArrayList<>();
            List<List<String>> names = new ArrayList<>();
            RigPose hold = QuinquePose.hold(q);
            rows.add(new ArrayList<>(List.of(hold == null ? QuinquePose.swing(q, 0, 0.0) : hold)));
            names.add(new ArrayList<>(List.of("stance")));
            if (q == Quinque.LAUNCHER) {
                RigPose kick = QuinquePose.hold(q);
                QuinquePose.recoil(kick, q, 1.0F);
                rows.get(0).add(kick);
                names.get(0).add("recoil");
            }
            for (int step = 0; step < 3; step++) {
                List<RigPose> row = new ArrayList<>();
                List<String> label = new ArrayList<>();
                for (double t : TIMES) {
                    row.add(QuinquePose.swing(q, step, t));
                    label.add("M1 " + (step + 1) + " t" + t);
                }
                rows.add(row);
                names.add(label);
                if (q == Quinque.LAUNCHER) {
                    break;
                }
            }
            float worst = 0.0F;
            for (int r = 0; r < rows.size(); r++) {
                for (int i = 0; i < rows.get(r).size(); i++) {
                    float miss = q.twoHanded ? com.hex.kagune.control.client.animation.QuinqueGrip.reach(rows.get(r).get(i), q, false) : 0.0F;
                    worst = Math.max(worst, Math.abs(miss));
                    if (Math.abs(miss) > 0.05F) {
                        names.get(r).set(i, names.get(r).get(i) + String.format(" miss %+.1f", miss));
                    }
                }
            }
            System.out.println(q + ": the left fist misses its grip by at most " + worst + " px (the shoulder reaching 1.5 of it)");
            for (int r = 0; r < rows.size(); r++) {
                String name = r == 0 ? "stance" : "m" + r;
                double[][] views = r == 0 ? new double[][] {{-35, 8}, {-100, 4}, {60, 10}, {200, 15}} : new double[][] {{-35, 10}, {-95, 4}, {40, 10}};
                sheet(new File(dir, "quinque-" + KINDS[k][0] + "-" + name + ".png"), KINDS[k][0], q, model, rows.get(r), names.get(r), views, 300);
            }
        }
    }

    static void sheet(File out, String kind, Quinque q, Model model, List<RigPose> poses, List<String> labels, double[][] views, int cell)
        throws Exception {
        BufferedImage sheet = new BufferedImage(cell * poses.size(), cell * views.length, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        for (int i = 0; i < poses.size(); i++) {
            RigPose pose = poses.get(i);
            List<PoseRender.Tri> extra = item(pose, model);
            double[] grip = q.twoHanded ? com.hex.kagune.control.client.animation.QuinqueGrip.leftGrip(pose, q, false) : null;
            if (grip != null) {
                mark(extra, PoseRender.world(pose, grip), 0xFF30FF);
                mark(extra, PoseRender.world(pose, com.hex.kagune.control.client.animation.QuinqueGrip.fist(pose, false, false)), 0xFFE020);
            }
            for (int v = 0; v < views.length; v++) {
                PoseRender.Camera cam = PoseRender.orbit(views[v][0], views[v][1], 5.0, 1.05);
                g.drawImage(PoseRender.render(pose, cam, cell, cell, extra), i * cell, v * cell, null);
            }
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            g.drawString(kind + " " + labels.get(i), i * cell + 6, 16);
        }
        g.dispose();
        ImageIO.write(sheet, "png", out);
        System.out.println("wrote " + out);
    }
}
