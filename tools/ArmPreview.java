import com.hex.kagune.control.RinkakuRibbon;
import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.quinque.ArmMesh;
import com.hex.kagune.control.quinque.WhipPath;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * 3.0.0: the higher ranks' quinques drawn off-game ({@link ArmMesh}'s real meshes, {@link WhipPath}'s real tendril through RinkakuRibbon):
 * a small z-buffered rasteriser, lit from above and in front, glowing quads unlit. One sheet of the weapons (regular and masterwork, from
 * the side and three-quarter), one of the whip's lashes and its bind.
 * <pre>
 * javac -nowarn -cp build/quick/classes -d build/arm-preview tools/ArmPreview.java
 * java -Djava.awt.headless=true -cp build/arm-preview:build/quick/classes ArmPreview &lt;out-dir&gt;
 * </pre>
 */
public class ArmPreview {
    static final int W = 560;
    static final int H = 420;

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/arm-preview");
        out.mkdirs();
        Arm[] rigid = {Arm.CRYSTAL_SPEAR, Arm.FLESH_HAMMER, Arm.BONE_SWORD, Arm.WHIP};
        BufferedImage sheet = new BufferedImage(W * 4, H * 2 + 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(26, 26, 32));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.drawString("3.0.0: the higher ranks' quinques (top: Rank 4-7; bottom: Rank 8-9 masterworks)", 10, 28);
        for (int row = 0; row < 2; row++) {
            for (int i = 0; i < rigid.length; i++) {
                boolean master = row == 1;
                Scene s = new Scene(W, H);
                ArmMesh.Mesh mesh = ArmMesh.of(rigid[i], master);
                float[] span = mesh.span();
                double mid = (span[0] + span[1]) / 2.0;
                double size = Math.max(1.0, span[1] - span[0]);
                // The weapon laid flat across the picture, turned a little toward us, point to the right.
                s.camera(new double[] {0.0, 0.0, -mid}, Math.toRadians(-90.0 + 24.0), Math.toRadians(14.0), size * 1.25);
                s.mesh(mesh);
                if (rigid[i] == Arm.WHIP) {
                    double[][] rest = WhipPath.lash(0, 0.45, 0.45, WhipPath.length(master), 10.0);
                    s.whip(toHand(rest, ArmMesh.WHIP_ROOT), WhipPath.thickness(master));
                    s.camera(new double[] {0.0, 0.0, -1.5}, Math.toRadians(-90.0 + 30.0), Math.toRadians(60.0), 6.0);
                    s = new Scene(W, H);
                    s.camera(new double[] {-1.0, 0.0, -2.0}, Math.toRadians(-90.0 + 20.0), Math.toRadians(55.0), 7.5);
                    s.mesh(mesh);
                    s.whip(toHand(rest, ArmMesh.WHIP_ROOT), WhipPath.thickness(master));
                }
                g.drawImage(s.image, i * W, 40 + row * H, null);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
                g.setColor(Color.WHITE);
                g.drawString(rigid[i].title + (master ? " (masterwork)" : "") + "  " + mesh.quads + " quads, " + String.format("%.2f", span[1] - span[0])
                    + " blocks", i * W + 8, 40 + row * H + H - 10);
            }
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(out, "arms-3.0.0.png"));
        // Big single shots, three-quarter.
        for (Arm arm : rigid) {
            if (arm == Arm.WHIP) {
                continue;
            }
            Scene s = new Scene(1100, 720);
            ArmMesh.Mesh mesh = ArmMesh.of(arm, true);
            float[] span = mesh.span();
            double mid = (span[0] + span[1]) / 2.0;
            s.camera(new double[] {0.0, 0.0, -mid}, Math.toRadians(-90.0 + 32.0), Math.toRadians(20.0), (span[1] - span[0]) * 1.1);
            s.mesh(mesh);
            ImageIO.write(s.image, "png", new File(out, arm.name().toLowerCase() + "-3.0.0.png"));
        }
        whips(out);
    }

    /** The whip's lashes (a frame of each blow at its landing) and its bind round a stand-in foe. */
    static void whips(File out) throws Exception {
        int w = 520;
        int h = 400;
        String[] names = {"forehand lash", "backhand lash", "overhead crack", "low sweep", "double lash", "spinning lash", "flay", "bind (coiled)"};
        BufferedImage sheet = new BufferedImage(w * 4, h * 2 + 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(26, 26, 32));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.drawString("3.0.0: the Tendril Whip (seen from above and behind; the investigator's fist at the dot, facing up the picture)", 10, 28);
        for (int i = 0; i < names.length; i++) {
            Scene s = new Scene(w, h);
            s.cameraWorld(new double[] {2.6, 0.0, 0.0}, Math.toRadians(180.0), Math.toRadians(62.0), 8.5);
            double[][] path;
            if (i < 6) {
                Arm.Strike strike = Arm.WHIP.strike(i);
                double land = strike.land() / (double)strike.total();
                path = WhipPath.lash(i, land + 0.04, land, WhipPath.length(false), 20.0);
            } else if (i == 6) {
                path = WhipPath.flay(Arm.Ability.FLAY.hits[2], WhipPath.length(false), 20.0);
            } else {
                double[] target = {4.0, 0.6, -0.2};
                path = WhipPath.bind(16.0, target, 0.35, WhipPath.length(false), 20.0);
                s.box(target, 0.35, 0.9);
            }
            s.whipWorld(path, WhipPath.thickness(false));
            s.dot(new double[] {0.0, 0.0, 0.0});
            g.drawImage(s.image, (i % 4) * w, 40 + (i / 4) * h, null);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            g.setColor(Color.WHITE);
            g.drawString(names[i], (i % 4) * w + 8, 40 + (i / 4) * h + h - 10);
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(out, "whip-3.0.0.png"));
    }

    /** A path in the investigator's frame {forward, left, up} into the weapon's hand frame held level (x across, y down, z back), from its knot. */
    static double[][] toHand(double[][] path, double root) {
        double[][] p = new double[path.length][];
        for (int i = 0; i < path.length; i++) {
            p[i] = new double[] {-path[i][1], -path[i][2], -(root + path[i][0])};
        }
        return p;
    }

    static final class Scene {
        final BufferedImage image;
        final double[] zb;
        final int w;
        final int h;
        double[] centre = {0, 0, 0};
        double yaw;
        double pitch;
        double fit = 2.0;
        boolean world;

        Scene(int w, int h) {
            this.w = w;
            this.h = h;
            this.image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            this.zb = new double[w * h];
            java.util.Arrays.fill(this.zb, Double.MAX_VALUE);
            for (int y = 0; y < h; y++) {
                int shade = 30 + (int)(22.0 * y / h);
                for (int x = 0; x < w; x++) {
                    this.image.setRGB(x, y, new Color(shade, shade, shade + 6).getRGB());
                }
            }
        }

        void camera(double[] centre, double yaw, double pitch, double fit) {
            this.centre = centre;
            this.yaw = yaw;
            this.pitch = pitch;
            this.fit = fit;
            this.world = false;
        }

        /** In the investigator's frame {forward, left, up}. */
        void cameraWorld(double[] centre, double yaw, double pitch, double fit) {
            this.camera(centre, yaw, pitch, fit);
            this.world = true;
        }

        /** A point into the camera's frame: {x right, y up, depth}. Hand-frame points are y down; the investigator's frame is up. */
        double[] view(double[] p) {
            double x;
            double y;
            double z;
            if (this.world) {
                x = -(p[1] - this.centre[1]);
                y = p[2] - this.centre[2];
                z = p[0] - this.centre[0];
            } else {
                x = p[0] - this.centre[0];
                y = -(p[1] - this.centre[1]);
                z = p[2] - this.centre[2];
            }
            double cy = Math.cos(this.yaw);
            double sy = Math.sin(this.yaw);
            double x1 = cy * x + sy * z;
            double z1 = -sy * x + cy * z;
            double cp = Math.cos(this.pitch);
            double sp = Math.sin(this.pitch);
            double y2 = cp * y - sp * z1;
            double z2 = sp * y + cp * z1;
            return new double[] {x1, y2, z2};
        }

        double[] viewDir(double[] n) {
            double[] o = this.view(new double[] {this.centre[0], this.centre[1], this.centre[2]});
            double[] p = this.view(new double[] {this.centre[0] + n[0], this.centre[1] + n[1], this.centre[2] + n[2]});
            return new double[] {p[0] - o[0], p[1] - o[1], p[2] - o[2]};
        }

        int[] screen(double[] v) {
            double s = Math.min(this.w, this.h) * 0.92 / this.fit;
            return new int[] {(int)Math.round(this.w / 2.0 + v[0] * s), (int)Math.round(this.h / 2.0 - v[1] * s)};
        }

        void mesh(ArmMesh.Mesh m) {
            for (int q = 0; q < m.quads; q++) {
                double[][] p = new double[4][];
                double[][] n = new double[4][];
                int[] c = new int[4];
                for (int k = 0; k < 4; k++) {
                    int v = q * 4 + k;
                    p[k] = new double[] {m.pos[v * 3], m.pos[v * 3 + 1], m.pos[v * 3 + 2]};
                    n[k] = new double[] {m.normal[v * 3], m.normal[v * 3 + 1], m.normal[v * 3 + 2]};
                    c[k] = m.argb[v];
                }
                this.quad(p, n, c, m.glow[q]);
            }
        }

        void whip(double[][] handPath, float thickness) {
            RinkakuRibbon.Mesh mesh = RinkakuRibbon.build(handPath, thickness, 0xA50F37, 77, null, 20.0);
            this.ribbon(mesh);
        }

        void whipWorld(double[][] path, float thickness) {
            RinkakuRibbon.Mesh mesh = RinkakuRibbon.build(path, thickness, 0xA50F37, 77, null, 20.0);
            this.ribbon(mesh);
        }

        void ribbon(RinkakuRibbon.Mesh mesh) {
            if (mesh == null) {
                return;
            }
            int sides = RinkakuRibbon.SIDES;
            for (int r = 0; r < mesh.at().length - 1; r++) {
                for (int k = 0; k < sides; k++) {
                    int[] ks = {k, k, k + 1, k + 1};
                    int[] rs = {r, r + 1, r + 1, r};
                    double[][] p = new double[4][];
                    double[][] n = new double[4][];
                    int[] c = new int[4];
                    for (int v = 0; v < 4; v++) {
                        p[v] = mesh.at()[rs[v]][ks[v]];
                        n[v] = mesh.way()[rs[v]][ks[v]];
                        c[v] = RinkakuRibbon.lifted(mesh.tint()[rs[v]][ks[v]]) | 0xFF000000;
                    }
                    this.quad(p, n, c, false);
                }
            }
            for (RinkakuRibbon.Glow glow : RinkakuRibbon.veins(mesh, 77, 20.0)) {
                double[] up = {0, 1, 0};
                this.quad(glow.corners(), new double[][] {up, up, up, up}, glow.argb(), true);
            }
        }

        /** A stand-in foe: a box. */
        void box(double[] at, double r, double half) {
            double[][] c = new double[8][];
            for (int i = 0; i < 8; i++) {
                c[i] = new double[] {at[0] + ((i & 1) == 0 ? -r : r), at[1] + ((i & 2) == 0 ? -r : r), at[2] + ((i & 4) == 0 ? -half : half)};
            }
            int[][] faces = {{0, 1, 3, 2}, {4, 5, 7, 6}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
            int grey = 0xFF5A6070;
            for (int[] f : faces) {
                double[][] p = {c[f[0]], c[f[1]], c[f[2]], c[f[3]]};
                double[] nn = {0, 0, 1};
                this.quad(p, new double[][] {nn, nn, nn, nn}, new int[] {grey, grey, grey, grey}, false);
            }
        }

        void dot(double[] at) {
            int[] s = this.screen(this.view(at));
            for (int y = -4; y <= 4; y++) {
                for (int x = -4; x <= 4; x++) {
                    if (x * x + y * y <= 16 && s[0] + x >= 0 && s[0] + x < this.w && s[1] + y >= 0 && s[1] + y < this.h) {
                        this.image.setRGB(s[0] + x, s[1] + y, 0xFFFFFFFF);
                    }
                }
            }
        }

        void quad(double[][] p, double[][] n, int[] c, boolean glow) {
            this.tri(p[0], p[1], p[2], n[0], n[1], n[2], c[0], c[1], c[2], glow);
            this.tri(p[0], p[2], p[3], n[0], n[2], n[3], c[0], c[2], c[3], glow);
        }

        void tri(double[] a, double[] b, double[] c, double[] na, double[] nb, double[] nc, int ca, int cb, int cc, boolean glow) {
            double[] va = this.view(a);
            double[] vb = this.view(b);
            double[] vc = this.view(c);
            int[] sa = this.screen(va);
            int[] sb = this.screen(vb);
            int[] sc = this.screen(vc);
            int minX = Math.max(0, Math.min(sa[0], Math.min(sb[0], sc[0])));
            int maxX = Math.min(this.w - 1, Math.max(sa[0], Math.max(sb[0], sc[0])));
            int minY = Math.max(0, Math.min(sa[1], Math.min(sb[1], sc[1])));
            int maxY = Math.min(this.h - 1, Math.max(sa[1], Math.max(sb[1], sc[1])));
            double area = (double)(sb[0] - sa[0]) * (sc[1] - sa[1]) - (double)(sc[0] - sa[0]) * (sb[1] - sa[1]);
            if (Math.abs(area) < 1.0E-9) {
                return;
            }
            double[] la = this.light(na, ca, glow);
            double[] lb = this.light(nb, cb, glow);
            double[] lc = this.light(nc, cc, glow);
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    double w0 = ((double)(sb[0] - x) * (sc[1] - y) - (double)(sc[0] - x) * (sb[1] - y)) / area;
                    double w1 = ((double)(sc[0] - x) * (sa[1] - y) - (double)(sa[0] - x) * (sc[1] - y)) / area;
                    double w2 = 1.0 - w0 - w1;
                    if (w0 < -1.0E-6 || w1 < -1.0E-6 || w2 < -1.0E-6) {
                        continue;
                    }
                    double z = w0 * va[2] + w1 * vb[2] + w2 * vc[2];
                    int idx = y * this.w + x;
                    if (z >= this.zb[idx] - (glow ? 0.004 : 0.0)) {
                        continue;
                    }
                    this.zb[idx] = z;
                    int r = clamp(w0 * la[0] + w1 * lb[0] + w2 * lc[0]);
                    int g = clamp(w0 * la[1] + w1 * lb[1] + w2 * lc[1]);
                    int bl = clamp(w0 * la[2] + w1 * lb[2] + w2 * lc[2]);
                    this.image.setRGB(x, y, 0xFF000000 | r << 16 | g << 8 | bl);
                }
            }
        }

        /** A vertex's colour lit: from above-front-left, a fill from behind, glowing ones as they are (a little bloom). */
        double[] light(double[] n, int argb, boolean glow) {
            double r = argb >> 16 & 0xFF;
            double g = argb >> 8 & 0xFF;
            double b = argb & 0xFF;
            if (glow) {
                return new double[] {Math.min(255, r * 1.08 + 18), Math.min(255, g * 1.05 + 10), Math.min(255, b * 1.05 + 10)};
            }
            double[] v = this.viewDir(n);
            double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            if (len > 1.0E-9) {
                v = new double[] {v[0] / len, v[1] / len, v[2] / len};
            }
            double[] key = {-0.45, 0.75, -0.48};
            double kl = Math.sqrt(key[0] * key[0] + key[1] * key[1] + key[2] * key[2]);
            double d = Math.abs(v[0] * key[0] / kl + v[1] * key[1] / kl + v[2] * key[2] / kl);
            double rim = Math.pow(1.0 - Math.abs(v[2]), 3.0) * 0.25;
            double k = 0.32 + 0.68 * d + rim;
            return new double[] {r * k, g * k, b * k};
        }

        static int clamp(double v) {
            return (int)Math.max(0, Math.min(255, Math.round(v)));
        }
    }
}
