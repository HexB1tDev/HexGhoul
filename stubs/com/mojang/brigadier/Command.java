// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package com.mojang.brigadier;

@FunctionalInterface
public interface Command<S> {
    int run(com.mojang.brigadier.context.CommandContext<S> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException;
}
