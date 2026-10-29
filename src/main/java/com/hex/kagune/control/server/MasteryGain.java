package com.hex.kagune.control.server;

import com.hex.kagune.control.MasteryCurve;
import com.hex.kagune.data.KaguneProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * 6.0.4: what gameplay earns goes up the mastery curve ({@link MasteryCurve}). Bytecode hook: inside
 * {@code KaguneProfile.addMastery(player, action, amount)} (every kill, hit, grapple, guard and hunt, the original's and ours),
 * the call that added {@code amount} straight to the mastery comes here instead; the action's own tally still counts every raw
 * point. {@code /kagune} setting or adding mastery by hand goes straight to the profile, exact.
 */
public final class MasteryGain {
    /** Thousandths of a raw point carried toward the next point of mastery, in the profile root (death-cloned, reset with it). */
    static final String CARRY = "HexMasteryCarry";

    private MasteryGain() {
    }

    public static void earn(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        CompoundTag root = KaguneProfile.root(player);
        int mastery = KaguneProfile.mastery(player);
        int[] after = MasteryCurve.earn(mastery, root.m_128451_(CARRY), amount);
        root.m_128405_(CARRY, after[1]);
        if (after[0] > mastery) {
            KaguneProfile.addMastery(player, after[0] - mastery);
        }
    }
}
