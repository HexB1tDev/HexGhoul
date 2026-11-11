// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.event.level;

/** Real hierarchy: ChunkEvent -> LevelEvent (getLevel) -> Event; Load is its nested subclass (4.1.14). */
public class ChunkEvent extends LevelEvent {
    public net.minecraft.world.level.chunk.ChunkAccess getChunk() { throw new UnsupportedOperationException(); }

    public static class Load extends ChunkEvent {
    }
}
