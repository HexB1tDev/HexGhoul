package net.minecraft.network.protocol.game;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public class ClientboundSetSubtitleTextPacket implements Packet<ClientGamePacketListener> {
    public ClientboundSetSubtitleTextPacket(Component text) {}
}
