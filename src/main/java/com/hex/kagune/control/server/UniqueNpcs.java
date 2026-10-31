package com.hex.kagune.control.server;

import com.hex.kagune.control.npc.Romance;
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
 * says how it is made again ({@link #kind}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class UniqueNpcs {
    static final String NAME = "hexkagune_unique_npcs";
    /** How each one is made again, by key. */
    private static final Map<String, Supplier<? extends EntityType<? extends Mob>>> KINDS = new LinkedHashMap<>();

    private UniqueNpcs() {
    }

    /** The one-of-a-kind NPC {@code key} is made of {@code type} when she comes back. */
    public static void kind(String key, Supplier<? extends EntityType<? extends Mob>> type) {
        KINDS.put(key, type);
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
            Supplier<? extends EntityType<? extends Mob>> kind = KINDS.get(entry.getKey());
            if (kind == null || !npc.dueBack(now)) {
                continue;
            }
            BlockPos home = new BlockPos(npc.homeX, npc.homeY, npc.homeZ);
            if (!level.m_46805_(home) || !someoneNear(level, home)) {
                continue;
            }
            Mob body = kind.get().m_20615_(level);
            if (body == null) {
                continue;
            }
            body.m_7678_(npc.homeX + 0.5, npc.homeY - 1.0, npc.homeZ + 0.5, 0.0F, 0.0F);
            npc.body = body.m_20148_();
            npc.alive = true;
            npc.seen = now;
            npc.diedAt = Long.MIN_VALUE;
            level.m_7967_(body);
            changed(level);
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
