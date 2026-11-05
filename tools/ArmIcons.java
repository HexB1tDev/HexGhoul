import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.quinque.ArmMesh;
import com.hex.kagune.control.quinque.WhipPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 3.0.6: the inventory icons of the investigators' kagune-made weapons as items, drawn from their own meshes ({@link ArmMesh}, the very
 * points the game draws in the hand), laid on a sword sprite's diagonal (the point up and right), its broad face to the viewer, lit from
 * the upper left, its glowing parts full bright; the whip's tendril curled beside its handle. Writes
 * textures/item/quinque_&lt;key&gt;.png (64 x 64) and a 4x preview sheet.
 * <pre>
 * javac -nowarn -cp build/quick/classes -d build/arm-icons tools/ArmIcons.java
 * java -Djava.awt.headless=true -cp build/arm-icons:build/quick/classes ArmIcons src/main/resources/assets/hexkagune/textures/item &lt;preview.png&gt;
 * </pre>
 */
public class ArmIcons {
    static final int SIZE = 64;
    static final int SS = 4;
    static final double MARGIN = 2.0;

    record Tri(double[][] v, int rgb, double[] n, boolean glow) {
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "build/arm-icons");
        dir.mkdirs();
        List<BufferedImage> icons = new ArrayList<>();
        for (Arm arm : Arm.values()) {
            List<Tri> tris = new ArrayList<>();
            ArmMesh.Mesh mesh = ArmMesh.of(arm, false);
            for (int q = 0; q < mesh.quads; q++) {
                double[][] v = new double[4][];
                for (int c = 0; c < 4; c++) {
                    int i = (q * 4 + c) * 3;
                    v[c] = new double[] {mesh.pos[i], mesh.pos[i + 1], mesh.pos[i + 2]};
                }
                int i = q * 4 * 3;
                double[] n = {mesh.normal[i], mesh.normal[i + 1], mesh.normal[i + 2]};
                int rgb = mesh.argb[q * 4] & 0xFFFFFF;
                tris.add(new Tri(new double[][] {v[0], v[1], v[2]}, rgb, n, mesh.glow[q]));
                tris.add(new Tri(new double[][] {v[0], v[2], v[3]}, rgb, n, mesh.glow[q]));
            }
            if (arm == Arm.WHIP) {
                tendril(tris);
            }
            BufferedImage icon = draw(tris);
            File file = new File(dir, "quinque_" + arm.key() + ".png");
            ImageIO.write(icon, "png", file);
            System.out.println(file);
            icons.add(icon);
        }
        if (args.length > 1) {
            int k = 4;
            BufferedImage sheet = new BufferedImage(SIZE * k * icons.size() + 10 * (icons.size() + 1), SIZE * k + 20, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < sheet.getHeight(); y++) {
                for (int x = 0; x < sheet.getWidth(); x++) {
                    sheet.setRGB(x, y, ((x / 16 + y / 16) & 1) == 0 ? 0x8B8B8B : 0x7A7A7A); // as the inventory's slots, near enough
                }
            }
            for (int a = 0; a < icons.size(); a++) {
                BufferedImage icon = icons.get(a);
                for (int y = 0; y < SIZE * k; y++) {
                    for (int x = 0; x < SIZE * k; x++) {
                        int c = icon.getRGB(x / k, y / k);
                        int alpha = c >>> 24;
                        int sx = 10 + a * (SIZE * k + 10) + x;
                        int sy = 10 + y;
                        int under = sheet.getRGB(sx, sy);
                        sheet.setRGB(sx, sy, mix(under, c, alpha / 255.0));
                    }
                }
            }
            ImageIO.write(sheet, "png", new File(args[1]));
            System.out.println(args[1]);
        }
    }

    /** The whip's tendril, curled in a loose coil out of the handle's knot (a tapering crimson tube). */
    static void tendril(List<Tri> tris) {
        int n = 60;
        double[][] path = new double[n][];
        double[] knot = {0.0, 0.0, -ArmMesh.WHIP_ROOT};
        double length = WhipPath.length(false) * 0.55;
        double r0 = 0.95;
        for (int i = 0; i < n; i++) {
            double t = i / (n - 1.0);
            // Out along the handle's line, then round in a coil beside it, tighter as it goes.
            double theta = t * Math.PI * 2.1;
            double r = r0 * (1.0 - 0.55 * t);
            double cy = r0;
            double cz = knot[2] - 0.7;
            path[i] = new double[] {0.05 * Math.sin(theta * 2.0), cy - r * Math.cos(theta), cz - r * Math.sin(theta) - 0.12 * t * length};
        }
        int sides = 7;
        double thick = WhipPath.thickness(false);
        for (int i = 0; i + 1 < n; i++) {
            double[] a = i == 0 ? knot : path[i];
            double[] b = path[i + 1];
            double[] t = norm(sub(b, a));
            double[] u = norm(cross(t, Math.abs(t[0]) < 0.9 ? new double[] {1, 0, 0} : new double[] {0, 1, 0}));
            double[] w = cross(t, u);
            double ra = thick * (1.0 - 0.8 * i / (n - 1.0));
            double rb = thick * (1.0 - 0.8 * (i + 1) / (n - 1.0));
            for (int s = 0; s < sides; s++) {
                double a0 = 2 * Math.PI * s / sides;
                double a1 = 2 * Math.PI * (s + 1) / sides;
                double[] d0 = add(scale(u, Math.cos(a0)), scale(w, Math.sin(a0)));
                double[] d1 = add(scale(u, Math.cos(a1)), scale(w, Math.sin(a1)));
                double[] p0 = add(a, scale(d0, ra));
                double[] p1 = add(a, scale(d1, ra));
                double[] p2 = add(b, scale(d1, rb));
                double[] p3 = add(b, scale(d0, rb));
                double[] nn = norm(add(d0, d1));
                int c = i % 4 == 0 ? 0x7A0C16 : 0xA8141F;
                tris.add(new Tri(new double[][] {p0, p1, p2}, c, nn, false));
                tris.add(new Tri(new double[][] {p0, p2, p3}, c, nn, false));
            }
        }
    }

    /** Its point up and right, its broad face (+x) to the viewer. */
    static double[] project(double[] p) {
        double u = (-p[2] + p[1]) / Math.sqrt(2.0);
        double v = (-p[2] - p[1]) / Math.sqrt(2.0);
        return new double[] {u, v, -p[0]};
    }

    static BufferedImage draw(List<Tri> tris) {
        double minU = Double.MAX_VALUE, maxU = -Double.MAX_VALUE, minV = Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
        for (Tri t : tris) {
            for (double[] p : t.v) {
                double[] s = project(p);
                minU = Math.min(minU, s[0]);
                maxU = Math.max(maxU, s[0]);
                minV = Math.min(minV, s[1]);
                maxV = Math.max(maxV, s[1]);
            }
        }
        int w = SIZE * SS;
        double fit = (SIZE - 2 * MARGIN) * SS / Math.max(maxU - minU, maxV - minV);
        double cu = (minU + maxU) * 0.5;
        double cv = (minV + maxV) * 0.5;
        int[] rgb = new int[w * w];
        double[] depth = new double[w * w];
        java.util.Arrays.fill(depth, Double.MAX_VALUE);
        double[] light = norm(new double[] {1.0, -0.55, -0.25});
        for (Tri t : tris) {
            double[][] s = new double[3][];
            for (int c = 0; c < 3; c++) {
                double[] q = project(t.v[c]);
                s[c] = new double[] {w * 0.5 + (q[0] - cu) * fit, w * 0.5 - (q[1] - cv) * fit, q[2]};
            }
            int color = t.rgb;
            if (!t.glow) {
                double k = Math.min(1.15, 0.42 + 0.75 * Math.max(0.0, dot(norm(t.n), light)) + 0.18 * Math.abs(t.n[0]));
                color = shade(color, k);
            } else {
                color = shade(color, 1.15);
            }
            raster(s, color, rgb, depth, w);
        }
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int r = 0, g = 0, b = 0, hit = 0;
                for (int dy = 0; dy < SS; dy++) {
                    for (int dx = 0; dx < SS; dx++) {
                        int i = (y * SS + dy) * w + x * SS + dx;
                        if (depth[i] < Double.MAX_VALUE) {
                            r += rgb[i] >> 16 & 255;
                            g += rgb[i] >> 8 & 255;
                            b += rgb[i] & 255;
                            hit++;
                        }
                    }
                }
                if (hit == 0) {
                    continue;
                }
                // Hard edges as the game's own sprites: a pixel is in if half its samples are.
                int alpha = hit * 2 >= SS * SS ? 255 : 0;
                out.setRGB(x, y, alpha << 24 | (r / hit) << 16 | (g / hit) << 8 | b / hit);
            }
        }
        outline(out);
        return out;
    }

    /** A dark rim round it, as vanilla's item sprites have, so it reads on the slot's grey. */
    static void outline(BufferedImage img) {
        int n = img.getWidth();
        boolean[] in = new boolean[n * n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                in[y * n + x] = img.getRGB(x, y) >>> 24 != 0;
            }
        }
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (!in[y * n + x]) {
                    continue;
                }
                boolean edge = x == 0 || y == 0 || x == n - 1 || y == n - 1 || !in[y * n + x - 1] || !in[y * n + x + 1] || !in[(y - 1) * n + x]
                    || !in[(y + 1) * n + x];
                if (edge) {
                    int c = img.getRGB(x, y);
                    img.setRGB(x, y, 0xFF000000 | shade(c & 0xFFFFFF, 0.45));
                }
            }
        }
    }

    static void raster(double[][] s, int color, int[] rgb, double[] depth, int w) {
        double[] a = s[0], b = s[1], c = s[2];
        double area = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
        if (Math.abs(area) < 1.0E-9) {
            return;
        }
        int minX = (int)Math.max(0, Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
        int maxX = (int)Math.min(w - 1, Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
        int minY = (int)Math.max(0, Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
        int maxY = (int)Math.min(w - 1, Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double px = x + 0.5, py = y + 0.5;
                double w0 = ((b[0] - px) * (c[1] - py) - (b[1] - py) * (c[0] - px)) / area;
                double w1 = ((c[0] - px) * (a[1] - py) - (c[1] - py) * (a[0] - px)) / area;
                double w2 = 1 - w0 - w1;
                if (w0 < -1e-6 || w1 < -1e-6 || w2 < -1e-6) {
                    continue;
                }
                double z = w0 * a[2] + w1 * b[2] + w2 * c[2];
                int i = y * w + x;
                if (z < depth[i]) {
                    depth[i] = z;
                    rgb[i] = color;
                }
            }
        }
    }

    static int mix(int under, int over, double k) {
        int r = (int)((under >> 16 & 255) * (1 - k) + (over >> 16 & 255) * k);
        int g = (int)((under >> 8 & 255) * (1 - k) + (over >> 8 & 255) * k);
        int b = (int)((under & 255) * (1 - k) + (over & 255) * k);
        return r << 16 | g << 8 | b;
    }

    static int shade(int rgb, double k) {
        int r = (int)Math.min(255, (rgb >> 16 & 255) * k), g = (int)Math.min(255, (rgb >> 8 & 255) * k), b = (int)Math.min(255, (rgb & 255) * k);
        return r << 16 | g << 8 | b;
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] scale(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] norm(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1e-12 ? new double[] {0, 1, 0} : scale(a, 1 / l);
    }
}
