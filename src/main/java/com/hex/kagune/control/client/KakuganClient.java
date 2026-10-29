package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.kakugan.KakuganEyes;
import com.hex.kagune.control.kakugan.KakuganModel;
import com.hex.kagune.control.kakugan.KakuganSkin;
import com.hex.kagune.control.network.S2CGhoulState;
import com.hex.kagune.control.network.S2CKakuganState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client side of the kakugan: every known player's eye placement (from the server) and how awake each player's
 * kakugan is. It wakes fully while the kagune is out and flares with bloodlust (a starving or frenzied ghoul's eyes turn
 * even with the kagune away), fading in quickly and out a little slower.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class KakuganClient {
    private static final float WAKE_PER_TICK = 0.16F;
    private static final float SLEEP_PER_TICK = 0.07F;

    private static final Map<UUID, KakuganEyes> EYES = new HashMap<>();
    private static final Map<UUID, KakuganEyes> PLACED = new HashMap<>();
    private static final Map<UUID, float[]> AWAKE = new HashMap<>(); // {previous tick, this tick}
    /** Players in ghoul mode (R: the kakugan open without the kagune). */
    private static final java.util.Set<UUID> GHOULS = new java.util.HashSet<>();

    private KakuganClient() {
    }

    public static void acceptGhoul(S2CGhoulState message) {
        if (message.ghoul()) {
            GHOULS.add(message.playerId());
        } else {
            GHOULS.remove(message.playerId());
        }
    }

    /** 1.0.18: true while {@code player} wears Peace (the final kakuja with his face bare); 1.0.19: and while it leaves him. */
    static boolean peace(AbstractClientPlayer player) {
        KingClient.View view = KingClient.get(player);
        return view != null && view.form() == com.hex.kagune.control.king.KingForm.PEACE && (view.on()
            || com.hex.kagune.control.king.KingTransform.going(view.form(), false, ControlClient.worldTime(0.0F) - view.since()));
    }

    /** True while a player is in ghoul mode. */
    public static boolean ghoul(AbstractClientPlayer player) {
        return player != null && GHOULS.contains(player.m_36316_().getId());
    }

    public static void accept(S2CKakuganState message) {
        EYES.put(message.playerId(), message.eyes());
        if (message.placed()) {
            PLACED.put(message.playerId(), message.eyes());
        } else {
            PLACED.remove(message.playerId());
        }
    }

    /**
     * Where a player's kakugan goes: where they placed it, or (3.0.0) where the eyes are on their skin if they never did
     * ({@link SkinReader}), or Steve's eyes.
     */
    public static KakuganEyes eyes(AbstractClientPlayer player) {
        if (player == null) {
            return KakuganEyes.DEFAULT;
        }
        KakuganEyes eyes = placedEyes(player);
        // 6.1.0: Pale is a natural one-eyed ghoul: only one eye ever turns. The one the player chose, or (left on both) her right,
        // as she is drawn.
        if (PathClient.get(player) == com.hex.kagune.control.KagunePath.GRAFT) {
            return eyes.withGhoul(KakuganEyes.RIGHT); // 7.0.0: Graft's one kakugan is always the right eye
        }
        return PathClient.get(player) == com.hex.kagune.control.KagunePath.PALE && !eyes.oneEyed() ? eyes.withGhoul(KakuganEyes.RIGHT) : eyes;
    }

    /** Where a player placed their kakugan (or where it was found on their skin), before any path's own rule. */
    private static KakuganEyes placedEyes(AbstractClientPlayer player) {
        UUID id = player.m_36316_().getId();
        KakuganEyes placed = PLACED.get(id);
        if (placed != null) {
            return placed;
        }
        KakuganEyes found = detected(player);
        return found != null ? found : EYES.getOrDefault(id, KakuganEyes.DEFAULT);
    }

    /** The eyes found on a player's skin, or null (not read yet, or none stand out). */
    public static KakuganEyes detected(AbstractClientPlayer player) {
        SkinReader.Skin skin = SkinReader.of(player);
        return skin == null ? null : skin.eyes();
    }

    /** The face cells a player's skin covers with its second layer ({@code KakuganSkin.covered}; 0 until read). */
    public static long covered(AbstractClientPlayer player) {
        SkinReader.Skin skin = SkinReader.of(player);
        return skin == null ? 0L : skin.covered();
    }

    /** 6.0.2: true once a player's skin has been read (and could be trusted): {@link #covered} says what it covers. */
    public static boolean skinRead(AbstractClientPlayer player) {
        return SkinReader.of(player) != null;
    }

    /** Which of those are glass ({@code KakuganSkin.glass}). */
    public static long glass(AbstractClientPlayer player) {
        SkinReader.Skin skin = SkinReader.of(player);
        return skin == null ? 0L : skin.glass();
    }

    /** True if glass on the player's second layer lies over one of their eye pixels (lenses): it must be drawn after the kakugan. */
    public static boolean glassOverEyes(AbstractClientPlayer player) {
        long glass = glass(player);
        return glass != 0L && (glass & KakuganSkin.eyeCells(eyes(player))) != 0L;
    }

    /** This client's own eyes (for the kakugan screen). */
    public static KakuganEyes selfEyes() {
        return eyes(Minecraft.m_91087_().f_91074_);
    }

    /** The eyes found on this client's own skin, or null. */
    public static KakuganEyes selfDetected() {
        return detected(Minecraft.m_91087_().f_91074_);
    }

    /** The face cells this client's own skin covers with its second layer. */
    public static long selfCovered() {
        return covered(Minecraft.m_91087_().f_91074_);
    }

    /** Which of those are glass. */
    public static long selfGlass() {
        return glass(Minecraft.m_91087_().f_91074_);
    }

    /** How awake a player's kakugan is this frame (0 human eyes .. 1 fully turned). */
    public static float intensity(AbstractClientPlayer player, float partialTick) {
        float[] awake = AWAKE.get(player.m_36316_().getId());
        return awake == null ? 0.0F : awake[0] + (awake[1] - awake[0]) * partialTick;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (level == null) {
            AWAKE.clear();
            return;
        }
        Map<UUID, float[]> seen = new HashMap<>();
        for (AbstractClientPlayer player : level.m_6907_()) {
            UUID id = player.m_36316_().getId();
            // Bloodlust only ever arrives for ghouls (humans read as neutral), so it needs no extra check. 1.0.18: Peace leaves his face
            // bare: the kakugan is open in it while he wears it.
            float goal = KakuganModel.awake(KaguneClientState.get(player).active() || GHOULS.contains(id) || peace(player), ControlClient.get(player).bloodlust());
            float[] awake = AWAKE.getOrDefault(id, new float[2]);
            float now = awake[1];
            now = goal > now ? Math.min(goal, now + WAKE_PER_TICK) : Math.max(goal, now - SLEEP_PER_TICK);
            seen.put(id, new float[] {awake[1], now});
        }
        AWAKE.clear();
        AWAKE.putAll(seen);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        EYES.clear();
        PLACED.clear();
        AWAKE.clear();
        GHOULS.clear();
    }
}
