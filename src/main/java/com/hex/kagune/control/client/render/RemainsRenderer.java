package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.PaleClient;
import com.hex.kagune.control.client.ReviveClient;
import com.hex.kagune.control.client.animation.Remains;
import com.hex.kagune.control.pale.ReviveRules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 6.2.0, You Thought You'd Get Rid of Me That Easily?: what is left of an Pale player in pieces ({@link ReviveClient},
 * {@link Remains}), drawn in the world in her own skin: every piece where it is this frame (flying, lying where it fell, hauled
 * back), its torn ends raw; her guts in ropes out of both halves of her torso; and, once the five seconds are up, the flesh lines
 * running out of her chest to every piece and hauling them in. The world's light falls on all of it.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class RemainsRenderer {
    private static final RenderType FLESH = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    static final double RANGE = 96.0;
    /** The flesh lines: how thick (blocks, a radius), how many strands twisted round each other, their colours. */
    static final double LINE_RADIUS = 0.028;
    static final int LINE_SIDES = 5;
    static final int LINE_COLOUR = 0xFFB8323E;
    static final int LINE_LIGHT = 0xFFE07A80;
    static final int GUT_SIDES = 6;

    /** One piece's faces in its own frame, worked out once. */
    record Quad(double[][] p, double[] n, float[] uv, boolean layer, int[] argb) {
    }

    private static final Map<ReviveClient.Corpse, List<List<Quad>>> MESHES = new IdentityHashMap<>();

    private RemainsRenderer() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null || minecraft.f_91074_ == null) {
            MESHES.clear();
            return;
        }
        float partial = event.getPartialTick();
        double clock = PaleClient.clock(partial);
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        PoseStack stack = event.getPoseStack();
        Matrix4f matrix = stack.m_85850_().m_252922_();
        Matrix3f normals = stack.m_85850_().m_252943_();
        MultiBufferSource.BufferSource buffers = minecraft.m_91269_().m_110104_();
        List<ReviveClient.Corpse> drawn = new ArrayList<>();
        for (ReviveClient.Corpse corpse : ReviveClient.all()) {
            double age = corpse.age(clock);
            if (!ReviveRules.apart(age) || corpse.feet.m_82557_(camera) > RANGE * RANGE) {
                continue;
            }
            AbstractClientPlayer player = ReviveClient.player(corpse.player);
            if (player == null) {
                continue;
            }
            drawn.add(corpse);
            draw(corpse, player, age, clock, level, camera, matrix, normals, buffers);
        }
        MESHES.keySet().retainAll(drawn);
    }

    static void draw(ReviveClient.Corpse corpse, AbstractClientPlayer player, double age, double clock, ClientLevel level, Vec3 camera, Matrix4f matrix,
                     Matrix3f normals, MultiBufferSource.BufferSource buffers) {
        Remains.Scatter scatter = corpse.scatter;
        List<List<Quad>> meshes = MESHES.computeIfAbsent(corpse, c -> build(c));
        Map<Long, Integer> lights = new HashMap<>();
        double yaw = Math.toRadians(corpse.yaw);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        RenderType skinType = RenderType.m_110473_(player.m_108560_());
        VertexConsumer skin = buffers.m_6299_(skinType);
        for (Remains.Piece piece : Remains.Piece.values()) {
            Remains.Placed placed = Remains.place(scatter, piece, age);
            Vec3 middle = corpse.world(placed.at());
            int light = PaleFleshRenderer.light(level, lights, middle.f_82479_, middle.f_82480_ + 0.2, middle.f_82481_);
            for (Quad quad : meshes.get(piece.ordinal())) {
                if (quad.uv() == null) {
                    continue;
                }
                double[][] w = new double[4][];
                for (int i = 0; i < 4; i++) {
                    w[i] = world(corpse, placed, quad.p()[i], camera, rx, rz, fx, fz);
                }
                double[] n = turn(Remains.rotate(placed.turn(), quad.n()), rx, rz, fx, fz);
                for (int i = 0; i < 4; i++) {
                    skinVertex(skin, matrix, normals, w[i], n, quad.uv()[i * 2], quad.uv()[i * 2 + 1], light);
                }
            }
        }
        buffers.m_109912_(skinType);
        VertexConsumer flesh = buffers.m_6299_(FLESH);
        for (Remains.Piece piece : Remains.Piece.values()) {
            Remains.Placed placed = Remains.place(scatter, piece, age);
            Vec3 middle = corpse.world(placed.at());
            int light = PaleFleshRenderer.light(level, lights, middle.f_82479_, middle.f_82480_ + 0.2, middle.f_82481_);
            for (Quad quad : meshes.get(piece.ordinal())) {
                if (quad.argb() == null) {
                    continue;
                }
                double[] n = turn(Remains.rotate(placed.turn(), quad.n()), rx, rz, fx, fz);
                double[][] p = quad.p();
                // A polygon as a fan of quads (each its first corner, two along, the last repeated).
                for (int i = 1; i + 1 < p.length; i += 2) {
                    int j = Math.min(p.length - 1, i + 2);
                    double[] a = world(corpse, placed, p[0], camera, rx, rz, fx, fz);
                    double[] b = world(corpse, placed, p[i], camera, rx, rz, fx, fz);
                    double[] c = world(corpse, placed, p[i + 1], camera, rx, rz, fx, fz);
                    double[] d = world(corpse, placed, p[j], camera, rx, rz, fx, fz);
                    vertex(flesh, matrix, normals, a, n, quad.argb()[0], light);
                    vertex(flesh, matrix, normals, b, n, quad.argb()[i], light);
                    vertex(flesh, matrix, normals, c, n, quad.argb()[i + 1], light);
                    vertex(flesh, matrix, normals, d, n, quad.argb()[j], light);
                }
            }
        }
        // Her guts.
        for (int rope = 0; rope < Remains.ROPES; rope++) {
            double[][] links = Remains.guts(scatter, rope, age);
            if (links != null) {
                tube(flesh, matrix, normals, corpse, links, camera, level, lights, GUT_SIDES, s -> Remains.GUT_RADIUS * (0.8 + 0.25 * Math.sin(s * 37.0)),
                    s -> ((int)(s * 9.0) & 1) == 0 ? Remains.GUT : Remains.GUT_DARK);
            }
        }
        // The flesh lines out of her chest, two strands twisting round each other.
        for (Remains.Piece piece : Remains.Piece.values()) {
            Remains.Line line = Remains.line(scatter, piece, age);
            if (line == null) {
                continue;
            }
            for (int strand = 0; strand < 2; strand++) {
                double[][] path = linePath(line, strand, clock + piece.ordinal() * 3.1);
                tube(flesh, matrix, normals, corpse, path, camera, level, lights, LINE_SIDES, s -> LINE_RADIUS * (1.0 - 0.5 * s * s),
                    s -> s < 0.08 || s > 0.92 ? LINE_LIGHT : LINE_COLOUR);
            }
        }
        buffers.m_109912_(FLESH);
    }

    /** Every piece's faces in its own frame. */
    static List<List<Quad>> build(ReviveClient.Corpse corpse) {
        List<List<Quad>> all = new ArrayList<>();
        for (Remains.Piece piece : Remains.Piece.values()) {
            List<Quad> quads = new ArrayList<>();
            Remains.mesh(piece, corpse.scatter.slim(), (int)corpse.seed, new Remains.Sink() {
                @Override
                public void skin(double[] a, double[] b, double[] c, double[] d, double[] normal, float[] uv, boolean layer) {
                    quads.add(new Quad(new double[][] {a, b, c, d}, normal, uv, layer, null));
                }

                @Override
                public void flesh(double[][] polygon, double[] normal, int[] argb) {
                    quads.add(new Quad(polygon, normal, null, false, argb));
                }
            });
            all.add(quads);
        }
        return all;
    }

    /** A line's path (rig space): from her chest out as far as it has run, sagging, writhing while it is slack. */
    static double[][] linePath(Remains.Line line, int strand, double time) {
        int points = 14;
        double[][] path = new double[points][];
        double[] d = sub(line.to(), line.from());
        double length = Math.sqrt(dot(d, d));
        double[] along = length < 1.0E-6 ? new double[] {0.0, 1.0, 0.0} : mul(d, 1.0 / length);
        double[] side = normalize(new double[] {-along[2], 0.0, along[0]});
        double[] up = cross(side, along);
        double slack = line.sag() > 0.0 ? 1.0 : 0.0;
        for (int i = 0; i < points; i++) {
            double t = i / (points - 1.0) * line.reach();
            double[] at = add(line.from(), mul(d, t));
            at[1] -= line.sag() * 4.0 * t * (1.0 - t);
            double twist = t * Math.PI * 6.0 + strand * Math.PI + time * 0.4;
            double writhe = slack * 0.07 * Math.sin(t * Math.PI * 3.0 + time * 0.6 + strand) * Math.sin(Math.PI * t);
            at = add(at, add(mul(side, 0.022 * Math.cos(twist) + writhe), mul(up, 0.022 * Math.sin(twist))));
            path[i] = at;
        }
        return path;
    }

    /** A tube along {@code path} (rig space), {@code radius} and {@code colour} by how far along (0..1). */
    static void tube(VertexConsumer out, Matrix4f matrix, Matrix3f normals, ReviveClient.Corpse corpse, double[][] path, Vec3 camera, ClientLevel level,
                     Map<Long, Integer> lights, int sides, java.util.function.DoubleUnaryOperator radius, java.util.function.DoubleToIntFunction colour) {
        double yaw = Math.toRadians(corpse.yaw);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        Remains.Placed none = new Remains.Placed(new double[] {0.0, 0.0, 0.0}, new double[] {1.0, 0.0, 0.0, 0.0});
        double[][] previous = null;
        double[][] previousNormals = null;
        int previousColour = 0;
        for (int i = 0; i < path.length; i++) {
            double s = i / (path.length - 1.0);
            double[] tangent = normalize(sub(path[Math.min(path.length - 1, i + 1)], path[Math.max(0, i - 1)]));
            double[] a = normalize(cross(tangent, Math.abs(tangent[1]) > 0.9 ? new double[] {1.0, 0.0, 0.0} : new double[] {0.0, 1.0, 0.0}));
            double[] b = cross(tangent, a);
            double r = radius.applyAsDouble(s);
            double[][] ring = new double[sides + 1][];
            double[][] ringNormals = new double[sides + 1][];
            for (int k = 0; k <= sides; k++) {
                double angle = k * Math.PI * 2.0 / sides;
                double[] dir = add(mul(a, Math.cos(angle)), mul(b, Math.sin(angle)));
                ring[k] = world(corpse, none, add(path[i], mul(dir, r)), camera, rx, rz, fx, fz);
                ringNormals[k] = turn(dir, rx, rz, fx, fz);
            }
            int c = colour.applyAsInt(s);
            if (previous != null) {
                Vec3 at = corpse.world(path[i]);
                int light = PaleFleshRenderer.light(level, lights, at.f_82479_, at.f_82480_ + 0.1, at.f_82481_);
                for (int k = 0; k < sides; k++) {
                    vertex(out, matrix, normals, previous[k], previousNormals[k], previousColour, light);
                    vertex(out, matrix, normals, previous[k + 1], previousNormals[k + 1], previousColour, light);
                    vertex(out, matrix, normals, ring[k + 1], ringNormals[k + 1], c, light);
                    vertex(out, matrix, normals, ring[k], ringNormals[k], c, light);
                }
            }
            previous = ring;
            previousNormals = ringNormals;
            previousColour = c;
        }
    }

    /** A point of a piece's own frame, placed, in the world relative to the camera. */
    static double[] world(ReviveClient.Corpse corpse, Remains.Placed placed, double[] own, Vec3 camera, double rx, double rz, double fx, double fz) {
        double[] rig = add(placed.at(), Remains.rotate(placed.turn(), own));
        return new double[] {corpse.feet.f_82479_ + rx * rig[0] + fx * rig[2] - camera.f_82479_, corpse.feet.f_82480_ + rig[1] - camera.f_82480_,
            corpse.feet.f_82481_ + rz * rig[0] + fz * rig[2] - camera.f_82481_};
    }

    /** A rig-space direction in the world. */
    static double[] turn(double[] rig, double rx, double rz, double fx, double fz) {
        return new double[] {rx * rig[0] + fx * rig[2], rig[1], rz * rig[0] + fz * rig[2]};
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals, double[] p, double[] n, int colour, int light) {
        consumer.m_252986_(matrix, (float)p[0], (float)p[1], (float)p[2])
            .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, 255)
            .m_7421_(0.5F, 0.5F)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
            .m_5752_();
    }

    private static void skinVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals, double[] p, double[] n, float u, float v, int light) {
        consumer.m_252986_(matrix, (float)p[0], (float)p[1], (float)p[2])
            .m_6122_(255, 255, 255, 255)
            .m_7421_(u, v)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
            .m_5752_();
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] mul(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] normalize(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0E-12 ? new double[] {0.0, 1.0, 0.0} : mul(a, 1.0 / l);
    }
}
