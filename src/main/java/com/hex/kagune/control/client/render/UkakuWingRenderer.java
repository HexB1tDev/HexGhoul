package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.client.UkakuClient;
import com.hex.kagune.control.client.ControlClient;
import com.hex.kagune.control.client.fx.UkakuFx;
import com.hex.kagune.control.client.fx.UkakuSpikes;
import com.hex.kagune.control.client.fx.UkakuThorns;
import com.hex.kagune.control.client.fx.KakujaFx;
import com.hex.kagune.control.ukaku.CrystalSpike;
import com.hex.kagune.control.ukaku.CrystalThorns;
import com.hex.kagune.control.ukaku.KakujaNeedle;
import com.hex.kagune.control.ukaku.RcShard;
import com.hex.kagune.control.ukaku.WingRig;
import com.hex.kagune.control.ukaku.WingVortex;
import com.hex.kagune.control.network.S2CUkakuSpikes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws Ukaku players' Ukaku wings (and the :re One-Winged Kagune) in the world, after entities, with the same render
 * type and vertex layout as the original tendril renderer (a white texture tinted per vertex, translucent, unculled).
 * All shape and pose work happens in the tested {@link WingRig}; this class only places it on each player. It also draws
 * the shards of Ukaku's fire and his crystal spikes in flight ({@link UkakuFx}, {@link RcShard}, {@link CrystalSpike}), the spikes
 * stuck in bodies and planted in blocks ({@link UkakuSpikes}), and the blood they leave on the ground ({@link BloodStains}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class UkakuWingRenderer {
    /** A pure white texel, so the vertex colours show exactly (white concrete would grey every colour by about 18%). */
    private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    /** RC glows: full block and sky light. */
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final double MAX_DISTANCE_SQR = 96.0 * 96.0;
    private static final double DETAIL_DISTANCE_SQR = 28.0 * 28.0;
    /** PlayerAnimator turns the body about this height (blocks above the feet). */
    private static final double FLIP_PIVOT = 0.7;

    /** This frame's partial tick, for what is drawn later in the frame without one of its own (the first-person Steel Blade). */
    static float partial;

    private UkakuWingRenderer() {
    }

    /**
     * The wing quads (rig space: x right, y up from the feet, z forward) placed on a body at {@code x, y, z} (camera-relative feet)
     * turned {@code yaw} (radians, the body's), the whole turned {@code flip} about the hips (the Black Rabbit's backflip).
     */
    private static void emit(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals, List<WingRig.Quad> quads, double x, double y, double z, double yaw,
                             float flip) {
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double flipCos = Math.cos(-flip);
        double flipSin = Math.sin(-flip);
        for (WingRig.Quad quad : quads) {
            float[] xyz = quad.xyz();
            int[] argb = quad.argb();
            double qy = quad.ny();
            double qz = quad.nz();
            if (flip != 0.0F) {
                double ny = qy * flipCos - qz * flipSin;
                qz = qy * flipSin + qz * flipCos;
                qy = ny;
            }
            float nx = (float)(rightX * quad.nx() + forwardX * qz);
            float nz = (float)(rightZ * quad.nx() + forwardZ * qz);
            for (int corner = 0; corner < 4; corner++) {
                double lx = xyz[corner * 3];
                double ly = xyz[corner * 3 + 1];
                double lz = xyz[corner * 3 + 2];
                if (flip != 0.0F) {
                    double py = ly - FLIP_PIVOT;
                    ly = FLIP_PIVOT + py * flipCos - lz * flipSin;
                    lz = py * flipSin + lz * flipCos;
                }
                int colour = argb[corner];
                consumer.m_252986_(matrix, (float)(x + rightX * lx + forwardX * lz), (float)(y + ly), (float)(z + rightZ * lx + forwardZ * lz))
                    .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, colour >>> 24)
                    .m_7421_(0.5F, 0.5F)
                    .m_86008_(OverlayTexture.f_118083_)
                    .m_85969_(FULL_BRIGHT)
                    .m_252939_(normals, nx, (float)qy, nz)
                    .m_5752_();
            }
        }
    }

    /**
     * 1.0.24: as above, each rig-space corner (and normal) first carried by {@code carry} ({@link
     * com.hex.kagune.control.client.animation.BodyFrame#carry}: the posed torso's rigid transform).
     */
    private static void emit(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals, List<WingRig.Quad> quads, double x, double y, double z, double yaw,
                             double[] carry) {
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double[] a = carry;
        for (WingRig.Quad quad : quads) {
            float[] xyz = quad.xyz();
            int[] argb = quad.argb();
            double qx = a[0] * quad.nx() + a[1] * quad.ny() + a[2] * quad.nz();
            double qy = a[3] * quad.nx() + a[4] * quad.ny() + a[5] * quad.nz();
            double qz = a[6] * quad.nx() + a[7] * quad.ny() + a[8] * quad.nz();
            float nx = (float)(rightX * qx + forwardX * qz);
            float nz = (float)(rightZ * qx + forwardZ * qz);
            for (int corner = 0; corner < 4; corner++) {
                double px = xyz[corner * 3];
                double py = xyz[corner * 3 + 1];
                double pz = xyz[corner * 3 + 2];
                double lx = a[0] * px + a[1] * py + a[2] * pz + a[9];
                double ly = a[3] * px + a[4] * py + a[5] * pz + a[10];
                double lz = a[6] * px + a[7] * py + a[8] * pz + a[11];
                int colour = argb[corner];
                consumer.m_252986_(matrix, (float)(x + rightX * lx + forwardX * lz), (float)(y + ly), (float)(z + rightZ * lx + forwardZ * lz))
                    .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, colour >>> 24)
                    .m_7421_(0.5F, 0.5F)
                    .m_86008_(OverlayTexture.f_118083_)
                    .m_85969_(FULL_BRIGHT)
                    .m_252939_(normals, nx, (float)qy, nz)
                    .m_5752_();
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        UkakuWingRenderer.partial = event.getPartialTick();
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        LocalPlayer self = minecraft.f_91074_;
        if (level == null || self == null) {
            return;
        }
        float partial = event.getPartialTick();
        double time = ControlClient.worldTime(partial);
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        PoseStack pose = event.getPoseStack();
        Matrix4f matrix = pose.m_85850_().m_252922_();
        Matrix3f normals = pose.m_85850_().m_252943_();
        MultiBufferSource.BufferSource buffers = minecraft.m_91269_().m_110104_();
        // Blood on the ground first, so translucent wings and spikes over it still show it.
        BloodStains.render(buffers, matrix, normals, camera, partial, level.m_46467_());
        VertexConsumer consumer = null;
        for (AbstractClientPlayer player : level.m_6907_()) {
            if (!UkakuClient.isUkaku(player)) {
                continue;
            }
            // 5.0.5: his kakuja bursts out and shatters like Rinkaku's (KakujaFx); seen here first so no frame is missed.
            KakujaFx.observeUkaku(player);
            KaguneClientState.View view = KaguneClientState.get(player);
            if (!view.active() || !player.m_6084_() || player.m_20145_() || player.m_5803_() || player.m_6069_() || player.m_21255_()) {
                continue;
            }
            double distance = player.m_20280_(self);
            if (distance > MAX_DISTANCE_SQR) {
                continue;
            }
            boolean firstPerson = player == self && minecraft.f_91066_.m_92176_().m_90612_();
            float detail = firstPerson ? 0.3F : distance > DETAIL_DISTANCE_SQR ? 0.3F : 1.0F;
            double x = player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial - camera.f_82479_;
            double y = player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial - camera.f_82480_;
            double z = player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial - camera.f_82481_;
            float previousYaw = player.f_20884_;
            float yawDelta = ((player.f_20883_ - previousYaw) % 360.0F + 540.0F) % 360.0F - 180.0F;
            double yaw = Math.toRadians(previousYaw + yawDelta * partial);
            double forwardX = -Math.sin(yaw);
            double forwardZ = Math.cos(yaw);
            double rightX = -Math.cos(yaw);
            double rightZ = -Math.sin(yaw);
            // The camera in the rig's own space (x right, y up from the feet, z forward), for the gas's view shading.
            double[] eye = {-(rightX * x + rightZ * z), -y, -(forwardX * x + forwardZ * z)};
            WingRig.Input input = UkakuClient.rigInput(player, view, ControlClient.get(player), time, firstPerson, detail);
            List<WingRig.Quad> quads;
            if (input.form().kakuja()) {
                quads = UkakuClient.kakujaQuads(player, input, eye, partial, time, x + camera.f_82479_, y + camera.f_82480_, z + camera.f_82481_, yaw);
                KakujaFx.ukakuDrawn(player, quads, x + camera.f_82479_, y + camera.f_82480_, z + camera.f_82481_, yaw, level.m_46467_());
                quads = KakujaFx.ukakuBurst(player, quads, time);
            } else {
                quads = WingRig.build(input, eye);
                // 1.2.8: kept too, so the wings break with his body in the death that does not take (KakujaFx.breakApart).
                KakujaFx.ukakuDrawn(player, quads, x + camera.f_82479_, y + camera.f_82480_, z + camera.f_82481_, yaw, level.m_46467_());
            }
            if (quads.isEmpty()) {
                continue;
            }
            // The Black Rabbit's backflip turns the whole body about the hips (PlayerAnimator's body rotation): the wings too.
            float flip = UkakuClient.flip(player, view, time);
            // 1.0.24: the wings ride his torso as the body layer drew it (lean, lunge, drop, flip, roll), not only its flip; the
            // kakuja's tentacles lean with it on their own (UkakuClient.kakujaBody).
            double[] carry = null;
            if (!input.form().kakuja()) {
                com.hex.kagune.control.client.animation.RigPose drawn = com.hex.kagune.control.client.animation.KaguneBodyAnimation.body(player,
                    ControlClient.worldTime(partial));
                carry = drawn == null ? null : com.hex.kagune.control.client.animation.BodyFrame.carry(drawn, "torso");
            }
            if (consumer == null) {
                consumer = buffers.m_6299_(TYPE);
            }
            if (carry != null) {
                emit(consumer, matrix, normals, quads, x, y, z, yaw, carry);
            } else {
                emit(consumer, matrix, normals, quads, x, y, z, yaw, flip);
            }
        }
        // 7.0.2: the ukaku ghoul NPCs in view: the twin wings, as Ukaku's, aiming as they shoot.
        double clock = com.hex.kagune.control.client.PaleClient.clock(partial);
        for (com.hex.kagune.control.npc.GhoulNpc npc : GhoulNpcRenderer.drawn()) {
            com.hex.kagune.control.client.GhoulNpcClient.View seen = com.hex.kagune.control.client.GhoulNpcClient.get(npc.m_19879_());
            if (seen == null || seen.identity().kind() != com.hex.kagune.control.npc.NpcIdentity.Kind.UKAKU || !npc.m_6084_() || npc.m_20145_()) {
                continue;
            }
            float deploy = com.hex.kagune.control.client.GhoulNpcClient.deploy(seen, clock);
            double distance = npc.m_20280_(self);
            if (deploy <= 0.005F || distance > MAX_DISTANCE_SQR) {
                continue;
            }
            double x = npc.f_19790_ + (npc.m_20185_() - npc.f_19790_) * partial - camera.f_82479_;
            double y = npc.f_19791_ + (npc.m_20186_() - npc.f_19791_) * partial - camera.f_82480_;
            double z = npc.f_19792_ + (npc.m_20189_() - npc.f_19792_) * partial - camera.f_82481_;
            float yawDelta = ((npc.f_20883_ - npc.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
            double yaw = Math.toRadians(npc.f_20884_ + yawDelta * partial);
            double forwardX = -Math.sin(yaw);
            double forwardZ = Math.cos(yaw);
            double rightX = -Math.cos(yaw);
            double rightZ = -Math.sin(yaw);
            double[] eye = {-(rightX * x + rightZ * z), -y, -(forwardX * x + forwardZ * z)};
            // The wings aim at what it shoots at, from the shoulder blades, in the rig's own space.
            double[] aim = WingRig.FORWARD;
            net.minecraft.world.entity.Entity prey = seen.prey() == 0 ? null : level.m_6815_(seen.prey());
            if (prey != null) {
                double px = prey.f_19790_ + (prey.m_20185_() - prey.f_19790_) * partial - camera.f_82479_ - x;
                double py = prey.f_19791_ + (prey.m_20186_() - prey.f_19791_) * partial + prey.m_20206_() * 0.52 - camera.f_82480_ - y - WingRig.ROOT_Y;
                double pz = prey.f_19792_ + (prey.m_20189_() - prey.f_19792_) * partial - camera.f_82481_ - z;
                double[] rig = {px * rightX + pz * rightZ, py, px * forwardX + pz * forwardZ};
                double length = Math.sqrt(rig[0] * rig[0] + rig[1] * rig[1] + rig[2] * rig[2]);
                if (length > 1.0E-3) {
                    aim = new double[] {rig[0] / length, rig[1] / length, rig[2] / length};
                }
            }
            float aimed = com.hex.kagune.control.client.GhoulNpcClient.aim(seen, clock);
            WingRig.Input input = new WingRig.Input(com.hex.kagune.control.ukaku.WingForm.TWIN, seen.identity().lookMastery(), deploy, time / 20.0, 0.0F,
                0.0F, WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false, distance > DETAIL_DISTANCE_SQR ? 0.3F : 1.0F, 1.0F, !seen.out(), aimed, aimed, aim,
                com.hex.kagune.control.client.GhoulNpcClient.kick(seen, 1, clock), com.hex.kagune.control.client.GhoulNpcClient.kick(seen, -1, clock));
            List<WingRig.Quad> quads = WingRig.build(input, eye);
            if (quads.isEmpty()) {
                continue;
            }
            if (consumer == null) {
                consumer = buffers.m_6299_(TYPE);
            }
            // 1.0.41: its wings turned over with it as it flips.
            com.hex.kagune.control.client.animation.RigPose flipping = com.hex.kagune.control.client.GhoulNpcClient.flip(seen, clock, npc.m_20096_());
            emit(consumer, matrix, normals, quads, x, y, z, yaw, flipping == null ? 0.0F : flipping.flip);
        }
        // 1.3.19: a companion's wings (Touka's): a lesser pair at her size, riding her torso as it was drawn, crystal as she throws.
        for (int id : com.hex.kagune.control.client.NpcWingsClient.ids()) {
            com.hex.kagune.control.client.NpcWingsClient.View wings = com.hex.kagune.control.client.NpcWingsClient.get(id);
            if (!(level.m_6815_(id) instanceof com.hex.kagune.control.npc.EtoNpc npc) || !npc.m_6084_() || npc.m_20145_()) {
                continue;
            }
            float deploy = com.hex.kagune.control.client.NpcWingsClient.deploy(wings, clock);
            double distance = npc.m_20280_(self);
            if (deploy <= 0.005F || distance > MAX_DISTANCE_SQR) {
                continue;
            }
            double size = npc.persona().size();
            double x = npc.f_19790_ + (npc.m_20185_() - npc.f_19790_) * partial - camera.f_82479_;
            double y = npc.f_19791_ + (npc.m_20186_() - npc.f_19791_) * partial - camera.f_82480_;
            double z = npc.f_19792_ + (npc.m_20189_() - npc.f_19792_) * partial - camera.f_82481_;
            float yawDelta = ((npc.f_20883_ - npc.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
            double yaw = Math.toRadians(npc.f_20884_ + yawDelta * partial);
            double forwardX = -Math.sin(yaw);
            double forwardZ = Math.cos(yaw);
            double rightX = -Math.cos(yaw);
            double rightZ = -Math.sin(yaw);
            double[] eye = {-(rightX * x + rightZ * z) / size, -y / size, -(forwardX * x + forwardZ * z) / size};
            double[] aim = WingRig.FORWARD;
            net.minecraft.world.entity.Entity prey = wings.prey() == 0 ? null : level.m_6815_(wings.prey());
            if (prey != null) {
                double px = prey.f_19790_ + (prey.m_20185_() - prey.f_19790_) * partial - camera.f_82479_ - x;
                double py = prey.f_19791_ + (prey.m_20186_() - prey.f_19791_) * partial + prey.m_20206_() * 0.52 - camera.f_82480_ - y - WingRig.ROOT_Y * size;
                double pz = prey.f_19792_ + (prey.m_20189_() - prey.f_19792_) * partial - camera.f_82481_ - z;
                double[] rig = {px * rightX + pz * rightZ, py, px * forwardX + pz * forwardZ};
                double length = Math.sqrt(rig[0] * rig[0] + rig[1] * rig[1] + rig[2] * rig[2]);
                if (length > 1.0E-3) {
                    aim = new double[] {rig[0] / length, rig[1] / length, rig[2] / length};
                }
            }
            float aimed = com.hex.kagune.control.client.NpcWingsClient.aim(wings, clock);
            float crystal = com.hex.kagune.control.client.NpcWingsClient.crystal(wings, clock);
            WingRig.Input input = new WingRig.Input(com.hex.kagune.control.ukaku.WingForm.TWIN, com.hex.kagune.control.npc.WingRules.MASTERY, deploy,
                time / 20.0, crystal, crystal, WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false, distance > DETAIL_DISTANCE_SQR ? 0.3F : 1.0F, 1.0F,
                !wings.out(), aimed, aimed, aim, com.hex.kagune.control.client.NpcWingsClient.kick(wings, 1, clock),
                com.hex.kagune.control.client.NpcWingsClient.kick(wings, -1, clock), 0.0F, 0.0F, 0.0F, 0.0F);
            List<WingRig.Quad> quads = WingRig.build(input, eye);
            if (quads.isEmpty()) {
                continue;
            }
            if (consumer == null) {
                consumer = buffers.m_6299_(TYPE);
            }
            // Her torso as drawn this frame (her lean, her flips), and her size: the wings ride both.
            com.hex.kagune.control.client.animation.RigPose drawn = EtoRenderer.lastPose(id);
            double[] carry = drawn == null ? new double[] {1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0}
                : com.hex.kagune.control.client.animation.BodyFrame.carry(drawn, "torso");
            double[] scaled = new double[12];
            for (int k = 0; k < 12; k++) {
                scaled[k] = carry[k] * size;
            }
            emit(consumer, matrix, normals, quads, x, y, z, yaw, scaled);
        }
        // Shards of Ukaku's fire and crystal spikes in flight (camera-relative, facing the camera).
        double[] eye = {0.0, 0.0, 0.0};
        for (UkakuFx.Bolt bolt : UkakuFx.bolts()) {
            Vec3 head = bolt.at(time);
            double hx = head.f_82479_ - camera.f_82479_;
            double hy = head.f_82480_ - camera.f_82480_;
            double hz = head.f_82481_ - camera.f_82481_;
            double away = hx * hx + hy * hy + hz * hz;
            if (away > MAX_DISTANCE_SQR) {
                continue;
            }
            if (consumer == null) {
                consumer = buffers.m_6299_(TYPE);
            }
            RcShard.Sink sink = glowing(consumer, matrix, normals);
            double age = time - bolt.born();
            float fade = (float)Math.max(0.0, Math.min(1.0, Math.min((age + 1.0) / 1.5, bolt.life() + 1.0 - age)));
            double[] heading = {bolt.velocity().f_82479_, bolt.velocity().f_82480_, bolt.velocity().f_82481_};
            if (bolt.crystal() && bolt.kind() == UkakuFx.NEEDLE_KIND) {
                KakujaNeedle.build(sink, new double[] {hx, hy, hz}, heading, bolt.size(), eye, 1.0, 0.0, time / 20.0, bolt.seed(), away < DETAIL_DISTANCE_SQR,
                    fade);
            } else if (bolt.crystal()) {
                CrystalSpike.build(sink, new double[] {hx, hy, hz}, heading, bolt.size(), eye, 1.0, 0.0, time / 20.0, bolt.seed(),
                    away < DETAIL_DISTANCE_SQR, fade);
            } else {
                RcShard.build(sink, new double[] {hx, hy, hz}, heading, bolt.size(), time / 20.0, bolt.seed(), eye, fade);
            }
        }
        // Wing Vortices: the wing shooting forward as a spinning vortex from its shoulder blade, detaching where it ends.
        for (UkakuFx.Vortex vortex : UkakuFx.vortices()) {
            Vec3 head = vortex.at(time).m_82546_(camera);
            Vec3 root = vortex.root(time, partial).m_82546_(camera);
            double away = Math.min(head.m_82556_(), root.m_82556_());
            if (away > MAX_DISTANCE_SQR) {
                continue;
            }
            if (vortex.shooter() == self.m_19879_() && minecraft.f_91066_.m_92176_().m_90612_() && vortex.unwind(time) <= 0.0F) {
                // In first person, start it a little out from your own shoulder, so it doesn't fill the view.
                Vec3 along = head.m_82546_(root);
                double length = along.m_82553_();
                root = length < 1.0E-6 ? root : root.m_82549_(along.m_82490_(Math.min(length * 0.8, 1.6) / length));
            }
            if (consumer == null) {
                consumer = buffers.m_6299_(TYPE);
            }
            float unwind = vortex.unwind(time);
            double age = time - vortex.born();
            float fade = (float)Math.max(0.0, Math.min(1.0, Math.min((age + 1.0) / 2.0, 1.0 - unwind)));
            // The thrown wing keeps its colour: the √A and :re starry night past the middle.
            double dusk = 0.0;
            if (level.m_6815_(vortex.shooter()) instanceof AbstractClientPlayer shooter) {
                dusk = Math.max(0.0, Math.min(1.0, (KaguneClientState.get(shooter).mastery() - 250) / 200.0));
                dusk = dusk * dusk * (3.0 - 2.0 * dusk);
            }
            WingVortex.build(glowing(consumer, matrix, normals), new double[] {root.f_82479_, root.f_82480_, root.f_82481_},
                new double[] {head.f_82479_, head.f_82480_, head.f_82481_}, vortex.size(), time / 20.0, vortex.seed(), eye, fade, unwind, dusk,
                away < DETAIL_DISTANCE_SQR);
        }
        // Crystal spikes stuck in bodies: on the body as it moves and turns, the point inside it, glowing hotter as the burst comes.
        for (Map.Entry<Integer, UkakuSpikes.Body> entry : UkakuSpikes.bodies().entrySet()) {
            if (entry.getValue().spikes().isEmpty() || !(level.m_6815_(entry.getKey()) instanceof LivingEntity body) || !body.m_6084_()) {
                continue;
            }
            if (body == self && minecraft.f_91066_.m_92176_().m_90612_()) {
                continue; // your own body isn't drawn in first person, nor what is stuck in it
            }
            for (S2CUkakuSpikes.Spike spike : entry.getValue().spikes()) {
                Vec3[] placed = UkakuSpikes.place(body, spike, partial);
                // Stuck spikes show a little bigger than they fly, so they read on a body; a lance shows shorter than it flew
                // (most of it went through).
                boolean needle = spike.kind() == UkakuFx.NEEDLE_KIND;
                double size = (spike.kind() == 1 ? 1.6 : spike.kind() == 4 ? 0.85 : 1.0) * STUCK_SCALE;
                double length = needle ? KakujaNeedle.LENGTH : CrystalSpike.LENGTH;
                double since = time - spike.stuckAt();
                Vec3 dir = quiver(placed[1], since, spike.id());
                // Shoved in: it arrives a little out, slams in past its rest depth and settles back (only its point is inside).
                double depth = STUCK_DEPTH + shove(since);
                Vec3 tip = placed[0].m_82549_(dir.m_82490_(length * size * depth)).m_82546_(camera);
                double away = tip.m_82556_();
                if (away > MAX_DISTANCE_SQR) {
                    continue;
                }
                if (consumer == null) {
                    consumer = buffers.m_6299_(TYPE);
                }
                double heat = Math.max(0.0, Math.min(1.0, (time - (spike.burstAt() - 18.0)) / 18.0));
                double settle = Math.max(0.0, Math.min(1.0, (time - spike.stuckAt()) / 3.0));
                double[] at = {tip.f_82479_, tip.f_82480_, tip.f_82481_};
                double[] pointing = {dir.f_82479_, dir.f_82480_, dir.f_82481_};
                if (needle) {
                    KakujaNeedle.build(glowing(consumer, matrix, normals), at, pointing, size, eye, 0.0, heat * heat, time / 20.0, spike.id() & 0xFFFF,
                        away < DETAIL_DISTANCE_SQR, 1.0);
                } else {
                    CrystalSpike.build(glowing(consumer, matrix, normals), at, pointing, size, eye, 0.3 * (1.0 - settle), heat * heat, time / 20.0,
                        spike.id() & 0xFFFF, away < DETAIL_DISTANCE_SQR, 1.0);
                }
            }
        }
        // 5.0.15: crystal thorns out of an Ukaku held by a Rinkaku's tails, riding his body (only those clear of your eye on your own).
        for (Map.Entry<Integer, UkakuThorns.Eruption> entry : UkakuThorns.eruptions().entrySet()) {
            if (!(level.m_6815_(entry.getKey()) instanceof LivingEntity body) || !body.m_6084_()) {
                continue;
            }
            boolean own = body == self && minecraft.f_91066_.m_92176_().m_90612_();
            UkakuThorns.Eruption eruption = entry.getValue();
            double age = time - eruption.eruptedAt();
            double sunk = Double.isNaN(eruption.sunkAt()) ? Double.NaN : time - eruption.sunkAt();
            double scale = body.m_20206_() / CrystalThorns.HEIGHT;
            // A flash of red as they break out, settling to a faint glow.
            double glow = 0.15 + 0.6 * Math.max(0.0, 1.0 - age / 10.0);
            for (CrystalThorns.Thorn thorn : eruption.thorns()) {
                if (own && !CrystalThorns.clearOfEye(thorn)) {
                    continue;
                }
                double out = CrystalThorns.out(thorn, age, sunk);
                if (out <= 0.001) {
                    continue;
                }
                Vec3 tip = UkakuThorns.world(body, CrystalThorns.tip(thorn, out), partial, false).m_82546_(camera);
                double away = tip.m_82556_();
                if (away > MAX_DISTANCE_SQR) {
                    continue;
                }
                if (consumer == null) {
                    consumer = buffers.m_6299_(TYPE);
                }
                Vec3 dir = UkakuThorns.world(body, thorn.direction(), partial, true);
                CrystalSpike.build(glowing(consumer, matrix, normals), new double[] {tip.f_82479_, tip.f_82480_, tip.f_82481_},
                    new double[] {dir.f_82479_, dir.f_82480_, dir.f_82481_}, CrystalThorns.drawnSize(thorn, out) * scale, eye, glow, 0.0, time / 20.0,
                    thorn.seed(), away < DETAIL_DISTANCE_SQR, 1.0);
            }
        }
        // Spikes that hit a block, planted in it for a moment.
        for (UkakuSpikes.Planted planted : UkakuSpikes.planted()) {
            Vec3 tip = planted.tip().m_82546_(camera);
            double away = tip.m_82556_();
            if (away > MAX_DISTANCE_SQR) {
                continue;
            }
            if (consumer == null) {
                consumer = buffers.m_6299_(TYPE);
            }
            double left = Math.max(0.0, Math.min(1.0, (planted.until() - time) / 8.0));
            double[] at = {tip.f_82479_, tip.f_82480_, tip.f_82481_};
            double[] pointing = {planted.direction().f_82479_, planted.direction().f_82480_, planted.direction().f_82481_};
            if (planted.kind() == UkakuFx.NEEDLE_KIND) {
                KakujaNeedle.build(glowing(consumer, matrix, normals), at, pointing, planted.size(), eye, 0.0, 0.0, time / 20.0, planted.seed(),
                    away < DETAIL_DISTANCE_SQR, left);
            } else {
                CrystalSpike.build(glowing(consumer, matrix, normals), at, pointing, planted.size(), eye, 0.0, 0.0, time / 20.0, planted.seed(),
                    away < DETAIL_DISTANCE_SQR, left);
            }
        }
        if (consumer != null) {
            buffers.m_109912_(TYPE);
        }
    }

    /**
     * Stuck spikes are drawn this much bigger than in flight, with this share of their length inside the body at rest (5.0.3:
     * nearly half, they were hanging off the body at a fifth).
     */
    static final double STUCK_SCALE = 1.25;
    static final double STUCK_DEPTH = 0.45;

    /**
     * The extra depth (share of the spike's length) of a spike {@code since} ticks after it stuck: it arrives a quarter out,
     * slams in a little past its rest depth by the second tick, and settles back over a few more.
     */
    static double shove(double since) {
        if (since < 0.0) {
            return -0.25;
        }
        if (since < 2.0) {
            double t = since / 2.0;
            return -0.25 + 0.33 * t * t;
        }
        return 0.08 * Math.exp(-(since - 2.0) * 0.6) * Math.cos((since - 2.0) * 1.4);
    }

    /** A freshly stuck spike quivers from the blow, the shake dying away over about half a second. */
    static Vec3 quiver(Vec3 dir, double since, int seed) {
        if (since < 0.0 || since > 14.0) {
            return dir;
        }
        double amount = 0.16 * Math.exp(-since * 0.32) * Math.sin(since * 2.3 + (seed & 7));
        Vec3 up = Math.abs(dir.f_82480_) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 side = dir.m_82537_(up).m_82541_();
        return dir.m_82549_(side.m_82490_(amount)).m_82541_();
    }

    /** A sink that writes glowing quads (full bright, lit from above) in camera-relative blocks. */
    private static RcShard.Sink glowing(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals) {
        return (a, b, c, d, ca, cb, cc, cd) -> {
            double[][] corners = {a, b, c, d};
            int[] colours = {ca, cb, cc, cd};
            for (int corner = 0; corner < 4; corner++) {
                int colour = colours[corner];
                consumer.m_252986_(matrix, (float)corners[corner][0], (float)corners[corner][1], (float)corners[corner][2])
                    .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, colour >>> 24)
                    .m_7421_(0.5F, 0.5F)
                    .m_86008_(OverlayTexture.f_118083_)
                    .m_85969_(FULL_BRIGHT)
                    .m_252939_(normals, 0.0F, 1.0F, 0.0F)
                    .m_5752_();
            }
        };
    }
}
