package com.hex.kagune.control.server;

import com.hex.kagune.control.eto.OwlRules;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CEtoOwl;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import net.minecraft.server.level.ServerPlayer;

/**
 * 6.2.0, the Owl (Eto's first kakuja, {@link OwlRules}): K tapped grows it, K again draws it back into her. While it grows or goes
 * nothing starts; in it, her keys are its own moves ({@link EtoOwlMoves}, {@link EtoCancel#allowedOwl}), never her base form's.
 * Every client draws it from {@link S2CEtoOwl}.
 */
final class EtoOwl {
    private EtoOwl() {
    }

    /** K, tapped or held (6.2.0: the Owl is her only kakuja, so a hold is a tap): into the Owl or out of it. */
    static void key(ServerPlayer player, EtoServer.EtoState state, boolean hold, long now) {
        if (now < state.owlReadyAt || OwlRules.changing(state.owl, now - state.owlAt)) {
            return; // still growing or going
        }
        if (state.owl) {
            if (state.move.owl()) {
                // 6.2.0: out of whatever of its moves she was in.
                EtoCancel.cut(player, state, now);
                EtoServer.broadcast(player, state);
            }
            set(player, state, false, now);
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_DISMISS, 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
            return;
        }
        if (EtoServer.locked(player, state, "The Owl (kakuja V1)", OwlRules.MASTERY)) {
            return;
        }
        if (!KaguneServerController.useEnergy(player, OwlRules.COST, true)) {
            return;
        }
        // 6.2.0: out of whatever she was doing (EtoCancel).
        if (state.move != com.hex.kagune.control.eto.EtoMove.NONE || state.armOut) {
            EtoCancel.cut(player, state, now);
            EtoServer.broadcast(player, state);
        }
        EtoWatch.forget(player);
        set(player, state, true, now);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_STRAIN, 1.0F, KaguneServerController.vary(player, 0.55F, 0.05F));
        KaguneServerController.playSelf(player, HexKaguneSounds.MANIFEST_RIP, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
    }

    /** At the burst, the flesh tearing out of her. */
    static void tick(ServerPlayer player, EtoServer.EtoState state, long now) {
        if (state.owl && now - state.owlAt == OwlRules.BURST) {
            KaguneServerController.playSelf(player, HexKaguneSounds.BONE_CRACK, 1.0F, KaguneServerController.vary(player, 0.55F, 0.05F));
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        }
    }

    /** Whether it is out, growing or going: no move of her base form may start. */
    static boolean busy(EtoServer.EtoState state, long now) {
        return state.owl || OwlRules.changing(false, now - state.owlAt);
    }

    /** 6.2.0: whether she is in it, all grown out: its own moves may start. */
    static boolean ready(EtoServer.EtoState state, long now) {
        return state.owl && !OwlRules.changing(true, now - state.owlAt);
    }

    private static void set(ServerPlayer player, EtoServer.EtoState state, boolean on, long now) {
        if (!on) {
            OwlWatch.retract(player, now); // its lances draw back into her as it goes
        }
        state.owl = on;
        state.owlAt = now;
        state.owlReadyAt = now + (on ? OwlRules.GROW_TICKS : OwlRules.SHED_TICKS) + Cooldowns.of(player, OwlRules.COOLDOWN);
        ControlNetwork.broadcast(player, new S2CEtoOwl(player.m_20148_(), on, now));
    }

    /** It goes at once (her form put away, death, another world, a new path): turned back, every client told. */
    static void forget(ServerPlayer player, EtoServer.EtoState state) {
        if (state != null && state.owl) {
            set(player, state, false, player.m_9236_().m_46467_());
        }
    }

    /** {@code viewer} has just come to see {@code subject}: it learns she is in the Owl, if she is. */
    static void tracked(ServerPlayer viewer, ServerPlayer subject, EtoServer.EtoState state) {
        if (state != null && state.owl) {
            ControlNetwork.sendTo(viewer, new S2CEtoOwl(subject.m_20148_(), true, state.owlAt));
        }
    }
}
