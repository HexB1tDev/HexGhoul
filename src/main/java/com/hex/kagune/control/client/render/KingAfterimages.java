package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.ControlClient;
import com.hex.kagune.control.client.animation.BodyFrame;
import com.hex.kagune.control.client.animation.RigPose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 1.0.26: a final kakuja sprinting leaves afterimages: where it ran a moment ago its body stays, frozen in the stride it was in,
 * see-through and fading (Peace pale as ice, War crimson), the newest of them the whole form, armour and kagune and all ({@link
 * KingLayer#quads}), the older ones its body's shape alone ({@link BodyFrame#surface}). Drawn in the world after its entities.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class KingAfterimages {
    private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final int FULL_BRIGHT = 0xF000F0;
    /** How long one lasts (ticks), at most how many at once, how far off one is still drawn (blocks), and its whole form how far. */
    static final double LIFE = 10.0;
    static final int MOST = 48;
    static final double FAR = 64.0;
    static final double FORM_FAR = 28.0;
    /** How see-through each starts: the body's shape, and the whole form on the newest. */
    static final float ALPHA = 0.5F;
    static final float FORM_ALPHA = 0.32F;
    /** The tints: Peace's ice, War's crimson. */
    static final int PEACE = 0xDCE8FF;
    static final int WAR = 0xA0101E;
    /** 1.0.33: in first person his own are left out while the camera is this close across the ground (blocks): inside his own head. */
    static final double OWN_CLEAR = 1.4;
    /** Ticks a ghost is the newest (the one drawn as the whole form). */
    static final double NEWEST = 2.5;

    /** One afterimage: whose (for the form's shapes), the pose, his feet and body yaw (degrees) there, when (world ticks), whose tint. */
    record Ghost(WeakReference<AbstractClientPlayer> player, RigPose pose, double x, double y, double z, float yaw, double at, boolean war, boolean whole) {
    }

    private static final List<Ghost> GHOSTS = new ArrayList<>();
    /**
     * 1.0.36: each wearer sprinting flat out this tick: {his speed x, z (blocks a tick), when (world ticks), when the sprint began}; he is
     * drawn a little blurred along it (two faint copies of his body just behind him), most as the sprint takes off.
     */
    private static final java.util.Map<AbstractClientPlayer, double[]> BLUR = new java.util.WeakHashMap<>();
    /** 1.0.36: how far behind him each blur copy is (ticks of his speed), how see-through, and how much stronger as he takes off. */
    static final double[] BLUR_BACK = {0.45, 0.9};
    static final float[] BLUR_ALPHA = {0.2F, 0.11F};
    static final double BLUR_TAKEOFF = 10.0;

    private KingAfterimages() {
    }

    /** {@code player} left an afterimage at {@code feet} in {@code pose} (a copy is kept), facing {@code bodyYaw}, at {@code time}. */
    public static void add(AbstractClientPlayer player, RigPose pose, Vec3 feet, float bodyYaw, double time, boolean war) {
        add(player, pose, feet, bodyYaw, time, war, true);
    }

    /** 1.0.36: as above; {@code whole}: the newest drawn as the whole form (the dash's), else only ever his body's shape (the sprint's: light). */
    public static void add(AbstractClientPlayer player, RigPose pose, Vec3 feet, float bodyYaw, double time, boolean war, boolean whole) {
        if (GHOSTS.size() >= MOST) {
            GHOSTS.remove(0);
        }
        GHOSTS.add(new Ghost(new WeakReference<>(player), new RigPose().add(pose, 1.0F), feet.f_82479_, feet.f_82480_, feet.f_82481_, bodyYaw, time,
            war, whole));
    }

    /** 1.0.36: {@code player} is sprinting flat out this tick at {@code vx, vz} (blocks a tick): drawn a little blurred along it. */
    public static void blur(AbstractClientPlayer player, double vx, double vz, double time) {
        double[] was = BLUR.get(player);
        double since = was != null && time - was[2] <= 2.0 ? was[3] : time;
        BLUR.put(player, new double[] {vx, vz, time, since});
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || GHOSTS.isEmpty() && BLUR.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ == null) {
            GHOSTS.clear();
            return;
        }
        double time = ControlClient.worldTime(event.getPartialTick());
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        PoseStack stack = event.getPoseStack();
        Matrix4f matrix = stack.m_85850_().m_252922_();
        Matrix3f normals = stack.m_85850_().m_252943_();
        MultiBufferSource.BufferSource buffers = minecraft.m_91269_().m_110104_();
        VertexConsumer out = null;
        for (Iterator<Ghost> it = GHOSTS.iterator(); it.hasNext(); ) {
            Ghost ghost = it.next();
            double age = time - ghost.at();
            if (age > LIFE || age < -20.0) {
                it.remove();
                continue;
            }
            double dx = ghost.x() - camera.f_82479_;
            double dy = ghost.y() - camera.f_82480_;
            double dz = ghost.z() - camera.f_82481_;
            double away = dx * dx + dy * dy + dz * dz;
            if (age < 0.0 || away > FAR * FAR || away < 1.0) {
                continue;
            }
            AbstractClientPlayer player = ghost.player().get();
            if (player == minecraft.f_91074_ && minecraft.f_91066_.m_92176_().m_90612_() && dx * dx + dz * dz < OWN_CLEAR * OWN_CLEAR) {
                continue;
            }
            double fade = 1.0 - age / LIFE;
            int tint = ghost.war() ? WAR : PEACE;
            if (out == null) {
                out = buffers.m_6299_(TYPE);
            }
            double yaw = Math.toRadians(ghost.yaw());
            Frame frame = new Frame(dx, dy, dz, yaw);
            if (ghost.whole() && age < NEWEST && player != null && away < FORM_FAR * FORM_FAR) {
                form(out, matrix, normals, frame, ghost.pose(), player, tint, (int)Math.round(255.0 * FORM_ALPHA * fade));
            }
            int alpha = (int)Math.round(255.0 * ALPHA * fade * fade);
            if (alpha > 0) {
                int colour = alpha << 24 | tint;
                VertexConsumer sink = out;
                BodyFrame.surface(ghost.pose(), (a, b, c, d, n) -> {
                    double[] normal = frame.turn(n);
                    for (double[] p : new double[][] {a, b, c, d}) {
                        vertex(sink, matrix, normals, frame.place(p), colour, normal);
                    }
                });
            }
        }
        // 1.0.36: the blur: two faint copies of his body just behind him along his run (none of his own in first person: behind his eyes).
        for (java.util.Iterator<java.util.Map.Entry<AbstractClientPlayer, double[]>> it = BLUR.entrySet().iterator(); it.hasNext(); ) {
            java.util.Map.Entry<AbstractClientPlayer, double[]> e = it.next();
            AbstractClientPlayer player = e.getKey();
            double[] b = e.getValue();
            if (time - b[2] > 1.5 || player.m_213877_()) {
                it.remove();
                continue;
            }
            if (player == minecraft.f_91074_ && minecraft.f_91066_.m_92176_().m_90612_()) {
                continue;
            }
            RigPose pose = com.hex.kagune.control.client.animation.KaguneBodyAnimation.body(player, time);
            if (pose == null) {
                continue;
            }
            double px = player.f_19790_ + (player.m_20185_() - player.f_19790_) * event.getPartialTick();
            double py = player.f_19791_ + (player.m_20186_() - player.f_19791_) * event.getPartialTick();
            double pz = player.f_19792_ + (player.m_20189_() - player.f_19792_) * event.getPartialTick();
            double takeoff = 1.0 + 0.8 * Math.max(0.0, 1.0 - (time - b[3]) / BLUR_TAKEOFF);
            com.hex.kagune.control.client.KingClient.View view = com.hex.kagune.control.client.KingClient.get(player);
            int tint = view != null && view.form() == com.hex.kagune.control.king.KingForm.WAR ? WAR : PEACE;
            double yaw = Math.toRadians(player.f_20883_);
            if (out == null) {
                out = buffers.m_6299_(TYPE);
            }
            for (int k = 0; k < BLUR_BACK.length; k++) {
                double back = BLUR_BACK[k] * takeoff;
                Frame frame = new Frame(px - b[0] * back - camera.f_82479_, py - camera.f_82480_, pz - b[1] * back - camera.f_82481_, yaw);
                int colour = Math.min(255, (int)Math.round(255.0 * BLUR_ALPHA[k] * takeoff)) << 24 | tint;
                VertexConsumer sink = out;
                BodyFrame.surface(pose, (a, bb, c, d, n) -> {
                    double[] normal = frame.turn(n);
                    for (double[] p : new double[][] {a, bb, c, d}) {
                        vertex(sink, matrix, normals, frame.place(p), colour, normal);
                    }
                });
            }
        }
        if (out != null) {
            buffers.m_109912_(TYPE);
        }
    }

    /** Where a ghost stands: rig space (blocks: x its right, y up from its feet, z ahead) to camera-relative world. */
    record Frame(double ox, double oy, double oz, double fx, double fz, double rx, double rz) {
        Frame(double ox, double oy, double oz, double yaw) {
            this(ox, oy, oz, -Math.sin(yaw), Math.cos(yaw), -Math.cos(yaw), -Math.sin(yaw));
        }

        double[] place(double[] p) {
            return new double[] {this.ox + this.rx * p[0] + this.fx * p[2], this.oy + p[1], this.oz + this.rz * p[0] + this.fz * p[2]};
        }

        double[] turn(double[] n) {
            return new double[] {this.rx * n[0] + this.fx * n[2], n[1], this.rz * n[0] + this.fz * n[2]};
        }
    }

    /** The whole form on a ghost: every shape its layer last drew ({@link KingLayer#quads}), each body part's carried by that part as posed. */
    private static void form(VertexConsumer out, Matrix4f matrix, Matrix3f normals, Frame frame, RigPose pose, AbstractClientPlayer player, int tint,
                             int alpha) {
        if (alpha <= 0) {
            return;
        }
        String[] names = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};
        double[][] carry = new double[names.length][];
        for (int i = 0; i < names.length; i++) {
            carry[i] = carry(pose, names[i]);
        }
        double[] p = new double[3];
        KingLayer.quads(player, (q, part) -> {
            if (part < 0 || part >= carry.length) {
                return;
            }
            double[] a = carry[part];
            double[] n = {a[0] * q[12] + a[1] * q[13] + a[2] * q[14], a[3] * q[12] + a[4] * q[13] + a[5] * q[14], a[6] * q[12] + a[7] * q[13] + a[8] * q[14]};
            double[] normal = frame.turn(n);
            for (int k = 0; k < 4; k++) {
                p[0] = q[k * 3];
                p[1] = q[k * 3 + 1];
                p[2] = q[k * 3 + 2];
                double[] rig = {a[0] * p[0] + a[1] * p[1] + a[2] * p[2] + a[9], a[3] * p[0] + a[4] * p[1] + a[5] * p[2] + a[10],
                    a[6] * p[0] + a[7] * p[1] + a[8] * p[2] + a[11]};
                int own = (int)q[15 + k];
                vertex(out, matrix, normals, frame.place(rig), alpha << 24 | mix(own, tint), normal);
            }
        });
    }

    /**
     * Body part {@code name}'s place as {@code pose} has it, as one transform: {a00..a22, b0..b2}, a point of the part (its own model
     * pixels, as the layer builds its shapes) to rig space (blocks), A p + b ({@link BodyFrame#model} then {@link BodyFrame#world}, taken
     * as rigid: a limb's bend is left out of a ghost).
     */
    static double[] carry(RigPose pose, String name) {
        double[] b = BodyFrame.world(pose, BodyFrame.model(pose, name, new double[] {0, 0, 0}));
        double[] ex = BodyFrame.world(pose, BodyFrame.model(pose, name, new double[] {1, 0, 0}));
        double[] ey = BodyFrame.world(pose, BodyFrame.model(pose, name, new double[] {0, 1, 0}));
        double[] ez = BodyFrame.world(pose, BodyFrame.model(pose, name, new double[] {0, 0, 1}));
        return new double[] {ex[0] - b[0], ey[0] - b[0], ez[0] - b[0], ex[1] - b[1], ey[1] - b[1], ez[1] - b[1], ex[2] - b[2], ey[2] - b[2], ez[2] - b[2],
            b[0], b[1], b[2]};
    }

    /** A shape's own colour washed most of the way to the ghost's tint (RGB only). */
    static int mix(int own, int tint) {
        int r = (((own >> 16) & 0xFF) * 2 + ((tint >> 16) & 0xFF) * 3) / 5;
        int g = (((own >> 8) & 0xFF) * 2 + ((tint >> 8) & 0xFF) * 3) / 5;
        int b = ((own & 0xFF) * 2 + (tint & 0xFF) * 3) / 5;
        return r << 16 | g << 8 | b;
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Matrix3f normals, double[] p, int colour, double[] n) {
        out.m_252986_(matrix, (float)p[0], (float)p[1], (float)p[2])
            .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, colour >>> 24)
            .m_7421_(0.5F, 0.5F)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(FULL_BRIGHT)
            .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
            .m_5752_();
    }
}
