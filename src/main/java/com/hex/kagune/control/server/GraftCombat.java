package com.hex.kagune.control.server;

import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftMutation;
import com.hex.kagune.control.graft.GraftRules;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.control.wound.WoundBook;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.effect.KaguneEffects;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Graft DNA's moves on the server (7.0.0): each move's {@link GraftMove.Beat}s, tick by tick, exactly as every client draws them
 * ({@code GraftRig}): a thrust hurts on its landing tick along the line its limb shoots out on, a sweep hurts what its limb passes
 * through as it passes, a slam hurts round where it hits the ground (and breaks it, every block put back later by {@link BlockSmash}),
 * a seizing limb carries what it caught at its end until it throws it. Damage, bleeding, stuns, knockback and broken blocks are all
 * decided here; clients only see them ({@code S2CPaleFx}, the same impact effects as every path's).
 */
final class GraftCombat {
    /** How long a thrown body smashes through what is in its way (ticks). */
    static final int THROWN = 26;

    private GraftCombat() {
    }

    // ---------------------------------------------------------------- starting moves

    /** True while the current move's blows are still to land (a new move waits for it). */
    static boolean committed(GraftServer.GraftState state, long now) {
        if (state.move == GraftMove.NONE) {
            return false;
        }
        if (state.move.holds() && state.heldId != 0 && !state.move.followUp() || state.move.guards() || state.move.travels()) {
            return false; // a hold, a guard or a dash can be cut short by anything (7.0.2: not what follows on from a hold)
        }
        long age = now - state.startedAt;
        int last = 0;
        for (GraftMove.Beat beat : state.move.beats(4)) {
            last = Math.max(last, beat.kind() == GraftMove.Kind.SWEEP || beat.kind() == GraftMove.Kind.SPIN || beat.kind() == GraftMove.Kind.DIVE
                ? beat.end() : beat.land());
        }
        return age <= last;
    }

    /** The limbs out now. */
    static int limbs(ServerPlayer player) {
        return GraftServer.stage(player).limbs(KaguneProfile.mastery(player));
    }

    /** M1: the next strike of the string (a click within the chain window goes on; later, it starts again). */
    static void strike(ServerPlayer player, GraftServer.GraftState state, Entity hint, long now) {
        GraftStage stage = GraftServer.stage(player);
        if (stage.kakuja() && !GraftServer.kakujaReady(state, now)) {
            return;
        }
        boolean chained = state.lastStrike != null && now <= state.strikeEndsAt + GraftRules.CHAIN_WINDOW;
        GraftMove move = chained ? GraftMove.nextStrike(state.lastStrike, stage) : GraftMove.firstStrike(stage);
        if (start(player, state, move, hint, now)) {
            state.lastStrike = move;
            state.strikeEndsAt = now + move.ticks();
            // 1.0.40: on its way: another's M1 landing on him before its first blow lands meets it (KaguneClash), and both stop.
            int lands = move.ticks();
            double reach = 3.0;
            for (GraftMove.Beat beat : move.beats(limbs(player))) {
                lands = Math.min(lands, beat.land());
                reach = Math.max(reach, beat.reach());
            }
            long began = state.startedAt;
            KaguneClash.swing(player, now, lands + 1, reach, () -> state.move == move && state.startedAt == began, () -> clashed(player, state));
        }
    }

    /** 1.0.40: his blow met another's coming back (KaguneClash): cut off, nothing queued after it. */
    private static void clashed(ServerPlayer player, GraftServer.GraftState state) {
        state.queuedAt = Long.MIN_VALUE;
        state.queuedSlot = null;
        end(player, state);
    }

    /**
     * M1 with a sword or an axe in hand: his own blade, as Minecraft swings it (its damage, enchantments, sweep and cooldown), and
     * the kagune pierces what it cut a moment after. True if it was a blade's swing.
     */
    static boolean weaponStrike(ServerPlayer player, GraftServer.GraftState state, Entity hint, long now) {
        ItemStack held = player.m_21205_();
        if (!(held.m_41720_() instanceof SwordItem) && !(held.m_41720_() instanceof AxeItem)) {
            return false;
        }
        if (GraftMasses.sword(player, state, now)) {
            return true; // 7.0.2: the King's Game: he rushes in on its prey
        }
        if (hint instanceof LivingEntity living && hint != player && living.m_6084_() && living.m_20280_(player) <= 36.0) {
            player.m_5706_(hint);
        }
        player.m_21011_(InteractionHand.MAIN_HAND, true);
        if (!committed(state, now) && hint instanceof LivingEntity living && KaguneServerController.validSwipeTarget(player, living)) {
            start(player, state, GraftMove.ACCORD, hint, now);
        }
        return true;
    }

    /**
     * Starts {@code move} if it is unlocked, off cooldown and paid for: its aim (the hinted body, or the nearest in front, or where
     * he looks), each limb's own target for the moves that split, then every client told.
     */
    static boolean start(ServerPlayer player, GraftServer.GraftState state, GraftMove move, Entity hint, long now) {
        GraftStage stage = GraftServer.stage(player);
        if (stage.kakuja() && !GraftServer.kakujaReady(state, now)) {
            return false;
        }
        if (GraftServer.locked(player, state, move.displayName(), move.mastery()) || !GraftTails.ready(player, state, move)) {
            return false;
        }
        Long ready = state.readyAt.get(move);
        if (ready != null && now < ready) {
            GraftServer.notice(player, state, Component.m_237110_("message.hexkagune.graft.cooldown", (ready - now + 19) / 20));
            return false;
        }
        // 7.0.2: the King's Game needs its prey before it starts.
        LivingEntity prey = move == GraftMove.KINGS_GAME ? aim(player, hint, GraftRules.GAME_RANGE)
            : move == GraftMove.PERFORMANCE ? aim(player, hint, GraftRules.PERFORMANCE_RANGE) : null;
        if (move == GraftMove.KINGS_GAME && prey == null) {
            GraftServer.notice(player, state, Component.m_237115_("message.hexkagune.graft.no_prey"));
            return false;
        }
        if (move.cost() > 0.0F && !KaguneServerController.useEnergy(player, move.cost(), true)) {
            return false;
        }
        if (move.job()) {
            // 7.0.2: a job runs alongside whatever he is doing: the move in hand goes on.
            GraftCentipedes.beginJob(player, state, move, prey, now);
            state.readyAt.put(move, now + Cooldowns.of(player, (long)move.cooldown()));
            whip(player, move);
            return true;
        }
        release(player, state, false);
        double reach = GraftTails.seek(move, reach(move, limbs(player)));
        LivingEntity target = aim(player, hint, reach);
        Vec3 point;
        if (target != null) {
            point = KaguneServerController.center(target);
        } else {
            point = lookPoint(player, reach);
        }
        java.util.Arrays.fill(state.targets, 0);
        if (target != null) {
            state.targets[0] = target.m_19879_();
        }
        List<GraftMove.Beat> beats = move.beats(limbs(player));
        boolean multi = beats.stream().anyMatch(b -> b.has(GraftMove.MULTI));
        if (multi) {
            List<LivingEntity> picked = pick(player, reach, target, state.targets.length);
            for (int i = 0; i < state.targets.length; i++) {
                state.targets[i] = picked.isEmpty() ? 0 : picked.get(i % picked.size()).m_19879_();
            }
        }
        begin(player, state, move, now, player.m_146908_(), point);
        state.readyAt.put(move, now + Cooldowns.of(player, (long)move.cooldown()));
        whip(player, move);
        return true;
    }

    /** {@code move} begins now, thrown the way {@code yaw} faces at {@code point}; every client told. */
    static void begin(ServerPlayer player, GraftServer.GraftState state, GraftMove move, long now, float yaw, Vec3 point) {
        GraftCentipedes.yield(player, state, move);
        state.move = move;
        state.startedAt = now;
        state.yaw = yaw;
        state.point = point == null ? Vec3.f_82478_ : point;
        state.hits.clear();
        GraftServer.broadcast(player, state);
    }

    /** Back to rest, every client told; what is held is let go ({@code thrown}: it was thrown, keep its flight). */
    static void end(ServerPlayer player, GraftServer.GraftState state) {
        release(player, state, false);
        state.move = GraftMove.NONE;
        GraftServer.broadcast(player, state);
    }

    /** Lets go of what the limbs hold (dropped where it is, its fall forgotten so far, unless it was just {@code thrown}). */
    static void release(ServerPlayer player, GraftServer.GraftState state, boolean thrown) {
        if (state.heldId != 0 && !thrown) {
            Entity entity = player.m_9236_().m_6815_(state.heldId);
            if (entity != null) {
                entity.m_20256_(Vec3.f_82478_);
                entity.f_19864_ = true;
                entity.f_19789_ = 0.0F;
            }
        }
        state.heldId = 0;
    }

    /** M2: the dash, {@code way} 0 ahead, 1 back, 2 left, 3 right; the limbs plant behind and throw him. */
    static void step(ServerPlayer player, GraftServer.GraftState state, int way, long now) {
        if (GraftTails.step(player, state, now)) {
            return; // 7.0.2: Human Puppet's pull
        }
        if (state.move.travels() && now - state.startedAt < 4) {
            return;
        }
        GraftStage stage = GraftServer.stage(player);
        if (stage.kakuja() && !GraftServer.kakujaReady(state, now)) {
            return;
        }
        Long ready = state.readyAt.get(GraftMove.STEP);
        if (ready != null && now < ready || !KaguneServerController.useEnergy(player, GraftMove.STEP.cost(), true)) {
            return;
        }
        release(player, state, false);
        double turn = switch (way) {
            case 1 -> 180.0;
            case 2 -> -90.0;
            case 3 -> 90.0;
            default -> 0.0;
        };
        float yaw = (float)(player.m_146908_() + turn);
        double rad = Math.toRadians(yaw);
        Vec3 dir = new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
        double speed = GraftRules.STEP_SPEED * (stage.kakuja() ? 1.3 : 1.0) * (0.9 + 0.2 * stage.ordinal() / 4.0);
        UkakuFire.push(player, dir.m_82490_(speed).m_82520_(0.0, GraftRules.STEP_LIFT, 0.0));
        state.fallSafe = true;
        begin(player, state, GraftMove.STEP, now, yaw, player.m_20182_().m_82549_(dir.m_82490_(4.0)));
        state.readyAt.put(GraftMove.STEP, now + Cooldowns.of(player, (long)GraftMove.STEP.cooldown()));
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.7F, KaguneServerController.vary(player, 1.3F, 0.08F));
    }

    // ---------------------------------------------------------------- the tick

    static void tick(ServerPlayer player, GraftServer.GraftState state, long now) {
        GraftCentipedes.job(player, state, now);
        GraftMove move = state.move;
        if (move == GraftMove.NONE) {
            drainQueue(player, state, now);
            return;
        }
        long age = now - state.startedAt;
        int limbs = limbs(player);
        List<GraftMove.Beat> beats = move.beats(limbs);
        for (int i = 0; i < beats.size(); i++) {
            if (GraftCentipedes.taken(state, beats.get(i), now, limbs)) {
                continue; // 7.0.2: that limb holds what the job caught
            }
            beat(player, state, beats.get(i), i, age, now);
            if (state.move != move) {
                return; // the beat ended the move (a miss, a throw)
            }
        }
        travel(player, state, move, age, now);
        GraftTails.tick(player, state, move, age, now);
        if (state.move == move) {
            GraftCentipedes.tick(player, state, move, age, now);
        }
        if (state.move == move) {
            GraftThorns.tick(player, state, move, age, now);
        }
        if (state.move == move) {
            GraftMasses.tick(player, state, move, age, now);
        }
        if (state.move == move) {
            GraftFinal.tick(player, state, move, age, now);
        }
        if (state.move != move) {
            return; // it ended itself (the dive's impact)
        }
        if (age >= move.ticks()) {
            GraftTails.expired(player, state, move, now);
            end(player, state);
            drainQueue(player, state, now);
        } else if (!committed(state, now)) {
            drainQueue(player, state, now);
        }
    }

    /** A click or a key that came while the limbs were committed: now, if it is still fresh. */
    private static void drainQueue(ServerPlayer player, GraftServer.GraftState state, long now) {
        if (state.queuedSlot != null) {
            GraftMove.Slot slot = state.queuedSlot;
            state.queuedSlot = null;
            if (now - state.queuedSlotAt <= GraftRules.BUFFER) {
                Entity hint = state.queuedHint == 0 ? null : player.m_284548_().m_6815_(state.queuedHint);
                GraftMove move = GraftMove.of(slot, GraftServer.stage(player));
                if (move != GraftMove.NONE) {
                    start(player, state, move, hint, now);
                }
            }
            return;
        }
        if (state.queuedAt != Long.MIN_VALUE) {
            long at = state.queuedAt;
            state.queuedAt = Long.MIN_VALUE;
            if (now - at <= GraftRules.BUFFER) {
                Entity hint = state.queuedHint == 0 ? null : player.m_284548_().m_6815_(state.queuedHint);
                strike(player, state, hint, now);
            }
        }
    }

    /** One beat at {@code age} ticks into its move. */
    private static void beat(ServerPlayer player, GraftServer.GraftState state, GraftMove.Beat beat, int index, long age, long now) {
        switch (beat.kind()) {
            case THRUST -> {
                if (age == beat.land()) {
                    thrust(player, state, beat, index, now);
                }
            }
            case UPPER -> {
                if (age == beat.land()) {
                    upper(player, state, beat, index, now);
                }
            }
            case BITE -> {
                if (age == beat.land()) {
                    bite(player, state, beat, index, now);
                }
            }
            case SWEEP -> {
                if (age >= beat.land() && age <= beat.end()) {
                    sweep(player, state, beat, index, age, now);
                }
            }
            case SPIN -> {
                if (age >= beat.land() && age <= beat.end()) {
                    spin(player, state, beat, index, age, now);
                }
            }
            case SLAM -> {
                if (age == beat.land()) {
                    slam(player, state, beat, index, now);
                }
            }
            case BURST -> {
                if (age == beat.land()) {
                    burst(player, state, beat, now);
                }
            }
            case SEIZE -> hold(player, state, beat, index, age, now);
            case HOOK -> GraftTails.hook(player, state, beat, index, age, now);
            case FLANK -> {
                if (age == beat.land()) {
                    GraftTails.flank(player, state, beat, index, now);
                }
            }
            case CLAW -> {
                if (age == beat.start()) {
                    // The hand that rakes: a swipe from his right is his right hand's, from his left his left's.
                    player.m_21011_(beat.from() < 0.0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, true);
                }
                if (age == beat.land()) {
                    claw(player, state, beat, index, now);
                }
            }
            case CURL, PLANT, WHIP, LURK, DIVE, CHASE, CLAMP, CRAWL, STORM, CURTAIN, SOAR -> {
            }
        }
    }

    /** What moves him beyond his own legs in the strings: the kakuja's lunge behind its jaws. */
    private static void travel(ServerPlayer player, GraftServer.GraftState state, GraftMove move, long age, long now) {
        if (move == GraftMove.CLAW_5 && age == 2) {
            UkakuFire.push(player, UkakuFire.horizontal(player.m_20154_()).m_82490_(0.95).m_82520_(0.0, 0.12, 0.0));
        }
    }

    // ---------------------------------------------------------------- the blows

    /** A limb shot straight at its point: what is at the point (or, piercing, everything along its line) is run through. */
    private static void thrust(ServerPlayer player, GraftServer.GraftState state, GraftMove.Beat beat, int index, long now) {
        Vec3 from = shoulder(player, state, beat.limb());
        Vec3 to = strikePoint(player, state, beat);
        Set<Integer> hit = hitSet(state, index);
        Vec3 way = to.m_82546_(from);
        Vec3 dir = way.m_82556_() < 1.0E-6 ? UkakuFire.horizontal(player.m_20154_()) : way.m_82541_();
        double length = way.m_82553_();
        AABB area = new AABB(from.f_82479_, from.f_82480_, from.f_82481_, to.f_82479_, to.f_82480_, to.f_82481_).m_82400_(1.5);
        int landed = 0;
        for (LivingEntity body : player.m_284548_().m_6443_(LivingEntity.class, area,
            living -> living != player && KaguneServerController.validSwipeTarget(player, living))) {
            if (hit.contains(body.m_19879_())) {
                continue;
            }
            Vec3 centre = KaguneServerController.center(body);
            double along = centre.m_82546_(from).m_82526_(dir);
            double near = beat.has(GraftMove.PIERCE) ? -1.0 : length - 2.2;
            if (along < near || along > length + 1.0) {
                continue;
            }
            Vec3 closest = from.m_82549_(dir.m_82490_(Math.max(0.0, Math.min(length, along))));
            if (closest.m_82554_(centre) > body.m_20205_() * 0.5 + 0.7 || !KaguneServerController.clearLineTo(player, body)) {
                continue;
            }
            hit.add(body.m_19879_());
            strikeBody(player, state, body, beat.power(), dir, beat.flags(), now, WoundBook.Blow.STAB);
            PaleBlitz.fx(player, S2CPaleFx.IMPALED, body.m_19879_(), centre, dir, body.m_20206_(), 0);
            landed++;
        }
        if (beat.has(GraftMove.CRATER) && landed == 0) {
            // The lance drives on into the ground or the wall where it ends.
            BlockHitResult block = player.m_284548_().m_45547_(new ClipContext(from, to.m_82549_(dir.m_82490_(1.0)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
            if (block.m_6662_() == HitResult.Type.BLOCK) {
                BlockSmash.crater(player, block.m_82450_(), 1.4);
                ground(player, block.m_82450_(), 1.6F);
            }
        }
        KaguneServerController.playAt(player, to, landed > 0 ? HexKaguneSounds.FLESH_IMPACT : HexKaguneSounds.KAGUNE_WHIP, 0.8F,
            KaguneServerController.vary(player, 1.0F, 0.08F));
    }

    /** The mouth snapped shut on its point: torn, bleeding, dragged in a step; a kill feeds him. */
    private static void bite(ServerPlayer player, GraftServer.GraftState state, GraftMove.Beat beat, int index, long now) {
        // The kakuja's own jaws bite just ahead of his face; a limb's mouth bites at its point.
        Vec3 at = beat.limb() == GraftMove.ARM ? player.m_20182_().m_82520_(0.0, 1.5, 0.0).m_82549_(UkakuFire.horizontal(player.m_20154_()).m_82490_(2.0))
            : strikePoint(player, state, beat);
        Set<Integer> hit = hitSet(state, index);
        LivingEntity caught = nearest(player, at, 1.6, hit);
        KaguneServerController.playAt(player, at, HexKaguneSounds.BONE_CRUSH, 1.0F, KaguneServerController.vary(player, 0.9F, 0.08F));
        if (caught == null) {
            return;
        }
        hit.add(caught.m_19879_());
        Vec3 toward = player.m_20182_().m_82546_(caught.m_20182_());
        Vec3 pull = toward.m_82556_() < 1.0E-6 ? Vec3.f_82478_ : UkakuFire.horizontal(toward).m_82490_(0.7);
        strikeBody(player, state, caught, beat.power(), pull, beat.flags(), now, WoundBook.Blow.CRUSH);
        PaleBlitz.fx(player, S2CPaleFx.CRUSHED, caught.m_19879_(), KaguneServerController.center(caught), pull, 1.0F, 0);
        boolean appetite = GraftServer.hasMutation(player, GraftMutation.RIZES_APPETITE);
        if (appetite) {
            SharedBar.feed(player, GraftMutation.APPETITE_BITE);
        }
        if (!caught.m_6084_()) {
            SharedBar.feed(player, appetite ? GraftMutation.APPETITE_KILL : 6.0F); // it ate
            player.m_5634_(appetite ? GraftMutation.APPETITE_HEAL : 2.0F);
        }
    }

    /** A limb swept round at its reach: what it passes through this tick is struck, thrown the way it swept. */
    private static void sweep(ServerPlayer player, GraftServer.GraftState state, GraftMove.Beat beat, int index, long age, long now) {
        double span = Math.max(1, beat.end() - beat.land());
        double before = beat.from() + (beat.to() - beat.from()) * smooth((age - 1 - beat.land()) / span);
        double after = beat.from() + (beat.to() - beat.from()) * smooth((age - beat.land()) / span);
        if (age == beat.land()) {
            before = beat.from();
        }
        double low = Math.min(before, after) - 12.0;
        double high = Math.max(before, after) + 12.0;
        double reach = reach(state.move, limbs(player));
        Set<Integer> hit = hitSet(state, index);
        double sign = Math.signum(beat.to() - beat.from());
        for (LivingEntity body : around(player, reach + 1.0)) {
            if (hit.contains(body.m_19879_())) {
                continue;
            }
            double bearing = bearing(player, state, body);
            double dy = body.m_20186_() + body.m_20206_() * 0.5 - (player.m_20186_() + beat.height());
            if (bearing < low || bearing > high || Math.abs(dy) > 2.2 + body.m_20206_() * 0.5 || !KaguneServerController.clearLineTo(player, body)) {
                continue;
            }
            if (beat.has(GraftMove.GATHER)) {
                hit.add(body.m_19879_());
                GraftThorns.gather(player, state, beat, body, now); // 7.0.2: the dragnet drags it in instead
                continue;
            }
            hit.add(body.m_19879_());
            // Thrown on round the way the limb swept: square to the line from him to it.
            Vec3 out = UkakuFire.horizontal(KaguneServerController.center(body).m_82546_(player.m_20182_()));
            Vec3 across = new Vec3(-out.f_82481_ * sign, 0.0, out.f_82479_ * sign);
            // 7.0.2: the execution's cuts throw nothing: what he circles stays where it is for the next one.
            Vec3 push = state.move == GraftMove.OWLS_EXECUTION ? Vec3.f_82478_ : across.m_82549_(out.m_82490_(0.3));
            strikeBody(player, state, body, beat.power(), push, beat.flags(), now, WoundBook.Blow.SLASH);
            PaleBlitz.fx(player, S2CPaleFx.IMPACT, body.m_19879_(), KaguneServerController.center(body), across, 1.0F, S2CPaleFx.BONE);
        }
    }

    /** Every limb out round him, spinning: each time round, everything within reach is struck once. */
    private static void spin(ServerPlayer player, GraftServer.GraftState state, GraftMove.Beat beat, int index, long age, long now) {
        double turned = (beat.to() - beat.from()) * (age - beat.land()) / Math.max(1.0, beat.end() - beat.land());
        int round = (int)Math.floor(turned / 360.0);
        Set<Integer> hit = hitSet(state, index * 100 + round);
        double reach = Math.min(beat.reach(), GraftServer.stage(player).length(KaguneProfile.mastery(player)) * 1.4) + 0.6;
        for (LivingEntity body : around(player, reach)) {
            if (hit.contains(body.m_19879_()) || !KaguneServerController.clearLineTo(player, body)) {
                continue;
            }
            hit.add(body.m_19879_());
            Vec3 out = UkakuFire.horizontal(KaguneServerController.center(body).m_82546_(player.m_20182_()));
            strikeBody(player, state, body, beat.power(), out, beat.flags(), now, WoundBook.Blow.SLASH);
            PaleBlitz.fx(player, S2CPaleFx.IMPACT, body.m_19879_(), KaguneServerController.center(body), out, 1.0F, S2CPaleFx.BONE);
        }
        if (age % 4 == 0) {
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.7F, KaguneServerController.vary(player, 1.1F, 0.1F));
        }
    }

    /** A limb driven into the ground: a crater, a shock, everything round it hurt and thrown up. */
    private static void slam(ServerPlayer player, GraftServer.GraftState state, GraftMove.Beat beat, int index, long now) {
        Vec3 at = groundPoint(player, state, beat);
        double radius = switch (state.move) {
            case LASH_5 -> 2.1;
            case THORN_5, RIP_5 -> 2.3;
            case CLAW_6 -> 2.8;
            case DRAGNET -> 2.8;
            default -> 2.4;
        };
        double shock = switch (state.move) {
            case THORN_5 -> 5.0;
            case CLAW_6 -> 5.5;

            default -> radius + 1.0;
        };
        Set<Integer> shared = hitSet(state, -1);
        Set<Integer> hit = hitSet(state, index);
        if (beat.has(GraftMove.CRATER)) {
            BlockSmash.crater(player, at, radius * 0.75);
        }
        ground(player, at, (float)shock);
        // One shockwave a move (several impacts share it); each impact hurts round itself.
        for (LivingEntity body : player.m_284548_().m_6443_(LivingEntity.class, new AABB(at.f_82479_, at.f_82480_, at.f_82481_, at.f_82479_, at.f_82480_, at.f_82481_).m_82400_(shock + 1.0),
            living -> living != player && KaguneServerController.validSwipeTarget(player, living))) {
            Vec3 centre = KaguneServerController.center(body);
            double distance = Math.hypot(centre.f_82479_ - at.f_82479_, centre.f_82481_ - at.f_82481_);
            if (distance > shock + body.m_20205_() * 0.5 || Math.abs(body.m_20186_() - at.f_82480_) > 3.0) {
                continue;
            }
            boolean direct = distance <= radius + body.m_20205_() * 0.5;
            if (direct ? hit.contains(body.m_19879_()) : shared.contains(body.m_19879_())) {
                continue;
            }
            (direct ? hit : shared).add(body.m_19879_());
            shared.add(body.m_19879_());
            Vec3 away = UkakuFire.horizontal(centre.m_82546_(at));
            float power = beat.power() * (direct ? 1.0F : (float)Math.max(0.35, 1.0 - distance / shock) * 0.6F);
            strikeBody(player, state, body, power, away.m_82490_(direct ? 0.6 : 0.9), beat.flags(), now, WoundBook.Blow.SLAM);
        }
        KaguneServerController.playAt(player, at, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.75F, 0.05F));
        UkakuFire.sound(player.m_284548_(), at, "entity.generic.explode", 0.6F, KaguneServerController.vary(player, 0.8F, 0.08F));
    }

    /** Ripped up from under it: what is at its point is run through from below and thrown high, bleeding. */
    private static void upper(ServerPlayer player, GraftServer.GraftState state, GraftMove.Beat beat, int index, long now) {
        Vec3 at = strikePoint(player, state, beat);
        Set<Integer> hit = hitSet(state, index);
        int landed = 0;
        for (LivingEntity body : player.m_284548_().m_6443_(LivingEntity.class, new AABB(at.f_82479_, at.f_82480_, at.f_82481_, at.f_82479_,
            at.f_82480_, at.f_82481_).m_82400_(2.2), living -> living != player && KaguneServerController.validSwipeTarget(player, living))) {
            if (landed >= 3 || !hit.add(body.m_19879_()) || !KaguneServerController.clearLineTo(player, body)) {
                continue;
            }
            Vec3 out = UkakuFire.horizontal(KaguneServerController.center(body).m_82546_(player.m_20182_()));
            strikeBody(player, state, body, beat.power(), out.m_82490_(0.25), beat.flags() | GraftMove.KNOCK_UP, now, WoundBook.Blow.STAB);
            body.m_20256_(body.m_20184_().m_82520_(0.0, 0.5, 0.0));
            body.f_19864_ = true;
            PaleBlitz.fx(player, S2CPaleFx.IMPALED, body.m_19879_(), KaguneServerController.center(body), new Vec3(0.0, 1.0, 0.0), body.m_20206_(), 0);
            landed++;
        }
        KaguneServerController.playAt(player, at, landed > 0 ? HexKaguneSounds.FLESH_IMPACT : HexKaguneSounds.KAGUNE_WHIP, 0.9F,
            KaguneServerController.vary(player, 0.9F, 0.08F));
    }

    /** Out of the curl, flung out all round: everything within reach struck once, thrown away from him. */
    private static void burst(ServerPlayer player, GraftServer.GraftState state, GraftMove.Beat beat, long now) {
        Set<Integer> hit = hitSet(state, -1);
        double reach = Math.min(beat.reach(), GraftServer.stage(player).length(KaguneProfile.mastery(player)) * 1.4) + 0.6;
        for (LivingEntity body : around(player, reach)) {
            if (hit.contains(body.m_19879_()) || !KaguneServerController.clearLineTo(player, body)) {
                continue;
            }
            hit.add(body.m_19879_());
            Vec3 out = UkakuFire.horizontal(KaguneServerController.center(body).m_82546_(player.m_20182_()));
            strikeBody(player, state, body, beat.power(), out.m_82490_(1.2), beat.flags(), now, WoundBook.Blow.STAB);
            PaleBlitz.fx(player, S2CPaleFx.IMPALED, body.m_19879_(), KaguneServerController.center(body), out, body.m_20206_(), 0);
        }
        KaguneServerController.playSelf(player, HexKaguneSounds.BONE_CRACK, 1.0F, KaguneServerController.vary(player, 0.8F, 0.1F));
    }

    /** His own claws: a short cone in front, up to three bodies. */
    private static void claw(ServerPlayer player, GraftServer.GraftState state, GraftMove.Beat beat, int index, long now) {
        Set<Integer> hit = hitSet(state, index);
        int landed = 0;
        for (LivingEntity body : around(player, beat.reach() + 0.5)) {
            if (landed >= 3 || hit.contains(body.m_19879_())) {
                continue;
            }
            double bearing = bearing(player, state, body);
            if (Math.abs(bearing) > 60.0 || !KaguneServerController.clearLineTo(player, body)) {
                continue;
            }
            hit.add(body.m_19879_());
            Vec3 way = UkakuFire.horizontal(KaguneServerController.center(body).m_82546_(player.m_20182_()));
            strikeBody(player, state, body, beat.power(), way.m_82490_(0.5), beat.flags(), now, WoundBook.Blow.SLASH);
            PaleBlitz.fx(player, S2CPaleFx.IMPACT, body.m_19879_(), KaguneServerController.center(body), way, 0.8F, S2CPaleFx.BONE);
            landed++;
        }
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.6F, KaguneServerController.vary(player, 1.4F, 0.1F));
    }

    /**
     * A seizing beat: on its landing tick the limb closes on (or runs through, or coils round) what is at its point; until it lets
     * go, what it caught is carried at its end (the coil squeezes every so often).
     */
    private static void hold(ServerPlayer player, GraftServer.GraftState state, GraftMove.Beat beat, int index, long age, long now) {
        if (age == beat.land() && !beat.has(GraftMove.CARRY)) {
            Vec3 at = strikePoint(player, state, beat);
            Entity aimed = state.targets[0] == 0 ? null : player.m_284548_().m_6815_(state.targets[0]);
            LivingEntity caught = aimed instanceof LivingEntity living && living.m_6084_() && KaguneServerController.center(living).m_82554_(at) <= 2.5
                ? living : nearest(player, at, 1.8, hitSet(state, index));
            if (caught == null) {
                KaguneServerController.playAt(player, at, HexKaguneSounds.KAGUNE_WHIP, 0.7F, KaguneServerController.vary(player, 0.9F, 0.05F));
                // Missed: the limb snaps back.
                state.startedAt = now - Math.max(0, state.move.ticks() - 6);
                GraftServer.broadcast(player, state);
                return;
            }
            hitSet(state, index).add(caught.m_19879_());
            state.heldId = caught.m_19879_();
            Vec3 into = UkakuFire.horizontal(KaguneServerController.center(caught).m_82546_(player.m_20182_()));
            strikeBody(player, state, caught, beat.power(), Vec3.f_82478_, beat.flags(), now, WoundBook.Blow.CRUSH);
            PaleBlitz.fx(player, S2CPaleFx.CRUSHED, caught.m_19879_(),
                KaguneServerController.center(caught), into, caught.m_20206_(), 0);
            KaguneServerController.playAt(player, caught, HexKaguneSounds.KAGUNE_GRAB, 1.0F, KaguneServerController.vary(player, 1.0F, 0.05F));
            GraftServer.broadcast(player, state);
            return;
        }
        if (age < beat.land() || age == beat.land() && !beat.has(GraftMove.CARRY) || age > beat.end() || state.heldId == 0) {
            return;
        }
        Entity entity = player.m_284548_().m_6815_(state.heldId);
        if (!(entity instanceof LivingEntity held) || !held.m_6084_() || held.m_20280_(player) > 20.0 * 20.0) {
            release(player, state, false);
            GraftServer.broadcast(player, state);
            return;
        }
        if (!PaleStun.stun(held, now, 6)) {
            release(player, state, false); // 7.0.2: a player held too long breaks free
            GraftServer.broadcast(player, state);
            return;
        }
        if (state.move == GraftMove.PUPPET_PULL) {
            GraftTails.pin(held); // pinned where it is: he comes to it
        } else {
            Vec3 where = around(player, GraftRules.heldPoint(state.move, beat.kind(), age - beat.land()));
            carry(held, where.m_82520_(0.0, -held.m_20206_() * 0.5, 0.0));
        }
    }

    /**
     * One blow on {@code body}: its damage (the stage's, the beat's share, harder just after the propulsion), the wound it leaves,
     * bleeding, a stun, thrown along {@code push} (and up for the slams), mastery for him.
     */
    static void strikeBody(ServerPlayer player, GraftServer.GraftState state, LivingEntity body, float power, Vec3 push, int flags, long now,
                           WoundBook.Blow blow) {
        // 1.0.40: an M1's blow on a player whose own M1 is on its way back at him: the two meet, and neither lands.
        boolean strike = state.move.slot() == GraftMove.Slot.M1;
        if (strike && KaguneClash.meets(player, body, now)) {
            return;
        }
        int mastery = KaguneProfile.mastery(player);
        float damage = GraftRules.damage(mastery, GraftServer.stage(player)) * power;
        if (state.kakuja && GraftServer.hasMutation(player, GraftMutation.WASHUU_BLOOD)) {
            damage *= GraftMutation.BLOOD_DAMAGE;
        }
        if (body instanceof net.minecraft.world.entity.player.Player) {
            damage *= GraftRules.PVP_SHARE; // 7.0.2: a player is not a mob: his blows are scaled for a fight between players
        }
        Vec3 centre = KaguneServerController.center(body);
        Vec3 into = push.m_82556_() < 1.0E-6 ? UkakuFire.horizontal(centre.m_82546_(player.m_20182_())) : push.m_82541_();
        WoundHits.expect(body, blow, centre.m_82546_(into.m_82490_(0.3)), into, (player.m_217043_().m_188500_() - 0.5) * 1.4);
        // Every beat is its own blow (a barrage's stabs come two ticks apart): no hurt cooldown swallows the next, as on every path.
        body.f_19802_ = 0;
        if ((flags & GraftMove.BREAK) != 0 && body instanceof net.minecraft.world.entity.player.Player blocker && blocker.m_21254_()) {
            blocker.m_36384_(true); // 7.0.2: it breaks through: a raised shield is knocked down before the blow lands
        }
        if (!body.m_6469_(player.m_269291_().m_269075_(player), damage)) {
            return;
        }
        if (strike) {
            KaguneClash.landed(player);
        }
        if ((flags & GraftMove.BLEED) != 0) {
            // 7.0.2 (the user's rework): refreshed by every cut, never stacked; the heavier the blow, the harder it bleeds.
            UkakuCrystalCombat.graftBleed(body, player, GraftRules.bleedStacks(power), now);
        }
        if ((flags & (GraftMove.STUN | GraftMove.HEAVY)) != 0) {
            PaleStun.stun(body, now, (flags & GraftMove.HEAVY) != 0 ? GraftRules.HEAVY_STUN : GraftRules.STUN);
        }
        double strength = push.m_82553_();
        if (strength > 1.0E-4) {
            double heavy = (flags & GraftMove.HEAVY) != 0 ? 1.4 : 1.0;
            Vec3 shove = new Vec3(push.f_82479_, 0.0, push.f_82481_).m_82490_(0.55 * heavy);
            double up = (flags & GraftMove.KNOCK_UP) != 0 ? 0.75 * heavy : 0.18;
            body.m_20256_(body.m_20184_().m_82490_(0.3).m_82549_(shove).m_82520_(0.0, up, 0.0));
            body.f_19864_ = true;
        }
        KaguneProfile.addMastery(player, "Graft", 1);
        ControlServer.onKaguneHit(player, 1);
    }

    // ---------------------------------------------------------------- aim and geometry

    /** How far a move reaches (blocks): its longest beat, held to what the stage's limbs can stretch to. */
    static double reach(GraftMove move, int limbs) {
        double reach = 3.0;
        for (GraftMove.Beat beat : move.beats(limbs)) {
            reach = Math.max(reach, beat.reach());
        }
        return Math.min(reach, 18.0);
    }

    /** What a move goes after: the hinted body if it is fair game and in reach, else the nearest in front of him. */
    static LivingEntity aim(ServerPlayer player, Entity hint, double reach) {
        if (hint instanceof LivingEntity living && hint != player && KaguneServerController.validSwipeTarget(player, living)
            && living.m_20280_(player) <= (reach + 1.5) * (reach + 1.5) && KaguneServerController.clearLineTo(player, living)) {
            return living;
        }
        Vec3 eye = player.m_146892_();
        Vec3 look = player.m_20154_();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity body : around(player, reach + 0.5)) {
            Vec3 to = KaguneServerController.center(body).m_82546_(eye);
            double distance = to.m_82553_();
            if (distance < 1.0E-3) {
                continue;
            }
            double facing = to.m_82490_(1.0 / distance).m_82526_(look);
            if (facing < 0.72 || !KaguneServerController.clearLineTo(player, body)) {
                continue;
            }
            double score = distance * (2.0 - facing);
            if (score < bestScore) {
                bestScore = score;
                best = body;
            }
        }
        return best;
    }

    /** Up to {@code most} bodies in front of him in reach, the aimed one first, nearest first. */
    static List<LivingEntity> pick(ServerPlayer player, double reach, LivingEntity first, int most) {
        List<LivingEntity> out = new ArrayList<>();
        if (first != null) {
            out.add(first);
        }
        Vec3 eye = player.m_146892_();
        Vec3 look = UkakuFire.horizontal(player.m_20154_());
        List<LivingEntity> near = around(player, reach + 0.5);
        near.sort(Comparator.comparingDouble(body -> body.m_20280_(player)));
        for (LivingEntity body : near) {
            if (out.size() >= most) {
                break;
            }
            if (body == first) {
                continue;
            }
            Vec3 to = UkakuFire.horizontal(KaguneServerController.center(body).m_82546_(eye));
            if (to.m_82526_(look) < 0.15 || !KaguneServerController.clearLineTo(player, body)) {
                continue;
            }
            out.add(body);
        }
        return out;
    }

    /** Every body he may strike within {@code range} of him. */
    static List<LivingEntity> around(ServerPlayer player, double range) {
        AABB area = player.m_20191_().m_82400_(range);
        List<LivingEntity> out = new ArrayList<>(player.m_284548_().m_6443_(LivingEntity.class, area,
            living -> living != player && KaguneServerController.validSwipeTarget(player, living) && living.m_20280_(player) <= range * range));
        return out;
    }

    /** The body nearest {@code at} within {@code radius} that this beat has not struck yet. */
    static LivingEntity nearest(ServerPlayer player, Vec3 at, double radius, Set<Integer> hit) {
        LivingEntity best = null;
        double bestDistance = radius;
        for (LivingEntity body : player.m_284548_().m_6443_(LivingEntity.class, new AABB(at.f_82479_, at.f_82480_, at.f_82481_, at.f_82479_, at.f_82480_, at.f_82481_).m_82400_(radius + 1.0),
            living -> living != player && KaguneServerController.validSwipeTarget(player, living))) {
            double distance = KaguneServerController.center(body).m_82554_(at) - body.m_20205_() * 0.5;
            if (distance <= bestDistance && !hit.contains(body.m_19879_())) {
                bestDistance = distance;
                best = body;
            }
        }
        return best;
    }

    /** Where he looks out to {@code reach} (or the block in the way). */
    static Vec3 lookPoint(ServerPlayer player, double reach) {
        Vec3 eye = player.m_146892_();
        BlockHitResult block = KaguneServerController.traceBlock(player, reach);
        if (block.m_6662_() == HitResult.Type.BLOCK) {
            return block.m_82450_();
        }
        return eye.m_82549_(player.m_20154_().m_82490_(reach));
    }

    /** Where a beat's limb strikes: its own target, the move's point turned by the beat's angle, held to its reach. */
    static Vec3 strikePoint(ServerPlayer player, GraftServer.GraftState state, GraftMove.Beat beat) {
        Vec3 chest = player.m_20182_().m_82520_(0.0, 1.35, 0.0);
        Vec3 point = state.point;
        if (beat.has(GraftMove.MULTI) && beat.limb() >= 0 && beat.limb() < state.targets.length && state.targets[beat.limb()] != 0
            && player.m_284548_().m_6815_(state.targets[beat.limb()]) instanceof LivingEntity own && own.m_6084_()) {
            point = KaguneServerController.center(own);
        } else if (state.targets[0] != 0 && player.m_284548_().m_6815_(state.targets[0]) instanceof LivingEntity aimed && aimed.m_6084_()) {
            point = KaguneServerController.center(aimed);
            if (Math.abs(beat.from()) > 1.0E-3 && !beat.has(GraftMove.MULTI)) {
                point = chest.m_82549_(turn(point.m_82546_(chest), beat.from()));
            }
        } else if (Math.abs(beat.from()) > 1.0E-3) {
            point = chest.m_82549_(turn(point.m_82546_(chest), beat.from()));
        }
        Vec3 offset = point.m_82546_(chest);
        double reach = beat.reach();
        return offset.m_82553_() > reach ? chest.m_82549_(offset.m_82490_(reach / offset.m_82553_())) : point;
    }

    /** Where a slam hits the ground: the move's point if it is in reach and the beat aims straight, else out at its angle; the surface found there. */
    static Vec3 groundPoint(ServerPlayer player, GraftServer.GraftState state, GraftMove.Beat beat) {
        double yaw = Math.toRadians(state.yaw + beat.from());
        Vec3 out = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 base = player.m_20182_();
        Vec3 head;
        Vec3 flat = new Vec3(state.point.f_82479_ - base.f_82479_, 0.0, state.point.f_82481_ - base.f_82481_);
        if (Math.abs(beat.from()) < 1.0E-3 && flat.m_82553_() <= beat.reach() + 1.0 && flat.m_82553_() > 0.5) {
            head = new Vec3(state.point.f_82479_, base.f_82480_, state.point.f_82481_);
        } else {
            head = base.m_82549_(out.m_82490_(beat.reach()));
        }
        ServerLevel level = player.m_284548_();
        BlockHitResult ground = level.m_45547_(new ClipContext(head.m_82520_(0.0, 2.0, 0.0), head.m_82520_(0.0, -4.0, 0.0), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, player));
        return ground.m_6662_() == HitResult.Type.BLOCK ? ground.m_82450_() : new Vec3(head.f_82479_, base.f_82480_, head.f_82481_);
    }

    /** The shockwave seen where a limb hit the ground ({@code radius} its reach). */
    static void ground(ServerPlayer player, Vec3 at, float radius) {
        BlockPos below = BlockPos.m_274561_(at.f_82479_, at.f_82480_ - 0.01, at.f_82481_);
        int state = Block.m_49956_(player.m_284548_().m_8055_(below));
        double yaw = Math.toRadians(player.m_146908_());
        PaleBlitz.fx(player, S2CPaleFx.SHOCKWAVE, 0, at, new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw)), radius, state);
    }

    /** The limb's shoulder: where it leaves his back, a little to its side and up (the thrust's line starts here). */
    static Vec3 shoulder(ServerPlayer player, GraftServer.GraftState state, int limb) {
        double yaw = Math.toRadians(state.yaw);
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        double side = limb < 0 ? 0.0 : limb % 2 == 0 ? 0.6 : -0.6;
        return player.m_20182_().m_82520_(0.0, 2.1, 0.0).m_82549_(right.m_82490_(side));
    }

    /** The bearing of {@code body} from him, degrees from the way the move faces (plus to his right). */
    static double bearing(ServerPlayer player, GraftServer.GraftState state, LivingEntity body) {
        Vec3 to = KaguneServerController.center(body).m_82546_(player.m_20182_());
        double angle = Math.toDegrees(Math.atan2(-to.f_82479_, to.f_82481_));
        double rel = angle - state.yaw;
        // Minecraft's yaw grows turning right (south to west), so the difference is already + to his right.
        return ((rel % 360.0) + 540.0) % 360.0 - 180.0;
    }

    /** {@code v} turned about the vertical by {@code degrees} (plus to the right, as a yaw grows). */
    static Vec3 turn(Vec3 v, double degrees) {
        double h = Math.hypot(v.f_82479_, v.f_82481_);
        if (h < 1.0E-9) {
            return v;
        }
        double yaw = Math.atan2(-v.f_82479_, v.f_82481_) + Math.toRadians(degrees);
        return new Vec3(-Math.sin(yaw) * h, v.f_82480_, Math.cos(yaw) * h);
    }

    /** A point round him: blocks to his right, up from his feet, ahead (along his look, level). */
    static Vec3 around(ServerPlayer player, double[] at) {
        Vec3 ahead = UkakuFire.horizontal(player.m_20154_());
        Vec3 right = new Vec3(-ahead.f_82481_, 0.0, ahead.f_82479_);
        return player.m_20182_().m_82549_(right.m_82490_(at[0])).m_82549_(ahead.m_82490_(at[2])).m_82520_(0.0, at[1], 0.0);
    }

    /** Pulls a held body toward {@code at} (its feet) by its motion, so it is carried smoothly and never teleported. */
    static void carry(LivingEntity body, Vec3 at) {
        Vec3 gap = at.m_82546_(body.m_20182_());
        double length = gap.m_82553_();
        double most = 1.6;
        Vec3 move = length * 0.55 > most ? gap.m_82490_(most / length) : gap.m_82490_(0.55);
        body.m_20256_(move);
        body.f_19864_ = true;
        body.f_19812_ = true;
        body.f_19789_ = 0.0F;
    }

    /** The bodies this beat has struck (each at most once). */
    static Set<Integer> hitSet(GraftServer.GraftState state, int key) {
        return state.hits.computeIfAbsent(key, ignored -> new HashSet<>());
    }

    /** The whip of the limbs going out (the heavier moves louder). */
    private static void whip(ServerPlayer player, GraftMove move) {
        float volume = move.slot() == GraftMove.Slot.M1 ? 0.55F : 0.85F;
        KaguneServerController.playSelf(player, move.stage() == GraftStage.TRANSPLANT ? HexKaguneSounds.KAGUNE_WHIP : HexKaguneSounds.CENTIPEDE_CLICK, volume,
            KaguneServerController.vary(player, move.stage() == GraftStage.KAKUJA ? 0.7F : 1.0F, 0.1F));
    }

    static double smooth(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return t * t * (3.0 - 2.0 * t);
    }
}
