package com.hex.kagune.control.server;

import com.hex.kagune.control.king.KingCombo;
import com.hex.kagune.control.king.KingForm;
import com.hex.kagune.control.king.KingRules;
import com.hex.kagune.control.king.PeaceRules;
import com.hex.kagune.control.king.WarRules;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.KenRules;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.3.19: Ken's fight in Peace ({@link KenRules}): the moment a fight finds him it comes over him (as a player's does, drawn on every
 * client), then its own kit, the very one a player wears ({@link KingServer.BodyKit}), on his body: the blades' chain with the launcher, the
 * blows in the air and the spike, the heavy held and let go, the six abilities (the lance and the burial held, the Ascent's slam from over
 * what he fights); and with them Eto's fists, Big Punch, Speed Kick and flips ({@link EtoOwl#perform}), and his dash. Its stats on him while
 * he wears it (twice his health, faster, harder to hurt); long out of a fight, it goes back into him.
 */
final class KenPeace {
    private static final UUID HEALTH = UUID.fromString("5e2b8b0c-7d55-4f1e-9a0b-1c0de5a10a01");
    private static final UUID SPEED = UUID.fromString("5e2b8b0c-7d55-4f1e-9a0b-1c0de5a10a02");

    /** His side of it: the dash under way, what he holds and till when, his blades' chain, his own waits. */
    static final class State {
        long dashUntil = Long.MIN_VALUE;
        Vec3 dashWay = Vec3.f_82478_;
        long dashReady = Long.MIN_VALUE;
        long heavyReady = Long.MIN_VALUE;
        /** What he holds: -2 nothing, -1 the heavy, else that ability's slot; and when he lets it go. */
        int holding = -2;
        long holdUntil = Long.MIN_VALUE;
        boolean striking;
        long struckAt = Long.MIN_VALUE;
        int chained;
        boolean launch;
        /** When his lance was driven out (he calls it back himself a beat after it lands), or never. */
        long lanceAt = Long.MIN_VALUE;
        /** An ability that would not start (nothing in its reach) waits this long before he tries it again, by slot. */
        final long[] retry = {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
    }

    private KenPeace() {
    }

    /** True if {@code npc} fights in a final kakuja (Ken). */
    static boolean wears(EtoNpc npc) {
        return npc.persona().finalKakuja() != KingForm.NONE;
    }

    // ---------------------------------------------------------------- every tick

    /** Every tick, in a fight or not: its kit's moves, his dash, and it going back into him once the fight is long over. */
    static void tick(EtoNpc npc, EtoBrain.Mind mind, LivingEntity target, ServerLevel level, long now) {
        if (!wears(npc)) {
            return;
        }
        KingServer.bodyTick(npc, now);
        State st = mind.peace;
        if (now < st.dashUntil) {
            npc.m_20256_(new Vec3(st.dashWay.f_82479_, Math.min(npc.m_20184_().f_82480_, 0.0), st.dashWay.f_82481_));
            npc.f_19864_ = true;
            npc.f_19789_ = 0.0F;
        }
        KingServer.Worn worn = KingServer.wornBy(npc);
        if (worn != null && worn.on() && target == null && (mind.foughtAt == Long.MIN_VALUE || now - mind.foughtAt > KenRules.OFF_AFTER)) {
            KingServer.BodyKit kit = KingServer.bodyKit(npc);
            if (kit == null || !kit.busyOf(npc)) {
                off(npc, mind);
            }
        }
    }

    /** It goes back into him (drawn so), and its stats with it. */
    static void off(EtoNpc npc, EtoBrain.Mind mind) {
        KingServer.unwear(npc, true);
        unboost(npc);
        mind.peace.holding = -2;
        mind.peace.striking = false;
    }

    /** He is gone (dead, unloaded): what he wore is forgotten. */
    static void forget(EtoNpc npc) {
        if (wears(npc)) {
            KingServer.forgetBody(npc);
        }
    }

    // ---------------------------------------------------------------- his fight

