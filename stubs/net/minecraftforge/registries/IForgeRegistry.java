// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.registries;

public interface IForgeRegistry<V> {
    net.minecraft.resources.ResourceLocation getKey(V value);
    V getValue(net.minecraft.resources.ResourceLocation key);
}
