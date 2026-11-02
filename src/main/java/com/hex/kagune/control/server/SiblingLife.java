package com.hex.kagune.control.server;

import com.hex.kagune.control.family.ChildLines;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.family.ChildTemper;
import com.hex.kagune.control.family.Parenting;
import com.hex.kagune.control.family.Siblings;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.EtoNpc;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.3.51: brothers and sisters in the world ({@link Siblings}' rules and words). A child with a brother or sister near it, both free, now
 * and then has a moment with it: they stop and talk (back and forth in their bubbles: chatter, teasing, bragging, comfort for one that is
 * hurt, a scheme against a parent, a squabble a companion parent near breaks up), or play tag (the one who is it chases, tags, and is
 * chased), or an older one goes after a toddler that wandered off and walks it home. In a fight they cover each other (a monster after a
 * sibling is a threat too: {@code ChildBrain.threat}), and a sibling's kagune coming out gets a word.
 */
final class SiblingLife {
    private SiblingLife() {
    }

    /** True if {@code a} and {@code b} are brothers or sisters (a parent in common). */
    static boolean siblings(Children.Child a, Children.Child b) {
        if (a == null || b == null || a.id.equals(b.id)) {
            return false;
        }
        return a.mother != null && !a.mother.isEmpty() && (a.mother.equals(b.mother) || a.mother.equals(b.father))
            || a.father != null && !a.father.isEmpty() && (a.father.equals(b.father) || a.father.equals(b.mother));
    }

    /**
     * Its moment with a sibling this tick, if it is in one that moves it (tag, going after a toddler): true while it does. Else, now and
     * then, a new moment begun with a sibling near (a talk, which stands them both still; tag; minding a toddler).
     */
    static boolean tick(ChildNpc npc, ChildBrain.Mind mind, Children.Child child, ServerLevel level, long now) {
        if (mind.hideWith != 0) {
            if (hide(npc, mind, child, level, now)) {
                return true; // 1.3.58: playing hide and seek
            }
        }
        if (mind.tagWith != 0) {
            if (tag(npc, mind, child, level, now)) {
                return true;
            }
        }
        if (mind.minding != 0) {
            if (minding(npc, mind, child, level, now)) {
                return true;
            }
        }
        if (now % 40L == Math.floorMod(npc.m_19879_(), 40) && now - mind.siblingAt >= Siblings.EVERY && mind.talk.isEmpty()) {
            begin(npc, mind, child, level, now);
        }
        return false;
    }

    /**
     * A moment begun with a sibling near, both free (by day; at night only a whispered chat). 1.3.53: a talk is a {@link Conversation}: the
     * one who starts it walks up to the other first, both stay for it; a squabble's parent comes over to break it up.
     */
    private static void begin(ChildNpc npc, ChildBrain.Mind mind, Children.Child child, ServerLevel level, long now) {
        ChildStage stage = child.stage(now);
        if (stage == ChildStage.BABY || !Conversation.free(npc, level, now)) {
            return;
        }
        for (ChildNpc other : level.m_6443_(ChildNpc.class, npc.m_20191_().m_82400_(Siblings.NEAR), body -> body != npc && body.m_6084_())) {
            Children.Child known = Children.get(level, other.childId());
            ChildBrain.Mind them = other.mind;
            if (them == null || known == null || !siblings(child, known) || !Conversation.free(other, level, now) || !them.talk.isEmpty()
                || now - them.siblingAt < Siblings.EVERY / 2 || !npc.m_142582_(other)) {
                continue;
            }
            ChildStage theirs = known.stage(now);
            boolean hurt = other.m_21223_() < other.m_21233_() * Siblings.HURT;
            Households.Home house = Households.forChild(level, known);
            double beyond = house == null ? -1.0 : Math.sqrt(other.m_20238_(new Vec3(house.pos().m_123341_() + 0.5, house.pos().m_123342_(),
                house.pos().m_123343_() + 0.5))) - house.radius();
            boolean strayed = Parenting.strayed(theirs, beyond, HomeLife.night(level.m_46468_(), true), other.following() != null, false) != null;
            Siblings.Kind kind = Siblings.pick(stage, temper(child), theirs, temper(known), hurt, strayed, npc.m_217043_().m_188500_());
            if (kind == null) {
                continue;
            }
            boolean night = HomeLife.night(level.m_46468_(), true);
            if (night && kind != Siblings.Kind.CHAT && kind != Siblings.Kind.COMFORT && kind != Siblings.Kind.MIND) {
                continue;
            }
            if (kind == Siblings.Kind.MIND && parentGoing(level, other) != null) {
                continue; // 1.3.55: a parent is already going after it: one is enough
            }
            if (kind == Siblings.Kind.TAG && !(inYard(npc, child, level) && inYard(other, known, level))) {
                kind = Siblings.Kind.CHAT; // 1.3.54: tag only at home, both in its yard (they play there, never off beyond it)
            }
            mind.siblingAt = now;
            them.siblingAt = now;
            List<Siblings.Line> talk = Siblings.talk(kind, temper(child), stage, child.name, call(child, stage), girl(child), temper(known), theirs,
                known.name, call(known, theirs), girl(known), npc.m_217043_().m_188503_(1 << 20));
            switch (kind) {
                case TAG -> {
                    if (npc.m_217043_().m_188500_() < com.hex.kagune.control.family.HideAndSeek.INSTEAD_OF_TAG) {
                        startHide(npc, other, child, known, level, now); // 1.3.58: hide and seek instead
                        return;
                    }
                    // It tags the other: the other is it, and chases.
                    mind.tagWith = other.m_19879_();
                    mind.chasing = false;
                    mind.tagUntil = now + Siblings.TAG_TICKS;
                    them.tagWith = npc.m_19879_();
                    them.chasing = true;
                    them.tagUntil = now + Siblings.TAG_TICKS;
                    them.taggedAt = now;
                    queue(npc, other, null, talk, now);
                }
                case MIND -> {
                    mind.minding = other.m_19879_();
                    mind.mindingUntil = now + 1200L;
                    mind.mindingReached = false;
                }
                default -> {
                    // It walks up to the other (who waits, turned to it), and they talk; both stay till it's said.
                    Conversation together = Conversation.start(level, now, "sibling " + kind.name().toLowerCase(java.util.Locale.ROOT), other,
                        List.of(npc, other), List.of(npc), lines(npc, other, null, talk, scene(kind), temper(child), temper(known)));
                    if (together != null && kind == Siblings.Kind.SQUABBLE) {
                        breakUp(together, npc, other, child, known, level, now);
                    }
                }
            }
            return;
        }
    }

    /**
     * A squabble's end: a companion parent of theirs near, who sees it and is free, hears it start, puts down what it was doing and comes
     * over; once the squabble is said (and it is there), it breaks it up, and each says sorry.
     */
    private static void breakUp(Conversation squabble, ChildNpc npc, ChildNpc other, Children.Child child, Children.Child known, ServerLevel level,
                                long now) {
        EtoNpc parent = null;
        for (EtoNpc body : level.m_6443_(EtoNpc.class, npc.m_20191_().m_82400_(16.0), b -> b.m_6084_() && b.mind != null)) {
            String key = Pregnancies.npc(body.persona().key());
            if (child.parent(key) && known.parent(key) && body.m_142582_(npc) && Conversation.free(body, level, now)) {
                parent = body;
                break;
            }
        }
        if (parent == null) {
            return;
        }
        List<Siblings.Line> talk = Siblings.breakUp(parent.persona().key(), temper(child), child.name, temper(known), known.name,
            parent.m_217043_().m_188503_(1 << 20));
        squabble.join(level, parent, true, lines(npc, other, parent, talk, com.hex.kagune.control.family.TalkCues.Scene.MAKE_UP, temper(child), temper(known)));
    }

    /** Tag: the one who is it runs after the other and tags it (then it is the other's turn); the other runs off. Over after a while. */
    private static boolean tag(ChildNpc npc, ChildBrain.Mind mind, Children.Child child, ServerLevel level, long now) {
        Entity found = level.m_6815_(mind.tagWith);
        if (!(found instanceof ChildNpc other) || !other.m_6084_() || other.mind == null || other.mind.tagWith != npc.m_19879_() || now >= mind.tagUntil
            || other.m_20280_(npc) > 24.0 * 24.0) {
            mind.tagWith = 0;
            return false;
        }
        if (mind.chasing) {
            if (now % 5L == 0L) {
                npc.m_21573_().m_5624_(other, 1.3);
            }
            if (npc.m_20280_(other) <= Siblings.TAG_REACH * Siblings.TAG_REACH && now - mind.taggedAt > Siblings.NO_TAG_BACKS) {
                mind.chasing = false;
                other.mind.chasing = true;
                other.mind.taggedAt = now;
                npc.m_21011_(net.minecraft.world.InteractionHand.MAIN_HAND, true);
                Siblings.Line line = Siblings.tagged(temper(child), child.stage(now), other.mind == null ? "" : name(level, other),
                    npc.m_217043_().m_188503_(1 << 20));
                ParentDuty.Said said = new ParentDuty.Said(npc.m_19879_(), line.text(), now);
                mind.talk.add(said);
            }
        } else if (now % 10L == 0L) {
            // Away from the one who is it, a few blocks, a little to one side; 1.3.54: never off its home's yard (Siblings.runTo).
            Households.Home house = Households.forChild(level, child);
            if (house == null) {
                mind.tagWith = 0;
                return false;
            }
            double turn = (npc.m_217043_().m_188500_() - 0.5) * 1.4;
            double[] to = Siblings.runTo(npc.m_20185_(), npc.m_20189_(), other.m_20185_(), other.m_20189_(), turn, house.pos().m_123341_() + 0.5,
                house.pos().m_123343_() + 0.5, house.radius());
            npc.m_21573_().m_26519_(to[0], npc.m_20186_(), to[1], 1.25);
        }
        return true;
    }

    // ---------------------------------------------------------------- 1.3.58: hide and seek

    /** Hide and seek begun: {@code seeker} (who proposed it) counts, {@code hider} hides; a word each first. */
    private static void startHide(ChildNpc seeker, ChildNpc hider, Children.Child child, Children.Child known, ServerLevel level, long now) {
        ChildStage stage = child.stage(now);
        ChildStage theirs = known.stage(now);
        int seed = seeker.m_217043_().m_188503_(1 << 20);
        String propose = com.hex.kagune.control.family.HideAndSeek.line(com.hex.kagune.control.family.HideAndSeek.Part.PROPOSE, temper(child), stage, seed);
        String agree = com.hex.kagune.control.family.HideAndSeek.line(com.hex.kagune.control.family.HideAndSeek.Part.AGREE, temper(known), theirs, seed + 7);
        long at = Math.max(now, seeker.mind.talkFree);
        seeker.mind.talk.add(new ParentDuty.Said(seeker.m_19879_(), propose, at, com.hex.kagune.control.family.ChildCue.CHEER));
        long next = at + Parenting.pause(propose);
        seeker.mind.talk.add(new ParentDuty.Said(hider.m_19879_(), agree, next, com.hex.kagune.control.family.TalkCues.said(agree)));
        seeker.mind.talkFree = next + Parenting.pause(agree);
        for (ChildNpc body : new ChildNpc[] {seeker, hider}) {
            ChildBrain.Mind m = body.mind;
            m.hideWith = (body == seeker ? hider : seeker).m_19879_();
            m.seeker = body == seeker;
            m.hidePart = 0;
            m.hideAt = next; // the count begins as the other runs off
            m.hideBegan = now;
            m.hideFound = false;
            m.hideSpot = null;
            m.hidden = false;
            m.seekTo = null;
            m.hintAt = Long.MIN_VALUE / 2;
            m.seekSaidAt = Long.MIN_VALUE / 2;
        }
    }

    /** Its part in a game of hide and seek this tick: true while it plays. */
    private static boolean hide(ChildNpc npc, ChildBrain.Mind mind, Children.Child child, ServerLevel level, long now) {
        Entity found = level.m_6815_(mind.hideWith);
        Households.Home house = Households.forChild(level, child);
        if (!(found instanceof ChildNpc other) || !other.m_6084_() || other.mind == null || other.mind.hideWith != npc.m_19879_() || house == null
            || now - mind.hideBegan > com.hex.kagune.control.family.HideAndSeek.LONGEST || ChildBrain.carried(npc) || ChildBrain.carried(other)
            || other.m_20280_(npc) > 32.0 * 32.0 || other.mind.target != 0 || ChildBrain.busy(other.mind, now) || Conversation.in(other) != null
            || ChildTalk.talker(other, level) != null || other.m_5803_() || other.following() != null || npc.following() != null) {
            stopHide(npc);
            return false;
        }
        return mind.seeker ? seek(npc, mind, child, other, house, level, now) : hideAway(npc, mind, child, other, house, level, now);
    }

    /** The seeker: counts (eyes covered, turned away), looks about the yard (to a giggle it heard), finds it or gives up. */
    private static boolean seek(ChildNpc npc, ChildBrain.Mind mind, Children.Child child, ChildNpc hider, Households.Home house, ServerLevel level,
                                long now) {
        ChildStage stage = child.stage(now);
        long t = now - mind.hideAt;
        int seed = npc.m_217043_().m_188503_(1 << 20);
        switch (mind.hidePart) {
            case 0 -> {
                still(npc);
                if (t < 0L) {
                    Conversation.face(npc, hider); // still saying who counts
                    return true;
                }
                if (mind.seekSaidAt < mind.hideAt) {
                    mind.seekSaidAt = now;
                    mind.talk.add(new ParentDuty.Said(npc.m_19879_(), com.hex.kagune.control.family.HideAndSeek.line(
                        com.hex.kagune.control.family.HideAndSeek.Part.COUNT, temper(child), stage, seed), now, com.hex.kagune.control.family.ChildCue.COUNT));
                }
                // Turned away from where the other runs, eyes covered.
                Conversation.faceTo(npc, npc.m_20182_().m_82549_(npc.m_20182_().m_82546_(hider.m_20182_())));
                if (t >= com.hex.kagune.control.family.HideAndSeek.COUNT) {
                    mind.talk.add(new ParentDuty.Said(npc.m_19879_(), com.hex.kagune.control.family.HideAndSeek.line(
                        com.hex.kagune.control.family.HideAndSeek.Part.READY, temper(child), stage, seed), now, com.hex.kagune.control.family.ChildCue.CHEER));
                    mind.hidePart = 1;
                    mind.hideAt = now;
                    mind.seekSaidAt = now;
                }
                return true;
            }
            case 1 -> {
                Children.Child known = Children.get(level, hider.childId());
                ChildStage theirs = known == null ? ChildStage.CHILD : known.stage(now);
                ChildTemper theirTemper = known == null ? null : temper(known);
                if (npc.m_20280_(hider) <= com.hex.kagune.control.family.HideAndSeek.FINDS * com.hex.kagune.control.family.HideAndSeek.FINDS
                    && npc.m_142582_(hider) && t > 20L) {
                    // Found it.
                    String got = com.hex.kagune.control.family.HideAndSeek.line(com.hex.kagune.control.family.HideAndSeek.Part.FOUND, temper(child), stage, seed);
                    String caught = com.hex.kagune.control.family.HideAndSeek.line(com.hex.kagune.control.family.HideAndSeek.Part.CAUGHT, theirTemper, theirs,
                        seed + 3);
                    mind.talk.add(new ParentDuty.Said(npc.m_19879_(), got, now, com.hex.kagune.control.family.ChildCue.POINT));
                    mind.talk.add(new ParentDuty.Said(hider.m_19879_(), caught, now + Parenting.pause(got), caughtCue(caught, theirTemper)));
                    mind.talkFree = now + Parenting.pause(got) + Parenting.pause(caught);
                    over(npc, hider, true, now);
                    return true;
                }
                if (t >= com.hex.kagune.control.family.HideAndSeek.SEEKS) {
                    // Gives up: the other pops out, the winner.
                    String up = com.hex.kagune.control.family.HideAndSeek.line(com.hex.kagune.control.family.HideAndSeek.Part.GIVE_UP, temper(child), stage, seed);
                    String win = com.hex.kagune.control.family.HideAndSeek.line(com.hex.kagune.control.family.HideAndSeek.Part.WIN, theirTemper, theirs, seed + 5);
                    mind.talk.add(new ParentDuty.Said(npc.m_19879_(), up, now, com.hex.kagune.control.family.ChildCue.SHRUG));
                    mind.talk.add(new ParentDuty.Said(hider.m_19879_(), win, now + Parenting.pause(up), com.hex.kagune.control.family.ChildCue.CHEER));
                    mind.talkFree = now + Parenting.pause(up) + Parenting.pause(win);
                    over(npc, hider, false, now);
                    return true;
                }
                // Looking about: somewhere new in the yard now and then, or to where it heard a giggle.
                boolean heard = com.hex.kagune.control.family.HideAndSeek.heard(now - hider.mind.hintAt);
                if (mind.seekTo == null || now - mind.seekToAt > 70L || npc.m_20238_(mind.seekTo) < 1.6 * 1.6 || heard && now - mind.seekToAt > 20L
                    && hider.mind.hintAt > mind.seekToAt) {
                    Vec3 to;
                    if (heard) {
                        double a = npc.m_217043_().m_188500_() * Math.PI * 2.0;
                        to = hider.m_20182_().m_82520_(Math.cos(a) * 1.5, 0.0, Math.sin(a) * 1.5);
                    } else {
                        double[] xz = com.hex.kagune.control.family.HideAndSeek.spot(npc.m_217043_().m_188503_(com.hex.kagune.control.family.HideAndSeek.SPOTS),
                            house.pos().m_123341_() + 0.5, house.pos().m_123343_() + 0.5, house.radius(), seed);
                        to = new Vec3(xz[0], npc.m_20186_(), xz[1]);
                    }
                    mind.seekTo = to;
                    mind.seekToAt = now;
                    npc.m_21573_().m_26519_(to.f_82479_, to.f_82480_, to.f_82481_, heard ? 1.1 : 0.85);
                }
                if (now - mind.seekSaidAt > 220L && npc.m_217043_().m_188500_() < 0.02 && mind.talk.isEmpty()) {
                    mind.seekSaidAt = now;
                    mind.talk.add(new ParentDuty.Said(npc.m_19879_(), com.hex.kagune.control.family.HideAndSeek.line(
                        com.hex.kagune.control.family.HideAndSeek.Part.SEEKING, temper(child), stage, seed), now, com.hex.kagune.control.family.ChildCue.THINK));
                }
                return true;
            }
            default -> {
                still(npc);
                Conversation.face(npc, hider);
                if (t >= com.hex.kagune.control.family.HideAndSeek.AFTER) {
                    stopHide(npc);
                }
                return true;
            }
        }
    }

    /** The hider: to the best spot it can find in the yard out of the seeker's sight, crouched there, giggling now and then; out once it's over. */
    private static boolean hideAway(ChildNpc npc, ChildBrain.Mind mind, Children.Child child, ChildNpc seeker, Households.Home house, ServerLevel level,
                                    long now) {
        ChildBrain.Mind them = seeker.mind;
        if (them.hidePart == 2) {
            // Over: found, it stands up where it is; not found, it comes out to the one who gave up.
            if (!them.hideFound && npc.m_20280_(seeker) > 2.2 * 2.2) {
                if (now % 10L == 0L) {
                    npc.m_21573_().m_5624_(seeker, 1.15);
                }
            } else {
                still(npc);
                Conversation.face(npc, seeker);
            }
            return true;
        }
        if (them.hidePart == 0 && now < them.hideAt) {
            still(npc);
            Conversation.face(npc, seeker); // still saying it'll hide
            return true;
        }
        if (mind.hideSpot == null) {
            mind.hideSpot = spot(npc, seeker, house, level);
            mind.hideSetOff = now;
        }
        if (!mind.hidden) {
            if (npc.m_20238_(mind.hideSpot) > 0.8 * 0.8 && now - mind.hideSetOff < com.hex.kagune.control.family.HideAndSeek.REACHES) {
                if (now % 10L == Math.floorMod(npc.m_19879_(), 10)) {
                    npc.m_21573_().m_26519_(mind.hideSpot.f_82479_, mind.hideSpot.f_82480_, mind.hideSpot.f_82481_, 1.25);
                }
                return true;
            }
            // There (or as near as it got): crouched down small.
            mind.hidden = true;
            mind.cue = com.hex.kagune.control.family.ChildCue.HIDE;
            mind.cueAt = now;
            mind.dirty = true;
        }
        still(npc);
        Conversation.faceTo(npc, seeker.m_20182_()); // peeking out at it
        // Now and then, while it is looked for, a giggle it can't help (a hint the seeker may hear).
        if (them.hidePart == 1 && now - mind.hintAt >= com.hex.kagune.control.family.HideAndSeek.HINT_EVERY && now % 20L == Math.floorMod(npc.m_19879_(), 20)
            && npc.m_217043_().m_188500_() < 0.15) {
            mind.hintAt = now;
            Children.Child known = Children.get(level, npc.childId());
            mind.talk.add(new ParentDuty.Said(npc.m_19879_(), com.hex.kagune.control.family.HideAndSeek.line(com.hex.kagune.control.family.HideAndSeek.Part.HINT,
                known == null ? null : temper(known), child.stage(now), npc.m_217043_().m_188503_(1 << 20)), now));
        }
        return true;
    }

    /**
     * Where to hide: of {@link com.hex.kagune.control.family.HideAndSeek#SPOTS} spots round the yard with ground to stand on, one the seeker
     * can't see from where it counts (a block between its eyes and the spot), the farther the better; where it is if there is none.
     */
    private static Vec3 spot(ChildNpc npc, ChildNpc seeker, Households.Home house, ServerLevel level) {
        int n = com.hex.kagune.control.family.HideAndSeek.SPOTS;
        Vec3[] at = new Vec3[n];
        double[] distance = new double[n];
        boolean[] hidden = new boolean[n];
        int seed = npc.m_217043_().m_188503_(360);
        Vec3 eye = seeker.m_146892_();
        for (int i = 0; i < n; i++) {
            double[] xz = com.hex.kagune.control.family.HideAndSeek.spot(i, house.pos().m_123341_() + 0.5, house.pos().m_123343_() + 0.5, house.radius(), seed);
            Double y = ground(level, xz[0], xz[1], house.pos().m_123342_());
            if (y == null) {
                distance[i] = Double.NaN;
                continue;
            }
            at[i] = new Vec3(xz[0], y, xz[1]);
            distance[i] = Math.sqrt(seeker.m_20238_(at[i]));
            Vec3 head = at[i].m_82520_(0.0, Math.max(0.5, npc.m_20206_() * 0.6), 0.0);
            hidden[i] = level.m_45547_(new net.minecraft.world.level.ClipContext(eye, head, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, seeker)).m_6662_() == net.minecraft.world.phys.HitResult.Type.BLOCK;
        }
        int best = com.hex.kagune.control.family.HideAndSeek.bestSpot(distance, hidden);
        return best < 0 ? npc.m_20182_() : at[best];
    }

    /** The height of ground to stand on at ({@code x}, {@code z}) near {@code near} (a solid block under two clear ones), or null. */
    private static Double ground(ServerLevel level, double x, double z, int near) {
        for (int y = near + 2; y >= near - 3; y--) {
            net.minecraft.core.BlockPos feet = net.minecraft.core.BlockPos.m_274561_(x, y, z);
            net.minecraft.core.BlockPos below = net.minecraft.core.BlockPos.m_274561_(x, y - 1, z);
            net.minecraft.core.BlockPos above = net.minecraft.core.BlockPos.m_274561_(x, y + 1, z);
            if (!level.m_8055_(below).m_60812_(level, below).m_83281_() && level.m_8055_(feet).m_60812_(level, feet).m_83281_()
                && level.m_8055_(above).m_60812_(level, above).m_83281_()) {
                return (double)y;
            }
        }
        return null;
    }

    /** The game over: found ({@code found}) or given up; they stand a moment, then go back to what they were doing. */
    private static void over(ChildNpc seeker, ChildNpc hider, boolean found, long now) {
        seeker.mind.hidePart = 2;
        seeker.mind.hideAt = now;
        seeker.mind.hideFound = found;
        hider.mind.hidePart = 2;
        hider.mind.hideFound = found;
        seeker.m_21573_().m_26573_();
    }

    /** Hide and seek over for {@code npc} and whoever it played with (its crouch and its covered eyes let go). */
    static void stopHide(ChildNpc npc) {
        ChildBrain.Mind mind = npc.mind;
        if (mind == null || mind.hideWith == 0) {
            return;
        }
        Entity other = npc.m_9236_().m_6815_(mind.hideWith);
        for (ChildNpc body : other instanceof ChildNpc them && them.mind != null && them.mind.hideWith == npc.m_19879_() ? new ChildNpc[] {npc, them}
            : new ChildNpc[] {npc}) {
            ChildBrain.Mind m = body.mind;
            m.hideWith = 0;
            m.hideSpot = null;
            m.hidden = false;
            m.seekTo = null;
            m.hidePart = 0;
            if (m.cue == com.hex.kagune.control.family.ChildCue.HIDE || m.cue == com.hex.kagune.control.family.ChildCue.COUNT) {
                m.cue = com.hex.kagune.control.family.ChildCue.NONE;
                m.dirty = true;
            }
        }
    }

    /** What a found hider does as it says its line: what the line says, else by its temper. */
    private static com.hex.kagune.control.family.ChildCue caughtCue(String line, ChildTemper temper) {
        com.hex.kagune.control.family.ChildCue own = com.hex.kagune.control.family.TalkCues.said(line);
        if (own != com.hex.kagune.control.family.ChildCue.NONE) {
            return own;
        }
        return temper == ChildTemper.BOLD ? com.hex.kagune.control.family.ChildCue.STAMP : temper == ChildTemper.SHY
            ? com.hex.kagune.control.family.ChildCue.SULK : com.hex.kagune.control.family.ChildCue.GIGGLE;
    }

    /** Standing where it is (no walking, no sliding). */
    private static void still(ChildNpc npc) {
        npc.m_21573_().m_26573_();
        npc.m_20256_(new Vec3(0.0, npc.m_20184_().f_82480_, 0.0));
    }

    /** 1.3.54: true if {@code body} (the child {@code known}) is in its home's yard (where tag may be played: {@link Siblings#yard}). */
    private static boolean inYard(ChildNpc body, Children.Child known, ServerLevel level) {
        Households.Home house = Households.forChild(level, known);
        if (house == null) {
            return false;
        }
        double dx = body.m_20185_() - (house.pos().m_123341_() + 0.5);
        double dz = body.m_20189_() - (house.pos().m_123343_() + 0.5);
        return Math.sqrt(dx * dx + dz * dz) <= Siblings.yard(house.radius());
    }

    /** An older one after a toddler that wandered: to it; reached, a word each, and home together (the toddler at its side). */
    private static boolean minding(ChildNpc npc, ChildBrain.Mind mind, Children.Child child, ServerLevel level, long now) {
        Entity found = level.m_6815_(mind.minding);
        Households.Home house = Households.forChild(level, child);
        if (!(found instanceof ChildNpc little) || !little.m_6084_() || little.mind == null || now >= mind.mindingUntil || house == null
            || ChildBrain.carried(little)) {
            mind.minding = 0;
            return false;
        }
        // 1.3.55: a parent already on its way to the little one: one is enough. It stands down, and says so.
        EtoNpc going = parentGoing(level, little);
        if (going != null) {
            mind.minding = 0;
            mind.mindingTalk = null;
            if (npc.m_20148_().equals(little.mind.escort)) {
                little.mind.escort = null;
            }
            Children.Child known = Children.get(level, little.childId());
            if (Conversation.in(npc) == null && mind.talk.isEmpty()) {
                ChildStage stage = child.stage(now);
                mind.talk.add(new ParentDuty.Said(npc.m_19879_(), Siblings.standDown(temper(child), stage, known == null ? "" : known.name,
                    ChildLines.call(stage, going.persona().female()), npc.m_217043_().m_188503_(1 << 20)), now));
            }
            npc.m_21573_().m_26573_();
            return false;
        }
        if (!mind.mindingReached) {
            if (npc.m_20280_(little) > 2.2 * 2.2) {
                if (now % 10L == 0L) {
                    npc.m_21573_().m_5624_(little, 1.2);
                }
                return true;
            }
            mind.mindingReached = true;
            Children.Child known = Children.get(level, little.childId());
            if (known != null && Conversation.in(little) == null && Conversation.in(npc) == null) {
                // 1.3.53: a word each, both standing for it (Conversation); then home together.
                ChildStage stage = child.stage(now);
                ChildStage theirs = known.stage(now);
                mind.mindingTalk = Conversation.start(level, now, "sibling mind", little, List.of(npc, little), List.of(), lines(npc, little, null,
                    Siblings.talk(Siblings.Kind.MIND, temper(child), stage, child.name, call(child, stage), girl(child), temper(known), theirs, known.name,
                        call(known, theirs), girl(known), npc.m_217043_().m_188503_(1 << 20)), com.hex.kagune.control.family.TalkCues.Scene.MIND, temper(child),
                    temper(known)));
                return true;
            }
        }
        if (mind.mindingTalk != null) {
            if (!mind.mindingTalk.over()) {
                return true; // (held by the talk itself)
            }
            boolean said = mind.mindingTalk.ended == com.hex.kagune.control.family.Talks.End.DONE;
            mind.mindingTalk = null;
            if (!said) {
                mind.minding = 0; // cut short (hurt, a fight, a parent or a player took over): it leaves it be
                return false;
            }
        }
        Vec3 door = new Vec3(house.pos().m_123341_() + 0.5, house.pos().m_123342_(), house.pos().m_123343_() + 0.5);
        double inside = Math.pow(Math.max(3.0, house.radius() - 3.0), 2);
        if (little.m_20238_(door) <= inside && npc.m_20238_(door) <= inside) {
            little.mind.escort = null;
            mind.minding = 0;
            return false;
        }
        little.mind.escort = npc.m_20148_();
        little.mind.escortUntil = now + 100L;
        if (now % 10L == 0L) {
            npc.m_21573_().m_26519_(door.f_82479_, door.f_82480_, door.f_82481_, 0.9);
        }
        return true;
    }

    /**
     * Its kagune just came out: a brother or sister near who sees it has a word (fighting the same thing: "got your back"), not too often.
     */
    static void kagune(ChildNpc npc, Children.Child child, ServerLevel level, long now) {
        for (ChildNpc other : level.m_6443_(ChildNpc.class, npc.m_20191_().m_82400_(12.0), body -> body != npc && body.m_6084_() && body.mind != null)) {
            Children.Child known = Children.get(level, other.childId());
            if (known == null || !siblings(child, known) || now - other.mind.siblingRemarkAt < Siblings.REMARKS_EVERY || !other.m_142582_(npc)
                || other.m_5803_() || Conversation.in(other) != null) {
                continue;
            }
            other.mind.siblingRemarkAt = now;
            boolean together = other.mind.target != 0 && other.mind.target == (npc.mind == null ? -1 : npc.mind.target);
            ChildStage theirs = known.stage(now);
            List<Siblings.Line> talk = Siblings.talk(together ? Siblings.Kind.TEAM : Siblings.Kind.REACT, temper(known), theirs, known.name,
                call(known, theirs), girl(known), temper(child), child.stage(now), child.name, call(child, child.stage(now)), girl(child),
                other.m_217043_().m_188503_(1 << 20));
            queue(other, npc, null, talk, now);
            return;
        }
    }

    /** 1.3.55: a companion parent going after {@code little} (its duty: ParentDuty), or null. */
    static EtoNpc parentGoing(ServerLevel level, ChildNpc little) {
        for (EtoNpc parent : EtoBrain.live()) {
            if (parent.m_9236_() == level && parent.mind != null && parent.mind.duty != null && little.m_20148_().equals(parent.mind.duty.child)) {
                return parent;
            }
        }
        return null;
    }

    /** True if {@code body} is after one of {@code child}'s brothers or sisters (its target): a threat to it too. */
    static boolean afterSibling(LivingEntity target, Children.Child child, ServerLevel level) {
        return target instanceof ChildNpc sib && siblings(child, Children.get(level, sib.childId()));
    }

    // ---------------------------------------------------------------- their words

    /**
     * 1.3.53: their lines as a {@link Conversation}'s (1: {@code first}, 2: {@code second}, 0: {@code parent}); 1.3.57: each with what its
     * speaker does as it says it ({@link com.hex.kagune.control.family.TalkCues}, in {@code scene}, by each one's temper or the parent's way).
     */
    private static List<Conversation.Line> lines(ChildNpc first, ChildNpc second, EtoNpc parent, List<Siblings.Line> talk,
                                                 com.hex.kagune.control.family.TalkCues.Scene scene, ChildTemper firstTemper, ChildTemper secondTemper) {
        List<Conversation.Line> out = new java.util.ArrayList<>();
        int seed = first.m_217043_().m_188503_(1 << 20);
        for (int i = 0; i < talk.size(); i++) {
            Siblings.Line line = talk.get(i);
            if (line.who() == 0 && parent != null) {
                out.add(new Conversation.Line(parent.m_19879_(), line.text(), com.hex.kagune.control.family.ChildCue.NONE,
                    com.hex.kagune.control.family.TalkCues.parent(line.text(), scene, parent.persona().key(), i, seed + i)));
                continue;
            }
            boolean starter = line.who() != 2;
            int speaker = starter ? first.m_19879_() : second.m_19879_();
            out.add(new Conversation.Line(speaker, line.text(), com.hex.kagune.control.family.TalkCues.child(line.text(), scene, starter ? firstTemper
                : secondTemper, starter, i, seed + i), com.hex.kagune.control.npc.EtoGesture.NONE));
        }
        return out;
    }

    /** 1.3.57: the talk a sibling moment's lines are in. */
    private static com.hex.kagune.control.family.TalkCues.Scene scene(Siblings.Kind kind) {
        return switch (kind) {
            case TEASE -> com.hex.kagune.control.family.TalkCues.Scene.TEASE;
            case BRAG -> com.hex.kagune.control.family.TalkCues.Scene.BRAG;
            case COMFORT -> com.hex.kagune.control.family.TalkCues.Scene.COMFORT;
            case SCHEME -> com.hex.kagune.control.family.TalkCues.Scene.SCHEME;
            case SQUABBLE -> com.hex.kagune.control.family.TalkCues.Scene.SQUABBLE;
            case MIND -> com.hex.kagune.control.family.TalkCues.Scene.MIND;
            default -> com.hex.kagune.control.family.TalkCues.Scene.CHAT;
        };
    }

    /** A shout or a remark queued on {@code first}'s words (1: {@code first}, 2: {@code second}, 0: {@code parent}), in turn; when the last ends. */
    private static long queue(ChildNpc first, ChildNpc second, EtoNpc parent, List<Siblings.Line> talk, long now) {
        long at = Math.max(now, first.mind.talkFree);
        for (Siblings.Line line : talk) {
            int speaker = line.who() == 1 ? first.m_19879_() : line.who() == 2 ? second.m_19879_() : parent == null ? first.m_19879_() : parent.m_19879_();
            first.mind.talk.add(new ParentDuty.Said(speaker, line.text(), at));
            at += Parenting.pause(line.text());
        }
        first.mind.talkFree = at;
        return at;
    }

    private static ChildTemper temper(Children.Child child) {
        return ChildTemper.of(child.id, child.mother, child.father);
    }

    /** What it calls its companion parent ("Mom", "Mama", "Dad"...), or its mother if it has none. */
    private static String call(Children.Child child, ChildStage stage) {
        boolean motherNpc = child.mother != null && child.mother.startsWith("npc:");
        boolean fatherNpc = child.father != null && child.father.startsWith("npc:");
        return ChildLines.call(stage, motherNpc || !fatherNpc);
    }

    private static boolean girl(Children.Child child) {
        return child.genome().gender().female();
    }

    private static String name(ServerLevel level, ChildNpc npc) {
        Children.Child known = Children.get(level, npc.childId());
        return known == null ? "" : known.name;
    }
}
