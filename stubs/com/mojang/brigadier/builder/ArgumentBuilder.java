// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package com.mojang.brigadier.builder;

public abstract class ArgumentBuilder<S, T extends ArgumentBuilder<S, T>> {
    public T then(ArgumentBuilder<S, ?> argument) { throw new UnsupportedOperationException(); }
    public T executes(com.mojang.brigadier.Command<S> command) { throw new UnsupportedOperationException(); }
    public T requires(java.util.function.Predicate<S> requirement) { throw new UnsupportedOperationException(); }
}
