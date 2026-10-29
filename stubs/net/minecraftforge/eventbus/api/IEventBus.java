// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.eventbus.api;

public interface IEventBus {
    /** True when a listener cancelled it. */
    boolean post(Event event);
    <T extends Event> void addListener(EventPriority priority, boolean receiveCancelled, Class<T> eventType, java.util.function.Consumer<T> consumer);
}
