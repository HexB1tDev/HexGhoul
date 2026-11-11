import com.hex.kagune.control.client.animation.FirstPersonQuinque;
import com.hex.kagune.control.client.animation.QuinqueGrip;
import com.hex.kagune.control.client.animation.QuinqueHolds;
import com.hex.kagune.control.client.animation.QuinquePose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.quinque.ArmMesh;
import com.hex.kagune.control.quinque.Quinque;
import com.hex.kagune.control.quinque.QuinqueMesh;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.imageio.ImageIO;

/**
 * 4.1.20: a quinque grown as a mesh ({@link QuinqueMesh}), off-game: alone from four sides and close up (look), held in third person front
 * and side through its stance and swings (held), the draw out of its case (draw), and in first person as the game draws it (first: through
 * {@link FirstPersonQuinque}). Run a quick check first, then:
 * <pre>
 * javac -nowarn -cp build/quick/classes:build/quick/base.jar -d build/fp-preview tools/PoseRender.java tools/QuinquePreview.java tools/FirstPersonPreview.java tools/QuinqueMeshPreview.java
 * java -Djava.awt.headless=true -cp build/fp-preview:build/quick/classes:build/quick/base.jar QuinqueMeshPreview &lt;out-dir&gt; KATANA
 * </pre>
 */
public final class QuinqueMeshPreview {
    /**
     * The mesh's triangles, each vertex placed by {@code place} (its item model's pixels in), {@code out} of the way out of its case (the
     * weapon drawn in toward the fist, the small case round the grip), each quad's colour its corners' mean; glowing quads flagged by an
     * odd colour's low bit (drawn unlit).
     */
    static List<PoseRender.Tri> tris(Quinque q, float out, Function<float[], double[]> place, List<Boolean> glows) {
        List<PoseRender.Tri> tris = new ArrayList<>();
        ArmMesh.Mesh mesh = QuinqueMesh.of(q);
        add(tris, glows, mesh, v -> place.apply(QuinqueMesh.turned(q, QuinqueMesh.drawn(q, v, out))));
        float[] box = QuinqueMesh.caseBox(q, out);
        if (box != null) {
            add(tris, glows, QuinqueMesh.caseMesh(), v -> place.apply(QuinqueMesh.turned(new float[] {
                box[0] + (v[0] + 1.0F) * 0.5F * (box[3] - box[0]), box[1] + (v[1] + 1.0F) * 0.5F * (box[4] - box[1]),
                box[2] + (v[2] + 1.0F) * 0.5F * (box[5] - box[2])})));
        }
        return tris;
    }

    static void add(List<PoseRender.Tri> tris, List<Boolean> glows, ArmMesh.Mesh mesh, Function<float[], double[]> place) {
        for (int q = 0; q < mesh.quads; q++) {
            double[][] c = new double[4][];
            int r = 0;
            int g = 0;
            int b = 0;
            for (int k = 0; k < 4; k++) {
                int v = q * 4 + k;
                c[k] = place.apply(new float[] {mesh.pos[v * 3], mesh.pos[v * 3 + 1], mesh.pos[v * 3 + 2]});
                r += mesh.argb[v] >> 16 & 255;
                g += mesh.argb[v] >> 8 & 255;
                b += mesh.argb[v] & 255;
            }
            int rgb = (r / 4) << 16 | (g / 4) << 8 | b / 4;
            double[] n = PoseRender.norm(PoseRender.cross(PoseRender.sub(c[2], c[0]), PoseRender.sub(c[3], c[1])));
            // Two-sided, as the game draws it (no culling): lit from the side facing the viewer, decided at drawing.
            tris.add(new PoseRender.Tri(new double[][] {c[0], c[1], c[2]}, rgb, n));
            tris.add(new PoseRender.Tri(new double[][] {c[0], c[2], c[3]}, rgb, n));
            glows.add(mesh.glow[q]);
            glows.add(mesh.glow[q]);
        }
    }

    /** Draws {@code tris} (lit two-sided, glowing ones full bright) on {@code canvas} seen from {@code eye}. */
    static void draw(PoseRender.Canvas canvas, List<PoseRender.Tri> tris, List<Boolean> glows, double[] eye) {
        for (int i = 0; i < tris.size(); i++) {
            PoseRender.Tri t = tris.get(i);
            double[] mid = PoseRender.scale(PoseRender.add(PoseRender.add(t.v()[0], t.v()[1]), t.v()[2]), 1.0 / 3.0);
            double[] n = t.n();
            if (PoseRender.dot(n, PoseRender.sub(eye, mid)) < 0.0) {
                n = PoseRender.scale(n, -1.0);
            }
            canvas.tri(new PoseRender.Tri(t.v(), t.rgb(), n), !glows.get(i));
        }
    }

