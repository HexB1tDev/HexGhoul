package com.hex.kagune.control.server;

import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Keeps the Rinkaku path's own progression out of an Ukaku kit without losing it. On the Ukaku path the original
 * profile keeps the neutral Rinkaku seed (one tail), no mutations and Threat Auto Attack off, so no Rinkaku-only system
 * (Centipede autonomy, autonomous mutations, form buffs) ever runs for him; what was there is parked in
 * {@code RinkakuStash} inside the profile root and put back if the player's path becomes Rinkaku again. Death keeps the
 * stash (it lives in the root) and {@code /kagune reset} clears it with everything else.
 */
final class RinkakuStash {
    static final String KEY = "RinkakuStash";
    // The original profile's own keys (KaguneProfile).
    private static final String FORM = "Form";
    private static final String LIMBS = "ManifestedLimbs";
    private static final String MUTATIONS = "Mutations";
    private static final String THREAT = "ThreatAutoAttack";

    private RinkakuStash() {
    }

    /** Parks Rinkaku-only progression (merging with anything already parked). Returns true if the profile changed. */
    static boolean stash(ServerPlayer player) {
        CompoundTag root = KaguneProfile.root(player);
        boolean seed = KaguneProfile.form(player) == KaguneForm.RINKAKU_SEED;
        long mutations = root.m_128454_(MUTATIONS);
        boolean threat = root.m_128471_(THREAT);
        if (seed && mutations == 0L && !threat) {
            return false;
        }
        CompoundTag stash = root.m_128469_(KEY);
        if (!seed) {
            stash.m_128359_(FORM, root.m_128461_(FORM));
            stash.m_128405_(LIMBS, root.m_128451_(LIMBS));
            KaguneProfile.setForm(player, KaguneForm.RINKAKU_SEED);
        }
        stash.m_128356_(MUTATIONS, stash.m_128454_(MUTATIONS) | mutations);
        if (threat) {
            stash.m_128379_(THREAT, true);
        }
        root.m_128365_(KEY, stash);
        root.m_128356_(MUTATIONS, 0L);
        root.m_128379_(THREAT, false);
        KaguneServerController.onFormChanged(player);
        KaguneNetwork.broadcast(player);
        return true;
    }

    /** Puts parked Rinkaku progression back (the player left the Ukaku path). Returns true if the profile changed. */
    static boolean restore(ServerPlayer player) {
        CompoundTag root = KaguneProfile.root(player);
        if (!root.m_128441_(KEY)) {
            return false;
        }
        CompoundTag stash = root.m_128469_(KEY);
        boolean changed = false;
        if (stash.m_128441_(FORM)) {
            root.m_128359_(FORM, stash.m_128461_(FORM));
            root.m_128405_(LIMBS, stash.m_128451_(LIMBS));
            changed = true;
        }
        long mutations = stash.m_128454_(MUTATIONS);
        if (mutations != 0L) {
            root.m_128356_(MUTATIONS, root.m_128454_(MUTATIONS) | mutations);
            changed = true;
        }
        if (stash.m_128471_(THREAT)) {
            root.m_128379_(THREAT, true);
            changed = true;
        }
        root.m_128365_(KEY, new CompoundTag());
        if (changed) {
            KaguneServerController.onFormChanged(player);
            KaguneNetwork.broadcast(player);
        }
        return changed;
    }
}
