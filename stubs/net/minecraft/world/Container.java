// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world;

public interface Container extends Clearable {
    /** getContainerSize (1.3.29) */ int m_6643_();
    /** getItem(slot) (1.3.29) */ net.minecraft.world.item.ItemStack m_8020_(int slot);
    /** setItem(slot, stack) (1.3.29) */ void m_6836_(int slot, net.minecraft.world.item.ItemStack stack);
    /** setChanged (1.3.29) */ void m_6596_();
}
