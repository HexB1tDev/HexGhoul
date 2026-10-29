// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.network;

public class NetworkEvent extends net.minecraftforge.eventbus.api.Event {
    public static class Context {
        public java.util.concurrent.CompletableFuture<Void> enqueueWork(Runnable runnable) { throw new UnsupportedOperationException(); }
        public void setPacketHandled(boolean packetHandled) { throw new UnsupportedOperationException(); }
        /** null when received on the client */ public net.minecraft.server.level.ServerPlayer getSender() { throw new UnsupportedOperationException(); }
    }
}
