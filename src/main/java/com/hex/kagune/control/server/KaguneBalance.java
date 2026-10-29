package com.hex.kagune.control.server;

import com.hex.kagune.control.DamageBalance;
import com.hex.kagune.control.EnergyBalance;
import com.hex.kagune.data.KaguneProfile;
import net.minecraft.server.level.ServerPlayer;

/**
 * Kaneki's damage, brought down ({@link DamageBalance}, 3.0.3): the original's swipe, crush and slam damage are trampolines
 * here, so every Kaneki hit (combos, the Centipede legs, Threat Auto Attack, crush and slam) is scaled the same way.
 */
public final class KaguneBalance {
    private KaguneBalance() {
    }

    /** Trampoline target of {@code KaguneProfile.swipeDamage}. */
    public static float swipeDamage(ServerPlayer player) {
        return KaguneProfile.swipeDamageLegacy(player) * DamageBalance.scale(KaguneProfile.mastery(player));
    }

    /** Trampoline target of {@code KaguneProfile.crushDamage}. */
    public static float crushDamage(ServerPlayer player) {
        return KaguneProfile.crushDamageLegacy(player) * DamageBalance.scale(KaguneProfile.mastery(player));
    }

    /**
     * Trampoline target of {@code KaguneProfile.actionEnergyCost} (6.1.0): the original's cost of a move (more for every limb
     * out, less with the Reach mutation), its own mastery cut undone (1.0.33: the bar takes mastery off everything it pays for,
     * {@link SharedBar#consumeEnergy}).
     */
    public static float actionEnergyCost(ServerPlayer player, float base) {
        return KaguneProfile.actionEnergyCostLegacy(player, base) * EnergyBalance.undoOriginal(KaguneProfile.mastery(player));
    }

    /** Trampoline target of {@code KaguneProfile.slamDamage}. */
    public static float slamDamage(ServerPlayer player) {
        return KaguneProfile.slamDamageLegacy(player) * DamageBalance.scale(KaguneProfile.mastery(player));
    }
}
