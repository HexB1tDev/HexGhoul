package com.hex.kagune.control.client.animation;

import com.hex.kagune.client.animation.ManifestPainAnimation;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;

/**
 * The original manifestation pain pose, scaled by kagune control: slightly stronger than before for a
 * brand-new ghoul, barely a flinch for a master. The original animation is reused, with one correction: it doubles
 * over by rotating and bending the torso mesh, which PlayerAnimator does without the arms and head (they stayed upright
 * and came apart from the shoulders). That forward fold is a lean of the whole upper body about the hips instead
 * ({@link RigPose}: the torso, the head and both arms turned together, each through its own pivot and rotation).
 *
 * <p>6.0.2: until now the fold went to PlayerAnimator's body bend, which draws the head and arms under a matrix of its own
 * that their model parts never carry: everything else drawn on the head from its part (the kakugan, Rinkaku's mask, the skin's
 * second layer under 3D Skin Layers) stayed where the face had been as he doubled over, the eyes hanging in front of his chest.
 * Ukaku's leans were moved off the body bend the same way in 5.0.19.
 */
public final class ControlledManifestAnimation implements IAnimation {
    private final ManifestPainAnimation delegate;
    private final float intensity;

    public ControlledManifestAnimation(int duration, int limbCount, float intensity) {
        this.delegate = new ManifestPainAnimation(duration, limbCount);
        this.intensity = Math.max(0.0F, Math.min(1.4F, intensity));
    }

    @Override
    public void tick() {
        this.delegate.tick();
    }

    @Override
    public boolean isActive() {
        return this.delegate.isActive();
    }

    @Override
    public Vec3f get3DTransform(String modelName, TransformType type, float tickDelta, Vec3f value0) {
        if (type == TransformType.BEND && (modelName.equals("body") || modelName.equals("torso"))) {
            // The fold is a lean about the hips now (below), never a bend.
            return value0;
        }
        Vec3f out = this.scaled(modelName, type, tickDelta, value0);
        if (type == TransformType.BEND || !LEANS.contains(modelName)) {
            return out;
        }
        float fold = this.fold(tickDelta);
        if (fold == 0.0F) {
            return out;
        }
        RigPose lean = new RigPose();
        lean.lean = fold;
        float[] turned = lean.transform(modelName, type == TransformType.POSITION ? RigPose.POSITION : RigPose.ROTATION, out.getX(), out.getY(),
            out.getZ());
        return new Vec3f(turned[0], turned[1], turned[2]);
    }

    /** The parts the fold leans: the whole upper body. */
    private static final java.util.Set<String> LEANS = java.util.Set.of("head", "torso", "rightArm", "leftArm");

    /** The torso's forward fold this frame (its x rotation plus its bend in the original), scaled: radians forward. */
    private float fold(float tickDelta) {
        Vec3f zero = new Vec3f(0.0F, 0.0F, 0.0F);
        return (this.delegate.get3DTransform("torso", TransformType.ROTATION, tickDelta, zero).getX()
            + this.delegate.get3DTransform("torso", TransformType.BEND, tickDelta, zero).getY()) * this.intensity;
    }

    /** The original's own transform for a part, scaled by the intensity; the torso's fold taken out of its rotation. */
    private Vec3f scaled(String modelName, TransformType type, float tickDelta, Vec3f value0) {
        Vec3f animated = this.delegate.get3DTransform(modelName, type, tickDelta, value0);
        if (modelName.equals("torso") && type == TransformType.ROTATION && animated != value0) {
            animated = new Vec3f(value0.getX(), animated.getY(), animated.getZ());
        }
        if (animated == value0) {
            return value0;
        }
        float x0 = value0.getX();
        float y0 = value0.getY();
        float z0 = value0.getZ();
        return new Vec3f(
            x0 + (animated.getX() - x0) * this.intensity,
            y0 + (animated.getY() - y0) * this.intensity,
            z0 + (animated.getZ() - z0) * this.intensity);
    }

    @Override
    public void setupAnim(float tickDelta) {
        this.delegate.setupAnim(tickDelta);
    }
}
