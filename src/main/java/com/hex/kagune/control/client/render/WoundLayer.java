package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.WoundClient;
import com.hex.kagune.control.wound.Wound;
import com.hex.kagune.control.wound.WoundBook;
import com.hex.kagune.control.wound.WoundShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Players' wounds, carved into their bodies (3.0.0; {@link WoundShape}). This layer is put first among the player renderer's
 * layers, so it runs right after the body is handed to its batch and before that batch is drawn (the batch is drawn as soon
 * as anything asks for another one). Right then it draws each wound's walls and floor, and writes each opening into the depth
 * buffer just nearer than the skin with colour off, both at once; when the body's batch reaches the screen a moment later its
 * skin inside the openings fails the depth test, and the flesh shows through: the hole technique of Hex God of Time's Scepter.
 * The wet rims and the blood are then laid over the skin like any layer. The flesh is drawn unlit, so it is darkened here by
 * the light where the player stands.
 */
public final class WoundLayer extends RenderLayer<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> {
    private static final RenderType OVER = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    /** How much nearer than the skin's outer layer an opening is written (blocks, along the line of sight). */
    private static final float LIFT = 0.006F;
    private static final int LEQUAL = 515;

    public WoundLayer(RenderLayerParent<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** One wound polygon this frame: its corners already where they go (view space), or in its part with that part's matrix. */
    private record Piece(float[] points, int[] argb, Matrix4f matrix, Matrix3f normals, float[] normal) {
    }

    @Override
    public void m_6494_(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                        float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        PlayerLayers.ran(player);
        try {
            this.draw(pose, buffers, packedLight, player, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("wound", failure);
        }
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                        float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        WoundBook book = WoundClient.book(player);
        if (book == null || book.wounds().isEmpty() || player.m_20145_() || !(this.m_117386_() instanceof HumanoidModel<?> model)) {
            return;
        }
        boolean slim = "slim".equals(player.m_108564_());
        float light = brightness(packedLight, partialTick);
        List<Piece> carved = new ArrayList<>();
        List<Piece> openings = new ArrayList<>();
        List<Piece> over = new ArrayList<>();
        for (Wound w : book.wounds()) {
            double open = book.openness(w);
            ModelPart part = part(model, w.part());
            if (open <= 0.02 || part == null || !part.f_104207_) {
                continue;
            }
            pose.m_85836_();
            if (w.part() == Wound.Part.HEAD || w.part() == Wound.Part.RIGHT_ARM || w.part() == Wound.Part.LEFT_ARM) {
                PlayerLayers.upperBody(pose, player, partialTick);
            }
            part.m_104299_(pose);
            Matrix4f matrix = new Matrix4f(pose.m_85850_().m_252922_());
            Matrix3f normals = new Matrix3f(pose.m_85850_().m_252943_());
            pose.m_85849_();
            float[] normal = toFloats(w.face().normal());
            WoundShape.build(w, open, w.part().box(slim), (role, points, argb) -> {
                switch (role) {
                    case WALL, FLOOR, BONE -> carved.add(new Piece(view(matrix, points), lit(argb, light), null, null, null));
                    case OPENING -> openings.add(new Piece(nearer(view(matrix, points)), argb, null, null, null));
                    default -> over.add(new Piece(pixels(points), argb, matrix, normals, normal));
                }
            });
        }
        carve(carved, openings);
        if (!over.isEmpty()) {
            VertexConsumer out = buffers.m_6299_(OVER);
            for (Piece piece : over) {
                fan(out, piece.matrix(), piece.points(), piece.argb(), packedLight, piece.normals(), piece.normal());
            }
        }
    }

    /** Draws the flesh now, ahead of the body's own batch, then cuts the openings into the depth buffer. */
    private static void carve(List<Piece> flesh, List<Piece> openings) {
        if (flesh.isEmpty() && openings.isEmpty()) {
            return;
        }
        Matrix4f identity = new Matrix4f();
        BufferBuilder builder = Tesselator.m_85913_().m_85915_();
        VertexConsumer out = builder;
        try {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShader(GameRenderer::m_172811_);
            if (!flesh.isEmpty()) {
                builder.m_166779_(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.f_85815_);
                for (Piece piece : flesh) {
                    triangles(out, identity, piece.points(), piece.argb());
                }
                BufferUploader.m_231202_(builder.m_231175_());
            }
            // Depth only, and only where nothing nearer is drawn already (a block in front of the body stays in front).
            if (!openings.isEmpty()) {
                RenderSystem.colorMask(false, false, false, false);
                builder.m_166779_(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.f_85815_);
                for (Piece piece : openings) {
                    triangles(out, identity, piece.points(), piece.argb());
                }
                BufferUploader.m_231202_(builder.m_231175_());
            }
        } finally {
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthFunc(LEQUAL);
            RenderSystem.enableCull();
        }
    }

    private static void triangles(VertexConsumer out, Matrix4f matrix, float[] points, int[] argb) {
        int n = points.length / 3;
        for (int i = 1; i + 1 < n; i++) {
            for (int corner : new int[] {0, i, i + 1}) {
                int c = argb[corner];
                out.m_252986_(matrix, points[corner * 3], points[corner * 3 + 1], points[corner * 3 + 2])
                    .m_6122_((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, 255)
                    .m_5752_();
            }
        }
    }

    /** A rim or a trickle of blood, as quads (a polygon of n corners fanned into n - 2, the last corner repeated). */
    private static void fan(VertexConsumer out, Matrix4f matrix, float[] points, int[] argb, int light, Matrix3f normals, float[] normal) {
        int n = points.length / 3;
        for (int i = 1; i + 1 < n; i += 2) {
            int[] corners = {0, i, i + 1, Math.min(i + 2, n - 1)};
            for (int corner : corners) {
                int c = argb[corner];
                out.m_252986_(matrix, points[corner * 3], points[corner * 3 + 1], points[corner * 3 + 2])
                    .m_6122_((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, c >>> 24)
                    .m_7421_(0.5F, 0.5F)
                    .m_86008_(OverlayTexture.f_118083_)
                    .m_85969_(light)
                    .m_252939_(normals, normal[0], normal[1], normal[2])
                    .m_5752_();
            }
        }
    }

    /** Part pixels to model units. */
    private static float[] pixels(double[][] points) {
        float[] out = new float[points.length * 3];
        for (int i = 0; i < points.length; i++) {
            out[i * 3] = (float)(points[i][0] / 16.0);
            out[i * 3 + 1] = (float)(points[i][1] / 16.0);
            out[i * 3 + 2] = (float)(points[i][2] / 16.0);
        }
        return out;
    }

    /** Part pixels to view space through the part's matrix. */
    private static float[] view(Matrix4f matrix, double[][] points) {
        float[] out = new float[points.length * 3];
        Vector3f p = new Vector3f();
        for (int i = 0; i < points.length; i++) {
            matrix.transformPosition(p.set((float)(points[i][0] / 16.0), (float)(points[i][1] / 16.0), (float)(points[i][2] / 16.0)));
            out[i * 3] = p.x;
            out[i * 3 + 1] = p.y;
            out[i * 3 + 2] = p.z;
        }
        return out;
    }

    /**
     * A view-space polygon brought nearer the eye, each corner along its own line of sight, by {@link #LIFT}: it covers exactly
     * the pixels it did (as Hex God of Time's {@code BeamWounds.nearer}).
     */
    static float[] nearer(float[] polygon) {
        float[] out = polygon.clone();
        for (int i = 0; i < out.length; i += 3) {
            float distance = (float)Math.sqrt(out[i] * out[i] + out[i + 1] * out[i + 1] + out[i + 2] * out[i + 2]);
            if (distance < 1.0E-6F) {
                continue;
            }
            float keep = 1.0F - Math.min(LIFT, distance * 0.5F) / distance;
            out[i] *= keep;
            out[i + 1] *= keep;
            out[i + 2] *= keep;
        }
        return out;
    }

    /** How bright the light is where the player stands (0..1): the block light, or the sky's as dark as the time of day makes it. */
    private static float brightness(int packedLight, float partialTick) {
        float block = ((packedLight >> 4) & 0xF) / 15.0F;
        float sky = ((packedLight >> 20) & 0xF) / 15.0F;
        float day = Minecraft.m_91087_().f_91073_ == null ? 1.0F : Minecraft.m_91087_().f_91073_.m_104805_(partialTick);
        float bright = Math.max(block, sky * day);
        return 0.12F + 0.88F * bright * bright;
    }

    private static int[] lit(int[] argb, float light) {
        int[] out = new int[argb.length];
        for (int i = 0; i < argb.length; i++) {
            int c = argb[i];
            out[i] = c & 0xFF000000 | (int)(((c >> 16) & 0xFF) * light) << 16 | (int)(((c >> 8) & 0xFF) * light) << 8 | (int)((c & 0xFF) * light);
        }
        return out;
    }

    private static float[] toFloats(double[] v) {
        return new float[] {(float)v[0], (float)v[1], (float)v[2]};
    }

    private static ModelPart part(HumanoidModel<?> model, Wound.Part part) {
        return switch (part) {
            case HEAD -> model.f_102808_;
            case BODY -> model.f_102810_;
            case RIGHT_ARM -> model.f_102811_;
            case LEFT_ARM -> model.f_102812_;
            case RIGHT_LEG -> model.f_102813_;
            case LEFT_LEG -> model.f_102814_;
        };
    }
}
