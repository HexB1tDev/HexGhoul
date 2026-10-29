package com.hex.kagune.control.server;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CPathPrompt;
import com.hex.kagune.control.network.S2CPathState;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

/**
 * Server owner of each player's kagune path. The path is stored as {@code Path} inside the original profile
 * root ({@code HexKagune1_0}), so death-cloning carries it and {@code /kagune reset} clears it with everything else.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class PathServer {
    static final String KEY = "Path";
    /** Players currently owed the starting screen; entering this set (not staying in it) sends the prompt. */
    private static final Set<UUID> OWED = new HashSet<>();

    private PathServer() {
    }

    /** The stored path. A profile that never stored one is marked on first read (see {@link KagunePath#forUnmarkedProfile}). */
    public static KagunePath path(ServerPlayer player) {
        CompoundTag root = KaguneProfile.root(player);
        if (root.m_128441_(KEY)) {
            String stored = root.m_128461_(KEY);
            KagunePath path = KagunePath.fromId(stored);
            if (path.chosen() && !path.id().equals(stored)) {
                // 7.0.0: a save still naming the dormant Owl DNA (or its older id) is moved onto Graft DNA for good.
                root.m_128359_(KEY, path.id());
            }
            return path;
        }
        KagunePath initial = KagunePath.forUnmarkedProfile(KaguneProfile.isUnlocked(player), KaguneProfile.mastery(player),
            KaguneProfile.mutationMask(player), KaguneProfile.form(player) == KaguneForm.RINKAKU_SEED);
        root.m_128359_(KEY, initial.id());
        return initial;
    }

    /** Stores {@code path} and syncs it. A manifested kagune is put away first so no state from the old path lingers. */
    public static void setPath(ServerPlayer player, KagunePath requested) {
        // 7.0.0: a dormant path is never stored (1.2.5: asking for one changes nothing).
        if (requested.dormant()) {
            return;
        }
        KagunePath path = requested;
        if (path(player) != path && KaguneProfile.isActive(player)) {
            KaguneServerController.release(player, false);
            KaguneRuntime.remove(player.m_20148_());
            KaguneProfile.setActive(player, false);
            KaguneNetwork.broadcast(player);
        }
        KaguneProfile.root(player).m_128359_(KEY, path.id());
        ControlNetwork.broadcast(player, new S2CPathState(player.m_20148_(), path));
        UkakuServer.onPathChanged(player);
    }

    /**
     * {@code /kagune reset <player>} (5.0.2): everything kagune about the player is wiped, as if they had never picked one: the
     * kagune is put away (a Rinkaku kakuja shatters on every client), the whole profile goes back to its defaults (path, mastery,
     * form, both paths' mutations, the kakugan's placement, the flesh reserve, the stash), ghoul mode closes, Ukaku's state is
     * dropped, and the starting screen is offered again. A ghoul stays a ghoul (unlocked), as the original's {@code /kagune reset}
     * leaves it.
     */
    public static void reset(ServerPlayer player) {
        boolean unlocked = KaguneProfile.isUnlocked(player);
        KaguneServerController.release(player, false);
        KaguneRuntime.remove(player.m_20148_());
        GhoulServer.set(player, false, false);
        KaguneProfile.reset(player);
        KaguneProfile.setUnlocked(player, unlocked);
        setPath(player, KagunePath.NONE);
        KaguneNetwork.broadcast(player);
        KakuganServer.forget(player);
        OWED.remove(player.m_20148_());
        ControlNetwork.sendTo(player, new S2CPathPrompt());
    }

    /**
     * A player's own choice from the starting screen. Accepted only once: the kagune must be unlocked, no path may be
     * stored yet, and the request must name a real path. Anything else just resyncs the client's view.
     */
    public static void choose(ServerPlayer player, KagunePath requested) {
        KagunePath current = path(player);
        if (!requested.chosen() || current.chosen() || !KaguneProfile.isUnlocked(player)) {
            ControlNetwork.sendTo(player, new S2CPathState(player.m_20148_(), current));
            return;
        }
        setPath(player, requested);
        player.m_5661_(Component.m_237110_("message.hexkagune.path.awakened", requested.displayName()), true);
        KaguneServerController.playSelf(player, HexKaguneSounds.MANIFEST_RIP, 0.7F, requested.awakenPitch());
        KaguneMod.LOGGER.info(player.m_36316_().getName() + " chose the " + requested.id() + " kagune path");
        KakuganServer.askIfNeeded(player); // next: where their eyes are, for the kakugan
    }

    /**
     * Trampoline target for the original {@code KaguneServerController.toggle} (bytecode hook): an unlocked kagune
     * with no path cannot manifest; the player is reminded and the starting screen is offered again. Retracting and
     * everything on a chosen path go straight to the original.
     */
    public static void toggle(ServerPlayer player) {
        int spent = KaguneProfile.isActive(player) ? 0 : KaguneLock.locked(player);
        if (spent > 0) {
            // 6.0.0: run dry, the kagune stays in for a while; only the eyes.
            player.m_5661_(Component.m_237110_("message.hexkagune.kagune_spent_wait", spent), true);
            return;
        }
        if (!KaguneProfile.isActive(player) && !path(player).chosen()) {
            player.m_5661_(Component.m_237115_("message.hexkagune.path.choose_first"), true);
            ControlNetwork.sendTo(player, new S2CPathPrompt());
            return;
        }
        KaguneServerController.toggleLegacy(player);
    }

    /**
     * Redirect target for the manifest rip in the original {@code toggle} (bytecode hook): silent. Every client now plays
     * the path's own sound per limb as it comes out ({@code client/ManifestSounds}).
     */
    public static void manifestRip(ServerPlayer player, RegistryObject<SoundEvent> sound, float volume, float pitch) {
    }

    /** Offers the starting screen when a kagune becomes owed a path: on unlock, login, {@code /kagune reset} or {@code path set none}. */
    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.m_9236_().m_46467_() % 10L != 0L) {
            return;
        }
        boolean owed = KagunePath.choiceOwed(KaguneProfile.isUnlocked(player), path(player));
        if (owed && OWED.add(player.m_20148_())) {
            ControlNetwork.sendTo(player, new S2CPathPrompt());
        } else if (!owed) {
            OWED.remove(player.m_20148_());
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        OWED.remove(event.getEntity().m_20148_());
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OWED.remove(player.m_20148_()); // a fresh session gets the prompt again
            // Marks pre-1.2.0 profiles right away, before any new progress could change how they are classified.
            ControlNetwork.sendTo(player, new S2CPathState(player.m_20148_(), path(player)));
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject) {
            ControlNetwork.sendTo(viewer, new S2CPathState(subject.m_20148_(), path(subject)));
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        PathCommands.register(event.getDispatcher());
    }
}
