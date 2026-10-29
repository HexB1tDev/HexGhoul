package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.ayato.CentipedeMotion;
import com.hex.kagune.control.ayato.CentipedeTail;
import com.hex.kagune.control.ayato.KakujaModel;
import com.hex.kagune.control.client.EtoClient;
import com.hex.kagune.control.client.FurutaClient;
import com.hex.kagune.control.furuta.FurutaMesh;
import com.hex.kagune.control.furuta.FurutaModel;
import com.hex.kagune.control.furuta.FurutaMotion;
import com.hex.kagune.control.furuta.FurutaMove;
import com.hex.kagune.control.furuta.FurutaRig;
import com.hex.kagune.control.furuta.FurutaRules;
import com.hex.kagune.control.furuta.FurutaStage;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Furuta DNA's kagune in the world (7.0.0): the base stage's limbs ({@link FurutaModel}: the transplant's tails, the centipedes, the
 * thorns, the many-eyed ribbons) and the kakuja's two colossi, posed by {@link FurutaRig} and followed by
 * {@link FurutaMotion}'s springs on the player's own clock. Each limb grows out of his lower back as the kagune comes out (a
 * swelling under the skin first, then the limb drawn out along its own curve and unfolding), and is drawn back the same way; a
 * kakuja transformation draws the base stage back into him before the colossi tear out and unfold, and the reverse on the way back.
 *
 * <p>Same render type and vertex layout as the other kagune renderers (a white texel tinted per vertex, translucent), lit by the
 * world except the glowing RC (a quad lit from straight above). In first person nothing within a block of the eyes is drawn.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class FurutaRenderer {
    private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final double MAX_DISTANCE_SQR = 96.0 * 96.0;
    private static final double DETAIL_DISTANCE_SQR = 24.0 * 24.0;
    /** In first person, nothing of a limb this close to the eyes. */
    private static final double FIRST_PERSON_CLEAR = 1.15;
    private static final Map<UUID, Limbs> LIMBS = new HashMap<>();

    /** One player's limbs: the base stage's and the colossi, each following on its own. */
    private static final class Limbs {
        final FurutaMotion.Limb[] base = new FurutaMotion.Limb[5];
        final FurutaMotion.Limb[] colossi = new FurutaMotion.Limb[2];
        FurutaStage stage;
        long seen;
        /** The prey its mouths are after (an entity id, 0 none) and the game tick it was last looked for. */
        int prey;
        long preySought = Long.MIN_VALUE;
    }

    private FurutaRenderer() {
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
        double clock = EtoClient.clock(partial);
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        PoseStack stack = event.getPoseStack();
        Matrix4f matrix = stack.m_85850_().m_252922_();
        Matrix3f normals = stack.m_85850_().m_252943_();
        MultiBufferSource.BufferSource buffers = minecraft.m_91269_().m_110104_();
        VertexConsumer[] consumer = {null};
        for (AbstractClientPlayer player : level.m_6907_()) {
            if (!FurutaClient.isFuruta(player) || !player.m_6084_() || player.m_20145_() || player.m_5803_() || player.m_21255_()
                || com.hex.kagune.control.client.ReviveClient.apart(player, partial)) {
                continue;
            }
            float deploy = FurutaClient.deploy(player, partial);
            if (deploy <= 0.005F) {
                LIMBS.remove(player.m_20148_());
                continue;
            }
            double distance = player.m_20280_(self);
            if (distance > MAX_DISTANCE_SQR) {
                continue;
            }
            boolean firstPerson = player == self && minecraft.f_91066_.m_92176_().m_90612_();
            boolean fine = !firstPerson && distance <= DETAIL_DISTANCE_SQR;
            FurutaClient.View view = FurutaClient.get(player);
            Limbs limbs = LIMBS.computeIfAbsent(player.m_20148_(), id -> new Limbs());
            limbs.seen = level.m_46467_();
            if (limbs.stage != view.stage()) {
                // A new base stage grows out afresh from his back.
                java.util.Arrays.fill(limbs.base, null);
                limbs.stage = view.stage();
            }
            Frame frame = Frame.of(player, partial);
            Vec3 eye = player.m_146892_();
            double[] eyeRig = firstPerson ? new double[] {eye.f_82479_ - camera.f_82479_, eye.f_82480_ - camera.f_82480_, eye.f_82481_ - camera.f_82481_} : null;
            FurutaRig.Body body = body(player, partial, clock, frame, prey(level, player, limbs, partial, frame));
            // His body as drawn (hunched, lunging): the limbs' roots ride it.
            com.hex.kagune.control.client.animation.RigPose posed = com.hex.kagune.control.client.animation.KaguneBodyAnimation.body(player,
                com.hex.kagune.control.client.ControlClient.worldTime(partial));
            double shownBase = deploy * FurutaClient.baseShown(view, clock);
            double shownColossi = deploy * FurutaClient.colossi(view, clock);
            boolean kakujaMoves = view.move().stage() == FurutaStage.KAKUJA || view.kakuja() && view.move() != FurutaMove.NONE
                && view.move().stage() == FurutaStage.TRANSPLANT && shownColossi > 0.5;
            int mastery = com.hex.kagune.client.KaguneClientState.get(player).mastery();
            VertexConsumer out = consumer[0] == null ? (consumer[0] = buffers.m_6299_(TYPE)) : consumer[0];
            if (shownBase > 0.01) {
                drawSet(out, matrix, normals, level, player, view, view.stage(), limbs.base, shownBase, kakujaMoves ? null : view, body, frame,
                    camera, clock, partial, mastery, fine, eyeRig, posed);
            } else {
                java.util.Arrays.fill(limbs.base, null);
            }
            if (shownColossi > 0.01) {
                // 7.0.2: the kakuja's jobs (its recovery, its performance) are the colossi's too.
                boolean colossiJob = view.kakuja() && view.job().stage() == FurutaStage.KAKUJA;
                drawSet(out, matrix, normals, level, player, view, FurutaStage.KAKUJA, limbs.colossi, shownColossi, kakujaMoves || colossiJob ? view : null, body,
                    frame, camera, clock, partial, mastery, fine, eyeRig, posed);
            } else {
                java.util.Arrays.fill(limbs.colossi, null);
            }
        }
        // 7.0.2: the rinkaku ghoul NPCs in view: the transplant's tails, as a player's, at their tier's size.
        for (com.hex.kagune.control.npc.GhoulNpc npc : GhoulNpcRenderer.drawn()) {
            com.hex.kagune.control.client.GhoulNpcClient.View seen = com.hex.kagune.control.client.GhoulNpcClient.get(npc.m_19879_());
            if (seen == null || seen.identity().kind() != com.hex.kagune.control.npc.NpcIdentity.Kind.RINKAKU || !npc.m_6084_() || npc.m_20145_()) {
                continue;
            }
            float deploy = com.hex.kagune.control.client.GhoulNpcClient.deploy(seen, clock);
            if (deploy <= 0.005F) {
                LIMBS.remove(npc.m_20148_());
                continue;
            }
            double distance = npc.m_20280_(self);
            if (distance > MAX_DISTANCE_SQR) {
                continue;
            }
            FurutaClient.View view = com.hex.kagune.control.client.GhoulNpcClient.furuta(seen);
            Limbs limbs = LIMBS.computeIfAbsent(npc.m_20148_(), id -> new Limbs());
            limbs.seen = level.m_46467_();
            limbs.stage = FurutaStage.TRANSPLANT;
            Frame frame = Frame.of(npc, partial);
            FurutaRig.Body body = body(npc, partial, clock, frame, prey(level, npc, limbs, partial, frame));
            VertexConsumer out = consumer[0] == null ? (consumer[0] = buffers.m_6299_(TYPE)) : consumer[0];
            // 1.0.41: its tails turned over with it as it flips (carried on its torso, as a player's are).
            drawSet(out, matrix, normals, level, npc, view, FurutaStage.TRANSPLANT, limbs.base, deploy, view, body, frame, camera, clock, partial,
                seen.identity().lookMastery(), distance <= DETAIL_DISTANCE_SQR, null,
                com.hex.kagune.control.client.GhoulNpcClient.flip(seen, clock, npc.m_20096_()));
        }
        if (consumer[0] != null) {
            buffers.m_109912_(TYPE);
        }
        if (level.m_46467_() % 40L == 0L) {
            long now = level.m_46467_();
            LIMBS.values().removeIf(limbs -> now - limbs.seen > 100L);
        }
    }

    /** Every limb of {@code stage}, grown {@code grown} of the way out, making {@code moving}'s move (null: at rest). */
    private static void drawSet(VertexConsumer out, Matrix4f matrix, Matrix3f normals, ClientLevel level, LivingEntity player,
                                FurutaClient.View view, FurutaStage stage, FurutaMotion.Limb[] motion, double grown, FurutaClient.View moving,
                                FurutaRig.Body body, Frame frame, Vec3 camera, double clock, float partial, int mastery, boolean fine,
                                double[] eyeRig, com.hex.kagune.control.client.animation.RigPose posed) {
        int n = Math.min(motion.length, stage.limbs(mastery));
        double length = stage.length(mastery);
        double width = stage.width(mastery);
        FurutaMove move = moving == null ? FurutaMove.NONE : moving.move();
        double age = moving == null ? 0.0 : moving.age(clock);
        if (moving != null && age > move.ticks() + 2) {
            move = FurutaMove.NONE;
        }
        double[] aim = moving == null || move == FurutaMove.NONE ? null : frame.rig(new double[] {moving.x(), moving.y(), moving.z()});
        if (aim != null && move.follows() && moving.targets()[0] != 0) {
            // 7.0.2: its point is its target wherever it goes (as the server strikes it).
            Entity followed = level.m_6815_(moving.targets()[0]);
            if (followed != null) {
                aim = frame.rig(centre(followed, partial));
            }
        }
        double[] held = null;
        double heldRadius = 0.3;
        if (moving != null && moving.heldId() != 0) {
            Entity entity = level.m_6815_(moving.heldId());
            if (entity != null) {
                held = frame.rig(centre(entity, partial));
                heldRadius = entity.m_20205_() * 0.5 + width * 0.6;
            }
        }
        // 7.0.2: the job running alongside (the crawl, the mouth dragging), on its own clock (never the kakuja's colossi).
        FurutaMove job = moving == null || moving.job().stage() != stage ? FurutaMove.NONE : moving.job();
        double jobAge = job == FurutaMove.NONE ? 0.0 : moving.jobAge(clock);
        if (job != FurutaMove.NONE && jobAge > job.ticks() + 2) {
            job = FurutaMove.NONE;
        }
        double[] jobHeld = null;
        if (job != FurutaMove.NONE && moving.jobHeld() != 0) {
            Entity entity = level.m_6815_(moving.jobHeld());
            if (entity != null) {
                jobHeld = frame.rig(centre(entity, partial));
            }
        }
        int light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(player.m_20185_(), player.m_20186_() + 1.0, player.m_20189_()));
        for (int limb = 0; limb < n; limb++) {
            double[] target = null;
            if (moving != null && move != FurutaMove.NONE && limb < moving.targets().length && moving.targets()[limb] != 0) {
                Entity entity = level.m_6815_(moving.targets()[limb]);
                if (entity != null) {
                    target = frame.rig(centre(entity, partial));
                }
            }
            boolean multi = move != FurutaMove.NONE && (move.chases() || move.beats(n).stream().anyMatch(b -> b.has(FurutaMove.MULTI)));
            FurutaRig.Pose pose = FurutaRig.pose(new FurutaRig.Input(stage, n, limb, length * (0.55 + 0.45 * grown), body, move, age, aim,
                multi ? target : null, held, heldRadius, job, jobAge, jobHeld));
            if (motion[limb] == null) {
                motion[limb] = new FurutaMotion.Limb();
            }
            FurutaMotion.Limb leg = motion[limb];
            leg.follow(pose, clock);
            double[][] rig = com.hex.kagune.control.client.animation.FurutaPose.ride(posed, leg.points());
            double drawn = Math.max(0.2, CentipedeMotion.curveLength(rig));
            if (grown < 0.999) {
                // Drawn out of his back along its own curve: only its first part shows while it grows (or goes back in).
                rig = CentipedeMotion.resample(rig, drawn * Math.max(0.04, grown));
            }
            double[][] world = new double[rig.length][];
            for (int i = 0; i < rig.length; i++) {
                world[i] = frame.place(rig[i], camera);
            }
            if (eyeRig != null) {
                world = clearOfEyes(world, eyeRig);
                if (world == null) {
                    continue;
                }
            }
            int side = FurutaRig.side(stage, n, limb);
            double[] dorsal = frame.turn(leg.dorsal());
            KakujaModel.Spine spine = CentipedeTail.through(world, fine ? 72 : 36, side, dorsal);
            double[] tip = world[world.length - 1];
            // The eyes on what the move is aimed at, or at rest on the prey.
            double[] seen = aim != null ? aim : body.prey();
            double[] gaze = seen == null ? null : FurutaModel.gazeToward(tip, frame.place(seen, camera));
            double swell = grown < 0.35 ? 1.0 + 0.4 * Math.sin(Math.PI * grown / 0.35) : 1.0;
            FurutaModel.Look look = new FurutaModel.Look(width * (0.45 + 0.55 * grown) * swell, fine, 1.0, player.m_19879_() * 7 + limb * 13 + 3, leg.jaw(),
                leg.flare() * (0.4 + 0.6 * grown), leg.stare(), FurutaRig.maw(stage, limb), clock, gaze,
                move.guards() && FurutaRig.cocoon(stage));
            FurutaModel.limb((a, b, c, d, nn, ca, cb, cc, cd) -> quad(out, matrix, normals, a, b, c, d, nn, ca, cb, cc, cd, light), stage, spine, look);
        }
        if (grown < 0.35 && grown > 0.01) {
            // Before anything shows: muscle heaving under the skin of his lower back.
            double heave = Math.sin(Math.PI * grown / 0.35);
            double[] root = frame.place(com.hex.kagune.control.client.animation.FurutaPose.ride(posed,
                new double[][] {FurutaRig.root(stage, n, 0, body.crouching())})[0], camera);
            if (eyeRig == null) {
                FurutaMesh.blob((a, b, c, d, nn, ca, cb, cc, cd) -> quad(out, matrix, normals, a, b, c, d, nn, ca, cb, cc, cd, light), root,
                    frame.turn(new double[] {1.0, 0.0, 0.0}), new double[] {0.0, 1.0, 0.0}, frame.turn(new double[] {0.0, 0.0, 1.0}), 0.22 * heave,
                    0.18 * heave, 0.14 * heave, 5, 8, 0.25, player.m_19879_(), v -> 0.4 + 0.4 * v, 0x8A1820, 0x3A050C, 1.0);
            }
        }
    }

    /** The limb's points with those within reach of the eyes dropped (null if too little is left to draw). */
    private static double[][] clearOfEyes(double[][] world, double[] eye) {
        int first = 0;
        while (first < world.length && FurutaMesh.length3(FurutaMesh.sub(world[first], eye)) < FIRST_PERSON_CLEAR) {
            first++;
        }
        if (world.length - first < 3) {
            return null;
        }
        return first == 0 ? world : java.util.Arrays.copyOfRange(world, first, world.length);
    }

    /** How he moves this frame (rig space: his speed, crouch, rise and fall, where he looks). */
    /**
     * The prey his mouths are after (rig space, its middle): the nearest living thing within {@link FurutaRig#PREY_RANGE} of him,
     * looked for again every tick, or null.
     */
    private static double[] prey(ClientLevel level, LivingEntity player, Limbs limbs, float partial, Frame frame) {
        long tick = level.m_46467_();
        if (tick != limbs.preySought) {
            limbs.preySought = tick;
            limbs.prey = 0;
            double best = FurutaRig.PREY_RANGE * FurutaRig.PREY_RANGE;
            for (LivingEntity body : level.m_6443_(LivingEntity.class, player.m_20191_().m_82400_(FurutaRig.PREY_RANGE),
                living -> living != player && living.m_6084_() && !living.m_5833_())) {
                double distance = body.m_20280_(player);
                if (distance < best) {
                    best = distance;
                    limbs.prey = body.m_19879_();
                }
            }
        }
        Entity prey = limbs.prey == 0 ? null : level.m_6815_(limbs.prey);
        return prey == null || !prey.m_6084_() ? null : frame.rig(centre(prey, partial));
    }

    private static FurutaRig.Body body(LivingEntity player, float partial, double clock, Frame frame, double[] prey) {
        double speed = Math.hypot(player.m_20185_() - player.f_19790_, player.m_20189_() - player.f_19792_);
        double vertical = player.m_20186_() - player.f_19791_;
        double headYaw = player.f_20886_ + wrap(player.f_20885_ - player.f_20886_) * partial;
        double lookYaw = Math.toRadians(wrap(headYaw - frame.bodyYaw()));
        double lookPitch = Math.toRadians(player.m_146909_());
        return new FurutaRig.Body(speed, player.m_6144_(), vertical, player.m_20096_(), lookYaw, lookPitch, clock, prey);
    }

    /** An entity's middle, interpolated for this frame. */
    private static double[] centre(Entity entity, float partial) {
        return new double[] {entity.f_19790_ + (entity.m_20185_() - entity.f_19790_) * partial,
            entity.f_19791_ + (entity.m_20186_() - entity.f_19791_) * partial + entity.m_20206_() * 0.52,
            entity.f_19792_ + (entity.m_20189_() - entity.f_19792_) * partial};
    }

    /** A player's rig frame for this frame: feet in the world, and the body's forward and right. */
    private record Frame(double x, double y, double z, double forwardX, double forwardZ, double rightX, double rightZ, double bodyYaw) {
        static Frame of(LivingEntity player, float partial) {
            double feetX = player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial;
            double feetY = player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial;
            double feetZ = player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial;
            float previousYaw = player.f_20884_;
            double bodyYaw = previousYaw + wrap(player.f_20883_ - previousYaw) * partial;
            double yaw = Math.toRadians(bodyYaw);
            return new Frame(feetX, feetY, feetZ, -Math.sin(yaw), Math.cos(yaw), -Math.cos(yaw), -Math.sin(yaw), bodyYaw);
        }

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

    private static void quad(VertexConsumer out, Matrix4f matrix, Matrix3f normals, double[] a, double[] b, double[] c, double[] d, double[] n, int ca, int cb,
                             int cc, int cd, int light) {
        // Glowing quads (the RC) carry a straight-up normal: full brightness; flesh and carapace take the world's light.
        int lit = n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0 ? FULL_BRIGHT : light;
        vertex(out, matrix, normals, a, n, ca, lit);
        vertex(out, matrix, normals, b, n, cb, lit);
        vertex(out, matrix, normals, c, n, cc, lit);
        vertex(out, matrix, normals, d, n, cd, lit);
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
}
