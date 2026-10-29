package com.hex.kagune.control.client;

import com.hex.kagune.control.client.animation.KingRun;
import com.hex.kagune.control.king.KingForm;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * 1.2.2: War's and Peace's runs are heard, a footfall of the user's own pack at every step their legs take ({@link KingRun}'s own
 * stride: a whole turn of it is two steps, so the faster he runs the faster they fall): War's heavy (armoured, a stomp), Peace's light.
 * The pack's running loops have a pace of their own that a final kakuja's stride outruns at once, so the steps are played one by one
 * instead. Every client hears every runner it sees; nothing is sent.
 */
final class KingSteps {
    private static final SoundEvent HEAVY = SoundEvent.m_262824_(ResourceLocation.m_214293_("hexkagune", "sfx.step_heavy"));
    private static final SoundEvent LIGHT = SoundEvent.m_262824_(ResourceLocation.m_214293_("hexkagune", "sfx.step_light"));
    /** Blocks a tick below which he is standing, not running. */
    private static final double STILL = 0.05;
    private static final Map<UUID, Double> STRIDE = new HashMap<>();

    private KingSteps() {
    }

    /** One tick of {@code player} in {@code form} (NONE: not in one): a footfall each time a step of the stride comes down. */
    static void tick(AbstractClientPlayer player, KingForm form) {
        UUID id = player.m_20148_();
        if (form == KingForm.NONE || !player.m_20096_() || player.m_5833_()) {
            STRIDE.remove(id);
            return;
        }
        double dx = player.m_20185_() - player.f_19854_;
        double dz = player.m_20189_() - player.f_19856_;
        double speed = Math.sqrt(dx * dx + dz * dz);
        if (speed < STILL || speed > 3.0) {
            STRIDE.remove(id);
            return;
        }
        double before = STRIDE.getOrDefault(id, 0.0);
        double after = before + KingRun.phase(speed);
        if (Math.floor(after / Math.PI) > Math.floor(before / Math.PI)) {
            float run = KingRun.amount(speed);
            boolean war = form == KingForm.WAR;
            Minecraft.m_91087_().m_91106_().m_120367_(new Step(war ? HEAVY : LIGHT, player, (war ? 0.75F : 0.45F) * (0.5F + 0.5F * run),
                (war ? 0.9F : 1.05F) + 0.08F * player.m_217043_().m_188501_()));
        }
        STRIDE.put(id, after % (Math.PI * 64.0));
        if (STRIDE.size() > 64) {
            STRIDE.keySet().removeIf(other -> !other.equals(id));
        }
    }

    /** One footfall, where his feet are. */
    private static final class Step extends AbstractTickableSoundInstance {
        private Step(SoundEvent sound, AbstractClientPlayer player, float volume, float pitch) {
            super(sound, SoundSource.PLAYERS, SoundInstance.m_235150_());
            this.f_119578_ = false;
            this.f_119579_ = 0;
            this.f_119573_ = volume;
            this.f_119574_ = pitch;
            this.f_119580_ = SoundInstance.Attenuation.LINEAR;
            this.f_119575_ = player.m_20185_();
            this.f_119576_ = player.m_20186_() + 0.1;
            this.f_119577_ = player.m_20189_();
        }

        @Override
        public void m_7788_() {
        }
    }
}
