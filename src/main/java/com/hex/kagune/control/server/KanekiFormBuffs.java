package com.hex.kagune.control.server;

import com.hex.kagune.control.KanekiForms;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

/**
 * The centipede forms' buffs (speed, resistance, health boost) without the health they used to cost (3.0.2). The original
 * re-adds each buff every few ticks; re-adding Health Boost takes its extra hearts off and puts them back, and vanilla cuts the
 * health down to the bare maximum in between. With ghoul regeneration filling those hearts straight back in, a kakuja looked
 * like taking damage nonstop (a red flash and a new wound every quarter second). The health is put back in the same tick, so
 * nobody ever sees it drop.
 *
 * <p>5.0.9: the first kakuja runs faster than the original's Speed III ({@link KanekiForms#speed}): its legs fight on their own,
 * so the player's part is getting them in reach.
 */
public final class KanekiFormBuffs {
    private KanekiFormBuffs() {
    }

    /** Trampoline target of the original's {@code KaguneServerController.refreshFormEffect}. */
    public static void refreshFormEffect(ServerPlayer player, MobEffect effect, int amplifier) {
        if (effect == MobEffects.f_19596_ && KaguneProfile.isActive(player) && !AyatoServer.isAyato(player)) {
            amplifier = Math.max(amplifier, KanekiForms.speed(KaguneProfile.form(player)));
        }
        float health = player.m_21223_();
        KaguneServerController.refreshFormEffectLegacy(player, effect, amplifier);
        if (player.m_6084_() && player.m_21223_() < health) {
            player.m_21153_(health);
        }
    }
}
