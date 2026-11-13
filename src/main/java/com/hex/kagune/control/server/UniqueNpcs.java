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
    private static final Map<String, Long> TICKING = WorldTimes.map();
    /** 4.0.5: since when a player has been at each one's home (while one is). */
    private static final Map<String, Long> WATCHED = WorldTimes.map();

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

    /** 4.1.82: true if ({@code x}, {@code z}) is at official {@code key}'s own place (npc.HomePlace) as this world built it. */
    public static boolean atOwnPlace(String key, double x, double z) {
        String place = com.hex.kagune.control.npc.HomePlace.of(key);
        int[] chunk = place == null ? null : com.hex.kagune.control.PlaceClaims.claimed(place);
        return chunk != null && com.hex.kagune.control.npc.HomePlace.within(place, x - ((chunk[0] << 4) + 8), z - ((chunk[1] << 4) + 8));
    }

    /**
     * 4.1.82 (the owner: "the clown girl is not in the circus tent"): true if {@code body}, a body of {@code key}'s at her own place, takes
     * her slot from the world's one, whose home is not there (npc.HomePlace#takesOver: never a married one's).
     */
    public static boolean takesOver(ServerLevel level, String key, Mob body) {
        UniqueNpc slot = get(level, key);
        int[] home = slot.spawn();
        boolean slotHome = (slot.origined || slot.homed) && atOwnPlace(key, home[0] + 0.5, home[2] + 0.5);
        return com.hex.kagune.control.npc.HomePlace.takesOver(atOwnPlace(key, body.m_20185_(), body.m_20189_()), slotHome,
            slot.stage == com.hex.kagune.control.npc.Romance.Stage.MARRIED);
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

    /** 1.3.21: kinds that die for good (by key prefix): never brought back; {@code gone} told (once) as each is forgotten. */
    private static final Map<String, java.util.function.BiConsumer<ServerLevel, String>> MORTAL = new LinkedHashMap<>();

    public static void mortal(String prefix, java.util.function.BiConsumer<ServerLevel, String> gone) {
        MORTAL.put(prefix, gone);
    }

    private static java.util.function.BiConsumer<ServerLevel, String> mortalOf(String key) {
        for (Map.Entry<String, java.util.function.BiConsumer<ServerLevel, String>> e : MORTAL.entrySet()) {
            if (key.startsWith(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /** 1.3.21: {@code key} died for good (a mortal kind): forgotten by the world, and whatever keeps the rest of it told. */
    public static void perished(ServerLevel level, String key) {
        java.util.function.BiConsumer<ServerLevel, String> gone = mortalOf(key);
        forget(level, key);
        if (gone != null) {
            gone.accept(level, key);
        }
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

    /** 4.1.78: the NPC {@code key} as the world remembers her, or null if it never has (asking makes nothing: {@link #get} would). */
    public static UniqueNpc known(ServerLevel level, String key) {
        return data(level).all.get(key);
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

    /** 3.0.4: someone has met {@code key} (talked to her): from now on she stays the one they met. */
    public static void met(ServerLevel level, String key) {
        UniqueNpc npc = get(level, key);
        if (!npc.met) {
            npc.met = true;
            changed(level);
        }
    }

    /** Claims a permanent world-wide slot until its holder dies. */
    public static boolean claimExclusive(ServerLevel level, String key, UUID body) {
        UniqueNpc npc = get(level, key);
        if (npc.alive && body.equals(npc.body)) {
            return true;
        }
        if (!npc.claimExclusive(body, level.m_46467_())) {
            return false;
        }
        changed(level);
        return true;
    }

    /**
     * 1.3.3: how she is now: where she belongs, whether she is away from it, and when she last had company. Saved when it matters (not
     * every time she has company). 4.0.5: where she belongs as her body has it, kept up with (it was kept the first time only, so one
     * married with a home of their own came back to her old one after dying).
     */
    public static void report(ServerLevel level, String key, BlockPos origin, boolean away, long company) {
        UniqueNpc npc = get(level, key);
        boolean changed = false;
        if (origin != null && (!npc.origined || npc.originX != origin.m_123341_() || npc.originY != origin.m_123342_()
            || npc.originZ != origin.m_123343_())) {
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

    /**
     * Every so often: any of them dead long enough comes back to her home, if a player is near it (so it is loaded). 4.0.5: and any of the
     * official ones (Eto, Touka, Ken, Ayato, Noro...) who should be at her home and is not, with a player there half a minute, is made there
     * again ({@link UniqueNpc#missing}); her lost body, if it ever loads, is sent away.
     */
    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.m_46472_() != Level.f_46428_
            || level.m_46467_() % 200L != 0L) {
            return;
        }
        long now = level.m_46467_();
        // 1.3.21: the mortal ones dead (before this rule, too) are forgotten for good, never brought back.
        java.util.List<String> perished = new java.util.ArrayList<>();
        for (Map.Entry<String, UniqueNpc> entry : data(level).all.entrySet()) {
            if (!entry.getValue().alive && entry.getValue().diedAt != Long.MIN_VALUE && mortalOf(entry.getKey()) != null) {
                perished.add(entry.getKey());
            }
        }
        for (String key : perished) {
            perished(level, key);
        }
        for (Map.Entry<String, UniqueNpc> entry : data(level).all.entrySet()) {
            UniqueNpc npc = entry.getValue();
            Supplier<? extends EntityType<? extends Mob>> kind = kindOf(entry.getKey());
            Long ticked = TICKING.get(entry.getKey());
            // Dead long enough; or left away a day and her body not about to go home itself (it lies in unloaded ground).
            boolean recall = npc.dueHome(now) && (ticked == null || now - ticked > 400L);
            int[] spawn = npc.spawn();
            BlockPos home = new BlockPos(spawn[0], spawn[1], spawn[2]);
            boolean missing = false;
            if (KINDS.containsKey(entry.getKey()) && npc.alive && !npc.away && (npc.origined || npc.homed)) {
                if (level.m_46805_(home) && someoneNear(level, home, 24.0)) {
                    long since = WATCHED.computeIfAbsent(entry.getKey(), any -> now);
                    missing = npc.missing(now, since, ticked == null ? Long.MIN_VALUE : ticked);
                } else {
                    WATCHED.remove(entry.getKey());
                }
            }
            if (kind == null || !npc.dueBack(now) && !recall && !missing) {
                continue;
            }
            if (!level.m_46805_(home) || !someoneNear(level, home, 96.0)) {
                continue;
            }
            WATCHED.remove(entry.getKey());
            Mob body = kind.get().m_20615_(level);
            if (body == null) {
                continue;
            }
            body.m_7678_(spawn[0] + 0.5, spawn[1] - 1.0, spawn[2] + 0.5, 0.0F, 0.0F);
            // 4.1.73 (the owner: "Make sure NPCS dont spawn in the ground at any point, especially official NPCS."): on her floor there
            // (a slab's top, a carpet's), with room for all of her, never at the whole block her home was kept as.
            body.m_7678_(spawn[0] + 0.5, NpcFooting.feet(level, body, spawn[0] + 0.5, spawn[1] - 1.0, spawn[2] + 0.5), spawn[2] + 0.5, 0.0F, 0.0F);
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
        keepPlaces(level, now); // 4.1.82
    }

    /** 4.1.82: since when a player has been in each place with none of its official NPC in it (by her key). */
    private static final Map<String, Long> PLACE_WATCHED = WorldTimes.map();
    /** 4.1.83: each official's own spot in this world (ControlStructures#spot: {x, feet y, z, yaw}), once read (a place never moves). */
    private static final Map<String, int[]> SPOTS = new java.util.concurrent.ConcurrentHashMap<>();
    /** 4.1.83: the world those spots are of (they are read again in another). */
    private static volatile ServerLevel spotsOf;

    /** 4.1.83: official {@code key}'s own spot in this world ({x, feet y, z, yaw}), or null while her place isn't built or loaded. */
    static int[] spotOf(ServerLevel level, String key) {
        if (spotsOf != level) {
            SPOTS.clear();
            spotsOf = level;
        }
        int[] known = SPOTS.get(key);
        if (known == null) {
            known = com.hex.kagune.control.ControlStructures.spot(level, key);
            if (known != null) {
                SPOTS.put(key, known);
            }
        }
        return known;
    }

    /**
     * 4.1.82 (the owner: "the clown girl is not in the circus tent"), 4.1.83 for every official (the owner: "Gas mask guy is ALSO not
     * spawning in his room, or anywhere"; "and I'm 99% sure the general of the CCg isnt spawning either"): each official NPC (Eto, Touka,
     * Ken, Ayato, Noro, the General, Roma, Banjou) belongs at her own spot in her place (npc.HomePlace#spot, as this world built it:
     * ControlStructures#spot). When a player has been within HomePlace.KEEP_NEAR of it half a minute and none of her is in her place (her
     * body sent away as one met elsewhere held her, made elsewhere by `/kagune <name> here`, lost, or never made), unless she is out with
     * someone, left waiting somewhere, or married, she is made at her spot, facing the way the place turns her, and whatever body of hers is
     * elsewhere is sent away. One already in her place whose home isn't her spot (Banjou downstairs before his bedroom) is given her spot
     * as her home and walks there. Dead, she comes back to her spot.
     */
    static void keepPlaces(ServerLevel level, long now) {
        for (String key : KINDS.keySet()) {
            if (com.hex.kagune.control.npc.HomePlace.of(key) == null) {
                continue;
            }
            int[] spot = spotOf(level, key);
            if (spot == null || !someoneNear(level, new BlockPos(spot[0], spot[1], spot[2]), com.hex.kagune.control.npc.HomePlace.KEEP_NEAR)) {
                PLACE_WATCHED.remove(key);
                continue;
            }
            // Her home as a block: one over her feet (EtoNpc#settle), where she comes back to.
            BlockPos home = new BlockPos(spot[0], spot[1] + 1, spot[2]);
            com.hex.kagune.control.npc.EtoNpc live = null;
            for (com.hex.kagune.control.npc.EtoNpc one : EtoBrain.live()) {
                if (one.persona().key().equals(key) && one.m_6084_() && one.m_9236_() == level
                    && (live == null || atOwnPlace(key, one.m_20185_(), one.m_20189_()))) {
                    live = one; // the one in her place, if one is
                }
            }
            UniqueNpc slot = known(level, key);
            boolean married = slot != null && slot.stage == com.hex.kagune.control.npc.Romance.Stage.MARRIED;
            if (slot != null && !slot.alive && slot.diedAt != Long.MIN_VALUE) {
                // Dead: she comes back a day after, as ever (dueBack), but at her spot, unless married (her home is their home).
                int[] back = slot.spawn();
                if (!married && (back[0] != home.m_123341_() || back[1] != home.m_123342_() || back[2] != home.m_123343_())) {
                    slot.originX = home.m_123341_();
                    slot.originY = home.m_123342_();
                    slot.originZ = home.m_123343_();
                    slot.origined = true;
                    changed(level);
                }
                PLACE_WATCHED.remove(key);
                continue;
            }
            boolean inPlace = live != null && atOwnPlace(key, live.m_20185_(), live.m_20189_());
            boolean along = live != null && (live.following() != null || live.mind != null && live.mind.holding != null) || slot != null && slot.away;
            BlockPos was = live == null ? null : live.origin();
            if (inPlace && !along && !married && was != null
                && com.hex.kagune.control.npc.HomePlace.astray(was.m_123341_(), was.m_123342_(), was.m_123343_(), home.m_123341_(), home.m_123342_(), home.m_123343_())) {
                live.homeAt(home); // in her place, her home not her spot: it is from now on (she walks there)
                adopt(level, key, live, home);
            }
            long since = inPlace || along || married ? now : PLACE_WATCHED.computeIfAbsent(key, any -> now);
            if (inPlace || along || married) {
                PLACE_WATCHED.remove(key);
            }
            if (!com.hex.kagune.control.npc.HomePlace.remade(inPlace, along, married, now - since, UniqueNpc.MISSING)) {
                continue;
            }
            Supplier<? extends EntityType<? extends Mob>> kind = kindOf(key);
            Mob body = kind == null ? null : kind.get().m_20615_(level);
            if (body == null) {
                continue;
            }
            PLACE_WATCHED.remove(key);
            body.m_7678_(spot[0] + 0.5, NpcFooting.feet(level, body, spot[0] + 0.5, spot[1], spot[2] + 0.5), spot[2] + 0.5, spot[3], 0.0F);
            body.f_20885_ = spot[3];
            body.f_20883_ = spot[3];
            made(body, key);
            level.m_7967_(body);
            adopt(level, key, body, home);
            if (live != null) {
                live.m_146870_(); // the one elsewhere: she is home now
            }
        }
    }

    /** 1.3.3: a one-of-a-kind NPC never takes fall damage. */
    @SubscribeEvent
    public static void attacked(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (event.getEntity() instanceof UniqueBody && event.getSource() != null && "fall".equals(event.getSource().m_19385_())) {
            event.setCanceled(true);
        }
    }

    private static boolean someoneNear(ServerLevel level, BlockPos home, double within) {
        for (ServerPlayer player : level.m_6907_()) {
            double dx = player.m_20185_() - home.m_123341_();
            double dz = player.m_20189_() - home.m_123343_();
            if (dx * dx + dz * dz <= within * within && !player.m_5833_()) {
                return true;
            }
        }
        return false;
    }
}
