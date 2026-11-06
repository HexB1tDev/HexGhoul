package com.hex.kagune.control.server;

import com.hex.kagune.control.family.HomeRules;
import com.hex.kagune.control.family.HomeRecovery;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

/**
 * 2.0.7 (the owner: "NPCS should not be teleported home unless they die or bug inside a wall"): a spouse or a child is never put home for
 * being far; it walks back (its leash, {@code MoveTowardsRestrictionGoal}). It is put home only when its body is stuck inside blocks for
 * {@link HomeRules#WALLED} looks, or when it is out when it should be home and gets no nearer for {@link HomeRules#NO_HEADWAY} with nobody
 * near to see (stuck, not slow).
 */
final class Homeward {
    private static final Map<Entity, HomeRecovery> STATES = new WeakHashMap<>();

    private Homeward() {
    }

    /** Each tick (it looks every 10): true if {@code mob} must be put at {@code home} now ({@code away}: out when it should be home). */
    static boolean bugged(PathfinderMob mob, ServerLevel level, Vec3 home, boolean away, boolean busy, long now) {
        if (now % 10L != 0L) {
            return false;
        }
        HomeRecovery state = STATES.computeIfAbsent(mob, any -> new HomeRecovery());
        // noCollision also considers other entities: a crowd or melee was mistaken for a wall. Only actual blocks count.
        boolean inside = !busy && !mob.f_19794_ && !mob.m_5803_()
            && level.m_186434_(mob, mob.m_20191_().m_82400_(-0.15)).iterator().hasNext();
        HomeRecovery.Reason reason = state.sample(inside, away, busy, Math.sqrt(mob.m_20238_(home)), now);
        if (reason == HomeRecovery.Reason.WALLED || reason == HomeRecovery.Reason.STALLED && !seen(mob, level)) {
            STATES.remove(mob);
            return true;
        }
        return false;
    }

    /** Put home (its way there stopped). */
    static void put(PathfinderMob mob, Vec3 home) {
        mob.m_21573_().m_26573_();
        mob.m_6021_(home.f_82479_, home.f_82480_, home.f_82481_);
    }

    private static boolean seen(Entity mob, ServerLevel level) {
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20280_(mob) < 32.0 * 32.0) {
                return true;
            }
        }
        return false;
    }
}
