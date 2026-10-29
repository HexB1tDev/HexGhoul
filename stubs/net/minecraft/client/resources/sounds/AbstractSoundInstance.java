// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.client.resources.sounds;

public abstract class AbstractSoundInstance implements SoundInstance {
    /** volume */ protected float f_119573_;
    /** pitch */ protected float f_119574_;
    /** x */ protected double f_119575_;
    /** y */ protected double f_119576_;
    /** z */ protected double f_119577_;
    /** looping */ protected boolean f_119578_;
    /** delay */ protected int f_119579_;
    /** attenuation */ protected SoundInstance.Attenuation f_119580_;

    protected AbstractSoundInstance(net.minecraft.sounds.SoundEvent sound, net.minecraft.sounds.SoundSource source, net.minecraft.util.RandomSource random) {
        throw new UnsupportedOperationException();
    }
}
