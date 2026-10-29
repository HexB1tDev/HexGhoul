package com.hex.kagune.control.awakening.client;

import com.hex.kagune.control.awakening.V3;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Untextured, vertex-coloured geometry helpers for the Dragon's procedural mesh. */
final class DragonMesh {
    static final int FULL_BRIGHT = 0xF000F0;
    private final VertexConsumer out;
    private final Matrix4f pose;
    private final Matrix3f normal;

    DragonMesh(VertexConsumer out, Matrix4f pose, Matrix3f normal) {
        this.out = out;
        this.pose = pose;
        this.normal = normal;
    }

    void vertex(double x, double y, double z, int rgb, int alpha, double nx, double ny, double nz, int light) {
        out.vertex(pose, (float) x, (float) y, (float) z)
                .color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha)
                .uv(0.5F, 0.5F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, (float) nx, (float) ny, (float) nz)
                .endVertex();
    }

    void vertex(V3 p, int rgb, int alpha, V3 n, int light) {
        vertex(p.x(), p.y(), p.z(), rgb, alpha, n.x(), n.y(), n.z(), light);
    }

    void quad(V3 a, V3 b, V3 c, V3 d, int rgb, int alpha, V3 n, int light) {
        vertex(a, rgb, alpha, n, light);
        vertex(b, rgb, alpha, n, light);
        vertex(c, rgb, alpha, n, light);
        vertex(d, rgb, alpha, n, light);
    }

    void triangle(V3 a, V3 b, V3 c, int rgb, int alpha, int light) {
        V3 n = b.sub(a).cross(c.sub(a)).normalize();
        quad(a, b, c, c, rgb, alpha, n, light);
    }

    /** Filled ellipse in the plane spanned by u and v (unit vectors), facing n. */
    void disc(V3 center, V3 u, V3 v, double ru, double rv, int sides, int rgb, int alpha, V3 n, int light) {
        V3 previous = center.add(u.scale(ru));
        for (int i = 1; i <= sides; i++) {
            double a = Math.PI * 2.0 * i / sides;
            V3 next = center.add(u.scale(Math.cos(a) * ru)).add(v.scale(Math.sin(a) * rv));
            quad(center, previous, next, next, rgb, alpha, n, light);
            previous = next;
        }
    }

    /** Flat annulus in the plane spanned by u and v. */
    void annulus(V3 center, V3 u, V3 v, double inner, double outer, int sides, int innerRgb, int outerRgb, int innerAlpha, int outerAlpha, V3 n, int light) {
        for (int i = 0; i < sides; i++) {
            double a0 = Math.PI * 2.0 * i / sides;
            double a1 = Math.PI * 2.0 * (i + 1) / sides;
            V3 d0 = u.scale(Math.cos(a0)).add(v.scale(Math.sin(a0)));
            V3 d1 = u.scale(Math.cos(a1)).add(v.scale(Math.sin(a1)));
            vertex(center.add(d0.scale(inner)), innerRgb, innerAlpha, n, light);
            vertex(center.add(d0.scale(outer)), outerRgb, outerAlpha, n, light);
            vertex(center.add(d1.scale(outer)), outerRgb, outerAlpha, n, light);
            vertex(center.add(d1.scale(inner)), innerRgb, innerAlpha, n, light);
        }
    }

    /** Tapered prism from a to b; a radius of zero at b makes a cone. */
    void prism(V3 a, V3 b, double ra, double rb, int sides, int rgbA, int rgbB, int light) {
        V3 axis = b.sub(a);
        if (axis.length() < 1.0E-4) return;
        V3 t = axis.normalize();
        V3 u = perpendicular(t);
        V3 v = t.cross(u);
        for (int i = 0; i < sides; i++) {
            double a0 = Math.PI * 2.0 * i / sides;
            double a1 = Math.PI * 2.0 * (i + 1) / sides;
            V3 d0 = u.scale(Math.cos(a0)).add(v.scale(Math.sin(a0)));
            V3 d1 = u.scale(Math.cos(a1)).add(v.scale(Math.sin(a1)));
            V3 mid = d0.add(d1).normalize();
            vertex(a.add(d0.scale(ra)), rgbA, 255, mid, light);
            vertex(b.add(d0.scale(rb)), rgbB, 255, mid, light);
            vertex(b.add(d1.scale(rb)), rgbB, 255, mid, light);
            vertex(a.add(d1.scale(ra)), rgbA, 255, mid, light);
        }
    }

    /** 1.0.29: the expansion's shapes ({@code UkakuModel.Sink} quads, as Furuta's and War's are built) into this mesh: what burns at full light. */
    static com.hex.kagune.control.ayato.UkakuModel.Sink sink(DragonMesh mesh, int light) {
        return (a, b, c, d, n, ca, cb, cc, cd) -> {
            boolean glow = n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0;
            int l = glow ? FULL_BRIGHT : light;
            mesh.vertex(a[0], a[1], a[2], ca & 0xFFFFFF, ca >>> 24, n[0], n[1], n[2], l);
            mesh.vertex(b[0], b[1], b[2], cb & 0xFFFFFF, cb >>> 24, n[0], n[1], n[2], l);
            mesh.vertex(c[0], c[1], c[2], cc & 0xFFFFFF, cc >>> 24, n[0], n[1], n[2], l);
            mesh.vertex(d[0], d[1], d[2], cd & 0xFFFFFF, cd >>> 24, n[0], n[1], n[2], l);
        };
    }

    static V3 perpendicular(V3 t) {
        V3 seed = Math.abs(t.y()) < 0.9 ? V3.UP : new V3(1.0, 0.0, 0.0);
        return seed.sub(t.scale(seed.dot(t))).normalize();
    }

    /** Surface colouring and relief for {@link #tube}; {@code around} is the radial direction. */
    interface Skin {
        double radius(int ring, double around, V3 direction);

        int color(int ring, double around, V3 direction);
    }

    /**
     * A tube along a centreline using parallel-transported frames so it never twists. Rings with a
     * radius below 1 cm collapse to a point, which lets tubes taper into the ground or a tip.
     */
    void tube(V3[] centers, double[] radii, int sides, Skin skin, int[] lights) {
        int count = centers.length;
        if (count < 2) return;
        V3[][] points = new V3[count][sides];
        V3[][] normals = new V3[count][sides];
        int[][] colors = new int[count][sides];
        V3 tangent = centers[1].sub(centers[0]).normalize();
        if (tangent.length() < 1.0E-6) tangent = V3.UP;
        V3 n = perpendicular(tangent);
        for (int k = 0; k < count; k++) {
            V3 forward = centers[Math.min(count - 1, k + 1)].sub(centers[Math.max(0, k - 1)]).normalize();
            if (forward.length() < 1.0E-6) forward = tangent;
            V3 transported = n.sub(forward.scale(n.dot(forward)));
            n = transported.length() < 1.0E-6 ? perpendicular(forward) : transported.normalize();
            V3 b = forward.cross(n);
            tangent = forward;
            for (int s = 0; s < sides; s++) {
                double around = Math.PI * 2.0 * s / sides;
                V3 direction = n.scale(Math.cos(around)).add(b.scale(Math.sin(around)));
                double r = radii[k] < 0.01 ? 0.0 : skin.radius(k, around, direction) * radii[k];
                points[k][s] = centers[k].add(direction.scale(r));
                normals[k][s] = direction;
                colors[k][s] = skin.color(k, around, direction);
            }
        }
        for (int k = 0; k + 1 < count; k++) {
            if (radii[k] < 0.01 && radii[k + 1] < 0.01) continue;
            int light = lights[Math.min(lights.length - 1, k)];
            for (int s = 0; s < sides; s++) {
                int s1 = (s + 1) % sides;
                vertex(points[k][s], colors[k][s], 255, normals[k][s], light);
                vertex(points[k + 1][s], colors[k + 1][s], 255, normals[k + 1][s], light);
                vertex(points[k + 1][s1], colors[k + 1][s1], 255, normals[k + 1][s1], light);
                vertex(points[k][s1], colors[k][s1], 255, normals[k][s1], light);
            }
        }
    }
}
