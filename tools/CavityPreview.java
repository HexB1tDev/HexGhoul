import com.hex.kagune.control.awakening.geo.GeoAnimation;
import com.hex.kagune.control.awakening.geo.GeoModel;
import com.hex.kagune.control.awakening.geo.GeoPose;
import com.hex.kagune.control.awakening.geo.Mat4;
import com.hex.kagune.control.awakening.geo.Molang;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 3.6.5 preview: the inside of Grand Devouring's mouth (devour_cavity.geo.json) baked as GeckoLib bakes it (awakening/geo), posed by its
 * own pulse animation, its jaws' hinges turned by code as the game will, the flesh tinted a kagune colour and the teeth left bone, seen
 * from outside (cut away) and from the eyes of a player inside it.
 *
 * After tools/MawPreview's own compile (build/maw-preview, with build/quick/classes):
 * javac -nowarn -d <out> -cp build/maw-preview:build/quick/classes awakening/src/com/hex/kagune/control/awakening/geo/*.java tools/CavityPreview.java
 * java -Djava.awt.headless=true -cp <out>:build/maw-preview:build/quick/classes CavityPreview src/main/resources/assets/hexkagune <out.png>
 */
public final class CavityPreview {
    static GeoModel model;
    static GeoAnimation.Clip pulse;
    static BufferedImage tex;

