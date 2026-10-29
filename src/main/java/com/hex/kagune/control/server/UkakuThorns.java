package com.hex.kagune.control.server;

import com.hex.kagune.control.ukaku.CrystalThorns;
import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 5.0.15: the crystal thorns ({@link CrystalThorns}). An Ukaku in the crystal mode held by a Rinkaku's tails (or turning the
 * crystal mode on while they hold him) grows dozens of long crystal spikes out of his body, and a second later every kagune
 * holding him lets go: the original's own release, as when a crush ends (his AI, gravity and motion as they were). The legs
 * of the first kakuja end their hunt on him. Ticked by {@link UkakuServer} for every Ukaku after the crystal mode is settled;
 * only the timing and the release are here, every client draws the thorns ({@code UkakuThorns} on the client).
 */
final class UkakuThorns {
    private static final Map<UUID, CrystalThorns.Eruption> ERUPTIONS = new HashMap<>();
    /** The furthest a kagune holding him can be (blocks): beyond any tail's reach. */
    private static final double HOLDER_RANGE = 40.0;

    private UkakuThorns() {
    }

    static void tick(ServerPlayer player, boolean crystal, long now) {
        boolean thorny = crystal && KaguneProfile.isActive(player) && player.m_6084_();
        CrystalThorns.Eruption eruption = ERUPTIONS.get(player.m_20148_());
        boolean held = thorny && KaguneRuntime.isEntityHeld(player.m_20148_());
        if (eruption == null) {
            if (!held) {
                return;
            }
            eruption = new CrystalThorns.Eruption();
            ERUPTIONS.put(player.m_20148_(), eruption);
        }
        switch (eruption.tick(thorny, held, now)) {
            case ERUPT -> erupt(player);
            case RELEASE -> release(player, now);
            case SINK -> end(player, false);
            case SHATTER -> end(player, true);
            default -> {
            }
        }
        if (!eruption.out()) {
            ERUPTIONS.remove(player.m_20148_());
        }
    }

    private static Vec3 chest(ServerPlayer player) {
        return player.m_20182_().m_82520_(0.0, player.m_20206_() * 0.55, 0.0);
    }

    /** The thorns come out: a crack of crystal, and every client lays out the same thorns (the seed). */
    private static void erupt(ServerPlayer player) {
        ServerLevel level = player.m_284548_();
        Vec3 chest = chest(player);
        UkakuFire.sound(level, chest, "block.amethyst_cluster.place", 1.5F, KaguneServerController.vary(player, 0.5F, 0.04F));
        UkakuFire.sound(level, chest, "block.amethyst_cluster.break", 1.3F, KaguneServerController.vary(player, 0.65F, 0.05F));
        UkakuFire.sound(level, chest, "block.amethyst_block.chime", 1.2F, KaguneServerController.vary(player, 0.45F, 0.04F));
        UkakuFire.fx(player, S2CUkakuFx.THORNS, player.m_217043_().m_188503_(65536), chest, Vec3.f_82478_, 1.0F, CrystalThorns.COUNT);
    }

    /** Every kagune holding him lets go of him. */
    private static void release(ServerPlayer player, long now) {
        ServerLevel level = player.m_284548_();
        boolean freed = false;
        for (ServerPlayer holder : level.m_6443_(ServerPlayer.class, player.m_20191_().m_82400_(HOLDER_RANGE), other -> other != player)) {
            KaguneRuntime.State state = KaguneRuntime.peek(holder.m_20148_());
            if (state != null) {
                freed |= letGo(holder, state, player, now);
            }
        }
        if (freed) {
            Vec3 chest = chest(player);
            UkakuFire.sound(level, chest, "block.amethyst_block.break", 1.4F, KaguneServerController.vary(player, 0.6F, 0.05F));
            UkakuFire.sound(level, chest, "block.glass.break", 0.7F, KaguneServerController.vary(player, 0.7F, 0.05F));
            UkakuFire.fx(player, S2CUkakuFx.THORNS_PIERCE, 0, chest, Vec3.f_82478_, 1.0F, 0);
        }
    }

    /** {@code holder}'s kagune lets go of {@code body}: the tails holding it, or the legs' hunt on it. True if it held him. */
    static boolean letGo(ServerPlayer holder, KaguneRuntime.State state, ServerPlayer body, long now) {
        boolean legs = false;
        boolean tails = false;
        for (int slot = 0; slot < state.heldEntityIds.length; slot++) {
            boolean him = state.heldEntityIds[slot] != 0 && state.heldEntityIds[slot] == body.m_19879_()
                || body.m_20148_().equals(state.heldEntityUuids[slot]);
            if (!him) {
                continue;
            }
            if (state.autonomousCentipedeHeld[slot] && slot == state.centipedeAttackLimbIndex && state.centipedeAttackActive()) {
                legs = true; // the first kakuja's legs caught him: their hunt ends (letting him go)
            } else {
                KaguneServerController.releaseHeldSlot(holder.m_284548_(), state, slot);
                tails = true;
            }
        }
        if (legs) {
            CentipedeHunt.finish(holder, state, now);
        }
        if (tails) {
            if (RinkakuAutonomy.handling(state.mode)) {
                KaguneServerController.returnToHoldingOrRetract(holder, state, now);
            }
            KaguneNetwork.broadcast(holder);
        }
        return legs || tails;
    }

    /** The thorns sink back into him ({@code shatter} false) or shatter (the crystal thawed). */
    private static void end(ServerPlayer player, boolean shatter) {
        ServerLevel level = player.m_284548_();
        Vec3 chest = chest(player);
        if (shatter) {
            UkakuFire.sound(level, chest, "block.amethyst_cluster.break", 1.0F, KaguneServerController.vary(player, 0.8F, 0.05F));
        } else {
            UkakuFire.sound(level, chest, "block.amethyst_block.step", 1.0F, KaguneServerController.vary(player, 0.5F, 0.04F));
        }
        UkakuFire.fx(player, S2CUkakuFx.THORNS_END, 0, chest, Vec3.f_82478_, 1.0F, shatter ? 1 : 0);
    }

    /** He left the Ukaku path: thorns still out shatter. */
    static void forget(ServerPlayer player) {
        CrystalThorns.Eruption eruption = ERUPTIONS.remove(player.m_20148_());
        if (eruption != null && eruption.out()) {
            end(player, true);
        }
    }

    /** He logged out (his entity goes with him on every client). */
    static void loggedOut(UUID player) {
        ERUPTIONS.remove(player);
    }
}
