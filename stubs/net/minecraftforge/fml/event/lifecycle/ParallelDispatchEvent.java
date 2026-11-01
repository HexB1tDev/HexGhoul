// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.fml.event.lifecycle;

/** 1.3.25: a mod loading event run on loading threads: work for the game's own thread queued with enqueueWork. */
public class ParallelDispatchEvent extends net.minecraftforge.eventbus.api.Event {
    public java.util.concurrent.CompletableFuture<Void> enqueueWork(Runnable work) { throw new UnsupportedOperationException(); }
}
