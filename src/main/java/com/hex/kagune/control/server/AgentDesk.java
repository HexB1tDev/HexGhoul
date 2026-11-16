package com.hex.kagune.control.server;

import com.hex.kagune.control.npc.CcgAgent;
import com.hex.kagune.control.npc.HqRules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * 4.1.4 (the owner: "CCG building, tons of ccg agents guard and patrol the area, sit in their offices"): what the CCG building's
 * investigators do at peace ({@link HqRules}): one with a post stands at it and keeps its watch; one with a desk walks to its chair and
 * sits (held on the chair's step, its body down: {@link CcgAgent#seated}). Anything else an investigator does (a fight, a fright, a
 * stun) gets it up ({@link AgentBrain#tick}).
 */
final class AgentDesk {
    private AgentDesk() {
    }

    /** True if {@code agent} has a post or a desk (it is at it, or on its way: nothing else this tick). */
    static boolean idle(CcgAgent agent, AgentBrain.Mind mind, VillagePlace.Place place, ServerLevel level, long now) {
        CompoundTag data = agent.getPersistentData();
        boolean seat = data.m_128471_(HqRules.SEAT);
        if (!seat && !data.m_128471_(HqRules.POST)) {
            return false;
        }
        float yaw = HqRules.yaw(data.m_128451_(HqRules.YAW), place.turns());
        // A chair that is gone (broken, blown up): it stands where it was, as at a post.
        if (seat && !level.m_8055_(new BlockPos(place.homeX(), place.homeY(), place.homeZ())).m_60795_()) {
            double[] at = HqRules.seat(place.homeX(), place.homeY(), place.homeZ(), yaw);
            if (HqRules.sits(at[0] - agent.m_20185_(), at[1] - agent.m_20186_(), at[2] - agent.m_20189_())) {
                agent.m_21573_().m_26573_();
                double ox = at[0] - agent.m_20185_();
                double oy = at[1] - agent.m_20186_();
                double oz = at[2] - agent.m_20189_();
                if (ox * ox + oy * oy + oz * oz > 1.0E-4) {
                    agent.m_6021_(at[0], at[1], at[2]); // onto the chair's step (held there: its step carries it)
                }
                agent.m_20256_(Vec3.f_82478_);
                turn(agent, yaw);
                agent.f_20885_ = yaw;
                agent.seated(true);
                mind.seatKept = true;
            } else {
                walk(agent, place, now);
            }
            return true;
        }
        double dx = place.homeX() + 0.5 - agent.m_20185_();
        double dz = place.homeZ() + 0.5 - agent.m_20189_();
        if (HqRules.posted(dx, dz) && Math.abs(place.homeY() - agent.m_20186_()) <= HqRules.SEAT_HEIGHT) {
            agent.m_21573_().m_26573_();
            turn(agent, yaw);
        } else {
            walk(agent, place, now);
        }
        return true;
    }

    private static void walk(CcgAgent agent, VillagePlace.Place place, long now) {
        if (now % 20L == 0L || agent.m_21573_().m_26571_()) {
            agent.m_21573_().m_26519_(place.homeX() + 0.5, place.homeY(), place.homeZ() + 0.5, HqRules.PACE);
        }
    }

    private static void turn(CcgAgent agent, float yaw) {
        agent.m_146922_(yaw);
        agent.f_20883_ = yaw;
    }

    /** {@code agent} gets up (whatever it does next moves it as ever). */
    static void stand(CcgAgent agent) {
        agent.seated(false);
    }
}
