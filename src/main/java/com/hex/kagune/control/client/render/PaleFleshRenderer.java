package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.PaleClient;
import com.hex.kagune.control.client.animation.PaleFleshPose;
import com.hex.kagune.control.client.animation.PaleMotions;
import com.hex.kagune.control.client.animation.PaleOwnArm;
import com.hex.kagune.control.client.animation.PaleOwnLeg;
import com.hex.kagune.control.client.animation.WatchPose;
import com.hex.kagune.control.client.WatchClient;
import com.hex.kagune.control.client.OwlShardClient;
import com.hex.kagune.control.pale.OwlShards;
import com.hex.kagune.control.pale.OwlLances;
import com.hex.kagune.control.client.animation.OwlLancePose;
import com.hex.kagune.control.client.OwlLanceClient;
import com.hex.kagune.control.client.ArmClient;
import com.hex.kagune.control.client.KakuganClient;
import com.hex.kagune.control.client.OwlClient;
import com.hex.kagune.control.client.animation.OwlArms;
import com.hex.kagune.control.client.animation.OwlBulk;
import com.hex.kagune.control.client.animation.OwlMotions;
import com.hex.kagune.control.client.animation.OwlPose;
import com.hex.kagune.control.pale.OwlModel;
import com.hex.kagune.control.pale.OwlActs;
import com.hex.kagune.control.pale.OwlCreature;
import com.hex.kagune.control.client.animation.ArmPose;
import com.hex.kagune.control.client.animation.BodyFrame;
import com.hex.kagune.control.client.animation.FirstPersonArms;
import com.hex.kagune.control.pale.ArmRules;
import com.hex.kagune.control.pale.WatchRules;
import net.minecraft.world.entity.LivingEntity;
import com.hex.kagune.control.client.animation.KaguneBodyAnimation;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.pale.PaleFleshModel;
import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.FleshPhysics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
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
 * 6.1.0: Pale's pale flesh in the world ({@link PaleFleshModel}), for every Pale whose move has it out: out of her back as her body
 * is drawn this very frame, over her right shoulder, raking across her front (the swipe), reaching for, holding and flinging
 * what she catches (the grab). The world's light falls on it; in first person, what would fill the view near her own eyes (the
 * root and the bend over the shoulder) is left out.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class PaleFleshRenderer {
    private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    /** Drawn this close to the camera (blocks); in first person, nothing within this of the eyes. */
    static final double RANGE = 96.0;
    static final double EYE_CLEARANCE = 0.75;
    /**
     * 6.2.0: in first person, for her own leg and the club round it (seen from where her head has gone, {@link PaleOwnLeg#shift}),
     * only what is right on the eyes is left out.
     */
    static final double OWN_LEG_CLEARANCE = 0.2;
    private static final double[] NO_SHIFT = {0.0, 0.0, 0.0};
    /** Packed light at full brightness (sky and block light both 15). */
    private static final int FULL_BRIGHT = 0xF000F0;

    private PaleFleshRenderer() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null || minecraft.f_91074_ == null) {
            return;
        }
        float partial = event.getPartialTick();
        double time = PaleClient.clock(partial);
        // 1.2.2: the final kakujas (KingClient's views) are on the world's clock, not Pale's.
        double kingTime = com.hex.kagune.control.client.ControlClient.worldTime(partial);
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        PoseStack stack = event.getPoseStack();
        Matrix4f matrix = stack.m_85850_().m_252922_();
        Matrix3f normals = stack.m_85850_().m_252943_();
        MultiBufferSource.BufferSource buffers = minecraft.m_91269_().m_110104_();
        boolean drew = false;
        ownArms = false;
        for (AbstractClientPlayer player : level.m_6907_()) {
            PaleClient.PaleView view = PaleClient.get(player);
            if (WatchClient.out(player) && player.m_6084_() && !player.m_20145_() && player.m_20280_(minecraft.f_91074_) <= RANGE * RANGE) {
                drew |= tentacles(player, view, time, partial, level, minecraft, camera, matrix, normals, buffers);
            }
            if (ArmClient.out(player) && player.m_6084_() && !player.m_20145_() && player.m_20280_(minecraft.f_91074_) <= (RANGE + 40.0) * (RANGE + 40.0)) {
                drew |= arm(player, view, time, partial, level, minecraft, camera, matrix, normals, buffers);
            }
            OwlClient.Owl owl = OwlClient.get(player, time);
            if (owl != null && player.m_6084_() && !player.m_20145_() && player.m_20280_(minecraft.f_91074_) <= RANGE * RANGE) {
                drew |= owl(player, owl, time, partial, level, minecraft, camera, matrix, normals, buffers);
            }
            if (OwlLanceClient.out(player) && player.m_6084_() && !player.m_20145_() && player.m_20280_(minecraft.f_91074_) <= RANGE * RANGE) {
                drew |= lances(player, owl, view, time, partial, level, minecraft, camera, matrix, normals, buffers);
            }
            // (7.0.1: and any ghoul's with only the eyes open: the shared eye-mode fight.)
            if (firstPerson(minecraft, player) && player.m_6084_() && com.hex.kagune.control.client.PaleClient.fighter(player)
                && !OwlClient.creature(player, time)) {
                // 6.2.0: her own arms in first person through her strikes and the Big Punch (and the guard after), posed exactly as
                // third person poses them; vanilla's hands are not drawn meanwhile (ArmHider).
                ownArms = arms(player, partial, time, camera, level, matrix, normals, buffers);
            }
            if (firstPerson(minecraft, player) && player.m_6084_()) {
                // 1.0.16: Peace's blades in first person, wherever they reach out before him (the body is never drawn there). 1.0.26: and
                // War's rinkaku: all of the kagune each wears, seen from his eyes. 1.2.2: on the world's clock, as the final kakujas are
                // told and as third person draws them (on Pale's own tick count, which starts at 0 with the game, every one of them was
                // still to come: nothing of Peace, nor War's rinkaku, was ever drawn in first person in a world older than the session).
                peaceBlades(player, partial, kingTime, camera, level, matrix, normals, buffers);
                warRinkaku(player, partial, kingTime, camera, level, matrix, normals, buffers);
            }
            if (view.move() == PaleMove.KICK && firstPerson(minecraft, player) && player.m_6084_()) {
                if (com.hex.kagune.control.client.PaleClient.kickGone(player, partial)) {
                    continue; // 6.2.0: gone in the dash, nothing of her
                }
                // 6.2.0: her own leg in first person (6.2.0: the lunge; there, the knee up, the leg driven straight into its middle).
                float bodyYaw = player.f_20884_ + wrap(player.f_20883_ - player.f_20884_) * partial;
                RigPose body = PaleMotions.kickBody(view.age(time), view.detail(), (float)Math.toRadians(wrap(view.yaw() - bodyYaw)));
                double yaw = Math.toRadians(bodyYaw);
                Frame frame = new Frame(player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial, player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial,
                    player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial, -Math.sin(yaw), Math.cos(yaw), -Math.cos(yaw), -Math.sin(yaw));
                leg(body, PaleOwnLeg.shift(body), frame, camera, level, player, matrix, normals, buffers);
                continue;
            }
            if (!view.move().flesh() || !player.m_6084_() || player.m_20145_() || player.m_20280_(minecraft.f_91074_) > RANGE * RANGE) {
                continue;
            }
            double age = view.age(time);
            double feetX = player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial;
            double feetY = player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial;
            double feetZ = player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial;
            float bodyYaw = player.f_20884_ + wrap(player.f_20883_ - player.f_20884_) * partial;
            double yaw = Math.toRadians(bodyYaw);
            double turn = Math.toRadians(wrap(view.yaw() - bodyYaw));
            // On her body as drawn this frame; when it isn't (first person: the model is never set up, and what it was last drawn in
            // is stale), on the body as the move alone poses it, so the flesh is where it would be seen from outside.
            RigPose body = KaguneBodyAnimation.drawn(player, view.move(), time);
            if (body == null) {
                body = PaleMotions.fleshBody(view.move(), age, view.charge(), (float)turn);
            }
            if (body == null) {
                body = new RigPose();
            }
            Frame frame = new Frame(feetX, feetY, feetZ, -Math.sin(yaw), Math.cos(yaw), -Math.cos(yaw), -Math.sin(yaw));
            double[] tip;
            double grip = 0.5;
            Entity target = view.targetId() == 0 ? null : level.m_6815_(view.targetId());
            FleshMotion.Space space = new FleshMotion.Space() {
                @Override
                public double[] world(double[] rig) {
                    return frame.world(rig, Vec3.f_82478_);
                }

                @Override
                public double[] rig(double[] world) {
                    return frame.rig(world[0], world[1], world[2]);
                }
            };
            if (view.move() == PaleMove.HAMMER_CHARGE || view.move() == PaleMove.HAMMER) {
                // The hammer's club, round her leg.
                // 6.2.0: True Anonymity's club (detail 1): grown on past its full size, spiked.
                boolean huge = view.detail() == 1;
                float charge = view.move() == PaleMove.HAMMER ? view.charge() : PaleRules.hammerCharge(age, huge);
                PaleFleshModel.Pose club = FleshMotion.carried(player.m_19879_(), 0, 0L, FAMILY_HAMMER,
                    PaleFleshPose.club(body, view.move(), age, charge, time, player.m_19879_(), huge), space, time, FleshPhysics.CLUB, body);
                // In first person she sees her own leg swing up in front of her and the club grow round it, as from where her head
                // has gone (the camera doesn't tip back with her).
                boolean own = firstPerson(minecraft, player);
                double[] shift = own ? PaleOwnLeg.shift(body) : NO_SHIFT;
                drew |= draw(club, frame, camera, level, minecraft, player, matrix, normals, buffers, shift, own ? OWN_LEG_CLEARANCE : EYE_CLEARANCE);
                if (own) {
                    leg(body, shift, frame, camera, level, player, matrix, normals, buffers);
                }
                continue;
            }
            boolean flung = view.move() == PaleMove.GRAB_THROW && age > PaleRules.grabRelease();
            if (view.move() == PaleMove.SWIPE) {
                tip = PaleFleshPose.turned(PaleFleshPose.swipeTip(age), turn);
            } else if (flung) {
                // Let go: it draws back in from where it let go.
                tip = PaleFleshPose.turned(PaleRules.GRAB_FLING.clone(), turn);
            } else if (target != null && view.move() != PaleMove.GRAB_MISS) {
                double tx = target.f_19790_ + (target.m_20185_() - target.f_19790_) * partial;
                double ty = target.f_19791_ + (target.m_20186_() - target.f_19791_) * partial + target.m_20206_() * 0.5;
                double tz = target.f_19792_ + (target.m_20189_() - target.f_19792_) * partial;
                tip = frame.rig(tx, ty, tz);
                grip = Math.max(0.3, Math.max(target.m_20205_(), target.m_20206_() * 0.45) * 0.6);
            } else {
                tip = frame.rig(view.x(), view.y(), view.z());
            }
            // The flesh carries its own weight: the swipe's heavy end whips through; what it holds stays held at its tip.
            FleshPhysics.Feel feel = switch (view.move()) {
                case SWIPE -> FleshPhysics.WHIP;
                case GRAB_HOLD -> FleshPhysics.REACH.held(true);
                case GRAB_THROW -> FleshPhysics.REACH.held(!flung);
                default -> FleshPhysics.REACH;
            };
            int family = view.move() == PaleMove.SWIPE ? FAMILY_SWIPE : FAMILY_GRAB;
            PaleFleshModel.Pose flesh = FleshMotion.carried(player.m_19879_(), 0, family == FAMILY_SWIPE ? view.startedAt() : 0L, family,
                PaleFleshPose.of(body, view.move(), age, tip, grip, view.charge(), time, player.m_19879_()), space, time, feel, body);
            drew |= draw(flesh, frame, camera, level, minecraft, player, matrix, normals, buffers, NO_SHIFT, EYE_CLEARANCE);
        }
        drew |= punctures(level, partial, time, camera, matrix, normals, buffers);
        FleshMotion.forget(time);
        if (OwlShardClient.any()) {
            drew |= shards(time, level, camera, matrix, normals, buffers);
        }
        if (drew) {
            buffers.m_109912_(TYPE);
        }
    }

    /** 6.2.0: every Owl's volley spears out in the world ({@link OwlShards}): in flight, or stuck where they hit. */
    private static boolean shards(double time, ClientLevel level, Vec3 camera, Matrix4f matrix, Matrix3f normals, MultiBufferSource.BufferSource buffers) {
        // The world itself as the frame: its x, y and z as they are.
        Frame frame = new Frame(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0);
        Map<Long, Integer> lights = new HashMap<>();
        VertexConsumer out = buffers.m_6299_(TYPE);
        boolean[] any = {false};
        for (OwlShardClient.Shard shard : OwlShardClient.all()) {
            double[] point = shard.point(time);
            if ((point[0] - camera.f_82479_) * (point[0] - camera.f_82479_) + (point[1] - camera.f_82480_) * (point[1] - camera.f_82480_)
                + (point[2] - camera.f_82481_) * (point[2] - camera.f_82481_) > RANGE * RANGE) {
                continue;
            }
            double[] way = shard.way(time);
            double[] tail = {point[0] - way[0] * OwlShards.LENGTH, point[1] - way[1] * OwlShards.LENGTH, point[2] - way[2] * OwlShards.LENGTH};
            OwlShards.mesh((a, b, c, d, n, ca, cb, cc, cd) -> {
                double[] wa = frame.world(a, camera);
                double[] wb = frame.world(b, camera);
                double[] wc = frame.world(c, camera);
                double[] wd = frame.world(d, camera);
                int light = light(level, lights, (wa[0] + wc[0]) * 0.5 + camera.f_82479_, (wa[1] + wc[1]) * 0.5 + camera.f_82480_,
                    (wa[2] + wc[2]) * 0.5 + camera.f_82481_);
                vertex(out, matrix, normals, wa, n, ca, light);
                vertex(out, matrix, normals, wb, n, cb, light);
                vertex(out, matrix, normals, wc, n, cc, light);
                vertex(out, matrix, normals, wd, n, cd, light);
                any[0] = true;
            }, tail, way, 1.0, shard.id * 31);
        }
        return any[0];
    }

    /** 6.2.0: the holes the Watch This beaks left through mobs ({@link PalePunctures}), in the world's light. */
    private static boolean punctures(ClientLevel level, float partial, double time, Vec3 camera, Matrix4f matrix, Matrix3f normals,
                                     MultiBufferSource.BufferSource buffers) {
        boolean[] drew = {false};
        Map<Long, Integer> lights = new HashMap<>();
        VertexConsumer[] out = {null};
        PalePunctures.draw(level, partial, time, (a, b, c, d, n, ca, cb, cc, cd) -> {
            if (out[0] == null) {
                out[0] = buffers.m_6299_(TYPE);
            }
            double[] wa = {a[0] - camera.f_82479_, a[1] - camera.f_82480_, a[2] - camera.f_82481_};
            double[] wb = {b[0] - camera.f_82479_, b[1] - camera.f_82480_, b[2] - camera.f_82481_};
            double[] wc = {c[0] - camera.f_82479_, c[1] - camera.f_82480_, c[2] - camera.f_82481_};
            double[] wd = {d[0] - camera.f_82479_, d[1] - camera.f_82480_, d[2] - camera.f_82481_};
            int light = light(level, lights, a[0], a[1], a[2]);
            vertex(out[0], matrix, normals, wa, n, ca, light);
            vertex(out[0], matrix, normals, wb, n, cb, light);
            vertex(out[0], matrix, normals, wc, n, cc, light);
            vertex(out[0], matrix, normals, wd, n, cd, light);
            drew[0] = true;
        });
        return drew[0];
    }

    /** Which chain a move's flesh is carried by: one through a grab's reach, hold and throw; one through the hammer's raise and slam. */
    static final int FAMILY_SWIPE = 1;
    static final int FAMILY_GRAB = 2;
    static final int FAMILY_HAMMER = 3;
    /** 6.2.0: each of the Watch This tentacles, through all its ten seconds. */
    static final int FAMILY_WATCH = 4;
    /** 6.2.0: the Mutated Arm, from when it shoots out until it is back in her. */
    static final int FAMILY_ARM = 5;

    /**
     * 6.2.0: her Mutated Arm ({@link ArmPose}) out of her right shoulder as her body is drawn this frame (or as her move alone poses
     * it), carrying its weight, the hand right on what it holds where that is drawn this frame; and the sphere it folds into.
     */
    private static boolean arm(AbstractClientPlayer player, PaleClient.PaleView view, double time, float partial, ClientLevel level, Minecraft minecraft,
                               Vec3 camera, Matrix4f matrix, Matrix3f normals, MultiBufferSource.BufferSource buffers) {
        ArmClient.Arm arm = ArmClient.get(player);
        float bodyYaw = player.f_20884_ + wrap(player.f_20883_ - player.f_20884_) * partial;
        double yaw = Math.toRadians(bodyYaw);
        Frame frame = new Frame(player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial, player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial,
            player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial, -Math.sin(yaw), Math.cos(yaw), -Math.cos(yaw), -Math.sin(yaw));
        double age = arm.age(time);
        ArmPose.Reach reach = arm.reach(time);
        RigPose body = KaguneBodyAnimation.drawnNow(player, time);
        if (body == null) {
            // Not drawn this frame (first person): her body as the move poses it, under the arm's weight, the arm pointed out.
            body = PaleMotions.anyBody(view.move(), view.age(time), view.detail(), view.charge(), (float)Math.toRadians(wrap(view.yaw() - bodyYaw)));
            ArmPose.body(body, arm.phase, age, reach, frame.turnBack(arm.aim(partial)), arm.afterSphere(), time);
            body.settle(1.0F);
        }
        boolean own = firstPerson(minecraft, player);
        double[] shift = own ? PaleOwnLeg.shift(body) : NO_SHIFT;
        FleshMotion.Space space = new FleshMotion.Space() {
            @Override
            public double[] world(double[] rig) {
                return frame.world(rig, Vec3.f_82478_);
            }

            @Override
            public double[] rig(double[] world) {
                return frame.rig(world[0], world[1], world[2]);
            }
        };
        // Its reach is measured from her shoulder (as the server measures it); it bursts out of her hand.
        double[] shoulder = ArmPose.shoulder(body);
        double[] root = ArmPose.root(body);
        double[] aim = ArmPose.aimNow(frame.turnBack(arm.aim(partial)), reach.length(), ArmPose.way(body));
        double[] aimWorld = frame.turn(aim);
        double[] end;
        double girth;
        double[] sphere = null;
        double hold = 0.0;
        if (arm.phase == ArmRules.Phase.SPHERE) {
            sphere = frame.rig(arm.said.x(), arm.said.y(), arm.said.z());
            girth = ArmPose.girth(reach.girth(), ArmPose.distance(shoulder, sphere));
            end = ArmPose.onSphere(root, sphere, arm.said.radius());
            hold = 1.0;
        } else if (arm.said.caught().length > 0 && level.m_6815_(arm.said.caught()[0]) instanceof LivingEntity held) {
            // Its hand round what it holds, where that is drawn this frame.
            double[] at = frame.rig(held.f_19790_ + (held.m_20185_() - held.f_19790_) * partial,
                held.f_19791_ + (held.m_20186_() - held.f_19791_) * partial + held.m_20206_() * 0.5,
                held.f_19792_ + (held.m_20189_() - held.f_19792_) * partial);
            girth = ArmPose.girth(reach.girth(), ArmPose.distance(shoulder, at));
            end = ArmPose.holding(root, at, girth);
            hold = 1.0;
        } else {
            // As far out as it is, short of anything solid.
            double length = Math.min(reach.length(), clear(level, player, frame.world(shoulder, Vec3.f_82478_), aimWorld, reach.length()));
            girth = ArmPose.girth(reach.girth(), length);
            end = ArmPose.out(shoulder, aim, length, girth);
        }
        boolean drew = false;
        if (own && ArmPose.pointing(arm.phase, age, arm.afterSphere()) > 0.0) {
            // 6.2.0: in first person her own arm, pointed out, the flesh coming out of her hand (vanilla's hand is not drawn).
            ownArm(body, shift, frame, camera, level, player, matrix, normals, buffers);
            drew = true;
        }
        PaleFleshModel.Pose pose = FleshMotion.carried(player.m_19879_(), 3, arm.said.downAt(), FAMILY_ARM,
            ArmPose.pose(body, end, girth, reach.hand(), time, player.m_19879_() * 7 + 1), space, time, FleshPhysics.ARM.held(hold), body, false);
        drew |= draw(pose, frame, camera, level, minecraft, player, matrix, normals, buffers, shift, own ? OWN_LEG_CLEARANCE : EYE_CLEARANCE);
        if (sphere != null) {
            double[] state = ArmRules.sphere(age);
            double[] back = ArmPose.back(root, sphere);
            double[] centre = sphere;
            boolean[] any = {false};
            Map<Long, Integer> lights = new HashMap<>();
            VertexConsumer out = buffers.m_6299_(TYPE);
            PaleFleshModel.cocoon((a, b, c, d, n, ca, cb, cc, cd) -> {
                double[] wa = frame.world(a, camera);
                double[] wb = frame.world(b, camera);
                double[] wc = frame.world(c, camera);
                double[] wd = frame.world(d, camera);
                double[] wn = frame.turn(n);
                int light = light(level, lights, wa[0] + camera.f_82479_, wa[1] + camera.f_82480_, wa[2] + camera.f_82481_);
                vertex(out, matrix, normals, wa, wn, ca, light);
                vertex(out, matrix, normals, wb, wn, cb, light);
                vertex(out, matrix, normals, wc, wn, cc, light);
                vertex(out, matrix, normals, wd, wn, cd, light);
                any[0] = true;
            }, centre, back, arm.said.radius(), state[0], state[1], time, player.m_19879_());
            drew |= any[0];
        }
        return drew;
    }

    /**
     * 6.2.4: the Owl as a whole creature in her place ({@link OwlCreature}; her own body is not drawn meanwhile, {@link OwlClient#hideBody}):
     * carried over from her body as it is posed this frame (the layer run here, as her model would run it), grown out as far as it has
     * (swelling up out of her as it bursts out, shrinking back into her as it goes), the four kagune limbs off its back unfolding as it
     * grows; the bone spikes out of its back; the blades' trails. In first person she sees out of its eye (the creature is moved so its
     * eye is where hers is), its head left out.
     */
    private static boolean owl(AbstractClientPlayer player, OwlClient.Owl owl, double time, float partial, ClientLevel level, Minecraft minecraft,
                               Vec3 camera, Matrix4f matrix, Matrix3f normals, MultiBufferSource.BufferSource buffers) {
        double age = owl.age(time);
        double grown = owl.grown(time);
        if (!(grown > OwlClient.SHOWN)) {
            CREATURES.remove(player.m_20148_());
            HOPS.remove(player.m_20148_());
            ACTING.remove(player.m_20148_());
            return false; // still herself (doubled over before it bursts out of her)
        }
        float bodyYaw = player.f_20884_ + wrap(player.f_20883_ - player.f_20884_) * partial;
        double yaw = Math.toRadians(bodyYaw);
        float headYaw = player.f_20886_ + wrap(player.f_20885_ - player.f_20886_) * partial;
        double lookYaw = Math.toRadians(wrap(headYaw - bodyYaw));
        double lookPitch = Math.toRadians(player.m_146909_());
        PaleClient.PaleView view = PaleClient.get(player);
        RigPose body = KaguneBodyAnimation.posed(player, partial, time);
        if (body == null) {
            // No layer to pose her: as the Owl's move alone poses her, or the Owl alone.
            body = view.move().owl() ? OwlMotions.anyBody(view.move(), view.age(time), view.detail(), view.charge(),
                (float)Math.toRadians(wrap(view.yaw() - bodyYaw))) : null;
            if (body == null) {
                body = new RigPose();
                OwlPose.body(body, owl.on(), age, time);
                body.settle(1.0F);
            }
            OwlPose.neck(body, lookYaw, lookPitch, OwlPose.weight(owl.on(), age));
        }
        // Her look, as vanilla turns her head under the pose (the pose holds only what is put on top of it): the mask turns with it.
        body.headY += (float)lookYaw;
        body.headX += (float)lookPitch;
        Frame frame = new Frame(player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial, player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial,
            player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial, -Math.sin(yaw), Math.cos(yaw), -Math.cos(yaw), -Math.sin(yaw));
        boolean firstPerson = firstPerson(minecraft, player);
        double[] eye = frame.rig(camera.f_82479_, camera.f_82480_, camera.f_82481_);
        // Alive: breathing, swaying, flicking, striding as she walks (each Pale on her own clock, so two are never in step).
        double own = time + (player.m_19879_() % 997) * 7.3;
        double walk = Math.min(1.0, player.f_267362_.m_267711_(partial) * 1.4);
        double stride = player.f_267362_.m_267590_(partial) * 0.6662;
        boolean airborne = !player.m_20096_();
        // 6.2.4: off the ground how long, landed how long ago (a step down no landing: only a fall of a few ticks or more).
        Hops hops = HOPS.computeIfAbsent(player.m_20148_(), ignored -> new Hops());
        if (airborne && !hops.air) {
            hops.left = time;
        } else if (!airborne && hops.air && time - hops.left >= 4.0) {
            hops.landed = time;
        }
        hops.air = airborne;
        double air = airborne ? time - hops.left : 0.0;
        double landed = time - hops.landed;
        double run = player.m_20142_() ? 1.0 : 0.0;
        // 6.2.4: what the creature does through its move (OwlActs), reaching for what its move reaches for.
        OwlActs.Act act = view.move().owl() ? OwlActs.of(view.move(), view.age(time), view.detail(), view.charge(), aim(level, view, frame, grown, partial))
            : null;
        act = ACTING.computeIfAbsent(player.m_20148_(), ignored -> new Acting()).eased(view, act, time);
        // 6.2.0: through the Owl's own moves, its arms are theirs, out of and back into this (OwlArms).
        float moveTurn = PaleMotions.facesLook(view.move()) ? 0.0F : (float)Math.toRadians(wrap(view.yaw() - bodyYaw));
        OwlPose.Swing[] arms = OWL_ARMS.computeIfAbsent(player.m_20148_(), ignored -> new OwlArms()).swing(view.move(), view.startedAt(), view.detail(),
            view.age(time), view.charge(), time, OwlPose.swing(1, own, walk, stride), OwlPose.swing(-1, own, walk, stride), body, moveTurn, act);
        // 6.2.4: torn up out of its back as it grows, unfolding into them (folding up again as it goes).
        OwlPose.Swing right = OwlPose.unfurl(arms[0], 1, grown);
        OwlPose.Swing left = OwlPose.unfurl(arms[1], -1, grown);
        // (Through its own moves it is carried over from her plain stance and her look: the act moves it, not her old choreography.)
        RigPose carried = act != null ? OwlPose.acting(time, lookYaw, lookPitch, moveTurn) : body;
        OwlCreature.Rig rig = OwlCreature.rig(OwlPose.source(carried, walk, stride, airborne, own, grown).with(run, air, landed, act));
        CREATURES.put(player.m_20148_(), rig);
        OwlModel.Look look = OwlPose.look(rig, right, left, own, grown, act);
        // First person: its eye where hers is.
        double[] at = OwlCreature.eye(rig);
        double[] shift = firstPerson ? new double[] {eye[0] - at[0], eye[1] - at[1], eye[2] - at[2]} : new double[] {0.0, 0.0, 0.0};
        CREATURE_SHIFT.put(player.m_20148_(), shift);
        Map<Long, Integer> lights = new HashMap<>();
        VertexConsumer out = buffers.m_6299_(TYPE);
        boolean[] any = {false};
        com.hex.kagune.control.ukaku.UkakuModel.Sink owlSink = (a, b, c, d, n, ca, cb, cc, cd) -> {
            double[] sa = moved(a, shift);
            double[] sb = moved(b, shift);
            double[] sc = moved(c, shift);
            double[] sd = moved(d, shift);
            if (firstPerson && near(eye, EYE_CLEARANCE, sa, sb, sc, sd)) {
                return;
            }
            double[] wa = frame.world(sa, camera);
            double[] wb = frame.world(sb, camera);
            double[] wc = frame.world(sc, camera);
            double[] wd = frame.world(sd, camera);
            double[] wn = frame.turn(n);
            // The eye and the crest's burning facets at full brightness, whatever the light (OwlModel.glow).
            int light = OwlModel.glowing(ca) ? FULL_BRIGHT : floored(light(level, lights, (wa[0] + wc[0]) * 0.5 + camera.f_82479_,
                (wa[1] + wc[1]) * 0.5 + camera.f_82480_, (wa[2] + wc[2]) * 0.5 + camera.f_82481_));
            vertex(out, matrix, normals, wa, wn, ca, light);
            vertex(out, matrix, normals, wb, wn, cb, light);
            vertex(out, matrix, normals, wc, wn, cc, light);
            vertex(out, matrix, normals, wd, wn, cd, light);
            any[0] = true;
        };
        // Its painted parts gathered by texture, each drawn as one batch of its own once everything else of it is drawn.
        java.util.EnumMap<OwlCreature.Tex, java.util.List<Painted>> painted = new java.util.EnumMap<>(OwlCreature.Tex.class);
        OwlCreature.Skin skin = (t, a, b, c, d, uv, n, ca, cb, cc, cd) -> painted.computeIfAbsent(t, ignored -> new java.util.ArrayList<>())
            .add(new Painted(a, b, c, d, uv, n, ca, cb, cc, cd));
        OwlCreature.mesh(owlSink, skin, rig, !firstPerson);
        OwlCreature.halos(skin, rig, eye, !firstPerson);
        OwlCreature.kagune(owlSink, rig, look);
        if (view.move() == PaleMove.OWL_SPIKES && grown > 0.5) {
            // The bone spikes out of its back, each as far out as it is, to its size.
            double age2 = view.age(time);
            for (com.hex.kagune.control.pale.OwlSpikes.Spike spike : com.hex.kagune.control.pale.OwlSpikes.of(view.detail())) {
                double extended = com.hex.kagune.control.pale.OwlSpikes.out(spike, age2, view.charge());
                if (extended <= 0.02) {
                    continue;
                }
                double[] px = spike.px();
                double[] root = OwlCreature.backAt(rig, px);
                double[] way = OwlPose.spike(body, spike, -moveTurn)[1];
                double size = rig.size();
                com.hex.kagune.control.pale.OwlSpikes.mesh((a, b, c, d, n, ca, cb, cc, cd) -> owlSink.quad(grownFrom(a, root, size), grownFrom(b, root, size),
                    grownFrom(c, root, size), grownFrom(d, root, size), n, ca, cb, cc, cd), spike, root, way, new double[] {0.0, 1.0, 0.0}, extended);
            }
        }
        for (java.util.Map.Entry<OwlCreature.Tex, java.util.List<Painted>> batch : painted.entrySet()) {
            OwlCreature.Tex tex = batch.getKey();
            VertexConsumer paint = buffers.m_6299_(OWL_PAINT[tex.ordinal()]);
            for (Painted q : batch.getValue()) {
                // (Each corner lit the way it looks there: a block's face all one way, the cloak's folds rolling.)
                double[][] wn = {frame.turn(q.n()[0]), frame.turn(q.n()[1]), frame.turn(q.n()[2]), frame.turn(q.n()[3])};
                double[] sa = moved(q.a(), shift);
                double[] sb = moved(q.b(), shift);
                double[] sc = moved(q.c(), shift);
                double[] sd = moved(q.d(), shift);
                if (firstPerson && near(eye, EYE_CLEARANCE, sa, sb, sc, sd)) {
                    continue;
                }
                double[] wa = frame.world(sa, camera);
                double[] wb = frame.world(sb, camera);
                double[] wc = frame.world(sc, camera);
                double[] wd = frame.world(sd, camera);
                int light = tex.bright ? FULL_BRIGHT : floored(light(level, lights, (wa[0] + wc[0]) * 0.5 + camera.f_82479_, (wa[1] + wc[1]) * 0.5 + camera.f_82480_,
                    (wa[2] + wc[2]) * 0.5 + camera.f_82481_));
                float[] uv = q.uv();
                paintVertex(paint, matrix, normals, wa, wn[0], q.ca(), uv[0], uv[1], light);
                paintVertex(paint, matrix, normals, wb, wn[1], q.cb(), uv[2], uv[3], light);
                paintVertex(paint, matrix, normals, wc, wn[2], q.cc(), uv[4], uv[5], light);
                paintVertex(paint, matrix, normals, wd, wn[3], q.cd(), uv[6], uv[7], light);
                any[0] = true;
            }
        }
        boolean drew = any[0];
        if (grown > 0.9 && view.move().owl()) {
            // 6.2.0: the blades' trails as they whip through its moves (OwlTrails), from where they are drawn (all four, 6.2.4).
            double[][] parts = new double[8][];
            int i = 0;
            for (OwlModel.Arm limb : new OwlModel.Arm[] {look.right(), look.left(), look.rightInner(), look.leftInner()}) {
                double[] tip = moved(OwlCreature.limbAt(rig, limb, limb.tip()), shift);
                double[] mid = moved(OwlCreature.limbAt(rig, limb, new double[] {limb.elbow()[0] + (limb.tip()[0] - limb.elbow()[0]) * 0.6,
                    limb.elbow()[1] + (limb.tip()[1] - limb.elbow()[1]) * 0.6, limb.elbow()[2] + (limb.tip()[2] - limb.elbow()[2]) * 0.6}), shift);
                parts[i++] = frame.world(tip, Vec3.f_82478_);
                parts[i++] = frame.world(mid, Vec3.f_82478_);
            }
            com.hex.kagune.control.client.fx.OwlTrails.blades(player.m_20148_(), parts, time, firstPerson ? player.m_146892_() : null);
        }
        return drew;
    }

    /**
     * 6.2.4: one of {@code player}'s lances out of the Owl's creature as drawn this frame (moved where it was: first person), or (no
     * creature drawn) out of her own back as posed.
     */
    static double[][] lancePath(AbstractClientPlayer player, RigPose body, int i, OwlLances.Tip tip) {
        OwlCreature.Rig rig = CREATURES.get(player.m_20148_());
        if (rig == null) {
            return OwlLancePose.path(body, i, tip);
        }
        double[][] path = OwlLancePose.path(rig, i, tip);
        double[] shift = CREATURE_SHIFT.getOrDefault(player.m_20148_(), new double[] {0.0, 0.0, 0.0});
        // Only its root moved with the creature (where it hunts is in the world).
        for (int k = 0; k < 3 && k < path.length; k++) {
            path[k] = moved(path[k], shift);
        }
        return path;
    }

    /** 6.2.4: one of the Owl creature's painted quads (which way it looks), held until its texture's batch is drawn. */
    private record Painted(double[] a, double[] b, double[] c, double[] d, float[] uv, double[][] n, int ca, int cb, int cc, int cd) {
    }

    /** 6.2.4: how each of the creature's textures is drawn: as it is, or burning (added, at full brightness). */
    private static final RenderType[] OWL_PAINT = owlPaint();

    private static RenderType[] owlPaint() {
        OwlCreature.Tex[] all = OwlCreature.Tex.values();
        RenderType[] types = new RenderType[all.length];
        for (OwlCreature.Tex t : all) {
            net.minecraft.resources.ResourceLocation id = Objects.requireNonNull(KaguneMod.id("textures/entity/" + t.file + ".png"));
            types[t.ordinal()] = t.glows ? RenderType.m_110488_(id) : RenderType.m_110473_(id);
        }
        return types;
    }

    /** 6.2.4: each player's Owl creature as last drawn (the lances grow out of its back), and how far it was moved (first person). */
    private static final Map<java.util.UUID, OwlCreature.Rig> CREATURES = new HashMap<>();
    private static final Map<java.util.UUID, double[]> CREATURE_SHIFT = new HashMap<>();

    /** {@code p} carried out from {@code from} {@code k} times as far (a spike drawn to the creature's size). */
    static double[] grownFrom(double[] p, double[] from, double k) {
        return new double[] {from[0] + (p[0] - from[0]) * k, from[1] + (p[1] - from[1]) * k, from[2] + (p[2] - from[2]) * k};
    }

    /** 6.2.0: each player's giant arms between the Owl's moves and her idle. */
    private static final Map<java.util.UUID, OwlArms> OWL_ARMS = new HashMap<>();

    /** 6.2.4: one player's last leaving the ground and last landing (PaleClient's clock), and whether she was off it the frame before. */
    private static final class Hops {
        boolean air;
        double left;
        double landed = -OwlCreature.LONG_AGO;
    }

    private static final Map<java.util.UUID, Hops> HOPS = new HashMap<>();

    /**
     * 6.2.4: one player's creature between its moves: one move cut into by the next (its M1's blows one after another), or cut off,
     * goes over into it from where it was drawn over {@link #ACT_INTO} ticks.
     */
    private static final class Acting {
        OwlActs.Act shown;
        OwlActs.Act from;
        PaleMove move = PaleMove.NONE;
        long startedAt = Long.MIN_VALUE;
        double began = Double.NaN;
        double drawnAt = Double.NaN;

        OwlActs.Act eased(PaleClient.PaleView view, OwlActs.Act act, double time) {
            if (view.move() != this.move || view.startedAt() != this.startedAt) {
                boolean recent = Math.abs(time - this.drawnAt) <= 2.0;
                this.from = recent ? this.shown : null;
                this.began = time;
                this.move = view.move();
                this.startedAt = view.startedAt();
            }
            OwlActs.Act out = this.from != null && time - this.began < ACT_INTO
                ? OwlActs.blend(this.from, act, into((time - this.began) / ACT_INTO)) : act;
            this.shown = out;
            this.drawnAt = time;
            return out;
        }
    }

    static final double ACT_INTO = 3.0;

    static double into(double u) {
        double c = Math.max(0.0, Math.min(1.0, u));
        return c * c * (3.0 - 2.0 * c);
    }
    private static final Map<java.util.UUID, Acting> ACTING = new HashMap<>();

    /**
     * 6.2.4: where the creature's great hand reaches through its move (its own pixels, {@link OwlCreature#model}): the middle of what
     * it reaches for, holds or slams, or where it clutched at nothing; null for any other move.
     */
    private static double[] aim(ClientLevel level, PaleClient.PaleView view, Frame frame, double grown, float partial) {
        double[] at;
        boolean seized = view.move() == PaleMove.OWL_GRAB_REACH || view.move() == PaleMove.OWL_GRAB_HOLD || view.move() == PaleMove.OWL_GRAB_SLAM;
        Entity target = seized && view.targetId() != 0 ? level.m_6815_(view.targetId()) : null;
        if (target != null) {
            at = frame.rig(target.f_19790_ + (target.m_20185_() - target.f_19790_) * partial,
                target.f_19791_ + (target.m_20186_() - target.f_19791_) * partial + target.m_20206_() * 0.5,
                target.f_19792_ + (target.m_20189_() - target.f_19792_) * partial);
        } else if (view.move() == PaleMove.OWL_GRAB_MISS) {
            at = frame.rig(view.x(), view.y(), view.z());
        } else {
            return null;
        }
        return OwlCreature.model(at, grown);
    }

    /** 6.2.0: her chest bulked up under the Owl ({@link OwlBulk}), in her own skin and the world's light. */
    private static void chest(RigPose body, Frame frame, Vec3 camera, ClientLevel level, AbstractClientPlayer player, Matrix4f matrix, Matrix3f normals,
                              MultiBufferSource.BufferSource buffers) {
        RenderType skin = RenderType.m_110473_(player.m_108560_());
        Map<Long, Integer> lights = new HashMap<>();
        VertexConsumer out = buffers.m_6299_(skin);
        OwlBulk.chest(body, (a, b, c, d, n, uv, jacket) -> {
            double[] wa = frame.world(a, camera);
            double[] wb = frame.world(b, camera);
            double[] wc = frame.world(c, camera);
            double[] wd = frame.world(d, camera);
            double[] wn = frame.turn(n);
            int light = light(level, lights, (wa[0] + wc[0]) * 0.5 + camera.f_82479_, (wa[1] + wc[1]) * 0.5 + camera.f_82480_,
                (wa[2] + wc[2]) * 0.5 + camera.f_82481_);
            skinVertex(out, matrix, normals, wa, wn, uv[0], uv[1], light);
            skinVertex(out, matrix, normals, wb, wn, uv[2], uv[3], light);
            skinVertex(out, matrix, normals, wc, wn, uv[4], uv[5], light);
            skinVertex(out, matrix, normals, wd, wn, uv[6], uv[7], light);
        });
        buffers.m_109912_(skin);
    }

    /**
     * How far (blocks, up to {@code most}) from {@code from} along {@code aim} before something solid, for {@code who} (never null:
     * a ClipContext with no entity throws in 1.20.1, and the arm would never be drawn).
     */
    static double clear(ClientLevel level, Entity who, double[] from, double[] aim, double most) {
        Vec3 start = new Vec3(from[0], from[1], from[2]);
        Vec3 end = start.m_82520_(aim[0] * most, aim[1] * most, aim[2] * most);
        net.minecraft.world.phys.HitResult hit = level.m_45547_(new net.minecraft.world.level.ClipContext(start, end,
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, who));
        return hit.m_6662_() == net.minecraft.world.phys.HitResult.Type.MISS ? most : Math.max(ArmRules.ARM, hit.m_82450_().m_82554_(start) - 0.4);
    }

    /**
     * 6.2.0: her Watch This tentacles ({@link WatchPose}), out of her back as her body is drawn this frame (or as her move alone
     * poses it), each carrying its own weight; what one strikes or carries is where it is drawn this frame, so the beak is in it.
     */
    private static boolean tentacles(AbstractClientPlayer player, PaleClient.PaleView view, double time, float partial, ClientLevel level, Minecraft minecraft,
                                     Vec3 camera, Matrix4f matrix, Matrix3f normals, MultiBufferSource.BufferSource buffers) {
        float bodyYaw = player.f_20884_ + wrap(player.f_20883_ - player.f_20884_) * partial;
        double yaw = Math.toRadians(bodyYaw);
        RigPose body = KaguneBodyAnimation.drawnNow(player, time);
        if (body == null) {
            body = PaleMotions.anyBody(view.move(), view.age(time), view.detail(), view.charge(), (float)Math.toRadians(wrap(view.yaw() - bodyYaw)));
        }
        Frame frame = new Frame(player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial, player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial,
            player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial, -Math.sin(yaw), Math.cos(yaw), -Math.cos(yaw), -Math.sin(yaw));
        FleshMotion.Space space = new FleshMotion.Space() {
            @Override
            public double[] world(double[] rig) {
                return frame.world(rig, Vec3.f_82478_);
            }

            @Override
            public double[] rig(double[] world) {
                return frame.rig(world[0], world[1], world[2]);
            }
        };
        boolean drew = false;
        for (int side : new int[] {1, -1}) {
            WatchClient.Tentacle tentacle = WatchClient.get(player, side);
            if (tentacle.phase() == WatchRules.Phase.NONE) {
                continue;
            }
            double[] chest = null;
            if ((tentacle.phase() == WatchRules.Phase.STRIKE || tentacle.phase() == WatchRules.Phase.CARRY) && tentacle.targetId() != 0
                && level.m_6815_(tentacle.targetId()) instanceof LivingEntity prey) {
                chest = frame.rig(prey.f_19790_ + (prey.m_20185_() - prey.f_19790_) * partial,
                    prey.f_19791_ + (prey.m_20186_() - prey.f_19791_) * partial + prey.m_20206_() * WatchRules.CHEST,
                    prey.f_19792_ + (prey.m_20189_() - prey.f_19792_) * partial);
            }
            double[] from = tentacle.phase() == WatchRules.Phase.RECOVER || tentacle.phase() == WatchRules.Phase.MISS
                ? frame.rig(tentacle.x(), tentacle.y(), tentacle.z()) : null;
            WatchPose.Tip tip = WatchPose.tip(side, tentacle.phase(), tentacle.age(time), tentacle.lunge(), chest, from, time);
            PaleFleshModel.Pose pose = FleshMotion.carried(player.m_19879_(), side > 0 ? 1 : 2, 0L, FAMILY_WATCH,
                WatchPose.pose(body, side, tip, time, player.m_19879_() * 3 + side), space, time, FleshPhysics.TENTACLE.held(tip.hold()), body);
            drew |= draw(pose, frame, camera, level, minecraft, player, matrix, normals, buffers, NO_SHIFT, EYE_CLEARANCE);
        }
        return drew;
    }

    /**
     * 6.2.0: the Owl's Watch This lances ({@link OwlLances}, {@link OwlLancePose}) out of her back as her body is drawn this frame (or
     * as the Owl alone poses it); what one dives on, pins or hoists is where it is drawn this frame, so the spear is through it.
     */
    private static boolean lances(AbstractClientPlayer player, OwlClient.Owl owl, PaleClient.PaleView view, double time, float partial, ClientLevel level,
                                  Minecraft minecraft, Vec3 camera, Matrix4f matrix, Matrix3f normals, MultiBufferSource.BufferSource buffers) {
        float bodyYaw = player.f_20884_ + wrap(player.f_20883_ - player.f_20884_) * partial;
        double yaw = Math.toRadians(bodyYaw);
        RigPose body = KaguneBodyAnimation.drawnNow(player, time);
        if (body == null) {
            body = view.move().owl() ? OwlMotions.anyBody(view.move(), view.age(time), view.detail(), view.charge(),
                (float)Math.toRadians(wrap(view.yaw() - bodyYaw))) : null;
            if (body == null) {
                body = new RigPose();
                if (owl != null) {
                    OwlPose.body(body, owl.on(), owl.age(time), time);
                }
                body.settle(1.0F);
            }
        }
        Frame frame = new Frame(player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial, player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial,
            player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial, -Math.sin(yaw), Math.cos(yaw), -Math.cos(yaw), -Math.sin(yaw));
        boolean firstPerson = firstPerson(minecraft, player);
        double[] eye = frame.rig(camera.f_82479_, camera.f_82480_, camera.f_82481_);
        Map<Long, Integer> lights = new HashMap<>();
        VertexConsumer out = buffers.m_6299_(TYPE);
        boolean[] any = {false};
        // Each spear's point and the middle of it, for the trails it leaves as it dives and slams (OwlTrails).
        double[][] parts = new double[OwlLances.COUNT * 2][];
        for (int i = 0; i < OwlLances.COUNT; i++) {
            OwlLanceClient.Lance lance = OwlLanceClient.get(player, i);
            if (lance.phase() == OwlLances.Phase.NONE) {
                // In her: where it would tear out (no trail from anywhere else when it does).
                parts[i * 2] = frame.world(lancePath(player, body, i, OwlLances.crown(i, time))[0], Vec3.f_82478_);
                parts[i * 2 + 1] = parts[i * 2];
                continue;
            }
            double[] chest = null;
            double chestUp = 1.0;
            if ((lance.phase() == OwlLances.Phase.STRIKE || lance.phase() == OwlLances.Phase.PIN || lance.phase() == OwlLances.Phase.HOIST)
                && lance.targetId() != 0 && level.m_6815_(lance.targetId()) instanceof LivingEntity prey) {
                chestUp = prey.m_20206_() * OwlLances.CHEST;
                chest = frame.rig(prey.f_19790_ + (prey.m_20185_() - prey.f_19790_) * partial, prey.f_19791_ + (prey.m_20186_() - prey.f_19791_) * partial + chestUp,
                    prey.f_19792_ + (prey.m_20189_() - prey.f_19792_) * partial);
            }
            double[] from = lance.phase() == OwlLances.Phase.RECOVER || lance.phase() == OwlLances.Phase.MISS ? frame.rig(lance.x(), lance.y(), lance.z()) : null;
            OwlLances.Tip tip = OwlLances.tip(i, lance.phase(), lance.age(time), lance.dive(), chest, chestUp, from, time + player.m_19879_() * 3.1);
            double[] point = OwlLances.point(tip);
            parts[i * 2] = frame.world(point, Vec3.f_82478_);
            parts[i * 2 + 1] = frame.world(new double[] {(point[0] + tip.knot()[0]) * 0.5, (point[1] + tip.knot()[1]) * 0.5, (point[2] + tip.knot()[2]) * 0.5},
                Vec3.f_82478_);
            OwlLances.mesh((a, b, c, d, n, ca, cb, cc, cd) -> {
                if (firstPerson && near(eye, EYE_CLEARANCE, a, b, c, d)) {
                    return;
                }
                double[] wa = frame.world(a, camera);
                double[] wb = frame.world(b, camera);
                double[] wc = frame.world(c, camera);
                double[] wd = frame.world(d, camera);
                double[] wn = frame.turn(n);
                int light = light(level, lights, (wa[0] + wc[0]) * 0.5 + camera.f_82479_, (wa[1] + wc[1]) * 0.5 + camera.f_82480_,
                    (wa[2] + wc[2]) * 0.5 + camera.f_82481_);
                vertex(out, matrix, normals, wa, wn, ca, light);
                vertex(out, matrix, normals, wb, wn, cb, light);
                vertex(out, matrix, normals, wc, wn, cc, light);
                vertex(out, matrix, normals, wd, wn, cd, light);
                any[0] = true;
            }, lancePath(player, body, i, tip), tip.dir(), tip.grown(), player.m_19879_() * 7 + i, time);
        }
        // (Their own trails, apart from the giant blades': keyed off her id turned.)
        java.util.UUID id = player.m_20148_();
        com.hex.kagune.control.client.fx.OwlTrails.blades(new java.util.UUID(~id.getMostSignificantBits(), id.getLeastSignificantBits() ^ 0x4C414E4345L), parts, time,
            firstPerson ? player.m_146892_() : null);
        return any[0];
    }

    /** Whether {@code player} is this client's, seen in first person. */
    static boolean firstPerson(Minecraft minecraft, AbstractClientPlayer player) {
        return player == minecraft.f_91074_ && minecraft.f_91066_.m_92176_().m_90612_();
    }

    /**
     * One player's flesh into the world's light, moved by {@code shift} (rig space, blocks), its quads within {@code clearance} of
     * the eyes left out in first person; false when there is none.
     */
    private static boolean draw(PaleFleshModel.Pose pose, Frame frame, Vec3 camera, ClientLevel level, Minecraft minecraft, AbstractClientPlayer player,
                                Matrix4f matrix, Matrix3f normals, MultiBufferSource.BufferSource buffers, double[] shift, double clearance) {
        if (pose == null) {
            return false;
        }
        boolean firstPerson = firstPerson(minecraft, player);
        double[] eye = frame.rig(camera.f_82479_, camera.f_82480_, camera.f_82481_);
        Map<Long, Integer> lights = new HashMap<>();
        VertexConsumer out = buffers.m_6299_(TYPE);
        PaleFleshModel.mesh((ra, rb, rc, rd, n, ca, cb, cc, cd) -> {
            double[] a = moved(ra, shift);
            double[] b = moved(rb, shift);
            double[] c = moved(rc, shift);
            double[] d = moved(rd, shift);
            if (firstPerson && near(eye, clearance, a, b, c, d)) {
                return;
            }
            double[] wa = frame.world(a, camera);
            double[] wb = frame.world(b, camera);
            double[] wc = frame.world(c, camera);
            double[] wd = frame.world(d, camera);
            double[] wn = frame.turn(n);
            int light = light(level, lights, (wa[0] + wc[0]) * 0.5 + camera.f_82479_, (wa[1] + wc[1]) * 0.5 + camera.f_82480_,
                (wa[2] + wc[2]) * 0.5 + camera.f_82481_);
            vertex(out, matrix, normals, wa, wn, ca, light);
            vertex(out, matrix, normals, wb, wn, cb, light);
            vertex(out, matrix, normals, wc, wn, cc, light);
            vertex(out, matrix, normals, wd, wn, cd, light);
        }, pose);
        return true;
    }

    /** 6.2.0: her own right leg in first person ({@link PaleOwnLeg}), in her skin and the world's light, moved by {@code shift}. */
    private static void leg(RigPose body, double[] shift, Frame frame, Vec3 camera, ClientLevel level, AbstractClientPlayer player, Matrix4f matrix,
                            Matrix3f normals, MultiBufferSource.BufferSource buffers) {
        limb(sink -> PaleOwnLeg.mesh(body, shift, sink), frame, camera, level, player, matrix, normals, buffers);
    }

    /** 6.2.0: whether her own arms were drawn in first person this frame (ArmHider keeps vanilla's hands away then). */
    private static boolean ownArms;

    /** 6.2.0: true while her own arms are drawn in first person: vanilla's hands are not. */
    public static boolean ownArmsShown() {
        return ownArms;
    }

    /**
     * 6.2.0: both her arms in first person, on her body as third person poses it this frame, seen from where her head has gone; false
     * when there was nothing to draw them on.
     */
    private static boolean arms(AbstractClientPlayer player, float partial, double time, Vec3 camera, ClientLevel level, Matrix4f matrix,
                                Matrix3f normals, MultiBufferSource.BufferSource buffers) {
        RigPose body = KaguneBodyAnimation.ownArms(player, partial, time);
        if (body == null) {
            return false;
        }
        float bodyYaw = player.f_20884_ + wrap(player.f_20883_ - player.f_20884_) * partial;
        double yaw = Math.toRadians(bodyYaw);
        Frame frame = new Frame(player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial, player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial,
            player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial, -Math.sin(yaw), Math.cos(yaw), -Math.cos(yaw), -Math.sin(yaw));
        boolean slim = "slim".equals(player.m_108564_());
        double[] shift = PaleOwnLeg.shift(body);
        limb(sink -> PaleOwnArm.both(body, shift, slim, sink), frame, camera, level, player, matrix, normals, buffers);
        return true;
    }

    private static final RenderType WAR_SHAPES = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final RenderType WAR_LIGHT = RenderType.m_110488_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));


    /**
     * 1.0.16: Peace's blades in first person while a move of its has them (1.0.18: six, on their arms): from the model third person
     * draws, on the body as it is posed, seen from his eyes (what is right against the eyes left out).
     */
    private static void peaceBlades(AbstractClientPlayer player, float partial, double time, Vec3 camera, ClientLevel level, Matrix4f matrix,
                                    Matrix3f normals, MultiBufferSource.BufferSource buffers) {
        com.hex.kagune.control.client.KingClient.View king = com.hex.kagune.control.client.KingClient.get(player);
        if (king == null || king.form() != com.hex.kagune.control.king.KingForm.PEACE || !king.on() || king.grow(time) < 0.6) {
            return;
        }
        // 1.0.26: always, not only through its moves: at rest they frame him, reaching ahead of him now and then (and at work, they
        // are what he sees).
        com.hex.kagune.control.king.PeaceMotion.State state = KingLayer.peaceState(player, partial, time);
        state = KingLayer.alive(player, partial, time, state, time); // 1.0.25: its arms alive, as third person draws them
        RigPose body = KaguneBodyAnimation.current(player, partial);
        if (body == null) {
            // 1.0.36: nothing posing him this frame: his stance and the move's pose (never nothing: the M1 not showing at all).
            body = standing(king, time, true);
        }
        float bodyYaw = player.f_20884_ + wrap(player.f_20883_ - player.f_20884_) * partial;
        double yaw = Math.toRadians(bodyYaw);
        Frame frame = new Frame(player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial, player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial,
            player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial, -Math.sin(yaw), Math.cos(yaw), -Math.cos(yaw), -Math.sin(yaw));
        double[] shift = PaleOwnLeg.shift(body);
        double[] eyeAt = frame.rig(camera.f_82479_, camera.f_82480_, camera.f_82481_);
        List<double[][]> lit = new ArrayList<>();
        Map<Long, Integer> lights = new HashMap<>();
        // 1.0.27: the torso is carried rigidly, so one transform for every corner (not the whole body's chain per corner, per frame).
        double[] torso = KingAfterimages.carry(body, "torso");
        com.hex.kagune.control.king.PeaceModel.build((a, b, c, d, n, ca, cb, cc, cd) -> {
            double[][] corners = new double[4][];
            double[][] local = {a, b, c, d};
            for (int k = 0; k < 4; k++) {
                double[] r = affine(torso, local[k]);
                corners[k] = new double[] {r[0] + shift[0], r[1] + shift[1], r[2] + shift[2]};
            }
            if (!finite(corners) || near(eyeAt, 0.45, corners)) {
                return; // (1.0.36: nothing not a number ever reaches the buffer)
            }
            double[][] q = new double[6][];
            for (int k = 0; k < 4; k++) {
                q[k] = frame.world(corners[k], camera);
            }
            q[4] = frame.turn(turned(torso, n));
            q[5] = new double[] {ca, cb, cc, cd};
            lit.add(q);
        }, com.hex.kagune.control.king.WarModel.Part.TORSO, time, king.grow(time), state, true);
        VertexConsumer shapes = buffers.m_6299_(WAR_SHAPES);
        for (double[][] q : lit) {
            int light = light(level, lights, (q[0][0] + q[2][0]) * 0.5 + camera.f_82479_, (q[0][1] + q[2][1]) * 0.5 + camera.f_82480_,
                (q[0][2] + q[2][2]) * 0.5 + camera.f_82481_);
            for (int k = 0; k < 4; k++) {
                warVertex(shapes, matrix, normals, q[k], q[4], (int)q[5][k], light);
            }
        }
        buffers.m_109912_(WAR_SHAPES);
    }

    /**
     * 1.0.26: War's rinkaku in first person, always: from the model third person draws ({@link com.hex.kagune.control.king.WarModel#rinkaku}),
     * on the body as it is posed, seen from his eyes (what is right against the eyes left out), their glow burning as it does outside.
     */
    private static void warRinkaku(AbstractClientPlayer player, float partial, double time, Vec3 camera, ClientLevel level, Matrix4f matrix,
                                   Matrix3f normals, MultiBufferSource.BufferSource buffers) {
        com.hex.kagune.control.client.KingClient.View king = com.hex.kagune.control.client.KingClient.get(player);
        if (king == null || king.form() != com.hex.kagune.control.king.KingForm.WAR || !king.on() || king.grow(time) < 0.6) {
            return;
        }
        if (king.moveAt(time) == com.hex.kagune.control.king.KingMove.WYRM && time - king.moveAt() >= com.hex.kagune.control.king.WarRules.WYRM_PRELUDE) {
            return; // the small Dragon has his body (1.0.33: once it is out of him)
        }
        RigPose body = KaguneBodyAnimation.current(player, partial);
        if (body == null) {
            body = standing(king, time, false); // 1.0.36: (as Peace's)
        }
        // 1.2.4: square to the look, as the camera is (his body lags it as he turns: the rinkaku struck off to the side of the view),
        // the body's lean taken off at the neck, and nothing near the eyes: only what reaches out ahead of him.
        Frame frame = looking(player, partial);
        double[] shift = FirstPersonArms.neckShift(body);
        double[] eyeAt = frame.rig(camera.f_82479_, camera.f_82480_, camera.f_82481_);
        List<double[][]> lit = new ArrayList<>();
        List<double[][]> glow = new ArrayList<>();
        Map<Long, Integer> lights = new HashMap<>();
        double[] torso = KingAfterimages.carry(body, "torso"); // 1.0.27: one transform for every corner
        com.hex.kagune.control.king.WarModel.rinkaku((a, b, c, d, n, ca, cb, cc, cd) -> {
            double[][] corners = new double[4][];
            double[][] local = {a, b, c, d};
            for (int k = 0; k < 4; k++) {
                double[] r = affine(torso, local[k]);
                corners[k] = new double[] {r[0] + shift[0], r[1] + shift[1], r[2] + shift[2]};
            }
            if (!finite(corners) || !FirstPersonArms.clear(eyeAt, FirstPersonArms.RINKAKU_CLEAR, corners)) {
                return; // (1.0.36: nothing not a number ever reaches the buffer)
            }
            boolean burns = n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0;
            double[][] q = new double[6][];
            for (int k = 0; k < 4; k++) {
                q[k] = frame.world(corners[k], camera);
            }
            q[4] = frame.turn(turned(torso, n));
            q[5] = new double[] {ca, cb, cc, cd};
            (burns ? glow : lit).add(q);
        }, time, king.grow(time), KingLayer.warMotion(player, king, time));
        VertexConsumer shapes = buffers.m_6299_(WAR_SHAPES);
        for (double[][] q : lit) {
            int light = light(level, lights, (q[0][0] + q[2][0]) * 0.5 + camera.f_82479_, (q[0][1] + q[2][1]) * 0.5 + camera.f_82480_,
                (q[0][2] + q[2][2]) * 0.5 + camera.f_82481_);
            for (int k = 0; k < 4; k++) {
                warVertex(shapes, matrix, normals, q[k], q[4], (int)q[5][k], light);
            }
        }
        buffers.m_109912_(WAR_SHAPES);
        VertexConsumer burning = buffers.m_6299_(WAR_LIGHT);
        for (double[][] q : glow) {
            for (int k = 0; k < 4; k++) {
                warVertex(burning, matrix, normals, q[k], q[4], premultiplied((int)q[5][k]), 0xF000F0);
            }
            for (int k = 3; k >= 0; k--) {
                warVertex(burning, matrix, normals, q[k], q[4], premultiplied((int)q[5][k]), 0xF000F0);
            }
        }
        buffers.m_109912_(WAR_LIGHT);
    }

    /** 1.2.4: {@code player}'s rig frame squared to the camera's look (first person), at his feet. */
    private static Frame looking(AbstractClientPlayer player, float partial) {
        double yaw = Math.toRadians(player.f_19859_ + (player.m_146908_() - player.f_19859_) * partial);
        return new Frame(player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial, player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial,
            player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial, -Math.sin(yaw), Math.cos(yaw), -Math.cos(yaw), -Math.sin(yaw));
    }

    /** 1.0.27: a part-pixel point carried by {@code a} ({@link KingAfterimages#carry}: A p + b), in rig space. */
    private static double[] affine(double[] a, double[] p) {
        return new double[] {a[0] * p[0] + a[1] * p[1] + a[2] * p[2] + a[9], a[3] * p[0] + a[4] * p[1] + a[5] * p[2] + a[10],
            a[6] * p[0] + a[7] * p[1] + a[8] * p[2] + a[11]};
    }

    /** 1.0.27: a direction turned by {@code a}'s turn alone. */
    private static double[] turned(double[] a, double[] n) {
        return new double[] {a[0] * n[0] + a[1] * n[1] + a[2] * n[2], a[3] * n[0] + a[4] * n[1] + a[5] * n[2], a[6] * n[0] + a[7] * n[1] + a[8] * n[2]};
    }

    /** One of War's vertices: its colour with its own alpha. */
    private static void warVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals, double[] p, double[] n, int argb, int light) {
        consumer.m_252986_(matrix, (float)p[0], (float)p[1], (float)p[2])
            .m_6122_((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF)
            .m_7421_(0.5F, 0.5F)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
            .m_5752_();
    }

    /** A glow's colour scaled by its own alpha (the glow adds: the alpha is how bright). */
    private static int premultiplied(int argb) {
        double a = ((argb >>> 24) & 0xFF) / 255.0;
        int r = (int)Math.round(((argb >> 16) & 0xFF) * a);
        int g = (int)Math.round(((argb >> 8) & 0xFF) * a);
        int b = (int)Math.round((argb & 0xFF) * a);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** 6.2.0: her own right arm in first person (the Mutated Arm's), in her skin. */
    private static void ownArm(RigPose body, double[] shift, Frame frame, Vec3 camera, ClientLevel level, AbstractClientPlayer player, Matrix4f matrix,
                               Matrix3f normals, MultiBufferSource.BufferSource buffers) {
        boolean slim = "slim".equals(player.m_108564_());
        limb(sink -> PaleOwnArm.mesh(body, shift, slim, sink), frame, camera, level, player, matrix, normals, buffers);
    }

    /** A limb of hers in her skin ({@code mesh} gives its faces), lit where it is. */
    private static void limb(java.util.function.Consumer<PaleOwnLeg.Sink> mesh, Frame frame, Vec3 camera, ClientLevel level, AbstractClientPlayer player,
                             Matrix4f matrix, Matrix3f normals, MultiBufferSource.BufferSource buffers) {
        RenderType skin = RenderType.m_110473_(player.m_108560_());
        double[] eye = frame.rig(camera.f_82479_, camera.f_82480_, camera.f_82481_);
        Map<Long, Integer> lights = new HashMap<>();
        VertexConsumer out = buffers.m_6299_(skin);
        mesh.accept((a, b, c, d, n, uv, trousers) -> {
            if (near(eye, OWN_LEG_CLEARANCE, a, b, c, d)) {
                return;
            }
            double[] wa = frame.world(a, camera);
            double[] wb = frame.world(b, camera);
            double[] wc = frame.world(c, camera);
            double[] wd = frame.world(d, camera);
            double[] wn = frame.turn(n);
            int light = light(level, lights, (wa[0] + wc[0]) * 0.5 + camera.f_82479_, (wa[1] + wc[1]) * 0.5 + camera.f_82480_,
                (wa[2] + wc[2]) * 0.5 + camera.f_82481_);
            skinVertex(out, matrix, normals, wa, wn, uv[0], uv[1], light);
            skinVertex(out, matrix, normals, wb, wn, uv[2], uv[3], light);
            skinVertex(out, matrix, normals, wc, wn, uv[4], uv[5], light);
            skinVertex(out, matrix, normals, wd, wn, uv[6], uv[7], light);
        });
        buffers.m_109912_(skin);
    }

    static double[] moved(double[] p, double[] by) {
        return by == NO_SHIFT ? p : new double[] {p[0] + by[0], p[1] + by[1], p[2] + by[2]};
    }

    /** Her feet and facing: rig space (x her right, y up, z forward) to the world and back. */
    record Frame(double x, double y, double z, double fx, double fz, double rx, double rz) {
        double[] world(double[] rig, Vec3 camera) {
            return new double[] {this.x + this.rx * rig[0] + this.fx * rig[2] - camera.f_82479_, this.y + rig[1] - camera.f_82480_,
                this.z + this.rz * rig[0] + this.fz * rig[2] - camera.f_82481_};
        }

        double[] turn(double[] rig) {
            return new double[] {this.rx * rig[0] + this.fx * rig[2], rig[1], this.rz * rig[0] + this.fz * rig[2]};
        }

        /** A direction in the world in rig space. */
        double[] turnBack(double[] world) {
            return new double[] {world[0] * this.rx + world[2] * this.rz, world[1], world[0] * this.fx + world[2] * this.fz};
        }

        double[] rig(double wx, double wy, double wz) {
            double dx = wx - this.x;
            double dz = wz - this.z;
            return new double[] {dx * this.rx + dz * this.rz, wy - this.y, dx * this.fx + dz * this.fz};
        }
    }

    /** True when every corner of a quad is within {@code clearance} of the eye (first person: it would fill the view). */
    /** 1.0.36: every corner a real place (no NaN or infinity). */
    static boolean finite(double[][] corners) {
        for (double[] c : corners) {
            if (!Double.isFinite(c[0]) || !Double.isFinite(c[1]) || !Double.isFinite(c[2])) {
                return false;
            }
        }
        return true;
    }

    /** 1.0.36: a final kakuja's body as nothing else poses it: its stance, and its move's pose over it. */
    private static RigPose standing(com.hex.kagune.control.client.KingClient.View king, double time, boolean peace) {
        RigPose pose = new RigPose();
        com.hex.kagune.control.king.KingMove move = king.moveAt(time);
        if (peace) {
            com.hex.kagune.control.client.animation.WarPose.peace(pose, time, 1.0F);
            if (move != com.hex.kagune.control.king.KingMove.NONE) {
                com.hex.kagune.control.client.animation.PeacePose.act(pose, move, time - king.moveAt(), king.level(), 1.0F);
            }
        } else {
            com.hex.kagune.control.client.animation.WarPose.apply(pose, time, 7, 0.5F, 0.0F);
            if (move != com.hex.kagune.control.king.KingMove.NONE) {
                com.hex.kagune.control.client.animation.WarPose.act(pose, move, time - king.moveAt(), king.level(), 1.0F);
            }
        }
        return pose;
    }

    static boolean near(double[] eye, double clearance, double[]... corners) {
        for (double[] c : corners) {
            double dx = c[0] - eye[0];
            double dy = c[1] - eye[1];
            double dz = c[2] - eye[2];
            if (dx * dx + dy * dy + dz * dz > clearance * clearance) {
                return false;
            }
        }
        return true;
    }

    /** The world's light at a point, looked up once a block a frame. */
    /**
     * 6.2.4: the Owl creature lit never darker than its own glow lights it (block light {@link #OWL_LIGHT} at least): as the sheet has
     * it in the dark, its bone white and its red flesh bright under its burning crest.
     */
    static final int OWL_LIGHT = 13;

    static int floored(int packed) {
        int block = (packed >> 4) & 0xF;
        int sky = (packed >> 20) & 0xF;
        return (sky << 20) | (Math.max(block, OWL_LIGHT) << 4);
    }

    static int light(ClientLevel level, Map<Long, Integer> cache, double x, double y, double z) {
        int bx = (int)Math.floor(x);
        int by = (int)Math.floor(y);
        int bz = (int)Math.floor(z);
        long key = ((long)(bx & 0x3FFFFFF) << 38) | ((long)(bz & 0x3FFFFFF) << 12) | (by & 0xFFF);
        Integer known = cache.get(key);
        if (known != null) {
            return known;
        }
        int light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(x, y, z));
        cache.put(key, light);
        return light;
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals, double[] p, double[] n, int colour, int light) {
        consumer.m_252986_(matrix, (float)p[0], (float)p[1], (float)p[2])
            .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, 255)
            .m_7421_(0.5F, 0.5F)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
            .m_5752_();
    }

    private static void paintVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals, double[] p, double[] n, int tint, float u, float v, int light) {
        consumer.m_252986_(matrix, (float)p[0], (float)p[1], (float)p[2])
            .m_6122_((tint >> 16) & 0xFF, (tint >> 8) & 0xFF, tint & 0xFF, 255)
            .m_7421_(u, v)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
            .m_5752_();
    }

    private static void skinVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals, double[] p, double[] n, float u, float v, int light) {
        consumer.m_252986_(matrix, (float)p[0], (float)p[1], (float)p[2])
            .m_6122_(255, 255, 255, 255)
            .m_7421_(u, v)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
            .m_5752_();
    }

    private static float wrap(float degrees) {
        return degrees - 360.0F * (float)Math.floor((degrees + 180.0F) / 360.0F);
    }
}
