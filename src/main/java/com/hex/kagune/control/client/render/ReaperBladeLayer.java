package com.hex.kagune.control.client.render;

import com.hex.kagune.control.client.animation.ReaperBladePose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.npc.CcgAgent;
import com.hex.kagune.control.quinque.ArmMesh;
import com.hex.kagune.control.quinque.ReaperBladeMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Objects;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 4.1.29: the White Reaper's second-phase blade ({@link ReaperBladeMesh}) in his right fist, turned there as his pose has it
 * ({@link ReaperBladePose}: the weapon channels); while he draws it, the golden case hanging from his fist by its handle, shrinking into the
 * grip as the blade grows out of it. Drawn while he wears his bare look; no item (nothing to drop, nothing in his hand).
 */
public final class ReaperBladeLayer extends RenderLayer<CcgAgent, PlayerModel<CcgAgent>> {
    private static final RenderType SOLID = RenderType.m_110473_(Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/misc/white.png")));
    private static final int FULL_BRIGHT = 0xF000F0;

    public ReaperBladeLayer(RenderLayerParent<CcgAgent, PlayerModel<CcgAgent>> parent) {
        super(parent);
    }

    @Override
    public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, CcgAgent agent, float limbSwing, float limbSwingAmount, float partial,
                        float ageInTicks, float netHeadYaw, float headPitch) {
        if (agent.m_20145_() || agent.m_9236_() == null || agent.lookIndex() != AgentRules.REAPER_BARE_LOOK) {
            return;
        }
        try {
            draw(pose, buffers, light, agent, partial);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("the White Reaper's blade", failure);
        }
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, int light, CcgAgent agent, float partial) {
        RigPose posed = AgentModel.pose;
        double after = Double.NaN;
        if (agent.swing() == CcgAgent.SHED) {
            // 4.1.74: the change played slower than its keys (AgentRules.REAPER_CHANGE_SLOW).
            double since = AgentRules.changeAge(CcgAgent.since(agent.m_9236_().m_46467_(), agent.swingAt()) + partial) - AgentRules.REAPER_SHED_TICKS;
            if (since < AgentRules.REAPER_DRAW_TICKS) {
                after = since;
            }
        }
        VertexConsumer out = buffers.m_6299_(SOLID);
        if (agent.swing() == CcgAgent.DRESS) {
            stowed(pose, out, CcgAgent.since(agent.m_9236_().m_46467_(), agent.swingAt()) + partial, posed, light);
            return;
        }
        if (agent.swing() == CcgAgent.END) {
            ended(pose, out, agent, posed, CcgAgent.since(agent.m_9236_().m_46467_(), agent.swingAt()) + partial, light);
            return;
        }
        boolean drawing = !Double.isNaN(after);
        if (drawing && after < AgentRules.REAPER_CASE_FROM) {
            return; // still taking the jacket off, or reaching behind his back
        }
        if (drawing && after >= AgentRules.REAPER_CASE_DROPS) {
            fallen(pose, out, after, posed, light); // the empty case dropped at his feet
        }
        pose.m_85836_();
        this.m_117386_().f_102811_.m_104299_(pose);
        double[] fist = {-1.0, 9.0, 0.0};
        if (drawing && ReaperBladePose.caseHeld(after) && posed != null) {
            double[][] turn = ReaperBladePose.caseTurn(posed, after);
            double angle = ReaperBladePose.caseOpen(after);
            for (int side : new int[] {1, -1}) {
                emitCase(pose, out, ReaperBladeMesh.goldHalf(side), turn, fist, side, angle, light);
            }
            if (after < AgentRules.REAPER_CASE_OPENS) {
                emitCase(pose, out, ReaperBladeMesh.goldHandle(), turn, fist, 1, 0.0, light);
            }
        }
        if (!drawing || after >= AgentRules.REAPER_CASE_OPENS) {
            double[][] inArm = posed == null ? ReaperBladePose.HELD : ReaperBladePose.inArm(posed);
            emit(pose, out, ReaperBladeMesh.of(), inArm, fist, drawing ? ReaperBladePose.extended(after) : 1.0F, light);
        }
        pose.m_85849_();
    }

