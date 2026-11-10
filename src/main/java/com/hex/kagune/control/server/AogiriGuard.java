package com.hex.kagune.control.server;

import com.hex.kagune.control.npc.GhoulNpc;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 4.0.2: the Aogiri Tree hideout's people at their work (the owner: "Make sure that the guards currently patrol the place very often.
 * There's at least two guarding the entrances, always in the same spot, always returning there no matter what. And first time when you
 * arrive, they should give you a little message with multiple choices. This only appears once, and only one of them will ask you this
 * once. Just asking you the reason why you're there ... If he doesn't like it, they'll attack you. And they won't let you in, and you have
 * to wait let's say 5 MC days").
 *
 * <p>Placed with the mansion ({@code tools/exploration_structures.py}), each member carries what it does: a guard at its post at a gate
 * ({@link #POST}: back to it whenever it has nothing to fight, standing there facing out), the one at each gate who asks a newcomer their
 * business ({@link #ASKS}), the patrol ({@link #PATROL}: round the building on its loop, stop by stop), the rest about their own spot on
 * their own floor. A player is asked once ({@code GhoulNpcTalk.gate}); a wrong answer has every member go for them, and keeps doing so
 * for {@link #BAN} (five days), after which they let them pass. Kept with the player ({@code PlayerPersisted}: through death and logging
 * out).
 */
public final class AogiriGuard {
    static final String POST = "HexAogiriPost";
    static final String PATROL = "HexAogiriPatrol";
    static final String ASKS = "HexAogiriAsks";
    /** A patroller's next stop on the loop. */
    static final String STOP = "HexAogiriStop";
    /** Each player's answer at the gate: asked (and when it was a wrong one, until when they are kept out). */
    static final String GATE = "HexAogiriGate";
    /** Five days (ticks). */
    public static final long BAN = 5L * 24000L;
    /** How near a newcomer must come to the asker's post for it to come and stop them; how close it asks from. */
    static final double NOTICE = 14.0;
    static final double ASK_FROM = 3.5;
    /** After a newcomer walks off without answering: how long before it stops them again (ticks). */
    static final long ASK_AGAIN = 600L;
    private static final Map<UUID, Long> PUT_OFF = WorldTimes.map();
    /** Who is being asked now, by whom (one guard at a time). */
    private static final Map<UUID, Integer> ASKING = new HashMap<>();

    private AogiriGuard() {
    }

    // ---------------------------------------------------------------- the player's answer

    private static CompoundTag gate(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.m_128441_("PlayerPersisted")) {
            root.m_128365_("PlayerPersisted", new CompoundTag());
        }
        CompoundTag kept = root.m_128469_("PlayerPersisted");
        if (!kept.m_128441_(GATE)) {
            kept.m_128365_(GATE, new CompoundTag());
        }
        return kept.m_128469_(GATE);
    }

    /** True if {@code player} has answered at the gate (rightly or not). */
    static boolean asked(Player player) {
        return gate(player).m_128471_("Asked");
    }

    /** True while {@code player} is kept out of the hideout (a wrong answer, under five days ago). */
    static boolean banned(Player player, long now) {
        return now < gate(player).m_128454_("Ban");
    }

    /** Their answer: let in, or ({@code welcome} false) kept out for {@link #BAN} from {@code now}. */
    static void answered(ServerPlayer player, boolean welcome, long now) {
        CompoundTag gate = gate(player);
        gate.m_128379_("Asked", true);
        if (!welcome) {
            gate.m_128356_("Ban", now + BAN);
        }
        ASKING.remove(player.m_20148_());
    }

    /** They walked off without answering: they are stopped again a while later. */
    static void putOff(ServerPlayer player, long now) {
        ASKING.remove(player.m_20148_());
        PUT_OFF.put(player.m_20148_(), now + ASK_AGAIN);
    }

    /** {@code player}, kept out, is one every member near the hideout goes for. */
    static boolean unwelcome(GhoulNpc npc, Player player, long now) {
        return banned(player, now) && !player.m_5833_() && VillagePlace.of(npc) != null;
    }

    // ---------------------------------------------------------------- its day

    /** A member's idle tick at the hideout: true if it did its work (posted, asking, patrolling, about its floor), false if it has none. */
    static boolean idle(GhoulNpc npc, GhoulNpcBrain.Mind mind, ServerLevel level, long now) {
        VillagePlace.Place place = VillagePlace.of(npc);
        if (place == null || !place.hideout()) {
            return false;
        }
        CompoundTag data = npc.getPersistentData();
        if (data.m_128471_(ASKS) && ask(npc, mind, level, now)) {
            return true;
        }
        if (data.m_128471_(POST)) {
            post(npc, place, level, now);
            return true;
        }
        if (data.m_128471_(PATROL)) {
            patrol(npc, mind, place, now);
            return true;
        }
        about(npc, mind, place, now);
        return true;
    }

    /** At its post, always: walked back to it from wherever it was, and there it stands, facing out from the house. */
    private static void post(GhoulNpc npc, VillagePlace.Place place, ServerLevel level, long now) {
        double dx = place.homeX() + 0.5 - npc.m_20185_();
        double dz = place.homeZ() + 0.5 - npc.m_20189_();
        if (dx * dx + dz * dz > 1.5 * 1.5 || Math.abs(place.homeY() - npc.m_20186_()) > 1.5) {
            if (now % 20L == 0L || npc.m_21573_().m_26571_()) {
                npc.m_21573_().m_26519_(place.homeX() + 0.5, place.homeY(), place.homeZ() + 0.5, 1.0);
            }
            return;
        }
        npc.m_21573_().m_26573_();
        // Facing out: from the middle of the place through its post, a little to either side now and then.
        double ox = place.homeX() - place.x();
        double oz = place.homeZ() - place.z();
        double length = Math.max(1.0E-3, Math.hypot(ox, oz));
        double sway = 0.35 * Math.sin((now + npc.m_19879_() * 37L) * 0.01);
        double ax = ox / length * Math.cos(sway) - oz / length * Math.sin(sway);
        double az = ox / length * Math.sin(sway) + oz / length * Math.cos(sway);
        npc.m_21563_().m_24946_(npc.m_20185_() + ax * 8.0, npc.m_20186_() + 1.6, npc.m_20189_() + az * 8.0);
    }

    /** Round the house on its loop, stop by stop, a breath at each. */
    private static void patrol(GhoulNpc npc, GhoulNpcBrain.Mind mind, VillagePlace.Place place, long now) {
        int[][] route = place.route();
        if (route.length == 0) {
            about(npc, mind, place, now);
            return;
        }
        CompoundTag data = npc.getPersistentData();
        if (!data.m_128441_(STOP)) {
            // Its first stop: the nearest.
            int best = 0;
            double near = Double.MAX_VALUE;
            for (int i = 0; i < route.length; i++) {
                double[] at = place.world(route[i]);
                double d = Math.pow(at[0] - npc.m_20185_(), 2) + Math.pow(at[2] - npc.m_20189_(), 2);
                if (d < near) {
                    near = d;
                    best = i;
                }
            }
            data.m_128405_(STOP, best);
        }
        int stop = Math.floorMod(data.m_128451_(STOP), route.length);
        double[] at = place.world(route[stop]);
        double dx = at[0] - npc.m_20185_();
        double dz = at[2] - npc.m_20189_();
        if (dx * dx + dz * dz <= 2.5 * 2.5) {
            if (now >= mind.wanderAt) {
                // There: a breath, then on to the next.
                data.m_128405_(STOP, (stop + 1) % route.length);
                mind.wanderAt = now + 10L + (long)(npc.m_217043_().m_188500_() * 40.0);
            }
            return;
        }
        if (now >= mind.wanderAt && (now % 20L == 0L || npc.m_21573_().m_26571_())) {
            npc.m_21573_().m_26519_(at[0], at[1], at[2], 0.85);
        }
    }

    /** About its own spot on its own floor: a few steps here and there, never far, and back. */
    private static void about(GhoulNpc npc, GhoulNpcBrain.Mind mind, VillagePlace.Place place, long now) {
        if (now < mind.wanderAt || !npc.m_21573_().m_26571_()) {
            return;
        }
        mind.wanderAt = now + 100L + (long)(npc.m_217043_().m_188500_() * 200.0);
        double angle = npc.m_217043_().m_188500_() * Math.PI * 2.0;
        double distance = 1.5 + npc.m_217043_().m_188500_() * 4.5;
        npc.m_21573_().m_26519_(place.homeX() + 0.5 + Math.cos(angle) * distance, place.homeY(), place.homeZ() + 0.5 + Math.sin(angle) * distance, 0.6);
    }

    /**
     * The asker at a gate: a newcomer near its post (never asked, not kept out, not put off a while ago, not being asked by another) is
     * gone up to and asked their business once close. True while it is about it.
     */
    private static boolean ask(GhoulNpc npc, GhoulNpcBrain.Mind mind, ServerLevel level, long now) {
        if (mind.talking != 0) {
            return true; // asking (or talking): it stands where it is
        }
        VillagePlace.Place place = VillagePlace.of(npc);
        ServerPlayer newcomer = null;
        double nearest = NOTICE * NOTICE;
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_5833_() || player.m_7500_() || !player.m_6084_() || asked(player) || banned(player, now)) {
                continue;
            }
            Long off = PUT_OFF.get(player.m_20148_());
            Integer by = ASKING.get(player.m_20148_());
            if (off != null && now < off || by != null && by != npc.m_19879_() && level.m_6815_(by) instanceof GhoulNpc other && other.m_6084_()) {
                continue;
            }
            double d = Math.pow(player.m_20185_() - (place.homeX() + 0.5), 2) + Math.pow(player.m_20189_() - (place.homeZ() + 0.5), 2);
            if (d < nearest && Math.abs(player.m_20186_() - place.homeY()) < 6.0) {
                nearest = d;
                newcomer = player;
            }
        }
        if (newcomer == null) {
            return false;
        }
        ASKING.put(newcomer.m_20148_(), npc.m_19879_());
        npc.m_21563_().m_24960_(newcomer, 30.0F, 30.0F);
        if (npc.m_20280_(newcomer) > ASK_FROM * ASK_FROM || !npc.m_142582_(newcomer)) {
            if (now % 10L == 0L || npc.m_21573_().m_26571_()) {
                npc.m_21573_().m_5624_(newcomer, 1.0);
            }
            return true;
        }
        npc.m_21573_().m_26573_();
        GhoulNpcTalk.gate(newcomer, npc);
        return true;
    }

    /** {@code player} logged out: whoever was asking them lets it go (they are asked when they come back). */
    static void forget(UUID player) {
        ASKING.remove(player);
    }
}
