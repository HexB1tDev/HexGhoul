package com.hex.kagune.control.server;

import com.hex.kagune.control.RinkakuHurl;
import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftRules;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CGhoulNpc;
import com.hex.kagune.control.network.S2CHurl;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.NpcIdentity;
import com.hex.kagune.control.npc.NpcRules;
import com.hex.kagune.control.npc.NpcTemper;
import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.wound.WoundBook;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 4.1.49 (the owner: Roma "has a Rinkaku with 4 purple tendrils (can use all moves from Rinakaku, including impale, Debris Hurl, etc."):
 * a companion's Rinkaku tendrils in a fight, on the server ({@link com.hex.kagune.control.npc.Persona#tendrils}: Roma's four). They are
 * the transplant's tails every Rinkaku ghoul NPC fights with (drawn by every client from the same state a wild ghoul's are sent:
 * {@link S2CGhoulNpc} under her id, {@code GraftRenderer} in her purple), four of them, two a side. What they do:
 * <ul>
 *   <li>the Rinkaku string ({@code LASH_1} .. {@code LASH_5}: the rising lash, the backhand, the twin skewer, the gutting upper, all four
 *   brought down like a guillotine), within the tails' reach, and now and then all four run it through one after another
 *   ({@code THORN_4}, the Fourfold Pierce);</li>
 *   <li>the Impale ({@code ROMA_IMPALE}, as Rinkaku's: one tendril shot straight out through it from as far as {@link #IMPALE_MOST}, it
 *   lifted high on it, struggling, then torn back out of it);</li>
 *   <li>the Rubble Hurl ({@code ROMA_HURL}, {@code ROMA_THROW}, as Rinkaku's: the lower pair tear a chunk of the ground up between them,
 *   hold it high, and whip it at it from as far as {@link #HURL_MOST}; it bursts where it lands: {@link RinkakuHurl}'s numbers, drawn by
 *   every client as a player's is, {@link S2CHurl} under her id).</li>
 * </ul>
 * Up close her legs do the work (Kick Master, {@code EtoOwl}): the tendrils only join in there now and then, and never while a kick of
 * hers is under way.
 */
final class NpcTendrils {
    /** Within this her legs fight (the tendrils join in only {@link #CLOSE_LASH} of the time). */
    static final double KICK_RANGE = 2.8;
    static final double CLOSE_LASH = 0.3;
    /** The Impale: from how near to how far, how long before the next. */
    static final double IMPALE_LEAST = 3.5;
    static final double IMPALE_MOST = 10.0;
    static final long IMPALE_EVERY = 180L;
    /** The Rubble Hurl: from how near to how far, how long before the next. */
    static final double HURL_LEAST = 6.0;
    static final double HURL_MOST = 22.0;
    static final long HURL_EVERY = 220L;
    /** The Fourfold Pierce: how often it may come in place of a string's first lash. */
    static final long PIERCE_EVERY = 120L;
    /** Her tendrils' blow: damage per unit of a beat's power. */
    static final float POWER = 9.0F;
    /** The tendrils back in this long after her last fight. */
    static final long PUT_AWAY = 120L;
    /** The rank her tendrils are drawn and reach at (an S's length on the transplant's tails). */
    static final int TIER = NpcRules.S;

    /** Her tendrils: out and since when, the move under way, its point and facing, her string, what one holds, the chunk she hurls. */
    static final class State {
        boolean out;
        long outAt = Long.MIN_VALUE;
        long foughtAt = Long.MIN_VALUE;
        GraftMove move = GraftMove.NONE;
        long moveAt = Long.MIN_VALUE;
        Vec3 point = Vec3.f_82478_;
        double yaw;
        int prey;
        int strung;
        GraftMove last;
        long lastEnds = Long.MIN_VALUE;
        long readyAt = Long.MIN_VALUE;
        long impaleReady = Long.MIN_VALUE;
        long hurlReady = Long.MIN_VALUE;
        long pierceReady = Long.MIN_VALUE;
        int held;
        final Map<Integer, Set<Integer>> hits = new HashMap<>();
        // The Rubble Hurl's chunk: where it was torn from, where it is, how fast, how big, of what, and what it is doing (S2CHurl's phases).
        Vec3 dug = Vec3.f_82478_;
        double[] chunk;
        double[] velocity;
        double size;
        String item = "";
        int phase = S2CHurl.GONE;
        long flownAt = Long.MIN_VALUE;
        boolean dirty = true;
    }

    private NpcTendrils() {
    }

    // ---------------------------------------------------------------- every tick

    static void tick(EtoNpc npc, EtoBrain.Mind mind, ServerLevel level, long now) {
        if (npc.persona().tendrils() <= 0) {
            return;
        }
        State st = mind.tendrils;
        if (st.phase == S2CHurl.FLY) {
            fly(npc, st, level, now);
        }
        if (st.move != GraftMove.NONE && now - st.foughtAt > 4L) {
            // What it fought is gone: the move ends where it is (what it held let go, the chunk dropped).
            end(npc, st, level, now);
        }
        if (st.out && now - st.foughtAt > PUT_AWAY && st.move == GraftMove.NONE && st.phase == S2CHurl.GONE) {
            st.out = false;
            st.outAt = now;
            st.dirty = true;
            level.m_6263_(null, npc.m_20185_(), npc.m_20186_() + 1.0, npc.m_20189_(), HexKaguneSounds.KAGUNE_DISMISS.get(), SoundSource.HOSTILE, 0.8F,
                1.1F);
        }
        if (st.dirty) {
            st.dirty = false;
            ControlNetwork.broadcastAround(npc, message(npc, st));
        }
    }

    /** What every client is told of her tendrils: a wild Rinkaku ghoul's message, under her id. */
    static S2CGhoulNpc message(EtoNpc npc, State st) {
        NpcIdentity who = new NpcIdentity(NpcIdentity.Kind.RINKAKU, NpcTemper.DRIFTER, TIER, 0, 0, npc.m_19879_(), 0, false, 0);
        return new S2CGhoulNpc(npc.m_19879_(), who.pack(), st.out, st.outAt, st.move.ordinal(), st.moveAt, st.point.f_82479_, st.point.f_82480_,
            st.point.f_82481_, st.prey, Long.MIN_VALUE, Long.MIN_VALUE, 1, -1, Long.MIN_VALUE, false);
    }

    // ---------------------------------------------------------------- the fight

    /**
     * Her tendrils' turn at {@code target}: true while they have it (a move of theirs under way, or one begun now); false leaves it to her
     * legs and feet ({@code EtoOwl}: Kick Master's string and kicks, closing in, the flips).
     */
    static boolean fight(EtoNpc npc, EtoBrain.Mind mind, LivingEntity target, ServerLevel level, long now) {
        int limbs = npc.persona().tendrils();
        if (limbs <= 0) {
            return false;
        }
        State st = mind.tendrils;
        st.foughtAt = now;
        if (!st.out) {
            st.out = true;
            st.outAt = now;
            st.dirty = true;
            // Four of them, out of her back at once.
            level.m_6263_(null, npc.m_20185_(), npc.m_20186_() + 1.0, npc.m_20189_(), HexKaguneSounds.MANIFEST_RIP.get(), SoundSource.HOSTILE, 1.1F, 1.15F);
        }
        if (st.move != GraftMove.NONE) {
            advance(npc, st, target, level, now, limbs);
            return true;
        }
        if (mind.owl.move != PaleMove.NONE || now - st.outAt < NpcRules.DEPLOY_TICKS || now < st.readyAt) {
            return false; // a kick of hers plays out; the tendrils are still coming out; a breath between strings
        }
        double distance = Math.sqrt(target.m_20280_(npc));
        boolean sight = npc.m_142582_(target);
        double roll = npc.m_217043_().m_188500_();
        if (sight && distance >= IMPALE_LEAST && distance <= IMPALE_MOST && now >= st.impaleReady && roll < 0.35) {
            begin(npc, st, target, GraftMove.ROMA_IMPALE, level, now, limbs);
            st.impaleReady = now + IMPALE_EVERY;
            st.readyAt = now + GraftMove.ROMA_IMPALE.ticks() + 10L;
            return true;
        }
        if (sight && distance >= HURL_LEAST && distance <= HURL_MOST && now >= st.hurlReady && st.phase == S2CHurl.GONE && roll < 0.5
            && dig(npc, st, target, level, now)) {
            begin(npc, st, target, GraftMove.ROMA_HURL, level, now, limbs);
            st.point = new Vec3(st.chunk[0], st.chunk[1], st.chunk[2]);
            st.hurlReady = now + HURL_EVERY;
            return true;
        }
        double reach = GraftCombat.reach(GraftMove.LASH_1, limbs);
        boolean chained = st.strung > 0 && st.last != null && now <= st.lastEnds + GraftRules.CHAIN_WINDOW;
        if (!sight || distance > reach - 0.4 || distance <= KICK_RANGE && !chained && roll > CLOSE_LASH) {
            return false;
        }
        GraftMove move;
        if (!chained && now >= st.pierceReady && roll < 0.2) {
            move = GraftMove.THORN_4; // all four run it through, one after another
            st.pierceReady = now + PIERCE_EVERY;
        } else {
            move = chained ? GraftMove.nextStrike(st.last, GraftStage.TRANSPLANT) : GraftMove.LASH_1;
        }
        begin(npc, st, target, move, level, now, limbs);
        st.strung = chained ? st.strung + 1 : 1;
        if (move == GraftMove.LASH_5 || move == GraftMove.THORN_4 || st.strung >= 5) {
            st.strung = 0;
            st.readyAt = now + move.ticks() + 12L + npc.m_217043_().m_188503_(16);
        }
        return true;
    }

    /** {@code move} begun now at {@code target}: aimed at its middle, every client told, on the clash board as a lash of hers. */
    private static void begin(EtoNpc npc, State st, LivingEntity target, GraftMove move, ServerLevel level, long now, int limbs) {
        st.move = move;
        st.moveAt = now;
        st.prey = target.m_19879_();
        st.point = KaguneServerController.center(target);
        Vec3 facing = st.point.m_82546_(npc.m_20182_());
        st.yaw = Math.toDegrees(Math.atan2(-facing.f_82479_, facing.f_82481_));
        st.hits.clear();
        st.dirty = true;
        npc.m_21573_().m_26573_();
        sound(level, npc.m_20182_(), HexKaguneSounds.KAGUNE_WHIP.get(), 0.6F, 1.05F + 0.1F * (float)npc.m_217043_().m_188500_());
        if (move.slot() == GraftMove.Slot.M1) {
            int lands = move.ticks();
            float power = 0.0F;
            for (GraftMove.Beat beat : move.beats(limbs)) {
                lands = Math.min(lands, beat.land());
                power = Math.max(power, beat.power());
            }
            long began = now;
            KaguneClash.swing(npc, now, lands + 1, GraftCombat.reach(move, limbs), UkakuFire.horizontal(facing), target.m_19879_(),
                POWER * Math.max(0.5F, power), () -> st.move == move && st.moveAt == began, () -> cut(npc, st));
        }
    }

    /** Its lash met a blow coming back (KaguneClash): cut off, the string with it, a moment before the next. */
    private static void cut(EtoNpc npc, State st) {
        long now = npc.m_9236_().m_46467_();
        st.move = GraftMove.NONE;
        st.last = null;
        st.lastEnds = now;
        st.strung = 0;
        st.readyAt = now + com.hex.kagune.control.ClashRules.STAGGER + 6L;
        st.dirty = true;
    }

    /** A tick of the move under way: each beat on the tick every client shows it landing; at its end, the next (or nothing). */
    private static void advance(EtoNpc npc, State st, LivingEntity target, ServerLevel level, long now, int limbs) {
        GraftMove move = st.move;
        long age = now - st.moveAt;
        npc.m_21573_().m_26573_(); // planted while the tendrils strike
        EtoOwl.face(npc, target);
        if (move.follows() && st.held == 0) {
            st.point = KaguneServerController.center(target);
        }
        java.util.List<GraftMove.Beat> beats = move.beats(limbs);
        for (int i = 0; i < beats.size(); i++) {
            beat(npc, st, beats.get(i), i, age, level, now);
            if (st.move != move) {
                return;
            }
        }
        if (move == GraftMove.ROMA_HURL) {
            hold(npc, st, level, now, age);
        }
        if (age < move.ticks()) {
            return;
        }
        if (move == GraftMove.ROMA_HURL) {
            // Up and held: whipped at it.
            throwChunk(npc, st, target, level, now);
            begin(npc, st, target, GraftMove.ROMA_THROW, level, now, limbs);
            return;
        }
        if (move == GraftMove.ROMA_IMPALE) {
            tearOut(npc, st, level, now);
        }
        st.last = move;
        st.lastEnds = now;
        st.move = GraftMove.NONE;
        st.dirty = true;
    }

    /** The move under way stopped where it is: what it holds let go, a chunk still held dropped where it is. */
    private static void end(EtoNpc npc, State st, ServerLevel level, long now) {
        if (st.move == GraftMove.ROMA_IMPALE) {
            tearOut(npc, st, level, now);
        }
        if (st.phase == S2CHurl.HOLD) {
            burst(npc, st, level, now, new Vec3(st.chunk[0], st.chunk[1], st.chunk[2]), 0.4);
        }
        st.move = GraftMove.NONE;
        st.held = 0;
        st.dirty = true;
    }

    /** One beat at {@code age} ticks into its move: it lands on the tick every client shows it landing. */
    private static void beat(EtoNpc npc, State st, GraftMove.Beat beat, int index, long age, ServerLevel level, long now) {
        if (beat.kind() == GraftMove.Kind.SEIZE) {
            if (st.move == GraftMove.ROMA_IMPALE) {
                impale(npc, st, beat, index, age, level, now);
            }
            return;
        }
        boolean sweeping = beat.kind() == GraftMove.Kind.SWEEP;
        if (sweeping ? age < beat.land() || age > beat.end() : age != beat.land()) {
            return;
        }
        Vec3 chest = npc.m_20182_().m_82520_(0.0, 1.2, 0.0);
        Vec3 toward = st.point.m_82546_(chest);
        Vec3 dir = toward.m_82556_() < 1.0E-6 ? UkakuFire.horizontal(npc.m_20154_()) : toward.m_82541_();
        Vec3 at = toward.m_82553_() > beat.reach() ? chest.m_82549_(dir.m_82490_(beat.reach())) : st.point;
        Set<Integer> hit = st.hits.computeIfAbsent(index, ignored -> new HashSet<>());
        switch (beat.kind()) {
            case SWEEP -> {
                double span = Math.max(1, beat.end() - beat.land());
                double before = age == beat.land() ? beat.from()
                    : beat.from() + (beat.to() - beat.from()) * GraftCombat.smooth((age - 1 - beat.land()) / span);
                double after = beat.from() + (beat.to() - beat.from()) * GraftCombat.smooth((age - beat.land()) / span);
                double low = Math.min(before, after) - 12.0;
                double high = Math.max(before, after) + 12.0;
                double sign = Math.signum(beat.to() - beat.from());
                for (LivingEntity body : around(npc, beat.reach() + 1.0)) {
                    Vec3 to = UkakuFire.horizontal(KaguneServerController.center(body).m_82546_(npc.m_20182_()));
                    double bearing = bearing(npc, st, body);
                    double dy = body.m_20186_() + body.m_20206_() * 0.5 - (npc.m_20186_() + beat.height());
                    if (bearing < low || bearing > high || Math.abs(dy) > 2.2 + body.m_20206_() * 0.5 || !hit.add(body.m_19879_())) {
                        continue;
                    }
                    Vec3 across = new Vec3(-to.f_82481_ * sign, 0.0, to.f_82479_ * sign);
                    if (strike(npc, body, beat, across.m_82549_(to.m_82490_(0.3)), now, WoundBook.Blow.SLASH, S2CPaleFx.IMPACT)) {
                        return;
                    }
                }
            }
            case THRUST, UPPER -> {
                for (LivingEntity body : around(npc, beat.reach() + 1.0)) {
                    Vec3 centre = KaguneServerController.center(body);
                    double along = centre.m_82546_(chest).m_82526_(dir);
                    Vec3 closest = chest.m_82549_(dir.m_82490_(Math.max(0.0, Math.min(beat.reach(), along))));
                    if (along < 0.5 || closest.m_82554_(centre) > body.m_20205_() * 0.5 + 0.8 || !hit.add(body.m_19879_())) {
                        continue;
                    }
                    boolean upper = beat.kind() == GraftMove.Kind.UPPER;
                    if (strike(npc, body, beat, upper ? new Vec3(dir.f_82479_ * 0.25, 0.0, dir.f_82481_ * 0.25) : dir, now, WoundBook.Blow.STAB,
                        S2CPaleFx.IMPALED)) {
                        return;
                    }
                    if (upper) {
                        body.m_20256_(body.m_20184_().m_82520_(0.0, 0.45, 0.0));
                        body.f_19864_ = true;
                    }
                }
            }
            case SLAM -> {
                BlockHitResult ground = level.m_45547_(new ClipContext(at.m_82520_(0.0, 1.5, 0.0), at.m_82520_(0.0, -4.0, 0.0), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, npc));
                Vec3 down = ground.m_6662_() == HitResult.Type.BLOCK ? ground.m_82450_() : new Vec3(at.f_82479_, npc.m_20186_(), at.f_82481_);
                for (LivingEntity body : around(npc, beat.reach() + 3.5)) {
                    Vec3 centre = KaguneServerController.center(body);
                    double distance = Math.hypot(centre.f_82479_ - down.f_82479_, centre.f_82481_ - down.f_82481_) - body.m_20205_() * 0.5;
                    if (distance > 3.1 || Math.abs(body.m_20186_() - down.f_82480_) > 3.0 || !hit.add(body.m_19879_())) {
                        continue;
                    }
                    float share = distance <= 2.1 ? 1.0F : 0.5F;
                    if (strike(npc, body, new GraftMove.Beat(beat.limb(), beat.kind(), beat.start(), beat.land(), beat.end(), beat.reach(), 0.0, 0.0, 0.0,
                        beat.power() * share, beat.flags()), UkakuFire.horizontal(centre.m_82546_(down)).m_82490_(0.7), now, WoundBook.Blow.SLAM,
                        S2CPaleFx.CRUSHED)) {
                        return;
                    }
                }
                BlockPos below = BlockPos.m_274561_(down.f_82479_, down.f_82480_ - 0.01, down.f_82481_);
                PaleBlitz.fxAround(npc, npc.m_19879_(), S2CPaleFx.SHOCKWAVE, down, UkakuFire.horizontal(dir), 3.0F, Block.m_49956_(level.m_8055_(below)));
                sound(level, down, HexKaguneSounds.KAGUNE_SLAM.get(), 0.9F, 0.9F);
            }
            default -> {
            }
        }
    }

    // ---------------------------------------------------------------- the Impale

    /**
     * The Impale's tendril: on the tick it lands, through whatever stands on its line (its target first); from then till it lets go, what
     * it ran through is carried up on it ({@link GraftRules#heldPoint}), held fast.
     */
    private static void impale(EtoNpc npc, State st, GraftMove.Beat beat, int index, long age, ServerLevel level, long now) {
        if (age == beat.land()) {
            Vec3 chest = npc.m_20182_().m_82520_(0.0, 1.2, 0.0);
            Vec3 dir = st.point.m_82546_(chest).m_82556_() < 1.0E-6 ? UkakuFire.horizontal(npc.m_20154_()) : st.point.m_82546_(chest).m_82541_();
            LivingEntity caught = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity body : around(npc, beat.reach() + 1.0)) {
                Vec3 centre = KaguneServerController.center(body);
                double along = centre.m_82546_(chest).m_82526_(dir);
                Vec3 closest = chest.m_82549_(dir.m_82490_(Math.max(0.0, Math.min(beat.reach(), along))));
                double off = closest.m_82554_(centre);
                if (along >= 0.5 && off <= body.m_20205_() * 0.5 + 0.9 && (body.m_19879_() == st.prey ? -1.0 : along) < best) {
                    best = body.m_19879_() == st.prey ? -1.0 : along;
                    caught = body;
                }
            }
            if (caught == null) {
                // Missed: the tendril snaps back.
                sound(level, npc.m_20182_(), HexKaguneSounds.KAGUNE_WHIP.get(), 0.7F, 0.85F);
                st.moveAt = now - Math.max(0, st.move.ticks() - 6);
                st.dirty = true;
                return;
            }
            st.held = caught.m_19879_();
            strike(npc, caught, beat, dir, now, WoundBook.Blow.STAB, S2CPaleFx.IMPALED);
            sound(level, KaguneServerController.center(caught), HexKaguneSounds.KAGUNE_GRAB.get(), 1.0F, 0.9F);
            st.dirty = true;
            return;
        }
        if (age <= beat.land() || age > beat.end() || st.held == 0) {
            return;
        }
        Entity entity = level.m_6815_(st.held);
        if (!(entity instanceof LivingEntity held) || !held.m_6084_() || held.m_20280_(npc) > 16.0 * 16.0) {
            st.held = 0;
            st.dirty = true;
            return;
        }
        PaleStun.stun(held, now, 6);
        Vec3 where = around(npc, st.yaw, GraftRules.heldPoint(GraftMove.ROMA_IMPALE, GraftMove.Kind.SEIZE, age - beat.land()));
        GraftCombat.carry(held, where.m_82520_(0.0, -held.m_20206_() * 0.5, 0.0));
    }

    /** The Impale's end: torn back out of what it carried (a last wound), which drops where it is. */
    private static void tearOut(EtoNpc npc, State st, ServerLevel level, long now) {
        if (st.held != 0 && level.m_6815_(st.held) instanceof LivingEntity held && held.m_6084_()) {
            Vec3 centre = KaguneServerController.center(held);
            hurtBody(npc, held, POWER * 0.6F, centre, UkakuFire.horizontal(centre.m_82546_(npc.m_20182_())), WoundBook.Blow.STAB);
            held.m_20256_(new Vec3(0.0, -0.2, 0.0));
            held.f_19864_ = true;
            PaleBlitz.fxAround(held, npc.m_19879_(), S2CPaleFx.IMPALED, centre, UkakuFire.horizontal(centre.m_82546_(npc.m_20182_())), held.m_20206_(), 0);
            sound(level, centre, HexKaguneSounds.FLESH_IMPACT.get(), 1.0F, 0.7F);
        }
        st.held = 0;
        st.dirty = true;
    }

    // ---------------------------------------------------------------- the Rubble Hurl

    /** The chunk torn out of the ground between her and {@code target} (a few blocks ahead of her): true if there was ground to tear. */
    private static boolean dig(EtoNpc npc, State st, LivingEntity target, ServerLevel level, long now) {
        Vec3 ahead = UkakuFire.horizontal(target.m_20182_().m_82546_(npc.m_20182_()));
        BlockPos found = null;
        for (double out : new double[] {2.2, 1.6, 3.0}) {
            for (int dy = 0; dy >= -3 && found == null; dy--) {
                BlockPos pos = BlockPos.m_274561_(npc.m_20185_() + ahead.f_82479_ * out, npc.m_20186_() - 0.5 + dy, npc.m_20189_() + ahead.f_82481_ * out);
                if (HurlServer.usable(level, pos)) {
                    found = pos;
                }
            }
            if (found != null) {
                break;
            }
        }
        if (found == null) {
            return false;
        }
        BlockState torn = level.m_8055_(found);
        st.item = HurlServer.item(torn);
        st.dug = Vec3.m_82512_(found);
        st.size = RinkakuHurl.size(RinkakuHurl.charge(18.0), 1);
        st.chunk = new double[] {st.dug.f_82479_, st.dug.f_82480_, st.dug.f_82481_};
        st.phase = S2CHurl.HOLD;
        PaleBlitz.fxAround(npc, npc.m_19879_(), S2CPaleFx.SHOCKWAVE, st.dug.m_82520_(0.0, 0.5, 0.0), ahead, 1.6F, Block.m_49956_(torn));
        UkakuFire.sound(level, st.dug, "block.stone.break", 1.0F, 0.85F);
        sound(level, st.dug, HexKaguneSounds.KAGUNE_GRAB.get(), 0.9F, 0.9F);
        tell(npc, st);
        return true;
    }

    /**
     * The chunk rising out of the ground into her lower pair's grip and held before and above her head while the move lasts: where the
     * rig's grip has it ({@link GraftRules#heldPoint}, as every client draws her tendrils wrapped round it), from the tick they reach it.
     */
    private static void hold(EtoNpc npc, State st, ServerLevel level, long now, long age) {
        GraftMove.Beat first = GraftMove.ROMA_HURL.beats(npc.persona().tendrils()).get(0);
        if (age >= first.land()) {
            Vec3 at = around(npc, st.yaw, GraftRules.heldPoint(GraftMove.ROMA_HURL, GraftMove.Kind.SEIZE, age - first.land()));
            st.chunk = new double[] {at.f_82479_, at.f_82480_, at.f_82481_};
        }
        st.point = new Vec3(st.chunk[0], st.chunk[1], st.chunk[2]); // the tendrils go where it is
        st.dirty = true;
        tell(npc, st);
    }

    /** Whipped at {@code target}: a lob that lands on it ({@link RinkakuHurl#aim}). */
    private static void throwChunk(EtoNpc npc, State st, LivingEntity target, ServerLevel level, long now) {
        if (st.phase != S2CHurl.HOLD || st.chunk == null) {
            return;
        }
        Vec3 mark = target.m_20182_().m_82520_(0.0, target.m_20206_() * 0.4, 0.0);
        st.velocity = RinkakuHurl.aim(st.chunk, new double[] {mark.f_82479_, mark.f_82480_, mark.f_82481_}, RinkakuHurl.speed(0.6));
        st.phase = S2CHurl.FLY;
        st.flownAt = now;
        sound(level, npc.m_20182_(), HexKaguneSounds.KAGUNE_WHIP.get(), 1.0F, 0.7F);
        tell(npc, st);
    }

    /** A tick of the chunk's flight: it bursts on the first body or block it meets, or after {@link RinkakuHurl#FLIGHT} ticks. */
    private static void fly(EtoNpc npc, State st, ServerLevel level, long now) {
        double[] from = st.chunk.clone();
        RinkakuHurl.step(st.chunk, st.velocity);
        Vec3 a = new Vec3(from[0], from[1], from[2]);
        Vec3 b = new Vec3(st.chunk[0], st.chunk[1], st.chunk[2]);
        double radius = st.size * 0.5;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, new net.minecraft.world.phys.AABB(Math.min(a.f_82479_, b.f_82479_), Math.min(a.f_82480_, b.f_82480_), Math.min(a.f_82481_, b.f_82481_),
            Math.max(a.f_82479_, b.f_82479_), Math.max(a.f_82480_, b.f_82480_), Math.max(a.f_82481_, b.f_82481_)).m_82400_(radius + 1.0),
            living -> living != npc && living.m_6084_() && EtoBrain.foe(npc, living))) {
            if (RinkakuHurl.meets(from, st.chunk, body.m_20185_(), body.m_20186_(), body.m_20186_() + body.m_20206_(), body.m_20189_(),
                body.m_20205_() * 0.5, radius)) {
                burst(npc, st, level, now, b, 1.0);
                return;
            }
        }
        BlockHitResult wall = level.m_45547_(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc));
        if (wall.m_6662_() == HitResult.Type.BLOCK || now - st.flownAt > RinkakuHurl.FLIGHT) {
            burst(npc, st, level, now, wall.m_6662_() == HitResult.Type.BLOCK ? wall.m_82450_() : b, 1.0);
            return;
        }
        tell(npc, st);
    }

    /** The chunk bursts at {@code at}: every foe round it hurt ({@code share} of a full one's), knocked back and stunned a moment. */
    private static void burst(EtoNpc npc, State st, ServerLevel level, long now, Vec3 at, double share) {
        double charge = 0.6;
        double radius = RinkakuHurl.radius(st.size);
        for (LivingEntity body : level.m_6443_(LivingEntity.class, new net.minecraft.world.phys.AABB(at.f_82479_, at.f_82480_, at.f_82481_, at.f_82479_, at.f_82480_, at.f_82481_).m_82400_(radius + 1.0),
            living -> living != npc && living.m_6084_() && EtoBrain.foe(npc, living))) {
            Vec3 centre = KaguneServerController.center(body);
            double falloff = RinkakuHurl.falloff(centre.m_82554_(at), radius);
            if (falloff <= 0.0) {
                continue;
            }
            Vec3 away = UkakuFire.horizontal(centre.m_82546_(at));
            if (hurtBody(npc, body, (float)(POWER * 1.6 * RinkakuHurl.power(charge) * falloff * share), centre, away, WoundBook.Blow.CRUSH)) {
                body.m_20256_(body.m_20184_().m_82549_(away.m_82490_(RinkakuHurl.knock(charge) * falloff)).m_82520_(0.0, RinkakuHurl.lift(charge) * falloff, 0.0));
                body.f_19864_ = true;
                PaleStun.stun(body, now, RinkakuHurl.stun(charge));
            }
        }
        st.phase = S2CHurl.BURST;
        st.chunk = new double[] {at.f_82479_, at.f_82480_, at.f_82481_};
        tell(npc, st);
        PaleBlitz.fxAround(npc, npc.m_19879_(), S2CPaleFx.SHOCKWAVE, at, Vec3.f_82478_, (float)radius, Block.m_49956_(level.m_8055_(BlockPos.m_274561_(at.f_82479_,
            at.f_82480_ - 0.5, at.f_82481_))));
        sound(level, at, HexKaguneSounds.KAGUNE_SLAM.get(), 1.1F, 0.75F);
        st.phase = S2CHurl.GONE;
        ControlNetwork.broadcastAround(npc, new S2CHurl(npc.m_19879_(), 0, S2CHurl.GONE, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1, -1, ""));
    }

    /** Every client told where the chunk is (her lower pair gripping it while it is held). */
    private static void tell(EtoNpc npc, State st) {
        if (st.chunk == null || st.phase == S2CHurl.GONE) {
            return;
        }
        boolean held = st.phase == S2CHurl.HOLD;
        ControlNetwork.broadcastAround(npc, new S2CHurl(npc.m_19879_(), 0, st.phase, (float)st.chunk[0], (float)st.chunk[1], (float)st.chunk[2],
            (float)st.size, 0.6F, held ? 2 : -1, held ? 3 : -1, st.item));
    }

    // ---------------------------------------------------------------- blows

    /** One blow of a beat on {@code body}: hurt, wounded where it was struck, thrown along {@code push}; true if it met a blow coming back. */
    private static boolean strike(EtoNpc npc, LivingEntity body, GraftMove.Beat beat, Vec3 push, long now, WoundBook.Blow blow, int fx) {
        if (KaguneClash.meets(npc, body, now)) {
            return true;
        }
        Vec3 centre = KaguneServerController.center(body);
        Vec3 into = push.m_82556_() < 1.0E-6 ? UkakuFire.horizontal(centre.m_82546_(npc.m_20182_())) : push.m_82541_();
        if (!hurtBody(npc, body, POWER * beat.power(), centre.m_82546_(into.m_82490_(0.3)), into, blow)) {
            return false;
        }
        KaguneClash.landed(npc);
        double heavy = beat.has(GraftMove.HEAVY) ? 1.4 : 1.0;
        Vec3 shove = new Vec3(push.f_82479_, 0.0, push.f_82481_).m_82490_(0.45 * heavy);
        double up = beat.has(GraftMove.KNOCK_UP) ? 0.5 * heavy : 0.15;
        if (beat.kind() != GraftMove.Kind.SEIZE) {
            body.m_20256_(body.m_20184_().m_82490_(0.3).m_82549_(shove).m_82520_(0.0, up, 0.0));
            body.f_19864_ = true;
        }
        PaleBlitz.fxAround(body, npc.m_19879_(), fx, centre, into, fx == S2CPaleFx.IMPALED ? body.m_20206_() : 1.0F, fx == S2CPaleFx.IMPACT ? S2CPaleFx.BONE : 0);
        sound((ServerLevel)npc.m_9236_(), centre, HexKaguneSounds.FLESH_IMPACT.get(), 0.8F, 0.95F + 0.2F * (float)npc.m_217043_().m_188500_());
        return false;
    }

    /** Her blow on {@code body} (the wound it leaves where it was struck): true if it took it. */
    private static boolean hurtBody(EtoNpc npc, LivingEntity body, float damage, Vec3 at, Vec3 way, WoundBook.Blow blow) {
        WoundHits.expect(body, blow, at, way, (npc.m_217043_().m_188500_() - 0.5) * 1.4);
        body.f_19802_ = 0;
        return body.m_6469_(npc.m_269291_().m_269333_(npc), damage);
    }

    /** Every foe of hers within {@code range} that she can see. */
    private static java.util.List<LivingEntity> around(EtoNpc npc, double range) {
        return npc.m_9236_().m_6443_(LivingEntity.class, npc.m_20191_().m_82400_(range),
            living -> living.m_20280_(npc) <= range * range && EtoBrain.foe(npc, living) && npc.m_142582_(living));
    }

    /** The bearing of {@code body} from her, degrees from the way the move faces (plus to her right). */
    private static double bearing(EtoNpc npc, State st, LivingEntity body) {
        Vec3 to = KaguneServerController.center(body).m_82546_(npc.m_20182_());
        double rel = Math.toDegrees(Math.atan2(-to.f_82479_, to.f_82481_)) - st.yaw;
        return ((rel % 360.0) + 540.0) % 360.0 - 180.0;
    }

    /** A point of her rig ({@code at}: right, up, forward) in the world, facing {@code yaw} degrees. */
    static Vec3 around(EtoNpc npc, double yaw, double[] at) {
        double r = Math.toRadians(yaw);
        Vec3 ahead = new Vec3(-Math.sin(r), 0.0, Math.cos(r));
        Vec3 right = new Vec3(-ahead.f_82481_, 0.0, ahead.f_82479_);
        return npc.m_20182_().m_82549_(right.m_82490_(at[0])).m_82549_(ahead.m_82490_(at[2])).m_82520_(0.0, at[1], 0.0);
    }

    private static void sound(ServerLevel level, Vec3 at, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.m_6263_(null, at.f_82479_, at.f_82480_, at.f_82481_, sound, SoundSource.HOSTILE, volume, pitch);
    }

    /** Forgotten with her (sent away, gone). */
    static void forget(EtoNpc npc, EtoBrain.Mind mind) {
        if (mind != null && mind.tendrils.phase != S2CHurl.GONE) {
            ControlNetwork.broadcastAround(npc, new S2CHurl(npc.m_19879_(), 0, S2CHurl.GONE, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1, -1, ""));
            mind.tendrils.phase = S2CHurl.GONE;
        }
    }
}
