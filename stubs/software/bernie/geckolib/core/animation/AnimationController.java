// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / GeckoLib 4.8.4, bernie-g/geckolib branch 1.20.1). Never packaged; see verify/api-allowlist.txt.
package software.bernie.geckolib.core.animation;

/** core/src/main/java/software/bernie/geckolib/core/animation/AnimationController.java */
public class AnimationController<T extends software.bernie.geckolib.core.animatable.GeoAnimatable> {
    public AnimationController(T animatable, String name, int transitionTickTime, AnimationStateHandler<T> animationHandler) {
        throw new UnsupportedOperationException();
    }

    @FunctionalInterface
    public interface AnimationStateHandler<A extends software.bernie.geckolib.core.animatable.GeoAnimatable> {
        software.bernie.geckolib.core.object.PlayState handle(AnimationState<A> state);
    }
}
