import com.hex.kagune.control.client.animation.FirstPersonQuinque;
import com.hex.kagune.control.client.animation.QuinqueGrip;
import com.hex.kagune.control.client.animation.QuinqueHolds;
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
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * 4.1.19: first person off-game, as ArmItemLayer.FirstPerson draws it: the held quinque's real model (textured, face by face) placed by
 * {@link FirstPersonQuinque#held} (the code the game's chain is held to: the third person chain on the posed body, its step, seen from
 * behind the head, looking down, zoomed), 16:9, the hand's own 70 degree view, the ground 1.62 below the eye. Run a quick check first:
 * <pre>
 * javac -nowarn -cp build/quick/classes:build/quick/base.jar -d build/fp-preview tools/PoseRender.java tools/QuinquePreview.java tools/FirstPersonPreview.java
 * java -Djava.awt.headless=true -cp build/fp-preview:build/quick/classes:build/quick/base.jar FirstPersonPreview &lt;out-dir&gt; &lt;kind&gt;
 * </pre>
 * kind: katana, koukaku (the spear), mace, dawn, bikaku (the blade). Writes fp-KIND-stance.png (the draw out of its case and the stance)
 * and fp-KIND-mN.png (each swing at eight moments).
 */
public final class FirstPersonPreview {
    static final int W = 320;
    static final int H = 180;
    static final double[] TIMES = {0.0, 0.15, 0.3, 0.42, 0.5, 0.62, 0.75, 0.88};

    /** A model face: its four corners (item pixels, the element's own turn applied) and its uv box. */
    record Face(double[][] corners, double[] uv) {
    }

    static final Map<String, String[][]> FACES = Map.of(
        "north", new String[][] {{"t", "t", "f"}, {"f", "t", "f"}, {"f", "f", "f"}, {"t", "f", "f"}},
        "south", new String[][] {{"f", "t", "t"}, {"t", "t", "t"}, {"t", "f", "t"}, {"f", "f", "t"}},
        "east", new String[][] {{"t", "t", "t"}, {"t", "t", "f"}, {"t", "f", "f"}, {"t", "f", "t"}},
        "west", new String[][] {{"f", "t", "f"}, {"f", "t", "t"}, {"f", "f", "t"}, {"f", "f", "f"}},
        "up", new String[][] {{"f", "t", "f"}, {"t", "t", "f"}, {"t", "t", "t"}, {"f", "t", "t"}},
        "down", new String[][] {{"f", "f", "t"}, {"t", "f", "t"}, {"t", "f", "f"}, {"f", "f", "f"}});

    @SuppressWarnings("unchecked")
    static List<Face> faces(String kind, String frame) throws Exception {
        Map<String, Object> json = (Map<String, Object>)new QuinquePreview.Json(Files.readString(new File(QuinquePreview.MODELS, kind + "_" + frame + ".json").toPath())).value();
        List<Face> out = new ArrayList<>();
        for (Object o : (List<Object>)json.get("elements")) {
            Map<String, Object> e = (Map<String, Object>)o;
            double[] from = QuinquePreview.nums(e.get("from"));
            double[] to = QuinquePreview.nums(e.get("to"));
            Map<String, Object> r = (Map<String, Object>)e.get("rotation");
            Map<String, Object> fs = (Map<String, Object>)e.get("faces");
            for (Map.Entry<String, String[][]> f : FACES.entrySet()) {
                Map<String, Object> face = (Map<String, Object>)fs.get(f.getKey());
                if (face == null) {
                    continue;
                }
                double[][] c = new double[4][];
                for (int k = 0; k < 4; k++) {
                    double[] p = new double[3];
                    for (int i = 0; i < 3; i++) {
                        p[i] = (f.getValue()[k][i].equals("f") ? from : to)[i];
                    }
                    if (r != null) {
                        double a = Math.toRadians((Double)r.get("angle"));
                        double[] origin = QuinquePreview.nums(r.get("origin"));
                        double[] q = {p[0] - origin[0], p[1] - origin[1], p[2] - origin[2]};
                        char axis = ((String)r.get("axis")).charAt(0);
                        q = axis == 'x' ? QuinquePreview.rx(q, a) : axis == 'y' ? QuinquePreview.ry(q, a) : QuinquePreview.rz(q, a);
                        p = new double[] {q[0] + origin[0], q[1] + origin[1], q[2] + origin[2]};
                    }
                    c[k] = p;
                }
                out.add(new Face(c, QuinquePreview.nums(face.get("uv"))));
            }
        }
        return out;
    }

