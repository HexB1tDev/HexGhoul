// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.common;

public class ForgeConfigSpec implements net.minecraftforge.fml.config.IConfigSpec<ForgeConfigSpec> {
    public static class Builder {
        public Builder() { throw new UnsupportedOperationException(); }
        public BooleanValue define(String path, boolean defaultValue) { throw new UnsupportedOperationException(); }
        public DoubleValue defineInRange(String path, double defaultValue, double min, double max) { throw new UnsupportedOperationException(); }
        public Builder comment(String... comment) { throw new UnsupportedOperationException(); }
        public Builder push(String path) { throw new UnsupportedOperationException(); }
        public Builder pop() { throw new UnsupportedOperationException(); }
        public ForgeConfigSpec build() { throw new UnsupportedOperationException(); }
    }

    public static class ConfigValue<T> implements java.util.function.Supplier<T> {
        public T get() { throw new UnsupportedOperationException(); }
        public void set(T value) { throw new UnsupportedOperationException(); }
        public void save() { throw new UnsupportedOperationException(); }
    }

    public static class BooleanValue extends ConfigValue<Boolean> {
    }

    public static class DoubleValue extends ConfigValue<Double> {
    }
}