    /**
     * 4.1.38: putting his jacket back on ({@code t} ticks in: ReaperDressPose): the blade until it is back in the golden case, sliding into it;
     * the case in his left hand, carried to his right fist and opened about the blade, shut, then hung from his right hand and put away.
     */
    private void stowed(PoseStack pose, VertexConsumer out, double t, RigPose posed, int light) {
        if (posed == null || t >= AgentRules.REAPER_STOW_TICKS) {
            return;
        }
        pose.m_85836_();
        this.m_117386_().f_102811_.m_104299_(pose);
        double[] fist = {-1.0, 9.0, 0.0};
        com.hex.kagune.control.client.animation.ReaperDressPose.Held held = com.hex.kagune.control.client.animation.ReaperDressPose.held(posed, t);
        if (held != null) {
            for (int side : new int[] {1, -1}) {
                emitCase(pose, out, ReaperBladeMesh.goldHalf(side), held.turn(), held.origin(), side, held.open(), light);
            }
            if (held.handle()) {
                emitCase(pose, out, ReaperBladeMesh.goldHandle(), held.turn(), held.origin(), 1, 0.0, light);
            }
        }
        float k = com.hex.kagune.control.client.animation.ReaperDressPose.bladeOut(t);
        if (k >= 0.0F) {
            emit(pose, out, ReaperBladeMesh.of(), ReaperBladePose.inArm(posed), fist, k, light);
        }
        pose.m_85849_();
    }

    /**
     * 4.1.31: his end ({@code age} ticks in): the whole blade until it shatters, then the broken hilt in his hand and its shards flown out
     * from where the blade was, falling to the ground about him and lying there. 4.1.76 (the owner: "just make the blade instantly shatter
     * while he's holding it on one hand"): all at once again (4.1.74's piece at a time gone).
     */
    private void ended(PoseStack pose, VertexConsumer out, CcgAgent agent, RigPose posed, double age, int light) {
        double[] fist = {-1.0, 9.0, 0.0};
        if (age >= AgentRules.REAPER_END_SHATTER) {
            shards(pose, out, age, posed, light);
        }
        pose.m_85836_();
        this.m_117386_().f_102811_.m_104299_(pose);
        double[][] inArm = posed == null ? ReaperBladePose.HELD : ReaperBladePose.inArm(posed);
        emit(pose, out, age >= AgentRules.REAPER_END_SHATTER ? ReaperBladeMesh.broken() : ReaperBladeMesh.of(), inArm, fist, 1.0F, light);
        pose.m_85849_();
    }

    /**
     * 4.1.31: the shattered blade's shards, {@code age} ticks into his end (the model's frame): each a thin sliver, both sides drawn, all
     * from the tick it shatters (AgentRules.REAPER_END_SHATTER).
     */
    static void shards(PoseStack pose, VertexConsumer out, double age, RigPose now, int light) {
        Matrix4f at = pose.m_85850_().m_252922_();
        Matrix3f normals = pose.m_85850_().m_252943_();
        // 4.1.75 (the owner: "make him actually lay on the floor face down like falling on his face"): worked out in the frame of his pose as
        // the blade broke, then carried into his body's frame now (ReaperBladePose.carried): the model is turned as he falls, the shards
        // lying on the ground are not.
        RigPose broke = ReaperBladePose.shatterPose();
        double ground = 24.0 - broke.drop - 0.05;
        for (int i = 0; i < com.hex.kagune.control.npc.ReaperBladeRules.SHARDS; i++) {
            double dt = age - AgentRules.REAPER_END_SHATTER;
            double[][] s = ReaperBladePose.shard(i, dt);
            double fall = 0.5 * com.hex.kagune.control.npc.ReaperBladeRules.SHARD_FALL * dt * dt;
            double[] c = s[0];
            double[] l = s[1];
            double[] w = s[2];
            double low = c[1] + fall + Math.abs(l[1]) * 0.5 + Math.abs(w[1]) * 0.5;
            double y = c[1] + fall;
            if (low > ground) {
                // Down: lying flat where it fell.
                y = ground - 0.1;
                l = new double[] {l[0], 0.0, l[2]};
                w = new double[] {w[0], 0.0, w[2]};
                if (Math.hypot(l[0], l[2]) < 1.0E-3) {
                    l = new double[] {0.6, 0.0, 0.0};
                }
                if (Math.hypot(w[0], w[2]) < 1.0E-3) {
                    w = new double[] {0.0, 0.0, 0.3};
                }
            }
            double[][] q = {{c[0] - l[0] * 0.5, y - l[1] * 0.5, c[2] - l[2] * 0.5}, {c[0] + w[0], y + w[1], c[2] + w[2]},
                {c[0] + l[0] * 0.5, y + l[1] * 0.5, c[2] + l[2] * 0.5}, {c[0] - w[0], y - w[1], c[2] - w[2]}};
            double[] n = ReaperBladePose.carried(broke, now, ReaperBladePose.normal(l, w), true);
            for (int k = 0; k < 4; k++) {
                q[k] = ReaperBladePose.carried(broke, now, q[k], false);
            }
            int silver = i % 4 == 0 ? 0xFF9A0C16 : i % 3 == 0 ? 0xFFE8E9EB : 0xFFC9CBCE;
            for (int side = 0; side < 2; side++) {
                for (int k = 0; k < 4; k++) {
                    double[] v = q[side == 0 ? k : 3 - k];
                    float sign = side == 0 ? 1.0F : -1.0F;
                    out.m_252986_(at, (float)(v[0] / 16.0), (float)(v[1] / 16.0), (float)(v[2] / 16.0))
                        .m_6122_(silver >> 16 & 255, silver >> 8 & 255, silver & 255, 255).m_7421_(0.5F, 0.5F).m_86008_(OverlayTexture.f_118083_)
                        .m_85969_(light).m_252939_(normals, sign * (float)n[0], sign * (float)n[1], sign * (float)n[2]).m_5752_();
                }
            }
        }
    }

