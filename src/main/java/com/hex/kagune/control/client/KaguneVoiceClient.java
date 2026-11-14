package com.hex.kagune.control.client;

import com.hex.kagune.control.BikakuSounds;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.RinkakuSounds;
import com.hex.kagune.control.ukaku.UkakuSounds;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * 4.1.93: the kagune's sounds the client plays itself, as the player's kagune plays them: the tendrils' writhing loop (the original's
 * {@code KaguneAmbientSounds}, which {@code KagunePatch} sends here), the kagune tearing out ({@link ManifestSounds}), a kakuja breaking
 * apart ({@code KakujaFx}). A Rinkaku's, a Bikaku's and an Ukaku's are the game's own; the other paths keep theirs.
 */
public final class KaguneVoiceClient {
    private KaguneVoiceClient() {
    }

    /** A sound named with its namespace ("minecraft:..."), as an event to play. */
    public static SoundEvent event(String id) {
        int colon = id.indexOf(':');
        return SoundEvent.m_262824_(ResourceLocation.m_214293_(id.substring(0, colon), id.substring(colon + 1)));
    }

    /** The tendrils' writhing loop for {@code player} (the original's own idle for the paths that keep it). */
    public static SoundEvent writhe(AbstractClientPlayer player) {
        KagunePath path = PathClient.get(player);
        if (path == KagunePath.RINKAKU) {
            return event(RinkakuSounds.WRITHE);
        }
        if (path == KagunePath.BIKAKU) {
            return event(BikakuSounds.WRITHE);
        }
        return com.hex.kagune.sound.HexKaguneSounds.KAGUNE_IDLE.get();
    }

    /** The kagune tearing out of {@code player} ({@code centipede}: a half kakuja's legs), or null: the pack's own (ManifestSounds). */
    public static SoundEvent tear(AbstractClientPlayer player, boolean centipede) {
        KagunePath path = PathClient.get(player);
        if (path == KagunePath.RINKAKU) {
            return event(centipede ? RinkakuSounds.CHITTER : RinkakuSounds.MANIFEST);
        }
        if (path == KagunePath.BIKAKU) {
            return event(BikakuSounds.RESHAPE);
        }
        return null;
    }

    /** A kakuja breaking apart on {@code player}, or null: the pack's glass (KakujaFx). */
    public static SoundEvent kakujaBreak(AbstractClientPlayer player) {
        KagunePath path = PathClient.get(player);
        return switch (path) {
            case RINKAKU -> event(RinkakuSounds.KAKUJA_BREAK);
            case BIKAKU -> event(BikakuSounds.KAKUJA_BREAK);
            case UKAKU -> event(UkakuSounds.GLASS);
            default -> null;
        };
    }
}
