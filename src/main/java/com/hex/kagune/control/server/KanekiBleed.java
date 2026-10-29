package com.hex.kagune.control.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Kaneki's centipede legs bleed a body exactly as Ayato's spikes do (3.1.0): the original's {@code applyCentipedeBleed} (every
 * centipede hit: the hunt's strikes, bites and crushes, and M1 strikes with the legs) is a trampoline to Ayato's bleeding
 * ({@code AyatoCrystal.bleed}): a stack a hit up to nine, heavier every second stack, the same {@code hexkagune:bleeding} effect,
 * and every client near the body told, so it drips from its wounds, pools where it stands and leaves a trail like any body
 * Ayato's spikes have opened (the burst of blood where it was struck was already Ayato's spurt, {@code KanekiWounds}).
 */
public final class KanekiBleed {
    private KanekiBleed() {
    }

    /** Trampoline target of {@code KaguneServerController.applyCentipedeBleed}. */
    public static void applyCentipedeBleed(ServerPlayer player, LivingEntity target) {
        if (target == null || !target.m_6084_()) {
            return;
        }
        AyatoCrystal.kanekiBleed(target, player, player.m_9236_().m_46467_());
    }
}
