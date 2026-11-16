// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.core;

public interface RegistryAccess {
    /** registryOrThrow (4.1.9: the structure registry, to find the world's one-of-a-kind places) */
    default <E> Registry<E> m_175515_(net.minecraft.resources.ResourceKey<? extends Registry<? extends E>> key) { throw new UnsupportedOperationException(); }
}
