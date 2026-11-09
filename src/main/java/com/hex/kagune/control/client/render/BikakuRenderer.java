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
        // 4.0.0: Noro's three worms (the owner: "his maximum Bikaku combat configuration, consisting of three powerful tendrils ... fully
        // animated"): drawn as a Three Worms player's are, from the same state, under his own id.
        for (int id : com.hex.kagune.control.client.NpcWingsClient.ids()) {
            try {
                if (!(level.m_6815_(id) instanceof com.hex.kagune.control.npc.EtoNpc npc) || !npc.persona().worms() || !npc.m_6084_() || npc.m_20145_()) {
                    continue;
                }
                com.hex.kagune.control.client.NpcWingsClient.View wings = com.hex.kagune.control.client.NpcWingsClient.get(id);
                float deploy = com.hex.kagune.control.client.NpcWingsClient.deploy(wings, clock);
                double distance = npc.m_20280_(self);
                if (distance > MAX_DISTANCE_SQR) {
                    continue;
                }
                if (out == null) {
                    out = buffers.m_6299_(TYPE);
                }
                drawNpc(out, matrix, normals, level, npc, partial, clock, camera, distance <= DETAIL_DISTANCE_SQR, deploy,
                    com.hex.kagune.control.client.NpcWingsClient.mending(wings, clock));
            } catch (RuntimeException failure) {
                PlayerLayers.failed("Noro's worms", failure);
            }
        }
        if (out != null) {
            buffers.m_109912_(TYPE);
        }
    }

    /** 4.0.0: Noro's worms' colour: his kagune, a black red. */
    private static final int NORO_TINT = 0xFF4A1A22;

    /**
     * 4.0.0: an NPC's three worms (Noro's), as {@link #draw} draws a Three Worms player's: at rest, biting, every move made by all three; his
     * size carrying them; while he mends, strands of them over him ({@link com.hex.kagune.control.bikaku.MawTissue}).
     */
    private static void drawNpc(VertexConsumer out, Matrix4f matrix, Matrix3f normals, ClientLevel level, com.hex.kagune.control.npc.EtoNpc npc,
                                float partial, double clock, Vec3 camera, boolean fine, float deploy, double mendAge) {
        Frame frame = Frame.of(npc, partial);
        double size = npc.persona().size();
        BikakuClient.View view = BikakuClient.get(npc.m_20148_());
        int seed = BikakuRules.seed(npc.m_20148_().getMostSignificantBits(), npc.m_20148_().getLeastSignificantBits());
        int light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(npc.m_20185_(), npc.m_20186_() + 1.0, npc.m_20189_()));
        UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> quad(out, matrix, normals, a, b, c, d, n, ca, cb, cc, cd, light);
        if (mendAge < 70.0) {
            // His mend: strands of his worms crawling over him as he pulls himself back together (Monstrous Regeneration's, on his body).
            com.hex.kagune.control.client.animation.RigPose posed = new com.hex.kagune.control.client.animation.RigPose();
            java.util.List<double[][]> placed = new java.util.ArrayList<>();
            java.util.List<Double> radii = new java.util.ArrayList<>();
            for (com.hex.kagune.control.bikaku.MawTissue.Strand strand : com.hex.kagune.control.bikaku.MawTissue.strands(clock, mendAge, seed)) {
                double[][] points = new double[strand.local().length][];
                for (int i = 0; i < points.length; i++) {
                    double[] rig = com.hex.kagune.control.client.animation.BodyFrame.world(posed,
                        com.hex.kagune.control.client.animation.BodyFrame.model(posed, strand.part(), strand.local()[i]));
                    points[i] = frame.place(scale(rig, size), camera);
                }
                placed.add(points);
                radii.add(strand.radius() * size);
            }
            com.hex.kagune.control.bikaku.MawTissue.build(sink, placed, radii, NORO_TINT);
        }
        if (deploy <= 0.005F) {
            return;
        }
        int count = 3;
        double age = view.age(clock);
        double[] target = null;
        if (view.bite() != BikakuRules.Bite.NONE && age < view.bite().ticks()) {
            Entity followed = view.targetId() != 0 ? level.m_6815_(view.targetId()) : null;
            target = unscale(frame.rig(followed != null ? centre(followed, partial) : new double[] {view.x(), view.y(), view.z()}), size);
        }
        MawMotion.Worm[] worms = MawMotion.form(clock, seed, count, view.bite(), age, target, view.reach(), -1);
        double[][] starts = new double[count][];
        for (int i = 0; i < count; i++) {
            starts[i] = MawMotion.wormShoulder(count, i);
        }
        double moveAge = view.moveAge(clock);
        com.hex.kagune.control.bikaku.BikakuMove move = view.move();
        if (move != com.hex.kagune.control.bikaku.BikakuMove.NONE && moveAge < move.ticks() + 2 + (count - 1) * move.stagger()) {
            double[][] aims = new double[count][];
            double[][] helds = new double[count][];
            boolean[] caughts = new boolean[count];
            double[] point = {view.mx(), view.my(), view.mz()};
            for (int i = 0; i < count; i++) {
                Entity aimed = i < view.targets().length && view.targets()[i] != 0 ? level.m_6815_(view.targets()[i]) : null;
                double[] own = i < view.points().length && view.points()[i] != null ? view.points()[i] : point;
                aims[i] = unscale(frame.rig(aimed != null ? centre(aimed, partial) : own), size);
                Entity heldEntity = i < view.held().length && view.held()[i] != 0 ? level.m_6815_(view.held()[i]) : null;
                helds[i] = heldEntity != null ? unscale(frame.rig(centre(heldEntity, partial)), size) : null;
                caughts[i] = (view.caughtBits() >> i & 1) != 0;
            }
            worms = MawMotion.formMoves(clock, seed, count, move, moveAge, aims, helds, caughts, Math.max(move.baseReach(), view.moveReach()) / size);
            for (int i = 0; i < worms.length; i++) {
                starts[i] = move == com.hex.kagune.control.bikaku.BikakuMove.DRAG ? MawMotion.wormPoint(count, i, MawMotion.groundAnchor())
                    : MawMotion.wormShoulder(count, i);
            }
        }
        for (int w = 0; w < worms.length; w++) {
            MawMotion.Worm worm = worms[w];
            double grown = GraftMesh.smooth(deploy) * worm.grown();
            if (grown <= 0.002) {
                continue;
            }
            double[][] rig = MawMotion.grown(MawMotion.around(worm.shape().path(), worm.girth(), worm.shape().aperture()), grown);
            double[][] world = new double[rig.length][];
            for (int i = 0; i < rig.length; i++) {
                world[i] = frame.place(scale(rig[i], size), camera);
            }
            MawWorm.build(sink, new MawWorm.Pose(world, frame.turn(worm.shape().dorsal()), grown, worm.shape().aperture(), worm.girth() * size, worm.seed(),
                clock), NORO_TINT, fine);
        }
    }

    private static double[] scale(double[] p, double size) {
        return new double[] {p[0] * size, p[1] * size, p[2] * size};
    }

    private static double[] unscale(double[] p, double size) {
        return new double[] {p[0] / size, p[1] / size, p[2] / size};
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
        // 3.6.3: a worm feeding (Grand Devouring) does nothing else: the bite is the next worm's, the wind-up too.
        int busy = view.kakuja() ? -1 : view.busyWorm(clock);
        MawMotion.Worm[] worms = MawMotion.form(clock, seed, count, view.bite(), age, target, view.reach(), busy);
        int winder = busy == 0 ? (count > 1 ? 1 : -1) : 0;
        if (target == null && view.charge() >= 0 && !view.kakuja() && winder >= 0) {
            // 3.5.8: a key held: the worm (the first, in a form of several) winds up.
            worms[winder] = new MawMotion.Worm(MawMotion.framed(count, winder, MawMotion.charging(clock, count > 1 ? seed + 101 * winder : seed,
                view.charged(clock))), 1.0, 1.0, count > 1 ? seed + 101 * winder : seed);
        }
        // 3.5.9: each worm's way round blocks (the world) and where its throw starts (body frame); none: a straight line.
        double[][][] ways = new double[worms.length][][];
        double[][] starts = new double[worms.length][];
        for (int i = 0; i < worms.length; i++) {
            starts[i] = MawMotion.wormShoulder(count, i);
        }
        int biteWorm = com.hex.kagune.control.bikaku.BikakuForm.biter(view.bite(), count, busy);
        if (target != null && biteWorm >= 0) {
            ways[biteWorm] = view.route(0);
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
        // 3.6.3: Grand Devouring: the feeding worm, whatever the others are doing (its swelling, its great mouth, the bulge going down it).
        MawWorm.Feed[] feeds = new MawWorm.Feed[worms.length];
        if (busy >= 0 && busy < worms.length && view.devourSpot() != null) {
            int own = count > 1 ? seed + 101 * busy : seed;
            com.hex.kagune.control.bikaku.GrandDevour.Feeding feeding = com.hex.kagune.control.bikaku.GrandDevour.at(clock, own, count, busy,
                frame.rig(view.devourSpot()), view.devourWidth(), view.devourHeight(), view.devourAge(clock));
            worms[busy] = new MawMotion.Worm(feeding.shape(), 1.0, 1.0, own);
            feeds[busy] = feeding.feed();
            ways[busy] = null;
        }
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
            double[][] rig = com.hex.kagune.control.client.animation.GraftPose.ride(posed, MawMotion.around(line, worm.girth(), worm.shape().aperture(),
                feeds[w]));
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
                worm.girth() * (1.0 + growth.swell()), worm.seed(), clock, feeds[w]), tint, fine);
        }
        // 3.7.1: K tapped, they grow out of him (and back in when he leaves it).
        double walkerGrown = BikakuClient.walkerGrown(player, clock);
        if (!view.kakuja() && walkerGrown > 0.002 && growth.worm() > 0.002) {
            // 3.6.2: The Walker's two walking tentacles, out of the same root, down onto where their feet stand.
            legs(sink, frame, posed, level, player, partial, clock, camera, fine, firstPerson, deploy, walkerGrown, seed, tint);
        } else {
            LEGS.remove(player);
        }
        // 3.6.9 (the owner: "Also Kakuja detransformation needs a simple shatter animation like the rinkaku has."): turning back, it is not
        // drawn falling away: it broke into pieces (KakujaFx.shatterBikaku), and only the worm grows back out of him.
        if (growth.shows() && view.kakuja()) {
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
            // 3.6.8 (the owner: "IN kakuja form you cant see anything, the camera needs to adapt to the new size."): his own view sits on its
            // crest in first person and well behind it in third (BikakuCamera); either way nothing of it this near the lens is drawn.
            Minecraft minecraft = Minecraft.m_91087_();
            boolean own = player == minecraft.f_91074_ && minecraft.m_91288_() == player;
            double clearSq = com.hex.kagune.control.bikaku.MawKakuja.VIEW_CLEAR * com.hex.kagune.control.bikaku.MawKakuja.VIEW_CLEAR;
            UkakuModel.Sink placed = (a, b, c, d, n, ca, cb, cc, cd) -> {
                double[] pa = frame.place(a, camera);
                double[] pb = frame.place(b, camera);
                double[] pc = frame.place(c, camera);
                double[] pd = frame.place(d, camera);
                if (own && (sq(pa) < clearSq || sq(pb) < clearSq || sq(pc) < clearSq || sq(pd) < clearSq)) {
                    return;
                }
                quad(out, matrix, normals, pa, pb, pc, pd, frame.turn(n), ca, cb, cc, cd, light);
            };
            com.hex.kagune.control.bikaku.MawKakuja.build(placed, growth, clock, seed, tint, fine, act);
        }
    }

    /**
     * 3.6.9 (the owner: "Also Kakuja detransformation needs a simple shatter animation like the rinkaku has."): the kakuja as it stood,
     * grown {@code growth}, at low detail, as quads in the world where {@code player} stands: what breaks into pieces when he turns back.
     */
    public static java.util.List<com.hex.kagune.control.KakujaBurst.Quad> kakujaQuads(AbstractClientPlayer player, com.hex.kagune.control.bikaku.MawKakuja.Growth growth,
                                                                                 double clock) {
        Frame frame = Frame.of(player, 1.0F);
        Vec3 world = new Vec3(0.0, 0.0, 0.0);
        java.util.List<com.hex.kagune.control.KakujaBurst.Quad> quads = new java.util.ArrayList<>();
        com.hex.kagune.control.bikaku.MawKakuja.build((a, b, c, d, n, ca, cb, cc, cd) -> quads.add(new com.hex.kagune.control.KakujaBurst.Quad(
            frame.place(a, world), frame.place(b, world), frame.place(c, world), frame.place(d, world), frame.turn(n), ca, cb, cc, cd,
            n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0)), growth, clock, BikakuClient.seed(player), KaguneTints.of(player), false,
            com.hex.kagune.control.bikaku.MawKakuja.Act.still());
        return quads;
    }

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

    /** 3.6.2: each player's walking feet (kept from frame to frame: they stay planted until they step). */
    private static final java.util.Map<AbstractClientPlayer, com.hex.kagune.control.bikaku.WalkerLegs.Legs> LEGS = new java.util.WeakHashMap<>();

    /** 3.6.2: The Walker's legs: his feet moved on (stepping as he goes, up walls he climbs), each tentacle drawn from the root to its foot. */
    private static void legs(UkakuModel.Sink sink, Frame frame, com.hex.kagune.control.client.animation.RigPose posed, ClientLevel level,
                             AbstractClientPlayer player, float partial, double clock, Vec3 camera, boolean fine, boolean firstPerson, float deploy,
                             double walkerGrown, int seed, int tint) {
        com.hex.kagune.control.bikaku.WalkerLegs.Legs legs = LEGS.computeIfAbsent(player, ignored -> new com.hex.kagune.control.bikaku.WalkerLegs.Legs());
        double[] feet = {frame.x(), frame.y(), frame.z()};
        double[] right = {frame.rightX(), 0.0, frame.rightZ()};
        double[] ahead = {frame.forwardX(), 0.0, frame.forwardZ()};
        double[] going = {player.m_20185_() - player.f_19790_, player.m_20186_() - player.f_19791_, player.m_20189_() - player.f_19792_};
        boolean climbing = going[1] > 0.04 && com.hex.kagune.control.client.BikakuWalk.wallAhead(level, feet[0], feet[1], feet[2], ahead[0], ahead[2]);
        com.hex.kagune.control.bikaku.WalkerLegs.update(legs, feet, right, ahead, going, climbing, clock,
            (x, y, z) -> com.hex.kagune.control.client.BikakuWalk.solid(level, x, y, z));
        double grown = GraftMesh.smooth(deploy) * walkerGrown; // 3.7.1: grown out with The Walker (K tapped), back in as it ends
        for (int i = 0; i < com.hex.kagune.control.bikaku.WalkerLegs.LEGS; i++) {
            double[] foot = com.hex.kagune.control.bikaku.WalkerLegs.foot(legs, i, clock);
            if (foot == null) {
                continue;
            }
            double[] face = com.hex.kagune.control.bikaku.WalkerLegs.face(legs, i);
            double[] footRig = frame.rig(foot);
            double[] faceRig = GraftMesh.sub(frame.rig(GraftMesh.add(foot, face)), footRig);
            MawMotion.Shape shape = com.hex.kagune.control.bikaku.WalkerLegs.shape(i, footRig, faceRig,
                com.hex.kagune.control.bikaku.WalkerLegs.stepping(legs, i, clock), clock, seed);
            double girth = com.hex.kagune.control.bikaku.WalkerLegs.GIRTH;
            // Round his body rather than through it, carried with his torso near the root; grown out with the kagune.
            double[][] rig = com.hex.kagune.control.client.animation.GraftPose.ride(posed, MawMotion.around(shape.path(), girth, shape.aperture()));
            rig = MawMotion.grown(rig, grown);
            if (firstPerson) {
                rig = MawMotion.firstPerson(rig, frame.rig(new double[] {camera.f_82479_, camera.f_82480_, camera.f_82481_}));
            }
            double[][] world = new double[rig.length][];
            for (int k = 0; k < rig.length; k++) {
                world[k] = frame.place(rig[k], camera);
            }
            MawWorm.build(sink, new MawWorm.Pose(world, frame.turn(shape.dorsal()), grown, shape.aperture(), girth, seed + 31 * (i + 1), clock), tint, fine);
        }
    }

    /**
     * 3.6.5: the feeding worm's mouth this frame, for the player inside it ({@link DevourInside}): where its mouth and the back of its cavity
     * are in the world, worked out exactly as {@link #draw} lays the worm (the same feeding, round his body, carried with his torso's pose,
     * grown with his kagune), how wide its jaws are, its cavity's size, his kagune colour and how far into the feeding it is; null when no
     * worm of his is feeding.
     */
    public record Mouth(double[] mouth, double[] back, double aperture, double hole, double depth, int tint, double age) {
    }

    public static Mouth feedingMouth(AbstractClientPlayer player, float partial, double clock) {
        BikakuClient.View view = BikakuClient.get(player);
        int busy = view.kakuja() ? -1 : view.busyWorm(clock);
        if (busy < 0 || view.devourSpot() == null) {
            return null;
        }
        Frame frame = Frame.of(player, partial);
        int seed = BikakuClient.seed(player);
        int count = view.kakuja() ? 1 : view.form().worms();
        int own = count > 1 ? seed + 101 * busy : seed;
        double age = view.devourAge(clock);
        com.hex.kagune.control.bikaku.GrandDevour.Feeding feeding = com.hex.kagune.control.bikaku.GrandDevour.at(clock, own, count, busy,
            frame.rig(view.devourSpot()), view.devourWidth(), view.devourHeight(), age);
        com.hex.kagune.control.bikaku.MawKakuja.Growth growth = com.hex.kagune.control.bikaku.MawKakuja.growth(view.kakuja(), view.kakujaAge(clock));
        double grown = GraftMesh.smooth(BikakuClient.deploy(player, partial)) * growth.worm();
        com.hex.kagune.control.client.animation.RigPose posed = com.hex.kagune.control.client.animation.KaguneBodyAnimation.body(player,
            com.hex.kagune.control.client.ControlClient.worldTime(partial));
        double[][] rig = com.hex.kagune.control.client.animation.GraftPose.ride(posed, MawMotion.around(feeding.shape().path(), 1.0,
            feeding.shape().aperture(), feeding.feed()));
        rig = MawMotion.grown(rig, grown);
        double[][] axis = com.hex.kagune.control.bikaku.GrandDevour.mouthAxis(rig, feeding.feed().depth());
        Vec3 origin = Vec3.f_82478_;
        return new Mouth(frame.place(axis[0], origin), frame.place(axis[1], origin), feeding.shape().aperture(), feeding.feed().hole(),
            feeding.feed().depth(), KaguneTints.of(player), age);
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