    static final int W = 420;
    static final int H = 420;

    /** The mesh alone (item pixels shown as blocks / 16, centred), from {@code yaw}/{@code pitch}, {@code fov} wide, aimed at {@code at}. */
    static BufferedImage alone(Quinque q, double yaw, double pitch, double fov, double[] at, String label, int w, int h) {
        List<Boolean> glows = new ArrayList<>();
        // Standing (the turn undone), to look at it.
        List<PoseRender.Tri> tris = tris(q, 1.0F, v -> {
            double a = Math.toRadians(-QuinqueMesh.DIAGONAL);
            double x = v[0] - 8.0;
            double y = v[1] - 8.0;
            return new double[] {(8.0 + x * Math.cos(a) - y * Math.sin(a)) / 16.0, (8.0 + x * Math.sin(a) + y * Math.cos(a)) / 16.0, v[2] / 16.0};
        }, glows);
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        double d = 9.0;
        double[] target = {at[0] / 16.0, at[1] / 16.0, at[2] / 16.0};
        double[] eye = {target[0] + Math.sin(y) * Math.cos(p) * d, target[1] + Math.sin(p) * d, target[2] + Math.cos(y) * Math.cos(p) * d};
        PoseRender.Canvas canvas = new PoseRender.Canvas(w, h, 2, new PoseRender.Camera(eye, target, fov), 0x24262C);
        draw(canvas, tris, glows, eye);
        BufferedImage img = canvas.image();
        label(img, label);
        return img;
    }

    static void label(BufferedImage img, String text) {
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.drawString(text, 6, 16);
        g.dispose();
    }

    /** {@code body} holding it (third person), from {@code yaw} round him. */
    static BufferedImage held(Quinque q, RigPose body, float out, double yaw, String text) {
        QuinqueHolds.Hold h = QuinqueHolds.of(q);
        List<Boolean> glows = new ArrayList<>();
        List<PoseRender.Tri> tris = tris(q, out, v -> PoseRender.world(body, QuinqueGrip.held(body, h, false, v)), glows);
        PoseRender.Camera cam = PoseRender.orbit(yaw, 10, 5.2, 1.05);
        PoseRender.Canvas canvas = new PoseRender.Canvas(300, 300, 2, cam, 0x3A4450);
        canvas.floor(6, 0x58626C, 0x4E5862);
        for (PoseRender.Tri t : PoseRender.model(body)) {
            canvas.tri(t, true);
        }
        draw(canvas, tris, glows, cam.eye());
        BufferedImage img = canvas.image();
        label(img, text);
        return img;
    }

    /** First person as the game draws it (FirstPersonQuinque). */
    static BufferedImage first(Quinque q, RigPose body, float out, String text) {
        QuinqueHolds.Hold h = QuinqueHolds.of(q);
        List<Boolean> glows = new ArrayList<>();
        List<PoseRender.Tri> tris = tris(q, out, v -> FirstPersonQuinque.held(body, h, false, v), glows);
        int w = 320;
        int hh = 180;
        PoseRender.Canvas canvas = new PoseRender.Canvas(w, hh, 2,
            new PoseRender.Camera(new double[] {0, 0, 0}, new double[] {0, 0, -1}, Math.toDegrees(FirstPersonQuinque.FOV)), 0x8CA8C8);
        for (int i = -12; i < 12; i++) {
            for (int j = -24; j < 0; j++) {
                int col = ((i + j) & 1) == 0 ? 0x5E7A46 : 0x56703F;
                double[] a = {i, -1.62, j};
                double[] b = {i + 1, -1.62, j};
                double[] c = {i + 1, -1.62, j + 1};
                double[] d = {i, -1.62, j + 1};
                canvas.tri(new PoseRender.Tri(new double[][] {a, b, c}, col, new double[] {0, 1, 0}), false);
                canvas.tri(new PoseRender.Tri(new double[][] {a, c, d}, col, new double[] {0, 1, 0}), false);
            }
        }
        draw(canvas, tris, glows, new double[] {0, 0, 0});
        BufferedImage img = canvas.image();
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.drawLine(w / 2 - 5, hh / 2, w / 2 + 5, hh / 2);
        g.drawLine(w / 2, hh / 2 - 5, w / 2, hh / 2 + 5);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g.drawString(text, 5, hh - 6);
        g.dispose();
        return img;
    }

