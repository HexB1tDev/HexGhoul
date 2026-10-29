// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package dev.kosmx.playerAnim.minecraftApi;

@FunctionalInterface
public interface PlayerAnimationFactory {
    FactoryHolder ANIMATION_DATA_FACTORY = null;

    dev.kosmx.playerAnim.api.layered.IAnimation invoke(net.minecraft.client.player.AbstractClientPlayer player);

    class FactoryHolder {
        public void registerFactory(net.minecraft.resources.ResourceLocation id, int priority, PlayerAnimationFactory factory) { throw new UnsupportedOperationException(); }
    }
}