    /** A half of the case (or its handle), swung {@code angle} open on its hinge, turned into the arm's frame by {@code turn}, in his fist. */
    static void emitCase(PoseStack pose, VertexConsumer out, ArmMesh.Mesh mesh, double[][] turn, double[] fist, int side, double angle, int light) {
        Matrix4f at = pose.m_85850_().m_252922_();
        Matrix3f normals = pose.m_85850_().m_252943_();
        for (int v = 0; v < mesh.quads * 4; v++) {
            int i = v * 3;
            double[] p = ReaperBladeMesh.opened(new double[] {mesh.pos[i], mesh.pos[i + 1], mesh.pos[i + 2]}, side, angle);
            double[] n = ReaperBladeMesh.opened(new double[] {mesh.normal[i], mesh.normal[i + 1] + ReaperBladeMesh.CASE_BOTTOM, mesh.normal[i + 2]}, side,
                angle);
            n[1] -= ReaperBladeMesh.CASE_BOTTOM;
            double[] q = apply(turn, p);
            double[] m = apply(turn, n);
            int argb = mesh.argb[v];
            out.m_252986_(at, (float)((fist[0] + q[0]) / 16.0), (float)((fist[1] + q[1]) / 16.0), (float)((fist[2] + q[2]) / 16.0))
                .m_6122_(argb >> 16 & 255, argb >> 8 & 255, argb & 255, 255).m_7421_(0.5F, 0.5F).m_86008_(OverlayTexture.f_118083_)
                .m_85969_(mesh.glow[v / 4] ? FULL_BRIGHT : light).m_252939_(normals, (float)m[0], (float)m[1], (float)m[2]).m_5752_();
        }
    }

