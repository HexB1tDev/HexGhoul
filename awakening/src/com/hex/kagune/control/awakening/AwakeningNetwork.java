package com.hex.kagune.control.awakening;

import com.hex.kagune.KaguneMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Separate channel for the awakening. A client without it is refused at login with a version
 * mismatch instead of failing later on an unknown packet.
 */
public final class AwakeningNetwork {
    // "2" since 1.0.1: the client mirrors the crawling rig's attachment points.
    private static final String PROTOCOL = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(KaguneMod.id("awakening"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private AwakeningNetwork() {
    }

    static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, S2CDragonFrame.class, S2CDragonFrame::encode, S2CDragonFrame::decode, S2CDragonFrame::handle);
        CHANNEL.registerMessage(id++, S2CDragonEvent.class, S2CDragonEvent::encode, S2CDragonEvent::decode, S2CDragonEvent::handle);
        CHANNEL.registerMessage(id++, S2CAwakeningStatus.class, S2CAwakeningStatus::encode, S2CAwakeningStatus::decode, S2CAwakeningStatus::handle);
    }

    /** Everyone who can see the owner, including the owner. */
    static void toWatchers(ServerPlayer owner, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> owner), message);
    }

    static void toPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    static void sendStatus(ServerPlayer player) {
        toPlayer(player, new S2CAwakeningStatus(
                AwakeningProfile.stage(player), AwakeningProfile.finalKakuja(player)));
    }
}