    /** The held quinque's textured cells in camera space for {@code body}, as the game draws them. */
    static List<PoseRender.Tri> item(RigPose body, QuinqueHolds.Hold h, List<Face> faces, BufferedImage tex) {
        List<PoseRender.Tri> tris = new ArrayList<>();
        int tw = tex.getWidth();
        for (Face f : faces) {
            double[][] c = new double[4][];
            for (int k = 0; k < 4; k++) {
                c[k] = FirstPersonQuinque.held(body, h, false, new float[] {(float)f.corners[k][0], (float)f.corners[k][1], (float)f.corners[k][2]});
            }
            double[] n = PoseRender.norm(PoseRender.cross(PoseRender.sub(c[1], c[0]), PoseRender.sub(c[3], c[0])));
            double[] mid = PoseRender.scale(PoseRender.add(PoseRender.add(c[0], c[1]), PoseRender.add(c[2], c[3])), 0.25);
            if (PoseRender.dot(n, mid) > 0.0) {
                n = PoseRender.scale(n, -1.0); // lit as the side the camera sees
            }
            int nu = (int)Math.max(1, Math.min(16, Math.ceil(Math.abs(f.uv[2] - f.uv[0]) * tw / 16.0)));
            int nv = (int)Math.max(1, Math.min(16, Math.ceil(Math.abs(f.uv[3] - f.uv[1]) * tw / 16.0)));
            for (int i = 0; i < nu; i++) {
                for (int j = 0; j < nv; j++) {
                    double u = f.uv[0] + (f.uv[2] - f.uv[0]) * (i + 0.5) / nu;
                    double v = f.uv[1] + (f.uv[3] - f.uv[1]) * (j + 0.5) / nv;
                    int argb = tex.getRGB(Math.max(0, Math.min(tw - 1, (int)(u / 16.0 * tw))),
                        Math.max(0, Math.min(tex.getHeight() - 1, (int)(v / 16.0 * tex.getHeight()))));
                    if ((argb >>> 24) < 10) {
                        continue;
                    }
                    double[] a = at(c, (double)i / nu, (double)j / nv);
                    double[] b = at(c, (double)(i + 1) / nu, (double)j / nv);
                    double[] cc = at(c, (double)(i + 1) / nu, (double)(j + 1) / nv);
                    double[] d = at(c, (double)i / nu, (double)(j + 1) / nv);
                    tris.add(new PoseRender.Tri(new double[][] {a, b, cc}, argb & 0xFFFFFF, n));
                    tris.add(new PoseRender.Tri(new double[][] {a, cc, d}, argb & 0xFFFFFF, n));
                }
            }
        }
        return tris;
    }

    static double[] at(double[][] c, double a, double b) {
        double[] out = new double[3];
        for (int q = 0; q < 3; q++) {
            double top = c[0][q] + (c[1][q] - c[0][q]) * a;
            double bottom = c[3][q] + (c[2][q] - c[3][q]) * a;
            out[q] = top + (bottom - top) * b;
        }
        return out;
    }

    static BufferedImage frame(RigPose body, Quinque q, List<Face> faces, BufferedImage tex, String label) {
        QuinqueHolds.Hold h = QuinqueHolds.of(q);
        PoseRender.Canvas canvas = new PoseRender.Canvas(W, H, 2,
            new PoseRender.Camera(new double[] {0, 0, 0}, new double[] {0, 0, -1}, Math.toDegrees(FirstPersonQuinque.FOV)), 0x8CA8C8);
        // The ground 1.62 blocks below the eye (the world does not tip with the weapon's view).
        for (int i = -12; i < 12; i++) {
            for (int j = -24; j < 0; j++) {
                int col = ((i + j) & 1) == 0 ? 0x5E7A46 : 0x56703F;
                double y = -1.62;
                double[] a = {i, y, j};
                double[] b = {i + 1, y, j};
                double[] c = {i + 1, y, j + 1};
                double[] d = {i, y, j + 1};
                canvas.tri(new PoseRender.Tri(new double[][] {a, b, c}, col, new double[] {0, 1, 0}), false);
                canvas.tri(new PoseRender.Tri(new double[][] {a, c, d}, col, new double[] {0, 1, 0}), false);
            }
        }
        for (PoseRender.Tri t : item(body, h, faces, tex)) {
            canvas.tri(t, true);
        }
        BufferedImage img = canvas.image();
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.drawLine(W / 2 - 5, H / 2, W / 2 + 5, H / 2);
        g.drawLine(W / 2, H / 2 - 5, W / 2, H / 2 + 5);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g.drawString(label, 5, H - 6);
        g.dispose();
        return img;
    }

