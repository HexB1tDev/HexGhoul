package com.hex.kagune.control.server;

import com.hex.kagune.control.kakugan.KakuganEyes;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CKakuganPrompt;
import com.hex.kagune.control.network.S2CKakuganState;
import com.hex.kagune.data.KaguneProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server owner of each player's kakugan placement: where their eyes are on their skin, stored as {@code Kakugan}
 * (one packed int) inside the original profile root, so death keeps it. Synced to the player and everyone tracking
 * them (every client draws everyone's kakugan). A player whose path is chosen but whose eyes were never placed is
 * asked once per session (right after choosing a path, or on login); {@code /kagune eyes} asks again any time.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class KakuganServer {
    static final String KEY = "Kakugan";
    /** Players already asked to place their eyes this session. */
    private static final Set<UUID> ASKED = new HashSet<>();

    private KakuganServer() {
    }

    public static boolean placed(ServerPlayer player) {
        return KaguneProfile.root(player).m_128441_(KEY);
    }

    public static KakuganEyes eyes(ServerPlayer player) {
        return placed(player) ? KakuganEyes.unpack(KaguneProfile.root(player).m_128451_(KEY)) : KakuganEyes.DEFAULT;
    }

    /**
     * The player's own placement from the kakugan screen (validated: an eye can only ever land on the face; on Pale's path, one
     * eye only, whatever a client sends).
     */
    public static void place(ServerPlayer player, KakuganEyes eyes) {
        if (PathServer.path(player) == com.hex.kagune.control.KagunePath.PALE && !eyes.oneEyed()) {
            eyes = eyes.withGhoul(KakuganEyes.RIGHT);
        }
        if (PathServer.path(player) == com.hex.kagune.control.KagunePath.GRAFT) {
            eyes = eyes.withGhoul(KakuganEyes.RIGHT); // 7.0.0: Graft's one kakugan is always the right eye
        }
        KaguneProfile.root(player).m_128405_(KEY, eyes.validated().pack());
        ASKED.add(player.m_20148_());
        ControlNetwork.broadcast(player, state(player));
    }

    static S2CKakuganState state(ServerPlayer player) {
        return new S2CKakuganState(player.m_20148_(), eyes(player), placed(player));
    }

    /** After a reset: the eyes were never placed; every client hears so, and the player is asked again once they pick a path. */
    static void forget(ServerPlayer player) {
        ASKED.remove(player.m_20148_());
        ControlNetwork.broadcast(player, state(player));
    }

    /** Asks the player to place their eyes now (the screen opens as soon as they are free). */
    public static void ask(ServerPlayer player) {
        ASKED.add(player.m_20148_());
        ControlNetwork.sendTo(player, new S2CKakuganPrompt());
    }

    /** Right after a path is chosen, or on login with a chosen path: asks once if the eyes were never placed. */
    static void askIfNeeded(ServerPlayer player) {
        if (KaguneProfile.isUnlocked(player) && PathServer.path(player).chosen() && !placed(player) && !ASKED.contains(player.m_20148_())) {
            ask(player);
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.m_9236_().m_46467_() % 20L == 7L) {
            askIfNeeded(player);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ASKED.remove(player.m_20148_());
            ControlNetwork.sendTo(player, state(player));
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ASKED.remove(event.getEntity().m_20148_());
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject) {
            ControlNetwork.sendTo(viewer, state(subject));
        }
    }

    /** {@code /kagune eyes}: place (or move) your kakugan. */
    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.m_82127_("kagune").then(Commands.m_82127_("eyes").executes(KakuganServer::eyesCommand)));
    }

    private static int eyesCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().m_81375_();
        if (!KaguneProfile.isUnlocked(player)) {
            context.getSource().m_81352_(Component.m_237115_("message.hexkagune.kakugan.locked"));
            return 0;
        }
        ask(player);
        return 1;
    }
}
