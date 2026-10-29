package com.mojang.brigadier.builder;

import com.mojang.brigadier.Command;
import java.util.function.Predicate;

public abstract class ArgumentBuilder<S, T extends ArgumentBuilder<S, T>> {
    public T then(ArgumentBuilder<S, ?> argument) { throw null; }
    public T executes(Command<S> command) { throw null; }
    public T requires(Predicate<S> requirement) { throw null; }
}
