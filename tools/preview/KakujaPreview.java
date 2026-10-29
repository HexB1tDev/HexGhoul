package com.hex.kagune.control.awakening.geo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Offline turnaround of a kakuja model on a grey stand-in player, through the mod's own model
 * runtime (GeoModel, GeoAnimation, GeoPose) with the real textures. A development aid, not part of
 * the mod or the build.
 *
 * <pre>
 *   python3 build.py   # once, for build/awakening-mojang
 *   javac -cp build/awakening-mojang -d build/preview tools/preview/KakujaPreview.java
 *   java -cp build/preview:build/awakening-mojang com.hex.kagune.control.awakening.geo.KakujaPreview war out.png [animation.json clip seconds]
 * </pre>
 * Writes front, right side, back and three-quarter views side by side.
 */
public final class KakujaPreview {
    private static final int VIEW = 420;

    private static final class Raster {
        final BufferedImage image;
        final double[] depth;
        final int offsetX;
        double[] eye;
        double[] forward;
        double[] right;
        double[] up;
        final double focal = VIEW * 1.25;

        Raster(BufferedImage image, double[] depth, int offsetX) {
            this.image = image;
            this.depth = depth;
            this.offsetX = offsetX;
        }

        void look(double[] eye, double[] target) {
            this.eye = eye;
            forward = normalize(sub(target, eye));
            right = normalize(cross(forward, new double[] {0, 1, 0}));
            up = cross(right, forward);
        }

        double[] project(double[] p) {
            double[] d = sub(p, eye);
            double z = dot(d, forward);
            if (z < 0.1) return null;
            return new double[] {offsetX + VIEW / 2.0 + dot(d, right) * focal / z, VIEW * 0.55 - dot(d, up) * focal / z, z};
        }

