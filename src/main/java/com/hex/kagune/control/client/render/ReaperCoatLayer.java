package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.animation.ReaperCoatPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.npc.CcgAgent;
import com.hex.kagune.control.outfit.OutfitModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 4.1.37 (the owner: "he should actually like take off an actual jacket, before switching to the other skin"; "you can use the armor that we
 * got ... make sure he takes it off, bro. And you can see him taking it off"): the White Reaper's coat while he sheds it: the Formal
 * Overcoat's own model (its body, sleeves and tails: {@link OutfitModels}), white ({@code textures/entity/ccg/reaper_coat.png}, made by
 * tools/reaper_coat.py), each piece where {@link ReaperCoatPose} has it: worn, coming off his shoulders and down his arms, dropped, flat on
 * the ground behind him (kept where it fell, however he turns), sunk away by the end of his change.
 */
public final class ReaperCoatLayer extends RenderLayer<CcgAgent, PlayerModel<CcgAgent>> {
    private static final RenderType TYPE = RenderType.m_110431_(Objects.requireNonNull(KaguneMod.id("textures/entity/ccg/reaper_coat.png")));
    /** Each Reaper's body turn as his coat came off (degrees) and the change it came off in: it lies where it fell, not where he faces. */
    private static final Map<CcgAgent, double[]> FELL = new WeakHashMap<>();

    public ReaperCoatLayer(RenderLayerParent<CcgAgent, PlayerModel<CcgAgent>> parent) {
        super(parent);
    }

    @Override
    public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, CcgAgent agent, float limbSwing, float limbSwingAmount, float partial,
                        float ageInTicks, float netHeadYaw, float headPitch) {
        try {
            if (agent.m_20145_() || agent.m_9236_() == null || !AgentRules.reaperLook(agent.lookIndex())
                || agent.swing() != CcgAgent.SHED && agent.swing() != CcgAgent.DRESS) {
                FELL.remove(agent);
                return;
            }
            double age = CcgAgent.since(agent.m_9236_().m_46467_(), agent.swingAt()) + partial;
            if (agent.swing() == CcgAgent.DRESS) {
                // 4.1.38: putting it back on: his taking it off, backwards (ReaperDressPose.coatAge), from when his hands have it.
                FELL.remove(agent);
                age = age < AgentRules.REAPER_DRESS_TICKS ? com.hex.kagune.control.client.animation.ReaperDressPose.coatAge(age) : -1.0;
                if (age < 0.0) {
                    return;
                }
            } else {
                age = AgentRules.changeAge(age); // 4.1.74: his change played slower than its keys (AgentRules.REAPER_CHANGE_SLOW)
                if (!ReaperCoatPose.drawn(age)) {
                    return;
                }
            }
            draw(pose, buffers.m_6299_(TYPE), light, agent, age, partial);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("the White Reaper's coat", failure);
        }
    }

    private static void draw(PoseStack pose, VertexConsumer out, int light, CcgAgent agent, double age, float partial) {
        OutfitModels.Model model = OutfitModels.get("formal");
        if (model == null) {
            return;
        }
        RigPose now = AgentModel.pose;
        double ground = 24.0 - (now == null ? 0.0 : now.drop);
        // Once off him it stays where it fell: turned back by as far as he has turned since (the model's frame turns with his body).
        double turn = 0.0;
        if (age >= ReaperCoatPose.OFF) {
            float yaw = agent.f_20884_ + (agent.f_20883_ - agent.f_20884_) * partial;
            double[] fell = FELL.get(agent);
            if (fell == null || fell[1] != agent.swingAt()) {
                fell = new double[] {yaw, agent.swingAt()};
                FELL.put(agent, fell);
            }
            turn = Math.toRadians(fell[0] - yaw);
        } else {
            FELL.remove(agent);
        }
        double c = Math.cos(turn);
        double s = Math.sin(turn);
        boolean slim = agent.lookIndex() == AgentRules.REAPER_LOOK; // his first look's slim arms (before his look turns bare under it)
        Matrix4f matrix = pose.m_85850_().m_252922_();
        Matrix3f normals = pose.m_85850_().m_252943_();
        for (int piece = 0; piece < ReaperCoatPose.PIECES; piece++) {
            double[][][] frame = ReaperCoatPose.frame(piece, age, ground);
            double[][] m = frame[0];
            int bone = ReaperCoatPose.BONE[piece];
            if (slim && (bone == OutfitModels.RIGHT_ARM || bone == OutfitModels.LEFT_ARM)) {
                bone = bone == OutfitModels.RIGHT_ARM ? OutfitModels.RIGHT_ARM_SLIM : OutfitModels.LEFT_ARM_SLIM;
            }
            for (OutfitModels.Cube cube : model.on(bone, ReaperCoatPose.SLOT[piece])) {
                for (OutfitModels.Face face : cube.faces()) {
                    float[] corners = face.corners();
                    float[] uv = face.uvs();
                    float[] fn = face.normal();
                    double nx = m[0][0] * fn[0] + m[0][1] * fn[1] + m[0][2] * fn[2];
                    double ny = m[1][0] * fn[0] + m[1][1] * fn[1] + m[1][2] * fn[2];
                    double nz = m[2][0] * fn[0] + m[2][1] * fn[1] + m[2][2] * fn[2];
                    double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (nl > 1.0E-6) {
                        nx /= nl;
                        ny /= nl;
                        nz /= nl;
                    }
                    double tx = c * nx + s * nz;
                    nz = -s * nx + c * nz;
                    nx = tx;
                    for (int k = 0; k < 4; k++) {
                        double[] q = ReaperCoatPose.place(frame, new double[] {corners[k * 3], corners[k * 3 + 1], corners[k * 3 + 2]});
                        double y = Math.min(q[1], ground - 0.02); // never through the ground
                        double x = c * q[0] + s * q[2];
                        double z = -s * q[0] + c * q[2];
                        out.m_252986_(matrix, (float)(x / 16.0), (float)(y / 16.0), (float)(z / 16.0))
                            .m_6122_(255, 255, 255, 255)
                            .m_7421_(uv[k * 2], uv[k * 2 + 1])
                            .m_86008_(OverlayTexture.f_118083_)
                            .m_85969_(light)
                            .m_252939_(normals, (float)nx, (float)ny, (float)nz)
                            .m_5752_();
                    }
                }
            }
        }
    }
}
