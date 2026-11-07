package com.hex.kagune.control.server;

import com.hex.kagune.control.family.ChildLines;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.family.ChildTemper;
import com.hex.kagune.control.family.Homecoming;
import com.hex.kagune.control.family.SiblingWelcome;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.EtoNpc;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 3.2.1: a player bringing their child home, as it happens ({@link Homecoming}'s rules). {@code ChildBrain} tells it when a child comes
 * back inside its home after a while out ({@link #cameHome}); if a parent of it is carrying it in or walking it in at their side, that is
 * a homecoming, remembered a moment ({@link Homecoming#LASTS}) so the scenes know (the child's {@code back}, a companion's {@code brought},
 * both {@code found} if it had gone off on its own). Its other parent at home, if a companion who sees them (Eto, Touka, Ken: not out with
 * the player, not fighting, not asleep, not in another talk), meets them at the door with a {@code brought} scene while the child calls out
 * to them ({@link Homecoming#hello}); otherwise the child has its own {@code back} scene with the player. A companion still out looking
 * for that child stops: it is home. 3.3.9: a family with no home of its own has homecomings too, back at the place the child lives
 * ({@link Homecoming#NO_HOUSE}); their scenes are the ones that never speak of a house ({@code !home}, or no {@code home}).
 */
final class HomecomingServer {
    private HomecomingServer() {
    }

    /** A homecoming: which child (its id), who brought it, when, and whether it was found (else an outing together). */
    record Brought(String child, UUID player, long at, boolean found) {
    }

    /** The last homecoming of each child (by its id). */
    private static final Map<String, Brought> BROUGHT = WorldTimes.kept();

    /** How far off its other parent may be to meet them at the door (blocks). */
    static final double WAITING = 24.0;

    // ---------------------------------------------------------------- who brings it

    /** The player {@code npc} is with as it goes: the one carrying it, else the one it follows (null: on its own, or with an NPC). */
    static UUID with(ChildNpc npc, ChildBrain.Mind mind, ServerLevel level) {
        if (mind.carrier != null && ChildBrain.player(level, mind.carrier) != null) {
            return mind.carrier;
        }
        UUID follows = npc.following();
        return follows != null && ChildBrain.player(level, follows) != null ? follows : null;
    }

    /** The parent bringing it in now (carrying it, or with it following them), or null. */
    private static ServerPlayer bringer(ChildNpc npc, ChildBrain.Mind mind, Children.Child child, ServerLevel level) {
        UUID id = with(npc, mind, level);
        ServerPlayer player = id == null ? null : ChildBrain.player(level, id);
        return player != null && player.m_6084_() && player.m_9236_() == level && child.parent(Pregnancies.player(player.m_20148_())) ? player : null;
    }

    // ---------------------------------------------------------------- the moment it comes in

    /**
     * From its home check: {@code npc} is back inside its home after {@code away} ticks out ({@code leftWith}: who it went out with). A
     * homecoming if a parent brings it in, it was out long enough, and its last one was a while ago.
     */
    static void cameHome(ChildNpc npc, ChildBrain.Mind mind, Children.Child child, UUID leftWith, ServerLevel level, long now, long away) {
        ServerPlayer by = bringer(npc, mind, child, level);
        if (by == null) {
            return;
        }
        Brought last = BROUGHT.get(child.id);
        if (!Homecoming.counts(away, last == null ? Long.MAX_VALUE : now - last.at())) {
            return;
        }
        boolean found = Homecoming.found(leftWith, by.m_20148_());
        BROUGHT.put(child.id, new Brought(child.id, by.m_20148_(), now, found));
        ParentDuty.broughtHome(npc, level); // nobody need go out after it now
        greet(npc, child, by, found, level, now);
    }

    /** Its other parent meets them at the door, the child calling out to them; with nobody waiting, the child's own word. */
    private static void greet(ChildNpc npc, Children.Child child, ServerPlayer player, boolean found, ServerLevel level, long now) {
        siblings(npc, child, player, found, level, now); // 3.4.0
        EtoNpc waiting = waiting(child, player, level);
        if (waiting != null && SceneServer.approachBrought(waiting, player, now)) {
            ChildStage stage = child.stage(now);
            ChildTemper temper = ChildTemper.of(child.id, child.mother, child.father);
            String call = ChildLines.call(stage, waiting.persona().female());
            npc.mind.talk.add(new ParentDuty.Said(npc.m_19879_(), Homecoming.hello(temper, stage, found, call, npc.m_217043_().m_188503_(1 << 20)), now + 8L));
            return;
        }
        SceneServer.approachBack(npc, child, player, now);
    }

    /**
     * 3.4.0 (the owner's list: siblings at home reacting to the one brought back): its brothers and sisters near, who were not out with it
     * (not carried by nor following {@code player}), awake and with nothing else to say, react to it ({@link SiblingWelcome}): one says its
     * piece, the one brought back answers; at most {@link SiblingWelcome#MOST} of them, one after the other, after its call-out at the door.
     */
    private static void siblings(ChildNpc npc, Children.Child child, ServerPlayer player, boolean found, ServerLevel level, long now) {
        if (npc.mind == null) {
            return;
        }
        ChildStage stage = child.stage(now);
        ChildTemper temper = ChildTemper.of(child.id, child.mother, child.father);
        long at = now + SiblingWelcome.FIRST;
        int spoke = 0;
        for (ChildNpc other : level.m_6443_(ChildNpc.class, npc.m_20191_().m_82400_(SiblingWelcome.NEAR), body -> body != npc && body.m_6084_() && body.mind != null)) {
            if (spoke >= SiblingWelcome.MOST) {
                break;
            }
            Children.Child known = Children.get(level, other.childId());
            if (known == null || !SiblingLife.siblings(child, known) || other.m_5803_() || !other.mind.talk.isEmpty()
                || player.m_20148_().equals(other.mind.carrier) || player.m_20148_().equals(other.following())) {
                continue;
            }
            ChildStage theirs = known.stage(now);
            ChildTemper own = ChildTemper.of(known.id, known.mother, known.father);
            SiblingWelcome.Act act = SiblingWelcome.act(found, known.born < child.born, own, theirs, stage);
            if (act == null) {
                continue;
            }
            int seed = other.m_217043_().m_188503_(1 << 20);
            other.mind.talk.add(new ParentDuty.Said(other.m_19879_(), SiblingWelcome.open(act, own, theirs, child.name, seed), at));
            npc.mind.talk.add(new ParentDuty.Said(npc.m_19879_(), SiblingWelcome.reply(act, temper, stage, known.name, seed + 1), at + SiblingWelcome.GAP));
            Conversation.face(other, npc);
            at += 2L * SiblingWelcome.GAP;
            spoke++;
        }
    }

    /** A companion parent of {@code child} at home near {@code player}, free to meet them (not out with them, fighting, asleep or talking). */
    private static EtoNpc waiting(Children.Child child, ServerPlayer player, ServerLevel level) {
        EtoNpc best = null;
        double bestDistance = WAITING * WAITING;
        for (EtoNpc npc : level.m_6443_(EtoNpc.class, player.m_20191_().m_82400_(WAITING), body -> body.m_6084_() && body.mind != null)) {
            if (!child.parent(Pregnancies.npc(npc.persona().key())) || npc.mind.target != 0 || npc.m_5803_() || npc.following() != null
                || Conversation.in(npc) != null || npc.mind.duty != null) {
                continue;
            }
            double distance = npc.m_20280_(player);
            if (distance < bestDistance) {
                best = npc;
                bestDistance = distance;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- what the scenes know

    /** {@code child}'s homecoming by {@code player} if it is still the moment, else null. */
    static Brought of(Children.Child child, ServerPlayer player, long now) {
        Brought b = child == null ? null : BROUGHT.get(child.id);
        return b != null && b.player().equals(player.m_20148_()) && Homecoming.fresh(now - b.at()) ? b : null;
    }

    /** The latest of {@code kids}' homecomings by {@code player} that is still the moment, else null. */
    static Brought latest(List<Children.Child> kids, ServerPlayer player, long now) {
        Brought best = null;
        for (Children.Child child : kids) {
            Brought b = of(child, player, now);
            if (b != null && (best == null || b.at() > best.at())) {
                best = b;
            }
        }
        return best;
    }

    /** The one of {@code kids} that {@code player} carries now (in arms, on shoulders, by the hand), or null. */
    static Children.Child carried(ServerLevel level, ServerPlayer player, List<Children.Child> kids) {
        for (Children.Child child : kids) {
            com.hex.kagune.control.npc.UniqueNpc unique = UniqueNpcs.get(level, "child:" + child.id);
            if (unique != null && unique.body != null && level.m_8791_(unique.body) instanceof ChildNpc body && body.mind != null
                && player.m_20148_().equals(body.mind.carrier)) {
                return child;
            }
        }
        return null;
    }
}
