package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.bikaku.PotentialRules;
import com.hex.kagune.control.client.KaguneTints;
import com.hex.kagune.control.client.PaleClient;
import com.hex.kagune.control.client.PotentialClient;
import com.hex.kagune.control.client.animation.Reattach;
import com.hex.kagune.control.client.animation.Remains;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
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
 * 3.7.3, Potential Unlock (the owner's words in {@code docs/handoff/owner-words-mutations.md}): what of a Bikaku player is off him and
 * coming back, drawn in the world on every client ({@link PotentialClient}, {@link Reattach}): each part in his own skin where it is this
 * frame (thrown, lying where it fell, hauled back), its torn end raw; the wound on him, raw; the strands out of it in his kagune colour
 * (the owner: "All regenerative strands and biological tissue must inherit the player's currently selected kagune color"), reaching,
 * wrapping its torn end, hauling it in, pulsing, sinking into him; the tissue closing between. The world's light falls on all of it.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class PotentialRenderer {
    private static final RenderType FLESH = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    static final double RANGE = 96.0;
    static final int STRAND_SIDES = 5;
    static final int TISSUE_SIDES = 7;
    static final int WOUND = 0xFFB2222E;
    static final int WOUND_DEEP = 0xFF5A0A12;

    record Quad(double[][] p, double[] n, float[] uv, int[] argb) {
    }

    /** Each part's faces in its own frame (its pieces moved as one), worked out once for each kind of skin. */
    private static final Map<Boolean, Map<PotentialRules.Part, List<Quad>>> MESHES = new HashMap<>();

    private PotentialRenderer() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null || minecraft.f_91074_ == null) {
            return;
        }
        float partial = event.getPartialTick();
        double clock = PaleClient.clock(partial);
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        PoseStack stack = event.getPoseStack();
        Matrix4f matrix = stack.m_85850_().m_252922_();
        Matrix3f normals = stack.m_85850_().m_252943_();
        MultiBufferSource.BufferSource buffers = minecraft.m_91269_().m_110104_();
        for (AbstractClientPlayer player : level.m_6907_()) {
            List<PotentialClient.Torn> torn = PotentialClient.of(player);
            if (torn.isEmpty() || player.m_20238_(camera) > RANGE * RANGE || player.m_20145_()) {
                continue;
            }
            try {
                draw(player, torn, partial, clock, level, camera, matrix, normals, buffers);
            } catch (RuntimeException failure) {
                PlayerLayers.failed("a Bikaku player's torn parts", failure);
            }
        }
    }

    /** His frame this frame: feet in the world and his body's turn (as the other Bikaku drawing). */
    record Frame(double x, double y, double z, double yaw, double rx, double rz, double fx, double fz) {
        static Frame of(AbstractClientPlayer player, float partial) {
            double x = player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial;
            double y = player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial;
            double z = player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial;
            double previous = player.f_20884_;
            double delta = player.f_20883_ - previous;
            delta -= 360.0 * Math.floor((delta + 180.0) / 360.0);
            double yaw = Math.toRadians(previous + delta * partial);
            return new Frame(x, y, z, yaw, -Math.cos(yaw), -Math.sin(yaw), -Math.sin(yaw), Math.cos(yaw));
        }

        /** A rig point in the world, relative to the camera. */
        double[] world(double[] rig, Vec3 camera) {
            return new double[] {this.x + this.rx * rig[0] + this.fx * rig[2] - camera.f_82479_, this.y + rig[1] - camera.f_82480_,
                this.z + this.rz * rig[0] + this.fz * rig[2] - camera.f_82481_};
        }

        double[] turn(double[] rig) {
            return new double[] {this.rx * rig[0] + this.fx * rig[2], rig[1], this.rz * rig[0] + this.fz * rig[2]};
        }

        /** Where a part thrown from where he stood then ({@code x, y, z}, his body facing {@code yaw} degrees) lies from him now. */
        Reattach.Anchor anchor(double x, double y, double z, float yaw) {
            double dx = x - this.x;
            double dz = z - this.z;
            return new Reattach.Anchor(new double[] {dx * this.rx + dz * this.rz, y - this.y, dx * this.fx + dz * this.fz},
                Math.toRadians(yaw) - this.yaw);
        }
    }

    static void draw(AbstractClientPlayer player, List<PotentialClient.Torn> torn, float partial, double clock, ClientLevel level, Vec3 camera,
                     Matrix4f matrix, Matrix3f normals, MultiBufferSource.BufferSource buffers) {
        boolean slim = "slim".equals(player.m_108564_());
        Frame frame = Frame.of(player, partial);
        Map<PotentialRules.Part, List<Quad>> meshes = MESHES.computeIfAbsent(slim, s -> build(s));
        Map<Long, Integer> lights = new HashMap<>();
        int tint = 0xFF000000 | KaguneTints.of(player);
        // Each part in his skin where it is (off him).
        RenderType skinType = RenderType.m_110473_(player.m_108560_());
        VertexConsumer skin = buffers.m_6299_(skinType);
        for (PotentialClient.Torn t : torn) {
            double age = t.age(clock);
            if (Reattach.onHim(age)) {
                continue;
            }
            Remains.Placed placed = Reattach.place(t.part(), t.flight(), age, slim, frame.anchor(t.x(), t.y(), t.z(), t.yaw()));
            int light = light(level, lights, frame, placed.at());
            for (Quad quad : meshes.get(t.part())) {
                if (quad.uv() == null) {
                    continue;
                }
                double[] n = frame.turn(Remains.rotate(placed.turn(), quad.n()));
                for (int i = 0; i < 4; i++) {
                    double[] w = frame.world(add(placed.at(), Remains.rotate(placed.turn(), quad.p()[i])), camera);
                    skinVertex(skin, matrix, normals, w, n, quad.uv()[i * 2], quad.uv()[i * 2 + 1], light);
                }
            }
        }
        buffers.m_109912_(skinType);
        VertexConsumer flesh = buffers.m_6299_(FLESH);
        for (PotentialClient.Torn t : torn) {
            double age = t.age(clock);
            Reattach.Anchor anchor = frame.anchor(t.x(), t.y(), t.z(), t.yaw());
            if (!Reattach.onHim(age)) {
                // Its torn end, raw; the wound on him, raw.
                Remains.Placed placed = Reattach.place(t.part(), t.flight(), age, slim, anchor);
                int light = light(level, lights, frame, placed.at());
                for (Quad quad : meshes.get(t.part())) {
                    if (quad.argb() == null) {
                        continue;
                    }
                    double[] n = frame.turn(Remains.rotate(placed.turn(), quad.n()));
                    double[][] p = quad.p();
                    for (int i = 1; i + 1 < p.length; i += 2) {
                        int j = Math.min(p.length - 1, i + 2);
                        int[] corners = {0, i, i + 1, j};
                        for (int c : corners) {
                            vertex(flesh, matrix, normals, frame.world(add(placed.at(), Remains.rotate(placed.turn(), p[c])), camera), n, quad.argb()[c], light);
                        }
                    }
                }
                double[][] wound = Reattach.wound(t.part(), slim);
                disc(flesh, matrix, normals, frame, camera, wound[0], wound[1], t.part() == PotentialRules.Part.HEAD ? 0.14 : 0.11,
                    light(level, lights, frame, wound[0]));
            }
            // The strands, in his kagune colour, a darker vein down every other one.
            double[][][] strands = Reattach.strands(t.part(), t.seed(), t.flight(), age, clock, slim, anchor);
            if (strands != null) {
                for (int s = 0; s < strands.length; s++) {
                    double width = Reattach.STRAND_RADIUS * Reattach.strandWidth(s, age, clock);
                    if (width > 0.002 && strands[s].length >= 2) {
                        int colour = s % 2 == 0 ? tint : darker(tint);
                        tube(flesh, matrix, normals, frame, camera, level, lights, strands[s], STRAND_SIDES, u -> width * (1.0 - 0.35 * u * u), u -> colour);
                    }
                }
            }
            double[][] tissue = Reattach.tissue(t.part(), t.flight(), age, slim, anchor);
            if (tissue != null && tissue[2][0] > 0.01) {
                double radius = 0.1 * tissue[2][0];
                int colour = mix(WOUND, tint);
                tube(flesh, matrix, normals, frame, camera, level, lights, new double[][] {tissue[0], lerp(tissue[0], tissue[1], 0.5), tissue[1]}, TISSUE_SIDES,
                    u -> radius * (0.85 + 0.3 * Math.sin(Math.PI * u)), u -> colour);
            }
        }
        buffers.m_109912_(FLESH);
    }

    /** Every part's faces in its own frame: its pieces' skin (inner layer) and their torn ends. */
    static Map<PotentialRules.Part, List<Quad>> build(boolean slim) {
        Map<PotentialRules.Part, List<Quad>> all = new EnumMap<>(PotentialRules.Part.class);
        for (PotentialRules.Part part : PotentialRules.Part.values()) {
            List<Quad> quads = new ArrayList<>();
            for (Remains.Piece piece : Reattach.piecesOf(part)) {
                Remains.mesh(piece, slim, 7 + part.ordinal(), new Remains.Sink() {
                    @Override
                    public void skin(double[] a, double[] b, double[] c, double[] d, double[] normal, float[] uv, boolean layer) {
                        quads.add(new Quad(new double[][] {local(a), local(b), local(c), local(d)}, normal, uv, null));
                    }

                    @Override
                    public void flesh(double[][] polygon, double[] normal, int[] argb) {
                        double[][] p = new double[polygon.length][];
                        for (int i = 0; i < polygon.length; i++) {
                            p[i] = local(polygon[i]);
                        }
                        quads.add(new Quad(p, normal, null, argb));
                    }

                    private double[] local(double[] own) {
                        return Reattach.local(part, piece, own, slim);
                    }
                });
            }
            all.put(part, quads);
        }
        return all;
    }

    /** A raw disc (rig space) at {@code at}, facing {@code out}. */
    static void disc(VertexConsumer out, Matrix4f matrix, Matrix3f normals, Frame frame, Vec3 camera, double[] at, double[] way, double radius, int light) {
        double[] a = normalize(way);
        double[] side = normalize(cross(a, Math.abs(a[1]) > 0.9 ? new double[] {1.0, 0.0, 0.0} : new double[] {0.0, 1.0, 0.0}));
        double[] up = cross(side, a);
        double[] centre = add(at, mul(a, 0.012));
        double[] n = frame.turn(a);
        int sides = 10;
        for (int i = 0; i < sides; i += 1) {
            double a0 = Math.PI * 2.0 * i / sides;
            double a1 = Math.PI * 2.0 * (i + 1) / sides;
            double[] p0 = add(centre, add(mul(side, Math.cos(a0) * radius), mul(up, Math.sin(a0) * radius)));
            double[] p1 = add(centre, add(mul(side, Math.cos(a1) * radius), mul(up, Math.sin(a1) * radius)));
            vertex(out, matrix, normals, frame.world(centre, camera), n, WOUND_DEEP, light);
            vertex(out, matrix, normals, frame.world(p0, camera), n, WOUND, light);
            vertex(out, matrix, normals, frame.world(p1, camera), n, WOUND, light);
            vertex(out, matrix, normals, frame.world(centre, camera), n, WOUND_DEEP, light);
        }
    }

    /** A tube along {@code path} (rig space), its radius and colour by how far along (0..1). */
    static void tube(VertexConsumer out, Matrix4f matrix, Matrix3f normals, Frame frame, Vec3 camera, ClientLevel level, Map<Long, Integer> lights,
                     double[][] path, int sides, java.util.function.DoubleUnaryOperator radius, java.util.function.DoubleToIntFunction colour) {
        double[][] previous = null;
        double[][] previousNormals = null;
        int previousColour = 0;
        for (int i = 0; i < path.length; i++) {
            double s = path.length < 2 ? 0.0 : i / (path.length - 1.0);
            double[] tangent = normalize(sub(path[Math.min(path.length - 1, i + 1)], path[Math.max(0, i - 1)]));
            double[] a = normalize(cross(tangent, Math.abs(tangent[1]) > 0.9 ? new double[] {1.0, 0.0, 0.0} : new double[] {0.0, 1.0, 0.0}));
            double[] b = cross(tangent, a);
            double r = radius.applyAsDouble(s);
            double[][] ring = new double[sides + 1][];
            double[][] ringNormals = new double[sides + 1][];
            for (int k = 0; k <= sides; k++) {
                double angle = k * Math.PI * 2.0 / sides;
                double[] dir = add(mul(a, Math.cos(angle)), mul(b, Math.sin(angle)));
                ring[k] = frame.world(add(path[i], mul(dir, r)), camera);
                ringNormals[k] = frame.turn(dir);
            }
            int c = colour.applyAsInt(s);
            if (previous != null) {
                int light = light(level, lights, frame, path[i]);
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

    static int light(ClientLevel level, Map<Long, Integer> lights, Frame frame, double[] rig) {
        return PaleFleshRenderer.light(level, lights, frame.x + frame.rx * rig[0] + frame.fx * rig[2], frame.y + rig[1] + 0.1,
            frame.z + frame.rz * rig[0] + frame.fz * rig[2]);
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

    static int darker(int argb) {
        return 0xFF000000 | ((argb >> 16 & 255) * 3 / 5) << 16 | ((argb >> 8 & 255) * 3 / 5) << 8 | (argb & 255) * 3 / 5;
    }

    static int mix(int a, int b) {
        return 0xFF000000 | ((a >> 16 & 255) + (b >> 16 & 255)) / 2 << 16 | ((a >> 8 & 255) + (b >> 8 & 255)) / 2 << 8 | ((a & 255) + (b & 255)) / 2;
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
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

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] normalize(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return l < 1.0E-12 ? new double[] {0.0, 1.0, 0.0} : mul(a, 1.0 / l);
    }
}