    /** A tick of his fight against {@code target}; false when Eto's grammar of the body is to go on (one of her moves under way). */
    static boolean fight(EtoNpc npc, EtoBrain.Mind mind, LivingEntity target, ServerLevel level, long now, EtoRules.Phase phase) {
        KingForm form = npc.persona().finalKakuja();
        if (form == KingForm.NONE) {
            return false;
        }
        State st = mind.peace;
        KingServer.Worn worn = KingServer.wornBy(npc);
        if (worn == null || !worn.on()) {
            // The fight has found him: it comes over him at once.
            KingServer.wear(npc, form);
            boost(npc);
            npc.m_21153_(Math.min(npc.m_21233_(), npc.m_21223_() + npc.m_21233_() * 0.25F));
            EtoOwl.face(npc, target);
            npc.m_21573_().m_26573_();
            st.dashReady = now + 40L;
            return true;
        }
        if (KingServer.bodyComing(npc)) {
            EtoOwl.face(npc, target);
            npc.m_21573_().m_26573_();
            return true;
        }
        KingServer.BodyKit kit = KingServer.bodyKit(npc);
        if (kit == null) {
            return false;
        }
        if (mind.owl.busy()) {
            return false; // a flip, a fist, the kick under way: EtoOwl carries it
        }
        if (now < st.dashUntil) {
            EtoOwl.face(npc, target);
            return true;
        }
        aim(npc, target);
        double gap = EtoOwl.gap(npc, target);
        int id = target.m_19879_();
        // What he holds, let go when he means to.
        if (st.holding != -2) {
            npc.m_21573_().m_26573_();
            if (now >= st.holdUntil || !kit.holdingOf(npc, st.holding)) {
                if (st.holding == -1) {
                    kit.heavyOf(npc, 0, id);
                } else if (kit.holdingOf(npc, st.holding)) {
                    kit.keyOf(npc, st.holding, WarRules.RELEASE, id);
                    if (st.holding == 3) {
                        st.lanceAt = now;
                    }
                }
                st.holding = -2;
            }
            return true;
        }
        // Up on his wings over it: down onto it.
        double risen = kit.risenOf(npc);
        if (!Double.isNaN(risen)) {
            npc.m_21573_().m_26573_();
            if (risen >= KenRules.SLAM_HEIGHT && level(npc, target) <= KenRules.SLAM_OVER) {
                kit.keyOf(npc, 2, WarRules.PRESS, id);
            }
            return true;
        }
        if (st.lanceAt != Long.MIN_VALUE && now - st.lanceAt >= 30L) {
            // His lance has had its moment: he calls it back (B again), and is free to move on.
            st.lanceAt = Long.MIN_VALUE;
            kit.keyOf(npc, 3, WarRules.PRESS, id);
        }
        if (kit.busyOf(npc)) {
            // His blades are taken. A chain under way goes on while it is in reach (the launcher after two, the blows in the air after).
            if (st.striking && gap <= KenRules.REACH + 0.8 && now - st.struckAt >= 4L) {
                st.chained++;
                int flags = st.launch && st.chained >= KingCombo.LAUNCH_AFTER ? KingCombo.JUMP : 0;
                if (!npc.m_20096_()) {
                    flags |= KingCombo.AIR;
                }
                kit.strikeOf(npc, id, flags);
                st.struckAt = now;
            } else if (!st.striking || gap > KenRules.REACH + 0.8) {
                npc.m_21573_().m_26573_();
            }
            return true;
        }
        st.striking = false;
        KenRules.Ready ready = new KenRules.Ready(waits(npc, st, 0), waits(npc, st, 1), waits(npc, st, 2), waits(npc, st, 3), waits(npc, st, 4),
            waits(npc, st, 5), st.dashReady, st.heavyReady, mind.owl.punchReady, mind.owl.kickReady, mind.owl.flipReady);
        float health = npc.m_21223_() / Math.max(1.0F, npc.m_21233_());
        boolean hurt = mind.hurtAt != Long.MIN_VALUE && now - mind.hurtAt < 40L; // (never hurt: no overflow into "just now")
        KenRules.Act act = KenRules.choose(gap, npc.m_142582_(target), npc.m_20096_(), health, hurt, foes(npc, level), now, ready,
            npc.m_217043_().m_188500_());
        switch (act) {
            case STRIKE -> {
                npc.m_21573_().m_26573_();
                st.striking = true;
                st.chained = 0;
                st.launch = npc.m_217043_().m_188500_() < KenRules.LAUNCH_CHANCE;
                kit.strikeOf(npc, id, 0);
                st.struckAt = now;
            }
            case HEAVY -> {
                npc.m_21573_().m_26573_();
                kit.heavyOf(npc, KingCombo.PRESS, id);
                st.holding = -1;
                st.holdUntil = now + (npc.m_217043_().m_188499_() ? KenRules.HEAVY_HOLD : KenRules.HEAVY_TAP);
                st.heavyReady = now + KenRules.HEAVY_EVERY;
            }
            case WALTZ, AEGIS, ASCENT, HORIZON -> ability(npc, st, kit, KenRules.slot(act), id, now);
            case PIERCE -> {
                if (ability(npc, st, kit, 3, id, now)) {
                    st.holding = 3;
                    st.holdUntil = now + KenRules.PIERCE_HOLD;
                }
            }
            case REBIRTH -> {
                if (ability(npc, st, kit, 4, id, now)) {
                    st.holding = 4;
                    st.holdUntil = now + KenRules.REBIRTH_HOLD;
                }
            }
            case DASH_IN, DASH_OUT -> dash(npc, st, target, act == KenRules.Act.DASH_IN, level, now);
            case BLITZ -> EtoOwl.perform(npc, mind, target, level, now, phase, EtoRules.Act.STRIKE);
            case PUNCH -> EtoOwl.perform(npc, mind, target, level, now, phase, EtoRules.Act.PUNCH);
            case KICK -> EtoOwl.perform(npc, mind, target, level, now, phase, EtoRules.Act.KICK);
            case FLIP_IN -> EtoOwl.perform(npc, mind, target, level, now, phase, EtoRules.Act.FLIP_IN);
            case FLIP_BACK -> EtoOwl.perform(npc, mind, target, level, now, phase, EtoRules.Act.FLIP_BACK);
            default -> npc.m_21573_().m_5624_(target, phase.chase * 1.25);
        }
        return true;
    }

