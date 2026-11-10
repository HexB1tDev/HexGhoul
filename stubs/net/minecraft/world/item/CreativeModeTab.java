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
        void accept(ItemDisplayParameters parameters, Output output);
    }

    public static class ItemDisplayParameters {
    }

    public interface Output {
        /** accept(ItemStack) */ void m_246342_(ItemStack stack);
    }
}
