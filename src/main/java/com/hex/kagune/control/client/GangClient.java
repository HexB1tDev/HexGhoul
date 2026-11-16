package com.hex.kagune.control.client;

import com.hex.kagune.control.network.S2CGangName;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** 4.1.50: what this client is told of each player's gang ({@link S2CGangName}): kept, and their name asked for again. */
public final class GangClient {
    private GangClient() {
    }

    /** 4.1.70: true if {@code player} is one of Anteiku, as this side knows it (the server tells every client each player's gang). */
    public static boolean anteiku(Player player) {
        Integer gang = player == null ? null : com.hex.kagune.control.server.GangServer.SEEN.get(player.m_20148_());
        return gang != null && gang == com.hex.kagune.control.npc.GangPlayer.ANTEIKU;
    }

    public static void accept(S2CGangName message) {
        com.hex.kagune.control.server.GangServer.SEEN.put(message.player(), message.gang());
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ == null) {
            return;
        }
        for (Player player : minecraft.f_91073_.m_6907_()) {
            if (player.m_20148_().equals(message.player())) {
                player.refreshDisplayName();
            }
        }
    }
}
