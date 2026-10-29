package com.hex.kagune.control.server;

import com.hex.kagune.control.npc.GhoulNpc;
import net.minecraft.world.entity.LivingEntity;

/**
 * 7.0.1: what counts as a ghoul in the world besides ghoul players, for the quests and hunts: the ghoul NPCs (7.0.2), every one a
 * ghoul, the ones whose temper goes for people on sight hostile too.
 */
final class GhoulKinds {
    private GhoulKinds() {
    }

    /** True for a ghoul NPC. */
    static boolean ghoul(LivingEntity entity) {
        return entity instanceof GhoulNpc;
    }

    /** True for a ghoul NPC that goes for people on sight (its kill is a hostile one). */
    static boolean hostile(LivingEntity entity) {
        return entity instanceof GhoulNpc npc && npc.identity() != null && npc.identity().temper().hostile();
    }

    /** A ghoul NPC's temper, as the quests name it ("feral", "anteiku"), or null. */
    static String temper(LivingEntity entity) {
        return entity instanceof GhoulNpc npc && npc.identity() != null ? npc.identity().temper().name().toLowerCase(java.util.Locale.ROOT) : null;
    }

    /** True once ghoul NPCs roam the world (a hunt may then ask for one). */
    static boolean roaming() {
        return true;
    }
}