    public static void main(String[] args) throws Exception {
        File assets = new File(args[0]);
        model = GeoModel.read(Files.readString(new File(assets, "geo/devour_cavity.geo.json").toPath()));
        pulse = GeoAnimation.read(Files.readString(new File(assets, "animations/devour_cavity.animation.json").toPath()))
            .clip("animation.hexkagune.devour_cavity.pulse");
        tex = ImageIO.read(new File(assets, "textures/geo/devour_cavity.png"));
        int fw = 400;
        int fh = 360;
        int cols = 4;
        BufferedImage img = new BufferedImage(fw * cols, fh * 3 + 34, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x16181E));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("Bikaku 3.6.5 preview (GeckoLib model): inside Grand Devouring's mouth, as the player being eaten sees it", 8, 23);
        int tint = 0xA50F37;
        // A player-sized body: the cavity scaled to it (hole 0.756, depth 2.43).
        double hole = 0.756;
        double depth = 2.43;
        double[] opens = {1.0, 0.6, 0.3, 0.08};
        String[] notes = {"caught: jaws wide", "jaws closing", "closing", "shut"};
        for (int i = 0; i < cols; i++) {
            double lift = 0.54 * i / 3.0;
            List<PoseRender.Tri> tris = bake(opens[i], 0.6 * i, hole, depth, tint, false);
            // From his eyes, looking down past his feet at the jaws.
            double eye = lift + 1.62;
            PoseRender.Camera down = new PoseRender.Camera(new double[] {0.0, eye, 0.05}, new double[] {0.15, lift - 0.6, 0.55}, 80);
            draw(g, tris, down, i * fw, 34, fw, fh, false);
            label(g, String.format("looking down at the jaws (%s)", notes[i]), i * fw, 34);
        }
        for (int i = 0; i < cols; i++) {
            double lift = 0.2;
            double t = 0.6 * i;
            List<PoseRender.Tri> tris = bake(0.6, t, hole, depth, tint, false);
            PoseRender.Camera cam = i < 2
                ? new PoseRender.Camera(new double[] {0.0, lift + 1.62, 0.0}, new double[] {i == 0 ? 0.0 : 0.6, lift + 1.7, 1.0}, 80)
                : new PoseRender.Camera(new double[] {0.0, lift + 1.62, 0.0}, new double[] {0.1, lift + 4.0, 0.25}, 80);
            draw(g, tris, cam, i * fw, 34 + fh, fw, fh, false);
            label(g, i < 2 ? String.format("looking ahead at the walls (%.1f s: they pulse)", t) : String.format("looking up the throat (%.1f s)", t),
                i * fw, 34 + fh);
        }
        for (int i = 0; i < cols; i++) {
            double open = opens[i];
            List<PoseRender.Tri> tris = bake(open, 0.6 * i, hole, depth, tint, true);
            tris.addAll(box(new double[] {0.0, 0.54 * i / 3.0, 0.0}, 0.6, 1.8, 0x2E8B57));
            PoseRender.Camera side = new PoseRender.Camera(new double[] {0.0, 1.6, 5.2}, new double[] {0.0, 1.2, 0.0}, 50);
            draw(g, tris, side, i * fw, 34 + 2 * fh, fw, fh, true);
            label(g, String.format("outside, cut in half (%s)", notes[i]), i * fw, 34 + 2 * fh);
        }
        g.dispose();
        ImageIO.write(img, "png", new File(args[1]));
        System.out.println(args[1]);
    }

    static void label(Graphics2D g, String text, int x, int y) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(0xE0E0E0));
        g.drawString(text, x + 6, y + 16);
    }

    static void draw(Graphics2D g, List<PoseRender.Tri> tris, PoseRender.Camera cam, int x, int y, int w, int h, boolean floor) {
        PoseRender.Canvas canvas = new PoseRender.Canvas(w, h, 2, cam, floor ? 0x3A4450 : 0x101010);
        if (floor) {
            canvas.floor(6, 0x58626C, 0x4E5862);
        }
        for (PoseRender.Tri t : tris) {
            canvas.tri(t, true);
        }
        g.drawImage(canvas.image(), x, y, null);
    }

    /**
     * The model posed {@code seconds} into its pulse, its jaws {@code open} (1 wide: hanging out and down; 0 shut across the hole), scaled
     * to a cavity {@code hole} wide and {@code depth} deep, its mouth at the ground at the origin; {@code cut}: only the half away from a
     * camera before it (z below 0).
     */
    static List<PoseRender.Tri> bake(double open, double seconds, double hole, double depth, int tint, boolean cut) {
        GeoPose pose = new GeoPose(model);
        Molang.Context context = new Molang.Context() {
            public double animTime() {
                return seconds;
            }

            public double lifeTime() {
                return seconds;
            }

            public double variable(String name) {
                return 0.0;
            }
        };
        pose.apply(pulse, seconds, 1.0, context, null);
        for (int j = 0; j < 8; j++) {
            int hinge = model.bone("hinge" + j);
            // Open: turned 100 degrees, out and down past the rim; shut: lying across the hole.
            pose.rotX[hinge] = model.bones[hinge].rotX + Math.toRadians(-100.0) * open;
        }
        double[][] m = pose.matrices();
        double sx = hole / 1.0;
        double sy = depth / 2.25;
        List<PoseRender.Tri> out = new ArrayList<>();
        for (int b = 0; b < model.bones.length; b++) {
            if (pose.invisible(b)) {
                continue;
            }
            String name = model.bones[b].name;
            boolean bone = name.startsWith("tooth") || name.startsWith("fang");
            for (GeoModel.Cube cube : model.bones[b].cubes) {
                double[] cm = Mat4.multiply(m[b], GeoPose.cubeMatrix(cube));
                for (GeoModel.Quad q : cube.quads) {
                    if (q == null) {
                        continue;
                    }
                    double[][] v = new double[4][];
                    for (int k = 0; k < 4; k++) {
                        double[] p = Mat4.point(cm, q.positions[k * 3], q.positions[k * 3 + 1], q.positions[k * 3 + 2]);
                        v[k] = new double[] {-p[0] * sx, p[1] * sy, p[2] * sx};
                    }
                    if (cut && (v[0][2] + v[1][2] + v[2][2] + v[3][2]) / 4.0 > 0.0) {
                        continue;
                    }
                    double u = (q.uvs[0] + q.uvs[2] + q.uvs[4] + q.uvs[6]) / 4.0;
                    double w = (q.uvs[1] + q.uvs[3] + q.uvs[5] + q.uvs[7]) / 4.0;
                    int px = (int)Math.max(0, Math.min(tex.getWidth() - 1, u * (u <= 1.0 ? tex.getWidth() : 1)));
                    int py = (int)Math.max(0, Math.min(tex.getHeight() - 1, w * (w <= 1.0 ? tex.getHeight() : 1)));
                    int rgb = tex.getRGB(px, py) & 0xFFFFFF;
                    if (!bone) {
                        rgb = multiply(rgb, tint);
                    }
                    double[] e1 = sub(v[1], v[0]);
                    double[] e2 = sub(v[2], v[0]);
                    double[] n = {e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]};
                    double len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
                    if (len < 1e-12) {
                        continue;
                    }
                    n = new double[] {n[0] / len, n[1] / len, n[2] / len};
                    out.add(new PoseRender.Tri(new double[][] {v[0], v[1], v[2]}, rgb, n));
                    out.add(new PoseRender.Tri(new double[][] {v[0], v[2], v[3]}, rgb, n));
                }
            }
        }
        return out;
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static int multiply(int a, int b) {
        int r = (a >> 16 & 255) * (b >> 16 & 255) / 255;
        int g = (a >> 8 & 255) * (b >> 8 & 255) / 255;
        int bl = (a & 255) * (b & 255) / 255;
        // The kagune colour is dark; lift it as the game's lighting would on a lit face.
        return Math.min(255, (int)(r * 1.5)) << 16 | Math.min(255, (int)(g * 1.5)) << 8 | Math.min(255, (int)(bl * 1.5));
    }

    static List<PoseRender.Tri> box(double[] feet, double width, double height, int rgb) {
        List<PoseRender.Tri> out = new ArrayList<>();
        double h = width / 2.0;
        double[][] c = new double[8][];
        for (int i = 0; i < 8; i++) {
            c[i] = new double[] {feet[0] + ((i & 1) == 0 ? -h : h), feet[1] + ((i >> 1 & 1) == 0 ? 0.0 : height), feet[2] + ((i >> 2 & 1) == 0 ? -h : h)};
        }
        int[][] faces = {{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
        double[][] normals = {{0, 0, -1}, {0, 0, 1}, {0, -1, 0}, {0, 1, 0}, {-1, 0, 0}, {1, 0, 0}};
        for (int f = 0; f < 6; f++) {
            int[] q = faces[f];
            out.add(new PoseRender.Tri(new double[][] {c[q[0]], c[q[1]], c[q[2]]}, rgb, normals[f]));
            out.add(new PoseRender.Tri(new double[][] {c[q[0]], c[q[2]], c[q[3]]}, rgb, normals[f]));
        }
        return out;
    }
}
