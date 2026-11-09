package com.hex.kagune.control.server;

import com.hex.kagune.control.bikaku.BikakuForm;
import com.hex.kagune.control.bikaku.BikakuMove;
import com.hex.kagune.control.bikaku.BikakuRules;
import com.hex.kagune.control.family.ChildKaguneRules;
import com.hex.kagune.control.family.KaguneGenes;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CBikakuState;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.sound.HexKaguneSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 4.0.0 (the owner: "Make sure The child of a Bikaku user with all the other combinations matches perfectly"): a Bikaku parent's child's
 * worm (or two) in a fight, on the server ({@link ChildKaguneRules#bites}). It bites in turn (a snap, a lunge, a maul: Bikaku's own bites)
 * whatever it fights within its worm's reach, each bite landing on the tick its clients draw it shut and feeding it a little; with nothing
 * else to fight with, it closes in until its worm reaches. Every client draws its worm from the state a Bikaku player's are sent
 * ({@link S2CBikakuState}, under the child's id), at its size, in its colour.
 */
final class ChildWorm {
    private static final BikakuRules.Bite[] BITES = {BikakuRules.Bite.SNAP, BikakuRules.Bite.LUNGE, BikakuRules.Bite.MAUL};

    /** Its worm: the bite under way (since when, at whom, where it aimed), how many it has bitten, when the next can go. */
    static final class Worm {
        BikakuRules.Bite bite = BikakuRules.Bite.NONE;
        long biteAt = Long.MIN_VALUE;
        int target;
        Vec3 aim = Vec3.f_82478_;
        int bites;
        long ready = Long.MIN_VALUE;
        boolean dirty;
    }

    private ChildWorm() {
    }

    /**
     * A tick of its worm against {@code threat}: a bite under way carried on (landing on its tick), or a new one if its worm reaches and is
     * ready. True if the worm is what it does this tick (the rest of its kit waits); false to let its tentacle, its wings or its flesh act.
     */
    static boolean fight(ChildNpc npc, Worm w, int bits, double scale, float damage, LivingEntity threat, ServerLevel level, long now) {
        if (KaguneGenes.worms(bits) <= 0) {
            return false;
        }
        if (w.bite != BikakuRules.Bite.NONE) {
            long age = now - w.biteAt;
            if (age == w.bite.land()) {
                land(npc, w, scale, damage, level);
            }
            if (age < w.bite.ticks()) {
                npc.m_21573_().m_26573_();
                return true;
            }
            w.bite = BikakuRules.Bite.NONE;
            w.dirty = true;
        }
        double reach = ChildKaguneRules.WORM_REACH * scale;
        double distance = Math.sqrt(npc.m_20280_(threat));
        boolean sees = npc.m_142582_(threat);
        if (ChildKaguneRules.bites(bits, distance, sees, reach, now, w.ready)) {
            BikakuRules.Bite bite = BITES[w.bites++ % BITES.length];
            w.bite = bite;
            w.biteAt = now;
            w.target = threat.m_19879_();
            w.aim = GhoulNpcBrain.centre(threat);
            w.ready = now + bite.ticks() + ChildKaguneRules.WORM_PAUSE;
            w.dirty = true;
            npc.m_21573_().m_26573_();
            level.m_6263_(null, npc.m_20185_(), npc.m_20186_() + 1.0, npc.m_20189_(), HexKaguneSounds.KAGUNE_WHIP.get(), SoundSource.HOSTILE, 0.7F,
                (float)(1.0 + 0.3 * (1.0 - scale)));
            return true;
        }
        if (KaguneGenes.tails(bits) || KaguneGenes.wings(bits) > 0) {
            return false; // its other parent's kagune, between bites and beyond its worm's reach (Eto's flesh has had its turn)
        }
        // Its worm alone: in to where it reaches, then it stands and lets the worm work.
        if (distance > reach - 0.5 || !sees) {
            npc.m_21573_().m_5624_(threat, 1.25);
        } else {
            npc.m_21573_().m_26573_();
        }
        return true;
    }

    /** A bite shuts where it aimed: what it fights, if it is still there and within reach, is bitten, and the child feeds. */
    private static void land(ChildNpc npc, Worm w, double scale, float damage, ServerLevel level) {
        if (!(level.m_6815_(w.target) instanceof LivingEntity body) || !body.m_6084_() || !ChildKagune.fair(body, body)
            || Math.sqrt(npc.m_20280_(body)) > ChildKaguneRules.WORM_REACH * scale + 1.5) {
            return;
        }
        if (body.m_6469_(npc.m_269291_().m_269333_(npc), damage * ChildKaguneRules.WORM_SHARE * w.bite.power())) {
            npc.m_5634_(ChildKaguneRules.WORM_HEAL);
            level.m_6263_(null, body.m_20185_(), body.m_20186_() + 1.0, body.m_20189_(), HexKaguneSounds.FLESH_IMPACT.get(), SoundSource.HOSTILE, 0.8F, 1.0F);
        }
    }

    /** Out of a fight: its worm's bite let go. */
    static void calm(Worm w) {
        if (w.bite != BikakuRules.Bite.NONE) {
            w.bite = BikakuRules.Bite.NONE;
            w.dirty = true;
        }
    }

    /** Every client near told how its worm is, if that changed. */
    static void send(ChildNpc npc, Worm w, int bits) {
        if (w.dirty) {
            w.dirty = false;
            ControlNetwork.broadcastAround(npc, state(npc, w, bits));
        }
    }

    /** A player starts seeing it: how its worm is. */
    static void tracked(ServerPlayer player, ChildNpc npc, Worm w, int bits) {
        if (KaguneGenes.worms(bits) > 0) {
            ControlNetwork.sendTo(player, state(npc, w, bits));
        }
    }

    /** What its clients are told: its bite as a Bikaku player's (its worms, one or two, at rest otherwise). */
    static S2CBikakuState state(ChildNpc npc, Worm w, int bits) {
        int count = Math.max(1, KaguneGenes.worms(bits));
        long[] autoAt = new long[S2CBikakuState.AUTOS];
        java.util.Arrays.fill(autoAt, Long.MIN_VALUE);
        float reach = (float)ChildKaguneRules.WORM_REACH;
        return new S2CBikakuState(npc.m_20148_(), w.bite.ordinal(), w.biteAt, w.aim.f_82479_, w.aim.f_82480_, w.aim.f_82481_, w.target,
            BikakuMove.NONE.ordinal(), Long.MIN_VALUE, 0.0, 0.0, 0.0, new int[S2CBikakuState.TARGETS], 0, false, Long.MIN_VALUE, false,
            Long.MIN_VALUE, Long.MIN_VALUE, autoAt, new int[S2CBikakuState.AUTOS], reach, reach, -1, Long.MIN_VALUE,
            new double[S2CBikakuState.ROUTES][][], (count == 2 ? BikakuForm.TWIN : BikakuForm.ONE).ordinal(), count, new int[3], 0,
            new double[3][], 0, 0, -1, Long.MIN_VALUE, null, 0.6F, 1.8F, Long.MIN_VALUE);
    }
}
