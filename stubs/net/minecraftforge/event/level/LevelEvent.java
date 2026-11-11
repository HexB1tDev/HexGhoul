// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.event.level;

/** Real hierarchy: LevelEvent -> Event; Load and Unload are its nested subclasses (4.1.14). */
public class LevelEvent extends net.minecraftforge.eventbus.api.Event {
    public net.minecraft.world.level.LevelAccessor getLevel() { throw new UnsupportedOperationException(); }

    public static class Load extends LevelEvent {
    }
}
