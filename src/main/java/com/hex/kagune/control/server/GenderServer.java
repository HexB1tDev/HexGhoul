package com.hex.kagune.control.server;

import com.hex.kagune.control.Gender;
import com.hex.kagune.control.Genders;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CGender;
import com.hex.kagune.data.KaguneProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.9: each player's gender, kept in their profile root ({@link Gender#KEY}: it goes with them through death), known to this side
 * ({@link Genders}) from the moment they log in, and sent to them and to every client that sees them.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class GenderServer {
    private GenderServer() {
    }

    public static Gender of(ServerPlayer player) {
        return Gender.byOrdinal(KaguneProfile.root(player).m_128451_(Gender.KEY));
    }

    /** The gender screen's pick: kept, her box remade, everyone told. */
    public static void choose(ServerPlayer player, Gender gender) {
        if (!gender.choice()) {
            return;
        }
        String refused = refusal(player, gender);
        if (refused != null) {
            player.m_5661_(Component.m_237113_(refused), false);
            return;
        }
        Gender was = of(player);
        KaguneProfile.root(player).m_128405_(Gender.KEY, gender.ordinal());
        apply(player, gender);
        ControlNetwork.broadcast(player, new S2CGender(player.m_20148_(), gender.ordinal()));
        if (was != gender) {
            player.m_5661_(Component.m_237113_(gender.female() ? "You're a woman now." : "You're a man now."), true);
        }
    }

    /** Why {@code player} may not become {@code gender} now (null: they may). Hooks for what a body carries (1.3.10: a child). */
    static String refusal(ServerPlayer player, Gender gender) {
        if (!gender.female() && Pregnancies.pregnant(player.m_284548_(), Pregnancies.player(player.m_20148_()))) {
            return "Not while you're carrying a child.";
        }
        return null;
    }

    private static void apply(ServerPlayer player, Gender gender) {
        Genders.put(player.m_20148_(), gender);
        player.m_6210_();
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Gender gender = of(player);
            apply(player, gender);
            ControlNetwork.sendTo(player, new S2CGender(player.m_20148_(), gender.ordinal()));
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Gender gender = of(player);
            apply(player, gender);
            ControlNetwork.broadcast(player, new S2CGender(player.m_20148_(), gender.ordinal()));
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Gender gender = of(player);
            apply(player, gender);
            ControlNetwork.broadcast(player, new S2CGender(player.m_20148_(), gender.ordinal()));
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject) {
            Gender gender = of(subject);
            if (gender != Gender.UNSET) {
                ControlNetwork.sendTo(viewer, new S2CGender(subject.m_20148_(), gender.ordinal()));
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Genders.put(event.getEntity().m_20148_(), Gender.UNSET);
    }
}
