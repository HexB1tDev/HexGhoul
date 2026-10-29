// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package dev.kosmx.playerAnim.api.layered;

public interface IAnimation {
    default void tick() {
    }

    boolean isActive();

    dev.kosmx.playerAnim.core.util.Vec3f get3DTransform(String modelName, dev.kosmx.playerAnim.api.TransformType type, float tickDelta,
        dev.kosmx.playerAnim.core.util.Vec3f value0);

    void setupAnim(float tickDelta);
}
