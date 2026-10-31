package com.hex.kagune.control.client.render;

import com.hex.kagune.control.FemaleBody;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 1.3.9: the one female body ({@link FemaleBody}) drawn on any humanoid model: Eto's, a female player's, every woman to come. Her
 * legs wider and deeper, a wide-armed skin's arms slim, the bust on the torso (painted with the skin's own torso front, and its second
 * layer over it), bouncing as she moves. Never on a child.
 */
public final class FemaleFigure {
    /** Each body's bust bounce: how far it lags, how fast, her height and the clock when last carried on (by entity id). */
    private static final Map<Integer, double[]> JIGGLES = new HashMap<>();

    private FemaleFigure() {
    }

    /** Her legs (and a player model's trousers) drawn wider and deeper; undone by {@link #unshape}. Pivots are moved where they are set. */
    static void shapeLegs(HumanoidModel<?> model) {
        for (ModelPart leg : legs(model)) {
            if (leg != null) {
                leg.f_233553_ *= FemaleBody.HIPS;
                leg.f_233555_ *= FemaleBody.THIGHS;
            }
        }
    }

    /** A wide-armed skin's arms (and sleeves) drawn as wide as slim ones. */
    static void shapeArms(HumanoidModel<?> model) {
        for (ModelPart arm : arms(model)) {
            if (arm != null) {
                arm.f_233553_ *= FemaleBody.ARMS;
            }
        }
    }

    /** {@link #shapeLegs} (and {@link #shapeArms}, if {@code arms}) undone: each scale divided back (whatever else scaled them too). */
    static void unshape(HumanoidModel<?> model, boolean arms) {
        for (ModelPart leg : legs(model)) {
            if (leg != null) {
                leg.f_233553_ /= FemaleBody.HIPS;
                leg.f_233555_ /= FemaleBody.THIGHS;
            }
        }
        if (arms) {
            for (ModelPart arm : arms(model)) {
                if (arm != null) {
                    arm.f_233553_ /= FemaleBody.ARMS;
                }
            }
        }
    }

    /** Her legs' pivots out so the wider legs meet at the middle (where nothing else sets them every frame: an NPC's model). */
    static void spreadLegs(HumanoidModel<?> model) {
        ModelPart[] legs = legs(model);
        for (int i = 0; i < legs.length; i++) {
            if (legs[i] != null) {
                legs[i].f_104200_ = i % 2 == 0 ? -FemaleBody.LEG_X : FemaleBody.LEG_X;
            }
        }
    }

    /** Right leg, left leg, then (a player model's) their trousers. */
    private static ModelPart[] legs(HumanoidModel<?> model) {
        if (model instanceof PlayerModel<?> player) {
            return new ModelPart[] {model.f_102813_, model.f_102814_, player.f_103377_, player.f_103376_};
        }
        return new ModelPart[] {model.f_102813_, model.f_102814_, null, null};
    }

    private static ModelPart[] arms(HumanoidModel<?> model) {
        if (model instanceof PlayerModel<?> player) {
            return new ModelPart[] {model.f_102811_, model.f_102812_, player.f_103375_, player.f_103374_};
        }
        return new ModelPart[] {model.f_102811_, model.f_102812_, null, null};
    }

    /**
     * The bust on {@code torso} (as {@code torso} is posed), its tip {@code bounce} pixels up off its rest: a wedge the torso's whole
     * width, painted with a 64 x 64 skin's torso front; {@code jacket}: the skin's second layer's, a quarter pixel out over it (its clear
     * texels cut out, as the jacket's are).
     */
    public static void bust(ModelPart torso, PoseStack pose, VertexConsumer consumer, int light, float bounce, boolean jacket) {
        pose.m_85836_();
        torso.m_104299_(pose);
        Matrix4f m = pose.m_85850_().m_252922_();
        Matrix3f n = pose.m_85850_().m_252943_();
        float grow = jacket ? 0.25F : 0.0F;
        float x0 = -4.0F - grow;
        float x1 = 4.0F + grow;
        float top = FemaleBody.BUST_TOP;
        float tip = FemaleBody.BUST_TIP - bounce;
        float bottom = FemaleBody.BUST_BOTTOM;
        float front = FemaleBody.BUST_FRONT - grow;
        float out = FemaleBody.BUST_OUT - grow;
        float v0 = jacket ? 36.0F : 20.0F;
        // The upper slope, then the underside (each a quad), then the two sides (a triangle each, as a quad with a repeated corner).
        quad(consumer, m, n, light, v0, new float[][] {{x0, top, front}, {x1, top, front}, {x1, tip, out}, {x0, tip, out}}, 0.0F, -0.8F, -1.0F);
        quad(consumer, m, n, light, v0, new float[][] {{x0, tip, out}, {x1, tip, out}, {x1, bottom, front}, {x0, bottom, front}}, 0.0F, 0.6F, -1.0F);
        for (float x : new float[] {x0, x1}) {
            quad(consumer, m, n, light, v0, new float[][] {{x, top, front}, {x, tip, out}, {x, bottom, front}, {x, bottom, front}}, Math.signum(x), 0.0F,
                0.0F);
        }
        pose.m_85849_();
    }

    /** One face of the bust, each corner painted with the torso front's texel at its own place (its x across, its y down from {@code v0}). */
    private static void quad(VertexConsumer consumer, Matrix4f m, Matrix3f n, int light, float v0, float[][] corners, float nx, float ny, float nz) {
        for (float[] c : corners) {
            // A side's corners sit on the torso's edge: pulled a quarter pixel in, onto the edge column's texels.
            float across = Math.max(-4.0F, Math.min(4.0F, c[0]));
            float u = (24.0F + across - (nx != 0.0F ? Math.signum(across) * 0.25F : 0.0F)) / 64.0F;
            float v = (v0 + c[1]) / 64.0F;
            consumer.m_252986_(m, c[0] / 16.0F, c[1] / 16.0F, c[2] / 16.0F).m_6122_(255, 255, 255, 255).m_7421_(u, v).m_86008_(OverlayTexture.f_118083_)
                .m_85969_(light).m_252939_(n, nx, ny, nz).m_5752_();
        }
    }

    /** How far (model pixels, up) the bust of body {@code id} is off its rest this frame, her body at height {@code y} (blocks). */
    public static float jiggle(int id, double y, double clock) {
        double[] state = JIGGLES.get(id);
        if (state == null) {
            JIGGLES.put(id, new double[] {0.0, 0.0, y, clock});
            return 0.0F;
        }
        if (clock > state[3]) {
            FemaleBody.jiggle(state, y - state[2], clock - state[3]);
            state[2] = y;
            state[3] = clock;
        }
        return FemaleBody.jigglePixels(state[0]);
    }

    /** Forgets every body no longer about. */
    public static void prune(java.util.function.IntPredicate gone) {
        JIGGLES.keySet().removeIf(gone::test);
    }

    public static void clear() {
        JIGGLES.clear();
    }
}
