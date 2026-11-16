package com.hex.kagune.control.client.render;

import com.hex.kagune.control.client.animation.WeaponPose;
import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.npc.ArmRules;
import com.hex.kagune.control.npc.CcgAgent;
import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.quinque.ArmMesh;
import com.hex.kagune.control.quinque.WhipPath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 3.0.0: an investigator's kagune-made weapon ({@link Arm}) in its right hand, from what its entity data says ({@code CcgAgent.arm}): its
 * mesh ({@link ArmMesh}: bone, flesh, sinew, crystal, glowing veins and eyes full bright) where the quinque it takes after is held
 * ({@link WeaponPose#meshToArm}), growing out of the hand as it tears out of its case and shrinking back as it goes in; the spear's javelin
 * gone as it is thrown and another growing back. The whip's tendril is drawn as a Rinkaku player's tail is ({@link RinkakuTube}), out of
 * its handle's knot along its path ({@link WhipPath}: hanging and trailing at rest, each lash, the Flay, the Bind round whoever it caught).
 */
final class WeaponLayer extends RenderLayer<CcgAgent, PlayerModel<CcgAgent>> {
    private static final RenderType SOLID = RenderType.m_110473_(Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/misc/white.png")));
    private static final int FULL_BRIGHT = 0xF000F0;
    /** The tendril's colour (a Rinkaku's crimson; a masterwork's darker). */
    private static final int TENDRIL = 0xA0101E;
    private static final int TENDRIL_MASTER = 0x6E0A14;
    /** How long a lash or a move blends out of the tendril's rest and back into it (ticks). */
    private static final double BLEND = 4.0;
    /** Its feet, in its model's frame (blocks down from its neck). */
    private static final double GROUND = 1.501;

    /** Each weapon's mesh as it lies on the right arm (blocks, its own frame): positions, then normals; built once. */
    private static final float[][][] PLACED = new float[Arm.values().length * 2][][];

    WeaponLayer(RenderLayerParent<CcgAgent, PlayerModel<CcgAgent>> parent) {
        super(parent);
    }

    @Override
    public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, CcgAgent agent, float limbSwing, float limbSwingAmount, float partial,
                        float ageInTicks, float netHeadYaw, float headPitch) {
        if (agent.m_20145_() || agent.m_9236_() == null
            || com.hex.kagune.control.client.BikakuClient.devoured(agent.m_19879_(), com.hex.kagune.control.client.PaleClient.clock(partial)) >= 0.0) {
            return; // 3.6.3: devoured (Grand Devouring): its weapon gone from view at once
        }
        Arm arm = agent.weapon();
        if (arm == null || !agent.m_21205_().m_41619_()) {
            return; // none, or its case in its hand
        }
        try {
            draw(pose, buffers, light, agent, arm, partial);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("an investigator's kagune weapon", failure);
        }
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, int light, CcgAgent agent, Arm arm, float partial) {
        long now = agent.m_9236_().m_46467_();
        int packed = agent.armPacked();
        float shown = ArmRules.shown(ArmRules.out(packed), CcgAgent.since(now, agent.armAt()) + partial);
        if (shown <= 0.0F) {
            return;
        }
        boolean master = ArmRules.masterwork(packed);
        AgentRules.Look look = agent.look();
        boolean slim = look.slim() || look.female();
        int swing = agent.swing();
        double swingAge = CcgAgent.since(now, agent.swingAt()) + partial;
        Arm.Ability ability = ArmRules.ability(swing);
        if (ability != null && (arm.slot(ability) < 0 || swingAge < 0.0 || swingAge >= ability.total)) {
            ability = null;
        }
        float grown = ability == null ? 1.0F : WeaponPose.grown(ability, swingAge);
        ModelPart hand = this.m_117386_().f_102811_;
        float k = shown * grown;
        if (k > 0.002F) {
            pose.m_85836_();
            if (slim) {
                pose.m_85837_(0.5 / 16.0, 0.0, 0.0); // PlayerModel.translateToHand: a slim arm's half pixel
            }
            hand.m_104299_(pose);
            mesh(pose, buffers.m_6299_(SOLID), arm, master, k, light, AgentModel.pose);
            pose.m_85849_();
        }
        if (arm == Arm.WHIP) {
            float size = AgentRules.size(look);
            whip(pose, buffers, light, agent, agent.armTarget(), size, size * AgentRules.build(look), hand, slim, master, shown, swing, swingAge,
                ability, partial, AgentModel.pose);
        }
    }

    /**
     * Its mesh, {@code k} of its size (grown out of the fist), in the right arm's frame; 3.4.8 turned in the fist as {@code posed} has it
     * (WeaponPose: both hands on a two-handed one's grip wherever its blade points), or as it is set in the fist with no pose.
     */
    static void mesh(PoseStack pose, VertexConsumer out, Arm arm, boolean master, float k, int light, com.hex.kagune.control.client.animation.RigPose posed) {
        ArmMesh.Mesh mesh = ArmMesh.of(arm, master);
        float[][] placed = placed(arm, master, mesh);
        float[] at = placed[0];
        float[] turned = placed[1];
        float[] fist = placed[2];
        boolean wrist = posed != null && (posed.weaponX != 0.0F || posed.weaponY != 0.0F || posed.weaponZ != 0.0F);
        if (wrist) {
            pose.m_85836_();
            pose.m_85837_(fist[0], fist[1], fist[2]);
            pose.m_252781_(new org.joml.Quaternionf().rotateAxis(posed.weaponZ, 0.0F, 0.0F, 1.0F).rotateAxis(posed.weaponY, 0.0F, 1.0F, 0.0F)
                .rotateAxis(posed.weaponX, 1.0F, 0.0F, 0.0F));
            pose.m_85837_(-fist[0], -fist[1], -fist[2]);
        }
        try {
            emit(pose, out, mesh, at, turned, fist, k, light);
        } finally {
            if (wrist) {
                pose.m_85849_();
            }
        }
    }

    private static void emit(PoseStack pose, VertexConsumer out, ArmMesh.Mesh mesh, float[] at, float[] turned, float[] fist, float k, int light) {
        Matrix4f m = pose.m_85850_().m_252922_();
        Matrix3f n = pose.m_85850_().m_252943_();
        for (int q = 0; q < mesh.quads; q++) {
            int lit = mesh.glow[q] ? FULL_BRIGHT : light;
            for (int c = 0; c < 4; c++) {
                int v = q * 4 + c;
                int i = v * 3;
                int argb = mesh.argb[v];
                out.m_252986_(m, fist[0] + (at[i] - fist[0]) * k, fist[1] + (at[i + 1] - fist[1]) * k, fist[2] + (at[i + 2] - fist[2]) * k)
                    .m_6122_(argb >> 16 & 255, argb >> 8 & 255, argb & 255, 255).m_7421_(0.5F, 0.5F).m_86008_(OverlayTexture.f_118083_)
                    .m_85969_(lit).m_252939_(n, turned[i], turned[i + 1], turned[i + 2]).m_5752_();
            }
        }
    }

    /** {@code arm}'s mesh on the right arm (built the first time): its points, its normals, where the fist is. */
    private static synchronized float[][] placed(Arm arm, boolean master, ArmMesh.Mesh mesh) {
        int key = arm.ordinal() * 2 + (master ? 1 : 0);
        if (PLACED[key] == null) {
            double[][] onArm = WeaponPose.meshToArm(arm);
            float[] at = new float[mesh.pos.length];
            float[] turned = new float[mesh.normal.length];
            for (int i = 0; i < mesh.pos.length; i += 3) {
                double[] p = WeaponPose.onArm(onArm, new double[] {mesh.pos[i], mesh.pos[i + 1], mesh.pos[i + 2]});
                double[] nn = WeaponPose.turnOnArm(onArm, new double[] {mesh.normal[i], mesh.normal[i + 1], mesh.normal[i + 2]});
                for (int j = 0; j < 3; j++) {
                    at[i + j] = (float)p[j];
                    turned[i + j] = (float)nn[j];
                }
            }
            PLACED[key] = new float[][] {at, turned, {(float)onArm[3][0], (float)onArm[3][1], (float)onArm[3][2]}};
        }
        return PLACED[key];
    }

    // ---------------------------------------------------------------- the whip's tendril

    /** 3.0.6: anyone's (an investigator's, a player's): {@code bound} whom its Bind holds (an entity id), {@code size} and {@code wide} its body's. */
    static void whip(PoseStack pose, MultiBufferSource buffers, int light, net.minecraft.world.entity.LivingEntity agent, int bound, float size,
                     float wide, ModelPart hand, boolean slim, boolean master, float shown, int swing, double swingAge, Arm.Ability ability,
                     float partial, com.hex.kagune.control.client.animation.RigPose posed) {
        double[] knot = WeaponPose.onBody(hand.f_104200_, hand.f_104201_, hand.f_104202_, hand.f_104203_, hand.f_104204_, hand.f_104205_, slim,
            WeaponPose.wristed(posed, Arm.WHIP, WeaponPose.onArm(WeaponPose.meshToArm(Arm.WHIP), new double[] {0.0, 0.0, -ArmMesh.WHIP_ROOT})));
        double length = WhipPath.length(master);
        double time = agent.f_19797_ + partial;
        double drop = Math.max(0.2, GROUND - knot[1]);
        double[][] rest = WhipPath.rest(length, drop, time);
        double[][] path = rest;
        int step = ArmRules.step(swing);
        if (step >= 0) {
            Arm.Strike strike = Arm.WHIP.strike(step);
            double total = strike.total();
            if (swingAge >= 0.0 && swingAge < total + BLEND) {
                double land = strike.land() / total;
                double[][] lash = WhipPath.lash(step, Math.min(1.0, swingAge / total), land, length, time);
                path = blend(rest, lash, weight(swingAge, total));
            }
        } else if (ability == Arm.Ability.FLAY) {
            path = blend(rest, WhipPath.flay(swingAge, length, time), moving(swingAge, ability.total));
        } else if (ability == Arm.Ability.BIND) {
            double[] target = {5.0, 0.0, -0.2};
            double girth = 0.35;
            Entity held = bound != 0 ? agent.m_9236_().m_6815_(bound) : null;
            double yaw = agent.f_20884_ + wrap(agent.f_20883_ - agent.f_20884_) * partial;
            if (held != null) {
                double x = held.f_19790_ + (held.m_20185_() - held.f_19790_) * partial - (agent.f_19790_ + (agent.m_20185_() - agent.f_19790_) * partial);
                double y = held.f_19791_ + (held.m_20186_() - held.f_19791_) * partial + held.m_20206_() * 0.5
                    - (agent.f_19791_ + (agent.m_20186_() - agent.f_19791_) * partial);
                double z = held.f_19792_ + (held.m_20189_() - held.f_19792_) * partial - (agent.f_19792_ + (agent.m_20189_() - agent.f_19792_) * partial);
                target = WeaponPose.toBody(knot, WeaponPose.toModel(new double[] {x, y, z}, yaw, wide, size));
                girth = Math.max(0.2, held.m_20205_() * 0.5 / wide);
            }
            // Let go at its end, it falls back to its rest (not drawn back into the hand and out again).
            path = blend(rest, WhipPath.bind(Math.min(swingAge, ability.total - BLEND - 0.01), target, girth, length, time),
                moving(swingAge, ability.total));
        } else if (ability == Arm.Ability.SWING) {
            // 3.1.8: stretched to where it bit.
            double[] bit = com.hex.kagune.control.client.WeaponClient.anchor(agent.m_19879_());
            if (bit != null) {
                double yaw = agent.f_20884_ + wrap(agent.f_20883_ - agent.f_20884_) * partial;
                double x = bit[0] - (agent.f_19790_ + (agent.m_20185_() - agent.f_19790_) * partial);
                double y = bit[1] - (agent.f_19791_ + (agent.m_20186_() - agent.f_19791_) * partial);
                double z = bit[2] - (agent.f_19792_ + (agent.m_20189_() - agent.f_19792_) * partial);
                double[] target = WeaponPose.toBody(knot, WeaponPose.toModel(new double[] {x, y, z}, yaw, wide, size));
                path = blend(rest, WhipPath.swing(Math.min(swingAge, ability.total - BLEND - 0.01), target, length, time),
                    moving(swingAge, ability.total));
            }
        }
        Vec3[] points = new Vec3[path.length];
        for (int i = 0; i < path.length; i++) {
            double[] at = WeaponPose.fromBody(knot, new double[] {path[i][0] * shown, path[i][1] * shown, path[i][2] * shown});
            // Never into the ground under its feet: what would go below lies along it.
            points[i] = new Vec3(at[0], Math.min(at[1], GROUND - 0.03), at[2]);
        }
        double[] eye = eye(agent, size, wide, partial);
        RinkakuTube.emit(buffers.m_6299_(SOLID), pose.m_85850_().m_252922_(), pose.m_85850_().m_252943_(), points, WhipPath.thickness(master),
            master ? TENDRIL_MASTER : TENDRIL, light, agent.m_19879_() * 31 + 7, eye, time);
    }

    /** How much of a move (not its rest) the tendril is, {@code age} ticks into one {@code total} long: in, held, out by its end. */
    static double moving(double age, double total) {
        return weight(age, total - BLEND);
    }

    /** How much of a lash (not its rest) the tendril is, {@code age} ticks into one {@code total} long: in, held, out after it. */
    static double weight(double age, double total) {
        if (age < 0.0 || age >= total + BLEND) {
            return 0.0;
        }
        double in = Math.min(1.0, age / (BLEND * 0.5));
        double out = age <= total ? 1.0 : 1.0 - (age - total) / BLEND;
        double w = Math.min(in, out);
        return w * w * (3.0 - 2.0 * w);
    }

    private static double[][] blend(double[][] a, double[][] b, double k) {
        double[][] out = new double[a.length][];
        for (int i = 0; i < a.length; i++) {
            double[] p = a[i];
            double[] q = b[Math.min(i, b.length - 1)];
            out[i] = new double[] {p[0] + (q[0] - p[0]) * k, p[1] + (q[1] - p[1]) * k, p[2] + (q[2] - p[2]) * k};
        }
        return out;
    }

    /** The camera in the investigator's model frame (its tendril's gloss is seen from there). */
    private static double[] eye(net.minecraft.world.entity.LivingEntity agent, float size, float wide, float partial) {
        Vec3 camera = Minecraft.m_91087_().f_91063_.m_109153_().m_90583_();
        double x = camera.f_82479_ - (agent.f_19790_ + (agent.m_20185_() - agent.f_19790_) * partial);
        double y = camera.f_82480_ - (agent.f_19791_ + (agent.m_20186_() - agent.f_19791_) * partial);
        double z = camera.f_82481_ - (agent.f_19792_ + (agent.m_20189_() - agent.f_19792_) * partial);
        double yaw = agent.f_20884_ + wrap(agent.f_20883_ - agent.f_20884_) * partial;
        return WeaponPose.toModel(new double[] {x, y, z}, yaw, wide, size);
    }

    private static double wrap(double degrees) {
        return ((degrees % 360.0) + 540.0) % 360.0 - 180.0;
    }
}
