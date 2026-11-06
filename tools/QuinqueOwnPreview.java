import com.hex.kagune.control.client.animation.QuinqueOwn;
import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.quinque.ArmMesh;
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
 * 3.1.3: real first-person meshes/model displays in the hand camera, using the production motion/whip path.
 * javac -cp build/quick/classes -d build/own-preview tools/PoseRender.java tools/QuinquePreview.java tools/QuinqueOwnPreview.java
 * java -Djava.awt.headless=true -cp build/own-preview:build/quick/classes QuinqueOwnPreview out.png
 */
public final class QuinqueOwnPreview {
    static final int W = 320, H = 205, BG = 0x303946;
    static final PoseRender.Camera CAMERA = new PoseRender.Camera(new double[] {0, 0, 0}, new double[] {0, 0, -1}, 70);

    static List<PoseRender.Tri> arm(Arm arm, int step, double age) {
        double[] m = QuinqueOwn.arm(arm, step, null, age);
        List<PoseRender.Tri> out = new ArrayList<>();
        ArmMesh.Mesh mesh = ArmMesh.of(arm, false);
        for (int q = 0; q < mesh.quads; q++) {
            double[][] v = new double[4][];
            for (int c = 0; c < 4; c++) {
                int i = (q * 4 + c) * 3;
                v[c] = QuinqueOwn.point(arm, m, new double[] {mesh.pos[i], mesh.pos[i + 1], mesh.pos[i + 2]}, 1, 0);
            }
            quad(out, v, mesh.argb[q * 4] & 0xFFFFFF);
        }
        if (arm == Arm.WHIP) {
            double[][] path = QuinqueOwn.whip(m, step, null, age, false, 1, 0, Math.max(0, age));
            com.hex.kagune.control.RinkakuRibbon.Mesh ribbon = com.hex.kagune.control.RinkakuRibbon.build(path, 0.072F, 0xA0101E, 31,
                new double[] {0, 0, 0}, Math.max(0, age));
            for (int i = 0; i + 1 < ribbon.at().length; i++) {
                for (int side = 0; side < com.hex.kagune.control.RinkakuRibbon.SIDES; side++) {
                    int next = side + 1;
                    quad(out, new double[][] {ribbon.at()[i][side], ribbon.at()[i + 1][side], ribbon.at()[i + 1][next], ribbon.at()[i][next]}, 0xA0101E);
                }
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static List<PoseRender.Tri> legacy(Quinque q, int step, double age) throws Exception {
        QuinquePreview.Model model = QuinquePreview.load(q.model, q.model);
        Map<String, Object> json = (Map<String, Object>)new QuinquePreview.Json(Files.readString(new File(QuinquePreview.MODELS,
            q.model + "_full.json").toPath())).value();
        Map<String, Object> display = (Map<String, Object>)((Map<String, Object>)json.get("display")).get("firstperson_righthand");
        double[] rotation = QuinquePreview.nums(display.get("rotation"));
        double[] translation = QuinquePreview.nums(display.get("translation"));
        double scale = QuinquePreview.nums(display.get("scale"))[0];
        double[] motion = QuinqueOwn.legacy(q, step, age);
        List<PoseRender.Tri> out = new ArrayList<>();
        for (QuinquePreview.Element e : model.elements()) {
            double[][] corners = new double[8][];
            for (int i = 0; i < corners.length; i++) {
                double[] p = {(i & 1) == 0 ? e.from()[0] : e.to()[0], (i & 2) == 0 ? e.from()[1] : e.to()[1],
                    (i & 4) == 0 ? e.from()[2] : e.to()[2]};
                p = PoseRender.sub(p, e.origin());
                p = PoseRender.rotate(p, e.axis() == 'x' ? new double[] {1, 0, 0} : e.axis() == 'y' ? new double[] {0, 1, 0} : new double[] {0, 0, 1},
                    Math.toRadians(e.angle()));
                p = PoseRender.add(p, e.origin());
                p = PoseRender.scale(PoseRender.sub(PoseRender.scale(p, 1.0 / 16), new double[] {0.5, 0.5, 0.5}), scale);
                p = QuinquePreview.rz(p, Math.toRadians(rotation[2]));
                p = QuinquePreview.ry(p, Math.toRadians(rotation[1]));
                p = QuinquePreview.rx(p, Math.toRadians(rotation[0]));
                p = PoseRender.add(p, PoseRender.scale(translation, 1.0 / 16));
                corners[i] = QuinqueOwn.point(null, motion, p, 1, 0);
            }
            QuinquePreview.box(out, corners, e.rgb());
        }
        return out;
    }

    static void quad(List<PoseRender.Tri> out, double[][] v, int rgb) {
        double[] n = PoseRender.norm(PoseRender.cross(PoseRender.sub(v[1], v[0]), PoseRender.sub(v[2], v[0])));
        out.add(new PoseRender.Tri(new double[][] {v[0], v[1], v[2]}, rgb, n));
        out.add(new PoseRender.Tri(new double[][] {v[0], v[2], v[3]}, rgb, n));
    }

    public static void main(String[] args) throws Exception {
        for (Quinque q : Quinque.values()) {
            for (int step = 0; step < 3; step++) {
                List<PoseRender.Tri> tris = legacy(q, step, q.swing * 0.5);
                for (double fov : new double[] {55, 70, 100}) {
                    PoseRender.Canvas camera = new PoseRender.Canvas(W, H, 1,
                        new PoseRender.Camera(new double[] {0, 0, 0}, new double[] {0, 0, -1}, fov), BG);
                    for (PoseRender.Tri tri : tris) { camera.tri(tri, true); }
                    long pixels = java.util.Arrays.stream(camera.rgb).filter(c -> c != BG).count();
                    if (pixels < 50) { throw new AssertionError(q + " M1 " + step + " invisible at FOV " + fov + ": " + pixels); }
                }
            }
        }
        System.out.println("All 18 legacy M1 impacts visible at FOV 55/70/100 using their actual first-person model transforms.");
        int count = Arm.values().length + Quinque.values().length;
        BufferedImage sheet = new BufferedImage(W * 4, H * count + 35, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics(); g.setColor(new Color(BG)); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.drawString("3.1.3 - actual first-person geometry: ready / windup / impact / recovery (offline camera check, not an in-game test)", 10, 23);
        for (int row = 0; row < count; row++) {
            Arm arm = row < Arm.values().length ? Arm.values()[row] : null;
            Quinque q = arm == null ? Quinque.values()[row - Arm.values().length] : null;
            double land = arm == null ? q.swing * 0.5 : arm.strike(0).land();
            double total = arm == null ? q.swing : arm.strike(0).total();
            double[] ages = {-1, land * 0.6, land, total * 0.85};
            for (int col = 0; col < 4; col++) {
                List<PoseRender.Tri> tris = arm == null ? legacy(q, 0, ages[col]) : arm(arm, 0, ages[col]);
                PoseRender.Canvas canvas = new PoseRender.Canvas(W, H, 1, CAMERA, BG);
                for (PoseRender.Tri tri : tris) { canvas.tri(tri, true); }
                long visible = java.util.Arrays.stream(canvas.rgb).filter(c -> c != BG).count();
                if (col == 2 && visible < 100) { throw new AssertionError("Invisible impact: " + (arm == null ? q : arm) + ": " + visible); }
                g.drawImage(canvas.image(), col * W, 35 + row * H, null);
                g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                g.drawString((arm == null ? q.toString() : arm.title) + " / " + new String[] {"ready", "windup", "impact", "recovery"}[col],
                    col * W + 8, 35 + row * H + 16);
            }
            System.out.println("Camera render checked: " + (arm == null ? q : arm));
        }
        g.dispose(); File out = new File(args.length > 0 ? args[0] : "build/own-preview/quinques-3.1.3.png");
        out.getAbsoluteFile().getParentFile().mkdirs(); ImageIO.write(sheet, "png", out);
    }
}
