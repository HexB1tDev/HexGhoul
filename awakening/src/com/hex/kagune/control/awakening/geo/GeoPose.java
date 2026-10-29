package com.hex.kagune.control.awakening.geo;

import java.util.function.IntPredicate;

/**
 * The state of every bone of a {@link GeoModel}: its rest pose plus any blended animation clips,
 * and the forward kinematics that turn it into one matrix per bone.
 *
 * <p>Values follow GeckoLib: rotations are radians in GeckoLib space, a keyframe rotation
 * {@code [x, y, z]} in degrees adds {@code (-x, -y, z)} to the rest rotation, positions are the
 * keyframe's pixels as written (the matrix negates x), and scales are factors. Matrices are in
 * "geo space": blocks, y up, the model's front towards -z, x negated as in GeckoLib.
 */
public final class GeoPose {
    public final GeoModel model;
    public final double[] rotX;
    public final double[] rotY;
    public final double[] rotZ;
    public final double[] posX;
    public final double[] posY;
    public final double[] posZ;
    public final double[] scaleX;
    public final double[] scaleY;
    public final double[] scaleZ;
    /** Bones that should not be drawn this frame (scale 0 hides them too). */
    public final boolean[] hidden;

    public GeoPose(GeoModel model) {
        this.model = model;
        int n = model.bones.length;
        rotX = new double[n];
        rotY = new double[n];
        rotZ = new double[n];
        posX = new double[n];
        posY = new double[n];
        posZ = new double[n];
        scaleX = new double[n];
        scaleY = new double[n];
        scaleZ = new double[n];
        hidden = new boolean[n];
        reset();
    }

    /** Back to the model's rest pose. */
    public void reset() {
        for (int i = 0; i < model.bones.length; i++) {
            GeoModel.Bone bone = model.bones[i];
            rotX[i] = bone.rotX;
            rotY[i] = bone.rotY;
            rotZ[i] = bone.rotZ;
            posX[i] = 0.0;
            posY[i] = 0.0;
            posZ[i] = 0.0;
            scaleX[i] = 1.0;
            scaleY[i] = 1.0;
            scaleZ[i] = 1.0;
            hidden[i] = false;
        }
    }

    /**
     * Blends a clip, {@code seconds} after it started, over the current pose: {@code weight} 1
     * replaces what the clip animates, 0 leaves the pose alone. Bones for which {@code skip} is true
     * are left untouched (the client's player-model proxies, which PlayerAnimator already moved).
     */
    public void apply(GeoAnimation.Clip clip, double seconds, double weight, Molang.Context context, IntPredicate skip) {
        if (clip == null || weight <= 0.0) return;
        double local = clip.localTime(seconds);
        Molang.Context at = new Molang.Context() {
            @Override
            public double animTime() {
                return local;
            }

            @Override
            public double lifeTime() {
                return context.lifeTime();
            }

            @Override
            public double variable(String name) {
                return context.variable(name);
            }
        };
        double[] value = new double[3];
        for (var entry : clip.bones.entrySet()) {
            int i = model.bone(entry.getKey());
            if (i < 0 || (skip != null && skip.test(i))) continue;
            GeoAnimation.BoneChannels channels = entry.getValue();
            GeoModel.Bone bone = model.bones[i];
            if (channels.rotation != null) {
                channels.rotation.sample(local, at, value);
                rotX[i] = blend(rotX[i], bone.rotX + Math.toRadians(-value[0]), weight);
                rotY[i] = blend(rotY[i], bone.rotY + Math.toRadians(-value[1]), weight);
                rotZ[i] = blend(rotZ[i], bone.rotZ + Math.toRadians(value[2]), weight);
            }
            if (channels.position != null) {
                channels.position.sample(local, at, value);
                posX[i] = blend(posX[i], value[0], weight);
                posY[i] = blend(posY[i], value[1], weight);
                posZ[i] = blend(posZ[i], value[2], weight);
            }
            if (channels.scale != null) {
                channels.scale.sample(local, at, value);
                scaleX[i] = blend(scaleX[i], value[0], weight);
                scaleY[i] = blend(scaleY[i], value[1], weight);
                scaleZ[i] = blend(scaleZ[i], value[2], weight);
            }
        }
    }

    private static double blend(double from, double to, double weight) {
        return weight >= 1.0 ? to : from + (to - from) * weight;
    }

    /** One matrix per bone, parents first, from geo space of the bone's contents to the model's geo space. */
    public double[][] matrices() {
        int n = model.bones.length;
        double[][] out = new double[n][];
        for (int i = 0; i < n; i++) {
            GeoModel.Bone bone = model.bones[i];
            double[] local = Mat4.multiply(Mat4.translation(-posX[i] / 16.0, posY[i] / 16.0, posZ[i] / 16.0),
                    Mat4.pivoted(bone.pivotX / 16.0, bone.pivotY / 16.0, bone.pivotZ / 16.0, rotX[i], rotY[i], rotZ[i], scaleX[i], scaleY[i], scaleZ[i]));
            out[i] = bone.parent < 0 ? local : Mat4.multiply(out[bone.parent], local);
        }
        return out;
    }

    /** True if the bone or any ancestor is hidden or scaled to nothing. */
    public boolean invisible(int bone) {
        for (int i = bone; i >= 0; i = model.bones[i].parent) {
            if (hidden[i] || scaleX[i] == 0.0 || scaleY[i] == 0.0 || scaleZ[i] == 0.0) return true;
        }
        return false;
    }

    /** The matrix that places a cube within its bone: its own rotation about its own pivot. */
    public static double[] cubeMatrix(GeoModel.Cube cube) {
        return Mat4.pivoted(cube.pivotX / 16.0, cube.pivotY / 16.0, cube.pivotZ / 16.0, cube.rotX, cube.rotY, cube.rotZ, 1.0, 1.0, 1.0);
    }
}
