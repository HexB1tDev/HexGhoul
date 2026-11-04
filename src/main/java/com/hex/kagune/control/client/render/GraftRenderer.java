package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.ukaku.CentipedeMotion;
import com.hex.kagune.control.ukaku.CentipedeTail;
import com.hex.kagune.control.ukaku.KakujaModel;
import com.hex.kagune.control.client.PaleClient;
import com.hex.kagune.control.client.GraftClient;
import com.hex.kagune.control.graft.GraftMesh;
import com.hex.kagune.control.graft.GraftModel;
import com.hex.kagune.control.graft.GraftMotion;
import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftRig;
import com.hex.kagune.control.graft.GraftRules;
import com.hex.kagune.control.graft.GraftStage;
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
 * Graft DNA's kagune in the world (7.0.0): the base stage's limbs ({@link GraftModel}: the transplant's tails, the centipedes, the
 * thorns, the many-eyed ribbons) and the kakuja's two colossi, posed by {@link GraftRig} and followed by
 * {@link GraftMotion}'s springs on the player's own clock. Each limb grows out of his lower back as the kagune comes out (a
 * swelling under the skin first, then the limb drawn out along its own curve and unfolding), and is drawn back the same way; a
 * kakuja transformation draws the base stage back into him before the colossi tear out and unfold, and the reverse on the way back.
 *
 * <p>Same render type and vertex layout as the other kagune renderers (a white texel tinted per vertex, translucent), lit by the
 * world except the glowing RC (a quad lit from straight above). In first person nothing within a block of the eyes is drawn.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class GraftRenderer {
    private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final double MAX_DISTANCE_SQR = 96.0 * 96.0;
    private static final double DETAIL_DISTANCE_SQR = 24.0 * 24.0;
    /** In first person, nothing of a limb this close to the eyes. */
    private static final double FIRST_PERSON_CLEAR = 1.15;
    private static final Map<UUID, Limbs> LIMBS = new HashMap<>();

    /** One player's limbs: the base stage's and the colossi, each following on its own. */
    private static final class Limbs {
        final GraftMotion.Limb[] base = new GraftMotion.Limb[5];
        final GraftMotion.Limb[] colossi = new GraftMotion.Limb[2];
        GraftStage stage;
        long seen;
        /** The prey its mouths are after (an entity id, 0 none) and the game tick it was last looked for. */
        int prey;
        long preySought = Long.MIN_VALUE;
    }

    private GraftRenderer() {
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
        VertexConsumer[] consumer = {null};
        for (AbstractClientPlayer player : level.m_6907_()) {
            try {
            if (!GraftClient.isGraft(player) || !player.m_6084_() || player.m_20145_() || player.m_5803_() || player.m_21255_()
                || com.hex.kagune.control.client.ReviveClient.apart(player, partial)) {
                continue;
            }
            float deploy = GraftClient.deploy(player, partial);
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
            GraftClient.View view = GraftClient.get(player);
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
            GraftRig.Body body = body(player, partial, clock, frame, prey(level, player, limbs, partial, frame));
            // His body as drawn (hunched, lunging): the limbs' roots ride it.
            com.hex.kagune.control.client.animation.RigPose posed = com.hex.kagune.control.client.animation.KaguneBodyAnimation.body(player,
                com.hex.kagune.control.client.ControlClient.worldTime(partial));
            double shownBase = deploy * GraftClient.baseShown(view, clock);
            double shownColossi = deploy * GraftClient.colossi(view, clock);
            boolean kakujaMoves = view.move().stage() == GraftStage.KAKUJA || view.kakuja() && view.move() != GraftMove.NONE
                && view.move().stage() == GraftStage.TRANSPLANT && shownColossi > 0.5;
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
                boolean colossiJob = view.kakuja() && view.job().stage() == GraftStage.KAKUJA;
                drawSet(out, matrix, normals, level, player, view, GraftStage.KAKUJA, limbs.colossi, shownColossi, kakujaMoves || colossiJob ? view : null, body,
                    frame, camera, clock, partial, mastery, fine, eyeRig, posed);
            } else {
                java.util.Arrays.fill(limbs.colossi, null);
            }
            } catch (RuntimeException failure) {
                // 2.0.27: one player's limbs failing never keeps the next player's, the wild ghouls' or the children's from being drawn.
                LIMBS.remove(player.m_20148_());
                PlayerLayers.failed("a Graft player's limbs", failure);
            }
        }
        // 7.0.2: the rinkaku ghoul NPCs in view: the transplant's tails, as a player's, at their tier's size.
        // 2.0.27: each ghoul on its own: one that fails is skipped (logged), never the rest of them nor the children after.
        for (com.hex.kagune.control.npc.GhoulNpc npc : GhoulNpcRenderer.drawn()) {
            try {
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
            GraftClient.View view = com.hex.kagune.control.client.GhoulNpcClient.graft(seen);
            Limbs limbs = LIMBS.computeIfAbsent(npc.m_20148_(), id -> new Limbs());
            limbs.seen = level.m_46467_();
            limbs.stage = GraftStage.TRANSPLANT;
            Frame frame = Frame.of(npc, partial);
            GraftRig.Body body = body(npc, partial, clock, frame, prey(level, npc, limbs, partial, frame));
            VertexConsumer out = consumer[0] == null ? (consumer[0] = buffers.m_6299_(TYPE)) : consumer[0];
            // 1.0.41: its tails turned over with it as it flips (carried on its torso, as a player's are). 1.3.35: in its own colour.
            tint = com.hex.kagune.control.KaguneTint.wild(seen.identity().seed());
            try {
                drawSet(out, matrix, normals, level, npc, view, GraftStage.TRANSPLANT, limbs.base, deploy, view, body, frame, camera, clock, partial,
                    seen.identity().lookMastery(), distance <= DETAIL_DISTANCE_SQR, null,
                    com.hex.kagune.control.client.GhoulNpcClient.flip(seen, clock, npc.m_20096_()));
            } finally {
                tint = com.hex.kagune.control.KaguneColours.DEFAULT;
            }
            } catch (RuntimeException failure) {
                LIMBS.remove(npc.m_20148_());
                PlayerLayers.failed("a wild ghoul's tails", failure);
            }
        }
        // 1.3.43: the ghoul children in view with a Rinkaku tentacle of their own. 1.3.44 (the owner: "one tentacle, not two", and not the
        // graft's toothed tails): one, drawn as a Rinkaku player's is (the original's tube, KaguneWorldRenderer.emitTubeLegacy), curled over
        // its head at rest as a player's first tentacle is, lashing on the beats of its string, at its size, in the colour it took.
        for (com.hex.kagune.control.npc.ChildNpc child : ChildRenderer.drawn()) {
            com.hex.kagune.control.client.ChildClient.View kid = com.hex.kagune.control.client.ChildClient.get(child.m_19879_());
            com.hex.kagune.control.client.GhoulNpcClient.View seen = com.hex.kagune.control.client.GhoulNpcClient.get(child.m_19879_());
            if (kid == null || seen == null || !com.hex.kagune.control.family.KaguneGenes.tails(kid.genome().kagune()) || !child.m_6084_()
                || child.m_20145_() || child.m_5803_()) {
                continue;
            }
            float deploy = com.hex.kagune.control.client.GhoulNpcClient.deploy(seen, clock);
            if (deploy <= 0.005F) {
                LIMBS.remove(child.m_20148_());
                continue;
            }
            if (child.m_20280_(self) > MAX_DISTANCE_SQR) {
                continue;
            }
            Limbs limbs = LIMBS.computeIfAbsent(child.m_20148_(), id -> new Limbs());
            limbs.seen = level.m_46467_();
            limbs.stage = GraftStage.TRANSPLANT;
            Frame frame = Frame.of(child, partial, com.hex.kagune.control.family.ChildKaguneRules.scale(kid.stage()));
            GraftRig.Body body = body(child, partial, clock, frame, prey(level, child, limbs, partial, frame));
            VertexConsumer out = consumer[0] == null ? (consumer[0] = buffers.m_6299_(TYPE)) : consumer[0];
            drawTentacle(out, matrix, normals, level, child, kid, com.hex.kagune.control.client.GhoulNpcClient.graft(seen), limbs, deploy, body, frame,
                camera, clock, partial);
        }
        if (consumer[0] != null) {
            buffers.m_109912_(TYPE);
        }
        if (level.m_46467_() % 40L == 0L) {
            long now = level.m_46467_();
            LIMBS.values().removeIf(limbs -> now - limbs.seen > 100L);
            TENTACLES.keySet().removeIf(id -> level.m_6815_(id) == null);
        }
    }

    /** 1.3.44: each child's tentacle as the original's tube needs it (its own, so its rings keep their turn from frame to frame). */
    private static final Map<Integer, com.hex.kagune.client.render.TendrilChain> TENTACLES = new HashMap<>();

    /**
     * 1.3.44: a ghoul child's one Rinkaku tentacle (ChildKaguneRules): at rest the curl a Rinkaku player's first tentacle has, swaying;
     * in a lash the move's path (GraftRig, one limb, on the beats the server strikes on) moved onto its pelvis; followed by springs (the
     * same as every limb's, GraftMotion), grown out as far as it is, and drawn by the original's own tube (its taper, surface, veins and
     * point) in the child's colour and its veins, as thick as a Rinkaku player's first tentacle at the child's size.
     */
    private static void drawTentacle(VertexConsumer out, Matrix4f matrix, Matrix3f normals, ClientLevel level, com.hex.kagune.control.npc.ChildNpc child,
                                     com.hex.kagune.control.client.ChildClient.View kid, GraftClient.View moving, Limbs limbs, double grown,
                                     GraftRig.Body body, Frame frame, Vec3 camera, double clock, float partial) {
        int mastery = com.hex.kagune.control.family.ChildKaguneRules.tailMastery(kid.stage());
        double length = GraftStage.TRANSPLANT.length(mastery);
        GraftMove move = moving.move();
        double age = moving.age(clock);
        if (age > move.ticks() + 2) {
            move = GraftMove.NONE;
        }
        // 1.3.46: over the shoulder it was born to carry it over, or as a tail.
        com.hex.kagune.control.family.ChildKaguneRules.Layout layout = com.hex.kagune.control.family.ChildKaguneRules.layout(kid.genome().kagune(),
            kid.genome().seed());
        GraftRig.Pose pose;
        if (move == GraftMove.NONE) {
            double[][] rest = com.hex.kagune.control.family.ChildKaguneRules.tentacleRest(length, clock, child.m_19879_(), GraftMotion.POINTS, layout);
            pose = new GraftRig.Pose(rest, new double[] {0.0, 1.0, 0.0}, 0.0, 1.0, length, 1.0, 0.0);
        } else {
            double[] aim = frame.rig(new double[] {moving.x(), moving.y(), moving.z()});
            if (move.follows() && moving.targets()[0] != 0) {
                Entity followed = level.m_6815_(moving.targets()[0]);
                if (followed != null) {
                    aim = frame.rig(centre(followed, partial));
                }
            }
            // 1.3.48: a grab's tentacle wound round what it holds (the server holds it there).
            double[] held = null;
            double heldRadius = 0.3;
            Entity grabbed = move.holds() && moving.targets()[0] != 0 ? level.m_6815_(moving.targets()[0]) : null;
            if (grabbed != null) {
                held = frame.rig(centre(grabbed, partial));
                heldRadius = grabbed.m_20205_() * 0.5 / frame.scale() + 0.15;
            }
            GraftRig.Pose lash = GraftRig.pose(new GraftRig.Input(GraftStage.TRANSPLANT, com.hex.kagune.control.family.ChildKaguneRules.TENTACLES, 0, length,
                body, move, age, aim, null, held, heldRadius));
            // The move's path, carried from the graft's root onto the pelvis it leaves from.
            double[] from = GraftRig.root(GraftStage.TRANSPLANT, 1, 0, body.crouching());
            double[] to = com.hex.kagune.control.family.ChildKaguneRules.tentacleRoot(layout);
            double[][] points = new double[lash.points().length][];
            for (int i = 0; i < points.length; i++) {
                double f = 1.0 - i / (double)Math.max(1, points.length - 1);
                double[] p = lash.points()[i];
                points[i] = new double[] {p[0] + (to[0] - from[0]) * f, p[1] + (to[1] - from[1]) * f, p[2] + (to[2] - from[2]) * f};
            }
            pose = new GraftRig.Pose(points, lash.dorsal(), 0.0, 1.0, length, lash.response(), 0.0);
        }
        if (limbs.base[0] == null) {
            limbs.base[0] = new GraftMotion.Limb();
        }
        GraftMotion.Limb leg = limbs.base[0];
        leg.follow(pose, clock);
        double[][] rig = leg.points();
        // Evenly along its curve, only as much as has grown out of its back.
        int samples = 64;
        double[][] dense = new double[samples + 1][];
        double[] along = new double[samples + 1];
        for (int i = 0; i <= samples; i++) {
            dense[i] = CentipedeTail.catmullRom(rig, i / (double)samples);
            along[i] = i == 0 ? 0.0 : along[i - 1] + GraftMesh.length3(GraftMesh.sub(dense[i], dense[i - 1]));
        }
        double shown = along[samples] * Math.max(0.04, Math.min(1.0, grown));
        int count = 28;
        Vec3[] path = new Vec3[count];
        int j = 0;
        for (int i = 0; i < count; i++) {
            double want = shown * i / (count - 1);
            while (j < samples - 1 && along[j + 1] < want) {
                j++;
            }
            double span = along[j + 1] - along[j];
            double f = span < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, (want - along[j]) / span));
            double[] p = new double[3];
            for (int k = 0; k < 3; k++) {
                p[k] = dense[j][k] + (dense[j + 1][k] - dense[j][k]) * f;
            }
            double[] world = frame.place(p, camera);
            path[i] = new Vec3(world[0], world[1], world[2]);
        }
        int colour = kid.genome().colour() < 0 ? com.hex.kagune.control.KaguneColours.DEFAULT : kid.genome().colour() & 0xFFFFFF;
        float size = (float)frame.scale();
        com.hex.kagune.client.KaguneClientState.View look = new com.hex.kagune.client.KaguneClientState.View(true, true, 0, 100.0F, colour,
            com.hex.kagune.control.KaguneColours.veinOf(colour), com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, 1, 0L, 1.0F, 1.0F, 1, 1, 0,
            com.hex.kagune.gameplay.TendrilMode.IDLE, 0L, 0, 0, 0, 1, 1, Vec3.f_82478_, 0L, 0, -1, 0, Vec3.f_82478_,
            com.hex.kagune.gameplay.CentipedeTechnique.STRIKE, 0L, 0, -1, Vec3.f_82478_, new int[6], (float)(length * size),
            com.hex.kagune.control.family.ChildKaguneRules.TENTACLE_THICKNESS * size, 0.46F, 6.0F, 10.0F, 8.5F, 5.0F, false, false, 0, 0, 0);
        com.hex.kagune.client.render.TendrilChain chain = TENTACLES.computeIfAbsent(child.m_19879_(), id -> new com.hex.kagune.client.render.TendrilChain(0));
        int light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(child.m_20185_(), child.m_20186_() + 1.0, child.m_20189_()));
        // 2.0.39: as a Rinkaku player's tail is drawn now (RinkakuTube: the owner's reference, in the child's own colour).
        RinkakuTube.emit(out, matrix, normals, path, look.thickness(), colour, light, child.m_19879_(), new double[] {0.0, 0.0, 0.0}, clock);
    }

    /** Every limb of {@code stage}, grown {@code grown} of the way out, making {@code moving}'s move (null: at rest). */
    private static void drawSet(VertexConsumer out, Matrix4f matrix, Matrix3f normals, ClientLevel level, LivingEntity player,
                                GraftClient.View view, GraftStage stage, GraftMotion.Limb[] motion, double grown, GraftClient.View moving,
                                GraftRig.Body body, Frame frame, Vec3 camera, double clock, float partial, int mastery, boolean fine,
                                double[] eyeRig, com.hex.kagune.control.client.animation.RigPose posed) {
        int n = Math.min(motion.length, stage.limbs(mastery));
        double length = stage.length(mastery);
        double width = stage.width(mastery) * frame.scale(); // 1.3.43: a child's tails as thick as they are long, at its size
        GraftMove move = moving == null ? GraftMove.NONE : moving.move();
        double age = moving == null ? 0.0 : moving.age(clock);
        if (moving != null && age > move.ticks() + 2) {
            move = GraftMove.NONE;
        }
        double[] aim = moving == null || move == GraftMove.NONE ? null : frame.rig(new double[] {moving.x(), moving.y(), moving.z()});
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
        GraftMove job = moving == null || moving.job().stage() != stage ? GraftMove.NONE : moving.job();
        double jobAge = job == GraftMove.NONE ? 0.0 : moving.jobAge(clock);
        if (job != GraftMove.NONE && jobAge > job.ticks() + 2) {
            job = GraftMove.NONE;
        }
        double[] jobHeld = null;
        if (job != GraftMove.NONE && moving.jobHeld() != 0) {
            Entity entity = level.m_6815_(moving.jobHeld());
            if (entity != null) {
                jobHeld = frame.rig(centre(entity, partial));
            }
        }
        int light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(player.m_20185_(), player.m_20186_() + 1.0, player.m_20189_()));
        for (int limb = 0; limb < n; limb++) {
            double[] target = null;
            if (moving != null && move != GraftMove.NONE && limb < moving.targets().length && moving.targets()[limb] != 0) {
                Entity entity = level.m_6815_(moving.targets()[limb]);
                if (entity != null) {
                    target = frame.rig(centre(entity, partial));
                }
            }
            boolean multi = move != GraftMove.NONE && (move.chases() || move.beats(n).stream().anyMatch(b -> b.has(GraftMove.MULTI)));
            GraftRig.Pose pose = GraftRig.pose(new GraftRig.Input(stage, n, limb, length * (0.55 + 0.45 * grown), body, move, age, aim,
                multi ? target : null, held, heldRadius, job, jobAge, jobHeld));
            if (motion[limb] == null) {
                motion[limb] = new GraftMotion.Limb();
            }
            GraftMotion.Limb leg = motion[limb];
            leg.follow(pose, clock);
            double[][] rig = com.hex.kagune.control.client.animation.GraftPose.ride(posed, leg.points());
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
            int side = GraftRig.side(stage, n, limb);
            double[] dorsal = frame.turn(leg.dorsal());
            KakujaModel.Spine spine = CentipedeTail.through(world, fine ? 72 : 36, side, dorsal);
            double[] tip = world[world.length - 1];
            // The eyes on what the move is aimed at, or at rest on the prey.
            double[] seen = aim != null ? aim : body.prey();
            double[] gaze = seen == null ? null : GraftModel.gazeToward(tip, frame.place(seen, camera));
            double swell = grown < 0.35 ? 1.0 + 0.4 * Math.sin(Math.PI * grown / 0.35) : 1.0;
            GraftModel.Look look = new GraftModel.Look(width * (0.45 + 0.55 * grown) * swell, fine, 1.0, player.m_19879_() * 7 + limb * 13 + 3, leg.jaw(),
                leg.flare() * (0.4 + 0.6 * grown), leg.stare(), GraftRig.maw(stage, limb), clock, gaze,
                move.guards() && GraftRig.cocoon(stage));
            GraftModel.limb((a, b, c, d, nn, ca, cb, cc, cd) -> quad(out, matrix, normals, a, b, c, d, nn, ca, cb, cc, cd, light), stage, spine, look);
        }
        if (grown < 0.35 && grown > 0.01) {
            // Before anything shows: muscle heaving under the skin of his lower back.
            double heave = Math.sin(Math.PI * grown / 0.35);
            double[] root = frame.place(com.hex.kagune.control.client.animation.GraftPose.ride(posed,
                new double[][] {GraftRig.root(stage, n, 0, body.crouching())})[0], camera);
            if (eyeRig == null) {
                GraftMesh.blob((a, b, c, d, nn, ca, cb, cc, cd) -> quad(out, matrix, normals, a, b, c, d, nn, ca, cb, cc, cd, light), root,
                    frame.turn(new double[] {1.0, 0.0, 0.0}), new double[] {0.0, 1.0, 0.0}, frame.turn(new double[] {0.0, 0.0, 1.0}), 0.22 * heave * frame.scale(),
                    0.18 * heave * frame.scale(), 0.14 * heave * frame.scale(), 5, 8, 0.25, player.m_19879_(), v -> 0.4 + 0.4 * v, 0x8A1820, 0x3A050C, 1.0);
            }
        }
    }

    /** The limb's points with those within reach of the eyes dropped (null if too little is left to draw). */
    private static double[][] clearOfEyes(double[][] world, double[] eye) {
        int first = 0;
        while (first < world.length && GraftMesh.length3(GraftMesh.sub(world[first], eye)) < FIRST_PERSON_CLEAR) {
            first++;
        }
        if (world.length - first < 3) {
            return null;
        }
        return first == 0 ? world : java.util.Arrays.copyOfRange(world, first, world.length);
    }

    /** How he moves this frame (rig space: his speed, crouch, rise and fall, where he looks). */
    /**
     * The prey his mouths are after (rig space, its middle): the nearest living thing within {@link GraftRig#PREY_RANGE} of him,
     * looked for again every tick, or null.
     */
    private static double[] prey(ClientLevel level, LivingEntity player, Limbs limbs, float partial, Frame frame) {
        long tick = level.m_46467_();
        if (tick != limbs.preySought) {
            limbs.preySought = tick;
            limbs.prey = 0;
            double best = GraftRig.PREY_RANGE * GraftRig.PREY_RANGE;
            for (LivingEntity body : level.m_6443_(LivingEntity.class, player.m_20191_().m_82400_(GraftRig.PREY_RANGE),
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

    private static GraftRig.Body body(LivingEntity player, float partial, double clock, Frame frame, double[] prey) {
        double speed = Math.hypot(player.m_20185_() - player.f_19790_, player.m_20189_() - player.f_19792_);
        double vertical = player.m_20186_() - player.f_19791_;
        double headYaw = player.f_20886_ + wrap(player.f_20885_ - player.f_20886_) * partial;
        double lookYaw = Math.toRadians(wrap(headYaw - frame.bodyYaw()));
        double lookPitch = Math.toRadians(player.m_146909_());
        return new GraftRig.Body(speed, player.m_6144_(), vertical, player.m_20096_(), lookYaw, lookPitch, clock, prey);
    }

    /** An entity's middle, interpolated for this frame. */
    private static double[] centre(Entity entity, float partial) {
        return new double[] {entity.f_19790_ + (entity.m_20185_() - entity.f_19790_) * partial,
            entity.f_19791_ + (entity.m_20186_() - entity.f_19791_) * partial + entity.m_20206_() * 0.52,
            entity.f_19792_ + (entity.m_20189_() - entity.f_19792_) * partial};
    }

    /**
     * A player's rig frame for this frame: feet in the world, and the body's forward and right; 1.3.43: and its size (a ghoul child's tails
     * are a grown ghoul's rig drawn at its size: every rig point placed {@code scale} as far from its feet, every world point read back).
     */
    private record Frame(double x, double y, double z, double forwardX, double forwardZ, double rightX, double rightZ, double bodyYaw, double scale) {
        static Frame of(LivingEntity player, float partial) {
            return of(player, partial, 1.0);
        }

        static Frame of(LivingEntity player, float partial, double scale) {
            double feetX = player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial;
            double feetY = player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial;
            double feetZ = player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial;
            float previousYaw = player.f_20884_;
            double bodyYaw = previousYaw + wrap(player.f_20883_ - previousYaw) * partial;
            double yaw = Math.toRadians(bodyYaw);
            return new Frame(feetX, feetY, feetZ, -Math.sin(yaw), Math.cos(yaw), -Math.cos(yaw), -Math.sin(yaw), bodyYaw, scale);
        }

        /** A rig point in the world, relative to the camera. */
        double[] place(double[] rig, Vec3 camera) {
            double s = this.scale;
            return new double[] {this.x + (this.rightX * rig[0] + this.forwardX * rig[2]) * s - camera.f_82479_, this.y + rig[1] * s - camera.f_82480_,
                this.z + (this.rightZ * rig[0] + this.forwardZ * rig[2]) * s - camera.f_82481_};
        }

        /** A rig direction in the world. */
        double[] turn(double[] rig) {
            return new double[] {this.rightX * rig[0] + this.forwardX * rig[2], rig[1], this.rightZ * rig[0] + this.forwardZ * rig[2]};
        }

        /** A world point in the rig. */
        double[] rig(double[] world) {
            double dx = (world[0] - this.x) / this.scale;
            double dz = (world[2] - this.z) / this.scale;
            return new double[] {dx * this.rightX + dz * this.rightZ, (world[1] - this.y) / this.scale, dx * this.forwardX + dz * this.forwardZ};
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

    /** 1.3.35: the colour the limbs being drawn now are turned to (a wild ghoul's own, KaguneTint.wild; the render thread's alone). */
    private static int tint = com.hex.kagune.control.KaguneColours.DEFAULT;

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals, double[] p, double[] n, int argb, int light) {
        int colour = com.hex.kagune.control.KaguneTint.recolour(argb, tint);
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
