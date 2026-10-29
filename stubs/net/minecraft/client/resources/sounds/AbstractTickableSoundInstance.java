// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.client.resources.sounds;

public abstract class AbstractTickableSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {
    protected AbstractTickableSoundInstance(net.minecraft.sounds.SoundEvent sound, net.minecraft.sounds.SoundSource source,
                                            net.minecraft.util.RandomSource random) {
        super(sound, source, random);
    }

    /** isStopped */ public boolean m_7801_() { throw new UnsupportedOperationException(); }
    /** stop */ protected final void m_119609_() { throw new UnsupportedOperationException(); }
}
