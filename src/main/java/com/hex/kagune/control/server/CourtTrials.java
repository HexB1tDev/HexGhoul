package com.hex.kagune.control.server;

import com.hex.kagune.control.Safety;
import com.hex.kagune.control.npc.CourtVoice;
import com.hex.kagune.control.npc.Courtship;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.GangPlayer;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.npc.Persona;
import com.hex.kagune.control.quest.QuestLog;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 4.1.80 (the plan: "swap the repeated tasks so each companion's five differ"): the courtship's chapters done with the hands, in the
 * world: Roma's show (fireworks set off near her at night, a right click on a block with a rocket), Banjou's garden (a flower or a sapling
 * planted at the den while he watches: the click remembered, the block looked at the tick after, so only what really went into the
 * ground counts), and his boys' supper (a bowl of the cookbook's handed to one of his Gas Masks, each once). Ayato's spar is
 * {@code JoinServer}'s, Touka's homework {@code EtoServer}'s, the rest {@code CourtshipServer}'s.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class CourtTrials {
    /** On one of Banjou's boys: fed by this player ("HexCourtFed" and their id). */
    static final String FED = "HexCourtFed";
    /** A click with something to plant, waiting to be looked at: where, what, when. */
    record Planting(BlockPos at, String what, long when, String[] before) {
    }

    /** Each player's last click with something to plant (looked at the tick after). */
    static final Map<UUID, Planting> PLANTING = new ConcurrentHashMap<>();

    private CourtTrials() {
    }

    // ---------------------------------------------------------------- 4.1.81: dates at real places

    /** A date on its way: whose, its scene's key, where and when, since when. */
    record Dating(String key, String scene, com.hex.kagune.control.npc.DateSpot.Spot spot, long began) {
    }

    /** Each player's date on its way (not saved: logged out, it is off). */
    static final Map<UUID, Dating> DATING = new ConcurrentHashMap<>();

    /** {@code npc}'s date {@code scene} begun for {@code player}: had at {@code spot}; she follows them there. */
    static void dateOn(ServerPlayer player, EtoNpc npc, com.hex.kagune.control.npc.scene.Scene scene, com.hex.kagune.control.npc.DateSpot.Spot spot,
                       long now) {
        DATING.put(player.m_20148_(), new Dating(npc.persona().key(), scene.key(), spot, now));
        npc.follow(player.m_20148_());
    }

    /** True if the world has {@code spot}'s place (a companion's home known; a kind of place is anywhere); a date with no place, always. */
    static boolean exists(ServerPlayer player, com.hex.kagune.control.npc.DateSpot.Spot spot) {
        return spot == null || spot.place().home == null || GangServer.home(player.m_284548_(), spot.place().home) != null;
    }

    /** True if {@code player} is at {@code spot} now (its place, at its time). */
    static boolean at(ServerPlayer player, com.hex.kagune.control.npc.DateSpot.Spot spot) {
        return spot.when().at(Math.floorMod(player.m_9236_().m_46468_(), 24000L)) && place(player, spot.place());
    }

    /** True if {@code player} is at {@code place}. */
    static boolean place(ServerPlayer player, com.hex.kagune.control.npc.DateSpot.Place place) {
        ServerLevel level = player.m_284548_();
        int x = (int)Math.floor(player.m_20185_());
        int z = (int)Math.floor(player.m_20189_());
        return switch (place) {
            case ROOF -> CourtshipServer.onRoof(player);
            case HIGH -> com.hex.kagune.control.npc.DateSpot.high(player.m_20186_(), top(level, x, z));
            case WATER -> water(level, player);
            case FIELD -> player.m_20186_() >= top(level, x, z) - 0.5 && com.hex.kagune.control.npc.DateSpot.field(block(level,
                BlockPos.m_274561_(player.m_20185_(), player.m_20186_() - 0.5, player.m_20189_())));
            case RAIN -> level.m_46758_(BlockPos.m_274561_(player.m_20185_(), player.m_20186_() + 1.0, player.m_20189_()));
            case VILLAGE -> CourtshipServer.villagers(player);
            default -> CourtshipServer.near(player, GangServer.home(level, place.home), com.hex.kagune.control.npc.DateSpot.AT_HOME);
        };
    }

    /** The first open block over the column at x, z (the world's height map). */
    static int top(ServerLevel level, int x, int z) {
        return level.m_6924_(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    /** The block at {@code at}, by name ("minecraft:grass_block"). */
    static String block(ServerLevel level, BlockPos at) {
        return com.hex.kagune.control.family.Chores.id(String.valueOf(level.m_8055_(at).m_60734_()));
    }

    /** True if water is within DateSpot.WATER_NEAR of {@code player} (round them, from under their feet to their head). */
    static boolean water(ServerLevel level, ServerPlayer player) {
        int r = com.hex.kagune.control.npc.DateSpot.WATER_NEAR;
        int x = (int)Math.floor(player.m_20185_());
        int y = (int)Math.floor(player.m_20186_());
        int z = (int)Math.floor(player.m_20189_());
        for (int dy = -2; dy <= 1; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if ("minecraft:water".equals(block(level, new BlockPos(x + dx, y + dy, z + dz)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * A second of a date on its way: off after DateSpot.WAITS; there with her along (following them, near), its scene brought to them
     * (she comes up with it) and the date had (EtoServer.dated); else, now and then, where to go in the action bar.
     */
    static void dating(ServerPlayer player, Dating d) {
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        Persona persona = Persona.of(d.key());
        if (now - d.began() > com.hex.kagune.control.npc.DateSpot.WAITS || now < d.began()) {
            DATING.remove(player.m_20148_());
            player.m_5661_(Component.m_237113_(persona.colour() + persona.name() + ": §f" + CourtVoice.stood(d.key(), (int)(now & 0xFF))), false);
            return;
        }
        EtoNpc npc = CourtshipServer.nearby(player, d.key(), com.hex.kagune.control.npc.DateSpot.ALONG);
        boolean along = npc != null && player.m_20148_().equals(npc.following());
        if (along && at(player, d.spot())) {
            com.hex.kagune.control.npc.scene.Scene scene = SceneServer.scene(d.scene());
            if (scene == null) {
                DATING.remove(player.m_20148_());
            } else if (SceneServer.approachMoment(npc, player, scene, now)) {
                DATING.remove(player.m_20148_());
                EtoServer.dated(player, npc);
            }
            return;
        }
        if (player.f_19797_ % 60 == 0) {
            BlockPos home = d.spot().place().home == null ? null : GangServer.home(level, d.spot().place().home);
            String way = home == null ? "" : ", " + com.hex.kagune.control.npc.JoinTrial.way(home.m_123341_() + 0.5 - player.m_20185_(),
                home.m_123343_() + 0.5 - player.m_20189_());
            player.m_5661_(Component.m_237113_(persona.colour() + "❤ A date with " + persona.name() + ": §f" + d.spot().words() + way
                + (along ? "" : " §7(" + persona.she() + "'s not with you: ask " + persona.her() + " to follow)")), true);
        }
    }

    @SubscribeEvent
    public static void loggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        DATING.remove(event.getEntity().m_20148_());
        PLANTING.remove(event.getEntity().m_20148_());
    }

    @SubscribeEvent
    public static void clicked(PlayerInteractEvent.RightClickBlock event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack held = event.getHand() == InteractionHand.MAIN_HAND ? player.m_21205_() : player.m_21206_();
            if (held.m_41619_()) {
                return;
            }
            String id = String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.m_41720_()));
            if (FIREWORK.equals(id)) {
                firework(player);
            } else if (Courtship.plant(id)) {
                planting(player, event.getPos(), id);
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "CourtTrials.clicked");
        }
    }

    static final String FIREWORK = "minecraft:firework_rocket";

    /** A rocket set off: Roma's show, if it is her chapter on, at night, with her near enough to watch. */
    static void firework(ServerPlayer player) {
        if (!CourtshipServer.on(player, "roma", Courtship.Task.SHOW)) {
            return;
        }
        if (!EtoRules.night(player.m_9236_().m_46468_())) {
            player.m_5661_(Component.m_237113_(Persona.of("roma").colour() + "Roma: §f\"Fireworks in DAYLIGHT? Where's the drama? Tonight!\""), true);
            return;
        }
        EtoNpc roma = CourtshipServer.nearby(player, "roma", Courtship.SHOW_NEAR);
        if (roma == null) {
            player.m_5661_(Component.m_237113_(Persona.of("roma").colour() + "Roma: §7(she isn't here to see it: by her ring, where she can watch)"), true);
            return;
        }
        QuestLog.Bond after = CourtshipServer.count(player, "roma", Courtship.Task.SHOW);
        if (after != null) {
            int n = EtoRules.count(after);
            Courtship.Chapter chapter = Courtship.chapter("roma", after);
            int need = chapter == null ? n : chapter.need();
            player.m_5661_(Component.m_237113_(Persona.of("roma").colour() + "Roma: §f" + CourtVoice.firework(n, need, roma.m_217043_().m_188503_(100))),
                false);
            EtoBrain.hearts(player.m_284548_(), roma, player, 2);
        }
    }

    /** A click with something to plant at the den, Banjou watching: remembered, looked at the tick after ({@link #tick}). */
    static void planting(ServerPlayer player, BlockPos at, String what) {
        if (!CourtshipServer.on(player, "banjou", Courtship.Task.GARDEN)) {
            return;
        }
        ServerLevel level = player.m_284548_();
        if (!CourtshipServer.near(player, GangServer.home(level, "banjou"), Courtship.GARDEN_NEAR)) {
            return;
        }
        if (CourtshipServer.nearby(player, "banjou", Courtship.WATCHING) == null) {
            player.m_5661_(Component.m_237113_(Persona.of("banjou").colour() + "Banjou: §7(he isn't here to see it: plant it with him by you)"), true);
            return;
        }
        PLANTING.put(player.m_20148_(), new Planting(at, what, level.m_46467_(), around(level, at)));
    }

    /** The blocks at {@code at} and its six sides, by name ("minecraft:poppy"). */
    static String[] around(ServerLevel level, BlockPos at) {
        BlockPos[] sides = sides(at);
        String[] out = new String[sides.length];
        for (int i = 0; i < sides.length; i++) {
            out[i] = com.hex.kagune.control.family.Chores.id(String.valueOf(level.m_8055_(sides[i]).m_60734_()));
        }
        return out;
    }

    static BlockPos[] sides(BlockPos at) {
        int x = at.m_123341_();
        int y = at.m_123342_();
        int z = at.m_123343_();
        return new BlockPos[] {at, new BlockPos(x, y + 1, z), new BlockPos(x, y - 1, z), new BlockPos(x, y, z - 1), new BlockPos(x, y, z + 1),
            new BlockPos(x + 1, y, z), new BlockPos(x - 1, y, z)};
    }

    /** The tick after a click: if what was clicked with is now in the ground beside it (where it wasn't), Banjou's garden has one more. */
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        try {
            if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
                return;
            }
            Dating d = DATING.get(player.m_20148_());
            if (d != null && player.f_19797_ % 20 == 0) {
                dating(player, d); // 4.1.81
            }
            Planting p = PLANTING.get(player.m_20148_());
            if (p == null) {
                return;
            }
            ServerLevel level = player.m_284548_();
            long now = level.m_46467_();
            if (now <= p.when()) {
                return;
            }
            PLANTING.remove(player.m_20148_());
            if (now - p.when() > 20L || !Courtship.planted(p.what(), p.before(), around(level, p.at()))) {
                return;
            }
            QuestLog.Bond after = CourtshipServer.count(player, "banjou", Courtship.Task.GARDEN);
            EtoNpc banjou = CourtshipServer.nearby(player, "banjou", Courtship.WATCHING * 2.0);
            if (after != null && banjou != null) {
                int n = EtoRules.count(after);
                Courtship.Chapter chapter = Courtship.chapter("banjou", after);
                player.m_5661_(Component.m_237113_(Persona.of("banjou").colour() + "Banjou: §f" + CourtVoice.planted(n, chapter == null ? n : chapter.need(),
                    banjou.m_217043_().m_188503_(100))), false);
                level.m_8767_(ParticleTypes.f_123748_, p.at().m_123341_() + 0.5, p.at().m_123342_() + 1.2, p.at().m_123343_() + 0.5, 5, 0.3, 0.2, 0.3, 0.0);
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "CourtTrials.tick");
        }
    }

    /**
     * A right click on {@code npc} by {@code player}: if it is one of Banjou's boys and his supper is on, a bowl of the cookbook's in the
     * hand is handed over (taken, eaten, thanked; each boy once). True if the click was the supper's (the talk stays shut).
     */
    static boolean serve(ServerPlayer player, GhoulNpc npc) {
        if (npc.identity() == null || GangPlayer.of(npc.identity()) != GangPlayer.GAS_MASKS || !CourtshipServer.on(player, "banjou", Courtship.Task.SERVE)) {
            return false;
        }
        ItemStack held = player.m_21205_();
        String id = held.m_41619_() ? "" : String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.m_41720_()));
        if (!Courtship.SUPPER.contains(id)) {
            return false; // anything else: the talk, as ever
        }
        String fed = FED + player.m_20148_();
        int seed = npc.m_217043_().m_188503_(100);
        if (npc.getPersistentData().m_128471_(fed)) {
            player.m_5661_(Component.m_237113_("§a" + npc.identity().displayName() + ": §f" + CourtVoice.SERVED_TWICE), true);
            return true;
        }
        QuestLog.Bond after = CourtshipServer.count(player, "banjou", Courtship.Task.SERVE);
        if (after == null) {
            return false;
        }
        npc.getPersistentData().m_128379_(fed, true);
        if (!player.m_7500_()) {
            held.m_41774_(1);
        }
        ServerLevel level = player.m_284548_();
        UkakuFire.sound(level, npc.m_20182_(), "entity.generic.eat", 0.8F, 1.0F);
        level.m_8767_(ParticleTypes.f_123748_, npc.m_20185_(), npc.m_20186_() + 1.8, npc.m_20189_(), 6, 0.3, 0.2, 0.3, 0.0);
        Courtship.Chapter chapter = Courtship.chapter("banjou", after);
        int n = EtoRules.count(after);
        player.m_5661_(Component.m_237113_("§a" + npc.identity().displayName() + ": §f" + CourtVoice.served(n, chapter == null ? n : chapter.need(), seed)),
            false);
        return true;
    }
}