    static void sheet(File out, List<BufferedImage> frames, int cols) throws Exception {
        int w = frames.get(0).getWidth();
        int h = frames.get(0).getHeight();
        int rows = (frames.size() + cols - 1) / cols;
        BufferedImage s = new BufferedImage(cols * w, rows * h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = s.createGraphics();
        for (int i = 0; i < frames.size(); i++) {
            g.drawImage(frames.get(i), i % cols * w, i / cols * h, null);
        }
        g.dispose();
        ImageIO.write(s, "png", out);
        System.out.println("wrote " + out);
    }

    static RigPose posed(Quinque q, RigPose p) {
        if (q.twoHanded) {
            QuinqueGrip.reach(p, q, false);
        }
        return p;
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        dir.mkdirs();
        Quinque q = Quinque.valueOf(args[1]);
        PoseRender.stiff = true;
        ArmMesh.Mesh mesh = QuinqueMesh.of(q);
        System.out.println(q + ": " + mesh.quads + " quads");
        // Alone: its whole length from its flat, from its edge, from above its back, three-quarter; then the hilt and guard close.
        List<BufferedImage> look = new ArrayList<>();
        look.add(alone(q, 0, 0, 17, new double[] {8, 13.5, 8}, q + " flat (" + mesh.quads + " quads)", W, H));
        look.add(alone(q, 90, 0, 17, new double[] {8, 13.5, 8}, "edge on", W, H));
        look.add(alone(q, 35, 25, 17, new double[] {8, 13.5, 8}, "three-quarter", W, H));
        look.add(alone(q, -60, -15, 17, new double[] {8, 13.5, 8}, "from below, behind", W, H));
        look.add(alone(q, 20, 15, 11, new double[] {8, 2.0, 8}, "the hilt", W, H));
        look.add(alone(q, -30, 30, 9, new double[] {8, 7.0, 8}, "the guard and its collar", W, H));
        look.add(alone(q, 150, 20, 9, new double[] {8, 7.0, 8}, "the guard, the other side", W, H));
        look.add(alone(q, 10, 5, 12, new double[] {8, 29.0, 8}, "the point", W, H));
        sheet(new File(dir, "mesh-" + q.name().toLowerCase() + "-look.png"), look, 4);
        // Held: the stance and each swing's windup, impact and follow through, from the front and the side.
        List<BufferedImage> heldFrames = new ArrayList<>();
        List<RigPose> poses = new ArrayList<>();
        List<String> names = new ArrayList<>();
        poses.add(posed(q, QuinquePose.hold(q)));
        names.add("stance");
        for (int step = 0; step < q.steps(); step++) {
            for (double t : new double[] {0.3, 0.5, 0.62}) {
                poses.add(posed(q, QuinquePose.swing(q, step, t)));
                names.add("M1 " + (step + 1) + " t" + t);
            }
        }
        for (int i = 0; i < poses.size(); i++) {
            heldFrames.add(held(q, poses.get(i), 1.0F, -30, names.get(i) + " (front)"));
        }
        for (int i = 0; i < poses.size(); i++) {
            heldFrames.add(held(q, poses.get(i), 1.0F, -100, names.get(i) + " (side)"));
        }
        sheet(new File(dir, "mesh-" + q.name().toLowerCase() + "-held.png"), heldFrames, poses.size());
        // The draw: out of its case, front and in first person.
        List<BufferedImage> draw = new ArrayList<>();
        float[] outs = {0.12F, 0.3F, 0.5F, 0.7F, 0.88F, 1.0F};
        for (float o : outs) {
            RigPose u = QuinquePose.unfolding(q, o);
            draw.add(held(q, u, o, -30, "draw " + o));
        }
        sheet(new File(dir, "mesh-" + q.name().toLowerCase() + "-draw.png"), draw, outs.length);
        List<BufferedImage> drawFirst = new ArrayList<>();
        for (float o : outs) {
            drawFirst.add(first(q, QuinquePose.unfolding(q, o), o, "first person draw " + o));
        }
        sheet(new File(dir, "mesh-" + q.name().toLowerCase() + "-draw-first.png"), drawFirst, 3);
        // First person through every swing.
        for (int step = 0; step < q.steps(); step++) {
            List<BufferedImage> fp = new ArrayList<>();
            for (double t : new double[] {0.0, 0.15, 0.3, 0.42, 0.5, 0.62, 0.75, 0.88}) {
                fp.add(first(q, posed(q, QuinquePose.swing(q, step, t)), 1.0F, "M1 " + (step + 1) + " t" + t));
            }
            sheet(new File(dir, "mesh-" + q.name().toLowerCase() + "-first-m" + (step + 1) + ".png"), fp, 4);
        }
    }
}
