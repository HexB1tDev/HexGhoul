package net.minecraftforge.network;

import java.util.concurrent.CompletableFuture;
import net.minecraftforge.eventbus.api.Event;

public class NetworkEvent extends Event {
    public static class Context {
        public CompletableFuture<Void> enqueueWork(Runnable work) { throw null; }
        public void setPacketHandled(boolean handled) { throw null; }
    }
}
