// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.event.entity.living;

public class LivingEvent extends net.minecraftforge.event.entity.EntityEvent {
    public net.minecraft.world.entity.LivingEntity getEntity() { throw new UnsupportedOperationException(); }

    /** 1.3.21: fired each tick of every living entity (a sleeper's night, HomeLife). */
    public static class LivingTickEvent extends LivingEvent {
    }
}
