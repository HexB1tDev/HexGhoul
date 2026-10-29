// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.fml.common;

@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
public @interface Mod {
    String value();

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
    @interface EventBusSubscriber {
        net.minecraftforge.api.distmarker.Dist[] value() default {net.minecraftforge.api.distmarker.Dist.CLIENT, net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER};

        String modid() default "";

        Bus bus() default Bus.FORGE;

        enum Bus {
            FORGE, MOD
        }
    }
}
