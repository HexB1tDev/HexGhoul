// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / GeckoLib 4.8.4, bernie-g/geckolib branch 1.20.1). Never packaged; see verify/api-allowlist.txt.
package software.bernie.geckolib.core.animatable;

/** core/src/main/java/software/bernie/geckolib/core/animatable/GeoAnimatable.java */
public interface GeoAnimatable {
    void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers);

    software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache();

    double getTick(Object object);
}
