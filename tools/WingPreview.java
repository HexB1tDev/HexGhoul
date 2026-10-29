import com.hex.kagune.control.ayato.WingForm;
import com.hex.kagune.control.ayato.WingRig;
import com.hex.kagune.control.kakugan.KakuganEyes;
import com.hex.kagune.control.kakugan.KakuganModel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Off-game 3D renders of Ayato's kagune straight from {@link WingRig} (the same quads the game draws), next to a
 * stand-in player. A small software rasterizer: perspective camera, depth buffer, the opaque player first, then the
 * wing quads sorted back to front and alpha-blended (as Minecraft sorts translucent quads), shaded with Minecraft's
 * entity diffuse lighting (two fixed lights, 0.4 ambient) and full brightness. No bloom: what glows in game must glow
 * here too. Usage after a build: {@code java -cp build/classes tools/WingPreview.java <out-dir>}.
 */
public final class WingPreview {
    // Minecraft's entity diffuse lights (Lighting.DIFFUSE_LIGHT_0/1), normalized.
    private static final double[] L0 = norm(0.2, 1.0, -0.7);
    private static final double[] L1 = norm(-0.2, 1.0, 0.7);

    /** {@code argb} lit as the game lights a vertex facing {@code n} (its two lights, its ambient). */
    static int lit(int argb, double[] n) {
        double l = Math.min(1.0, (Math.max(0, dot(L0, n)) + Math.max(0, dot(L1, n))) * 0.6 + 0.4);
        int r = (int)(((argb >> 16) & 255) * l);
        int g = (int)(((argb >> 8) & 255) * l);
        int b = (int)((argb & 255) * l);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
    private static final int SS = 2; // supersampling

    /** A triangle with one ARGB colour per corner (the wings use gradients). */
    record Tri(double[][] p, int[] argb, double[] n, boolean opaque, boolean additive, double[][] uv, Texture tex) {
        Tri(double[][] p, int[] argb, double[] n, boolean opaque) {
            this(p, argb, n, opaque, false, null, null);
        }

        Tri(double[][] p, int[] argb, double[] n, boolean opaque, boolean additive) {
            this(p, argb, n, opaque, additive, null, null);
        }
    }

    /** A texture as the game samples an entity's: nearest texel, wrapping; its pixels ARGB. */
    record Texture(int w, int h, int[] argb) {
        static final java.util.Map<String, Texture> LOADED = new java.util.HashMap<>();

        static Texture load(String path) {
            return LOADED.computeIfAbsent(path, p -> {
                try {
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.File(p));
                    int[] px = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
                    return new Texture(img.getWidth(), img.getHeight(), px);
                } catch (java.io.IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        }

        /** Drawn at full brightness, whatever the world's light (the game's FULL_BRIGHT light). */
        static final java.util.Set<Texture> BRIGHT = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

        /** The texture at {@code path} drawn at full brightness (a copy of it of its own, so the same texture can be drawn both ways). */
        static Texture bright(String path) {
            return LOADED.computeIfAbsent(path + "#bright", p -> {
                Texture t = load(path);
                Texture copy = new Texture(t.w, t.h, t.argb);
                BRIGHT.add(copy);
                return copy;
            });
        }

        int at(double u, double v) {
            int x = Math.floorMod((int)Math.floor(u * w), w);
            int y = Math.floorMod((int)Math.floor(v * h), h);
            return argb[y * w + x];
        }
    }

    /** Wound flesh drawn before the scene, and wound openings written to depth only before it (see Panel.draw). */
    static final ThreadLocal<List<Tri>> CARVED = ThreadLocal.withInitial(ArrayList::new);
    static final ThreadLocal<List<Tri>> OPENINGS = ThreadLocal.withInitial(ArrayList::new);

    /** One panel: a camera looking at the player-local scene. */
    static final class Panel {
        final int w;
        final int h;
        final double[] eye;
        final double[] target;
        final double fov;
        final int[] color;
        final float[] depth;
        double[] f;
        double[] r;
        double[] u;
        /** World light for lit (non-glowing) surfaces: 1 by day, low at night; glowing (additive) parts ignore it. */
        double dim = 1.0;

        Panel(int w, int h, double[] eye, double[] target, double fov, int background) {
            this.w = w * SS;
            this.h = h * SS;
            this.eye = eye;
            this.target = target;
            this.fov = fov;
            this.color = new int[this.w * this.h];
            this.depth = new float[this.w * this.h];
            for (int i = 0; i < this.color.length; i++) {
                int y = i / this.w;
                double t = y / (double)this.h;
                this.color[i] = mixRgb(background, 0x05060A, t * 0.6);
                this.depth[i] = Float.MAX_VALUE;
            }
            this.f = norm(target[0] - eye[0], target[1] - eye[1], target[2] - eye[2]);
            // Rig space is x right, y up, z forward (left-handed): screen right is up x forward.
            this.r = norm(cross(new double[] {0, 1, 0}, this.f));
            this.u = cross(this.f, this.r);
        }

        /** {screen x, screen y, depth} or null behind the camera. */
        double[] project(double[] p) {
            double dx = p[0] - eye[0];
            double dy = p[1] - eye[1];
            double dz = p[2] - eye[2];
            double cz = dx * f[0] + dy * f[1] + dz * f[2];
            if (cz < 0.05) {
                return null;
            }
            double cx = dx * r[0] + dy * r[1] + dz * r[2];
            double cy = dx * u[0] + dy * u[1] + dz * u[2];
            double scale = (h / 2.0) / Math.tan(Math.toRadians(fov) / 2.0);
            return new double[] {w / 2.0 + cx / cz * scale, h / 2.0 - cy / cz * scale, cz};
        }

        void draw(List<Tri> tris) {
            // Wounds carved as the game does: their flesh first, then their openings into the depth buffer only, then the body.
            for (Tri t : CARVED.get()) {
                raster(t, true);
            }
            for (Tri t : OPENINGS.get()) {
                rasterDepth(t, 0.002);
            }
            List<Tri> opaque = new ArrayList<>();
            List<Object[]> translucent = new ArrayList<>();
            for (Tri t : tris) {
                if (t.opaque) {
                    opaque.add(t);
                } else {
                    double d = 0;
                    for (double[] p : t.p) {
                        double dx = p[0] - eye[0], dy = p[1] - eye[1], dz = p[2] - eye[2];
                        d += Math.sqrt(dx * dx + dy * dy + dz * dz);
                    }
                    translucent.add(new Object[] {d / 3.0, t});
                }
            }
            for (Tri t : opaque) {
                raster(t, true);
            }
            translucent.sort(Comparator.comparingDouble(o -> -(double)o[0]));
            for (Object[] o : translucent) {
                if (!((Tri)o[1]).additive) {
                    raster((Tri)o[1], false);
                }
            }
            // Glowing (additive) parts are their own render type, batched after the translucent parts, as in game.
            for (Object[] o : translucent) {
                if (((Tri)o[1]).additive) {
                    raster((Tri)o[1], false);
                }
            }
        }

        void raster(Tri t, boolean writeDepth) {
            double[] a = project(t.p[0]);
            double[] b = project(t.p[1]);
            double[] c = project(t.p[2]);
            if (a == null || b == null || c == null) {
                return;
            }
            // (No normal: lit at each corner already, as the game lights a vertex by its own normal; only the dimming left.)
            double light = t.additive || (t.tex != null && Texture.BRIGHT.contains(t.tex)) ? 1.0
                : t.n == null ? dim : Math.min(1.0, (Math.max(0, dot(L0, t.n)) + Math.max(0, dot(L1, t.n))) * 0.6 + 0.4) * dim;
            int minX = (int)Math.max(0, Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
            int maxX = (int)Math.min(w - 1, Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
            int minY = (int)Math.max(0, Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
            int maxY = (int)Math.min(h - 1, Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
            double area = (b[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (b[1] - a[1]);
            if (Math.abs(area) < 1e-9) {
                return;
            }
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    double px = x + 0.5, py = y + 0.5;
                    double w0 = ((b[0] - px) * (c[1] - py) - (c[0] - px) * (b[1] - py)) / area;
                    double w1 = ((c[0] - px) * (a[1] - py) - (a[0] - px) * (c[1] - py)) / area;
                    double w2 = 1 - w0 - w1;
                    if (w0 < 0 || w1 < 0 || w2 < 0) {
                        continue;
                    }
                    float z = (float)(w0 * a[2] + w1 * b[2] + w2 * c[2]);
                    int i = y * w + x;
                    // (Glowing parts pass where they lie on what is drawn, as the game's eyes render type tests depth: less or equal.)
                    if (t.additive ? z > depth[i] + 0.002F : z >= depth[i]) {
                        continue;
                    }
                    int[] k = t.argb;
                    int rr = (int)Math.min(255, (w0 * ((k[0] >> 16) & 255) + w1 * ((k[1] >> 16) & 255) + w2 * ((k[2] >> 16) & 255)) * light);
                    int gg = (int)Math.min(255, (w0 * ((k[0] >> 8) & 255) + w1 * ((k[1] >> 8) & 255) + w2 * ((k[2] >> 8) & 255)) * light);
                    int bb = (int)Math.min(255, (w0 * (k[0] & 255) + w1 * (k[1] & 255) + w2 * (k[2] & 255)) * light);
                    float al = (float)((w0 * (k[0] >>> 24) + w1 * (k[1] >>> 24) + w2 * (k[2] >>> 24)) / 255.0);
                    if (t.tex != null) {
                        // Textured as the game's entity shaders do: perspective-correct, the texel times the tint, cut out under 0.1 alpha.
                        double ia = w0 / a[2];
                        double ib = w1 / b[2];
                        double ic = w2 / c[2];
                        double sum = ia + ib + ic;
                        double tu = (ia * t.uv[0][0] + ib * t.uv[1][0] + ic * t.uv[2][0]) / sum;
                        double tv = (ia * t.uv[0][1] + ib * t.uv[1][1] + ic * t.uv[2][1]) / sum;
                        int texel = t.tex.at(tu, tv);
                        float ta = (texel >>> 24) / 255.0F;
                        if (ta < 0.1F && !t.additive) {
                            continue;
                        }
                        rr = (int)Math.min(255, rr * ((texel >> 16) & 255) / 255.0);
                        gg = (int)Math.min(255, gg * ((texel >> 8) & 255) / 255.0);
                        bb = (int)Math.min(255, bb * (texel & 255) / 255.0);
                        al = t.additive ? al : ta;
                        if (!t.additive) {
                            // (Its painted parts are cut out, not blended: drawn as solid.)
                            depth[i] = z;
                            color[i] = (rr << 16) | (gg << 8) | bb;
                            continue;
                        }
                    }
                    if (t.additive) {
                        // RenderType.eyes: added at full brightness (alpha ignored), no depth write.
                        int dst = color[i];
                        color[i] = (Math.min(255, ((dst >> 16) & 255) + rr) << 16) | (Math.min(255, ((dst >> 8) & 255) + gg) << 8)
                            | Math.min(255, (dst & 255) + bb);
                    } else if (writeDepth) {
                        depth[i] = z;
                        color[i] = (rr << 16) | (gg << 8) | bb;
                    } else {
                        int dst = color[i];
                        int nr = (int)(((dst >> 16) & 255) * (1 - al) + rr * al);
                        int ng = (int)(((dst >> 8) & 255) * (1 - al) + gg * al);
                        int nb = (int)((dst & 255) * (1 - al) + bb * al);
                        color[i] = (nr << 16) | (ng << 8) | nb;
                    }
                }
            }
        }

        /** Writes a triangle into the depth buffer only, {@code bias} nearer (a wound's opening). */
        void rasterDepth(Tri t, double bias) {
            double[] a = project(t.p[0]);
            double[] b = project(t.p[1]);
            double[] c = project(t.p[2]);
            if (a == null || b == null || c == null) {
                return;
            }
            int minX = (int)Math.max(0, Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
            int maxX = (int)Math.min(w - 1, Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
            int minY = (int)Math.max(0, Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
            int maxY = (int)Math.min(h - 1, Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
            double area = (b[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (b[1] - a[1]);
            if (Math.abs(area) < 1e-9) {
                return;
            }
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    double px = x + 0.5, py = y + 0.5;
                    double w0 = ((b[0] - px) * (c[1] - py) - (c[0] - px) * (b[1] - py)) / area;
                    double w1 = ((c[0] - px) * (a[1] - py) - (a[0] - px) * (c[1] - py)) / area;
                    double w2 = 1 - w0 - w1;
                    if (w0 < 0 || w1 < 0 || w2 < 0) {
                        continue;
                    }
                    float z = (float)(w0 * a[2] + w1 * b[2] + w2 * c[2] - bias);
                    int i = y * w + x;
                    if (z < depth[i]) {
                        depth[i] = z;
                    }
                }
            }
        }

        BufferedImage image() {
            BufferedImage out = new BufferedImage(w / SS, h / SS, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h / SS; y++) {
                for (int x = 0; x < w / SS; x++) {
                    int r = 0, g = 0, b = 0;
                    for (int sy = 0; sy < SS; sy++) {
                        for (int sx = 0; sx < SS; sx++) {
                            int c = color[(y * SS + sy) * w + x * SS + sx];
                            r += (c >> 16) & 255;
                            g += (c >> 8) & 255;
                            b += c & 255;
                        }
                    }
                    int n = SS * SS;
                    out.setRGB(x, y, ((r / n) << 16) | ((g / n) << 8) | (b / n));
                }
            }
            return out;
        }
    }

    // ------------------------------------------------------------------ scene

    static List<Tri> scene(WingRig.Input input, boolean crouch, double[] eye) {
        List<Tri> tris = new ArrayList<>();
        player(tris, crouch);
        for (WingRig.Quad q : WingRig.build(input, eye)) {
            float[] v = q.xyz();
            double[] n = {q.nx(), q.ny(), q.nz()};
            double[][] c = new double[4][];
            for (int k = 0; k < 4; k++) {
                c[k] = new double[] {v[k * 3], v[k * 3 + 1], v[k * 3 + 2]};
            }
            int[] a = q.argb();
            tris.add(new Tri(new double[][] {c[0], c[1], c[2]}, new int[] {a[0], a[1], a[2]}, n, false));
            tris.add(new Tri(new double[][] {c[0], c[2], c[3]}, new int[] {a[0], a[2], a[3]}, n, false));
        }
        return tris;
    }

    /** Steve-proportioned stand-in for Ayato (x right, y up, z forward): black clothes, pale skin, dark navy hair. */
    static void player(List<Tri> tris, boolean crouch) {
        double drop = crouch ? 0.3 : 0.0;
        int cloth = 0x1C1A22;
        int skin = 0xE6C6AE;
        int hair = 0x2B3058;
        box(tris, -0.25, 0.0, -0.125, -0.005, 0.75, 0.125, cloth, 0, 0);            // right leg
        box(tris, 0.005, 0.0, -0.125, 0.25, 0.75, 0.125, cloth, 0, 0);              // left leg
        box(tris, -0.25, 0.75, -0.125, 0.25, 1.5 - drop, 0.125, cloth, 0, 0);      // body
        box(tris, -0.5, 0.75, -0.125, -0.25, 1.5 - drop, 0.125, cloth, 0, 0);      // right arm
        box(tris, 0.25, 0.75, -0.125, 0.5, 1.5 - drop, 0.125, cloth, 0, 0);        // left arm
        box(tris, -0.5, 0.75, -0.125, -0.25, 0.9, 0.125, skin, 0, 0);              // right hand
        box(tris, 0.25, 0.75, -0.125, 0.5, 0.9, 0.125, skin, 0, 0);                // left hand
        box(tris, -0.25, 1.5 - drop, -0.25, 0.25, 2.0 - drop, 0.25, skin, 0, 0);  // head
        box(tris, -0.26, 1.78 - drop, -0.26, 0.26, 2.02 - drop, 0.26, hair, 0, 0); // hair
    }

    static void box(List<Tri> tris, double x0, double y0, double z0, double x1, double y1, double z1, int rgb, int a, int b) {
        double[][] c = {
            {x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
        int[][] faces = {{0, 1, 2, 3}, {5, 4, 7, 6}, {4, 0, 3, 7}, {1, 5, 6, 2}, {3, 2, 6, 7}, {4, 5, 1, 0}};
        double[][] normals = {{0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}, {0, 1, 0}, {0, -1, 0}};
        for (int f = 0; f < 6; f++) {
            int[] q = faces[f];
            int[] solid = {0xFF000000 | rgb, 0xFF000000 | rgb, 0xFF000000 | rgb};
            tris.add(new Tri(new double[][] {c[q[0]], c[q[1]], c[q[2]]}, solid, normals[f], true));
            tris.add(new Tri(new double[][] {c[q[0]], c[q[2]], c[q[3]]}, solid, normals[f], true));
        }
    }

    // ------------------------------------------------------------------ sheets

    static WingRig.Input dissolving(WingForm form, int mastery, float deploy, double time, float crystal) {
        return new WingRig.Input(form, mastery, deploy, time, crystal, crystal, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1.0F, 1.0F, true);
    }

    static WingRig.Input in(WingForm form, int mastery, float deploy, double time, float crystal, WingRig.Motion motion, float progress, float volley) {
        return new WingRig.Input(form, mastery, deploy, time, crystal, crystal, motion, progress, 0.4F, volley, volley > 0 ? 1.0F : 0.0F,
            false, 1.0F, 1.0F, false);
    }

    /** Camera presets around a standing player, looking at the shoulders. */
    static double[] cam(String view, double distance) {
        return switch (view) {
            case "back" -> new double[] {-1.2 * distance / 5, 2.4, -distance};
            case "back34" -> new double[] {-3.2 * distance / 5, 2.6, -3.6 * distance / 5};
            case "front34" -> new double[] {2.8 * distance / 5, 2.1, 4.0 * distance / 5};
            case "front" -> new double[] {0.6, 1.9, distance};
            case "side" -> new double[] {distance, 1.9, 0.4};
            case "top" -> new double[] {0.01, 1.4 + distance, 0.6};
            case "above" -> new double[] {-1.0 * distance / 5, 1.6 + distance * 0.8, -2.2 * distance / 5};
            default -> throw new IllegalArgumentException(view);
        };
    }

    static void sheet(File file, String title, Object[][] panels, int columns, int pw, int ph) throws Exception {
        int rows = (panels.length + columns - 1) / columns;
        BufferedImage sheet = new BufferedImage(columns * pw, rows * ph + 36, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(0x101014));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.setColor(new Color(0xEDE3D6));
        g.drawString(title, 12, 25);
        for (int i = 0; i < panels.length; i++) {
            String label = (String)panels[i][0];
            if (panels[i][1] instanceof String reference) {
                reference(g, reference, label, (i % columns) * pw, (i / columns) * ph + 36, pw, ph);
                continue;
            }
            WingRig.Input input = (WingRig.Input)panels[i][1];
            String view = (String)panels[i][2];
            double distance = (double)panels[i][3];
            int background = panels[i].length > 4 ? (int)panels[i][4] : 0x4A5560;
            double[] eye = cam(view, distance);
            double[] target = {0.0, input.form() == WingForm.ONE_WING ? 2.6 : 1.75, 0.0};
            if (input.form() == WingForm.ONE_WING) {
                eye[1] += 0.8;
            }
            Panel panel = new Panel(pw, ph, eye, target, 50.0, background);
            panel.draw(scene(input, input.crouching(), eye));
            int x = (i % columns) * pw;
            int y = (i / columns) * ph + 36;
            g.drawImage(panel.image(), x, y, null);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.setColor(new Color(0x101014));
            g.fillRect(x, y + ph - 22, pw, 22);
            g.setColor(new Color(0xEDE3D6));
            g.drawString(label, x + 8, y + ph - 6);
            g.setColor(new Color(0x101014));
            g.drawRect(x, y, pw - 1, ph - 1);
        }
        ImageIO.write(sheet, "png", file);
        System.out.println("wrote " + file);
    }

    /** A reference image (anime or manga frame), fitted into a panel for side-by-side comparison. */
    static void reference(Graphics2D g, String name, String label, int x, int y, int pw, int ph) throws Exception {
        g.setColor(new Color(0x08080C));
        g.fillRect(x, y, pw, ph);
        File file = new File(System.getProperty("ref", "."), name);
        if (file.isFile()) {
            BufferedImage image = ImageIO.read(file);
            double scale = Math.min(pw / (double)image.getWidth(), (ph - 22) / (double)image.getHeight());
            int w = (int)(image.getWidth() * scale);
            int h = (int)(image.getHeight() * scale);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image, x + (pw - w) / 2, y + (ph - 22 - h) / 2, w, h, null);
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g.setColor(new Color(0x3A1020));
        g.fillRect(x, y + ph - 22, pw, 22);
        g.setColor(new Color(0xFFD8E0));
        g.drawString("REFERENCE: " + label, x + 8, y + ph - 6);
    }

    // ------------------------------------------------------------------ kakugan

    /** A stand-in skin face for the kakugan close-ups (like the user's: brown hair, tan skin, white and violet eyes on rows 4-5). */
    static final int[][] FACE = {
        {0x4A3A30, 0x6E5444, 0x4A3A30, 0x6E5444, 0x6E5444, 0x4A3A30, 0x6E5444, 0x4A3A30},
        {0x56463A, 0x87695A, 0x56463A, 0x87695A, 0x87695A, 0x56463A, 0x87695A, 0x56463A},
        {0x56463A, 0x9A7A64, 0x56463A, 0x93745F, 0x93745F, 0x56463A, 0x9A7A64, 0x56463A},
        {0x56463A, 0x28221A, 0x2E2820, 0xDCBC8C, 0xDCBC8C, 0x2E2820, 0x28221A, 0x56463A},
        {0x2A241C, 0xFAD8FA, 0xA050BE, 0xE6C49A, 0xEEC898, 0xA050BE, 0xFAD8FA, 0x2A241C},
        {0x2E2820, 0xFFFFFF, 0xE070FA, 0xE8C69C, 0xF0CC9C, 0xE070FA, 0xFFFFFF, 0x2E2820},
        {0x4E3E32, 0xE8BC90, 0xEAC496, 0xE6C49A, 0xE6C49A, 0xEAC496, 0xE8BC90, 0x4E3E32},
        {0x4E3E32, 0xE2B48A, 0xE4BC90, 0xE2BC92, 0xE2BC92, 0xE4BC90, 0xE2B48A, 0x4E3E32}};

    /** The stand-in player with a real face, and the kakugan on it, exactly as KakuganModel builds it for the head. */
    /** A second stand-in skin: darker skin, eyes one row higher and set wider, irises on the outer side. */
    static final int[][] FACE_DARK = {
        {0x141012, 0x1C1618, 0x141012, 0x1C1618, 0x1C1618, 0x141012, 0x1C1618, 0x141012},
        {0x1C1618, 0x262022, 0x1C1618, 0x262022, 0x262022, 0x1C1618, 0x262022, 0x1C1618},
        {0x6A4630, 0x1C1618, 0x262022, 0x6E4A34, 0x6E4A34, 0x262022, 0x1C1618, 0x6A4630},
        {0x2E9A5A, 0xF2F2EE, 0x6A4632, 0x72503A, 0x72503A, 0x6A4632, 0xF2F2EE, 0x2E9A5A},
        {0x6C4834, 0x72503A, 0x6E4C36, 0x7A5640, 0x7A5640, 0x6E4C36, 0x72503A, 0x6C4834},
        {0x664430, 0x6E4C36, 0x72503A, 0x7A5640, 0x7A5640, 0x72503A, 0x6E4C36, 0x664430},
        {0x60402C, 0x6A4832, 0x4A2A22, 0x3E2018, 0x3E2018, 0x4A2A22, 0x6A4832, 0x60402C},
        {0x5C3C2A, 0x644430, 0x6A4832, 0x6E4C36, 0x6E4C36, 0x6A4832, 0x644430, 0x5C3C2A}};

    /** An outer (hat) layer with glasses: a black frame and opaque grey lenses over the eyes (0 = empty pixel). */
    static final int[][] GLASSES = new int[8][8];

    static {
        int frame = 0xFF101010;
        int lens = 0xFFB4B8BC;
        for (int u = 0; u < 8; u++) {
            GLASSES[3][u] = frame;
        }
        for (int v = 4; v <= 5; v++) {
            GLASSES[v][0] = frame;
            GLASSES[v][7] = frame;
            GLASSES[v][1] = lens;
            GLASSES[v][2] = lens;
            GLASSES[v][5] = lens;
            GLASSES[v][6] = lens;
        }
        GLASSES[4][3] = frame;
        GLASSES[4][4] = frame;
    }

    /** An outer layer with the skin's own eyes painted on it ("3D eyes"): opaque whites and violet irises over rows 4-5. */
    static final int[][] HAT_EYES = new int[8][8];

    static {
        for (int v = 4; v <= 5; v++) {
            HAT_EYES[v][1] = 0xFFFFFFFF;
            HAT_EYES[v][2] = 0xFF8A3CB4;
            HAT_EYES[v][5] = 0xFF8A3CB4;
            HAT_EYES[v][6] = 0xFFFFFFFF;
        }
    }

    /** The user's own skin (shared for testing): its face, and its second layer with glasses (see-through grey lenses). */
    static final int[][] USER_FACE = {
        {0x191511, 0x1F1A15, 0x241E18, 0x191511, 0x191511, 0x1F1A14, 0x1C1813, 0x1C1813},
        {0x191511, 0x1F1A15, 0x251E18, 0x241E18, 0x1F1A15, 0x1F1A15, 0x241E18, 0x1C1813},
        {0x241E18, 0x241E18, 0x241E18, 0x1F1A15, 0x1F1A15, 0x1F1A15, 0x241E18, 0x241E18},
        {0x806A57, 0x241E18, 0x241E19, 0x1F1A15, 0x1F1A15, 0x1F1A15, 0x241E19, 0x806A56},
        {0x241E18, 0x1F1A15, 0x1F1A15, 0xF9C192, 0xF9C192, 0x1F1A15, 0x1F1A15, 0x241E18},
        {0x806A57, 0xBAC1C1, 0x191919, 0xFFD0AA, 0xFFD0AA, 0x161616, 0xBAC1C1, 0x806A57},
        {0x806A57, 0xE6E6E6, 0x1A1A1A, 0xE5A885, 0xE5A885, 0x050505, 0xE6E6E6, 0x806A57},
        {0xE5A885, 0xF8C093, 0xFFD0AA, 0xFFD0AA, 0xFFD0AA, 0xFFD0AA, 0xF9C193, 0xE5A885}};
    static final int[][] USER_HAT = {
        {0, 0, 0xFF2E261F, 0xFF2E261F, 0xFF2E261F, 0xFF2E261F, 0xFF26201A, 0},
        {0, 0xFF29221C, 0xFF29231C, 0xFF2E261E, 0xFF26201A, 0xFF26201A, 0xFF27201A, 0},
        {0xFF302921, 0xFF29221C, 0xFF29221C, 0xFF2E261F, 0xFF26201A, 0xFF26201A, 0xFF26201A, 0},
        {0xFF312921, 0xFF302921, 0xFF29221C, 0xFF26201A, 0xFF26201A, 0, 0xFF26201A, 0},
        {0xFF3D342A, 0xFF000000, 0xFF000100, 0xFF26201A, 0, 0xFF000000, 0xFF000000, 0},
        {0xFF000000, 0xDFC1C1C1, 0xDFC1C1C1, 0xFF000000, 0xFF000000, 0xDFC1C1C1, 0xDFC1C1C1, 0xFF000000},
        {0, 0xDFC1C1C1, 0xDFC1C1C1, 0, 0, 0xDFC1C1C1, 0xDFC1C1C1, 0},
        {0, 0, 0, 0, 0, 0, 0, 0}};

    /** Where KakuganSkin finds the eyes on the user's skin (checked in KakuganTests). */
    static final KakuganEyes USER_EYES = new KakuganEyes(1, 5, 5, 5, 2, true, false);

    /** The see-through covering cells of an outer layer (KakuganSkin.glass). */
    static long glass(int[][] hat) {
        long mask = 0L;
        for (int v = 0; hat != null && v < 8; v++) {
            for (int u = 0; u < 8; u++) {
                int alpha = hat[v][u] >>> 24;
                if (alpha >= com.hex.kagune.control.kakugan.KakuganSkin.COVERING_ALPHA && alpha < com.hex.kagune.control.kakugan.KakuganSkin.SOLID_ALPHA) {
                    mask |= com.hex.kagune.control.kakugan.KakuganSkin.bit(u, v);
                }
            }
        }
        return mask;
    }

    /** The face cells an outer layer covers, as the game reads them off the skin (KakuganSkin.covered). */
    static long covered(int[][] hat) {
        long mask = 0L;
        for (int v = 0; hat != null && v < 8; v++) {
            for (int u = 0; u < 8; u++) {
                if ((hat[v][u] >>> 24) >= com.hex.kagune.control.kakugan.KakuganSkin.COVERING_ALPHA) {
                    mask |= com.hex.kagune.control.kakugan.KakuganSkin.bit(u, v);
                }
            }
        }
        return mask;
    }

    static List<Tri> kakuganScene(KakuganEyes eyes, float intensity, double time) {
        return kakuganScene(eyes, intensity, time, FACE, 0);
    }

    static List<Tri> kakuganScene(KakuganEyes eyes, float intensity, double time, int[][] face, int seed) {
        List<Tri> tris = new ArrayList<>();
        player(tris, false);
        for (int v = 0; v < 8; v++) {
            for (int u = 0; u < 8; u++) {
                double x0 = (4 - u) / 16.0, x1 = (4 - u - 1) / 16.0, y0 = 2.0 - v / 16.0, y1 = 2.0 - (v + 1) / 16.0, z = 0.2502;
                int c = 0xFF000000 | face[v][u];
                double[] n = {0, 0, 1};
                tris.add(new Tri(new double[][] {{x0, y0, z}, {x1, y0, z}, {x1, y1, z}}, new int[] {c, c, c}, n, true));
                tris.add(new Tri(new double[][] {{x0, y0, z}, {x1, y1, z}, {x0, y1, z}}, new int[] {c, c, c}, n, true));
            }
        }
        if (HAT.get() != null) {
            int[][] hat = HAT.get();
            for (int v = 0; v < 8; v++) {
                for (int u = 0; u < 8; u++) {
                    if (hat[v][u] == 0) {
                        continue;
                    }
                    // The outer layer: half a pixel out, its 8 pixels stretched over 9 (Minecraft's hat inflation).
                    double x0 = (4.5 - u * 1.125) / 16.0, x1 = (4.5 - (u + 1) * 1.125) / 16.0;
                    double y0 = 1.5 + (8.5 - v * 1.125) / 16.0, y1 = 1.5 + (8.5 - (v + 1) * 1.125) / 16.0, z = 4.5 / 16.0;
                    int c = hat[v][u];
                    double[] n = {0, 0, 1};
                    // See-through pixels (glass) are blended over whatever is behind them, as the game draws them now.
                    boolean solid = (c >>> 24) == 255;
                    tris.add(new Tri(new double[][] {{x0, y0, z}, {x1, y0, z}, {x1, y1, z}}, new int[] {c, c, c}, n, solid));
                    tris.add(new Tri(new double[][] {{x0, y0, z}, {x1, y1, z}, {x0, y1, z}}, new int[] {c, c, c}, n, solid));
                }
            }
        }
        long glass = glass(HAT.get());
        for (KakuganModel.Quad q : KakuganModel.build(eyes, seed, time, intensity, true, 0.012, NO_COPIES.get() ? 0L : covered(HAT.get()) & ~glass)) {
            float[] v = q.xyz();
            if (NO_COPIES.get() && !q.outer()) {
                // Before 3.0.0 the glass was drawn first: it hid the kakugan behind it (the skin's own eyes showed instead).
                int u = (int)Math.floor((v[0] + v[3] + v[6] + v[9]) / 4.0F + 4.0F);
                int w = (int)Math.floor((v[1] + v[4] + v[7] + v[10]) / 4.0F + 8.0F);
                if (u >= 0 && u < 8 && w >= 0 && w < 8 && (glass & com.hex.kagune.control.kakugan.KakuganSkin.bit(u, w)) != 0L) {
                    continue;
                }
            }
            double[][] c = new double[4][];
            for (int k = 0; k < 4; k++) {
                // Head model pixels to the preview's player space: x mirrored (the model's +x is the player's left), y up, z forward.
                c[k] = new double[] {-v[k * 3] / 16.0, 1.5 - v[k * 3 + 1] / 16.0, -v[k * 3 + 2] / 16.0};
            }
            double[] n = {-q.nx(), -q.ny(), -q.nz()};
            int[] a = q.argb();
            if (q.glow() && !q.outer() && HAT.get() != null) {
                // The game draws the glow before the glass, which then blends over it: dimmed by the lens's opacity.
                int u = (int)Math.floor((v[0] + v[3] + v[6] + v[9]) / 4.0F + 4.0F);
                int w = (int)Math.floor((v[1] + v[4] + v[7] + v[10]) / 4.0F + 8.0F);
                if (u >= 0 && u < 8 && w >= 0 && w < 8 && (glass & com.hex.kagune.control.kakugan.KakuganSkin.bit(u, w)) != 0L) {
                    double through = 1.0 - (HAT.get()[w][u] >>> 24) / 255.0;
                    a = a.clone();
                    for (int k = 0; k < 4; k++) {
                        int r = (int)(((a[k] >> 16) & 255) * through);
                        int g = (int)(((a[k] >> 8) & 255) * through);
                        int b = (int)((a[k] & 255) * through);
                        a[k] = a[k] & 0xFF000000 | r << 16 | g << 8 | b;
                    }
                }
            }
            tris.add(new Tri(new double[][] {c[0], c[1], c[2]}, new int[] {a[0], a[1], a[2]}, n, false, q.glow()));
            tris.add(new Tri(new double[][] {c[0], c[2], c[3]}, new int[] {a[0], a[2], a[3]}, n, false, q.glow()));
        }
        return tris;
    }

    /** The outer layer drawn over the stand-in face for the panel being rendered (null: none). */
    static final ThreadLocal<int[][]> HAT = new ThreadLocal<>();
    /** True to draw a panel as the game did before 3.0.0 (nothing copied onto the outer layer). */
    static final ThreadLocal<Boolean> NO_COPIES = ThreadLocal.withInitial(() -> false);

    /** Head close-ups: {label, eyes, intensity, time, camera {x,y,z}, night, face, seed, target, hat}. */
    static void kakuganSheet(File file, String title, Object[][] panels, int columns, int pw, int ph) throws Exception {
        int rows = (panels.length + columns - 1) / columns;
        BufferedImage sheet = new BufferedImage(columns * pw, rows * ph + 36, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(0x101014));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.setColor(new Color(0xEDE3D6));
        g.drawString(title, 12, 25);
        for (int i = 0; i < panels.length; i++) {
            String label = (String)panels[i][0];
            int x = (i % columns) * pw;
            int y = (i / columns) * ph + 36;
            if (panels[i][1] instanceof String reference) {
                reference(g, reference, label, x, y, pw, ph);
                continue;
            }
            boolean night = (boolean)panels[i][5];
            double[] eye = (double[])panels[i][4];
            Panel panel;
            int[][] face = panels[i].length > 6 ? (int[][])panels[i][6] : FACE;
            int seed = panels[i].length > 7 ? (int)panels[i][7] : 0;
            double[] target = panels[i].length > 8 ? (double[])panels[i][8] : new double[] {0.0, 1.72, 0.2};
            HAT.set(panels[i].length > 9 ? (int[][])panels[i][9] : null);
            NO_COPIES.set(panels[i].length > 10 && (Boolean)panels[i][10]);
            panel = new Panel(pw, ph, eye, target, 34.0, night ? 0x05070C : 0x7D8B96);
            panel.dim = night ? 0.2 : 1.0;
            panel.draw(kakuganScene((KakuganEyes)panels[i][1], (float)panels[i][2], (double)panels[i][3], face, seed));
            g.drawImage(panel.image(), x, y, null);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.setColor(new Color(0x101014));
            g.fillRect(x, y + ph - 22, pw, 22);
            g.setColor(new Color(0xEDE3D6));
            g.drawString(label, x + 8, y + ph - 6);
        }
        ImageIO.write(sheet, "png", file);
        System.out.println("wrote " + file);
    }

    static boolean wanted(String[] args, String sheet) {
        if (args.length <= 1) {
            return !sheet.startsWith("part-");
        }
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals(sheet) || args[i].equals("all")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Usage: {@code java -Dref=<reference-dir> -cp build/classes tools/WingPreview.java <out-dir> [sheet ...]}. Sheets:
     * forms, actions, closeup, manifest (the default set), and the per-part comparison sheets part-flame,
     * part-crystal, part-onewing (renders next to the reference frames, which are read from -Dref).
     */
    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : ".");
        out.mkdirs();
        int night = 0x1A2234;
        int day = 0x7D8B96;
        double t = 1.3;
        if (wanted(args, "part-flame")) {
            sheet(new File(out, "part-flame.png"), "Gaseous wings (UkakuFlame) against the anime", new Object[][] {
                {"S1 gas puffs (ref 2)", "2.png"},
                {"S1 flame wing, side (ref 11)", "11.png"},
                {"S1 flame wings, front (first-542fbeb8)", "first-542fbeb8-image.png"},
                {"Tokyo Ghoul gaseous, back 3/4", in(WingForm.TWIN, 0, 1, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 6.0, day},
                {"Tokyo Ghoul gaseous, side", in(WingForm.TWIN, 0, 1, t, 0, WingRig.Motion.NONE, 0, 0), "side", 6.0, night},
                {"Tokyo Ghoul gaseous, front 3/4", in(WingForm.TWIN, 0, 1, t, 0, WingRig.Motion.NONE, 0, 0), "front34", 6.0, night},
                {"Cochlea wings (first-ff468f9f)", "first-ff468f9f-image.png"},
                {"Flame wing close-up (first-3b195baf)", "first-3b195baf-image.png"},
                {":re coloured wing (ref 6)", "6.png"},
                {":re gaseous, back 3/4", in(WingForm.TWIN, 800, 1, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 8.5, day},
                {":re gaseous, front 3/4", in(WingForm.TWIN, 800, 1, t, 0, WingRig.Motion.NONE, 0, 0), "front34", 8.5, night},
                {"Rabbit Guise, back 3/4", in(WingForm.RABBIT, 300, 1, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 6.0, day},
                {"flicker t=1.30", in(WingForm.TWIN, 0, 1, 1.30, 0, WingRig.Motion.NONE, 0, 0), "back", 6.0, night},
                {"flicker t=1.40", in(WingForm.TWIN, 0, 1, 1.40, 0, WingRig.Motion.NONE, 0, 0), "back", 6.0, night},
                {"flicker t=1.50", in(WingForm.TWIN, 0, 1, 1.50, 0, WingRig.Motion.NONE, 0, 0), "back", 6.0, night},
                {"emerging 0.2", in(WingForm.TWIN, 0, 0.2F, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 6.0, day},
                {"emerging 0.5", in(WingForm.TWIN, 0, 0.5F, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 6.0, day},
                {"dissolving 0.4", dissolving(WingForm.TWIN, 0, 0.4F, t, 0), "back34", 6.0, day},
                {"Kick, windup", in(WingForm.TWIN, 0, 1, t, 0, WingRig.Motion.KICK, 0.3F, 0), "back34", 6.0, day},
                {"Kick, just after impact", in(WingForm.TWIN, 0, 1, t, 0, WingRig.Motion.KICK, 0.5F, 0), "back34", 6.0, day},
                {"half crystallized (0.5)", in(WingForm.TWIN, 300, 1, t, 0.5F, WingRig.Motion.NONE, 0, 0), "back34", 6.0, day},
            }, 3, 520, 380);
        }
        if (wanted(args, "part-flame-close")) {
            sheet(new File(out, "part-flame-close.png"), "Gaseous wings, close up", new Object[][] {
                {"Season 1 (mastery 0), back 3/4", in(WingForm.TWIN, 0, 1, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 4.6, night},
                {"S1 flame wing (ref 11)", "11.png"},
                {"√A (mastery 450), back 3/4", in(WingForm.TWIN, 450, 1, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 5.2, night},
                {"Cochlea wings (first-ff468f9f)", "first-ff468f9f-image.png"},
                {"Season 1, side", in(WingForm.TWIN, 0, 1, t, 0, WingRig.Motion.NONE, 0, 0), "side", 4.6, day},
                {":re (mastery 800), back", in(WingForm.TWIN, 800, 1, t, 0, WingRig.Motion.NONE, 0, 0), "back", 8.0, night},
            }, 2, 780, 560);
        }
        if (wanted(args, "part-flame-3d")) {
            sheet(new File(out, "part-flame-3d.png"), "Gaseous wings from every side", new Object[][] {
                {"Season 1, back 3/4", in(WingForm.TWIN, 0, 1, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 5.0, night},
                {"Season 1, from above (like the F5 camera)", in(WingForm.TWIN, 0, 1, t, 0, WingRig.Motion.NONE, 0, 0), "above", 5.0, 0x4E7A3A},
                {"Season 1, side", in(WingForm.TWIN, 0, 1, t, 0, WingRig.Motion.NONE, 0, 0), "side", 5.0, day},
                {"√A (450), back 3/4", in(WingForm.TWIN, 450, 1, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 5.6, night},
                {"√A (450), from above", in(WingForm.TWIN, 450, 1, t, 0, WingRig.Motion.NONE, 0, 0), "above", 5.6, 0x4E7A3A},
                {"√A (450), front 3/4", in(WingForm.TWIN, 450, 1, t, 0, WingRig.Motion.NONE, 0, 0), "front34", 5.6, night},
                {":re (1000), back", in(WingForm.TWIN, 1000, 1, t, 0, WingRig.Motion.NONE, 0, 0), "back", 9.0, night},
                {":re (1000), from above", in(WingForm.TWIN, 1000, 1, t, 0, WingRig.Motion.NONE, 0, 0), "above", 9.0, 0x4E7A3A},
                {":re (1000), side", in(WingForm.TWIN, 1000, 1, t, 0, WingRig.Motion.NONE, 0, 0), "side", 9.0, day},
            }, 3, 600, 440);
        }
        if (wanted(args, "part-crystal-3d")) {
            sheet(new File(out, "part-crystal-3d.png"), "Crystallized wings from every side", new Object[][] {
                {"Touka's crystal wing (the crystal look)", "touka.jpg"},
                {"√A crystallized V-wings (ref 3)", "3.png"},
                {"√A (300), back 3/4, close", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back34", 4.2, night},
                {"√A (300), back", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back", 5.6, night},
                {"√A (300), from above", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "above", 5.6, 0x4E7A3A},
                {"√A (300), front 3/4", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "front34", 5.6, day},
                {"crystallizing (half)", in(WingForm.TWIN, 300, 1, t, 0.5F, WingRig.Motion.NONE, 0, 0), "back34", 5.6, night},
                {":re (1000), back 3/4", in(WingForm.TWIN, 1000, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back34", 9.0, night},
                {":re (1000), from above", in(WingForm.TWIN, 1000, 1, t, 1, WingRig.Motion.NONE, 0, 0), "above", 9.0, 0x4E7A3A},
            }, 3, 600, 440);
        }
        if (wanted(args, "kakugan")) {
            KakuganEyes eyes = KakuganEyes.DEFAULT;
            double[] front = {0.0, 1.74, 1.25};
            double[] threeQuarter = {0.75, 1.78, 1.0};
            double[] side = {1.2, 1.74, 0.35};
            KakuganEyes wide = new KakuganEyes(0, 3, 6, 3, 1, false, false);
            double[] close = {0.22, 1.69, 0.72};
            double[] closeTarget = {0.12, 1.66, 0.25};
            kakuganSheet(new File(out, "kakugan-close.png"), "Kakugan up close: the veins (stand-in skins; only the eyes and the skin around them change)",
                new Object[][] {
                {"Kaneki's kakugan (reference)", "eyes-anime-kaneki-closeup.png"},
                {"Right eye up close, day", eyes, 1.0F, 1.0, close, false, FACE, 0, closeTarget},
                {"Right eye up close, night", eyes, 1.0F, 1.0, close, true, FACE, 0, closeTarget},
                {"Another player's veins (own pattern)", eyes, 1.0F, 1.0, front, false, FACE, 1234567},
                {"Different skin: eyes placed higher, wider", wide, 1.0F, 1.0, front, false, FACE_DARK, 42},
                {"Different skin, night", wide, 1.0F, 1.0, front, true, FACE_DARK, 42},
            }, 3, 520, 400);
            double[] head = {0.0, 1.72, 0.2};
            kakuganSheet(new File(out, "kakugan-second-layer.png"), "3.0.0: the kakugan behind see-through glasses, and on eyes painted on the second layer",
                new Object[][] {
                {"Eyes painted on the 2nd layer, before: hidden", eyes, 1.0F, 1.0, front, false, FACE, 0, head, HAT_EYES, true},
                {"Eyes on the 2nd layer, now: copied onto it", eyes, 1.0F, 1.0, front, false, FACE, 0, head, HAT_EYES},
                {"Eyes on the 2nd layer, now, night", eyes, 1.0F, 1.0, threeQuarter, true, FACE, 0, head, HAT_EYES},
                {"Your skin, kakugan off", USER_EYES, 0.0F, 1.0, front, false, USER_FACE, 7, head, USER_HAT},
                {"Your skin, before: normal eyes behind the glass", USER_EYES, 1.0F, 1.0, front, false, USER_FACE, 7, head, USER_HAT, true},
                {"Your skin, now: ghoul eyes behind the glass", USER_EYES, 1.0F, 1.0, front, false, USER_FACE, 7, head, USER_HAT},
                {"Now, three quarters", USER_EYES, 1.0F, 1.0, threeQuarter, false, USER_FACE, 7, head, USER_HAT},
                {"Now, night", USER_EYES, 1.0F, 1.0, front, true, USER_FACE, 7, head, USER_HAT},
                {"Now, night, three quarters", USER_EYES, 1.0F, 1.0, threeQuarter, true, USER_FACE, 7, head, USER_HAT},
            }, 3, 520, 400);
            kakuganSheet(new File(out, "kakugan.png"), "Kakugan: the eyes when the kagune is out (stand-in skin)", new Object[][] {
                {"Anime kakugan (reference)", "eyes-anime-touka.png"},
                {"Kaneki's kakugan (reference)", "eyes-anime-kaneki-closeup.png"},
                {"Kakugan off (the skin as it is)", eyes, 0.0F, 1.0, front, false},
                {"Day, front", eyes, 1.0F, 1.0, front, false},
                {"Day, 3/4", eyes, 1.0F, 1.0, threeQuarter, false},
                {"Day, side", eyes, 1.0F, 1.0, side, false},
                {"Night, front (glowing)", eyes, 1.0F, 1.0, front, true},
                {"Night, 3/4", eyes, 1.0F, 1.0, threeQuarter, true},
                {"Night, heartbeat between thumps", eyes, 1.0F, 1.4, front, true},
                {"Waking up (0.35)", eyes, 0.35F, 1.0, front, false},
                {"Waking up (0.65)", eyes, 0.65F, 1.0, front, false},
                {"Bloodlust flare at dusk (0.6)", eyes, 0.6F, 1.0, threeQuarter, true},
            }, 3, 520, 400);
        }
        if (wanted(args, "part-crystal-views")) {
            sheet(new File(out, "part-crystal-views.png"), "Crystallized wings: back, side, front, F5 above, and the root up close", new Object[][] {
                {"√A (300), back", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back", 5.0, night},
                {"√A (300), side", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "side", 5.0, day},
                {"√A (300), front", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "front", 5.0, night},
                {"√A (300), from above (F5)", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "above", 5.0, 0x4E7A3A},
                {"√A (300), root up close, back", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back", 2.6, night},
                {"√A (300), root up close, back 3/4", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back34", 2.6, day},
                {":re (1000), back", in(WingForm.TWIN, 1000, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back", 8.0, night},
                {":re (1000), side", in(WingForm.TWIN, 1000, 1, t, 1, WingRig.Motion.NONE, 0, 0), "side", 8.0, day},
                {":re (1000), front", in(WingForm.TWIN, 1000, 1, t, 1, WingRig.Motion.NONE, 0, 0), "front", 8.0, night},
                {":re (1000), from above (F5)", in(WingForm.TWIN, 1000, 1, t, 1, WingRig.Motion.NONE, 0, 0), "above", 8.0, 0x4E7A3A},
                {":re (1000), root up close, back", in(WingForm.TWIN, 1000, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back", 3.2, night},
                {":re (1000), root up close, back 3/4", in(WingForm.TWIN, 1000, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back34", 3.2, day},
            }, 3, 600, 440);
        }
        if (wanted(args, "part-root")) {
            sheet(new File(out, "part-root.png"), "Where the wings come out (full mastery, from behind)", new Object[][] {
                {"gaseous, mastery 1000, back", in(WingForm.TWIN, 1000, 1, t, 0, WingRig.Motion.NONE, 0, 0), "back", 6.5, night},
                {"crystallized, mastery 1000, back", in(WingForm.TWIN, 1000, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back", 6.5, night},
                {"gaseous, mastery 0, back", in(WingForm.TWIN, 0, 1, t, 0, WingRig.Motion.NONE, 0, 0), "back", 5.0, night},
            }, 3, 600, 460);
        }
        if (wanted(args, "part-crystal")) {
            sheet(new File(out, "part-crystal.png"), "Crystallized wings, blades and volley (UkakuCrystal) against the anime", new Object[][] {
                {"√A crystallized V-wings (ref 3)", "3.png"},
                {"√A wings spread (ref 10)", "10.png"},
                {"Cochlea wings (first-ff468f9f)", "first-ff468f9f-image.png"},
                {"Tokyo Ghoul crystallized, back", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back", 6.0, day},
                {"Tokyo Ghoul crystallized, back 3/4", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back34", 6.0, night},
                {"Tokyo Ghoul crystallized, front 3/4", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "front34", 6.0, night},
                {"Wing blades (ref 4)", "4.png"},
                {":re manga wing (ref 6)", "6.png"},
                {"Shard barrage (ref 1)", "1.png"},
                {":re crystallized, back 3/4", in(WingForm.TWIN, 800, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back34", 8.5, day},
                {":re crystallized, side", in(WingForm.TWIN, 800, 1, t, 1, WingRig.Motion.NONE, 0, 0), "side", 8.5, night},
                {"Shard Volley, front 3/4", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 1), "front34", 6.0, night},
                {"Right wing blade, windup", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.SLASH_RIGHT, 0.25F, 0), "back34", 6.0, day},
                {"Right wing blade, at impact", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.SLASH_RIGHT, 0.4F, 0), "front34", 6.0, day},
                {"Cross Cut, at impact", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.CROSS, 0.4F, 0), "front34", 6.0, day},
                {"Wing burst, after impact", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.BURST, 0.5F, 0), "back34", 6.0, day},
                {"Shard Volley, back", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 1), "back", 6.0, night},
                {"dissolving 0.5 (crystal)", dissolving(WingForm.TWIN, 300, 0.5F, t, 1), "back34", 6.0, day},
            }, 3, 520, 380);
        }
        if (wanted(args, "part-onewing")) {
            sheet(new File(out, "part-onewing.png"), "One-Winged Kagune (UkakuOneWing) against the anime and manga", new Object[][] {
                {"One-Winged Kagune, anime (first-acae2b55)", "first-acae2b55-image.png"},
                {"One-Winged Kagune, manga (ref 5)", "5.png"},
                {"One-Wing, front 3/4", in(WingForm.ONE_WING, 800, 1, t, 1, WingRig.Motion.NONE, 0, 0), "front34", 8.5, night},
                {"One-Wing, front", in(WingForm.ONE_WING, 800, 1, t, 1, WingRig.Motion.NONE, 0, 0), "front", 8.5, day},
                {"One-Wing, side", in(WingForm.ONE_WING, 800, 1, t, 1, WingRig.Motion.NONE, 0, 0), "side", 8.5, day},
                {"One-Wing, back 3/4", in(WingForm.ONE_WING, 800, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back34", 8.5, day},
                {"Beheading Cut, windup", in(WingForm.ONE_WING, 800, 1, t, 1, WingRig.Motion.ONE_BEHEAD, 0.3F, 0), "front34", 9.0, day},
                {"Beheading Cut, at impact", in(WingForm.ONE_WING, 800, 1, t, 1, WingRig.Motion.ONE_BEHEAD, 0.4F, 0), "front34", 9.0, day},
                {"One-Wing slash, at impact", in(WingForm.ONE_WING, 800, 1, t, 1, WingRig.Motion.ONE_SLASH, 0.4F, 0), "front34", 9.0, night},
                {"emerging 0.4", in(WingForm.ONE_WING, 800, 0.4F, t, 1, WingRig.Motion.NONE, 0, 0), "front34", 8.5, day},
                {"dissolving 0.4", dissolving(WingForm.ONE_WING, 800, 0.4F, t, 1), "front34", 8.5, day},
            }, 3, 520, 380);
        }
        if (wanted(args, "forms")) {
            forms(out, t, day, night);
        }
        if (wanted(args, "actions")) {
            actions(out, t, day, night);
        }
        if (wanted(args, "closeup")) {
            closeup(out, t, day, night);
        }
        if (wanted(args, "manifest")) {
            manifest(out, t, day, night);
        }
    }

    static void forms(File out, double t, int day, int night) throws Exception {
        sheet(new File(out, "ayato-wings-forms.png"), "Ayato's kagune by era (renders of the in-game geometry)", new Object[][] {
            {"Tokyo Ghoul: gaseous (at rest), back", in(WingForm.TWIN, 0, 1, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 6.0, day},
            {"Tokyo Ghoul: crystallized, back", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back34", 6.0, day},
            {"Tokyo Ghoul: crystallized, front", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "front34", 6.0, night},
            {":re: gaseous, back", in(WingForm.TWIN, 800, 1, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 8.5, day},
            {":re: crystallized, back", in(WingForm.TWIN, 800, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back34", 8.5, day},
            {":re: crystallized, front", in(WingForm.TWIN, 800, 1, t, 1, WingRig.Motion.NONE, 0, 0), "front34", 8.5, night},
            {"Rabbit Guise, back", in(WingForm.RABBIT, 300, 1, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 6.0, day},
            {"One-Winged Kagune, front", in(WingForm.ONE_WING, 800, 1, t, 1, WingRig.Motion.NONE, 0, 0), "front34", 8.5, night},
            {"One-Winged Kagune, side", in(WingForm.ONE_WING, 800, 1, t, 1, WingRig.Motion.NONE, 0, 0), "side", 8.5, day},
        }, 3, 560, 420);
    }

    static void actions(File out, double t, int day, int night) throws Exception {
        sheet(new File(out, "ayato-wings-actions.png"), "Ayato's kagune in action", new Object[][] {
            {"Shard Volley, front", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 1), "front34", 6.0, night},
            {"Right wing blade, windup", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.SLASH_RIGHT, 0.25F, 0), "back34", 6.0, day},
            {"Right wing blade, impact", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.SLASH_RIGHT, 0.45F, 0), "front34", 6.0, day},
            {"Cross Cut, impact", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.CROSS, 0.45F, 0), "front34", 6.0, day},
            {"Beheading Cut, windup", in(WingForm.ONE_WING, 800, 1, t, 1, WingRig.Motion.ONE_BEHEAD, 0.3F, 0), "front34", 9.0, day},
            {"Beheading Cut, impact", in(WingForm.ONE_WING, 800, 1, t, 1, WingRig.Motion.ONE_BEHEAD, 0.5F, 0), "front34", 9.0, day},
            {"Kick (wings flare)", in(WingForm.TWIN, 0, 1, t, 0, WingRig.Motion.KICK, 0.4F, 0), "back34", 6.0, day},
        }, 3, 560, 420);
    }

    static void closeup(File out, double t, int day, int night) throws Exception {
        sheet(new File(out, "ayato-wings-closeup.png"), "Close-ups", new Object[][] {
            {"Tokyo Ghoul, crystallized", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back34", 4.6, night},
            {":re, crystallized", in(WingForm.TWIN, 800, 1, t, 1, WingRig.Motion.NONE, 0, 0), "back34", 7.0, night},
            {"Tokyo Ghoul, gaseous", in(WingForm.TWIN, 0, 1, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 4.6, night},
            {"One-Winged Kagune", in(WingForm.ONE_WING, 1000, 1, t, 1, WingRig.Motion.NONE, 0, 0), "front34", 9.5, night},
            {"Right wing folded into a blade", in(WingForm.TWIN, 300, 1, t, 1, WingRig.Motion.SLASH_RIGHT, 0.42F, 0), "front34", 5.5, night},
        }, 3, 900, 640);
    }

    static void manifest(File out, double t, int day, int night) throws Exception {
        sheet(new File(out, "ayato-wings-manifest.png"), "Emerging from the shoulder blades, and dissolving back", new Object[][] {
            {"deploy 0.15", in(WingForm.TWIN, 300, 0.15F, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 6.0, day},
            {"deploy 0.35", in(WingForm.TWIN, 300, 0.35F, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 6.0, day},
            {"deploy 0.6", in(WingForm.TWIN, 300, 0.6F, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 6.0, day},
            {"deploy 1.0", in(WingForm.TWIN, 300, 1.0F, t, 0, WingRig.Motion.NONE, 0, 0), "back34", 6.0, day},
            {"dissolving 0.8", dissolving(WingForm.TWIN, 300, 0.8F, t, 1), "back34", 6.0, day},
            {"dissolving 0.55", dissolving(WingForm.TWIN, 300, 0.55F, t, 1), "back34", 6.0, day},
            {"dissolving 0.3", dissolving(WingForm.TWIN, 300, 0.3F, t, 1), "back34", 6.0, day},
            {"dissolving 0.1", dissolving(WingForm.TWIN, 300, 0.1F, t, 1), "back34", 6.0, day},
        }, 4, 420, 380);
    }

    // ------------------------------------------------------------------ math

    static double[] norm(double x, double y, double z) {
        double l = Math.sqrt(x * x + y * y + z * z);
        return new double[] {x / l, y / l, z / l};
    }

    static double[] norm(double[] v) {
        return norm(v[0], v[1], v[2]);
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static int mixRgb(int a, int b, double t) {
        int r = (int)(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int)(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int)((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
