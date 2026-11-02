package com.hex.kagune.control.server;

import com.hex.kagune.control.family.ChildCue;
import com.hex.kagune.control.family.ChildLines;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.family.ChildTemper;
import com.hex.kagune.control.family.Parenting;
import com.hex.kagune.control.family.Talks;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CNpcSay;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.Speech;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * 1.3.53: one talk that holds together ({@link Talks}' rules; the owner: "prevent NPCs from walking away during conversations"). A parent
 * scolding a child that wandered, brothers and sisters squabbling, a parent breaking it up, a word at the door: everyone in it is a member,
 * and every member's brain asks this first ({@link #going}, {@link #hold}): it stands, its body turned to whoever speaks; nothing of its
 * own starts meanwhile (no moments, chores, pick-ups, tag, idle words). One who comes to it (the child who starts a talk with a sibling, a
 * parent coming over to a squabble) walks up first, and each line waits until its speaker is near. The lines are said in turn, each up
 * until the reply ({@code S2CNpcSay}'s life). It ends when its last line has had its time, or at once (the rest dropped) when one of them is
 * gone, walks off, is hurt, fights, falls asleep, or a player wants one of them; walked off on, whoever stays may say so. Kept by every
 * member's mind ({@code ChildBrain.Mind.conversation}, {@code EtoBrain.Mind.conversation}), ticked once a tick by whichever ticks first, so
 * it goes on as long as any of them is about.
 */
final class Conversation {
    /** One line: who says it (an entity id), what; 1.3.57: and what it does as it says it (a child's cue, a companion's gesture). */
    record Line(int speaker, String text, ChildCue cue, EtoGesture gesture) {
        Line(int speaker, String text) {
            this(speaker, text, ChildCue.NONE, EtoGesture.NONE);
        }
    }

    private final List<Integer> members = new ArrayList<>();
    private final Set<Integer> coming = new HashSet<>();
    private final int anchor;
    private final ArrayDeque<Line> lines = new ArrayDeque<>();
    final long since;
    /** What it is (for the log and the handoff: "sibling squabble", "parent wandered"...). */
    final String what;
    private long nextAt;
    private long waiting = Long.MIN_VALUE;
    private int speaker;
    private int previous;
    private long tickedAt = Long.MIN_VALUE;
    /** How it ended (null while it goes on). */
    Talks.End ended;

    private Conversation(int anchor, long now, String what) {
        this.anchor = anchor;
        this.since = now;
        this.nextAt = now;
        this.what = what;
    }

    // ---------------------------------------------------------------- beginning one

    /**
     * A talk among {@code members} round {@code anchor} (who stays where it is; the others gather round it), {@code coming} walking up to it
     * first, its {@code lines} said in turn; null if any of them is already in one. Whatever moment a child was having is over; a companion's
     * chore is put down.
     */
    static Conversation start(ServerLevel level, long now, String what, LivingEntity anchor, Collection<? extends LivingEntity> members,
                              Collection<? extends LivingEntity> coming, List<Line> lines) {
        for (LivingEntity member : members) {
            if (in(member) != null) {
                return null;
            }
        }
        Conversation talk = new Conversation(anchor.m_19879_(), now, what);
        for (LivingEntity member : members) {
            talk.enter(member, level);
        }
        for (LivingEntity member : coming) {
            talk.coming.add(member.m_19879_());
        }
        talk.lines.addAll(lines);
        return talk;
    }

    /** {@code who} joins it ({@code comes}: walking over first), with more lines after the rest; false if it is in another talk. */
    boolean join(ServerLevel level, LivingEntity who, boolean comes, List<Line> more) {
        if (this.ended != null || in(who) != null && in(who) != this) {
            return false;
        }
        if (!this.members.contains(who.m_19879_())) {
            this.enter(who, level);
        }
        if (comes) {
            this.coming.add(who.m_19879_());
        }
        this.lines.addAll(more);
        return true;
    }

    private void enter(LivingEntity member, ServerLevel level) {
        this.members.add(member.m_19879_());
        if (member instanceof ChildNpc child && child.mind != null) {
            child.mind.conversation = this;
            ChildBrain.stopMoment(child);
        } else if (member instanceof EtoNpc npc && npc.mind != null) {
            npc.mind.conversation = this;
            HomeChores.stop(npc, npc.mind, level);
            if (npc.mind.gesture.still() || npc.mind.gesture == com.hex.kagune.control.npc.EtoGesture.CLING) {
                EtoBrain.gesture(npc.mind, com.hex.kagune.control.npc.EtoGesture.NONE, level.m_46467_());
            }
        }
    }

    /** True if {@code body} (a child or a companion) is free to start a talk: in none, awake, not fighting, not held by a player's moment. */
    static boolean free(LivingEntity body, ServerLevel level, long now) {
        if (body == null || !body.m_6084_() || body.m_5803_() || in(body) != null) {
            return false;
        }
        if (body instanceof ChildNpc child) {
            ChildBrain.Mind mind = child.mind;
            return mind != null && mind.target == 0 && !ChildBrain.busy(mind, now) && !ChildBrain.carried(child) && child.following() == null
                && ChildTalk.talker(child, level) == null && mind.tagWith == 0 && mind.minding == 0 && (mind.escort == null || now >= mind.escortUntil)
                && mind.hideWith == 0;
        }
        if (body instanceof EtoNpc npc) {
            EtoBrain.Mind mind = npc.mind;
            return mind != null && mind.target == 0 && mind.duty == null && mind.holding == null && mind.kiss == null && mind.toward == null
                && mind.talking == null && mind.momentChild == null && mind.with == 0;
        }
        return false;
    }

    // ---------------------------------------------------------------- each member's brain

    /** The talk {@code body} is in, still going (null: none). */
    static Conversation in(LivingEntity body) {
        Conversation talk = body instanceof ChildNpc child && child.mind != null ? child.mind.conversation
            : body instanceof EtoNpc npc && npc.mind != null ? npc.mind.conversation : null;
        return talk == null || talk.ended != null ? null : talk;
    }

    /** True while {@code body} is in a talk that goes on (ticked first: it may end here, and its mind lets go of it). */
    static boolean going(LivingEntity body, ServerLevel level, long now) {
        Conversation talk = body instanceof ChildNpc child && child.mind != null ? child.mind.conversation
            : body instanceof EtoNpc npc && npc.mind != null ? npc.mind.conversation : null;
        if (talk == null) {
            return false;
        }
        talk.tick(level, now);
        if (talk.ended != null) {
            release(body, talk);
            return false;
        }
        return true;
    }

    /**
     * {@code body}'s part in its talk this tick: walking up to it if it comes, else standing, turned to whoever speaks. True while it does
     * (nothing else of its own meanwhile). A child in its parent's arms is held by them (its carrying does it).
     */
    static boolean hold(LivingEntity body, ServerLevel level, long now) {
        Conversation talk = in(body);
        if (talk == null) {
            return false;
        }
        if (body instanceof ChildNpc child && ChildBrain.carried(child)) {
            return true;
        }
        int id = body.m_19879_();
        LivingEntity anchor = entity(level, talk.anchor);
        if (talk.coming.contains(id) && anchor != null && anchor != body) {
            if (body.m_20280_(anchor) > Talks.GATHER * Talks.GATHER) {
                if (body instanceof Mob mob) {
                    if (now % 10L == Math.floorMod(id, 10)) {
                        mob.m_21573_().m_5624_(anchor, body instanceof EtoNpc ? 0.95 : 1.05);
                    }
                    mob.m_21563_().m_24960_(anchor, 30.0F, 30.0F);
                }
                return true;
            }
            talk.coming.remove(id);
        }
        // Standing, turned (a turn of the shoulders a tick, not a snap) to whoever speaks.
        if (body instanceof Mob mob) {
            mob.m_21573_().m_26573_();
            // Its leash where it stands: nothing pulls it home mid-line (its brain sets its home again once the talk is over).
            mob.m_21446_(new BlockPos((int)Math.floor(body.m_20185_()), (int)Math.floor(body.m_20186_()), (int)Math.floor(body.m_20189_())), 3);
        }
        body.m_20256_(new Vec3(0.0, body.m_20184_().f_82480_, 0.0));
        LivingEntity to = entity(level, talk.faces(id));
        if (to != null) {
            face(body, to);
        }
        return true;
    }

    /** {@code body} turned toward {@code to}: its body by {@link Talks#TURN} a tick, its head straight at them. */
    static void face(LivingEntity body, Entity to) {
        faceTo(body, to.m_20182_());
        if (body instanceof Mob mob) {
            mob.m_21563_().m_24960_(to, 30.0F, 30.0F);
        }
    }

    /** 1.3.58: {@code body} turned toward the point {@code at}: its body by {@link Talks#TURN} a tick, its head further. */
    static void faceTo(LivingEntity body, Vec3 at) {
        Vec3 way = at.m_82546_(body.m_20182_());
        if (way.f_82479_ * way.f_82479_ + way.f_82481_ * way.f_82481_ < 1.0E-4) {
            return;
        }
        float yaw = (float)Math.toDegrees(Math.atan2(-way.f_82479_, way.f_82481_));
        float turned = Talks.turn(body.f_20883_, yaw, Talks.TURN);
        body.m_146922_(turned);
        body.f_20883_ = turned;
        body.f_20885_ = Talks.turn(turned, yaw, 50.0F);
    }

    /** {@code body}'s talk over now, for {@code why} (the rest of it dropped; a fight, a hurt, a player: nothing more said). */
    static void end(LivingEntity body, Talks.End why, ServerLevel level, long now) {
        Conversation talk = in(body);
        if (talk != null) {
            talk.finish(why, level, null);
        }
        release(body, talk);
    }

    private static void release(LivingEntity body, Conversation talk) {
        if (body instanceof ChildNpc child && child.mind != null && (talk == null || child.mind.conversation == talk)) {
            child.mind.conversation = null;
            child.mind.lastTalk = talk;
        } else if (body instanceof EtoNpc npc && npc.mind != null && (talk == null || npc.mind.conversation == talk)) {
            npc.mind.conversation = null;
        }
    }

    // ---------------------------------------------------------------- the talk itself

    /** Who {@code self} faces now. */
    int faces(int self) {
        Line next = this.lines.peek();
        int other = 0;
        for (int member : this.members) {
            if (member != self) {
                other = member;
                break;
            }
        }
        return Talks.faces(self, this.speaker, this.previous, next == null ? 0 : next.speaker(), this.anchor, other);
    }

    /** Once a tick (by whichever member ticks first): its members checked, its next line said when it is due and its speaker is near. */
    void tick(ServerLevel level, long now) {
        if (this.ended != null || this.tickedAt == now) {
            return;
        }
        this.tickedAt = now;
        LivingEntity anchor = entity(level, this.anchor);
        for (int id : new ArrayList<>(this.members)) {
            LivingEntity member = entity(level, id);
            if (member == null || !member.m_6084_() || member.m_9236_() != level) {
                this.finish(Talks.End.GONE, level, null);
                return;
            }
            Talks.End why = this.trouble(member, level);
            if (why != null) {
                this.finish(why, level, null);
                return;
            }
            if (anchor != null && member != anchor && !this.coming.contains(id) && member.m_20280_(anchor) > Talks.LEAVES * Talks.LEAVES) {
                this.finish(Talks.End.LEFT, level, member);
                return;
            }
        }
        if (this.lines.isEmpty()) {
            if (now >= this.nextAt) {
                this.finish(Talks.End.DONE, level, null);
            }
            return;
        }
        if (now < this.nextAt) {
            return;
        }
        Line line = this.lines.peek();
        LivingEntity says = entity(level, line.speaker());
        if (says == null) {
            this.finish(Talks.End.GONE, level, null);
            return;
        }
        double apart = this.apart(says, level);
        if (apart > Talks.SPEAKS) {
            if (this.waiting == Long.MIN_VALUE) {
                this.waiting = now;
            }
            if (this.speaker == 0 && now - this.since > Talks.GATHERS) {
                this.finish(Talks.End.LEFT, level, null); // they never came together: nothing was said, nothing to say about it
                return;
            }
            if (now - this.waiting < Talks.WAITS) {
                return;
            }
            if (apart > Talks.LEAVES) {
                this.drop(line.speaker(), says); // never came over: its lines dropped, it is out of it, the rest go on
                this.waiting = Long.MIN_VALUE;
                return;
            }
            // Waited long enough: it says it from where it is.
        }
        this.waiting = Long.MIN_VALUE;
        this.lines.poll();
        if (this.speaker != 0 && this.speaker != line.speaker()) {
            this.previous = this.speaker;
        }
        this.speaker = line.speaker();
        int pause = Parenting.pause(line.text());
        Line next = this.lines.peek();
        say(level, says, line.text(), next == null ? 0 : Speech.turn(line.speaker(), next.speaker(), pause));
        act(says, line.cue(), line.gesture(), now);
        this.nextAt = now + pause;
    }

    /** How far {@code says} is from the others (from the anchor; the anchor, from the nearest of them). */
    private double apart(LivingEntity says, ServerLevel level) {
        if (says.m_19879_() != this.anchor) {
            LivingEntity anchor = entity(level, this.anchor);
            return anchor == null ? Double.MAX_VALUE : Math.sqrt(says.m_20280_(anchor));
        }
        double best = Double.MAX_VALUE;
        for (int id : this.members) {
            LivingEntity member = id == this.anchor ? null : entity(level, id);
            if (member != null) {
                best = Math.min(best, Math.sqrt(says.m_20280_(member)));
            }
        }
        return best;
    }

    /** {@code who} out of it: its lines dropped, no longer a member (it goes back to what it was doing). */
    private void drop(int who, LivingEntity body) {
        this.lines.removeIf(line -> line.speaker() == who);
        this.members.remove(Integer.valueOf(who));
        this.coming.remove(who);
        release(body, this);
    }

    /** What stops it for {@code member} (null: nothing): hurt, a fight, asleep, a player wanting it. */
    private Talks.End trouble(LivingEntity member, ServerLevel level) {
        if (member.m_5803_()) {
            return Talks.End.ASLEEP;
        }
        if (member instanceof ChildNpc child && child.mind != null) {
            ChildBrain.Mind mind = child.mind;
            if (mind.hurtAt >= this.since) {
                return Talks.End.HURT;
            }
            if (mind.target != 0) {
                return Talks.End.FIGHT;
            }
            if (ChildTalk.talker(child, level) != null || child.following() != null) {
                return Talks.End.INTERRUPTED;
            }
            LivingEntity carrier = mind.carrier == null ? null : ChildBrain.carrierOf(level, mind.carrier);
            if (carrier != null && !this.members.contains(carrier.m_19879_())) {
                return Talks.End.INTERRUPTED; // picked up by someone not in it
            }
        } else if (member instanceof EtoNpc npc && npc.mind != null) {
            EtoBrain.Mind mind = npc.mind;
            if (mind.hurtAt >= this.since) {
                return Talks.End.HURT;
            }
            if (mind.target != 0) {
                return Talks.End.FIGHT;
            }
            if (mind.talking != null || mind.holding != null || mind.kiss != null || mind.toward != null) {
                return Talks.End.INTERRUPTED;
            }
        }
        return null;
    }

    /** It ends: the rest dropped; walked off on, whoever stays may say so. Its members let go of it as they next tick. */
    private void finish(Talks.End why, ServerLevel level, LivingEntity left) {
        if (this.ended != null) {
            return;
        }
        this.ended = why;
        this.lines.clear();
        if (Talks.remarked(why) && left != null && this.speaker != 0) {
            this.cutShort(level, left);
        }
    }

    /** One still here (a companion first) says something about {@code left} walking off. */
    private void cutShort(ServerLevel level, LivingEntity left) {
        LivingEntity by = null;
        for (int id : this.members) {
            LivingEntity member = entity(level, id);
            if (member != null && member != left && member.m_6084_() && !member.m_5803_() && (by == null || member instanceof EtoNpc)) {
                by = member;
            }
        }
        if (by == null) {
            return;
        }
        String name = name(level, left);
        int seed = by.m_217043_().m_188503_(1 << 20);
        String line;
        ChildCue cue = ChildCue.NONE;
        EtoGesture gesture = EtoGesture.NONE;
        if (by instanceof EtoNpc npc) {
            line = Talks.cutShort(npc.persona().key(), ChildStage.CHILD, name, null, seed);
            gesture = com.hex.kagune.control.family.TalkCues.parent(line, com.hex.kagune.control.family.TalkCues.Scene.CUT, npc.persona().key(), 0, seed);
        } else if (by instanceof ChildNpc child && Children.get(level, child.childId()) != null) {
            Children.Child known = Children.get(level, child.childId());
            ChildStage stage = known.stage(level.m_46467_());
            boolean motherNpc = known.mother != null && known.mother.startsWith("npc:");
            boolean fatherNpc = known.father != null && known.father.startsWith("npc:");
            ChildTemper temper = ChildTemper.of(known.id, known.mother, known.father);
            line = Talks.cutShort(temper.word(), stage, name, ChildLines.call(stage, motherNpc || !fatherNpc), seed);
            cue = com.hex.kagune.control.family.TalkCues.child(line, com.hex.kagune.control.family.TalkCues.Scene.CUT, temper, false, 0, seed);
        } else {
            line = null;
        }
        if (line != null) {
            say(level, by, line, 0);
            act(by, cue, gesture, level.m_46467_());
        }
    }

    /** 1.3.57: {@code body} does what goes with its line: a child its cue (sent with it), a companion her gesture. */
    static void act(LivingEntity body, ChildCue cue, EtoGesture gesture, long now) {
        if (body instanceof ChildNpc child && child.mind != null && cue != null && cue != ChildCue.NONE) {
            child.mind.cue = cue;
            child.mind.cueAt = now;
            child.mind.dirty = true;
        } else if (body instanceof EtoNpc npc && npc.mind != null && gesture != null && gesture != EtoGesture.NONE) {
            EtoBrain.gesture(npc.mind, gesture, now);
        }
    }

    private static String name(ServerLevel level, LivingEntity body) {
        if (body instanceof ChildNpc child && Children.get(level, child.childId()) != null) {
            return Children.get(level, child.childId()).name;
        }
        return body instanceof EtoNpc npc ? npc.displayName() : "";
    }

    /** {@code text} in {@code speaker}'s bubble for {@code ticks} (0: as long as it takes to read), to every player near who sees it. */
    static void say(ServerLevel level, LivingEntity speaker, String text, int ticks) {
        for (ServerPlayer player : level.m_6907_()) {
            if (SceneServer.sees(speaker, player, 24.0)) {
                ControlNetwork.sendTo(player, new S2CNpcSay(speaker.m_19879_(), text, ticks));
            }
        }
    }

    private static LivingEntity entity(ServerLevel level, int id) {
        return id != 0 && level.m_6815_(id) instanceof LivingEntity living ? living : null;
    }

    /** Its members (entity ids), for a check that one of them is someone. */
    boolean has(Entity body) {
        return body != null && this.members.contains(body.m_19879_());
    }

    /** True once it is over, however it ended. */
    boolean over() {
        return this.ended != null;
    }
}
