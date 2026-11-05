package com.hex.kagune.control.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 2.5.0 (the owner's final sweep: "fully playable, fully server compatible and single player alike"): the moments the mod keeps in
 * memory while a world runs (cooldowns, "until", "last time": the world's game time, keyed by a player, an NPC or a companion's name)
 * belong to that world alone. In single player the game goes on from one world to the next in the same session, and the next world's
 * clock is its own: a moment kept from the last one could lie hours in its future (an ability on cooldown for hours, a companion never
 * called home, nothing ever coming up to talk). Every such map is made here ({@link #map}) and all of them are forgotten once a world has
 * stopped and saved; and the moves under way are let go of.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class WorldTimes {
    private WorldTimes() {
    }

    private static final List<Map<?, ?>> ALL = new CopyOnWriteArrayList<>();

    /** A new map of game times, forgotten when the world stops. */
    public static <K> Map<K, Long> map() {
        Map<K, Long> out = new HashMap<>();
        ALL.add(out);
        return out;
    }

    /** A new map of what's under way in a world (a move held, a crack begun: timed by its clock), forgotten when the world stops. */
    public static <K, V> Map<K, V> kept() {
        Map<K, V> out = new HashMap<>();
        ALL.add(out);
        return out;
    }

    /** How many maps of game times there are (made so far). */
    static int count() {
        return ALL.size();
    }

    /** Everything kept of the world that stopped, forgotten. */
    public static void forget() {
        for (Map<?, ?> kept : ALL) {
            kept.clear();
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        forget();
    }
}
