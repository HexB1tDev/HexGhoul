package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlEntities;
import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.npc.CcgAgent;
import com.hex.kagune.control.quinque.Quinque;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.27: where the CCG's investigators come from: the overworld, any time of day, always two together. Every
 * {@link AgentRules#SPAWN_EVERY} ticks each player out in it has a {@link AgentRules#SPAWN_CHANCE} chance of a pair turning up
 * {@link AgentRules#SPAWN_NEAREST} to {@link AgentRules#SPAWN_FARTHEST} blocks away, on the surface, no player close to the spot and no
 * other investigator anywhere near it ({@link AgentRules#spaced}): about one pair a day near someone (2.0.1; every two days before).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class AgentSpawner {
    private AgentSpawner() {
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.m_46472_() != Level.f_46428_) {
            return;
        }
        if (level.m_46467_() % AgentRules.SPAWN_EVERY != 0L) {
            return;
        }
        try {
            station(level); // 4.1.82
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "the ghoul division's post at the tower");
        }
        for (ServerPlayer player : new ArrayList<>(level.m_6907_())) {
            if (!player.m_5833_() && player.m_217043_().m_188500_() < AgentRules.SPAWN_CHANCE) {
                try {
                    // 4.1.46: now and then three of its ghoul division instead of a pair.
                    if (player.m_217043_().m_188500_() < DIVISION_CHANCE) {
                        division(level, player);
                    } else {
                        near(level, player);
                    }
                } catch (RuntimeException failure) {
                    com.hex.kagune.control.Safety.caught(failure, "a pair of investigators turning up");
                }
            }
        }
    }

    /** 4.1.46: the share of the CCG's turning up that is three of its ghoul division. */
    static final double DIVISION_CHANCE = 0.35;

    /**
     * 4.1.46 (the owner: "New Re CCG Ghoul division (Normal ccg agents (HUMANS not ghouls) ... they get a random kagune ... they spawn in
     * trios"): three of the CCG's ghoul division near {@code player}, where a pair would be ({@link #clear}): each a human of the CCG's in
     * its Formal Outfit with a kagune of its own (rolled as a ghoul met there would be: its kind, its rank), no kakugan, the three of one
     * squad (its number in each one's saved data) walking together. Always three: fewer placed and none are. The three (empty if none).
     */
    static java.util.List<com.hex.kagune.control.npc.GhoulNpc> division(ServerLevel level, ServerPlayer player) {
        java.util.List<com.hex.kagune.control.npc.GhoulNpc> made = new ArrayList<>();
        int mastery = com.hex.kagune.data.KaguneProfile.isUnlocked(player) ? com.hex.kagune.data.KaguneProfile.mastery(player) : 0;
        int number = 1 + player.m_217043_().m_188503_(Integer.MAX_VALUE - 1);
        for (int attempt = 0; attempt < 4 && made.isEmpty(); attempt++) {
            double angle = player.m_217043_().m_188500_() * Math.PI * 2.0;
            double distance = AgentRules.SPAWN_NEAREST + (AgentRules.SPAWN_FARTHEST - AgentRules.SPAWN_NEAREST) * player.m_217043_().m_188500_();
            double x = player.m_20185_() + Math.cos(angle) * distance;
            double z = player.m_20189_() + Math.sin(angle) * distance;
            if (!clear(level, x, z)) {
                continue;
            }
            for (int i = 0; i < com.hex.kagune.control.npc.Gang.TRIO; i++) {
                com.hex.kagune.control.npc.NpcIdentity who = com.hex.kagune.control.npc.NpcIdentity.roll(mastery, () -> player.m_217043_().m_188500_())
                    .ganged(com.hex.kagune.control.npc.Gang.CCG_DIVISION, 0);
                double side = i * Math.PI * 2.0 / com.hex.kagune.control.npc.Gang.TRIO;
                com.hex.kagune.control.npc.GhoulNpc one = GhoulNpcSpawner.place(level, x + (i == 0 ? 0.0 : Math.cos(side) * AgentRules.PAIR_APART),
                    z + (i == 0 ? 0.0 : Math.sin(side) * AgentRules.PAIR_APART), who, number);
                if (one != null) {
                    made.add(one);
                }
            }
            if (made.size() < com.hex.kagune.control.npc.Gang.TRIO) {
                made.forEach(com.hex.kagune.control.npc.GhoulNpc::m_146870_); // never fewer than three
                made.clear();
            }
        }
        return made;
    }

    /** 4.1.82: on each of a posted squad: its post (block x, y, z). */
    static final String POST = "HexDivisionPost";
    /** 4.1.82: how far (blocks) a posted squad strays from its post; how near the tower a player brings one; how far out it stands. */
    static final double POST_REACH = 20.0;
    static final double STATION_NEAR = 96.0;
    static final double POST_NEAREST = 32.0;
    static final double POST_FARTHEST = 44.0;
    /** 4.1.82: the least time (ticks: five minutes) between two squads posted at the tower (one wiped out is not made again at once). */
    static final long POST_EVERY = 6000L;
    /** 4.1.82: when a squad was last posted at the tower (the world's game time). */
    private static final java.util.Map<String, Long> POSTED = WorldTimes.map();

    /**
     * 4.1.82 (the owner: "where tf is the human ghoul division from the ccg?"): they only ever turned up by chance (a third of the CCG's
     * turning up, about one squad every three days of play near someone), looking like any investigator. Now one squad of three is always
     * at their tower: when a player comes within {@link #STATION_NEAR} of it and none of its squad is there, three are made on the ground
     * round it ({@link #POST_NEAREST} to {@link #POST_FARTHEST} from its middle) and keep to their post ({@link #POST}, GhoulNpcBrain); at most
     * one squad each {@link #POST_EVERY}. The chance squads go on as before.
     */
    static void station(ServerLevel level) {
        int[] middle = com.hex.kagune.control.ControlStructures.middle(level, "ccg_building");
        if (middle == null) {
            return;
        }
        ServerPlayer near = null;
        for (ServerPlayer player : level.m_6907_()) {
            double dx = player.m_20185_() - middle[0];
            double dz = player.m_20189_() - middle[2];
            if (!player.m_5833_() && dx * dx + dz * dz <= STATION_NEAR * STATION_NEAR) {
                near = player;
            }
        }
        if (near == null) {
            return;
        }
        double r = STATION_NEAR;
        AABB round = new AABB(middle[0] - r, middle[1] - 64.0, middle[2] - r, middle[0] + r, middle[1] + 128.0, middle[2] + r);
        if (!level.m_6443_(com.hex.kagune.control.npc.GhoulNpc.class, round, g -> g.m_6084_() && g.getPersistentData().m_128441_(POST + "X")).isEmpty()) {
            return; // its squad is there
        }
        long now = level.m_46467_();
        Long last = POSTED.get("ccg_building");
        if (last != null && now - last < POST_EVERY && now >= last) {
            return;
        }
        ServerPlayer them = near;
        int mastery = com.hex.kagune.data.KaguneProfile.isUnlocked(them) ? com.hex.kagune.data.KaguneProfile.mastery(them) : 0;
        int number = 1 + them.m_217043_().m_188503_(Integer.MAX_VALUE - 1);
        java.util.List<com.hex.kagune.control.npc.GhoulNpc> made = new ArrayList<>();
        for (int attempt = 0; attempt < 6 && made.isEmpty(); attempt++) {
            double angle = near.m_217043_().m_188500_() * Math.PI * 2.0;
            double distance = POST_NEAREST + (POST_FARTHEST - POST_NEAREST) * near.m_217043_().m_188500_();
            double x = middle[0] + 0.5 + Math.cos(angle) * distance;
            double z = middle[2] + 0.5 + Math.sin(angle) * distance;
            for (int i = 0; i < com.hex.kagune.control.npc.Gang.TRIO; i++) {
                com.hex.kagune.control.npc.NpcIdentity who = com.hex.kagune.control.npc.NpcIdentity.roll(mastery, () -> them.m_217043_().m_188500_())
                    .ganged(com.hex.kagune.control.npc.Gang.CCG_DIVISION, 0);
                double side = i * Math.PI * 2.0 / com.hex.kagune.control.npc.Gang.TRIO;
                com.hex.kagune.control.npc.GhoulNpc one = GhoulNpcSpawner.place(level, x + (i == 0 ? 0.0 : Math.cos(side) * AgentRules.PAIR_APART),
                    z + (i == 0 ? 0.0 : Math.sin(side) * AgentRules.PAIR_APART), who, number);
                if (one != null) {
                    made.add(one);
                }
            }
            if (made.size() < com.hex.kagune.control.npc.Gang.TRIO) {
                made.forEach(com.hex.kagune.control.npc.GhoulNpc::m_146870_); // never fewer than three
                made.clear();
                continue;
            }
            for (com.hex.kagune.control.npc.GhoulNpc one : made) {
                one.getPersistentData().m_128405_(POST + "X", (int)Math.floor(x));
                one.getPersistentData().m_128405_(POST + "Y", (int)Math.floor(made.get(0).m_20186_()));
                one.getPersistentData().m_128405_(POST + "Z", (int)Math.floor(z));
            }
        }
        if (!made.isEmpty()) {
            POSTED.put("ccg_building", now);
        }
    }

    /** A pair near {@code player}, if a place for them is found on the first few tries (false if not). */
    static boolean near(ServerLevel level, ServerPlayer player) {
        for (int attempt = 0; attempt < 4; attempt++) {
            double angle = player.m_217043_().m_188500_() * Math.PI * 2.0;
            double distance = AgentRules.SPAWN_NEAREST + (AgentRules.SPAWN_FARTHEST - AgentRules.SPAWN_NEAREST) * player.m_217043_().m_188500_();
            double x = player.m_20185_() + Math.cos(angle) * distance;
            double z = player.m_20189_() + Math.sin(angle) * distance;
            if (!clear(level, x, z)) {
                continue;
            }
            int look = AgentRules.regularLook(player.m_217043_().m_188500_());
            Quinque kind = AgentRules.kind(player.m_217043_().m_188500_());
            CcgAgent leader = place(level, x, z);
            if (leader == null) {
                continue;
            }
            // 4.1.24: the White Reaper never turns up in the wild any more (AgentRules.REAPER_CHANCE): he is at home in his mansion.
            if (partner(leader, level, look, kind) == null) {
                leader.m_146870_(); // never one alone
                continue;
            }
            return true;
        }
        return false;
    }

    /** True if no player is close to {@code x, z} and no other investigator is anywhere near it. */
    static boolean clear(ServerLevel level, double x, double z) {
        for (ServerPlayer player : level.m_6907_()) {
            double dx = player.m_20185_() - x;
            double dz = player.m_20189_() - z;
            if (dx * dx + dz * dz < AgentRules.CLEAR_OF_PLAYERS * AgentRules.CLEAR_OF_PLAYERS) {
                return false;
            }
        }
        double y = level.m_6924_(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)Math.floor(x), (int)Math.floor(z));
        double r = AgentRules.ALONE;
        double nearest = Double.MAX_VALUE;
        for (CcgAgent other : level.m_6443_(CcgAgent.class, new AABB(x - r, y - r, z - r, x + r, y + r, z + r), CcgAgent::m_6084_)) {
            nearest = Math.min(nearest, Math.hypot(other.m_20185_() - x, other.m_20189_() - z));
        }
        // 4.1.46: the CCG's ghoul division counts as the CCG's.
        for (com.hex.kagune.control.npc.GhoulNpc other : level.m_6443_(com.hex.kagune.control.npc.GhoulNpc.class, new AABB(x - r, y - r, z - r, x + r, y + r, z + r),
            g -> g.m_6084_() && AgentBrain.ccg(g))) {
            nearest = Math.min(nearest, Math.hypot(other.m_20185_() - x, other.m_20189_() - z));
        }
        return AgentRules.spaced(nearest);
    }

    /** An investigator on the surface at {@code x, z}: on solid ground, out of any fluid, with room to stand (null if not). */
    static CcgAgent place(ServerLevel level, double x, double z) {
        int bx = (int)Math.floor(x);
        int bz = (int)Math.floor(z);
        return placeAt(level, bx, level.m_6924_(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz), bz);
    }

    /** An investigator standing in block {@code bx, y, bz}: on solid ground, out of any fluid, with room to stand (null if not). */
    static CcgAgent placeAt(ServerLevel level, int bx, int y, int bz) {
        BlockState ground = level.m_8055_(BlockPos.m_274561_(bx + 0.5, y - 1, bz + 0.5));
        if (ground.m_60795_() || !ground.m_60819_().m_76178_()) {
            return null;
        }
        CcgAgent agent = ControlEntities.AGENT.get().m_20615_(level);
        if (agent == null) {
            return null;
        }
        agent.m_7678_(bx + 0.5, y, bz + 0.5, (float)(agent.m_217043_().m_188500_() * 360.0), 0.0F);
        if (!level.m_45786_(agent)) {
            return null;
        }
        return level.m_7967_(agent) ? agent : null;
    }

    /**
     * {@code leader}'s partner, made beside it ({@link AgentRules#PAIR_APART} off, wherever there is room): the two paired, the leader
     * looking like {@code look} with {@code kind}, its partner someone else with another quinque. Null if there was nowhere to stand.
     */
    static CcgAgent partner(CcgAgent leader, ServerLevel level, int look, Quinque kind) {
        double r = AgentRules.PAIR_APART;
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = attempt * Math.PI / 4.0 + leader.m_217043_().m_188500_();
            double x = leader.m_20185_() + Math.cos(angle) * r;
            double z = leader.m_20189_() + Math.sin(angle) * r;
            // Beside it where it stands (a command in a building), else on the surface there.
            CcgAgent other = placeAt(level, (int)Math.floor(x), (int)Math.floor(leader.m_20186_() + 0.01), (int)Math.floor(z));
            if (other == null && attempt >= 4) {
                other = place(level, x, z);
            }
            if (other == null) {
                continue;
            }
            int otherLook = Math.floorMod(look + 1 + (int)Math.floor(leader.m_217043_().m_188500_() * (AgentRules.REGULAR_LOOKS - 1)),
                AgentRules.REGULAR_LOOKS);
            Quinque otherKind = AgentRules.partnerKind(kind, leader.m_217043_().m_188500_());
            AgentBrain.pair(leader, look, kind, other, otherLook, otherKind);
            return other;
        }
        return null;
    }
}
