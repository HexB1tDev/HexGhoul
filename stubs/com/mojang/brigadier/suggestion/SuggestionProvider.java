// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package com.mojang.brigadier.suggestion;

@FunctionalInterface
public interface SuggestionProvider<S> {
    java.util.concurrent.CompletableFuture<Suggestions> getSuggestions(com.mojang.brigadier.context.CommandContext<S> context, SuggestionsBuilder builder)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException;
}