    /**
     * 4.1.29: the empty case dropped from his hand ({@code after} ticks after the jacket): from where it left his fist (both halves open) it
     * falls, tipping over, lands at his feet and sinks away into the ground (the model's own frame, his body at the moment it fell).
     */
    static void fallen(PoseStack pose, VertexConsumer out, double after, RigPose now, int light) {
        double dt = after - AgentRules.REAPER_CASE_DROPS;
        double[][][] placed = new double[2][][];
        double[][][] turned = new double[2][][];
        double lowest = -Double.MAX_VALUE;
        for (int k = 0; k < 2; k++) {
            int side = k == 0 ? 1 : -1;
            ArmMesh.Mesh mesh = ReaperBladeMesh.goldHalf(side);
            placed[k] = new double[mesh.quads * 4][];
            turned[k] = new double[mesh.quads * 4][];
            for (int v = 0; v < mesh.quads * 4; v++) {
                int i = v * 3;
                placed[k][v] = ReaperBladePose.dropped(new double[] {mesh.pos[i], mesh.pos[i + 1], mesh.pos[i + 2]}, side, dt, false);
                turned[k][v] = ReaperBladePose.dropped(new double[] {mesh.normal[i], mesh.normal[i + 1], mesh.normal[i + 2]}, side, dt, true);
                lowest = Math.max(lowest, placed[k][v][1]);
            }
        }
        double ground = 24.0 - (now == null ? 0.0 : now.drop);
        double fall = 0.5 * ReaperBladePose.CASE_FALL * dt * dt;
        double landed = ground - lowest;
        double shift = Math.min(fall, landed) + (fall >= landed ? ReaperBladePose.sunk(dt, landed) : 0.0);
        Matrix4f at = pose.m_85850_().m_252922_();
        Matrix3f normals = pose.m_85850_().m_252943_();
        for (int k = 0; k < 2; k++) {
            ArmMesh.Mesh mesh = ReaperBladeMesh.goldHalf(k == 0 ? 1 : -1);
            for (int v = 0; v < mesh.quads * 4; v++) {
                double[] q = placed[k][v];
                double[] m = turned[k][v];
                int argb = mesh.argb[v];
                out.m_252986_(at, (float)(q[0] / 16.0), (float)((q[1] + shift) / 16.0), (float)(q[2] / 16.0))
                    .m_6122_(argb >> 16 & 255, argb >> 8 & 255, argb & 255, 255).m_7421_(0.5F, 0.5F).m_86008_(OverlayTexture.f_118083_)
                    .m_85969_(light).m_252939_(normals, (float)m[0], (float)m[1], (float)m[2]).m_5752_();
            }
        }
    }

    static double[] apply(double[][] m, double[] v) {
        return new double[] {m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2], m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]};
    }

    /** {@code mesh} (its own frame, pixels) in the arm's frame: turned by {@code m} out of {@code fist}, slid {@code k} of the way out of its case. */
    static void emit(PoseStack pose, VertexConsumer out, ArmMesh.Mesh mesh, double[][] m, double[] fist, float k, int light) {
        Matrix4f at = pose.m_85850_().m_252922_();
        Matrix3f normals = pose.m_85850_().m_252943_();
        for (int q = 0; q < mesh.quads; q++) {
            int lit = mesh.glow[q] ? FULL_BRIGHT : light;
            for (int c = 0; c < 4; c++) {
                int v = q * 4 + c;
                int i = v * 3;
                double px = k >= 1.0F ? mesh.pos[i] : ReaperBladeMesh.slid(mesh.pos[i], k);
                double py = mesh.pos[i + 1];
                double pz = mesh.pos[i + 2];
                double nx = mesh.normal[i];
                double ny = mesh.normal[i + 1];
                double nz = mesh.normal[i + 2];
                float x = (float)((fist[0] + m[0][0] * px + m[0][1] * py + m[0][2] * pz) / 16.0);
                float y = (float)((fist[1] + m[1][0] * px + m[1][1] * py + m[1][2] * pz) / 16.0);
                float z = (float)((fist[2] + m[2][0] * px + m[2][1] * py + m[2][2] * pz) / 16.0);
                int argb = mesh.argb[v];
                out.m_252986_(at, x, y, z).m_6122_(argb >> 16 & 255, argb >> 8 & 255, argb & 255, 255).m_7421_(0.5F, 0.5F)
                    .m_86008_(OverlayTexture.f_118083_).m_85969_(lit)
                    .m_252939_(normals, (float)(m[0][0] * nx + m[0][1] * ny + m[0][2] * nz), (float)(m[1][0] * nx + m[1][1] * ny + m[1][2] * nz),
                        (float)(m[2][0] * nx + m[2][1] * ny + m[2][2] * nz))
                    .m_5752_();
            }
        }
    }

    static double[][] transpose(double[][] m) {
        return new double[][] {{m[0][0], m[1][0], m[2][0]}, {m[0][1], m[1][1], m[2][1]}, {m[0][2], m[1][2], m[2][2]}};
    }
}
