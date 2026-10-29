package com.hex.kagune.control.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Rinkaku's centipede legs bleed a body exactly as Ukaku's spikes do (3.1.0): the original's {@code applyCentipedeBleed} (every
 * centipede hit: the hunt's strikes, bites and crushes, and M1 strikes with the legs) is a trampoline to Ukaku's bleeding
 * ({@code UkakuCrystalCombat.bleed}): a stack a hit up to nine, heavier every second stack, the same {@code hexkagune:bleeding} effect,
 * and every client near the body told, so it drips from its wounds, pools where it stands and leaves a trail like any body
 * Ukaku's spikes have opened (the burst of blood where it was struck was already Ukaku's spurt, {@code RinkakuWounds}).
 */
public final class RinkakuBleed {
    private RinkakuBleed() {
    }

    /** Trampoline target of {@code KaguneServerController.applyCentipedeBleed}. */
    public static void applyCentipedeBleed(ServerPlayer player, LivingEntity target) {
        if (target == null || !target.m_6084_()) {
            return;
        }
        UkakuCrystalCombat.rinkakuBleed(target, player, player.m_9236_().m_46467_());
    }
}
