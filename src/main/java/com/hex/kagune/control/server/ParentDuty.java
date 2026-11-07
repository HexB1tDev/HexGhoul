package com.hex.kagune.control.server;

import com.hex.kagune.control.family.ChildLines;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.family.ChildTemper;
import com.hex.kagune.control.family.Parenting;
import com.hex.kagune.control.family.TalkCues;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CNpcSay;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoNpc;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.3.49: a companion parent looking after its children in the world ({@link Parenting}'s rules and words). It notices a child of its own
 * (in sight within {@link Parenting#NOTICES}, or gone from home a while) that has strayed too far, is out at night, or has gone on a
 * rampage with its kagune; it stops what it is doing (a chore, a cuddle) and goes after it, running for a rampage. Reached, it stops the
 * child (its fight over: {@link Parenting#CALMED}), and they have it out, back and forth in their bubbles; a toddler is picked up and
 * carried home, an older child walks home at its side; at the door, a line each (3.2.0: a back and forth that knows why it went after it,
 * {@link Parenting#home}). Its kagune coming out gets a word from a parent near.
 * Asleep, a parent is woken by a child out at night or fighting too long (HomeLife). It never leaves a fight of its own for it.
 */
final class ParentDuty {
    /**
     * What it is doing about which child: why, which (its body's id), since when, whether it got to it, whether it carries it; 1.3.53: their
     * talk (a {@link Conversation} both stay for), and whether that is the word at the door (the duty is over once it is said).
     */
    static final class Duty {
        Parenting.Why why;
        UUID child;
        long since;
        boolean reached;
        boolean carrying;
        Conversation talk;
        boolean home;
    }

    private ParentDuty() {
    }

    // ---------------------------------------------------------------- the parent

    /** Its duty this tick (from its calm, before its chores): true while a child has it. */
    static boolean tick(EtoNpc npc, EtoBrain.Mind mind, ServerLevel level, long now) {
        if (mind.target != 0) {
            // Its own fight first; a toddler it carries home is put down out of harm's way first.
            if (mind.duty != null && mind.duty.carrying) {
                ChildNpc held = body(level, mind.duty.child);
                if (held != null && held.mind != null && ChildBrain.carried(held)) {
                    ChildBrain.putDown(held, held.mind);
                }
                mind.cradling = null;
                mind.duty.carrying = false;
            }
            return false;
        }
        if (mind.duty == null) {
            if (now % 20L != Math.floorMod(npc.m_19879_(), 20)) {
                return false;
            }
            mind.duty = look(npc, level, now, false);
            if (mind.duty == null) {
                return false;
            }
            if (mind.chore != null && mind.duty.why != com.hex.kagune.control.family.Parenting.Why.RAMPAGE) {
                mind.duty = null; // 2.0.8: a chore under way is finished first (a rampage can't wait)
                return false;
            }
            HomeChores.stop(npc, mind, level);
            if (mind.cradling != null) {
                ParentCare.drop(npc, mind, level, now); // whoever it held is put down: it is going after another
            }
        }
        Duty d = mind.duty;
        ChildNpc child = body(level, d.child);
        Children.Child known = child == null ? null : Children.get(level, child.childId());
        if (child == null || known == null || !child.m_6084_() || child.mind == null || child.m_9236_() != level || now - d.since > Parenting.GIVES_UP) {
            finish(npc, mind, child, level);
            return false;
        }
        ChildStage stage = known.stage(now);
        Households.Home house = Households.forChild(level, known);
        if (!d.reached) {
            double distance = Math.sqrt(child.m_20280_(npc));
            if (distance > Parenting.REACHES) {
                if (now % 10L == 0L) {
                    npc.m_21573_().m_5624_(child, d.why == Parenting.Why.RAMPAGE ? 1.4 : 1.1);
                }
                npc.m_21563_().m_24960_(child, 30.0F, 30.0F);
                return true;
            }
            // Got to it: the child stops (its fight over), and they have it out: a talk both stay for (1.3.53: Conversation).
            d.reached = true;
            npc.m_21573_().m_26573_();
            child.mind.calmedUntil = now + Parenting.CALMED;
            child.mind.fetchedAt = now;
            child.mind.target = 0;
            child.mind.turned = false;
            child.mind.dirty = true;
            Conversation.end(child, com.hex.kagune.control.family.Talks.End.INTERRUPTED, level, now); // whatever it was saying to a sibling
            List<Parenting.Line> talk = Parenting.talk(d.why, npc.persona().key(), stage, temper(known), known.name,
                ChildLines.call(stage, npc.persona().female()), npc.m_217043_().m_188503_(1 << 20));
            d.talk = Conversation.start(level, now, "parent " + d.why.name().toLowerCase(java.util.Locale.ROOT), child, List.of(child, npc), List.of(),
                lines(child, npc, talk, d.why == Parenting.Why.RAMPAGE ? TalkCues.Scene.RAMPAGE : TalkCues.Scene.SCOLDED, temper(known)));
            if (stage.carried() && house != null) {
                ChildBrain.pickUp(child, npc);
                d.carrying = true;
                mind.cradling = child.m_20148_();
                mind.cradleUntil = now + Parenting.GIVES_UP;
                EtoBrain.gesture(mind, EtoGesture.PET, now);
            }
            return true;
        }
        // 1.3.53: the talk over (both stayed for it: EtoBrain and ChildBrain hold them while it goes on). Cut short (the child hurt, a fight,
        // a player wanting it, gone): the duty is over too, for now. The word at the door said: done.
        if (d.talk != null && d.talk.over() && (d.home || d.talk.ended != com.hex.kagune.control.family.Talks.End.DONE)) {
            finish(npc, mind, child, level);
            return false;
        }
        if (d.talk != null && !d.talk.over()) {
            npc.m_21573_().m_26573_(); // (held by the talk itself; only here if her hold was skipped this tick)
            return true;
        }
        // Said its piece: home, the child carried or at its side; nowhere to take it, done once they have talked.
        if (house == null || d.why == Parenting.Why.TRANSFORM) {
            finish(npc, mind, child, level);
            return false;
        }
        BlockPos home = house.pos();
        Vec3 door = new Vec3(home.m_123341_() + 0.5, home.m_123342_(), home.m_123343_() + 0.5);
        boolean childHome = child.m_20238_(door) <= Math.pow(Math.max(3.0, house.radius() - 3.0), 2);
        boolean selfHome = npc.m_20238_(door) <= Math.pow(Math.max(3.0, house.radius() - 3.0), 2);
        if (childHome && (selfHome || d.carrying)) {
            // At the door: put down, and a line each (1.3.53: both stay for it; the duty ends once it is said).
            if (d.carrying) {
                ChildBrain.putDown(child, child.mind);
                d.carrying = false;
                mind.cradling = null;
                if (mind.gesture == EtoGesture.PET) {
                    EtoBrain.gesture(mind, EtoGesture.NONE, now);
                }
            }
            if (child.mind.escort != null && npc.m_20148_().equals(child.mind.escort)) {
                child.mind.escort = null;
            }
            d.home = true;
            // 3.2.0: the word at the door knows why it went after the child (d.why: it strayed, was out at night, or on a rampage).
            d.talk = Conversation.start(level, now, "parent home", child, List.of(child, npc), List.of(), lines(child, npc,
                Parenting.home(d.why, npc.persona().key(), stage, temper(known), known.name, ChildLines.call(stage, npc.persona().female()),
                    npc.m_217043_().m_188503_(1 << 20)), TalkCues.Scene.HOME, temper(known)));
            if (d.talk == null) {
                finish(npc, mind, child, level);
            }
            return true;
        }
        if (!d.carrying) {
            child.mind.escort = npc.m_20148_();
            child.mind.escortUntil = now + 200L;
        }
        if (now % 10L == 0L) {
            npc.m_21573_().m_26519_(door.f_82479_, door.f_82480_, door.f_82481_, 0.9);
        }
        return true;
    }

    /** Its duty over: whatever it carried put down, the child let go. */
    private static void finish(EtoNpc npc, EtoBrain.Mind mind, ChildNpc child, ServerLevel level) {
        Duty d = mind.duty;
        mind.duty = null;
        if (d != null && d.talk != null && !d.talk.over()) {
            Conversation.end(npc, com.hex.kagune.control.family.Talks.End.INTERRUPTED, level, level.m_46467_());
        }
        if (d != null && d.carrying && child != null && child.mind != null && ChildBrain.carried(child)) {
            ChildBrain.putDown(child, child.mind);
        }
        if (d != null && d.carrying) {
            mind.cradling = null;
        }
        if (child != null && child.mind != null && npc.m_20148_().equals(child.mind.escort)) {
            child.mind.escort = null;
        }
    }

    /**
     * A child of its own that needs it now, and why (null: none): one that has strayed too far or is out at night, or is on a rampage
     * (that first); noticed in sight, or gone a while ({@code anyway}: woken in the night, it knows a bed is empty).
     */
    static Duty look(EtoNpc npc, ServerLevel level, long now, boolean anyway) {
        String key = Pregnancies.npc(npc.persona().key());
        Duty best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Children.Child known : Children.of(level, key)) {
            ChildNpc child = body(level, known);
            if (child == null || child.mind == null || !child.m_6084_() || child.m_9236_() != level) {
                continue;
            }
            double distance = Math.sqrt(child.m_20280_(npc));
            if (distance > 96.0 || now - child.mind.fetchedAt < Parenting.AGAIN_AFTER) {
                continue;
            }
            // 1.3.55: someone else (a big brother or sister) is already walking it home: one is enough.
            if (child.mind.escort != null && now < child.mind.escortUntil && !child.mind.escort.equals(npc.m_20148_())) {
                continue;
            }
            ChildStage stage = known.stage(now);
            Households.Home house = Households.forChild(level, known);
            double beyond = house == null ? -1.0 : Math.sqrt(child.m_20238_(new Vec3(house.pos().m_123341_() + 0.5, house.pos().m_123342_(),
                house.pos().m_123343_() + 0.5))) - house.radius();
            ChildKagune.Fight fight = child.mind.kagune;
            float health = child.m_21223_() / Math.max(1.0F, child.m_21233_());
            Parenting.Why why = null;
            if (fight.out && Parenting.rampage(stage, now - fight.outAt, Math.max(0.0, beyond), health)) {
                why = Parenting.Why.RAMPAGE;
            } else if (house != null) {
                why = Parenting.strayed(stage, beyond, HomeLife.night(level.m_46468_(), true), child.following() != null, ChildBrain.carried(child));
            }
            if (why == null) {
                continue;
            }
            long missing = child.mind.outsideSince == Long.MIN_VALUE ? 0L : now - child.mind.outsideSince;
            boolean noticed = why == Parenting.Why.RAMPAGE ? distance <= 64.0 : Parenting.notices(distance, npc.m_142582_(child), anyway ? Long.MAX_VALUE : missing);
            if (!noticed) {
                continue;
            }
            double rank = why == Parenting.Why.RAMPAGE ? distance - 1000.0 : distance;
            if (rank < bestDistance) {
                bestDistance = rank;
                best = new Duty();
                best.why = why;
                best.child = child.m_20148_();
                best.since = now;
            }
        }
        return best;
    }

    /** True if a child of hers needs her badly enough to get up for: out at night a while, or on a rampage (HomeLife: she wakes). */
    static boolean wakes(EtoNpc npc, ServerLevel level, long now) {
        Duty d = look(npc, level, now, false);
        if (d == null) {
            return false;
        }
        ChildNpc child = body(level, d.child);
        return d.why == Parenting.Why.RAMPAGE || child != null && child.mind != null && child.mind.outsideSince != Long.MIN_VALUE
            && now - child.mind.outsideSince >= Parenting.MISSED_AFTER / 2;
    }

    // ---------------------------------------------------------------- the child's side

    /**
     * 1.3.49: its kagune just came out: a parent of its own near (who sees it) has a word about it, and it answers, now and then the parent
     * again; not too often ({@link Parenting#REMARKS_EVERY}).
     */
    static void transformed(ChildNpc child, Children.Child known, ServerLevel level, long now) {
        if (child.mind == null || now - child.mind.remarkedAt < Parenting.REMARKS_EVERY) {
            return;
        }
        EtoNpc parent = nearParent(child, known, level, 24.0);
        if (parent == null || Conversation.in(parent) != null) {
            return;
        }
        child.mind.remarkedAt = now;
        ChildStage stage = known.stage(now);
        queue(child, parent, Parenting.talk(Parenting.Why.TRANSFORM, parent.persona().key(), stage, temper(known), known.name,
            ChildLines.call(stage, parent.persona().female()), parent.m_217043_().m_188503_(1 << 20)), now);
    }

    /**
     * Its parent (or an older brother or sister) walks it home at their side ({@code escort}): true while it does (nothing else of its own
     * meanwhile). 1.3.53: standing still for a talk is the talk's own ({@link Conversation#hold}).
     */
    static boolean led(ChildNpc npc, ChildBrain.Mind mind, ServerLevel level, long now) {
        if (mind.escort != null && now < mind.escortUntil) {
            Entity parent = level.m_8791_(mind.escort);
            if (parent instanceof LivingEntity living && living.m_6084_() && living.m_9236_() == level) {
                // At its side: a step behind, walking when they walk.
                if (npc.m_20280_(living) > 2.2 * 2.2 && now % 5L == 0L) {
                    npc.m_21573_().m_5624_(living, 1.05);
                }
                return true;
            }
            mind.escort = null;
        }
        return false;
    }

    /**
     * 1.3.53: a parent's talk with its child as a {@link Conversation}'s lines (each by its speaker's entity id); 1.3.57: each with what its
     * speaker does as it says it ({@link TalkCues}: the parent's gesture, the child's cue, in {@code scene}, by its {@code temper}).
     */
    static List<Conversation.Line> lines(ChildNpc child, EtoNpc parent, List<Parenting.Line> talk, TalkCues.Scene scene, ChildTemper temper) {
        List<Conversation.Line> out = new java.util.ArrayList<>();
        int seed = parent.m_217043_().m_188503_(1 << 20);
        for (int i = 0; i < talk.size(); i++) {
            Parenting.Line line = talk.get(i);
            if (line.parent()) {
                out.add(new Conversation.Line(parent.m_19879_(), line.text(), com.hex.kagune.control.family.ChildCue.NONE,
                    TalkCues.parent(line.text(), scene, parent.persona().key(), i, seed + i)));
            } else {
                out.add(new Conversation.Line(child.m_19879_(), line.text(), TalkCues.child(line.text(), scene, temper, false, i, seed + i),
                    EtoGesture.NONE));
            }
        }
        return out;
    }

    /** A remark's words queued (a word about its kagune, in a fight: no one stops for it), each in its turn; when the last ends. */
    static long queue(ChildNpc child, EtoNpc parent, List<Parenting.Line> talk, long now) {
        long at = Math.max(now, child.mind.talkFree);
        for (Parenting.Line line : talk) {
            child.mind.talk.add(new Said(line.parent() ? parent.m_19879_() : child.m_19879_(), line.text(), at));
            at += Parenting.pause(line.text());
        }
        child.mind.talkFree = at;
        return at;
    }

    /** One line due: who says it (an entity id), what, on which tick. */
    record Said(int speaker, String text, long at, com.hex.kagune.control.family.ChildCue cue) {
        Said(int speaker, String text, long at) {
            this(speaker, text, at, com.hex.kagune.control.family.ChildCue.NONE);
        }
    }

    /**
     * Its talk this tick: the lines that are due said, in the speaker's bubble, to every player near who can see the speaker. 1.3.52: a
     * line answered by someone else is up only until the answer comes ({@link #life}): the two bubbles take turns, never both up at once.
     */
    static void talk(ChildNpc npc, ChildBrain.Mind mind, ServerLevel level, long now) {
        while (!mind.talk.isEmpty() && mind.talk.peek().at() <= now) {
            Said said = mind.talk.poll();
            Entity speaker = level.m_6815_(said.speaker());
            if (!(speaker instanceof LivingEntity body) || !body.m_6084_()) {
                continue;
            }
            int ticks = life(said, mind.talk.peek(), now);
            for (ServerPlayer player : level.m_6907_()) {
                if (SceneServer.sees(body, player, 24.0)) {
                    ControlNetwork.sendTo(player, new S2CNpcSay(said.speaker(), said.text(), ticks));
                }
            }
            Conversation.act(body, said.cue(), EtoGesture.NONE, now); // 1.3.58: what it does as it says it
        }
    }

    /**
     * 1.3.52: how long {@code said}'s bubble stays up (ticks; 0: as long as it takes to read): until {@code next} is said, if someone else
     * says it (the same speaker's next line takes its bubble's place anyway); never less than a second.
     */
    static int life(Said said, Said next, long now) {
        return next == null ? 0 : com.hex.kagune.control.npc.Speech.turn(said.speaker(), next.speaker(), next.at() - now);
    }

    // ---------------------------------------------------------------- finding them

    /** The body of a child by its entity UUID, in this level, or null. */
    private static ChildNpc body(ServerLevel level, UUID id) {
        return id != null && level.m_8791_(id) instanceof ChildNpc child ? child : null;
    }

    /** The body of {@code known} in this level (as {@code UniqueNpcs} keeps it), or null. */
    private static ChildNpc body(ServerLevel level, Children.Child known) {
        com.hex.kagune.control.npc.UniqueNpc unique = UniqueNpcs.get(level, "child:" + known.id);
        return unique == null || unique.body == null ? null : body(level, unique.body);
    }

    /** A companion parent of {@code known} within {@code range} of {@code child} who sees it, or null. */
    private static EtoNpc nearParent(ChildNpc child, Children.Child known, ServerLevel level, double range) {
        EtoNpc best = null;
        double bestDistance = range * range;
        for (EtoNpc npc : level.m_6443_(EtoNpc.class, child.m_20191_().m_82400_(range), body -> body.m_6084_() && body.mind != null)) {
            String key = Pregnancies.npc(npc.persona().key());
            double distance = npc.m_20280_(child);
            if (known.parent(key) && distance <= bestDistance && npc.m_142582_(child)) {
                best = npc;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static ChildTemper temper(Children.Child known) {
        return ChildTemper.of(known.id, known.mother, known.father);
    }
}
