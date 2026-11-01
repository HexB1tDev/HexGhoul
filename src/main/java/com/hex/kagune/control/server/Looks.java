package com.hex.kagune.control.server;

import com.hex.kagune.control.family.Genome;
import com.hex.kagune.data.KaguneProfile;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.10: what each player passes on to a child: their hair's colour and their skin tone (the server has no skins: each client reads its
 * own player's off their skin and sends them, {@code C2SLooks}), and whether they are a ghoul, with one kakugan.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class Looks {
    private static final Map<UUID, Integer> HAIR = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SKIN = new ConcurrentHashMap<>();

    private Looks() {
    }

    /** What their client read off their skin ({@code -1}: nothing could be told, the usual colours are kept). */
    public static void set(ServerPlayer player, int hair, int skin) {
        if (hair >= 0) {
            HAIR.put(player.m_20148_(), hair & 0xFFFFFF);
        }
        if (skin >= 0) {
            SKIN.put(player.m_20148_(), skin & 0xFFFFFF);
        }
    }

    public static int skin(ServerPlayer player) {
        return SKIN.getOrDefault(player.m_20148_(), Genome.SKIN);
    }

    public static int hair(ServerPlayer player) {
        return HAIR.getOrDefault(player.m_20148_(), Genome.HAIR);
    }

    /**
     * {@code player} as a parent: their hair, the usual eyes and skin, ghoul or not, one-eyed or not; 1.3.43: the kagune they pass on (their
     * path's: Rinkaku tails, the Ukaku pair) and its colour (their own).
     */
    public static Genome.Parent parent(ServerPlayer player) {
        boolean ghoul = KaguneProfile.isUnlocked(player);
        boolean oneEyed = ghoul && KakuganServer.eyes(player).oneEyed();
        return new Genome.Parent(hair(player), Genome.EYES, skin(player), ghoul, oneEyed, kagune(player), kaguneColour(player));
    }

    /** 1.3.43: the kagune {@code player} passes on to a child ({@code KaguneGenes} bits; none for a human). */
    public static int kagune(ServerPlayer player) {
        return com.hex.kagune.control.family.KaguneGenes.ofPath(PathServer.path(player).id(), KaguneProfile.isUnlocked(player));
    }

    /** 1.3.43: {@code player}'s kagune colour (their profile's primary colour). */
    public static int kaguneColour(ServerPlayer player) {
        try {
            return KaguneProfile.appearance(player).primaryColor() & 0xFFFFFF;
        } catch (RuntimeException unreadable) {
            return com.hex.kagune.control.KaguneColours.DEFAULT;
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        HAIR.remove(event.getEntity().m_20148_());
        SKIN.remove(event.getEntity().m_20148_());
    }
}
