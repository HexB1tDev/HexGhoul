// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.item;

public class CreativeModeTab {
    /** Forge: builder() */ public static Builder builder() { throw new UnsupportedOperationException(); }

    public static class Builder {
        /** title */ public Builder m_257941_(net.minecraft.network.chat.Component title) { throw new UnsupportedOperationException(); }
        /** icon */ public Builder m_257737_(java.util.function.Supplier<ItemStack> icon) { throw new UnsupportedOperationException(); }
        /** displayItems */ public Builder m_257501_(DisplayItemsGenerator items) { throw new UnsupportedOperationException(); }
        /** build */ public CreativeModeTab m_257652_() { throw new UnsupportedOperationException(); }
    }

    public interface DisplayItemsGenerator {
        /** accept (4.1.9: SRG m_257865_; the old name "accept" left the creative tabs' lambdas unimplemented: the inventory crashed) */
        void m_257865_(ItemDisplayParameters parameters, Output output);
    }

    public static class ItemDisplayParameters {
    }

    public interface Output {
        /** accept(ItemStack) */ void m_246342_(ItemStack stack);
    }
}
