package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.EtoClient;
import com.hex.kagune.control.client.FurutaClient;
import com.hex.kagune.control.client.KakuganClient;
import com.hex.kagune.control.client.animation.BodyFrame;
import com.hex.kagune.control.client.animation.KaguneBodyAnimation;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.furuta.FurutaArmor;
import com.hex.kagune.control.furuta.FurutaSkin;
import com.hex.kagune.control.kakugan.KakuganEyes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Objects;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 7.0.0: the kakuja's body on a Furuta DNA player ({@link FurutaSkin}, {@link FurutaArmor}): the black hide on every part, painted
 * from its texture and its burning slashes drawn again at full brightness, and what stands out of it (the sockets in the lower back,
 * the claws, and the head: crown, star, grin). A layer of the player renderer, so it rides every animation of every part; it spreads
 * over him as the kakuja grows and draws back as it ends ({@link FurutaClient#hide}), the jaw dropping through his attacks.
 */
public final class FurutaArmorLayer extends RenderLayer<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> {
    private static final RenderType SHAPES = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final RenderType HIDE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id(FurutaSkin.TEXTURE)));
    private static final RenderType GLOW = RenderType.m_110488_(Objects.requireNonNull(KaguneMod.id(FurutaSkin.GLOW)));
    /** 7.0.0: what burns (the star, the sockets): added onto what is behind it at full brightness, never shaded by the world's light. */
    private static final RenderType LIGHT = RenderType.m_110488_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final int FULL_BRIGHT = 0xF000F0;

    public FurutaArmorLayer(RenderLayerParent<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void m_6494_(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                        float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        PlayerLayers.ran(player);
        try {
            this.draw(pose, buffers, packedLight, player, partialTick);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("furuta kakuja", failure);
        }
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float partial) {
        if (player.m_20145_() || !FurutaClient.isFuruta(player) || !(this.m_117386_() instanceof HumanoidModel<?> model)) {
            return;
        }
        double clock = EtoClient.clock(partial);
        FurutaClient.View view = FurutaClient.get(player);
        double grow = FurutaClient.hide(view, clock);
        if (grow <= 0.003) {
            return;
        }
        boolean slim = "slim".equals(player.m_108564_());
        Object[][] parts = {{FurutaSkin.Box.HEAD, model.f_102808_, FurutaArmor.Part.HEAD}, {FurutaSkin.Box.TORSO, model.f_102810_, FurutaArmor.Part.TORSO},
            {FurutaSkin.Box.RIGHT_ARM, model.f_102811_, FurutaArmor.Part.RIGHT_ARM}, {FurutaSkin.Box.LEFT_ARM, model.f_102812_, FurutaArmor.Part.LEFT_ARM},
            {FurutaSkin.Box.RIGHT_LEG, model.f_102813_, FurutaArmor.Part.RIGHT_LEG}, {FurutaSkin.Box.LEFT_LEG, model.f_102814_, FurutaArmor.Part.LEFT_LEG}};
        // The elbows and knees as bendy-lib folds them: the hide and the claws fold with them (else the bare forearm shows).
        RigPose limbs = KaguneBodyAnimation.limbs(player);
        VertexConsumer hide = buffers.m_6299_(HIDE);
        for (Object[] entry : parts) {
            FurutaSkin.Box box = (FurutaSkin.Box)entry[0];
            ModelPart part = (ModelPart)entry[1];
            if (part == null || !part.f_104207_) {
                continue;
            }
            pose.m_85836_();
            place(pose, player, partial, box, part);
            Matrix4f matrix = pose.m_85850_().m_252922_();
            Matrix3f normals = pose.m_85850_().m_252943_();
            FurutaSkin.build((corners, uv, n, alpha) -> {
                for (int k = 0; k < 4; k++) {
                    double[] p = bent(limbs, box, slim(box, slim, corners[k]));
                    texel(hide, matrix, normals, p, uv[k], n, 0xFFFFFF, alpha[k], light);
                }
            }, box, grow, 2.0);
            pose.m_85849_();
        }
        VertexConsumer glow = buffers.m_6299_(GLOW);
        // The slashes burn with his pulse.
        double pulse = 0.78 + 0.22 * Math.sin(clock * 0.16) * Math.sin(clock * 0.16);
        for (Object[] entry : parts) {
            FurutaSkin.Box box = (FurutaSkin.Box)entry[0];
            ModelPart part = (ModelPart)entry[1];
            if (box != FurutaSkin.Box.TORSO || part == null || !part.f_104207_) {
                continue;
            }
            pose.m_85836_();
            place(pose, player, partial, box, part);
            Matrix4f matrix = pose.m_85850_().m_252922_();
            Matrix3f normals = pose.m_85850_().m_252943_();
            FurutaSkin.build((corners, uv, n, alpha) -> {
                for (int k = 0; k < 4; k++) {
                    // The eyes render type adds its colour: the spreading edge fades by darkening it.
                    int shade = (int)Math.round(255.0 * pulse * Math.max(0.0, Math.min(1.0, alpha[k])));
                    texel(glow, matrix, normals, corners[k], uv[k], n, shade << 16 | shade << 8 | shade, 1.0, FULL_BRIGHT);
                }
            }, box, grow, 2.0);
            pose.m_85849_();
        }
        VertexConsumer shapes = buffers.m_6299_(SHAPES);
        // Two types never take vertices in turn (they share one buffer): what burns is gathered and drawn after the rest.
        java.util.List<Lit> lit = new java.util.ArrayList<>();
        KakuganEyes eyes = KakuganClient.eyes(player);
        double eyeX = -2.0 + (eyes.column(true) - KakuganEyes.DEFAULT.column(true));
        double eyeY = -3.0 + (eyes.row(true) - KakuganEyes.DEFAULT.row(true)) + (eyes.height() - KakuganEyes.DEFAULT.height()) * 0.5;
        double jaw = FurutaClient.jaw(view, clock);
        double stare = view.move() == com.hex.kagune.control.furuta.FurutaMove.NONE ? 0.0 : Math.min(1.0, jaw * 1.5);
        for (Object[] entry : parts) {
            FurutaSkin.Box box = (FurutaSkin.Box)entry[0];
            ModelPart part = (ModelPart)entry[1];
            FurutaArmor.Part which = (FurutaArmor.Part)entry[2];
            if (part == null || !part.f_104207_ || which == FurutaArmor.Part.RIGHT_LEG || which == FurutaArmor.Part.LEFT_LEG) {
                continue;
            }
            pose.m_85836_();
            place(pose, player, partial, box, part);
            Matrix4f matrix = pose.m_85850_().m_252922_();
            Matrix3f normals = pose.m_85850_().m_252943_();
            Matrix4f keptMatrix = new Matrix4f(matrix);
            Matrix3f keptNormals = new Matrix3f(normals);
            com.hex.kagune.control.ayato.UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
                double[][] q = {bent(limbs, box, a), bent(limbs, box, b), bent(limbs, box, c), bent(limbs, box, d)};
                if (n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0) {
                    // The model's glow convention (a quad lit from straight above): light, added on at full brightness. The
                    // translucent type would shade it by its normal, which on the face points at the ground: the star came out grey.
                    lit.add(new Lit(keptMatrix, keptNormals, q, n, new int[] {ca, cb, cc, cd}));
                    return;
                }
                shape(shapes, matrix, normals, q[0], n, ca, light);
                shape(shapes, matrix, normals, q[1], n, cb, light);
                shape(shapes, matrix, normals, q[2], n, cc, light);
                shape(shapes, matrix, normals, q[3], n, cd, light);
            };
            if (which == FurutaArmor.Part.HEAD) {
                FurutaArmor.head(sink, grow, jaw, eyeX, eyeY, stare, clock, true);
            } else {
                FurutaArmor.build(sink, which, slim, grow, clock, true);
            }
            pose.m_85849_();
        }
        if (!lit.isEmpty()) {
            VertexConsumer lights = buffers.m_6299_(LIGHT);
            for (Lit quad : lit) {
                for (int k = 0; k < 4; k++) {
                    shape(lights, quad.matrix(), quad.normals(), quad.corners()[k], quad.normal(), premultiplied(quad.argb()[k]), FULL_BRIGHT);
                }
            }
        }
    }

    /** A burning quad kept for the light pass, with the part's placement it was built on. */
    private record Lit(Matrix4f matrix, Matrix3f normals, double[][] corners, double[] normal, int[] argb) {
    }

    /** Onto {@code part} as drawn: the head and the arms turned with the upper body as PlayerAnimator turns them. */
    private static void place(PoseStack pose, AbstractClientPlayer player, float partial, FurutaSkin.Box box, ModelPart part) {
        if (box == FurutaSkin.Box.HEAD || box == FurutaSkin.Box.RIGHT_ARM || box == FurutaSkin.Box.LEFT_ARM) {
            PlayerLayers.upperBody(pose, player, partial);
        }
        part.m_104299_(pose);
    }

    /** A point of {@code box} (an arm or a leg; the head and torso never bend) folded at the elbow or knee as {@code limbs} bends it. */
    private static double[] bent(RigPose limbs, FurutaSkin.Box box, double[] p) {
        if (limbs == null) {
            return p;
        }
        return switch (box) {
            case RIGHT_ARM -> BodyFrame.bent(limbs, "rightArm", p);
            case LEFT_ARM -> BodyFrame.bent(limbs, "leftArm", p);
            case RIGHT_LEG -> BodyFrame.bent(limbs, "rightLeg", p);
            case LEFT_LEG -> BodyFrame.bent(limbs, "leftLeg", p);
            default -> p;
        };
    }

    /** An ARGB colour for the additive type: its alpha folded into its colour (that type adds the colour and ignores the alpha). */
    static int premultiplied(int argb) {
        double a = (argb >>> 24) / 255.0;
        int r = (int)Math.round(((argb >> 16) & 0xFF) * a);
        int g = (int)Math.round(((argb >> 8) & 0xFF) * a);
        int b = (int)Math.round((argb & 0xFF) * a);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** A point of the wide arm's hide moved onto the slim arm (a pixel narrower on the outside). */
    private static double[] slim(FurutaSkin.Box box, boolean slim, double[] p) {
        if (!slim) {
            return p;
        }
        if (box == FurutaSkin.Box.RIGHT_ARM) {
            return new double[] {1.0 + (p[0] - 1.0) * 0.75, p[1], p[2]};
        }
        if (box == FurutaSkin.Box.LEFT_ARM) {
            return new double[] {-1.0 + (p[0] + 1.0) * 0.75, p[1], p[2]};
        }
        return p;
    }

    private static void texel(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals, double[] p, double[] uv, double[] n, int rgb, double alpha,
                              int light) {
        consumer.m_252986_(matrix, (float)(p[0] / 16.0), (float)(p[1] / 16.0), (float)(p[2] / 16.0))
            .m_6122_((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, (int)Math.round(255.0 * Math.max(0.0, Math.min(1.0, alpha))))
            .m_7421_((float)uv[0], (float)uv[1])
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
            .m_5752_();
    }

    private static void shape(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals, double[] p, double[] n, int colour, int light) {
        consumer.m_252986_(matrix, (float)(p[0] / 16.0), (float)(p[1] / 16.0), (float)(p[2] / 16.0))
            .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, colour >>> 24)
            .m_7421_(0.5F, 0.5F)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
            .m_5752_();
    }
}
