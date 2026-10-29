package com.hex.kagune.control.awakening;

import com.hex.kagune.control.server.KingServer;
import com.hex.kagune.data.KaguneProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Unlock and transition point for the final evolution of the Kaneki path.
 *
 * <p>The Final Kakuja's appearance, abilities and mechanics are not designed yet, so this class only
 * records the permanent unlock and hands control back. When the form is designed, activate it in
 * {@link #emerge} and gate it on {@link #isUnlocked}; clients already receive the flag through
 * {@link S2CAwakeningStatus#finalKakuja()}.
 */
public final class FinalKakuja {
    private FinalKakuja() {
    }

    /** True once the Dragon has shattered for this player. Permanent: survives death and kagune resets. */
    public static boolean isUnlocked(ServerPlayer player) {
        return AwakeningProfile.finalKakuja(player);
    }

    /** Permanently unlocks the final evolution. Idempotent. */
    static void unlock(ServerPlayer player) {
        boolean first = !AwakeningProfile.finalKakuja(player);
        AwakeningProfile.setStage(player, AwakeningStage.FINAL);
        if (!first) return;
        AwakeningProfile.setFinalKakuja(player, player.serverLevel().getGameTime());
        AwakeningNetwork.sendStatus(player);
        if (player.connection != null) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 30));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§f§lFINAL KAKUJA")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§cWhat kind of King will you become?")));
        }
        player.displayClientMessage(Component.literal("§4The Dragon shatters. §fKaneki emerges."), false);
        // 1.0.5: the final kakujas: War or Peace, chosen once on the two cards (1.0.10).
        KingServer.awaken(player);
    }

    /**
     * Kaneki stands up out of the fragments, back in his own body, and the player regains control. The final evolution (Peace or
     * War) is chosen here once it exists; until then nothing is forced on: the kagune stays in, as it was before the death.
     */
    static void emerge(ServerPlayer player, boolean ceremony) {
        if (!ceremony || !KaguneProfile.isUnlocked(player)) return;
        KaguneProfile.setEnergy(player, KaguneProfile.MAX_ENERGY);
    }
}