    /** One of Peace's six pressed at {@code id}; true if it began (his wait then set to his own, shorter than a player's). */
    private static boolean ability(EtoNpc npc, State st, KingServer.BodyKit kit, int slot, int id, long now) {
        npc.m_21573_().m_26573_();
        long before = KingServer.ready(npc, slot);
        kit.keyOf(npc, slot, WarRules.PRESS, id);
        long after = KingServer.ready(npc, slot);
        if (after == before || after <= now) {
            st.retry[slot] = now + 40L; // it would not start: something else meanwhile
            return false;
        }
        PeaceRules.Ability ability = PeaceRules.Ability.bySlot(slot);
        if (ability != null) {
            KingServer.cooldown(npc, slot, now + KenRules.cooldown(ability.cooldown()));
        }
        return true;
    }

    /** When ability {@code slot} is his to throw again: its wait, or a retry after it would not start. */
    private static long waits(EtoNpc npc, State st, int slot) {
        return Math.max(KingServer.ready(npc, slot), st.retry[slot]);
    }

    /** His dash: in at it (or back out from it), low and fast, gone in a few ticks. */
    private static void dash(EtoNpc npc, State st, LivingEntity target, boolean in, ServerLevel level, long now) {
        Vec3 way = UkakuFire.horizontal(target.m_20182_().m_82546_(npc.m_20182_()));
        if (way.m_82556_() < 1.0E-6) {
            way = UkakuFire.horizontal(npc.m_20154_());
        }
        if (!in) {
            way = way.m_82490_(-1.0);
        }
        double speed = KenRules.DASH_SPEED;
        if (in) {
            // Never past it: the dash stops short of it, in his blades' reach.
            speed = Math.min(speed, Math.max(0.6, (EtoOwl.gap(npc, target) - KenRules.REACH * 0.6) / KenRules.DASH_TICKS));
        }
        st.dashWay = way.m_82490_(speed);
        st.dashUntil = now + KenRules.DASH_TICKS;
        st.dashReady = now + KenRules.DASH_EVERY;
        npc.m_21573_().m_26573_();
        npc.m_20256_(new Vec3(st.dashWay.f_82479_, 0.12, st.dashWay.f_82481_));
        npc.f_19864_ = true;
        UkakuFire.sound(level, npc.m_20182_(), "entity.player.attack.sweep", 0.6F, 1.6F);
    }

