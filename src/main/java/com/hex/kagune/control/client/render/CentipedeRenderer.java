package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.render.KaguneWorldRenderer;
import com.hex.kagune.client.render.TendrilChain;
import com.hex.kagune.control.KakujaBurst;
import com.hex.kagune.control.ayato.CentipedeMotion;
import com.hex.kagune.control.ayato.CentipedeTail;
import com.hex.kagune.control.ayato.KakujaModel;
import com.hex.kagune.control.client.AyatoClient;
import com.hex.kagune.control.client.fx.KakujaFx;
import com.hex.kagune.gameplay.CentipedeTechnique;
import com.hex.kagune.gameplay.KaguneForm;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Kaneki's centipede legs in the world (both kakuja): the plated legs of {@link CentipedeTail}, moved by {@link CentipedeMotion}
 * (fixed length, critically damped springs on the player's own clock, poses for rest, rearing, striking, coiling round a held
 * body, guarding). The original still runs everything else (its tendrils, the hunt, the hits); its segmented limb is not drawn
 * any more ({@link #emitTube}, the trampoline target of {@code KaguneWorldRenderer.emitTube}). Runs after the original renderer
 * in the same frame, with a pure white texel so the colours show exactly, lit by the world except the glowing flesh.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class CentipedeRenderer {
    private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final double MAX_DISTANCE_SQR = 96.0 * 96.0;
    private static final double DETAIL_DISTANCE_SQR = 28.0 * 28.0;
    private static final Map<UUID, Legs> LEGS = new HashMap<>();

    /** Each player's legs (by limb index) and when they were last drawn. */
    private static final class Legs {
        final CentipedeMotion.Leg[] legs = new CentipedeMotion.Leg[6];
        long seen;
        /** The burst these legs came out in (a new burst starts them afresh). */
        double burstAt = Double.NaN;
    }

    private CentipedeRenderer() {
    }

    /** Trampoline target of {@code KaguneWorldRenderer.emitTube}: centipede legs are drawn by this class, everything else as before. */
    public static void emitTube(VertexConsumer consumer, Matrix4f pose, Matrix3f normalMatrix, TendrilChain chain, Vec3[] points,
                                KaguneClientState.View state, int limbIndex, int light, double time, int seed) {
        if (state.form().hasCentipedeLegs() && state.form().limb(limbIndex).segmented()) {
            return;
        }
        KakujaFx.tube(state, limbIndex, points, (long)time);
        KaguneWorldRenderer.emitTubeLegacy(consumer, pose, normalMatrix, chain, points, state, limbIndex, light, time, seed);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        LocalPlayer self = minecraft.f_91074_;
        if (level == null || self == null) {
            return;
        }
        float partial = event.getPartialTick();
        double time = com.hex.kagune.control.client.ControlClient.worldTime(partial);
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        PoseStack stack = event.getPoseStack();
        Matrix4f matrix = stack.m_85850_().m_252922_();
        Matrix3f normals = stack.m_85850_().m_252943_();
        MultiBufferSource.BufferSource buffers = minecraft.m_91269_().m_110104_();
        VertexConsumer consumer = null;
        for (AbstractClientPlayer player : level.m_6907_()) {
            if (AyatoClient.isAyato(player)) {
                continue;
            }
            KakujaFx.Track track = KakujaFx.observe(player);
            KaguneClientState.View view = KaguneClientState.get(player);
            KaguneForm form = view.form();
            if (!view.active() || !form.hasCentipedeLegs() || !player.m_6084_() || player.m_20145_() || player.m_5803_() || player.m_6069_()
                || player.m_21255_()) {
                continue;
            }
            double distance = player.m_20280_(self);
            if (distance > MAX_DISTANCE_SQR) {
                continue;
            }
            boolean firstPerson = player == self && minecraft.f_91066_.m_92176_().m_90612_();
            boolean fine = !firstPerson && distance <= DETAIL_DISTANCE_SQR;
            Legs legs = LEGS.computeIfAbsent(player.m_20148_(), id -> new Legs());
            legs.seen = level.m_46467_();
            if (Double.compare(legs.burstAt, track.burstAt()) != 0) {
                // A new burst: the legs tear out afresh from his back.
                java.util.Arrays.fill(legs.legs, null);
                legs.burstAt = track.burstAt();
            }
            int legCount = 0;
            for (int limb = 0; limb < form.limbCount(); limb++) {
                legCount += form.limb(limb).segmented() ? 1 : 0;
            }
            boolean bursting = track.legsBursting(time, legCount);
            double burstAge = track.burstAge(time);
            double feetX = player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial;
            double feetY = player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial;
            double feetZ = player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial;
            float previousYaw = player.f_20884_;
            double bodyYaw = previousYaw + wrap(player.f_20883_ - previousYaw) * partial;
            double yaw = Math.toRadians(bodyYaw);
            Frame frame = new Frame(feetX, feetY, feetZ, -Math.sin(yaw), Math.cos(yaw), -Math.cos(yaw), -Math.sin(yaw));
            double headYaw = player.f_20886_ + wrap(player.f_20885_ - player.f_20886_) * partial;
            double lookYaw = Math.toRadians(wrap(headYaw - bodyYaw));
            double lookPitch = Math.toRadians(player.m_146909_());
            double speed = Math.hypot(player.m_20185_() - player.f_19790_, player.m_20189_() - player.f_19792_);
            double fullLength = CentipedeMotion.length(view.mastery());
            double width = CentipedeMotion.width(fullLength);
            int plates = CentipedeMotion.plates(fullLength, width);
            boolean finalForm = form == KaguneForm.FULL_KAKUJA;
            for (int limb = 0; limb < form.limbCount() && limb < legs.legs.length; limb++) {
                KaguneForm.LimbLayout layout = form.limb(limb);
                if (!layout.segmented()) {
                    continue;
                }
                float deploy = KaguneWorldRenderer.limbDeployment(view, limb, time);
                if (deploy <= 0.01F) {
                    legs.legs[limb] = null;
                    continue;
                }
                int order = KakujaFx.legOrder(form, limb);
                double emerged = bursting ? KakujaBurst.legOut(burstAge, order) : 1.0;
                if (emerged <= 0.01) {
                    // Still inside him: the wound tears open first.
                    continue;
                }
                double grown = bursting ? 1.0 : smooth(deploy);
                int side = layout.fanSide() >= 0.0 ? 1 : -1;
                CentipedeMotion.Input input = input(level, view, limb, side, finalForm, Math.max(0.6, fullLength * grown),
                    player.m_20206_() * 0.55, frame, partial, time, lookYaw, lookPitch, speed, width);
                if (legs.legs[limb] == null) {
                    legs.legs[limb] = new CentipedeMotion.Leg();
                }
                CentipedeMotion.Leg leg = legs.legs[limb];
                leg.follow(CentipedeMotion.pose(input), time, speed);
                double[][] rig = leg.points();
                double swell = 1.0;
                int shownPlates = plates;
                double jaw = leg.jaw();
                double flare = leg.flare();
                if (bursting) {
                    // Torn out along its own rest curve, overshooting its length, swollen, jaw gaping, claws splayed.
                    rig = KakujaBurst.extrude(rig, emerged, KakujaBurst.legStretch(burstAge, order));
                    swell = KakujaBurst.legSwell(burstAge, order);
                    shownPlates = Math.max(2, (int)Math.round(plates * emerged));
                    jaw = Math.max(jaw, KakujaBurst.legJaw(burstAge, order));
                    flare = Math.max(flare, 1.0 + 0.5 * (1.0 - emerged));
                }
                double[][] world = new double[rig.length][];
                for (int i = 0; i < rig.length; i++) {
                    world[i] = frame.place(rig[i], camera);
                }
                double[] dorsal = frame.turn(leg.dorsal());
                KakujaModel.Spine spine = CentipedeTail.through(world, fine ? 64 : 40, side, dorsal);
                CentipedeTail.Look look = new CentipedeTail.Look(width * (0.45 + 0.55 * grown) * swell, shownPlates, fine, 1.0, side > 0 ? 1 : 2, jaw,
                    flare, 0.35, leg.phase());
                double[][] absolute = new double[world.length][];
                for (int i = 0; i < world.length; i++) {
                    absolute[i] = new double[] {world[i][0] + camera.f_82479_, world[i][1] + camera.f_82480_, world[i][2] + camera.f_82481_};
                }
                KakujaFx.leg(track, limb, absolute, dorsal, side, look, level.m_46467_());
                double[] middle = world[world.length / 2];
                int light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(middle[0] + camera.f_82479_, middle[1] + camera.f_82480_,
                    middle[2] + camera.f_82481_));
                if (consumer == null) {
                    consumer = buffers.m_6299_(TYPE);
                }
                VertexConsumer out = consumer;
                CentipedeTail.tail((a, b, c, d, n, ca, cb, cc, cd) -> {
                    // Glowing quads (the flesh) carry a straight-up normal: full brightness; armor and claws take the world's light.
                    int lit = n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0 ? FULL_BRIGHT : light;
                    vertex(out, matrix, normals, a, n, ca, lit);
                    vertex(out, matrix, normals, b, n, cb, lit);
                    vertex(out, matrix, normals, c, n, cc, lit);
                    vertex(out, matrix, normals, d, n, cd, lit);
                }, spine, look);
            }
        }
        if (consumer != null) {
            buffers.m_109912_(TYPE);
        }
        if (level.m_46467_() % 40L == 0L) {
            long now = level.m_46467_();
            LEGS.values().removeIf(legs -> now - legs.seen > 100L);
        }
    }

    /** What one leg does this frame, from the original's synced state: its hunt, its hold, or rest. */
    private static CentipedeMotion.Input input(ClientLevel level, KaguneClientState.View view, int limb, int side, boolean finalForm, double length,
                                               double rootY, Frame frame, float partial, double time, double lookYaw, double lookPitch,
                                               double speed, double width) {
        CentipedeMotion.Act act = CentipedeMotion.Act.REST;
        double[] target = null;
        double radius = 0.3;
        boolean grab = false;
        int held = view.heldEntityId(limb);
        Entity holding = held == 0 ? null : level.m_6815_(held);
        if (holding != null) {
            act = CentipedeMotion.Act.COIL;
            target = frame.rig(centre(holding, partial));
            radius = holding.m_20205_() * 0.5 + width * 0.85;
        } else if (view.centipedeAttackDuration() > 0 && view.centipedeAttackTargetId() != 0
            && Math.floorMod(view.centipedeAttackLimbIndex(), Math.max(1, view.form().limbCount())) == limb) {
            Entity prey = level.m_6815_(view.centipedeAttackTargetId());
            Vec3 at = view.centipedeAttackTargetPosition();
            double[] world = prey != null ? centre(prey, partial) : new double[] {at.f_82479_, at.f_82480_, at.f_82481_};
            target = frame.rig(world);
            double age = time - view.centipedeAttackStarted();
            CentipedeTechnique technique = view.centipedeTechnique();
            grab = technique == CentipedeTechnique.GRAB;
            act = switch (technique) {
                case BLOCK -> CentipedeMotion.Act.BLOCK;
                // CentipedeHunt's timing: a strike rears to tick 3 and lands on 5; a grab rears to 4 and bites on 7.
                case STRIKE -> age < 3.0 ? CentipedeMotion.Act.LOCK : age < 8.0 ? CentipedeMotion.Act.STRIKE : CentipedeMotion.Act.REST;
                case GRAB -> age < 4.0 ? CentipedeMotion.Act.LOCK : CentipedeMotion.Act.STRIKE;
                case CRUSH -> CentipedeMotion.Act.COIL;
                case SLAM -> age < 4.0 ? CentipedeMotion.Act.COIL : CentipedeMotion.Act.SLAM;
                case THROW -> age < 4.0 ? CentipedeMotion.Act.COIL : CentipedeMotion.Act.THROW;
            };
            if (act == CentipedeMotion.Act.COIL && prey != null) {
                radius = prey.m_20205_() * 0.5 + width * 0.85;
            }
            if (act == CentipedeMotion.Act.SLAM) {
                target = new double[] {target[0], Math.max(0.2, target[1] - 1.2), target[2]};
            }
        }
        return new CentipedeMotion.Input(side, finalForm, length, rootY, act, target, radius, grab, lookYaw, lookPitch, speed, time);
    }

    /** An entity's middle (the original's hunting point: 52% up its height), interpolated for this frame. */
    private static double[] centre(Entity entity, float partial) {
        return new double[] {entity.f_19790_ + (entity.m_20185_() - entity.f_19790_) * partial,
            entity.f_19791_ + (entity.m_20186_() - entity.f_19791_) * partial + entity.m_20206_() * 0.52,
            entity.f_19792_ + (entity.m_20189_() - entity.f_19792_) * partial};
    }

    /** A player's rig frame for this frame: feet in the world, and the body's forward and right. */
    private record Frame(double x, double y, double z, double forwardX, double forwardZ, double rightX, double rightZ) {
        /** A rig point in the world, relative to the camera. */
        double[] place(double[] rig, Vec3 camera) {
            return new double[] {this.x + this.rightX * rig[0] + this.forwardX * rig[2] - camera.f_82479_, this.y + rig[1] - camera.f_82480_,
                this.z + this.rightZ * rig[0] + this.forwardZ * rig[2] - camera.f_82481_};
        }

        /** A rig direction in the world. */
        double[] turn(double[] rig) {
            return new double[] {this.rightX * rig[0] + this.forwardX * rig[2], rig[1], this.rightZ * rig[0] + this.forwardZ * rig[2]};
        }

        /** A world point in the rig. */
        double[] rig(double[] world) {
            double dx = world[0] - this.x;
            double dz = world[2] - this.z;
            return new double[] {dx * this.rightX + dz * this.rightZ, world[1] - this.y, dx * this.forwardX + dz * this.forwardZ};
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals, double[] p, double[] n, int colour, int light) {
        consumer.m_252986_(matrix, (float)p[0], (float)p[1], (float)p[2])
            .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, colour >>> 24)
            .m_7421_(0.5F, 0.5F)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
            .m_5752_();
    }

    private static double wrap(double degrees) {
        return ((degrees % 360.0) + 540.0) % 360.0 - 180.0;
    }

    private static double smooth(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return t * t * (3.0 - 2.0 * t);
    }
}
