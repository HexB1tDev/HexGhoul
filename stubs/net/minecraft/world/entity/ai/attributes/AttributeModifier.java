// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.entity.ai.attributes;

public class AttributeModifier {
    public AttributeModifier(java.util.UUID id, String name, double amount, Operation operation) { throw new UnsupportedOperationException(); }

    public enum Operation {
        ADDITION,
        MULTIPLY_BASE,
        MULTIPLY_TOTAL
    }
}
