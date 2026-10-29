package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.ukaku.UkakuModel;
import com.hex.kagune.control.client.KingClient;
import com.hex.kagune.control.client.animation.BodyFrame;
import com.hex.kagune.control.client.animation.KaguneBodyAnimation;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.king.WarHoldModel;
import com.hex.kagune.control.king.KingMove;
import com.hex.kagune.control.king.WarRules;
import com.hex.kagune.control.network.S2CKingFx;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.0.28: what War's held moves look like out in the world, on every client: Black Reaper's black centipedes out of his back, out at
 * what he aims at, biting, dragging it in, going back into him; the Spire Wave's centipedes winding round his outstretched arm while
 * it charges, and the mass of them sent out along it (crawling, writhing round one another, carrying what they reach, closing on it at
 * the end and crushing it, coming back); King's Defiance's spikes out of the cocoon at whatever struck it. The cocoon itself is his
 * rinkaku ({@code WarModel.Motion#orbed}). Their shapes are {@link WarHoldModel}'s.
 */
public final class WarHoldFx {
    /** How far off any of it is drawn finely (blocks). */
    static final double FINE = 32.0;
    /** Each player's last Spire Wave: {when it was let go (world ticks), when it crushes, when it is all back}. */
    private static final Map<UUID, double[]> SPIRES = new HashMap<>();

    private WarHoldFx() {
    }

    /** How long effect {@code kind} lasts (ticks), or -1 if it is not one of these. */
    public static int lasts(int kind) {
        return switch (kind) {
            case S2CKingFx.DEFY_SPIKE -> WarRules.DEFY_SPIKE_TICKS + 1;
            case S2CKingFx.SPIRE_WAVE -> WarRules.spireTicks(WarRules.SPIRE_REACH_HIGH, WarRules.SPIRE_SPEED_LOW) + 2;
            default -> -1;
        };
    }

    /** {@code player}'s Spire Wave as his body has it: {when it crushes, when it is all back} (ticks after letting go). */
    public static double[] spireEnds(AbstractClientPlayer player) {
        double[] s = SPIRES.get(player.m_20148_());
        return s == null ? new double[] {26.0, 48.0} : new double[] {s[1], s[2]};
    }

    /** An effect arrived ({@code now}: world ticks). */
    public static void accept(int kind, UUID owner, int[] ids, float[] data, double now) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ == null) {
            return;
        }
        switch (kind) {
            case S2CKingFx.SPIRE_WAVE -> {
                if (data.length < 11) {
                    return;
                }
                int arrive = WarRules.spireArrive(data[6], data[8]);
                SPIRES.put(owner, new double[] {now, arrive, WarRules.spireTicks(data[6], data[8])});
                Vec3 from = new Vec3(data[0], data[1], data[2]);
                WarPathFx.feel(minecraft, from, 10.0, 0.35F + 0.35F * data[9], 8.0);
                Vec3 dir = new Vec3(data[3], data[4], data[5]);
                for (int k = 0; k < RcFx.count(14.0F); k++) {
                    Vec3 v = dir.m_82490_(0.15 + Math.random() * 0.25).m_82520_((Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.2);
                    RcFx.at(RcFx.EMBER, true, 12 + (int)(Math.random() * 8), from).velocity(v).colors(RcFx.RED, RcFx.clear(RcFx.CRIMSON)).size(0.16F, 0.04F)
                        .drag(0.88F).spawn();
                }
            }
            case S2CKingFx.DEFY_SPIKE -> {
                if (data.length < 6) {
                    return;
                }
                Vec3 at = new Vec3(data[0], data[1], data[2]);
                Vec3 out = new Vec3(data[3] - data[0], data[4] - data[1], data[5] - data[2]).m_82541_();
                for (int k = 0; k < RcFx.count(8.0F); k++) {
                    Vec3 v = out.m_82490_(0.1 + Math.random() * 0.2).m_82520_((Math.random() - 0.5) * 0.15, (Math.random() - 0.5) * 0.15, (Math.random() - 0.5) * 0.15);
                    RcFx.at(RcFx.SPARK, true, 8, at).velocity(v).colors(0xFFFF6040, RcFx.clear(RcFx.CRIMSON)).size(0.12F, 0.03F).drag(0.85F).spawn();
                }
            }
            default -> {
            }
        }
    }

    /** Each tick of an effect, {@code age} ticks into it. */
    public static void tick(int kind, UUID owner, int[] ids, float[] data, double age, double now) {
        if (kind != S2CKingFx.SPIRE_WAVE || data.length < 11) {
            return;
        }
        double reach = data[6];
        double speed = data[8];
        int arrive = WarRules.spireArrive(reach, speed);
        double front = WarRules.spireFront(age, reach, speed);
        Vec3 head = new Vec3(data[0] + data[3] * front, data[1] + data[4] * front, data[2] + data[5] * front);
        if (!RcFx.near(head, WarPathFx.FAR)) {
            return;
        }
        double size = data[7];
        if (age < arrive) {
            // Dust and black flakes torn up round the head as it goes.
            for (int k = 0; k < RcFx.count((float)(2.0 + size)); k++) {
                double a = Math.random() * Math.PI * 2.0;
                Vec3 at = head.m_82520_(Math.cos(a) * size * 0.8, (Math.random() - 0.5) * size, Math.sin(a) * size * 0.8);
                RcFx.at(RcFx.PUFF, false, 16 + (int)(Math.random() * 10), at).velocity(new Vec3(data[3], data[4], data[5]).m_82490_(0.1).m_82520_(0.0, 0.03, 0.0))
                    .drag(0.88F).colors(0xA0302A26, RcFx.clear(0xFF4A423A)).size((float)(0.4 + size * 0.2), (float)(1.0 + size * 0.4)).spawn();
            }
            if (Math.random() < 0.6) {
                RcFx.at(RcFx.ASH, false, 20, head).velocity(new Vec3(Math.random() - 0.5, 0.3, Math.random() - 0.5).m_82490_(0.15))
                    .colors(0xFF120E0E, RcFx.clear(0xFF120E0E)).size(0.12F, 0.08F).fall(0.03F).spawn();
            }
        } else if (age < arrive + 1.0) {
            // The crush: blood and broken RC burst out of where they closed; the ground shakes near it.
            for (int k = 0; k < RcFx.count((float)(16.0 + 10.0 * data[9])); k++) {
                Vec3 v = new Vec3(Math.random() - 0.5, Math.random() * 0.6, Math.random() - 0.5).m_82490_(0.3 + 0.2 * data[9]);
                boolean red = k % 2 == 0;
                RcFx.at(red ? RcFx.EMBER : RcFx.PUFF, red, 14 + (int)(Math.random() * 12), head).velocity(v).drag(0.86F)
                    .colors(red ? RcFx.RED : 0xC0302A26, red ? RcFx.clear(RcFx.CRIMSON) : RcFx.clear(0xFF4A423A)).size(red ? 0.18F : 0.6F, red ? 0.05F : 1.6F)
                    .spawn();
            }
            RcFx.at(RcFx.RING, true, 10, head).colors(0xC0FF4A3A, RcFx.clear(RcFx.CRIMSON)).size(0.4F, (float)(size * 3.0)).bloom(1.0F).spawn();
            WarPathFx.feel(Minecraft.m_91087_(), head, 18.0 + 8.0 * data[9], 0.6F + 0.4F * data[9], 12.0);
        }
    }

    /** Its shapes this frame, {@code age} ticks in. */
    public static void draw(UkakuModel.Sink sink, int kind, UUID owner, int[] ids, float[] data, double age, double now, float partial) {
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null) {
            return;
        }
        switch (kind) {
            case S2CKingFx.SPIRE_WAVE -> {
                if (data.length >= 11) {
                    AbstractClientPlayer who = WarPathFx.player(level, owner);
                    double[] from = {data[0], data[1], data[2]};
                    double[] hand = from;
                    if (who != null) {
                        RigPose pose = KaguneBodyAnimation.current(who, partial);
                        hand = new Body(who, partial, pose == null ? new RigPose() : pose).at("leftArm", WarHoldModel.HAND);
                    }
                    double front = WarRules.spireFront(age, data[6], data[8]);
                    Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
                    double mx = data[0] + data[3] * front * 0.5 - camera.f_82479_;
                    double my = data[1] + data[4] * front * 0.5 - camera.f_82480_;
                    double mz = data[2] + data[5] * front * 0.5 - camera.f_82481_;
                    boolean fine = mx * mx + my * my + mz * mz < 40.0 * 40.0;
                    WarHoldModel.draw(sink, WarHoldModel.mass(from, new double[] {data[3], data[4], data[5]}, hand, data[6], data[7], data[8], data[9],
                        Math.max(1, Math.round(data[10])), ids.length > 0 ? ids[0] : 0, age, now), fine);
                }
            }
            case S2CKingFx.DEFY_SPIKE -> {
                if (data.length >= 6) {
                    int seed = ids.length > 0 ? ids[0] * 31 + (int)(data[0] * 7.0F) : 7;
                    WarHoldModel.defySpike(sink, new double[] {data[0], data[1], data[2]}, new double[] {data[3], data[4], data[5]}, age, seed, now);
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ on his body

    /** What {@code player}'s held move puts out of his body this frame: Black Reaper's centipedes, the Spire Wave's round his arm. */
    public static void player(UkakuModel.Sink sink, AbstractClientPlayer player, float partial, double now, Vec3 camera) {
        KingClient.View view = KingClient.get(player);
        if (view == null || !view.on()) {
            return;
        }
        KingMove move = view.moveAt(now);
        if (move != KingMove.REAPER && move != KingMove.SPIRE_AIM) {
            return;
        }
        RigPose pose = KaguneBodyAnimation.current(player, partial);
        if (pose == null) {
            pose = new RigPose();
        }
        Body body = new Body(player, partial, pose);
        boolean fine = player.m_20182_().m_82557_(camera) < FINE * FINE;
        double age = now - view.moveAt();
        if (move == KingMove.REAPER) {
            reaper(sink, body, view, age, now, partial, fine);
        } else {
            coils(sink, body, age, now, fine);
        }
    }

    /** A player's body this frame: its pose, and where it stands and faces in the world. */
    record Body(AbstractClientPlayer player, double x, double y, double z, double[] forward, double[] right, RigPose pose) {
        Body(AbstractClientPlayer player, float partial, RigPose pose) {
            this(player, player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial, player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial,
                player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial, forward(player, partial), right(player, partial), pose);
        }

        static double yaw(AbstractClientPlayer player, float partial) {
            float was = player.f_20884_;
            float turn = ((player.f_20883_ - was) % 360.0F + 540.0F) % 360.0F - 180.0F;
            return Math.toRadians(was + turn * partial);
        }

        static double[] forward(AbstractClientPlayer player, float partial) {
            double yaw = yaw(player, partial);
            return new double[] {-Math.sin(yaw), 0.0, Math.cos(yaw)};
        }

        static double[] right(AbstractClientPlayer player, float partial) {
            double yaw = yaw(player, partial);
            return new double[] {-Math.cos(yaw), 0.0, -Math.sin(yaw)};
        }

        /** A point of body part {@code part} (its own pixels) in the world, as posed. */
        double[] at(String part, double[] local) {
            double[] rig = BodyFrame.world(this.pose, BodyFrame.model(this.pose, part, local));
            return new double[] {this.x + this.right[0] * rig[0] + this.forward[0] * rig[2], this.y + rig[1], this.z + this.right[2] * rig[0] + this.forward[2] * rig[2]};
        }

        /** Where he looks. */
        double[] look(float partial) {
            float yaw = this.player.f_19859_ + (this.player.m_146908_() - this.player.f_19859_) * partial;
            float pitch = this.player.f_19860_ + (this.player.m_146909_() - this.player.f_19860_) * partial;
            double y = Math.toRadians(yaw);
            double p = Math.toRadians(pitch);
            return new double[] {-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p)};
        }
    }

    /** Black Reaper's centipedes ({@link WarHoldModel#reaper}) out of {@code body}'s back at what {@code view} drags. */
    static void reaper(UkakuModel.Sink sink, Body body, KingClient.View view, double age, double now, float partial, boolean fine) {
        Entity target = view.target() == 0 ? null : body.player().m_9236_().m_6815_(view.target());
        double[] prey;
        double radius;
        if (target != null) {
            prey = new double[] {target.f_19790_ + (target.m_20185_() - target.f_19790_) * partial,
                target.f_19791_ + (target.m_20186_() - target.f_19791_) * partial + target.m_20206_() * 0.55,
                target.f_19792_ + (target.m_20189_() - target.f_19792_) * partial};
            radius = target.m_20205_() * 0.5;
        } else {
            // At nothing: out where he looks, and back.
            double[] eye = {body.x(), body.y() + 1.62, body.z()};
            double[] look = body.look(partial);
            double far = WarRules.REAPER_RANGE * 0.75;
            prey = new double[] {eye[0] + look[0] * far, eye[1] + look[1] * far, eye[2] + look[2] * far};
            radius = 0.2;
        }
        double[][] roots = new double[WarRules.REAPER_ARMS][];
        for (int k = 0; k < roots.length; k++) {
            roots[k] = body.at("torso", WarHoldModel.reaperRoot(k));
        }
        WarHoldModel.draw(sink, WarHoldModel.reaper(roots, body.forward(), body.right(), prey, radius, view.level(), age, now), fine);
    }

    /** The Spire Wave's centipedes round {@code body}'s outstretched arm while it is held ({@link WarHoldModel#coils}). */
    static void coils(UkakuModel.Sink sink, Body body, double age, double now, boolean fine) {
        WarHoldModel.draw(sink, WarHoldModel.coils(body.at("leftArm", WarHoldModel.ELBOW), body.at("leftArm", WarHoldModel.HAND), age, now), fine);
    }
}
