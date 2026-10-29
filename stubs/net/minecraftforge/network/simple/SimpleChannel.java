// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.network.simple;

public class SimpleChannel {
    public <MSG> IndexedMessageCodec.MessageHandler<MSG> registerMessage(int index, Class<MSG> messageType,
        java.util.function.BiConsumer<MSG, net.minecraft.network.FriendlyByteBuf> encoder,
        java.util.function.Function<net.minecraft.network.FriendlyByteBuf, MSG> decoder,
        java.util.function.BiConsumer<MSG, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context>> messageConsumer) { throw new UnsupportedOperationException(); }
    public <MSG> void sendToServer(MSG message) { throw new UnsupportedOperationException(); }
    public <MSG> void send(net.minecraftforge.network.PacketDistributor.PacketTarget target, MSG message) { throw new UnsupportedOperationException(); }
}
