package com.hex.kagune.control.server;

import com.hex.kagune.control.KaguneVoices;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

/**
 * 4.1.93 (the owner: "do for both of the other cognates completely. Replace all sounds"): every sound the original controller plays
 * ({@code KaguneServerController.playAt}, which {@code KagunePatch} sends here; its own moves and the mod's call it) is played as the
 * player's kagune plays it ({@link KaguneVoices}): a Rinkaku's, a Bikaku's and an Ukaku's are the game's own; any other path, and any other
 * sound, as it was ({@code playAtLegacy}).
 */
public final class KaguneVoice {
    private static Map<Object, String> events;

    private KaguneVoice() {
    }

    /** The original's {@code playAt(player, at, sound, volume, pitch)}, as the player's kagune plays it. */
    public static void playAt(ServerPlayer player, Vec3 at, RegistryObject<SoundEvent> sound, float volume, float pitch) {
        String id = player == null ? null : KaguneVoices.of(PathServer.path(player), eventName(sound));
        if (id == null) {
            KaguneServerController.playAtLegacy(player, at, sound, volume, pitch);
            return;
        }
        UkakuFire.sound(player.m_284548_(), at, id, volume, pitch);
    }

    /** A sound named with its namespace ("minecraft:..."), as an event to play. */
    public static SoundEvent event(String id) {
        int colon = id.indexOf(':');
        return SoundEvent.m_262824_(net.minecraft.resources.ResourceLocation.m_214293_(id.substring(0, colon), id.substring(colon + 1)));
    }

    /** The name of the original's event {@code sound} is ({@link KaguneVoices#EVENTS}), by which of its fields it is; null for any other. */
    static synchronized String eventName(Object sound) {
        if (events == null) {
            Map<Object, String> map = new IdentityHashMap<>();
            map.put(HexKaguneSounds.MANIFEST_RIP, "manifest_rip");
            map.put(HexKaguneSounds.KAGUNE_RESHAPE, "kagune_reshape");
            map.put(HexKaguneSounds.KAGUNE_DISMISS, "kagune_dismiss");
            map.put(HexKaguneSounds.KAGUNE_WHIP, "kagune_whip");
            map.put(HexKaguneSounds.FLESH_IMPACT, "flesh_impact");
            map.put(HexKaguneSounds.BONE_CRACK, "bone_crack");
            map.put(HexKaguneSounds.BONE_CRUSH, "bone_crush");
            map.put(HexKaguneSounds.KAGUNE_GRAB, "kagune_grab");
            map.put(HexKaguneSounds.KAGUNE_STRAIN, "kagune_strain");
            map.put(HexKaguneSounds.KAGUNE_SLAM, "kagune_slam");
            map.put(HexKaguneSounds.KAGUNE_GUARD, "kagune_guard");
            map.put(HexKaguneSounds.KAGUNE_ANCHOR, "kagune_anchor");
            map.put(HexKaguneSounds.CENTIPEDE_CLICK, "centipede_click");
            map.put(HexKaguneSounds.SHARD_LAUNCH, "shard_launch");
            map.put(HexKaguneSounds.KAGUNE_IDLE, "kagune_idle");
            map.put(HexKaguneSounds.CRUSH_KILL, "crush_kill");
            events = map;
        }
        return events.get(sound);
    }
}
