// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package dev.kosmx.playerAnim.api.layered;

public class ModifierLayer<T extends IAnimation> implements IAnimation {
    public ModifierLayer() { throw new UnsupportedOperationException(); }
    public void setAnimation(T animation) { throw new UnsupportedOperationException(); }
    public T getAnimation() { throw new UnsupportedOperationException(); }
    public boolean isActive() { throw new UnsupportedOperationException(); }
    public dev.kosmx.playerAnim.core.util.Vec3f get3DTransform(String modelName, dev.kosmx.playerAnim.api.TransformType type, float tickDelta,
        dev.kosmx.playerAnim.core.util.Vec3f value0) { throw new UnsupportedOperationException(); }
    public void setupAnim(float tickDelta) { throw new UnsupportedOperationException(); }
}
