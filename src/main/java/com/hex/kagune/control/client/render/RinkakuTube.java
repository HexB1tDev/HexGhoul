package com.hex.kagune.control.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
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
 * broad, flat ribbon (wider than it is thick, {@link #WIDE}, {@link #THIN}), swelling a little from the back then tapering to a point,
 * crimson at the middle of each face darkening to deep maroon at its edges, with dark smudges along it ({@link #shade}); over it the
 * markings ({@code tools/rinkaku_scales.py}): thin chevrons black and light all over it, pointing back toward the body, and a light stripe
 * along each face near one edge. Every colour is the tail's own kagune colour's (its palette turned to it, {@code KaguneTint}).
 *
 * <p>Drawn in two passes: the ribbon at once into the original's own buffer (a white texel tinted per vertex, as before), and the
 * markings a hair outside it, kept until after every tail is drawn ({@link #render}): switching buffers in the middle of the original's
 * drawing would end its batch under it.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class RinkakuTube {
    private static final RenderType MARKS = RenderType.m_110473_(Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/misc/rinkaku_scales.png")));

    /** One tail's markings, kept for the pass after: its rings (where, the way out, how far along), its tint, light and matrices. */
    private record Marks(Matrix4f pose, Matrix3f normals, com.hex.kagune.control.RinkakuRibbon.Mesh mesh, int light) {
    }

    private static final List<Marks> PENDING = new ArrayList<>();

    private RinkakuTube() {
    }

    /**
     * Draws a tail along {@code points} ({@code pose}'s space: world for the original's, the camera's for the children's) into
     * {@code out} (the original's white-texel buffer), {@code thickness} its original radius, {@code colour} its kagune colour (0xRRGGBB:
     * the owner: "we gotta adapt the texture to the different colors": every colour of it is the reference palette turned to this one,
     * {@link com.hex.kagune.control.RinkakuRibbon}), {@code seed} its smudges.
     */
    public static void emit(VertexConsumer out, Matrix4f pose, Matrix3f normals, Vec3[] points, float thickness, int colour, int light, int seed) {
        if (points == null || points.length < 2 || thickness <= 0.0F) {
            return;
        }
        double[][] path = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            path[i] = new double[] {points[i].f_82479_, points[i].f_82480_, points[i].f_82481_};
        }
        com.hex.kagune.control.RinkakuRibbon.Mesh mesh = com.hex.kagune.control.RinkakuRibbon.build(path, thickness, colour, seed);
        if (mesh == null) {
            return;
        }
        int rings = mesh.at().length;
        int sides = com.hex.kagune.control.RinkakuRibbon.SIDES;
        for (int r = 0; r < rings - 1; r++) {
            for (int k = 0; k < sides; k++) {
                vertex(out, pose, normals, mesh.at()[r][k], mesh.way()[r][k], mesh.tint()[r][k], light, 0.5F, 0.5F);
                vertex(out, pose, normals, mesh.at()[r + 1][k], mesh.way()[r + 1][k], mesh.tint()[r + 1][k], light, 0.5F, 0.5F);
                vertex(out, pose, normals, mesh.at()[r + 1][k + 1], mesh.way()[r + 1][k + 1], mesh.tint()[r + 1][k + 1], light, 0.5F, 0.5F);
                vertex(out, pose, normals, mesh.at()[r][k + 1], mesh.way()[r][k + 1], mesh.tint()[r][k + 1], light, 0.5F, 0.5F);
            }
        }
        if (PENDING.size() < 64) {
            PENDING.add(new Marks(new Matrix4f(pose), new Matrix3f(normals), mesh, light));
        }
    }

    /** After every tail: their markings, a hair outside each, in their light colour (black marks stay black). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || PENDING.isEmpty()) {
            return;
        }
        try {
            MultiBufferSource.BufferSource buffers = Minecraft.m_91087_().m_91269_().m_110104_();
            VertexConsumer out = buffers.m_6299_(MARKS);
            int sides = com.hex.kagune.control.RinkakuRibbon.SIDES;
            for (Marks marks : PENDING) {
                com.hex.kagune.control.RinkakuRibbon.Mesh mesh = marks.mesh();
                double[][][] over = com.hex.kagune.control.RinkakuRibbon.over(mesh);
                for (int r = 0; r < mesh.at().length - 1; r++) {
                    for (int k = 0; k < sides; k++) {
                        int[] ks = {k, k, k + 1, k + 1};
                        int[] rs = {r, r + 1, r + 1, r};
                        for (int v = 0; v < 4; v++) {
                            vertex(out, marks.pose(), marks.normals(), over[rs[v]][ks[v]], mesh.way()[rs[v]][ks[v]], mesh.light(), marks.light(),
                                com.hex.kagune.control.RinkakuRibbon.u(ks[v]), com.hex.kagune.control.RinkakuRibbon.v(mesh.along()[rs[v]]));
                        }
                    }
                }
            }
            buffers.m_109912_(MARKS);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("a Rinkaku tail's markings", failure);
        } finally {
            PENDING.clear();
        }
    }

    private static void vertex(VertexConsumer out, Matrix4f pose, Matrix3f normals, double[] p, double[] n, int argb, int light, float u, float v) {
        out.m_252986_(pose, (float)p[0], (float)p[1], (float)p[2])
            .m_6122_((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, 0xFF)
            .m_7421_(u, v)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
            .m_5752_();
    }

}
