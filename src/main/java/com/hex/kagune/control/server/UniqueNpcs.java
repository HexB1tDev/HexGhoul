package com.hex.kagune.control.server;

import com.hex.kagune.control.npc.Romance;
import com.hex.kagune.control.npc.UniqueBody;
import com.hex.kagune.control.npc.UniqueNpc;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.2: the one-of-a-kind NPCs of a world ({@link UniqueNpc}), kept with the overworld: only one body of each lives (any other that
 * turns up, a second structure's, a summoned one, is sent away on its first tick), her partner for good, and her coming back to her
 * home a couple of days after she was killed (ghouls heal), once a player is near enough for the place to be loaded. Each kind of NPC
 * says how it is made again ({@link #kind}). 1.3.3: her home is where she was made (her origin, {@link UniqueBody}): she comes back there
 * a day after she dies, and, left away from it a day with no one about, she goes back (herself if she is ticking; if her body lies in
 * unloaded ground, a new one is made at home and the old sent away when it loads). She never takes fall damage.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class UniqueNpcs {
    static final String NAME = "hexkagune_unique_npcs";
    /** How each one is made again, by key. */
    private static final Map<String, Supplier<? extends EntityType<? extends Mob>>> KINDS = new LinkedHashMap<>();
    /** 1.3.3: when each last ticked (this server's run only): one ticking recently goes home herself. */
    private static final Map<String, Long> TICKING = new java.util.HashMap<>();

    private UniqueNpcs() {
    }

    /** The one-of-a-kind NPC {@code key} is made of {@code type} when she comes back. */
    public static void kind(String key, Supplier<? extends EntityType<? extends Mob>> type) {
        KINDS.put(key, type);
    }

    /** 1.3.10: every one-of-a-kind NPC whose key starts with {@code prefix} ("child:") is made of {@code type}, then {@code made} with its key. */
    private static final Map<String, Supplier<? extends EntityType<? extends Mob>>> PREFIXES = new LinkedHashMap<>();
    private static final Map<String, java.util.function.BiConsumer<Mob, String>> MADE = new java.util.HashMap<>();

    public static void kinds(String prefix, Supplier<? extends EntityType<? extends Mob>> type, java.util.function.BiConsumer<Mob, String> made) {
        PREFIXES.put(prefix, type);
        MADE.put(prefix, made);
    }

    /** What {@code key} is made of (its own kind, or its prefix's), and what is done to a new body of it; null if nothing. */
    private static Supplier<? extends EntityType<? extends Mob>> kindOf(String key) {
        Supplier<? extends EntityType<? extends Mob>> kind = KINDS.get(key);
        if (kind != null) {
            return kind;
        }
        for (Map.Entry<String, Supplier<? extends EntityType<? extends Mob>>> prefix : PREFIXES.entrySet()) {
            if (key.startsWith(prefix.getKey())) {
                return prefix.getValue();
            }
        }
        return null;
    }

    private static void made(Mob body, String key) {
        for (Map.Entry<String, java.util.function.BiConsumer<Mob, String>> prefix : MADE.entrySet()) {
            if (key.startsWith(prefix.getKey())) {
                prefix.getValue().accept(body, key);
            }
        }
    }

    /**
     * 1.3.14: {@code body} is {@code key}'s from now on (a command made it where the old one could not be found): whoever it is with and
     * everything the world remembers of it stay; the old body, if it ever loads again, is sent away.
     */
    public static void adopt(ServerLevel level, String key, Mob body, BlockPos home) {
        UniqueNpc npc = get(level, key);
        long now = level.m_46467_();
        npc.body = body.m_20148_();
        npc.alive = true;
        npc.seen = now;
        npc.diedAt = Long.MIN_VALUE;
        npc.away = false;
        npc.company = now;
        npc.homeX = home.m_123341_();
        npc.homeY = home.m_123342_();
        npc.homeZ = home.m_123343_();
        npc.homed = true;
        npc.originX = home.m_123341_();
        npc.originY = home.m_123342_();
        npc.originZ = home.m_123343_();
        npc.origined = true;
        TICKING.put(key, now);
        changed(level);
    }

    /** 1.3.10: {@code key} is no more (forgotten by the world: nothing of it comes back). */
    public static void forget(ServerLevel level, String key) {
        if (data(level).all.remove(key) != null) {
            changed(level);
        }
    }

    static final class Data extends SavedData {
        final Map<String, UniqueNpc> all;

        Data(Map<String, UniqueNpc> all) {
            this.all = all;
        }

        static Data load(CompoundTag tag) {
            return new Data(UniqueNpc.loadAll(tag.m_128461_("npcs")));
        }

        @Override
        public CompoundTag m_7176_(CompoundTag tag) {
            tag.m_128359_("npcs", UniqueNpc.saveAll(this.all));
            return tag;
        }
    }

    /** The overworld of {@code level}'s server (where the registry is kept), or {@code level} itself if none is found. */
    static ServerLevel overworld(ServerLevel level) {
        MinecraftServer server = level.m_7654_();
        if (server != null) {
            for (ServerLevel each : server.m_129785_()) {
                if (each.m_46472_() == Level.f_46428_) {
                    return each;
                }
            }
        }
        return level;
    }

    private static Data data(ServerLevel level) {
        return overworld(level).m_8895_().m_164861_(Data::load, () -> new Data(new LinkedHashMap<>()), NAME);
    }

    /** The NPC {@code key} as the world remembers her (made, blank, the first time she is asked after). */
    public static UniqueNpc get(ServerLevel level, String key) {
        return data(level).all.computeIfAbsent(key, ignored -> new UniqueNpc());
    }

    /** What was changed of her is saved with the world. */
    public static void changed(ServerLevel level) {
        data(level).m_77762_();
    }

    /** Whether {@code body} may live as {@code key} ({@link UniqueNpc#claim}); her home is remembered as she ticks. */
    public static boolean claim(ServerLevel level, String key, UUID body, BlockPos home) {
        UniqueNpc npc = get(level, key);
        long now = level.m_46467_();
        TICKING.put(key, now);
        boolean wasHers = body.equals(npc.body) && npc.alive;
        long seenBefore = npc.seen;
        if (!npc.claim(body, now)) {
            return false;
        }
        boolean moved = home != null && (!npc.homed || npc.homeX != home.m_123341_() || npc.homeY != home.m_123342_() || npc.homeZ != home.m_123343_());
        if (moved) {
            npc.homeX = home.m_123341_();
            npc.homeY = home.m_123342_();
            npc.homeZ = home.m_123343_();
            npc.homed = true;
        }
        if (!wasHers || moved || seenBefore == Long.MIN_VALUE || now - seenBefore >= 1200L) {
            changed(level);
        } else {
            npc.seen = seenBefore;
        }
        return true;
    }

    /**
     * 1.3.3: how she is now: where she belongs (kept the first time it is known), whether she is away from it, and when she last had
     * company. Saved when it matters (not every time she has company).
     */
    public static void report(ServerLevel level, String key, BlockPos origin, boolean away, long company) {
        UniqueNpc npc = get(level, key);
        boolean changed = false;
        if (!npc.origined && origin != null) {
            npc.originX = origin.m_123341_();
            npc.originY = origin.m_123342_();
            npc.originZ = origin.m_123343_();
            npc.origined = true;
            changed = true;
        }
        if (npc.away != away) {
            npc.away = away;
            changed = true;
        }
        if (company != Long.MIN_VALUE && (npc.company == Long.MIN_VALUE || company - npc.company >= 1200L)) {
            npc.company = company;
            changed = true;
        }
        if (changed) {
            changed(level);
        }
    }

    /** {@code key}'s body died. */
    public static void died(ServerLevel level, String key, UUID body) {
        UniqueNpc npc = get(level, key);
        npc.died(body, level.m_46467_());
        changed(level);
    }

    /** {@code player} and {@code key} now stand at {@code stage} (none: she is free again). */
    public static void pair(ServerLevel level, String key, ServerPlayer player, Romance.Stage stage) {
        get(level, key).pair(player == null ? null : player.m_20148_(), player == null ? "" : player.m_36316_().getName(), stage);
        changed(level);
    }

    /** {@code player}'s stage with {@code key}. */
    public static Romance.Stage stage(ServerLevel level, String key, ServerPlayer player) {
        return get(level, key).stageOf(player.m_20148_());
    }

    /** The key of the NPC {@code player} is married to, other than {@code except}, or null. */
    public static String spouseOf(ServerLevel level, ServerPlayer player, String except) {
        for (Map.Entry<String, UniqueNpc> entry : data(level).all.entrySet()) {
            if (!entry.getKey().equals(except) && entry.getValue().stageOf(player.m_20148_()) == Romance.Stage.MARRIED) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Every NPC {@code player} is together with (dating or married), other than {@code except}. */
    public static Map<String, Romance.Stage> partnersOf(ServerLevel level, ServerPlayer player, String except) {
        Map<String, Romance.Stage> out = new LinkedHashMap<>();
        for (Map.Entry<String, UniqueNpc> entry : data(level).all.entrySet()) {
            Romance.Stage stage = entry.getValue().stageOf(player.m_20148_());
            if (!entry.getKey().equals(except) && stage.together()) {
                out.put(entry.getKey(), stage);
            }
        }
        return out;
    }

    /** Every so often: any of them dead long enough comes back to her home, if a player is near it (so it is loaded). */
    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.m_46472_() != Level.f_46428_
            || level.m_46467_() % 200L != 0L) {
            return;
        }
        long now = level.m_46467_();
        for (Map.Entry<String, UniqueNpc> entry : data(level).all.entrySet()) {
            UniqueNpc npc = entry.getValue();
            Supplier<? extends EntityType<? extends Mob>> kind = kindOf(entry.getKey());
            Long ticked = TICKING.get(entry.getKey());
            // Dead long enough; or left away a day and her body not about to go home itself (it lies in unloaded ground).
            boolean recall = npc.dueHome(now) && (ticked == null || now - ticked > 400L);
            if (kind == null || !npc.dueBack(now) && !recall) {
                continue;
            }
            int[] spawn = npc.spawn();
            BlockPos home = new BlockPos(spawn[0], spawn[1], spawn[2]);
            if (!level.m_46805_(home) || !someoneNear(level, home)) {
                continue;
            }
            Mob body = kind.get().m_20615_(level);
            if (body == null) {
                continue;
            }
            body.m_7678_(spawn[0] + 0.5, spawn[1] - 1.0, spawn[2] + 0.5, 0.0F, 0.0F);
            made(body, entry.getKey());
            npc.body = body.m_20148_();
            npc.alive = true;
            npc.seen = now;
            npc.diedAt = Long.MIN_VALUE;
            npc.away = false;
            npc.company = now;
            npc.homeX = spawn[0];
            npc.homeY = spawn[1];
            npc.homeZ = spawn[2];
            npc.homed = true;
            level.m_7967_(body);
            TICKING.put(entry.getKey(), now);
            changed(level);
        }
    }

    /** 1.3.3: a one-of-a-kind NPC never takes fall damage. */
    @SubscribeEvent
    public static void attacked(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (event.getEntity() instanceof UniqueBody && event.getSource() != null && "fall".equals(event.getSource().m_19385_())) {
            event.setCanceled(true);
        }
    }

    private static boolean someoneNear(ServerLevel level, BlockPos home) {
        for (ServerPlayer player : level.m_6907_()) {
            double dx = player.m_20185_() - home.m_123341_();
            double dz = player.m_20189_() - home.m_123343_();
            if (dx * dx + dz * dz <= 96.0 * 96.0) {
                return true;
            }
        }
        return false;
    }
}
