package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.bikaku.BikakuRules;
import com.hex.kagune.control.bikaku.MawMotion;
import com.hex.kagune.control.bikaku.MawWorm;
import com.hex.kagune.control.client.BikakuClient;
import com.hex.kagune.control.client.KaguneTints;
import com.hex.kagune.control.client.PaleClient;
import com.hex.kagune.control.graft.GraftMesh;
import com.hex.kagune.control.ukaku.UkakuModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Objects;
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
 * 3.4.9: the Bikaku path's worm in the world ({@link MawWorm}, posed by {@link MawMotion}): out of the lower back, riding his body as it
 * is drawn, in his own kagune colour ({@link KaguneTints}), alive at rest (it slithers, scans, gapes now and then) and thrown out on his
 * M1 bites at what the server says it bites. It grows out of his back as the kagune comes out, mouth first along its own curve, and goes
 * back in the same way.
 *
 * <p>Same render type and vertex layout as the other kagune renderers (a white texel tinted per vertex, translucent), lit by the world
 * except the glowing veins (a quad lit from straight above). In first person nothing of it comes within a block and a bit of the eyes:
 * the worm passes round him, and anything nearer is not drawn.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class BikakuRenderer {
    private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final double MAX_DISTANCE_SQR = 96.0 * 96.0;
    private static final double DETAIL_DISTANCE_SQR = 24.0 * 24.0;
    /** In first person no quad is drawn with a corner nearer the eyes than this (its line is kept further: MawMotion.firstPerson). */
    private static final double QUAD_CLEAR = 0.6;

    private BikakuRenderer() {
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
        double clock = PaleClient.clock(partial);
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        PoseStack stack = event.getPoseStack();
        Matrix4f matrix = stack.m_85850_().m_252922_();
        Matrix3f normals = stack.m_85850_().m_252943_();
        MultiBufferSource.BufferSource buffers = minecraft.m_91269_().m_110104_();
        VertexConsumer out = null;
        for (AbstractClientPlayer player : level.m_6907_()) {
            try {
                if (!BikakuClient.isBikaku(player) || !player.m_6084_() || player.m_20145_() || player.m_5803_() || player.m_21255_()
                    || com.hex.kagune.control.client.ReviveClient.apart(player, partial)) {
                    continue;
                }
                double distance = player.m_20280_(self);
                if (distance > MAX_DISTANCE_SQR) {
                    continue;
                }
                boolean firstPerson = player == self && minecraft.f_91066_.m_92176_().m_90612_();
                // 3.5.3: Monstrous Regeneration's strands over his body (his own body isn't drawn in first person: nor are they).
                BikakuClient.View view = BikakuClient.get(player);
                if (!firstPerson && com.hex.kagune.control.bikaku.MawTissue.shown(view.regenAge(clock)) > 0.002) {
                    if (out == null) {
                        out = buffers.m_6299_(TYPE);
                    }
                    tissue(out, matrix, normals, level, player, partial, clock, camera, view);
                }
                float deploy = BikakuClient.deploy(player, partial);
                if (deploy <= 0.005F) {
                    continue;
                }
                if (out == null) {
                    out = buffers.m_6299_(TYPE);
                }
                draw(out, matrix, normals, level, player, partial, clock, camera, distance <= DETAIL_DISTANCE_SQR, firstPerson, deploy);
            } catch (RuntimeException failure) {
                // One player's worm failing never keeps the next player's from being drawn (logged, as the other paths').
                PlayerLayers.failed("a Bikaku player's worm", failure);
            }
        }
        if (out != null) {
            buffers.m_109912_(TYPE);
        }
    }

    private static void draw(VertexConsumer out, Matrix4f matrix, Matrix3f normals, ClientLevel level, AbstractClientPlayer player, float partial,
                             double clock, Vec3 camera, boolean fine, boolean firstPerson, float deploy) {
        Frame frame = Frame.of(player, partial);
        BikakuClient.View view = BikakuClient.get(player);
        int seed = BikakuClient.seed(player);
        double age = view.age(clock);
        double[] target = null;
        // 3.5.4: the kakuja (or its growing or falling away): how far each part of it has grown, the worm swelling into it.
        com.hex.kagune.control.bikaku.MawKakuja.Growth growth = com.hex.kagune.control.bikaku.MawKakuja.growth(view.kakuja(), view.kakujaAge(clock));
        boolean tentacle = view.bite() == BikakuRules.Bite.TENTACLE;
        double biteSlower = tentacle ? 1.0 : BikakuRules.slower(view.reach(), BikakuRules.REACH); // 3.5.8: thrown further, slower
        if (view.bite() != BikakuRules.Bite.NONE && !tentacle && age < view.bite().ticks() * biteSlower) {
            Entity followed = view.targetId() != 0 ? level.m_6815_(view.targetId()) : null;
            target = frame.rig(followed != null ? centre(followed, partial) : new double[] {view.x(), view.y(), view.z()});
        }
        // 3.6.0: his form's worms (one in the kakuja: the worm it grows from), the one whose turn it is making the bite.
        int count = view.kakuja() ? 1 : view.form().worms();
        MawMotion.Worm[] worms = MawMotion.form(clock, seed, count, view.bite(), age, target, view.reach());
        if (target == null && view.charge() >= 0 && !view.kakuja()) {
            // 3.5.8: a key held: the worm (the first, in a form of several) winds up.
            worms[0] = new MawMotion.Worm(MawMotion.framed(count, 0, MawMotion.charging(clock, seed, view.charged(clock))), 1.0, 1.0, seed);
        }
        // 3.5.9: each worm's way round blocks (the world) and where its throw starts (body frame); none: a straight line.
        double[][][] ways = new double[worms.length][][];
        double[][] starts = new double[worms.length][];
        for (int i = 0; i < worms.length; i++) {
            starts[i] = MawMotion.wormShoulder(count, i);
        }
        if (target != null) {
            ways[com.hex.kagune.control.bikaku.BikakuForm.biter(view.bite(), count)] = view.route(0);
        }
        // 3.5.0: a move, while it is being made, over a bite.
        double moveAge = view.moveAge(clock);
        com.hex.kagune.control.bikaku.BikakuMove move = view.move();
        double moveSlower = move.held() ? BikakuRules.slower(view.moveReach(), move.baseReach()) : 1.0;
        int moveWorms = view.kakuja() ? 1 : view.moveWorms();
        // 3.6.1: each worm after the first a beat later.
        double moveLater = moveWorms >= 2 ? (moveWorms - 1) * move.stagger() : 0.0;
        if (move != com.hex.kagune.control.bikaku.BikakuMove.NONE && moveAge < (move.ticks() + 2) * moveSlower + moveLater) {
            double[] point = {view.mx(), view.my(), view.mz()};
            if (moveWorms >= 2 && (move == com.hex.kagune.control.bikaku.BikakuMove.MAW || move == com.hex.kagune.control.bikaku.BikakuMove.DRAG
                || move == com.hex.kagune.control.bikaku.BikakuMove.PURSUIT || move == com.hex.kagune.control.bikaku.BikakuMove.ERUPTION)) {
                // 3.6.1: every worm of his form makes it, each in its lane after its own body (or the first's), a beat after the one before.
                double[][] aims = new double[moveWorms][];
                double[][] helds = new double[moveWorms][];
                boolean[] caughts = new boolean[moveWorms];
                for (int i = 0; i < moveWorms; i++) {
                    Entity aimed = i < view.targets().length && view.targets()[i] != 0 ? level.m_6815_(view.targets()[i]) : null;
                    double[] own = i < view.points().length && view.points()[i] != null ? view.points()[i] : point;
                    aims[i] = frame.rig(aimed != null ? centre(aimed, partial) : own);
                    Entity heldEntity = i < view.held().length && view.held()[i] != 0 ? level.m_6815_(view.held()[i]) : null;
                    helds[i] = heldEntity != null ? frame.rig(centre(heldEntity, partial)) : null;
                    caughts[i] = (view.caughtBits() >> i & 1) != 0;
                }
                worms = MawMotion.formMoves(clock, seed, moveWorms, move, moveAge, aims, helds, caughts, Math.max(move.baseReach(), view.moveReach()));
                ways = new double[worms.length][][];
                starts = new double[worms.length][];
                for (int i = 0; i < worms.length; i++) {
                    ways[i] = move == com.hex.kagune.control.bikaku.BikakuMove.ERUPTION ? null : view.route(1 + i);
                    starts[i] = move == com.hex.kagune.control.bikaku.BikakuMove.DRAG ? MawMotion.wormPoint(moveWorms, i, MawMotion.groundAnchor())
                        : MawMotion.wormShoulder(moveWorms, i);
                }
            } else if (move == com.hex.kagune.control.bikaku.BikakuMove.PURSUIT) {
                // 3.5.1: three worms, each after its own body.
                double[][] aims = new double[view.targets().length][];
                for (int i = 0; i < aims.length; i++) {
                    Entity aimed = view.targets()[i] != 0 ? level.m_6815_(view.targets()[i]) : null;
                    aims[i] = frame.rig(aimed != null ? centre(aimed, partial) : point);
                }
                worms = MawMotion.pursuit(clock, seed, aims, moveAge, Math.max(move.baseReach(), view.moveReach()));
                ways = new double[worms.length][][];
                starts = new double[worms.length][];
                for (int i = 0; i < worms.length; i++) {
                    ways[i] = view.route(1 + i);
                    starts[i] = MawMotion.pursuitShoulder(i);
                }
            } else {
                Entity aimed = view.targets()[0] != 0 ? level.m_6815_(view.targets()[0]) : null;
                double[] aim = frame.rig(aimed != null ? centre(aimed, partial) : point);
                Entity heldEntity = view.heldId() != 0 ? level.m_6815_(view.heldId()) : null;
                double[] held = heldEntity != null ? frame.rig(centre(heldEntity, partial)) : null;
                // 3.6.0: its first worm makes it (in its lane), the rest at rest.
                worms = MawMotion.formMove(clock, seed, count, move, moveAge, aim, held, view.caught(), Math.max(move.baseReach(), view.moveReach()));
                ways = new double[worms.length][][];
                starts = new double[worms.length][];
                for (int i = 0; i < worms.length; i++) {
                    starts[i] = MawMotion.wormShoulder(count, i);
                }
                ways[0] = move == com.hex.kagune.control.bikaku.BikakuMove.MAW || move == com.hex.kagune.control.bikaku.BikakuMove.DRAG ? view.route(1) : null;
                starts[0] = move == com.hex.kagune.control.bikaku.BikakuMove.DRAG ? MawMotion.wormPoint(count, 0, MawMotion.groundAnchor())
                    : MawMotion.wormShoulder(count, 0);
            }
        }
        // His body as drawn (hunched, leaning): the worm's root rides it.
        com.hex.kagune.control.client.animation.RigPose posed = com.hex.kagune.control.client.animation.KaguneBodyAnimation.body(player,
            com.hex.kagune.control.client.ControlClient.worldTime(partial));
        int light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(player.m_20185_(), player.m_20186_() + 1.0, player.m_20189_()));
        double[] near = firstPerson ? new double[] {0.0, 0.0, 0.0} : null; // in first person the camera is at his eyes
        UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
            if (near != null && (closer(a, near) || closer(b, near) || closer(c, near) || closer(d, near))) {
                return;
            }
            quad(out, matrix, normals, a, b, c, d, n, ca, cb, cc, cd, light);
        };
        int tint = KaguneTints.of(player);
        for (int w = 0; w < worms.length; w++) {
            MawMotion.Worm worm = worms[w];
            double grown = GraftMesh.smooth(deploy) * worm.grown() * growth.worm();
            if (grown <= 0.002) {
                continue;
            }
            double[][] line = worm.shape().path();
            double[][] way = w < ways.length ? ways[w] : null;
            if (way != null && way.length >= 3) {
                // 3.5.9: laid along its way round the blocks.
                double[][] wayRig = new double[way.length][];
                for (int i = 0; i < way.length; i++) {
                    wayRig[i] = frame.rig(way[i]);
                }
                line = MawMotion.along(MawMotion.dense(line), starts[w], wayRig[wayRig.length - 1], wayRig);
            }
            // 3.5.5: the curve it is drawn along, round his body rather than through it; then carried with his torso's pose.
            double[][] rig = com.hex.kagune.control.client.animation.GraftPose.ride(posed, MawMotion.around(line, worm.girth(), worm.shape().aperture()));
            // Drawn out of his back along its own curve, mouth first: only its first part shows while it grows (or goes back in).
            rig = MawMotion.grown(rig, grown);
            if (firstPerson) {
                // The worm passes him to the top right of his view, never into it.
                rig = MawMotion.firstPerson(rig, frame.rig(new double[] {camera.f_82479_, camera.f_82480_, camera.f_82481_}));
            }
            double[][] world = new double[rig.length][];
            for (int i = 0; i < rig.length; i++) {
                world[i] = frame.place(rig[i], camera);
            }
            MawWorm.build(sink, new MawWorm.Pose(world, frame.turn(worm.shape().dorsal()), grown, worm.shape().aperture(),
                worm.girth() * (1.0 + growth.swell()), worm.seed(), clock), tint, fine);
        }
        if (growth.shows()) {
            // 3.5.4: the kakuja, built in his body's frame and placed in the world; in first person nothing of it near the eyes (he is
            // inside it: he sees out).
            // 3.5.7: what it does this frame: its own move (each tentacle after its own body, Endless Hunger's catch where the client sees it),
            // Undying Abomination's mending, M1's tentacle bite, and its tentacles' bites of their own, each tentacle doing one thing.
            com.hex.kagune.control.bikaku.MawKakuja.Act act = com.hex.kagune.control.bikaku.MawKakuja.Act.still();
            com.hex.kagune.control.bikaku.BikakuMove kakujaMove = view.move();
            if (kakujaMove.kakuja()) {
                double[][] aims = new double[com.hex.kagune.control.bikaku.MawKakuja.tentacles()][];
                for (int i = 0; i < aims.length && i < view.targets().length; i++) {
                    Entity aimed = view.targets()[i] != 0 ? level.m_6815_(view.targets()[i]) : null;
                    aims[i] = aimed != null ? frame.rig(centre(aimed, partial)) : null;
                }
                Entity heldEntity = view.heldId() != 0 ? level.m_6815_(view.heldId()) : null;
                double[] heldAt = heldEntity != null ? frame.rig(centre(heldEntity, partial)) : null;
                act = com.hex.kagune.control.bikaku.KakujaActs.act(kakujaMove, view.moveAge(clock), clock, aims, heldAt,
                    frame.rig(new double[] {view.mx(), view.my(), view.mz()}), view.caught());
            }
            double undyingAge = view.undyingAge(clock);
            if (undyingAge < com.hex.kagune.control.bikaku.BikakuMove.UNDYING.ticks()) {
                act = com.hex.kagune.control.bikaku.KakujaActs.withMend(act, com.hex.kagune.control.bikaku.KakujaActs.mend(undyingAge));
            }
            if (tentacle && age < view.bite().ticks()) {
                Entity followed = view.targetId() != 0 ? level.m_6815_(view.targetId()) : null;
                double[] lunge = frame.rig(followed != null ? centre(followed, partial) : new double[] {view.x(), view.y(), view.z()});
                int biter = com.hex.kagune.control.bikaku.MawKakuja.biter(lunge, clock);
                if (act.lunge()[biter] == null && biter != act.wrapper()) {
                    act = act.lunging(biter, lunge, com.hex.kagune.control.bikaku.MawKakuja.lunge(age), com.hex.kagune.control.bikaku.MawKakuja.lungeMouth(age));
                }
            }
            for (int i = 0; i < com.hex.kagune.control.bikaku.MawKakuja.tentacles() && i < view.autoTarget().length; i++) {
                double autoAge = view.autoAge(i, clock);
                if (autoAge < com.hex.kagune.control.bikaku.KakujaActs.autoTicks() && view.autoTarget()[i] != 0) {
                    Entity prey = level.m_6815_(view.autoTarget()[i]);
                    if (prey != null) {
                        act = com.hex.kagune.control.bikaku.KakujaActs.withAuto(act, i, frame.rig(centre(prey, partial)), autoAge);
                    }
                }
            }
            double clearSq = KAKUJA_CLEAR * KAKUJA_CLEAR;
            UkakuModel.Sink placed = (a, b, c, d, n, ca, cb, cc, cd) -> {
                double[] pa = frame.place(a, camera);
                double[] pb = frame.place(b, camera);
                double[] pc = frame.place(c, camera);
                double[] pd = frame.place(d, camera);
                if (near != null && (sq(pa) < clearSq || sq(pb) < clearSq || sq(pc) < clearSq || sq(pd) < clearSq)) {
                    return;
                }
                quad(out, matrix, normals, pa, pb, pc, pd, frame.turn(n), ca, cb, cc, cd, light);
            };
            com.hex.kagune.control.bikaku.MawKakuja.build(placed, growth, clock, seed, tint, fine, act);
        }
    }

    /** 3.5.4: in first person, nothing of the kakuja this near his eyes (he is inside its mass). */
    private static final double KAKUJA_CLEAR = 2.6;

    private static double sq(double[] p) {
        return p[0] * p[0] + p[1] * p[1] + p[2] * p[2];
    }

    /** 3.5.3: Monstrous Regeneration's strands, each laid on the part of his body it crawls over, as that part is posed now. */
    private static void tissue(VertexConsumer out, Matrix4f matrix, Matrix3f normals, ClientLevel level, AbstractClientPlayer player, float partial,
                               double clock, Vec3 camera, BikakuClient.View view) {
        Frame frame = Frame.of(player, partial);
        com.hex.kagune.control.client.animation.RigPose posed = com.hex.kagune.control.client.animation.KaguneBodyAnimation.body(player,
            com.hex.kagune.control.client.ControlClient.worldTime(partial));
        if (posed == null) {
            posed = new com.hex.kagune.control.client.animation.RigPose();
        }
        java.util.List<double[][]> placed = new java.util.ArrayList<>();
        java.util.List<Double> radii = new java.util.ArrayList<>();
        for (com.hex.kagune.control.bikaku.MawTissue.Strand strand : com.hex.kagune.control.bikaku.MawTissue.strands(clock, view.regenAge(clock),
            BikakuClient.seed(player))) {
            double[][] points = new double[strand.local().length][];
            for (int i = 0; i < points.length; i++) {
                double[] rig = com.hex.kagune.control.client.animation.BodyFrame.world(posed,
                    com.hex.kagune.control.client.animation.BodyFrame.model(posed, strand.part(), strand.local()[i]));
                points[i] = frame.place(rig, camera);
            }
            placed.add(points);
            radii.add(strand.radius());
        }
        int light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(player.m_20185_(), player.m_20186_() + 1.0, player.m_20189_()));
        com.hex.kagune.control.bikaku.MawTissue.build((a, b, c, d, n, ca, cb, cc, cd) -> quad(out, matrix, normals, a, b, c, d, n, ca, cb, cc, cd, light),
            placed, radii, KaguneTints.of(player));
    }

    private static boolean closer(double[] p, double[] eye) {
        double dx = p[0] - eye[0];
        double dy = p[1] - eye[1];
        double dz = p[2] - eye[2];
        return dx * dx + dy * dy + dz * dz < QUAD_CLEAR * QUAD_CLEAR;
    }

    /** {@code entity}'s middle in the world this frame. */
    private static double[] centre(Entity entity, float partial) {
        return new double[] {entity.f_19790_ + (entity.m_20185_() - entity.f_19790_) * partial,
            entity.f_19791_ + (entity.m_20186_() - entity.f_19791_) * partial + entity.m_20206_() * 0.52,
            entity.f_19792_ + (entity.m_20189_() - entity.f_19792_) * partial};
    }

    /** A player's rig frame for this frame: feet in the world, and the body's forward and right (as the other kagune renderers'). */
    private record Frame(double x, double y, double z, double forwardX, double forwardZ, double rightX, double rightZ) {
        static Frame of(LivingEntity player, float partial) {
            double feetX = player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial;
            double feetY = player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial;
            double feetZ = player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial;
            float previousYaw = player.f_20884_;
            double bodyYaw = previousYaw + wrap(player.f_20883_ - previousYaw) * partial;
            double yaw = Math.toRadians(bodyYaw);
            return new Frame(feetX, feetY, feetZ, -Math.sin(yaw), Math.cos(yaw), -Math.cos(yaw), -Math.sin(yaw));
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
        // Glowing quads (the veins) carry a straight-up normal: full brightness; the flesh, teeth and bone take the world's light.
        int lit = n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0 ? FULL_BRIGHT : light;
        vertex(out, matrix, normals, a, n, ca, lit);
        vertex(out, matrix, normals, b, n, cb, lit);
        vertex(out, matrix, normals, c, n, cc, lit);
        vertex(out, matrix, normals, d, n, cd, lit);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals, double[] p, double[] n, int argb, int light) {
        consumer.m_252986_(matrix, (float)p[0], (float)p[1], (float)p[2])
            .m_6122_((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, argb >>> 24)
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
