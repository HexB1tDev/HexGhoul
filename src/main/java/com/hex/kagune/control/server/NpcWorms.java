package com.hex.kagune.control.server;

import com.hex.kagune.control.bikaku.BikakuForm;
import com.hex.kagune.control.bikaku.BikakuMove;
import com.hex.kagune.control.bikaku.BikakuRules;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CBikakuState;
import com.hex.kagune.control.network.S2CNpcWings;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 4.0.0 (The Great Exploration): Noro's fight, on the server. The owner: "When attacked, Noro must defend himself using his maximum
 * Bikaku combat configuration, consisting of three powerful tendrils. These tendrils must be fully animated and capable of performing his
 * available combat abilities ... overwhelming strength and abnormal regeneration ... Do not turn him into a standard ghoul NPC with
 * increased statistics."
 *
 * <p>He does not run. He walks at what he fights, slowly, and his three worms do the rest from as far as {@link #REACH}: their bites in
 * turn (Snap, Lunge, Maul, each healing him), and every so often a move made by all three, each from its own lane and a beat after the
 * one before (as a Three Worms player's): Devouring Maw (each catches a body and shakes it), Predatory Drag (each drags one to him and
 * slams it down), Carrion Eruption (under each), Ravenous Pursuit (after up to three bodies). He knits back faster than any ghoul; and
 * the first blow that should kill him in a while does not: he folds, strands of his worms over him, and pulls himself back together
 * ({@link #MEND}), taking almost nothing while he does. Every client draws his worms from the same state a Bikaku player's are sent
 * ({@code S2CBikakuState}, under his id), and his worms out and his mending ({@code S2CNpcWings}).
 */
final class NpcWorms {
    /** How far his worms reach (blocks) and how near he walks before he stops and lets them work. */
    static final double REACH = 14.0;
    static final double STAND = 8.0;
    /** Their bites: how hard (per unit of the bite's power), how much each heals him. */
    static final float BITE_DAMAGE = 9.0F;
    static final float BITE_HEAL = 4.0F;
    /** A move by all three: how hard each worm's lands, how long between them (ticks, least and most). */
    static final float MOVE_DAMAGE = 11.0F;
    static final int MOVE_LEAST = 90;
    static final int MOVE_MOST = 150;
    /** Abnormal regeneration: unhurt this long, a health back every {@link #REGEN_EVERY} ticks (out of a fight too). */
    static final int REGEN_AFTER = 40;
    static final int REGEN_EVERY = 6;
    /** His mend: how long it takes (ticks), how much of a blow he takes meanwhile, how long before it can save him again. */
    static final int MEND = 70;
    static final float MEND_TAKEN = 0.1F;
    static final long MEND_EVERY = 20L * 120L;
    /** The worms back in this long after his last fight. */
    static final long PUT_AWAY = 120L;
    private static final BikakuRules.Bite[] BITES = {BikakuRules.Bite.SNAP, BikakuRules.Bite.LUNGE, BikakuRules.Bite.MAUL};

    /** His worms: out and since when, the bite and the move under way, their marks and catches, his mend. */
    static final class State {
        boolean out;
        long outAt = Long.MIN_VALUE;
        long foughtAt = Long.MIN_VALUE;
        BikakuRules.Bite bite = BikakuRules.Bite.NONE;
        long biteAt = Long.MIN_VALUE;
        int biteTarget;
        Vec3 aim = Vec3.f_82478_;
        int bites;
        long nextBite = Long.MIN_VALUE;
        BikakuMove move = BikakuMove.NONE;
        long moveAt = Long.MIN_VALUE;
        Vec3 movePoint = Vec3.f_82478_;
        final int[] targets = new int[S2CBikakuState.TARGETS];
        final int[] held = new int[3];
        int caughtBits;
        final boolean[] landed = new boolean[3];
        long nextMove = Long.MIN_VALUE;
        long hurtAt = Long.MIN_VALUE;
        long mendAt = Long.MIN_VALUE;
        long mendReady = Long.MIN_VALUE;
        boolean dirty = true;
    }

    private NpcWorms() {
    }

    // ---------------------------------------------------------------- every tick

    static void tick(EtoNpc npc, EtoBrain.Mind mind, ServerLevel level, long now) {
        if (!npc.persona().worms()) {
            return;
        }
        State st = mind.worms;
        // Abnormal regeneration: the moment he's let be.
        if ((st.hurtAt == Long.MIN_VALUE || now - st.hurtAt >= REGEN_AFTER) && now % REGEN_EVERY == 0L && npc.m_21223_() < npc.m_21233_()) {
            npc.m_5634_(1.0F);
        }
        // His mend: pulled back together, fast.
        if (mending(st, now)) {
            npc.m_5634_(npc.m_21233_() / MEND);
            if (now % 10L == 0L) {
                level.m_6263_(null, npc.m_20185_(), npc.m_20186_() + 1.0, npc.m_20189_(), HexKaguneSounds.KAGUNE_RESHAPE.get(), SoundSource.HOSTILE,
                    0.7F, 0.6F);
            }
        }
        if (st.move != BikakuMove.NONE) {
            moving(npc, st, level, now);
        }
        if (st.out && now - st.foughtAt > PUT_AWAY && !mending(st, now)) {
            st.out = false;
            st.outAt = now;
            st.bite = BikakuRules.Bite.NONE;
            st.move = BikakuMove.NONE;
            release(st);
            st.dirty = true;
            level.m_6263_(null, npc.m_20185_(), npc.m_20186_() + 1.0, npc.m_20189_(), HexKaguneSounds.KAGUNE_DISMISS.get(), SoundSource.HOSTILE, 0.8F, 0.6F);
        }
        if (st.dirty) {
            st.dirty = false;
            ControlNetwork.broadcastAround(npc, state(npc, st));
            ControlNetwork.broadcastAround(npc, wings(npc, st));
        }
    }

    // ---------------------------------------------------------------- the fight

    /** A tick of his fight; true always (his worms are his whole kit). */
    static boolean fight(EtoNpc npc, EtoBrain.Mind mind, LivingEntity target, ServerLevel level, long now) {
        if (!npc.persona().worms()) {
            return false;
        }
        State st = mind.worms;
        st.foughtAt = now;
        if (!st.out) {
            st.out = true;
            st.outAt = now;
            st.nextBite = now + 14L;
            st.nextMove = now + 50L;
            st.dirty = true;
            // Three of them, out of his back at once.
            level.m_6263_(null, npc.m_20185_(), npc.m_20186_() + 1.0, npc.m_20189_(), HexKaguneSounds.MANIFEST_RIP.get(), SoundSource.HOSTILE, 1.2F, 0.6F);
        }
        EtoOwl.face(npc, target);
        double gap = Math.sqrt(npc.m_20280_(target));
        // Deliberate: he walks, never runs; past where his worms reach he closes in, then stands and lets them work.
        if (gap > STAND) {
            npc.m_21573_().m_5624_(target, 0.75);
        } else {
            npc.m_21573_().m_26573_();
        }
        if (mending(st, now) || now - st.outAt < 12L) {
            return true;
        }
        if (st.move != BikakuMove.NONE) {
            return true;
        }
        if (now >= st.nextMove && gap <= REACH && npc.m_142582_(target)) {
            begin(npc, st, target, level, now, gap);
            return true;
        }
        if (st.bite != BikakuRules.Bite.NONE && now - st.biteAt == st.bite.land()) {
            land(npc, st, level);
        }
        if (now >= st.nextBite && gap <= REACH && npc.m_142582_(target)) {
            BikakuRules.Bite bite = BITES[st.bites++ % BITES.length];
            st.bite = bite;
            st.biteAt = now;
            st.biteTarget = target.m_19879_();
            st.aim = GhoulNpcBrain.centre(target);
            st.nextBite = now + bite.ticks() + 6L;
            st.dirty = true;
            level.m_6263_(null, npc.m_20185_(), npc.m_20186_() + 1.5, npc.m_20189_(), HexKaguneSounds.KAGUNE_WHIP.get(), SoundSource.HOSTILE, 0.9F, 0.7F);
        }
        return true;
    }

    /** A bite lands where its mark is: whoever is there (its target or anyone in the way) is bitten, and he feeds. */
    private static void land(EtoNpc npc, State st, ServerLevel level) {
        if (!(level.m_6815_(st.biteTarget) instanceof LivingEntity body) || !body.m_6084_() || Math.sqrt(npc.m_20280_(body)) > REACH + 1.5) {
            return;
        }
        if (body.m_6469_(npc.m_269291_().m_269333_(npc), BITE_DAMAGE * st.bite.power())) {
            npc.m_5634_(BITE_HEAL);
            level.m_6263_(null, body.m_20185_(), body.m_20186_() + 1.0, body.m_20189_(), HexKaguneSounds.FLESH_IMPACT.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
        }
    }

    /** A move by all three: which, by how far off it is and how many are fighting him. */
    private static void begin(EtoNpc npc, State st, LivingEntity target, ServerLevel level, long now, double gap) {
        List<LivingEntity> foes = level.m_6443_(LivingEntity.class, npc.m_20191_().m_82400_(REACH),
            b -> b != npc && b.m_6084_() && EtoBrain.foe(npc, b) && npc.m_142582_(b));
        double roll = npc.m_217043_().m_188500_();
        BikakuMove move = foes.size() >= 2 && roll < 0.5 ? BikakuMove.PURSUIT
            : gap > 7.0 && roll < 0.45 ? BikakuMove.ERUPTION
            : gap > 4.0 && roll < 0.7 ? BikakuMove.DRAG
            : BikakuMove.MAW;
        java.util.Arrays.fill(st.targets, 0);
        java.util.Arrays.fill(st.held, 0);
        java.util.Arrays.fill(st.landed, false);
        st.caughtBits = 0;
        // Each worm after its own body (the target first), or all on the one if he's alone with it.
        for (int i = 0; i < 3; i++) {
            LivingEntity mark = move == BikakuMove.PURSUIT && i < foes.size() ? foes.get(i) : i == 0 || foes.size() < 2 ? target
                : foes.get(i % foes.size());
            st.targets[i] = mark.m_19879_();
        }
        st.move = move;
        st.moveAt = now;
        st.movePoint = GhoulNpcBrain.centre(target);
        st.bite = BikakuRules.Bite.NONE;
        st.nextMove = now + move.ticks() + MOVE_LEAST + npc.m_217043_().m_188503_(MOVE_MOST - MOVE_LEAST);
        st.dirty = true;
        level.m_6263_(null, npc.m_20185_(), npc.m_20186_() + 1.5, npc.m_20189_(), HexKaguneSounds.KAGUNE_STRAIN.get(), SoundSource.HOSTILE, 1.1F, 0.55F);
    }

    /** A move under way: each worm lands a beat after the one before; its catch is held and done with; then it's over. */
    private static void moving(EtoNpc npc, State st, ServerLevel level, long now) {
        BikakuMove move = st.move;
        long age = now - st.moveAt;
        for (int i = 0; i < 3; i++) {
            long lands = move.land() + (long)(i * move.stagger());
            if (!st.landed[i] && age >= lands) {
                st.landed[i] = true;
                if (level.m_6815_(st.targets[i]) instanceof LivingEntity body && body.m_6084_() && Math.sqrt(npc.m_20280_(body)) <= REACH + 2.0) {
                    strike(npc, st, i, body, move, level);
                }
            }
            // Its catch, held up and shaken (the Maw), or dragged in to him (the Drag), until the move lets go.
            if (st.held[i] != 0 && level.m_6815_(st.held[i]) instanceof LivingEntity held && held.m_6084_()) {
                hold(npc, i, held, move, age);
            }
        }
        if (age >= move.ticks() + (long)(2 * move.stagger())) {
            // Over: whatever is held is let go (the Maw flings it; the Drag slams it down).
            for (int i = 0; i < 3; i++) {
                if (st.held[i] != 0 && level.m_6815_(st.held[i]) instanceof LivingEntity held && held.m_6084_()) {
                    Vec3 away = UkakuFire.horizontal(held.m_20182_().m_82546_(npc.m_20182_()));
                    if (move == BikakuMove.DRAG) {
                        held.m_6469_(npc.m_269291_().m_269333_(npc), MOVE_DAMAGE * 0.8F);
                        held.m_20256_(new Vec3(0.0, -0.6, 0.0));
                    } else {
                        held.m_20256_(new Vec3(away.f_82479_ * 1.4, 0.6, away.f_82481_ * 1.4));
                    }
                    held.f_19864_ = true;
                }
            }
            st.move = BikakuMove.NONE;
            release(st);
            st.dirty = true;
        }
    }

    /** Worm {@code i}'s part of {@code move} landing on {@code body}. */
    private static void strike(EtoNpc npc, State st, int i, LivingEntity body, BikakuMove move, ServerLevel level) {
        float damage = MOVE_DAMAGE * (move == BikakuMove.ERUPTION ? 1.2F : move == BikakuMove.PURSUIT ? 0.8F : 0.7F);
        if (!body.m_6469_(npc.m_269291_().m_269333_(npc), damage)) {
            return;
        }
        npc.m_5634_(BITE_HEAL);
        switch (move) {
            case ERUPTION -> {
                body.m_20256_(new Vec3(0.0, 1.1, 0.0));
                body.f_19864_ = true;
                level.m_6263_(null, body.m_20185_(), body.m_20186_(), body.m_20189_(), HexKaguneSounds.BONE_CRACK.get(), SoundSource.HOSTILE, 1.2F, 0.6F);
            }
            case MAW, DRAG -> {
                st.held[i] = body.m_19879_();
                st.caughtBits |= 1 << i;
                st.dirty = true;
                level.m_6263_(null, body.m_20185_(), body.m_20186_() + 1.0, body.m_20189_(), HexKaguneSounds.KAGUNE_GRAB.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
            }
            default -> level.m_6263_(null, body.m_20185_(), body.m_20186_() + 1.0, body.m_20189_(), HexKaguneSounds.FLESH_IMPACT.get(), SoundSource.HOSTILE,
                1.0F, 0.9F);
        }
    }

    /** A catch held: up in front of him and shaken (the Maw), or hauled in to his feet (the Drag). */
    private static void hold(EtoNpc npc, int i, LivingEntity held, BikakuMove move, long age) {
        double yaw = Math.toRadians(npc.f_20883_);
        Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 side = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw)).m_82490_((i - 1) * 1.6);
        Vec3 spot = move == BikakuMove.DRAG ? npc.m_20182_().m_82549_(ahead.m_82490_(1.8)).m_82549_(side)
            : npc.m_20182_().m_82549_(ahead.m_82490_(3.0)).m_82549_(side).m_82520_(0.0, 2.6 + 0.3 * Math.sin(age * 1.3), 0.0);
        Vec3 pull = spot.m_82546_(held.m_20182_()).m_82490_(move == BikakuMove.DRAG ? 0.25 : 0.45);
        held.m_20256_(pull);
        held.f_19864_ = true;
        held.f_19789_ = 0.0F;
        if (move == BikakuMove.MAW && age % 6L == 0L) {
            held.m_6469_(npc.m_269291_().m_269333_(npc), MOVE_DAMAGE * 0.25F);
        }
    }

    private static void release(State st) {
        java.util.Arrays.fill(st.held, 0);
        st.caughtBits = 0;
    }

    // ---------------------------------------------------------------- not dying

    /** True while he is pulling himself back together. */
    static boolean mending(State st, long now) {
        return st.mendAt != Long.MIN_VALUE && now - st.mendAt < MEND;
    }

    /**
     * A blow of {@code amount} on him: while he mends he takes almost nothing; a blow that would kill him, if his mend is ready, leaves him
     * at one health, folded, mending (the owner: "his overwhelming strength and abnormal regeneration"). What he takes.
     */
    static float hurt(EtoNpc npc, State st, float amount, long now) {
        st.hurtAt = now;
        if (mending(st, now)) {
            return amount * MEND_TAKEN;
        }
        if (amount >= npc.m_21223_() && now >= st.mendReady) {
            st.mendAt = now;
            st.mendReady = now + MEND_EVERY;
            st.move = BikakuMove.NONE;
            st.bite = BikakuRules.Bite.NONE;
            release(st);
            st.out = true;
            st.foughtAt = now;
            st.dirty = true;
            return Math.max(0.0F, npc.m_21223_() - 1.0F);
        }
        return amount;
    }

    static void forget(EtoNpc npc, EtoBrain.Mind mind) {
        if (mind != null) {
            release(mind.worms);
        }
    }

    // ---------------------------------------------------------------- what every client is told

    static S2CBikakuState state(EtoNpc npc, State st) {
        double[][] points = new double[3][];
        for (int i = 0; i < 3; i++) {
            if (npc.m_9236_().m_6815_(st.targets[i]) instanceof LivingEntity mark) {
                Vec3 c = GhoulNpcBrain.centre(mark);
                points[i] = new double[] {c.f_82479_, c.f_82480_, c.f_82481_};
            }
        }
        long[] autoAt = new long[S2CBikakuState.AUTOS];
        java.util.Arrays.fill(autoAt, Long.MIN_VALUE);
        return new S2CBikakuState(npc.m_20148_(), st.bite.ordinal(), st.biteAt, st.aim.f_82479_, st.aim.f_82480_, st.aim.f_82481_, st.biteTarget,
            st.move.ordinal(), st.moveAt, st.movePoint.f_82479_, st.movePoint.f_82480_, st.movePoint.f_82481_, st.targets.clone(), st.held[0],
            st.caughtBits != 0, st.mendAt, false, Long.MIN_VALUE, Long.MIN_VALUE, autoAt, new int[S2CBikakuState.AUTOS], (float)REACH,
            (float)REACH, -1, Long.MIN_VALUE, new double[S2CBikakuState.ROUTES][][], BikakuForm.THREE.ordinal(), 3, st.held.clone(), st.caughtBits,
            points, 0, 0, -1, Long.MIN_VALUE, null, 0.6F, 1.8F, Long.MIN_VALUE);
    }

    static S2CNpcWings wings(EtoNpc npc, State st) {
        return new S2CNpcWings(npc.m_19879_(), st.out, st.outAt, false, Long.MIN_VALUE, 0, Long.MIN_VALUE, 1, Long.MIN_VALUE, false, Long.MIN_VALUE,
            st.mendAt, -1, Long.MIN_VALUE);
    }

    /** A player starts seeing him: how his worms are. */
    static void tracked(ServerPlayer player, EtoNpc npc) {
        if (npc.persona().worms() && npc.mind != null) {
            ControlNetwork.sendTo(player, state(npc, npc.mind.worms));
            ControlNetwork.sendTo(player, wings(npc, npc.mind.worms));
        }
    }
}
