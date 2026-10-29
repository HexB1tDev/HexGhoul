package net.minecraftforge.network.simple;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class SimpleChannel {
    public <M> IndexedMessageCodec.MessageHandler<M> registerMessage(int index, Class<M> type, BiConsumer<M, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, M> decoder, BiConsumer<M, Supplier<NetworkEvent.Context>> handler) { throw null; }
    public <M> void send(PacketDistributor.PacketTarget target, M message) { throw null; }
}