    /** He faces it, and looks at it (his blades cut along his look). */
    private static void aim(EtoNpc npc, LivingEntity target) {
        EtoOwl.face(npc, target);
        double dx = target.m_20185_() - npc.m_20185_();
        double dz = target.m_20189_() - npc.m_20189_();
        double dy = target.m_20186_() + target.m_20206_() * 0.5 - (npc.m_20186_() + npc.m_20206_() * 0.85);
        float pitch = (float)-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        npc.m_146926_(Math.max(-60.0F, Math.min(60.0F, pitch)));
    }

    /** How far {@code target} is from under him, level. */
    private static double level(EtoNpc npc, LivingEntity target) {
        double dx = target.m_20185_() - npc.m_20185_();
        double dz = target.m_20189_() - npc.m_20189_();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** How many of his foes are about him. */
    private static int foes(EtoNpc npc, ServerLevel level) {
        double r = 10.0;
        AABB around = new AABB(npc.m_20185_() - r, npc.m_20186_() - 4.0, npc.m_20189_() - r, npc.m_20185_() + r, npc.m_20186_() + 4.0, npc.m_20189_() + r);
        return level.m_6443_(LivingEntity.class, around, body -> body != npc && body.m_6084_() && (body instanceof Enemy || EtoBrain.foe(npc, body)))
            .size();
    }

    // ---------------------------------------------------------------- what it gives him

    /** A blow on him in it: what its shields leave, and of that what it lets through (none while it comes over him). */
    static float hurt(EtoNpc npc, net.minecraft.world.entity.Entity attacker, net.minecraft.world.entity.Entity direct, float amount) {
        KingServer.Worn worn = KingServer.wornBy(npc);
        if (worn == null || !worn.on()) {
            return amount;
        }
        if (KingServer.bodyComing(npc)) {
            return 0.0F;
        }
        float left = KingServer.bodyHurt(npc, attacker, direct, amount);
        return (float)(left * (1.0 - KingRules.reduction(worn.form())));
    }

    private static void boost(EtoNpc npc) {
        double[] boosts = KingRules.boosts(npc.persona().finalKakuja());
        modifier(npc.m_21051_(Attributes.f_22276_), HEALTH, "Final kakuja health", boosts[3]);
        modifier(npc.m_21051_(Attributes.f_22279_), SPEED, "Final kakuja speed", boosts[1]);
    }

    private static void unboost(EtoNpc npc) {
        for (AttributeInstance instance : new AttributeInstance[] {npc.m_21051_(Attributes.f_22276_), npc.m_21051_(Attributes.f_22279_)}) {
            if (instance != null) {
                instance.m_22120_(HEALTH);
                instance.m_22120_(SPEED);
            }
        }
        if (npc.m_21223_() > npc.m_21233_()) {
            npc.m_21153_(npc.m_21233_());
        }
    }

    private static void modifier(AttributeInstance instance, UUID id, String name, double amount) {
        if (instance == null || amount == 0.0 || instance.m_22111_(id) != null) {
            return;
        }
        instance.m_22118_(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_BASE));
    }
}
