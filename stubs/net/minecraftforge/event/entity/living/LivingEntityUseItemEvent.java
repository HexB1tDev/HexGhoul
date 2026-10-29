// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.event.entity.living;

public class LivingEntityUseItemEvent extends LivingEvent {
    public net.minecraft.world.item.ItemStack getItem() { throw new UnsupportedOperationException(); }

    public static class Start extends LivingEntityUseItemEvent {
    }

    public static class Tick extends LivingEntityUseItemEvent {
    }

    public static class Finish extends LivingEntityUseItemEvent {
        public net.minecraft.world.item.ItemStack getResultStack() { throw new UnsupportedOperationException(); }
    }
}
