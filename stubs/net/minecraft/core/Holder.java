// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.core;

public interface Holder<T> {
    /** unwrapKey (4.2.1) */ default java.util.Optional<net.minecraft.resources.ResourceKey<T>> m_203543_() { throw new UnsupportedOperationException(); }
    /** is(TagKey) (4.1.1: a wet biome) */
    boolean m_203656_(net.minecraft.tags.TagKey<?> tag);
}
