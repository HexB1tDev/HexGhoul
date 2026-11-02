// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package com.mojang.serialization;

public abstract class MapCodec<A> {
    public <S> MapCodec<S> xmap(java.util.function.Function<? super A, ? extends S> to, java.util.function.Function<? super S, ? extends A> from) { throw new UnsupportedOperationException(); }
    public Codec<A> codec() { throw new UnsupportedOperationException(); }

    public static final class MapCodecCodec<A> implements Codec<A> {
        public MapCodec<A> codec() { throw new UnsupportedOperationException(); }
    }
}