    static void sheet(File out, List<BufferedImage> frames, int cols) throws Exception {
        int rows = (frames.size() + cols - 1) / cols;
        BufferedImage s = new BufferedImage(cols * W, rows * H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = s.createGraphics();
        for (int i = 0; i < frames.size(); i++) {
            g.drawImage(frames.get(i), i % cols * W, i / cols * H, null);
        }
        g.setColor(Color.BLACK);
        for (int c = 1; c < cols; c++) {
            g.drawLine(c * W, 0, c * W, rows * H);
        }
        for (int r = 1; r < rows; r++) {
            g.drawLine(0, r * H, cols * W, r * H);
        }
        g.dispose();
        ImageIO.write(s, "png", out);
        System.out.println("wrote " + out);
    }

    static final double[] MATCH = {0.15, 0.3, 0.42, 0.5, 0.62, 0.75};

    /**
     * fp-KIND-match.png: each swing at six moments, third person from behind the right shoulder (QuinquePreview's model on PoseRender's
     * body, the body's whole turn kept) over first person (as the game draws it): the same cut, both ways.
     */
    static void match(File dir, String kind, Quinque q, List<Face> full, BufferedImage tex) throws Exception {
        PoseRender.stiff = true;
        QuinquePreview.Model flat = QuinquePreview.load(kind, kind);
        List<BufferedImage> frames = new ArrayList<>();
        for (int step = 0; step < q.steps(); step++) {
            List<BufferedImage> below = new ArrayList<>();
            for (double t : MATCH) {
                RigPose p = QuinquePose.swing(q, step, t);
                if (q.twoHanded) {
                    QuinqueGrip.reach(p, q, false);
                }
                PoseRender.Camera cam = PoseRender.orbit(200, 12, 4.2, 1.15);
                BufferedImage third = PoseRender.render(p, cam, W, H, QuinquePreview.item(p, flat));
                Graphics2D g = third.createGraphics();
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                g.drawString(kind + " M1 " + (step + 1) + " t" + t + " (third person)", 5, 14);
                g.dispose();
                frames.add(third);
                below.add(frame(p, q, full, tex, "first person"));
            }
            frames.addAll(below);
        }
        sheet(new File(dir, "fp-" + kind + "-match.png"), frames, MATCH.length);
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        dir.mkdirs();
        String kind = args[1];
        String[] kinds = {"koukaku", "katana", "mace", "launcher", "bikaku", "dawn"};
        Quinque[] qs = {Quinque.SPEAR, Quinque.KATANA, Quinque.MACE, Quinque.LAUNCHER, Quinque.BLADE, Quinque.DAWN};
        Quinque q = null;
        for (int k = 0; k < kinds.length; k++) {
            if (kinds[k].equals(kind)) {
                q = qs[k];
            }
        }
        BufferedImage tex = ImageIO.read(new File(QuinquePreview.TEXTURES, "quinque_" + kind + ".png"));
        List<Face> full = faces(kind, "full");
        if (args.length > 2 && args[2].equals("match")) {
            match(dir, kind, q, full, tex);
            return;
        }
        // The draw out of the case (QuinquePose.unfolding, with the frames the item shows) and the stance.
        List<BufferedImage> rest = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            RigPose u = QuinquePose.unfolding(q, i / 7.0F);
            rest.add(frame(u, q, faces(kind, "out_" + i), tex, kind + " draw " + i + "/7"));
        }
        RigPose stance = QuinquePose.hold(q);
        if (q.twoHanded) {
            QuinqueGrip.reach(stance, q, false);
        }
        rest.add(frame(stance, q, full, tex, kind + " stance"));
        sheet(new File(dir, "fp-" + kind + "-stance.png"), rest, 4);
        for (int step = 0; step < q.steps(); step++) {
            List<BufferedImage> frames = new ArrayList<>();
            for (double t : TIMES) {
                RigPose p = QuinquePose.swing(q, step, t);
                if (q.twoHanded) {
                    QuinqueGrip.reach(p, q, false);
                }
                frames.add(frame(p, q, full, tex, kind + " M1 " + (step + 1) + " t" + t));
            }
            sheet(new File(dir, "fp-" + kind + "-m" + (step + 1) + ".png"), frames, 4);
        }
    }
}
