// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.client.extensions.common;

/** 4.1.20: Forge 47's client item extensions (an item's own renderer, BEWLR, for a model whose parent is builtin/entity). */
public interface IClientItemExtensions {
    /** getCustomRenderer (Forge's name: Forge's own interface) */
    default net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { throw new UnsupportedOperationException(); }
}