        void triangle(double[][] p, double[][] uv, BufferedImage texture, BufferedImage glow, int flat, double light) {
            double[] a = project(p[0]);
            double[] b = project(p[1]);
            double[] c = project(p[2]);
            if (a == null || b == null || c == null) return;
            double area = (b[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (b[1] - a[1]);
            if (Math.abs(area) < 1.0E-9) return;
            int minX = (int) Math.max(offsetX, Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
            int maxX = (int) Math.min(offsetX + VIEW - 1, Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
            int minY = (int) Math.max(0, Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
            int maxY = (int) Math.min(VIEW - 1, Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    double px = x + 0.5;
                    double py = y + 0.5;
                    double w0 = ((b[0] - px) * (c[1] - py) - (c[0] - px) * (b[1] - py)) / area;
                    double w1 = ((c[0] - px) * (a[1] - py) - (a[0] - px) * (c[1] - py)) / area;
                    double w2 = 1.0 - w0 - w1;
                    if (w0 < 0 || w1 < 0 || w2 < 0) continue;
                    double iz = w0 / a[2] + w1 / b[2] + w2 / c[2];
                    double z = 1.0 / iz;
                    int i = y * image.getWidth() + x;
                    if (z >= depth[i]) continue;
                    int rgb;
                    if (texture == null) {
                        rgb = scale(flat, light);
                    } else {
                        double u = (w0 * uv[0][0] / a[2] + w1 * uv[1][0] / b[2] + w2 * uv[2][0] / c[2]) * z;
                        double v = (w0 * uv[0][1] / a[2] + w1 * uv[1][1] / b[2] + w2 * uv[2][1] / c[2]) * z;
                        int tx = Math.max(0, Math.min(texture.getWidth() - 1, (int) Math.floor(u * texture.getWidth())));
                        int ty = Math.max(0, Math.min(texture.getHeight() - 1, (int) Math.floor(v * texture.getHeight())));
                        int argb = texture.getRGB(tx, ty);
                        if ((argb >>> 24) < 128) continue;
                        int g = glow == null ? 0 : glow.getRGB(tx, ty);
                        rgb = (g >>> 24) > 128 ? g & 0xFFFFFF : scale(argb & 0xFFFFFF, light);
                    }
                    depth[i] = z;
                    image.setRGB(x, y, rgb);
                }
            }
        }
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] normalize(double[] a) {
        double n = Math.sqrt(dot(a, a));
        return new double[] {a[0] / n, a[1] / n, a[2] / n};
    }

    static int scale(int rgb, double k) {
        int r = (int) Math.min(255, ((rgb >> 16) & 0xFF) * k);
        int g = (int) Math.min(255, ((rgb >> 8) & 0xFF) * k);
        int b = (int) Math.min(255, (rgb & 0xFF) * k);
        return r << 16 | g << 8 | b;
    }

    /** Grey stand-in player in geo space (x negated, front -z): head, body, arms, legs. */
    private static final double[][] PLAYER = {
            {-4, 24, -4, 4, 32, 4}, {-4, 12, -2, 4, 24, 2}, {4, 12, -2, 8, 24, 2}, {-8, 12, -2, -4, 24, 2}, {0, 0, -2, 4, 12, 2}, {-4, 0, -2, 0, 12, 2}};

    private static void box(Raster r, double[] b) {
        double[][] c = new double[8][];
        for (int i = 0; i < 8; i++) c[i] = new double[] {((i & 1) == 0 ? b[0] : b[3]) / 16, ((i & 2) == 0 ? b[1] : b[4]) / 16, ((i & 4) == 0 ? b[2] : b[5]) / 16};
        int[][] faces = {{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
        double[] shades = {0.85, 0.85, 0.7, 1.0, 0.75, 0.75};
        for (int f = 0; f < 6; f++) {
            int[] q = faces[f];
            r.triangle(new double[][] {c[q[0]], c[q[1]], c[q[2]]}, null, null, null, 0x9A9A9A, shades[f]);
            r.triangle(new double[][] {c[q[0]], c[q[2]], c[q[3]]}, null, null, null, 0x9A9A9A, shades[f]);
        }
    }

    public static void main(String[] args) throws Exception {
        String form = args[0];
        Path assets = Path.of("awakening/assets");
        GeoModel model = GeoModel.read(Files.readString(assets.resolve("geo/" + form + ".geo.json")));
        BufferedImage texture = ImageIO.read(assets.resolve("textures/kakuja/" + form + ".png").toFile());
        BufferedImage glow = ImageIO.read(assets.resolve("textures/kakuja/" + form + "_glowmask.png").toFile());
        GeoPose pose = new GeoPose(model);
        if (args.length >= 5) {
            GeoAnimation animation = GeoAnimation.read(Files.readString(Path.of(args[2])));
            pose.apply(animation.clip(args[3]), Double.parseDouble(args[4]), 1.0, Molang.NONE, null);
        }
        double[][] matrices = pose.matrices();
        BufferedImage image = new BufferedImage(VIEW * 4, VIEW, BufferedImage.TYPE_INT_RGB);
        double[] depth = new double[VIEW * 4 * VIEW];
        java.util.Arrays.fill(depth, Double.MAX_VALUE);
        for (int y = 0; y < VIEW; y++) for (int x = 0; x < VIEW * 4; x++) image.setRGB(x, y, 0xE6E6E6);
        double[][] eyes = {{0, 1.6, -5.6}, {-5.6, 1.6, 0}, {0, 1.6, 5.6}, {-4.0, 2.3, -4.0}};
        double[] light = normalize(new double[] {-0.4, 0.8, -0.5});
        for (int v = 0; v < 4; v++) {
            Raster r = new Raster(image, depth, v * VIEW);
            r.look(eyes[v], new double[] {0, 1.35, 0});
            for (double[] b : PLAYER) box(r, b);
            for (int i = 0; i < model.bones.length; i++) {
                if (pose.invisible(i)) continue;
                for (GeoModel.Cube cube : model.bones[i].cubes) {
                    double[] m = Mat4.multiply(matrices[i], GeoPose.cubeMatrix(cube));
                    for (GeoModel.Quad q : cube.quads) {
                        if (q == null) continue;
                        double[][] p = new double[4][];
                        double[][] uv = new double[4][];
                        for (int k = 0; k < 4; k++) {
                            p[k] = Mat4.point(m, q.positions[k * 3], q.positions[k * 3 + 1], q.positions[k * 3 + 2]);
                            uv[k] = new double[] {q.uvs[k * 2], q.uvs[k * 2 + 1]};
                        }
                        double[] n = Mat4.direction(m, q.normalX, q.normalY, q.normalZ);
                        double shade = 0.45 + 0.55 * Math.abs(dot(normalize(n), light));
                        r.triangle(new double[][] {p[0], p[1], p[2]}, new double[][] {uv[0], uv[1], uv[2]}, texture, glow, 0, shade);
                        r.triangle(new double[][] {p[0], p[2], p[3]}, new double[][] {uv[0], uv[2], uv[3]}, texture, glow, 0, shade);
                    }
                }
            }
        }
        ImageIO.write(image, "png", new File(args[1]));
        System.out.println(form + ": " + model.bones.length + " bones, " + model.quadCount() + " quads -> " + args[1]);
    }
}
