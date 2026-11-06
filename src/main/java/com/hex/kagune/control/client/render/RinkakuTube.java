package com.hex.kagune.control.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 2.0.39 (the owner: "change the texture to match the reference picture on the tail"): a Rinkaku tail as the owner's picture has it: a
 * broad, flat ribbon (wider than it is thick; 2.0.50: thin where it leaves the back, broadest near its far end, a rounded end), crimson
 * at the middle of each face darkening to deep maroon at its edges, with dark smudges along it, covered in overlapping keeled scales
 * ({@code tools/rinkaku_scales.py}). Every colour is the tail's own kagune colour's ({@code KaguneTint}).
 *
 * <p>2.0.51 (the owner, with screenshots: "patch the bug shown in the image (texture breakage etc)"): the scales were a second layer a hair
 * (1.8%) outside the ribbon, which a few blocks off flickered against it and broke up in patches. Now there is one skin: the scales baked
 * into its texture ({@code rinkaku_skin.png}, greys the tail's colour is multiplied into, {@link com.hex.kagune.control.RinkakuRibbon#lifted}),
 * and over it the glowing veins on a plain white texel. Each tail is kept as it is drawn ({@link #emit}: nothing goes into the original's own
 * buffer, whose batch must not be ended under it) and every one is drawn after the original's tails ({@link #render}: the skins, then the
 * veins over them).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class RinkakuTube {
    private static final RenderType SKIN = RenderType.m_110473_(Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/misc/rinkaku_skin.png")));
    private static final RenderType GLOW = RenderType.m_110473_(Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/misc/white.png")));

    /** One tail kept for the pass after: its mesh, its light and matrices, its seed and the time (its veins' pulse). */
    private record Tail(Matrix4f pose, Matrix3f normals, com.hex.kagune.control.RinkakuRibbon.Mesh mesh, int light, int seed, double time) {
    }

    private static final List<Tail> PENDING = new ArrayList<>();
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Never more than this many kept at once (a frame's worth). */
    private static final int MOST = 96;

    private RinkakuTube() {
    }

    /**
     * Draws a tail along {@code points} ({@code pose}'s space: world for the original's, the camera's for the children's), {@code thickness}
     * its original radius, {@code colour} its kagune colour (0xRRGGBB), {@code seed} its smudges. ({@code out}, the original's buffer, is
     * no longer drawn into: 2.0.51.)
     */
    public static void emit(VertexConsumer out, Matrix4f pose, Matrix3f normals, Vec3[] points, float thickness, int colour, int light, int seed) {
        emit(out, pose, normals, points, thickness, colour, light, seed, null, 0.0);
    }

    /**
     * 2.0.41: the same, seen from {@code eye} (the points' own frame: the camera in the world for the original's, the origin for the
     * children's) at {@code time} (ticks): its glossy rim and glint by the camera, a little clear seen edge-on, its glowing veins pulsing.
     * 2.0.51: alive ({@code RinkakuRibbon.stretch}, {@code swell}).
     */
    public static void emit(VertexConsumer out, Matrix4f pose, Matrix3f normals, Vec3[] points, float thickness, int colour, int light, int seed,
                            double[] eye, double time) {
        if (points == null || points.length < 2 || thickness <= 0.0F || PENDING.size() >= MOST) {
            return;
        }
        double[][] path = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            path[i] = new double[] {points[i].f_82479_, points[i].f_82480_, points[i].f_82481_};
        }
        com.hex.kagune.control.RinkakuRibbon.Mesh mesh = com.hex.kagune.control.RinkakuRibbon.build(path, thickness, colour, seed, eye,
            eye == null ? Double.NaN : time);
        if (mesh != null) {
            PENDING.add(new Tail(new Matrix4f(pose), new Matrix3f(normals), mesh, light, seed, time));
        }
    }

    /** Camera-space tendril for the hand pass, emitted immediately with the same skin and veins as the world tendrils. */
    public static void immediate(MultiBufferSource buffers, Matrix4f pose, Matrix3f normals, double[][] path, float thickness, int colour,
                                 int light, int seed, double time) {
        // 3.1.3: hands are rendered after AFTER_ENTITIES. A first-person whip must be emitted now, never queued for a world pass.
        com.hex.kagune.control.RinkakuRibbon.Mesh mesh = com.hex.kagune.control.RinkakuRibbon.build(path, thickness, colour, seed,
            new double[] {0, 0, 0}, time);
        if (mesh == null) {
            return;
        }
        VertexConsumer skin = buffers.m_6299_(SKIN);
        int sides = com.hex.kagune.control.RinkakuRibbon.SIDES;
        for (int r = 0; r < mesh.at().length - 1; r++) {
            for (int k = 0; k < sides; k++) {
                int[] ks = {k, k, k + 1, k + 1};
                int[] rs = {r, r + 1, r + 1, r};
                for (int v = 0; v < 4; v++) {
                    int argb = com.hex.kagune.control.RinkakuRibbon.lifted(mesh.tint()[rs[v]][ks[v]]);
                    vertex(skin, pose, normals, mesh.at()[rs[v]][ks[v]], mesh.way()[rs[v]][ks[v]], argb, light,
                        com.hex.kagune.control.RinkakuRibbon.u(ks[v]), com.hex.kagune.control.RinkakuRibbon.v(mesh.along()[rs[v]]));
                }
            }
        }
        VertexConsumer glow = buffers.m_6299_(GLOW);
        double[] up = {0, 1, 0};
        for (com.hex.kagune.control.RinkakuRibbon.Glow g : com.hex.kagune.control.RinkakuRibbon.veins(mesh, seed, time)) {
            for (int i = 0; i < 4; i++) {
                vertex(glow, pose, normals, g.corners()[i], up, g.argb()[i], FULL_BRIGHT, 0.5F, 0.5F);
            }
        }
    }

    /** After the original's tails: every tail's skin, then every tail's veins glowing over it. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || PENDING.isEmpty()) {
            return;
        }
        try {
            MultiBufferSource.BufferSource buffers = Minecraft.m_91087_().m_91269_().m_110104_();
            int sides = com.hex.kagune.control.RinkakuRibbon.SIDES;
            VertexConsumer skin = buffers.m_6299_(SKIN);
            for (Tail tail : PENDING) {
                com.hex.kagune.control.RinkakuRibbon.Mesh mesh = tail.mesh();
                for (int r = 0; r < mesh.at().length - 1; r++) {
                    for (int k = 0; k < sides; k++) {
                        int[] ks = {k, k, k + 1, k + 1};
                        int[] rs = {r, r + 1, r + 1, r};
                        for (int v = 0; v < 4; v++) {
                            int argb = com.hex.kagune.control.RinkakuRibbon.lifted(mesh.tint()[rs[v]][ks[v]]);
                            vertex(skin, tail.pose(), tail.normals(), mesh.at()[rs[v]][ks[v]], mesh.way()[rs[v]][ks[v]], argb, tail.light(),
                                com.hex.kagune.control.RinkakuRibbon.u(ks[v]), com.hex.kagune.control.RinkakuRibbon.v(mesh.along()[rs[v]]));
                        }
                    }
                }
            }
            buffers.m_109912_(SKIN);
            // Its veins, glowing: lit from straight above at full brightness (as Ukaku's glowing quads), so the world's light never dims them.
            VertexConsumer glow = buffers.m_6299_(GLOW);
            double[] up = {0.0, 1.0, 0.0};
            for (Tail tail : PENDING) {
                for (com.hex.kagune.control.RinkakuRibbon.Glow g : com.hex.kagune.control.RinkakuRibbon.veins(tail.mesh(), tail.seed(), tail.time())) {
                    for (int i = 0; i < 4; i++) {
                        vertex(glow, tail.pose(), tail.normals(), g.corners()[i], up, g.argb()[i], FULL_BRIGHT, 0.5F, 0.5F);
                    }
                }
            }
            buffers.m_109912_(GLOW);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("a Rinkaku tail", failure);
        } finally {
            PENDING.clear();
        }
    }

    private static void vertex(VertexConsumer out, Matrix4f pose, Matrix3f normals, double[] p, double[] n, int argb, int light, float u, float v) {
        out.m_252986_(pose, (float)p[0], (float)p[1], (float)p[2])
            .m_6122_((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, argb >>> 24)
            .m_7421_(u, v)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
            .m_5752_();
    }
}
