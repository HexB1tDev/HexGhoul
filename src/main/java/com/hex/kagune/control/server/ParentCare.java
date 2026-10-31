package com.hex.kagune.control.server;

import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoNpc;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

/**
 * 1.3.15: a companion as a parent (the owner: "they will protect the baby at all costs, and stay with them as long as it's still a child;
 * they can grab them and hold them in their arms properly"). For each of its children still a child (baby, toddler, child; not a teen):
 * anything hostile near it, or after it, is fought ({@link #threat}; anything that hurts it, too: {@code EtoBrain.childHurt}); calm, the
 * companion keeps close to it ({@link #tend}), and now and then picks a baby or a toddler up and holds it in its arms a while (the child's
 * own carrying: {@code ChildBrain.pickUp}, drawn in the arms; its cuddling pose, {@link EtoGesture#PET}), putting it down to fight, to
 * kiss, to take a hand.
 */
final class ParentCare {
    /** How far it looks for its children, how close it keeps to them, how near a hostile to one of them is a threat (blocks). */
    static final double LOOKS = 32.0;
    static final double KEEPS = 6.0;
    static final double GUARDS = 10.0;
    /** How long it holds a child (ticks, at least and more), how long until it may pick one up again, and its chance each second. */
    static final long HOLDS = 600L;
    static final long HOLDS_MORE = 900L;
    static final long REST = 1800L;
    static final double PICKS = 0.04;

    private ParentCare() {
    }

    /** Its children about it still young enough to be looked after (bodies in the world, nearest first not guaranteed). */
    static java.util.List<ChildNpc> children(EtoNpc npc, ServerLevel level) {
        String key = Pregnancies.npc(npc.persona().key());
        return level.m_6443_(ChildNpc.class, npc.m_20191_().m_82400_(LOOKS), child -> {
            if (!child.m_6084_() || child.stage == ChildStage.TEEN) {
                return false;
            }
            Children.Child known = Children.get(level, child.childId());
            return known != null && known.parent(key);
        });
    }

    /** Something hostile near one of its children (or after one): it goes for it. Null if there is none. */
    static LivingEntity threat(EtoNpc npc, ServerLevel level) {
        LivingEntity best = null;
        double nearest = Double.MAX_VALUE;
        for (ChildNpc child : children(npc, level)) {
            AABB around = child.m_20191_().m_82400_(GUARDS);
            for (LivingEntity body : level.m_6443_(LivingEntity.class, around, body -> body instanceof Enemy && body.m_6084_())) {
                boolean after = body instanceof Mob mob && mob.m_5448_() == child;
                double distance = body.m_20280_(child);
                if ((after || distance < 6.0 * 6.0) && distance < nearest) {
                    nearest = distance;
                    best = body;
                }
            }
        }
        return best;
    }

    /** The child it holds now, if any. */
    static ChildNpc held(EtoNpc npc, EtoBrain.Mind mind, ServerLevel level) {
        if (mind.cradling == null) {
            return null;
        }
        return level.m_8791_(mind.cradling) instanceof ChildNpc child && child.m_6084_() && ChildBrain.carried(child) ? child : null;
    }

    /** It puts down whatever child it holds (to fight, to kiss, to take a hand). */
    static void drop(EtoNpc npc, EtoBrain.Mind mind, ServerLevel level, long now) {
        ChildNpc child = held(npc, mind, level);
        if (child != null && child.mind != null) {
            ChildBrain.putDown(child, child.mind);
        }
        if (mind.cradling != null) {
            mind.cradling = null;
            mind.nextCradle = now + REST;
            if (mind.gesture == EtoGesture.PET) {
                EtoBrain.gesture(mind, EtoGesture.NONE, now);
            }
        }
    }

    /** Calm: it holds its child a while, or goes to it, or picks it up. True if that took it this tick. */
    static boolean tend(EtoNpc npc, EtoBrain.Mind mind, ServerLevel level, ServerPlayer partner, long now) {
        ChildNpc holding = held(npc, mind, level);
        if (mind.cradling != null && holding == null) {
            mind.cradling = null; // put down by someone else, or gone
        }
        if (holding != null) {
            if (now >= mind.cradleUntil) {
                drop(npc, mind, level, now);
                return false;
            }
            if (mind.gesture != EtoGesture.PET) {
                EtoBrain.gesture(mind, EtoGesture.PET, now);
            }
            if (now % 10L == 0L && partner != null && partner.m_20280_(npc) > 4.0 * 4.0 && partner.m_20280_(npc) < 16.0 * 16.0) {
                npc.m_21573_().m_5624_(partner, 0.8); // carries the child over to its other parent
            } else if (now % 10L == 0L) {
                npc.m_21573_().m_26573_();
            }
            return true;
        }
        if (now % 20L != 9L) {
            return false;
        }
        ChildNpc nearest = null;
        double best = Double.MAX_VALUE;
        for (ChildNpc child : children(npc, level)) {
            double distance = child.m_20280_(npc);
            if (distance < best) {
                best = distance;
                nearest = child;
            }
        }
        if (nearest == null) {
            return false;
        }
        double distance = Math.sqrt(best);
        if (distance > KEEPS) {
            npc.m_21573_().m_5624_(nearest, 0.9);
            return true;
        }
        boolean free = nearest.mind != null && !ChildBrain.carried(nearest) && nearest.stage.carried();
        if (free && distance < 2.5 && now >= mind.nextCradle && npc.m_217043_().m_188500_() < PICKS) {
            ChildBrain.pickUp(nearest, npc);
            mind.cradling = nearest.m_20148_();
            mind.cradleUntil = now + HOLDS + npc.m_217043_().m_188503_((int)HOLDS_MORE);
            EtoBrain.gesture(mind, EtoGesture.PET, now);
            if (partner != null && partner.m_20280_(npc) < 24.0 * 24.0) {
                Children.Child known = Children.get(level, nearest.childId());
                String name = known == null ? "the baby" : known.name;
                EtoBrain.say(npc, partner, "*picks " + name + " up and holds " + (nearest.stage == ChildStage.BABY ? "them close, rocking them"
                    : "them on " + (npc.persona().female() ? "her" : "his") + " hip") + "*");
            }
            return true;
        }
        if (free && distance > 2.5 && now >= mind.nextCradle && npc.m_217043_().m_188500_() < PICKS * 2.0) {
            npc.m_21573_().m_5624_(nearest, 0.8);
        }
        return false;
    }

    /** True if {@code child} is one of {@code npc}'s. */
    static boolean parentOf(EtoNpc npc, ChildNpc child, ServerLevel level) {
        Children.Child known = Children.get(level, child.childId());
        return known != null && known.parent(Pregnancies.npc(npc.persona().key()));
    }
}
